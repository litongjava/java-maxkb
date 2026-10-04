package com.litongjava.mosskb.regression;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

/**
 * 品牌重命名回归测试。
 *
 * <p>项目改名之后，源码、配置、脚本和文档里不应该再出现旧品牌字样，只有产品要求保持不变的用户
 * 手册链接例外。旧品牌词根和例外链接都用片段拼出来，免得这个测试文件自己命中规则。
 */
public class BrandingRegressionTest {

  /** 旧品牌词根：ma + x + 可选分隔符 + kb，大小写不敏感，覆盖 kebab、下划线与驼峰写法。 */
  private static final Pattern LEGACY = Pattern.compile("ma" + "x" + "[_-]?" + "kb", Pattern.CASE_INSENSITIVE);

  /** 例外：指向上游的用户手册链接，产品要求保持原样。 */
  private static final List<String> ALLOWED = Arrays.asList(
      "ma" + "xkb" + ".cn/docs/",
      "docs." + "ma" + "xkb" + ".hk/");

  /** 生成物与依赖目录不参与扫描，它们要么是构建输出，要么不由本仓库维护。 */
  private static final Set<String> SKIP_DIRS = new HashSet<>(Arrays.asList(
      ".git", "node_modules", "target", "dist", "logs", "output", ".local", ".idea"));

  @Test
  public void repositoryHasNoLegacyBrandTokens() throws IOException {
    Path repoRoot = repositoryRoot();
    List<Path> files = new ArrayList<>();

    Files.walkFileTree(repoRoot, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        if (!dir.equals(repoRoot) && SKIP_DIRS.contains(dir.getFileName().toString())) {
          return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        files.add(file);
        return FileVisitResult.CONTINUE;
      }
    });

    List<String> violations = new ArrayList<>();
    int scanned = 0;
    for (Path file : files) {
      String text = readTextOrNull(file);
      if (text == null) {
        continue;
      }
      scanned++;
      String relative = repoRoot.relativize(file).toString();
      int lineNo = 0;
      for (String line : text.split("\n", -1)) {
        lineNo++;
        String cleaned = line;
        for (String allowed : ALLOWED) {
          cleaned = cleaned.replace(allowed, "");
        }
        Matcher matcher = LEGACY.matcher(cleaned);
        while (matcher.find()) {
          violations.add(relative + ":" + lineNo + " 命中 " + matcher.group() + " -> " + line.trim());
        }
      }
    }

    assertTrue("只扫到 " + scanned + " 个文本文件，仓库根目录可能判断错了：" + repoRoot, scanned > 200);
    assertTrue("仍然存在旧品牌字样（用户手册链接除外），共 " + violations.size() + " 处：\n"
        + String.join("\n", violations), violations.isEmpty());
  }

  /** 测试由 surefire 在 mosskb-web 目录下运行，向上找到同时含两个模块的目录即仓库根。 */
  static Path repositoryRoot() {
    Path dir = Paths.get("").toAbsolutePath();
    while (dir != null) {
      if (Files.isDirectory(dir.resolve("mosskb-business")) && Files.isDirectory(dir.resolve("mosskb-web"))) {
        return dir;
      }
      dir = dir.getParent();
    }
    throw new IllegalStateException("找不到后端仓库根目录，当前工作目录是 " + Paths.get("").toAbsolutePath());
  }

  /** 读取文本文件；二进制返回 null。UTF-16 是 PowerShell 5.1 重定向日志的默认编码。 */
  private static String readTextOrNull(Path file) throws IOException {
    byte[] bytes = Files.readAllBytes(file);
    if (bytes.length == 0) {
      return null;
    }
    boolean utf16le = bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE;
    boolean utf16be = bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF;
    if (utf16le) {
      return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
    }
    if (utf16be) {
      return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
    }
    int limit = Math.min(bytes.length, 8192);
    for (int i = 0; i < limit; i++) {
      if (bytes[i] == 0) {
        return null;
      }
    }
    String text = new String(bytes, StandardCharsets.UTF_8);
    return text.startsWith("\uFEFF") ? text.substring(1) : text;
  }
}
