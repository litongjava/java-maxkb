package nexus.io.mosskb.service.kb;

import java.io.*;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class IsolatedPythonExecutorTest {
  private static class ContainerProcess extends Process {
    String result = "{\"ok\":true,\"data\":5}";
    boolean completed = true;
    boolean destroyed;
    @Override public OutputStream getOutputStream() { return new ByteArrayOutputStream(); }
    @Override public InputStream getInputStream() { return new ByteArrayInputStream(result.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    @Override public InputStream getErrorStream() { return new ByteArrayInputStream(new byte[0]); }
    @Override public int waitFor() { return 0; }
    @Override public boolean waitFor(long time, java.util.concurrent.TimeUnit unit) { return completed; }
    @Override public int exitValue() { return 0; }
    @Override public void destroy() { destroyed = true; }
    @Override public Process destroyForcibly() { destroyed = true; return this; }
  }
  private static class Executor extends IsolatedPythonExecutor {
    ContainerProcess process = new ContainerProcess();
    boolean cleaned;
    final List<List<String>> started = new java.util.ArrayList<>();
    @Override protected Process start(List<String> command) { started.add(List.copyOf(command)); return process; }
    @Override protected void cleanup(String name) { cleaned = true; }
  }

  @Test public void successfulContainerResultIsDecodedAndCleaned() {
    Executor executor = new Executor();
    assertEquals(5, executor.execute("def main(): return 5", null, null, false));
    assertTrue(executor.cleaned);
  }
  @Test public void timeoutKillsClientAndCleansContainer() {
    Executor executor = new Executor(); executor.process.completed = false;
    try { executor.execute("def main(): pass", null, null, false); fail("expected timeout"); }
    catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("超时")); }
    assertTrue(executor.process.destroyed); assertTrue(executor.cleaned);
  }
  @Test public void oversizedOutputIsRejectedAndCleaned() {
    Executor executor = new Executor(); executor.process.result = "x".repeat(262145);
    try { executor.execute("def main(): pass", null, null, false); fail("expected limit"); }
    catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("输出")); }
    assertTrue(executor.cleaned);
  }
  @Test public void pythonErrorsAreReturnedAsErrorsInsteadOfSuccessfulNull() {
    Executor executor = new Executor(); executor.process.result = "{\"ok\":false,\"error\":\"division by zero\"}";
    try { executor.execute("def main(): return 1/0", null, null, false); fail("expected error"); }
    catch (IllegalStateException expected) { assertEquals("division by zero", expected.getMessage()); }
  }
  @Test public void sandboxCommandHasNoHostMountsAndEnforcesResourceAndPrivilegeLimits() throws Exception {
    List<String> command = new IsolatedPythonExecutor().command("mosskb-python-test");
    for (String flag : List.of("--network=none", "--read-only", "--cap-drop=ALL", "--security-opt=no-new-privileges", "--user=65534:65534", "--pids-limit=32", "--memory=256m", "--pull=never")) {
      assertTrue(flag, command.contains(flag));
    }
    assertFalse(command.contains("--privileged")); assertFalse(command.contains("-v")); assertFalse(command.contains("--mount"));
    assertTrue(command.contains("-I"));
  }
  @Test public void runnerIsPassedAsSpaceAndQuoteFreeBootstrap() throws Exception {
    List<String> command = new IsolatedPythonExecutor().command("mosskb-python-test");
    String bootstrap = command.get(command.indexOf("-c") + 1);
    assertFalse("含空格的参数会被 Windows 拆开", bootstrap.contains(" "));
    assertFalse("含双引号的参数会被 Windows 破坏", bootstrap.contains("\""));
    String marker = "b64decode('";
    String encoded = bootstrap.substring(bootstrap.indexOf(marker) + marker.length(), bootstrap.lastIndexOf('\''));
    String decoded = new String(java.util.Base64.getDecoder().decode(encoded), java.nio.charset.StandardCharsets.UTF_8);
    String source;
    try (InputStream stream = IsolatedPythonExecutor.class.getResourceAsStream("/python/runner.py")) {
      source = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }
    assertEquals(source, decoded);
    assertFalse(command.contains(source));
  }
  @Test public void remoteEngineHostIsPassedAsGlobalFlagBeforeRun() {
    String key = "kb.python.docker.host";
    String previous = System.getProperty(key);
    try {
      System.setProperty(key, "tcp://192.168.31.97:2375");
      Executor executor = new Executor();
      assertEquals(5, executor.execute("def main(): return 5", null, null, false));
      List<String> command = executor.started.get(0);
      int host = command.indexOf("--host");
      assertTrue("--host must be present", host > 0);
      assertTrue("--host must precede the run subcommand", host < command.indexOf("run"));
      assertEquals("tcp://192.168.31.97:2375", command.get(host + 1));
      assertTrue(command.contains("--network=none"));
    } finally {
      if (previous == null) {
        System.clearProperty(key);
      } else {
        System.setProperty(key, previous);
      }
    }
  }
  @Test public void unavailableDockerFailsClosedWithoutHostPythonFallback() {
    IsolatedPythonExecutor executor = new IsolatedPythonExecutor() {
      @Override protected Process start(List<String> command) throws IOException { throw new IOException("not installed"); }
    };
    try { executor.execute("def main(): return 1", null, null, false); fail("must fail closed"); }
    catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("不会在宿主机执行代码")); }
  }

  /** 宿主 runner 的拒绝路径可以在任意平台验证：真实 /etc/passwd 与 uid 由测试替身提供。 */
  private static class HostExecutor extends IsolatedPythonExecutor {
    boolean posix = true;
    long sandboxUid = 1001;
    long processUid = 0;
    boolean dirExists = true;
    boolean dirWorldWritable;
    String runuser = "/usr/sbin/runuser";
    @Override protected boolean posixHost() { return posix; }
    @Override protected long passwdUid(String user) { return sandboxUid; }
    @Override protected long currentUid() { return processUid; }
    @Override protected File sandboxDir() { return new File(dirExists ? System.getProperty("java.io.tmpdir") : "/nonexistent/mosskb-sandbox"); }
    @Override protected boolean worldWritable(File dir) { return dirWorldWritable; }
    @Override protected String runuserExecutable() { return runuser; }
    @Override protected Process start(List<String> command) { throw new IllegalStateException("must not start a process"); }
  }

  private static void withHostRunner(Runnable block) {
    String previous = System.getProperty("kb.python.runner");
    try {
      System.setProperty("kb.python.runner", "host");
      block.run();
    } finally {
      if (previous == null) {
        System.clearProperty("kb.python.runner");
      } else {
        System.setProperty("kb.python.runner", previous);
      }
    }
  }

  private static String refusal(HostExecutor executor) {
    try {
      executor.execute("def main(): return 1", null, null, false);
      fail("host runner must fail closed");
      return null;
    } catch (IllegalStateException expected) {
      return expected.getMessage();
    }
  }

  private static String runnerSource() throws IOException {
    try (InputStream stream = IsolatedPythonExecutor.class.getResourceAsStream("/python/runner.py")) {
      return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    }
  }

  /** 从 {@code b64decode('...')} 里取出 base64 并解码，验证引导脚本就是 runner.py。 */
  private static String decodeBootstrap(String text) {
    String marker = "b64decode('";
    int start = text.indexOf(marker) + marker.length();
    String encoded = text.substring(start, text.indexOf('\'', start));
    return new String(java.util.Base64.getDecoder().decode(encoded), java.nio.charset.StandardCharsets.UTF_8);
  }

  @Test public void hostRunnerUsesRunuserWithoutShellOrSetuidBinary() throws Exception {
    String source = runnerSource();
    withHostRunner(() -> {
      HostExecutor executor = new HostExecutor();
      List<String> command;
      try {
        command = executor.command("mosskb-python-test");
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
      assertEquals("/usr/sbin/runuser", command.get(0));
      assertEquals("-u", command.get(1));
      assertEquals("sandbox", command.get(2));
      assertEquals("--", command.get(3));
      assertEquals("python3", command.get(4));
      assertEquals("-I", command.get(5));
      assertEquals("-B", command.get(6));
      assertEquals("-c", command.get(7));
      assertEquals(9, command.size());
      assertEquals(source, decodeBootstrap(command.get(8)));
      // runuser 直接传 argv：既没有 shell，也不会去启动 setuid 的 su。
      assertFalse(command.contains("/bin/sh"));
      assertFalse(command.stream().anyMatch(part -> part.endsWith("/su") || part.equals("su")));
    });
  }
  @Test public void hostRunnerWithConfiguredSuUsesShellFormKeptSafe() throws Exception {
    String source = runnerSource();
    String previous = System.getProperty("kb.python.sandbox.su");
    try {
      System.setProperty("kb.python.sandbox.su", "/bin/su");
      withHostRunner(() -> {
        HostExecutor executor = new HostExecutor();
        List<String> command;
        try {
          command = executor.command("mosskb-python-test");
        } catch (IOException e) {
          throw new IllegalStateException(e);
        }
        assertEquals("/bin/su", command.get(0));
        assertEquals("-s", command.get(1));
        assertEquals("/bin/sh", command.get(2));
        assertEquals("-c", command.get(3));
        assertEquals("sandbox", command.get(5));
        assertEquals(6, command.size());
        String inner = command.get(4);
        assertTrue(inner.startsWith("python3 -I -B -c \""));
        assertTrue(inner.endsWith("\""));
        // 双引号包住的引导脚本必须不含会被 shell 改写或提前闭合的字符。
        assertFalse(inner.contains("$"));
        assertFalse(inner.contains("`"));
        assertFalse(inner.contains("\\"));
        assertFalse(inner.contains("\"\""));
        assertEquals(source, decodeBootstrap(inner));
      });
    } finally {
      if (previous == null) {
        System.clearProperty("kb.python.sandbox.su");
      } else {
        System.setProperty("kb.python.sandbox.su", previous);
      }
    }
  }
  @Test public void hostRunnerWithoutRunuserOrSuFailsClosedWithGuidance() {
    withHostRunner(() -> {
      HostExecutor executor = new HostExecutor();
      executor.runuser = null;
      String message = refusal(executor);
      assertTrue(message.contains("找不到 runuser"));
      assertTrue(message.contains("launchMechanism=FORK"));
    });
  }
  @Test public void hostRunnerIsRefusedOnWindows() {
    withHostRunner(() -> {
      HostExecutor executor = new HostExecutor();
      executor.posix = false;
      assertTrue(refusal(executor).contains("仅支持 Linux"));
    });
  }
  @Test public void hostRunnerRejectsMissingAndRootSandboxUser() {
    withHostRunner(() -> {
      HostExecutor missing = new HostExecutor();
      missing.sandboxUid = -1;
      assertTrue(refusal(missing).contains("找不到沙箱用户"));
      HostExecutor root = new HostExecutor();
      root.sandboxUid = 0;
      assertTrue(refusal(root).contains("拒绝以 root 作为沙箱用户"));
    });
  }
  @Test public void hostRunnerRejectsNonRootJavaProcess() {
    withHostRunner(() -> {
      HostExecutor executor = new HostExecutor();
      executor.processUid = 1000;
      assertTrue(refusal(executor).contains("需要以 root 运行"));
    });
  }
  @Test public void hostRunnerRejectsMissingOrWorldWritableSandboxDirectory() {
    withHostRunner(() -> {
      HostExecutor missing = new HostExecutor();
      missing.dirExists = false;
      assertTrue(refusal(missing).contains("沙箱目录不存在"));
      HostExecutor writable = new HostExecutor();
      writable.dirWorldWritable = true;
      assertTrue(refusal(writable).contains("不能是所有人可写"));
    });
  }
  @Test public void hostRunnerWithoutPrivilegeDropBinaryFailsClosedWithHostMessage() {
    withHostRunner(() -> {
      IsolatedPythonExecutor executor = new IsolatedPythonExecutor() {
        @Override protected void preflight() {
        }
        @Override protected String runuserExecutable() {
          return "/usr/sbin/runuser";
        }
        @Override protected Process start(List<String> command) throws IOException { throw new IOException("no runuser"); }
      };
      try { executor.execute("def main(): return 1", null, null, false); fail("must fail closed"); }
      catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("不会以当前用户执行代码")); }
    });
  }
  @Test public void hostEnvironmentDropsEverythingOutsideTheAllowlist() {
    withHostRunner(() -> {
      Map<String, String> environment = new IsolatedPythonExecutor().hostEnvironment();
      List<String> intentional = List.of("PATH", "LANG", "LC_ALL", "LC_CTYPE", "TZ", "TERM", "HOME", "TMPDIR", "USER",
          "LOGNAME", "SHELL");
      for (String key : System.getenv().keySet()) {
        if (!intentional.contains(key.toUpperCase(java.util.Locale.ROOT))) {
          assertFalse("父进程变量泄漏进用户代码: " + key, environment.containsKey(key));
        }
      }
      assertEquals("sandbox", environment.get("USER"));
      assertEquals("sandbox", environment.get("LOGNAME"));
      assertEquals("/bin/sh", environment.get("SHELL"));
      assertFalse(environment.containsKey("GITEE_API_KEY"));
      assertNotNull(environment.get("PATH"));
      assertNotNull(environment.get("HOME"));
      assertNotNull(environment.get("TMPDIR"));
    });
  }
  @Test public void oversizedCodeIsRejectedBeforeStartingAnyProcess() {
    try { new IsolatedPythonExecutor().execute("x".repeat(65537), null, null, false); fail("must reject"); }
    catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("64 KiB")); }
  }
  @Test public void originalMossKbParameterTypesAreConverted() {
    assertEquals(12L, PythonFunctionService.convert("12", "int"));
    assertEquals(2, ((com.alibaba.fastjson2.JSONArray) PythonFunctionService.convert("[1,2]", "array")).size());
    assertNull(PythonFunctionService.convert(null, "string"));
  }
}
