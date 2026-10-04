package nexus.io.mosskb.service.api;

import java.util.List;

import nexus.io.chat.PlatformInput;
import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.can.MossKbSqlCan;
import nexus.io.mosskb.model.MossKbDataset;
import nexus.io.mosskb.service.kb.KbEmbeddingService;
import nexus.io.mosskb.service.kb.KbRetrievalService;
import nexus.io.mosskb.service.kb.MossKbModelService;
import nexus.io.mosskb.vo.ApiRagDatasetRetrievalRequest;
import nexus.io.mosskb.vo.MossKbRetrievalResult;

public class MossKbRagDatasetRetrievalService {

  private MossKbModelService mossKbModelService = Aop.get(MossKbModelService.class);
  private KbEmbeddingService kbEmbeddingService = Aop.get(KbEmbeddingService.class);
  private KbRetrievalService kbRetrievalService = Aop.get(KbRetrievalService.class);
  
  public List<MossKbRetrievalResult> retrievalByTitle(ApiRagDatasetRetrievalRequest req) {
    String datasetName = req.getDataset_name();
    String input = req.getInput();
    Double similarity = req.getSimilarity();
    Integer top_number = req.getTop_number();
    String sql = "select id,embedding_mode_id from moss_kb_dataset where deleted=0 and name=?";
    MossKbDataset dataset = MossKbDataset.dao.findFirst(sql,datasetName);
    Long datasetId = dataset.getId();
    Long embeddingModeId = dataset.getEmbeddingModeId();
    
    PlatformInput platformInput = mossKbModelService.getEmbeddingPlatformInput(embeddingModeId);
    
    Long vectorId = kbEmbeddingService.getVectorId(input, platformInput);
    
    List<MossKbRetrievalResult> results = kbRetrievalService.retrievalParagraph(MossKbSqlCan.retrievalByTitle, vectorId, datasetId, similarity, top_number);
    
    return results;

  }

}
