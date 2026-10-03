package nexus.io.maxkb.controller;

import nexus.io.annotation.RequestPath;
import nexus.io.jfinal.aop.Aop;
import nexus.io.maxkb.service.SystemFileService;
import nexus.io.maxkb.service.kb.MaxKbDocumentSplitService;
import nexus.io.model.result.ResultVo;
import nexus.io.model.upload.UploadFile;
import nexus.io.model.upload.UploadResult;

@RequestPath("/api/dataset/document")
public class ApiDatasetDocumentController {

  public ResultVo split(nexus.io.tio.http.common.HttpRequest request) throws Exception {
    Object[] files = request.getParams().get("file");
    if (files == null || files.length == 0) {
      return ResultVo.fail("请求体中未找到文件");
    }
    java.util.List<Object> results = new java.util.ArrayList<>();
    for (Object item : files) {
      if (!(item instanceof UploadFile)) {
        return ResultVo.fail("文件格式不正确");
      }
      UploadFile file = (UploadFile) item;
      UploadResult uploaded = Aop.get(SystemFileService.class).upload(file, "default", "default");
      ResultVo result = Aop.get(MaxKbDocumentSplitService.class).split(file.getData(), uploaded);
      results.addAll((java.util.List<?>) result.getData());
    }
    return ResultVo.ok(results);
  }

  public ResultVo split_pattern() {
    return ResultVo.ok(java.util.List.of(
        com.jfinal.kit.Kv.by("key", "\\n\\n").set("value", "段落"),
        com.jfinal.kit.Kv.by("key", "\\n").set("value", "换行")));
  }
}