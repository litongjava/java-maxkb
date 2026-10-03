package nexus.io.maxkb.service.kb;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import nexus.io.tio.utils.environment.EnvUtils;

/** Never falls back to executing user code on the Java host. */
public class IsolatedPythonExecutor {
  private static final Semaphore SLOTS = new Semaphore(2);

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
    String name = "maxkb-python-" + UUID.randomUUID();
    Process process = null;
    try {
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
        throw new IllegalStateException("Python 执行超时，容器已终止");
      }
      byte[] stdout = output.get(3, TimeUnit.SECONDS);
      writer.get(3, TimeUnit.SECONDS);
      errors.get(3, TimeUnit.SECONDS);
      if (process.exitValue() != 0) {
        throw new IllegalStateException("Python 容器启动或执行失败，请检查 Docker 服务及隔离镜像");
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
      throw new IllegalStateException("隔离 Python 执行器不可用，请先安装并启动 Docker；不会在宿主机执行代码", e);
    } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
      throw new IllegalStateException("Python 输出无效或超过限制", e);
    } finally {
      if (process != null) {
        process.destroyForcibly();
        cleanup(name);
      }
      SLOTS.release();
    }
  }

  protected List<String> command(String name) throws java.io.IOException {
    String runner;
    try (InputStream stream = getClass().getResourceAsStream("/python/runner.py")) {
      if (stream == null) {
        throw new IllegalStateException("Python runner resource missing");
      }
      runner = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
    List<String> command = dockerCommand();
    command.addAll(List.of("run", "--rm", "--pull=never", "--name", name,
        "--network=none", "--read-only", "--cap-drop=ALL", "--security-opt=no-new-privileges", "--pids-limit=32",
        "--memory=256m", "--memory-swap=256m", "--cpus=1", "--user=65534:65534",
        "--tmpfs=/tmp:rw,noexec,nosuid,size=16m", "--workdir=/tmp", "--log-driver=none", "-i",
        EnvUtils.get("kb.python.image", "python:3.12-slim"), "python", "-I", "-B", "-c", runner));
    return command;
  }

  private List<String> dockerCommand() {
    String distribution = EnvUtils.get("kb.python.wsl.distribution");
    if (distribution != null && !distribution.isBlank()) {
      return new ArrayList<>(List.of("wsl.exe", "--distribution", distribution, "--user", "root", "--exec", "docker"));
    }
    return new ArrayList<>(List.of(EnvUtils.get("kb.python.docker", "docker")));
  }

  protected Process start(List<String> command) throws java.io.IOException {
    ProcessBuilder builder = new ProcessBuilder(command);
    // The Docker CLI may use these host settings; none are passed into the container.
    builder.environment().keySet().removeIf(key -> !List.of("PATH", "SYSTEMROOT", "WINDIR", "TEMP", "TMP", "HOME", "USERPROFILE", "DOCKER_HOST", "DOCKER_CONTEXT", "DOCKER_CONFIG", "DOCKER_TLS_VERIFY", "DOCKER_CERT_PATH").contains(key.toUpperCase(java.util.Locale.ROOT)));
    return builder.start();
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
    try {
      List<String> command = dockerCommand();
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
