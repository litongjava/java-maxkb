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

  public ResultVo split(UploadFile file) throws Exception {
    if (file == null) {
      return ResultVo.fail("请求体中未找到文件");
    }
    SystemFileService systemFileService = Aop.get(SystemFileService.class);
    UploadResult vo = systemFileService.upload(file, "default", "default");
    return Aop.get(MaxKbDocumentSplitService.class).split(file.getData(), vo);
  }
}
