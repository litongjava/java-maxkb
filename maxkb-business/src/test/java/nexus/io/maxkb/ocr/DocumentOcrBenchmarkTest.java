package nexus.io.maxkb.ocr;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;
import org.junit.Assume;
import org.junit.Test;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;

import nexus.io.chat.ChatModelResponse;
import nexus.io.deepseek.DeepSeekClient;
import nexus.io.gitee.GiteeClient;
import nexus.io.gitee.GiteeConst;
import nexus.io.gitee.GiteeDocumentOutput;
import nexus.io.gitee.GiteeDocumentParseRequest;
import nexus.io.gitee.GiteeModels;
import nexus.io.gitee.GiteePromptConst;
import nexus.io.gitee.GiteeSimpleMarkdownUtils;
import nexus.io.gitee.GiteeTaskResponse;
import nexus.io.tio.utils.environment.EnvUtils;
import nexus.io.tio.utils.snowflake.SnowflakeIdUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** Opt-in, paid integration benchmark. Never uploads documents in ordinary unit tests. */
public class DocumentOcrBenchmarkTest {
  private static final List<String> MODELS = Arrays.asList(
      GiteeModels.MINERU2_5_PRO, GiteeModels.UNLIMITED_OCR, GiteeModels.PADDLEOCR_VL_1_5,
      GiteeModels.HUNYUAN_OCR, GiteeModels.DEEPSEEK_OCR, GiteeModels.MINERU2_5,
      GiteeModels.PDF_EXTRACT_KIT_1_0);
  private final ThreadLocal<Path> responseFile = new ThreadLocal<>();
  private final List<JSONObject> results = new ArrayList<>();
  private Path output;
  private String giteeKey;
  private String deepseekKey;

  @Test
  public void compareDocumentModels() throws Exception {
    Assume.assumeTrue("Use -Docr.benchmark=true to enable paid API calls",
        Boolean.parseBoolean(System.getProperty("ocr.benchmark", "false")));
    run();
  }

  public static void main(String[] args) throws Exception {
    if (!Boolean.parseBoolean(System.getProperty("ocr.benchmark", "false"))) {
      throw new IllegalArgumentException("Explicit -Docr.benchmark=true is required");
    }
    new DocumentOcrBenchmarkTest().run();
  }

  private void run() throws Exception {
    EnvUtils.load();
    giteeKey = EnvUtils.get("GITEE_API_KEY");
    deepseekKey = EnvUtils.get("DEEPSEEK_API_KEY");
    if (giteeKey == null || giteeKey.isBlank()) {
      throw new IllegalStateException("GITEE_API_KEY is required");
    }
    String source = System.getProperty("ocr.pdf");
    if (source == null) {
      throw new IllegalArgumentException("-Docr.pdf=<PDF path> is required");
    }
    File pdf = Path.of(source).toFile();
    if (!pdf.isFile()) {
      throw new IllegalArgumentException("Input PDF does not exist");
    }
    output = Path.of(System.getProperty("ocr.output", "target/ocr-benchmark/run-" + SnowflakeIdUtils.id()));
    Files.createDirectories(output);
    JSONObject manifest = new JSONObject();
    manifest.put("startedAt", Instant.now().toString());
    manifest.put("sourceFilename", pdf.getName());
    manifest.put("sourceBytes", pdf.length());
    manifest.put("sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(pdf.toPath()))));
    try (PDDocument doc = PDDocument.load(pdf)) {
      manifest.put("sourcePages", doc.getNumberOfPages());
    }
    manifest.put("failoverEnabled", false);
    manifest.put("models", MODELS);
    manifest.put("requestPolicy", "Full original PDF, no end_pages limit, include_image=false, include_image_base64=false; DeepSeek-OCR uses its documented grounding prompt; other models use provider defaults.");
    Path manifestPath = output.resolve("manifest.json");
    if (Files.exists(manifestPath)) {
      JSONObject original = JSON.parseObject(Files.readString(manifestPath));
      if (!manifest.getString("sha256").equals(original.getString("sha256"))) {
        throw new IllegalArgumentException("Output directory belongs to a different PDF");
      }
    } else {
      writeJson(manifestPath, manifest);
    }

    OkHttpClient http = new OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS).readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS).callTimeout(240, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .addInterceptor(chain -> {
          // Prevent provider fallback from silently changing the model being measured.
          Request request = chain.request().newBuilder().header("X-Failover-Enabled", "false").build();
          Response response = chain.proceed(request);
          Path capture = responseFile.get();
          if (capture != null) {
            Files.writeString(capture, redact(response.peekBody(64L * 1024 * 1024).string()), StandardCharsets.UTF_8);
          }
          return response;
        }).build();
    GiteeClient client = new GiteeClient(giteeKey, GiteeConst.BASE_URL, http);
    checkDeepSeekOfficial();

    String only = System.getProperty("ocr.models", "");
    List<String> selected = only.isBlank() ? MODELS : Arrays.asList(only.split(","));
    if (!MODELS.containsAll(selected)) {
      throw new IllegalArgumentException("Unknown model in ocr.models");
    }
    for (String model : selected) {
      if (GiteeModels.HUNYUAN_OCR.equals(model)) {
        runHunyuan(client, pdf);
        continue;
      }
      Path dir = output.resolve(model);
      Files.createDirectories(dir);
      Path statePath = dir.resolve("state.json");
      JSONObject state;
      if (Files.exists(statePath)) {
        state = JSON.parseObject(Files.readString(statePath));
        System.out.println(model + " resume: " + state.getString("status"));
      } else {
        state = new JSONObject();
        state.put("model", model);
        state.put("submittedAtEpochMs", System.currentTimeMillis());
        GiteeDocumentParseRequest request = new GiteeDocumentParseRequest();
        request.setModel(model);
        request.setInclude_image(false);
        request.setInclude_image_base64(false);
        if (GiteeModels.DEEPSEEK_OCR.equals(model)) {
          request.setPrompt(GiteePromptConst.pdf_to_markdown_prompt);
        }
        writeJson(dir.resolve("request.json"), request);
        try {
          responseFile.set(dir.resolve("submit.raw.json"));
          GiteeTaskResponse task = client.parseDocument(pdf, request);
          state.put("taskId", task.getTask_id());
          state.put("status", task.getStatus());
          state.put("submissionMs", System.currentTimeMillis() - state.getLongValue("submittedAtEpochMs"));
          if (task.getTask_id() == null || task.getTask_id().isBlank()) {
            state.put("status", "submission_failed");
            state.put("error", "Provider did not return a task ID; inspect submit.raw.json");
          }
          System.out.println(model + " submitted: " + task.getStatus());
        } catch (Exception e) {
          state.put("status", "submission_failed");
          state.put("error", redact(e.toString()));
          System.out.println(model + " submission failed: " + redact(e.toString()));
        }
      }
      results.add(state);
      writeJson(statePath, state);
    }
    long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(Long.parseLong(System.getProperty("ocr.timeoutMinutes", "45")));
    while (System.nanoTime() < deadline) {
      boolean pending = false;
      for (JSONObject state : results) {
        if (terminal(state.getString("status"))) {
          continue;
        }
        pending = true;
        Path dir = output.resolve(state.getString("model"));
        try {
          responseFile.set(dir.resolve("task.raw.json"));
          GiteeTaskResponse task = client.getTask(state.getString("taskId"));
          String previous = state.getString("status");
          state.put("status", task.getStatus());
          state.put("lastPolledAt", Instant.now().toString());
          if (!String.valueOf(previous).equals(task.getStatus())) {
            System.out.println(state.getString("model") + ": " + task.getStatus());
          }
          if (terminal(task.getStatus())) {
            state.put("observedTotalMs", System.currentTimeMillis() - state.getLongValue("submittedAtEpochMs"));
            state.put("providerCreatedAt", task.getCreated_at());
            state.put("providerStartedAt", task.getStarted_at());
            state.put("providerCompletedAt", task.getCompleted_at());
            writeJson(dir.resolve("task.typed.json"), task);
            GiteeDocumentOutput doc = task.getOutput();
            String markdown = GiteeSimpleMarkdownUtils.toMarkdown(doc);
            if (markdown.isBlank() && doc != null) {
              for (String candidate : Arrays.asList(doc.getText_result(), doc.getContent(), doc.getText())) {
                if (candidate != null && !candidate.isBlank()) {
                  markdown = candidate;
                  break;
                }
              }
            }
            Files.writeString(dir.resolve("result.md"), markdown, StandardCharsets.UTF_8);
            state.put("markdownChars", markdown.length());
            state.put("pagesReturned", doc != null && doc.getPages() != null ? doc.getPages().size() : null);
            state.put("segmentsReturned", doc != null && doc.getSegments() != null ? doc.getSegments().size() : null);
            System.out.println(state.getString("model") + " final chars=" + markdown.length());
          }
          writeJson(dir.resolve("state.json"), state);
        } catch (Exception e) {
          state.put("lastPollError", redact(e.toString()));
          writeJson(dir.resolve("state.json"), state);
          System.out.println(state.getString("model") + " polling error: " + redact(e.toString()));
        }
      }
      writeJson(output.resolve("summary.json"), results);
      if (!pending) {
        return;
      }
      Thread.sleep(15000);
    }
    // Keep task IDs for resuming instead of submitting and charging again.
    writeJson(output.resolve("summary.json"), results);
    throw new IllegalStateException("Polling deadline reached; rerun with the same ocr.output to resume existing tasks");
  }

  private void checkDeepSeekOfficial() throws Exception {
    JSONObject state = new JSONObject();
    state.put("requestedModel", "deepseek-ocr");
    state.put("endpoint", "https://api.deepseek.com/v1/models");
    if (deepseekKey == null || deepseekKey.isBlank()) {
      state.put("status", "missing_api_key");
    } else {
      try {
        ChatModelResponse models = DeepSeekClient.getModels();
        writeJson(output.resolve("deepseek-official-models.json"), models);
        List<String> ids = new ArrayList<>();
        if (models.getData() != null) {
          models.getData().forEach(model -> ids.add(model.getId()));
        }
        state.put("availableModels", ids);
        boolean hasOcr = ids.stream().anyMatch(id -> id.toLowerCase(Locale.ROOT).contains("ocr"));
        state.put("status", hasOcr ? "ocr_model_discovered_requires_documented_protocol" : "ocr_not_in_official_model_list");
        System.out.println("DeepSeek official models: " + ids);
      } catch (Exception e) {
        state.put("status", "discovery_failed");
        state.put("error", redact(e.toString()));
        System.out.println("DeepSeek discovery failed: " + redact(e.toString()));
      }
    }
    writeJson(output.resolve("deepseek-official-status.json"), state);
  }

  private void runHunyuan(GiteeClient client, File pdf) throws Exception {
    Path dir = output.resolve(GiteeModels.HUNYUAN_OCR);
    Files.createDirectories(dir);
    JSONObject state = new JSONObject();
    state.put("model", GiteeModels.HUNYUAN_OCR);
    state.put("endpoint", "/v1/images/ocr");
    state.put("renderDpi", 200);
    state.put("startedAt", Instant.now().toString());
    long start = System.currentTimeMillis();
    int failures = 0;
    StringBuilder markdown = new StringBuilder();
    try (PDDocument doc = PDDocument.load(pdf)) {
      PDFRenderer renderer = new PDFRenderer(doc);
      for (int i = 0; i < doc.getNumberOfPages(); i++) {
        String name = String.format("page-%02d", i + 1);
        Path textFile = dir.resolve(name + ".md");
        Path pageStateFile = dir.resolve(name + ".state.json");
        Path rawFile = dir.resolve(name + ".raw.json");
        // Reuse successful provider responses after a local decoding failure.
        if (!Files.exists(textFile) && Files.exists(rawFile)) {
          JSONObject saved = JSON.parseObject(Files.readString(rawFile));
          String savedText = saved.getString("text_result");
          if (savedText == null) {
            savedText = saved.getString("text");
          }
          if (savedText != null && !savedText.isBlank()) {
            Files.writeString(textFile, savedText, StandardCharsets.UTF_8);
            JSONObject recovered = Files.exists(pageStateFile) ? JSON.parseObject(Files.readString(pageStateFile)) : new JSONObject();
            recovered.put("status", "success");
            recovered.put("recoveredFromRawResponse", true);
            recovered.remove("error");
            writeJson(pageStateFile, recovered);
          }
        }
        if (!Files.exists(textFile)) {
          Path png = dir.resolve(name + ".png");
          if (!Files.exists(png)) {
            java.awt.image.BufferedImage image = renderer.renderImageWithDPI(i, 200, ImageType.RGB);
            ImageIO.write(image, "png", png.toFile());
            image.flush();
          }
          long pageStart = System.currentTimeMillis();
          JSONObject pageState = new JSONObject();
          pageState.put("physicalPage", i + 1);
          // Retry only a page explicitly rejected for exceeding the image size.
          if (Files.exists(rawFile) && Files.readString(rawFile).contains("maximum allowed size is 2048x2048")) {
            Files.copy(rawFile, dir.resolve(name + ".size-rejection.json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            java.awt.image.BufferedImage original = ImageIO.read(png.toFile());
            double scale = Math.min(1.0, 2048.0 / Math.max(original.getWidth(), original.getHeight()));
            java.awt.image.BufferedImage resized = new java.awt.image.BufferedImage(
                (int) Math.round(original.getWidth() * scale), (int) Math.round(original.getHeight() * scale),
                java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D graphics = resized.createGraphics();
            graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(original, 0, 0, resized.getWidth(), resized.getHeight(), null);
            graphics.dispose();
            png = dir.resolve(name + "-max2048.png");
            ImageIO.write(resized, "png", png.toFile());
            pageState.put("resizedAfterProviderRejection", true);
            pageState.put("uploadedWidth", resized.getWidth());
            pageState.put("uploadedHeight", resized.getHeight());
            original.flush();
            resized.flush();
          }
          try {
            responseFile.set(dir.resolve(name + ".raw.json"));
            String text = client.ocr(png.toFile(), GiteeModels.HUNYUAN_OCR).getText();
            if (text == null || text.isBlank()) {
              throw new IllegalStateException("OCR response has no text");
            }
            Files.writeString(textFile, text, StandardCharsets.UTF_8);
            pageState.put("status", "success");
          } catch (Exception e) {
            failures++;
            pageState.put("status", "failed");
            pageState.put("error", redact(e.toString()));
          }
          pageState.put("elapsedMs", System.currentTimeMillis() - pageStart);
          writeJson(pageStateFile, pageState);
        }
        if (Files.exists(textFile)) {
          markdown.append("> Page ").append(i + 1).append("\n\n").append(Files.readString(textFile)).append("\n\n");
        }
        System.out.println("HunyuanOCR page " + (i + 1) + "/" + doc.getNumberOfPages() + ": " + (Files.exists(textFile) ? "success" : "failed"));
        Files.writeString(dir.resolve("result.md"), markdown.toString(), StandardCharsets.UTF_8);
      }
      state.put("sourcePages", doc.getNumberOfPages());
      state.put("pagesReturned", doc.getNumberOfPages() - failures);
    }
    state.put("currentInvocationMs", System.currentTimeMillis() - start);
    long requestElapsedMs = 0;
    try (java.util.stream.Stream<Path> files = Files.list(dir)) {
      for (Path path : files.filter(p -> p.getFileName().toString().matches("page-\\d+\\.state\\.json")).toList()) {
        requestElapsedMs += JSON.parseObject(Files.readString(path)).getLongValue("elapsedMs");
      }
    }
    state.put("requestElapsedMsSum", requestElapsedMs);
    state.put("markdownChars", markdown.length());
    state.put("failedPages", failures);
    state.put("status", failures == 0 ? "success" : "partial_failure");
    writeJson(dir.resolve("state.json"), state);
    results.add(state);
  }

  private boolean terminal(String status) {
    return status != null && Arrays.asList("success", "succeeded", "completed", "failed", "failure", "partial_failure", "cancelled", "canceled", "submission_failed")
        .contains(status.toLowerCase(Locale.ROOT));
  }

  private String redact(String value) {
    for (String key : Arrays.asList(giteeKey, deepseekKey)) {
      if (key != null && !key.isBlank()) {
        value = value.replace(key, "[REDACTED]");
      }
    }
    return value;
  }

  private void writeJson(Path path, Object value) throws java.io.IOException {
    Files.writeString(path, redact(JSON.toJSONString(value, JSONWriter.Feature.PrettyFormat)), StandardCharsets.UTF_8);
  }
}
