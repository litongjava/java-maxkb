package nexus.io.maxkb.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import nexus.io.model.upload.UploadFile;
import nexus.io.tio.utils.environment.EnvUtils;

/**
 * 函数库图标落盘。
 *
 * <p>文件写进静态资源目录，库里只存相对路径；读取走 {@code /api/function_lib/icon/...}，
 * 这样静态目录怎么部署都不影响已经保存的图标。
 */
public class FunctionIconService {

  /** 图标在静态目录下的子路径，文件名和接口路径都用它。 */
  static final String ICON_DIR = "ui/fx/upload";

  /** 静态资源根目录，与 server.resources.static-locations 保持一致。 */
  private static final String STATIC_LOCATION = location();

  /** 只接受常见位图格式，SVG 可能带脚本，不放进图标目录。 */
  private static final List<String> ALLOWED_SUFFIX = List.of("png", "jpg", "jpeg", "gif", "webp", "bmp");

  /** 单张图标的上限，和前端 10 MB 的提示保持一致。 */
  private static final int MAX_ICON_BYTES = 10 * 1024 * 1024;

  /** 文件名规则：随机十六进制串加白名单后缀，读取时用它挡住目录穿越。 */
  private static final String FILE_NAME = "[0-9a-f]{32}\\.(png|jpg|jpeg|gif|webp|bmp)";

  /**
   * 保存上传的图标并返回可访问路径。
   *
   * @return 形如 {@code /api/function_lib/icon/<文件名>} 的路径
   */
  public String save(UploadFile file) {
    if (file == null || file.getData() == null || file.getData().length == 0) {
      throw new IllegalArgumentException("请选择要上传的图片");
    }
    if (file.getData().length > MAX_ICON_BYTES) {
      throw new IllegalArgumentException("图标不能超过 10 MB");
    }
    String suffix = suffix(file.getName());
    if (!ALLOWED_SUFFIX.contains(suffix)) {
      throw new IllegalArgumentException("图标只支持 png、jpg、gif、webp 格式");
    }
    String name = UUID.randomUUID().toString().replace("-", "") + "." + suffix;
    try {
      Path directory = Paths.get(STATIC_LOCATION, ICON_DIR);
      Files.createDirectories(directory);
      Files.write(directory.resolve(name), file.getData());
    } catch (IOException e) {
      throw new IllegalStateException("图标保存失败：" + e.getMessage(), e);
    }
    return "/api/function_lib/icon/" + name;
  }

  /**
   * 读取图标内容。
   *
   * @return 文件不存在或名称不合法时返回 null
   */
  public byte[] read(String name) {
    if (name == null || !name.matches(FILE_NAME)) {
      return null;
    }
    Path path = Paths.get(STATIC_LOCATION, ICON_DIR, name);
    try {
      return Files.exists(path) ? Files.readAllBytes(path) : null;
    } catch (IOException e) {
      return null;
    }
  }

  /** 静态资源根目录，去掉首尾斜杠。 */
  private static String location() {
    String location = EnvUtils.get("server.resources.static-locations", "pages");
    return location.replace("classpath:", "").replaceAll("^/+", "").replaceAll("/+$", "");
  }

  private String suffix(String filename) {
    if (filename == null) {
      return "";
    }
    int index = filename.lastIndexOf('.');
    return index < 0 ? "" : filename.substring(index + 1).toLowerCase(java.util.Locale.ROOT);
  }
}
