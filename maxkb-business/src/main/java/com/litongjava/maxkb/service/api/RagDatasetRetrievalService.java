package com.litongjava.maxkb.service.api;

import java.util.List;

import com.litongjava.chat.PlatformInput;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.can.MaxKbSqlCan;
import com.litongjava.maxkb.model.MaxKbDataset;
import com.litongjava.maxkb.service.kb.KbEmbeddingService;
import com.litongjava.maxkb.service.kb.KbRetrievalService;
import com.litongjava.maxkb.service.kb.MaxKbModelService;
import com.litongjava.maxkb.vo.ApiRagDatasetRetrievalRequest;
import com.litongjava.maxkb.vo.MaxKbRetrievalResult;

public class RagDatasetRetrievalService {

  private MaxKbModelService maxKbModelService = Aop.get(MaxKbModelService.class);
  private KbEmbeddingService kbEmbeddingService = Aop.get(KbEmbeddingService.class);
  private KbRetrievalService kbRetrievalService = Aop.get(KbRetrievalService.class);
  
  public List<MaxKbRetrievalResult> retrievalByTitle(ApiRagDatasetRetrievalRequest req) {
    String datasetName = req.getDataset_name();
    String input = req.getInput();
    Double similarity = req.getSimilarity();
    Integer top_number = req.getTop_number();
    String sql = "select id,embedding_mode_id from max_kb_dataset where deleted=0 and name=?";
    MaxKbDataset dataset = MaxKbDataset.dao.findFirst(sql,datasetName);
    Long datasetId = dataset.getId();
    Long embeddingModeId = dataset.getEmbeddingModeId();
    
    PlatformInput platformInput = maxKbModelService.getEmbeddingPlatformInput(embeddingModeId);
    
    Long vectorId = kbEmbeddingService.getVectorId(input, platformInput);
    
    List<MaxKbRetrievalResult> results = kbRetrievalService.retrievalParagraph(MaxKbSqlCan.retrievalByTitle, vectorId, datasetId, similarity, top_number);
    
    return results;

  }

}
