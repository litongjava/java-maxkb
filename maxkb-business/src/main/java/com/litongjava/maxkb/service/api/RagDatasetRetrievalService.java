package com.litongjava.maxkb.service.api;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.litongjava.chat.PlatformInput;
import com.litongjava.db.activerecord.Db;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.model.MaxKbDataset;
import com.litongjava.maxkb.service.kb.MaxKbModelService;
import com.litongjava.maxkb.vo.ApiRagDatasetResponse;
import com.litongjava.maxkb.vo.ApiRagDatasetRetrievalRequest;
import com.litongjava.maxkb.vo.KbParagraph;

public class RagDatasetRetrievalService {

  private MaxKbModelService maxKbModelService = Aop.get(MaxKbModelService.class);
  
  public ApiRagDatasetResponse retrievalByTitle(ApiRagDatasetRetrievalRequest req) {
    String datasetName = req.getDatasetName();
    String sql = "select id,embedding_mode_id from max_kb_dataset where deleted=0 and name=?";
    MaxKbDataset dataset = MaxKbDataset.dao.findFirst(sql,datasetName);
    Long datasetId = dataset.getId();
    Long embeddingModeId = dataset.getEmbeddingModeId();
    
    PlatformInput platformInput = maxKbModelService.getEmbeddingPlatformInput(embeddingModeId);
    
    
    List<KbParagraph> paragraphs = new ArrayList<>();

    Timestamp now = new Timestamp(System.currentTimeMillis());

    KbParagraph p1 = new KbParagraph(1L, "如何重置密码？", "请在登录页面点击【忘记密码】，通过邮箱验证码完成密码重置。", "1", 12, true, 100L, 200L, now,
        now);

    KbParagraph p2 = new KbParagraph(2L, "密码忘记怎么办？", "如果无法找回密码，请联系管理员或提交工单处理。", "1", 7, true, 100L, 201L, now, now);

    paragraphs.add(p1);
    paragraphs.add(p2);

    ApiRagDatasetResponse resp = new ApiRagDatasetResponse(paragraphs);
    return resp;

  }

}
