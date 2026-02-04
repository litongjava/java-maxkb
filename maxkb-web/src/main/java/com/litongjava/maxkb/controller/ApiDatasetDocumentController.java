package com.litongjava.maxkb.controller;

import com.litongjava.annotation.RequestPath;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.service.SystemFileService;
import com.litongjava.maxkb.service.kb.MaxKbDocumentSplitService;
import com.litongjava.model.result.ResultVo;
import com.litongjava.model.upload.UploadFile;
import com.litongjava.model.upload.UploadResult;

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
