package com.litongjava.maxkb.service.kb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Row;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.vo.MaxKbRetrieveResult;
import com.litongjava.maxkb.vo.ParagraphSearchResultVo;
import com.litongjava.openai.consts.OpenAiModels;
import com.litongjava.template.SqlTemplates;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MaxKbParagraphRetrieveService {

  public MaxKbRetrieveResult retrieve(Long[] datasetIdArray, Float similarity, Integer top_n, String quesiton) {
    MaxKbRetrieveResult maxKbSearchStep = new MaxKbRetrieveResult();
    long start = System.currentTimeMillis();
    List<ParagraphSearchResultVo> results = search0(datasetIdArray, similarity, top_n, quesiton);
    long end = System.currentTimeMillis();
    maxKbSearchStep.setStep_type("step_type")
        //
        .setModel_name(OpenAiModels.TEXT_EMBEDDING_3_LARGE).setProblem_text("problem_text")
        //
        .setCost(0).setRun_time(((double) (end - start)) / 1000)
        //
        .setParagraph_list(results);
    return maxKbSearchStep;
  }
  
  private List<ParagraphSearchResultVo> search0(Long[] datasetIdArray, Float similarity, Integer top_n, String quesiton) {
    Long vectorId = Aop.get(KbEmbeddingService.class).getVectorId(quesiton, OpenAiModels.TEXT_EMBEDDING_3_LARGE);
    String sql = SqlTemplates.get("kb.search_sentense_related_paragraph__with_dataset_ids");

    List<Row> records = Db.find(sql, vectorId, datasetIdArray, similarity, top_n);

    log.info("search_paragraph:{},{},{},{},{}", vectorId, Arrays.toString(datasetIdArray), similarity, top_n, records.size());
    List<ParagraphSearchResultVo> results = new ArrayList<>();
    for (Row record : records) {
      ParagraphSearchResultVo vo = record.toBean(ParagraphSearchResultVo.class);
      results.add(vo);
    }
    return results;
  }

  public MaxKbRetrieveResult searchV1(Long[] datasetIdArray, Float similarity, Integer top_n, String quesiton) {
    MaxKbRetrieveResult maxKbSearchStep = new MaxKbRetrieveResult();
    long start = System.currentTimeMillis();
    List<ParagraphSearchResultVo> results = searchV10(datasetIdArray, similarity, top_n, quesiton);
    long end = System.currentTimeMillis();
    maxKbSearchStep.setStep_type("step_type")
        //
        .setModel_name("text_embedding_3_large").setProblem_text("problem_text")
        //
        .setCost(0).setRun_time(((double) (end - start)) / 1000)
        //
        .setParagraph_list(results);
    return maxKbSearchStep;
  }

  public List<ParagraphSearchResultVo> searchV10(Long[] datasetIdArray, Float similarity, Integer top_n,
      //
      String quesiton) {
    Long vectorId = Aop.get(KbEmbeddingService.class).getVectorId(quesiton, OpenAiModels.TEXT_EMBEDDING_3_LARGE);
    String sql = SqlTemplates.get("kb.search_paragraph_with_dataset_ids");

    List<Row> records = Db.find(sql, vectorId, datasetIdArray, similarity, top_n);

    log.info("search_paragraph:{},{},{},{},{}", vectorId, Arrays.toString(datasetIdArray), similarity, top_n, records.size());
    List<ParagraphSearchResultVo> results = new ArrayList<>();
    for (Row record : records) {
      ParagraphSearchResultVo vo = record.toBean(ParagraphSearchResultVo.class);
      results.add(vo);
    }
    return results;
  }
}
