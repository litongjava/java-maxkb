package nexus.io.mosskb.service.kb;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import nexus.io.tio.utils.environment.EnvUtils;

/**
 * 用户代码只在一次性容器或一次性沙箱用户进程里执行，任何情况下都不以 Java 进程的身份运行。
 * 引擎由 {@code kb.python.runner} 选择：docker（默认，本机或远程引擎）或 host（Linux 宿主 su 到沙箱用户）。
 */
public class IsolatedPythonExecutor {
  private static final Semaphore SLOTS = new Semaphore(2);

  /** Docker CLI 只保留这些环境变量；其余全部剥离，容器里看不到宿主密钥。 */
  private static final List<String> DOCKER_ENV_ALLOWLIST = List.of("PATH", "SYSTEMROOT", "WINDIR", "TEMP", "TMP",
      "HOME", "USERPROFILE", "DOCKER_HOST", "DOCKER_CONTEXT", "DOCKER_CONFIG", "DOCKER_TLS_VERIFY", "DOCKER_CERT_PATH");

  /** 宿主 runner 只保留这些环境变量，应用密钥（GITEE_API_KEY、jdbc.pswd 等）不会传给用户代码。 */
  private static final List<String> HOST_ENV_ALLOWLIST = List.of("PATH", "LANG", "LC_ALL", "LC_CTYPE", "TZ", "TERM");

  private static final String DEFAULT_PATH = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin";

  public Object execute(String code, JSONObject params, String entrypoint, boolean lint) {
    if (code == null || code.isBlank() || code.getBytes(StandardCharsets.UTF_8).length > 65536) {
      throw new IllegalArgumentException("Python 代码不能为空且不能超过 64 KiB");
    }
    JSONObject request = JSONObject.of("code", code, "params", params == null ? new JSONObject() : params,
        "entrypoint", entrypoint, "mode", lint ? "lint" : "execute");
    byte[] input = request.toJSONString().getBytes(StandardCharsets.UTF_8);
    if (input.length > 131072) {
      throw new IllegalArgumentException("函数输入超过 128 KiB");
    }
    if (!SLOTS.tryAcquire()) {
      throw new IllegalStateException("Python 执行器繁忙，请稍后重试");
    }
    String name = "mosskb-python-" + UUID.randomUUID();
    Process process = null;
    try {
      if (hostRunner()) {
        preflight();
      }
      process = start(command(name));
      Process running = process;
      CompletableFuture<byte[]> output = CompletableFuture.supplyAsync(() -> readBounded(running.getInputStream(), 262144));
      CompletableFuture<byte[]> errors = CompletableFuture.supplyAsync(() -> readBounded(running.getErrorStream(), 8192));
      CompletableFuture<Void> writer = CompletableFuture.runAsync(() -> {
        try (var stdin = running.getOutputStream()) {
          stdin.write(input);
        } catch (java.io.IOException e) {
          throw new IllegalStateException("Python input write failed", e);
        }
      });
      int seconds = ContextBudget.setting("kb.python.timeout_seconds", 15, 1, 60);
      if (!process.waitFor(seconds, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Python 执行超时，进程已终止");
      }
      byte[] stdout = output.get(3, TimeUnit.SECONDS);
      writer.get(3, TimeUnit.SECONDS);
      errors.get(3, TimeUnit.SECONDS);
      if (process.exitValue() != 0) {
        throw new IllegalStateException(hostRunner() ? "宿主 Python 沙箱启动或执行失败，请检查 kb.python.sandbox.* 配置"
            : "Python 容器启动或执行失败，请检查 Docker 服务及隔离镜像");
      }
      JSONObject response = JSON.parseObject(new String(stdout, StandardCharsets.UTF_8));
      if (response == null || !response.getBooleanValue("ok")) {
        throw new IllegalStateException(response == null ? "Python 返回格式无效" : response.getString("error"));
      }
      return response.get("data");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Python 执行已中断", e);
    } catch (java.io.IOException e) {
      throw new IllegalStateException(engineUnavailableMessage(), e);
    } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
      throw new IllegalStateException("Python 输出无效或超过限制", e);
    } finally {
      try {
        if (process != null) {
          killTree(process);
          cleanup(name);
        }
      } finally {
        SLOTS.release();
      }
    }
  }

  protected List<String> command(String name) throws java.io.IOException {
    String runner = readRunner();
    if (hostRunner()) {
      return hostCommand(runner);
    }
    List<String> command = dockerClientCommand();
    command.addAll(List.of("run", "--rm", "--pull=never", "--name", name,
        "--network=none", "--read-only", "--cap-drop=ALL", "--security-opt=no-new-privileges", "--pids-limit=32",
        "--memory=256m", "--memory-swap=256m", "--cpus=1", "--user=65534:65534",
        "--tmpfs=/tmp:rw,noexec,nosuid,size=16m", "--workdir=/tmp", "--log-driver=none", "-i",
        EnvUtils.get("kb.python.image", "python:3.12-slim"), "python", "-I", "-B", "-c", bootstrap(runner)));
    return command;
  }

  /**
   * 宿主 runner 默认用 util-linux 的 runuser 切换用户：runuser 不带 setuid，argv 直接传递，
   * 既不需要 shell，也不需要额外的 JVM 启动参数。
   *
   * <p>默认不能用 su：{@code /bin/su} 是 setuid 程序，而 JDK 默认用 posix_spawn 启动进程，
   * 直接启动 setuid 程序会在 {@code ProcessBuilder.start()} 处卡死（实测，见 docs/agent-verification.md）。
   * 需要 su 时用 {@code kb.python.sandbox.su} 显式指定，并加
   * {@code -Djdk.lang.Process.launchMechanism=FORK}。
   */
  protected List<String> hostCommand(String runner) {
    String python = EnvUtils.get("kb.python.sandbox.python", "python3").trim();
    String su = configuredSu();
    if (su != null) {
      String inner = python + " -I -B -c \"" + bootstrap(runner) + "\"";
      return new ArrayList<>(List.of(su, "-s", "/bin/sh", "-c", inner, sandboxUser()));
    }
    String runuser = runuserExecutable();
    if (runuser == null) {
      throw new IllegalStateException(
          "宿主 Python runner 找不到 runuser(util-linux)；也可显式配置 kb.python.sandbox.su，但 su 是 setuid 程序，需要 -Djdk.lang.Process.launchMechanism=FORK");
    }
    return new ArrayList<>(
        List.of(runuser, "-u", sandboxUser(), "--", python, "-I", "-B", "-c", bootstrap(runner)));
  }

  /**
   * Windows 的 CreateProcess 会破坏参数里的双引号，而 runner 源码既有引号又有空格，
   * 直接作为 {@code -c} 传给 python 会被拆成多个参数并报语法错误。改成不含空格和双引号的
   * base64 引导脚本，各平台行为一致。请求 JSON 仍然通过 stdin 传入，协议不变。
   */
  protected String bootstrap(String runner) {
    String encoded = java.util.Base64.getEncoder().encodeToString(runner.getBytes(StandardCharsets.UTF_8));
    return "exec(__import__('base64').b64decode('" + encoded + "').decode())";
  }

  /**
   * Docker CLI prefix, then an optional remote engine address. The {@code --host} global flag must
   * precede the subcommand, so it is appended here rather than in {@link #command(String)}.
   */
  private List<String> dockerClientCommand() {
    List<String> command = new ArrayList<>();
    String distribution = EnvUtils.get("kb.python.wsl.distribution");
    if (distribution != null && !distribution.isBlank()) {
      command.addAll(List.of("wsl.exe", "--distribution", distribution, "--user", "root", "--exec", "docker"));
    } else {
      command.add(EnvUtils.get("kb.python.docker", "docker"));
    }
    String host = EnvUtils.get("kb.python.docker.host");
    if (host != null && !host.isBlank()) {
      command.add("--host");
      command.add(host.trim());
    }
    return command;
  }

  protected Process start(List<String> command) throws java.io.IOException {
    ProcessBuilder builder = new ProcessBuilder(command);
    if (hostRunner()) {
      applyHostEnvironment(builder);
      builder.directory(sandboxDir());
    } else {
      // The Docker CLI may use these host settings; none are passed into the container.
      builder.environment().keySet().removeIf(key -> !DOCKER_ENV_ALLOWLIST.contains(key.toUpperCase(java.util.Locale.ROOT)));
    }
    return builder.start();
  }

  private void applyHostEnvironment(ProcessBuilder builder) {
    Map<String, String> environment = builder.environment();
    environment.clear();
    environment.putAll(hostEnvironment());
  }

  /** 宿主 runner 传给子进程的完整环境：白名单之外一律不带，应用密钥不会进入用户代码。 */
  protected Map<String, String> hostEnvironment() {
    Map<String, String> inherited = System.getenv();
    Map<String, String> environment = new java.util.LinkedHashMap<>();
    for (String key : HOST_ENV_ALLOWLIST) {
      String value = inherited.get(key);
      if (value != null && !value.isBlank()) {
        environment.put(key, value);
      }
    }
    environment.putIfAbsent("PATH", DEFAULT_PATH);
    String user = sandboxUser();
    String dir = sandboxDir().getAbsolutePath();
    environment.put("HOME", dir);
    environment.put("TMPDIR", dir);
    environment.put("USER", user);
    environment.put("LOGNAME", user);
    environment.put("SHELL", "/bin/sh");
    return environment;
  }

  /**
   * 宿主 runner 的前置校验：任何一条不满足都拒绝执行，绝不退回以 Java 进程身份运行。
   * 拆成可覆盖方法，是为了让单元测试在没有 /etc/passwd 的平台上也能验证每条拒绝路径。
   */
  protected void preflight() {
    if (!posixHost()) {
      throw new IllegalStateException("宿主 Python runner 仅支持 Linux/类 Unix；Windows 请使用 kb.python.runner=docker");
    }
    String user = sandboxUser();
    long sandboxUid = passwdUid(user);
    if (sandboxUid < 0) {
      throw new IllegalStateException("宿主 Python runner 找不到沙箱用户：" + user + "，请先执行 scripts/setup-python-host-sandbox.sh");
    }
    if (sandboxUid == 0) {
      throw new IllegalStateException("宿主 Python runner 拒绝以 root 作为沙箱用户：" + user);
    }
    if (currentUid() != 0) {
      throw new IllegalStateException("宿主 Python runner 需要以 root 运行才能切到 " + user + "；否则请改用 kb.python.runner=docker");
    }
    File dir = sandboxDir();
    if (!dir.isDirectory()) {
      throw new IllegalStateException("宿主 Python runner 的沙箱目录不存在：" + dir.getAbsolutePath());
    }
    if (worldWritable(dir)) {
      throw new IllegalStateException("宿主 Python runner 的沙箱目录不能是所有人可写：" + dir.getAbsolutePath());
    }
    if (configuredSu() == null && runuserExecutable() == null) {
      throw new IllegalStateException(
          "宿主 Python runner 找不到 runuser(util-linux)；也可显式配置 kb.python.sandbox.su，但 su 是 setuid 程序，需要 -Djdk.lang.Process.launchMechanism=FORK");
    }
  }

  protected boolean hostRunner() {
    return "host".equalsIgnoreCase(EnvUtils.get("kb.python.runner", "docker").trim());
  }

  protected String sandboxUser() {
    return EnvUtils.get("kb.python.sandbox.user", "sandbox").trim();
  }

  protected File sandboxDir() {
    return new File(EnvUtils.get("kb.python.sandbox.dir", "/var/lib/java-mosskb/sandbox").trim());
  }

  protected boolean posixHost() {
    return !System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win");
  }

  protected long currentUid() {
    return passwdUid(ProcessHandle.current().info().user().orElse(""));
  }

  protected long passwdUid(String user) {
    if (user == null || user.isBlank()) {
      return -1;
    }
    try {
      for (String line : Files.readAllLines(Path.of("/etc/passwd"))) {
        String[] parts = line.split(":");
        if (parts.length >= 3 && parts[0].equals(user)) {
          return Long.parseLong(parts[2]);
        }
      }
    } catch (Exception ignored) {
      // 没有 /etc/passwd 的平台已经由 posixHost() 先行拒绝。
    }
    return -1;
  }

  protected boolean worldWritable(File dir) {
    try {
      return Files.getPosixFilePermissions(dir.toPath()).contains(java.nio.file.attribute.PosixFilePermission.OTHERS_WRITE);
    } catch (Exception ignored) {
      return false;
    }
  }

  /** 显式配置的 su；未配置时返回 null，走非 setuid 的 runuser。 */
  protected String configuredSu() {
    String configured = EnvUtils.get("kb.python.sandbox.su");
    return configured == null || configured.isBlank() ? null : configured.trim();
  }

  protected String runuserExecutable() {
    for (String candidate : List.of("/usr/sbin/runuser", "/sbin/runuser", "/usr/bin/runuser", "/bin/runuser")) {
      if (new File(candidate).canExecute()) {
        return candidate;
      }
    }
    return null;
  }

  private String engineUnavailableMessage() {
    return hostRunner() ? "宿主 Python 沙箱不可用，请检查 kb.python.sandbox.* 与提权命令；不会以当前用户执行代码"
        : "隔离 Python 执行器不可用，请先安装并启动 Docker；不会在宿主机执行代码";
  }

  /** 超时杀的是提权进程，用户代码是它的子进程，必须整棵树一起终止，否则会留下沙箱进程。 */
  private void killTree(Process process) {
    try {
      process.descendants().forEach(ProcessHandle::destroyForcibly);
    } catch (UnsupportedOperationException | IllegalStateException ignored) {
      // 进程已经结束，或测试替身没有 ProcessHandle，退回只终止直接子进程。
    }
    process.destroyForcibly();
  }

  private String readRunner() throws java.io.IOException {
    try (InputStream stream = getClass().getResourceAsStream("/python/runner.py")) {
      if (stream == null) {
        throw new IllegalStateException("Python runner resource missing");
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private byte[] readBounded(InputStream stream, int limit) {
    try (stream; java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
      byte[] buffer = new byte[4096];
      int count;
      while ((count = stream.read(buffer)) != -1) {
        if (out.size() + count > limit) {
          throw new IllegalStateException("Python output limit exceeded");
        }
        out.write(buffer, 0, count);
      }
      return out.toByteArray();
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Python output read failed", e);
    }
  }

  protected void cleanup(String name) {
    // 宿主 runner 没有容器需要回收；Docker runner 的清理失败由 --rm 兜底。
    if (hostRunner()) {
      return;
    }
    try {
      List<String> command = dockerClientCommand();
      command.addAll(List.of("rm", "-f", name));
      Process cleanup = start(command);
      if (!cleanup.waitFor(5, TimeUnit.SECONDS)) {
        cleanup.destroyForcibly();
      }
    } catch (Exception ignored) {
      // --rm remains a second cleanup mechanism if the daemon was temporarily unavailable.
    }
  }
}
