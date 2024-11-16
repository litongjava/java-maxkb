package com.litongjava.maxkb.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Record;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.maxkb.vo.MaxKbSearchStep;
import com.litongjava.maxkb.vo.ParagraphSearchResultVo;
import com.litongjava.openai.constants.OpenAiModels;
import com.litongjava.template.SqlTemplates;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MaxKbParagraphSearchService {

  public MaxKbSearchStep search(Long[] datasetIdArray, Float similarity, Integer top_n, String quesiton) {
    MaxKbSearchStep maxKbSearchStep = new MaxKbSearchStep();
    Long vectorId = Aop.get(MaxKbEmbeddingService.class).getVectorId(quesiton, OpenAiModels.text_embedding_3_large);
    String sql = SqlTemplates.get("kb.search_paragraph_with_dataset_ids");

    long start = System.currentTimeMillis();
    List<Record> records = Db.find(sql, vectorId, datasetIdArray, similarity, top_n);
    long end = System.currentTimeMillis();
    log.info("search_paragraph:{},{},{},{},{}", vectorId, Arrays.toString(datasetIdArray), similarity, top_n, records.size());
    List<ParagraphSearchResultVo> results = new ArrayList<>();
    for (Record record : records) {
      ParagraphSearchResultVo vo = record.toBean(ParagraphSearchResultVo.class);
      results.add(vo);
    }

    maxKbSearchStep.setStep_type("step_type")
        //
        .setModel_name("text_embedding_3_large").setProblem_text("problem_text")
        //
        .setCost(0).setRun_time(((double) (end - start)) / 1000)
        //
        .setParagraph_list(results);
    return maxKbSearchStep;
  }
}
