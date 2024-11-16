package com.litongjava.maxkb.service;
import java.util.Map;

import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Record;
import com.litongjava.tio.boot.http.forward.RequestProxyCallback;
import com.litongjava.tio.http.common.HeaderName;
import com.litongjava.tio.http.common.HeaderValue;
import com.litongjava.tio.http.common.HttpRequest;
import com.litongjava.tio.http.common.RequestLine;
import com.litongjava.tio.utils.hutool.ZipUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AppForwardRequestService implements RequestProxyCallback {

  public void saveRequest(long id, String ip, HttpRequest httpRequest) {
    StringBuffer stringBuffer = new StringBuffer();
    RequestLine requestLine = httpRequest.getRequestLine();

    stringBuffer.append(requestLine.toString()).append("\n");
    Map<String, String> headers = httpRequest.getHeaders();

    // 请求体
    String contentType = httpRequest.getContentType();
    if (contentType != null) {
      if (contentType.startsWith("application/json")) {
        stringBuffer.append(httpRequest.getBodyString());

      } else if (contentType.startsWith("application/x-www-form-urlencoded")) {
        Map<String, Object[]> params = httpRequest.getParams();
        if (params != null) {
          for (Map.Entry<String, Object[]> e : params.entrySet()) {
            stringBuffer.append(e.getKey() + ": " + e.getValue()[0]).append("\n");
          }
        }
      } else if (contentType.startsWith("application/from-data")) {
        Map<String, Object[]> params = httpRequest.getParams();

        for (Map.Entry<String, Object[]> e : params.entrySet()) {
          Object value = e.getValue()[0];
          // 添加参数
          if (value instanceof String) {
            stringBuffer.append(e.getKey()).append(":").append(e.getValue()[0]).append("\n");
          } else {
            stringBuffer.append(e.getKey()).append(":").append("binary \n");
          }
        }
      }
    }

    String method = requestLine.getMethod().toString();
    String path = requestLine.getPath();
    Record record = Record.by("id", id).set("ip", ip).set("ip_region", "")
        //
        .set("method", method).set("uri", path).set("request_header", headers).set("request_body", stringBuffer.toString());

    String[] jsonFields = { "request_header" };
    boolean saveResult = Db.save("sys_http_forward_statistics", record, jsonFields);

    if (!saveResult) {
      log.error("Failed to save db:{}", "sys_http_forward_statistics");

    }

  }

  @Override
  public void saveResponse(long id, long elapsed, int statusCode, Map<HeaderName, HeaderValue> headers,
      HeaderValue contentEncoding, byte[] body) {
    Record record = Record.by("id", id).set("elapsed", elapsed).set("response_status", statusCode);

    if (body != null && body.length > 0) {
      if (contentEncoding != null) {
        if (HeaderValue.Content_Encoding.gzip.equals(contentEncoding)) {
          String value = new String(ZipUtil.unGzip(body));
          log.info("response:{},{}", id, value);
          record.set("response_body", value);
        }
      }

      else {
        record.set("response_body", new String(body));
      }
    }

    String tableName = "sys_http_forward_statistics";
    try {
      boolean update = Db.update(tableName, record);
      if (!update) {
        log.error("Failed to update table:{},{}", tableName, id);
      }
    } catch (Exception e) {
      log.error("Failed to update table:{},{},{}", tableName, id, e.getMessage());
    }

  }
}
