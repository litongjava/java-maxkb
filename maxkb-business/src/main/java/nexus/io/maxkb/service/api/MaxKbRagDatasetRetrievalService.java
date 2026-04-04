package nexus.io.maxkb.service.api;

import java.util.List;

import nexus.io.chat.PlatformInput;
import nexus.io.jfinal.aop.Aop;
import nexus.io.maxkb.can.MaxKbSqlCan;
import nexus.io.maxkb.model.MaxKbDataset;
import nexus.io.maxkb.service.kb.KbEmbeddingService;
import nexus.io.maxkb.service.kb.KbRetrievalService;
import nexus.io.maxkb.service.kb.MaxKbModelService;
import nexus.io.maxkb.vo.ApiRagDatasetRetrievalRequest;
import nexus.io.maxkb.vo.MaxKbRetrievalResult;

public class MaxKbRagDatasetRetrievalService {

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
