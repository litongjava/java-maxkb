package nexus.io.maxkb.service.kb;

import java.io.*;
import java.util.List;
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
    @Override protected Process start(List<String> command) { return process; }
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
    List<String> command = new IsolatedPythonExecutor().command("maxkb-python-test");
    for (String flag : List.of("--network=none", "--read-only", "--cap-drop=ALL", "--security-opt=no-new-privileges", "--user=65534:65534", "--pids-limit=32", "--memory=256m", "--pull=never")) {
      assertTrue(flag, command.contains(flag));
    }
    assertFalse(command.contains("--privileged")); assertFalse(command.contains("-v")); assertFalse(command.contains("--mount"));
    assertTrue(command.contains("-I"));
  }
  @Test public void unavailableDockerFailsClosedWithoutHostPythonFallback() {
    IsolatedPythonExecutor executor = new IsolatedPythonExecutor() {
      @Override protected Process start(List<String> command) throws IOException { throw new IOException("not installed"); }
    };
    try { executor.execute("def main(): return 1", null, null, false); fail("must fail closed"); }
    catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("不会在宿主机执行代码")); }
  }
  @Test public void oversizedCodeIsRejectedBeforeStartingAnyProcess() {
    try { new IsolatedPythonExecutor().execute("x".repeat(65537), null, null, false); fail("must reject"); }
    catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("64 KiB")); }
  }
  @Test public void originalMaxKbParameterTypesAreConverted() {
    assertEquals(12L, PythonFunctionService.convert("12", "int"));
    assertEquals(2, ((com.alibaba.fastjson2.JSONArray) PythonFunctionService.convert("[1,2]", "array")).size());
    assertNull(PythonFunctionService.convert(null, "string"));
  }
}
