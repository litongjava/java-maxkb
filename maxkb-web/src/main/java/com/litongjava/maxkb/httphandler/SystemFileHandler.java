package com.litongjava.maxkb.httphandler;

import com.jfinal.kit.StrKit;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.service.SystemFileService;
import com.litongjava.model.body.RespBodyVo;
import com.litongjava.model.upload.UploadFile;
import com.litongjava.model.upload.UploadResult;
import com.litongjava.tio.boot.http.TioRequestContext;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.HttpResponse;
import com.litongjava.tio.http.server.model.HttpCors;
import com.litongjava.tio.http.server.util.CORSUtils;
import com.litongjava.tio.http.server.util.Resps;

public class SystemFileHandler {

  /**
   * 处理文件上传请求
   *
   * @param request HTTP 请求
   * @return HttpResponse 响应
   */
  public HttpResponse upload(HttpRequest request) {
    HttpResponse httpResponse = TioRequestContext.getResponse();
    CORSUtils.enableCORS(httpResponse, new HttpCors());

    UploadFile uploadFile = request.getUploadFile("file");
    String bucket = request.getParam("bucket");
    String category = request.getParam("category");
    if (uploadFile == null) {
      return httpResponse.fail(RespBodyVo.fail("请求体中未找到文件"));
    }
    if (bucket == null) {
      return httpResponse.fail(RespBodyVo.fail("存储桶名称不能为空"));
    }
    if (category == null) {
      return httpResponse.fail(RespBodyVo.fail("文件分类不能为空"));
    }
    SystemFileService systemFileService = Aop.get(SystemFileService.class);
    UploadResult vo = systemFileService.upload(uploadFile, bucket, category);
    return httpResponse.setJson(vo);
  }

  /**
   * 处理获取文件 URL 的请求
   *
   * @param request HTTP 请求
   * @return HttpResponse 响应
   * @throws Exception 异常
   */
  public HttpResponse getUrl(HttpRequest request) throws Exception {
    HttpResponse httpResponse = TioRequestContext.getResponse();
    CORSUtils.enableCORS(httpResponse, new HttpCors());

    SystemFileService systemFileService = Aop.get(SystemFileService.class);

    RespBodyVo respBodyVo = null;
    Long id = request.getLong("id");
    String md5 = request.getParam("md5");
    String bucket = request.getParam("bucket");
    if (id != null && id > 0) {
      // 根据 ID 获取文件信息
      UploadResult uploadResultVo = systemFileService.getUrlById(id);
      if (uploadResultVo == null) {
        respBodyVo = RespBodyVo.fail();
      } else {
        respBodyVo = RespBodyVo.ok(uploadResultVo);
      }

    } else if (StrKit.notBlank(md5)) {
      // 根据 MD5 获取文件信息
      UploadResult uploadResultVo = systemFileService.getUrlByMd5(bucket, md5);
      if (uploadResultVo == null) {
        respBodyVo = RespBodyVo.fail();
      } else {
        respBodyVo = RespBodyVo.ok(uploadResultVo);
      }
    } else {
      respBodyVo = RespBodyVo.fail("id 或 md5 不能为空");
    }

    return Resps.json(httpResponse, respBodyVo);
  }

}
