package nexus.io.mosskb.controller;

import java.nio.charset.StandardCharsets;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import nexus.io.annotation.Delete;
import nexus.io.annotation.Get;
import nexus.io.annotation.Post;
import nexus.io.annotation.Put;
import nexus.io.annotation.RequestPath;
import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.service.FunctionIconService;
import nexus.io.mosskb.service.kb.PythonFunctionService;
import nexus.io.mosskb.service.kb.MossKbDocumentPreviewService;
import nexus.io.model.result.ResultVo;
import nexus.io.model.upload.UploadFile;
import nexus.io.tio.boot.http.TioRequestContext;
import nexus.io.tio.http.common.HeaderName;
import nexus.io.tio.http.common.HeaderValue;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.server.util.Resps;

@RequestPath("/api/function_lib")
public class ApiFunctionLibController {
  private final PythonFunctionService service = Aop.get(PythonFunctionService.class);

  @Get("")
  public ResultVo all(HttpRequest request) {
    return service.list(TioRequestContext.getUserIdLong(), null, null, request.getParam("name"),
        request.getParam("function_type"), request.getParam("select_user_id"));
  }

  @Get("/{pageNo}/{pageSize}")
  public ResultVo page(Integer pageNo, Integer pageSize, HttpRequest request) {
    return service.list(TioRequestContext.getUserIdLong(), pageNo, pageSize, request.getParam("name"),
        request.getParam("function_type"), request.getParam("select_user_id"));
  }

  @Get("/{id}")
  public ResultVo get(Long id) {
    return service.get(TioRequestContext.getUserIdLong(), id);
  }

  @Post("")
  public ResultVo create(HttpRequest request) {
    return service.save(TioRequestContext.getUserIdLong(), null, JSON.parseObject(request.getBodyString()));
  }

  @Put("/{id}")
  public ResultVo update(Long id, HttpRequest request) {
    return service.save(TioRequestContext.getUserIdLong(), id, JSON.parseObject(request.getBodyString()));
  }

  @Delete("/{id}")
  public ResultVo remove(Long id) {
    return service.remove(TioRequestContext.getUserIdLong(), id);
  }

  @Post("/debug")
  public ResultVo debug(HttpRequest request) {
    try {
      return ResultVo.ok(service.debug(JSON.parseObject(request.getBodyString())));
    } catch (RuntimeException e) {
      return ResultVo.fail(e.getMessage());
    }
  }

  @Post("/pylint")
  public ResultVo pylint(HttpRequest request) {
    try {
      JSONObject body = JSON.parseObject(request.getBodyString());
      return service.pylint(body.getString("code"));
    } catch (RuntimeException e) {
      return ResultVo.fail(e.getMessage());
    }
  }

  @Post("/{id}/execute")
  public ResultVo execute(Long id, HttpRequest request) {
    try {
      JSONObject arguments = JSON.parseObject(request.getBodyString());
      return ResultVo.ok(service.execute(TioRequestContext.getUserIdLong(), id, arguments));
    } catch (RuntimeException e) {
      return ResultVo.fail(e.getMessage());
    }
  }

  /** 把内置函数模板复制成自己的函数，请求体只需要一个新名字。 */
  @Post("/{id}/add_internal_fun")
  public ResultVo addInternal(Long id, HttpRequest request) {
    try {
      JSONObject body = JSON.parseObject(request.getBodyString());
      return service.addInternal(TioRequestContext.getUserIdLong(), id, body == null ? null : body.getString("name"));
    } catch (RuntimeException e) {
      return ResultVo.fail(e.getMessage());
    }
  }

  /** 上传图标，返回新的图标路径。 */
  @Put("/{id}/edit_icon")
  public ResultVo editIcon(Long id, HttpRequest request) {
    try {
      return service.editIcon(TioRequestContext.getUserIdLong(), id, upload(request));
    } catch (RuntimeException e) {
      return ResultVo.fail(e.getMessage());
    }
  }

  /**
   * 读取图标。
   *
   * <p>图标要出现在函数列表、工作流节点和第三方嵌入页里，所以不要求登录；
   * 文件名是随机串，没有它取不到内容。
   */
  @Get("/icon/{name}")
  public HttpResponse icon(String name) {
    byte[] data = Aop.get(FunctionIconService.class).read(name);
    if (data == null) {
      return Resps.json(TioRequestContext.getResponse(), ResultVo.fail("图标不存在"));
    }
    String suffix = name.substring(name.lastIndexOf('.') + 1);
    return Resps.bytesWithContentType(TioRequestContext.getResponse(), data,
        MossKbDocumentPreviewService.contentType(suffix));
  }

  /** 下载 .fx 文件，文件名用函数名。 */
  @Get("/{id}/export")
  public HttpResponse export(Long id) {
    try {
      JSONObject payload = service.export(TioRequestContext.getUserIdLong(), id);
      byte[] data = payload.toJSONString().getBytes(StandardCharsets.UTF_8);
      HttpResponse response = Resps.bytesWithContentType(TioRequestContext.getResponse(), data,
          "application/octet-stream");
      response.addHeader(HeaderName.Content_Disposition,
          HeaderValue.from(MossKbDocumentPreviewService.disposition(true, payload.getString("name") + ".fx")));
      return response;
    } catch (RuntimeException e) {
      return Resps.json(TioRequestContext.getResponse(), ResultVo.fail(e.getMessage()));
    }
  }

  /** 导入 .fx 文件，导入结果默认是停用的私有函数。 */
  @Post("/import")
  public ResultVo importFunction(HttpRequest request) {
    try {
      return service.importFunction(TioRequestContext.getUserIdLong(), upload(request));
    } catch (RuntimeException e) {
      return ResultVo.fail(e.getMessage());
    }
  }

  /** multipart 请求里的第一个文件。 */
  private UploadFile upload(HttpRequest request) {
    Object[] files = request.getParams().get("file");
    if (files == null || files.length == 0) {
      return null;
    }
    for (Object item : files) {
      if (item instanceof UploadFile file) {
        return file;
      }
    }
    return null;
  }
}
