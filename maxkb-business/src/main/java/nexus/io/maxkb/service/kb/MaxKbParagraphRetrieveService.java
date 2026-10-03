package nexus.io.maxkb.service.kb;

import java.util.ArrayList;
import java.util.List;
import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.jfinal.aop.Aop;
import nexus.io.maxkb.vo.MaxKbRetrieveResult;
import nexus.io.maxkb.vo.ParagraphSearchResultVo;

public class MaxKbParagraphRetrieveService {
  public MaxKbRetrieveResult retrieve(Long[] datasets, Float threshold, Integer limit, String question) {
    return retrieve(datasets, threshold, limit, question, "embedding");
  }

  public MaxKbRetrieveResult retrieve(Long[] datasets, Float threshold, Integer limit, String question, String mode) {
    long start = System.currentTimeMillis();
    List<ParagraphSearchResultVo> paragraphs = new ArrayList<>();
    for (Row row : searchRows(datasets, threshold, limit, question, mode)) {
      paragraphs.add(row.toBean(ParagraphSearchResultVo.class));
    }
    return new MaxKbRetrieveResult().setStep_type("search_step").setModel_name(KnowledgeModelService.embeddingModel())
        .setProblem_text(question).setCost(0).setRun_time((System.currentTimeMillis() - start) / 1000.0)
        .setParagraph_list(paragraphs);
  }

  public List<Row> searchRows(Long[] datasets, Float threshold, Integer limit, String question, String mode) {
    if (datasets == null || datasets.length == 0) {
      return List.of();
    }
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("检索问题不能为空");
    }
    String searchMode = mode == null ? "embedding" : mode;
    if (!List.of("embedding", "keywords", "blend").contains(searchMode)) {
      throw new IllegalArgumentException("未知的检索模式");
    }
    if (!searchMode.equals("keywords")) {
      java.util.Map<Long, List<Long>> groups = new java.util.LinkedHashMap<>();
      for (Row dataset : Db.find("select id,coalesce(embedding_mode_id,1002) as model_id from max_kb_dataset where id=any(?) and deleted=0", (Object) datasets)) {
        groups.computeIfAbsent(dataset.getLong("model_id"), key -> new ArrayList<>()).add(dataset.getLong("id"));
      }
      List<Row> all = new ArrayList<>();
      for (java.util.Map.Entry<Long, List<Long>> group : groups.entrySet()) {
        Long vectorId = Aop.get(KbEmbeddingService.class).getVectorIdForModel(question, group.getKey());
        all.addAll(searchGroup(group.getValue().toArray(new Long[0]), threshold, limit, question, searchMode, vectorId));
      }
      all.sort(java.util.Comparator.<Row>comparingDouble(row -> ((Number) row.get("similarity")).doubleValue()).reversed().thenComparing(row -> row.getLong("id")));
      return new ArrayList<>(all.subList(0, Math.min(all.size(), limit == null ? 10 : Math.max(1, Math.min(50, limit)))));
    }
    return searchGroup(datasets, threshold, limit, question, searchMode, null);
  }

  private List<Row> searchGroup(Long[] datasets, Float threshold, Integer limit, String question, String searchMode, Long vectorId) {
    List<Object> args = new ArrayList<>();
    String score;
    String cacheJoin = "";
    if (searchMode.equals("keywords")) {
      score = "word_similarity(?,p.content)";
      args.add(question);
    } else {
      score = "1-(p.embedding <=> c.v)";
      if (searchMode.equals("blend")) {
        score = "(" + score + ")+word_similarity(?,p.content)";
        args.add(question);
      }
      cacheJoin = " join max_kb_embedding_cache c on c.id=?";
      args.add(vectorId);
    }
    String sql = "select sub.*,similarity as comprehensive_score from (select p.id,p.content,p.title,p.status,p.hit_num,p.is_active,p.dataset_id,p.document_id,d.name as document_name,ds.name as dataset_name," + score
        + " as similarity from max_kb_paragraph p join max_kb_document d on d.id=p.document_id join max_kb_dataset ds on ds.id=p.dataset_id" + cacheJoin
        + " where p.is_active=true and d.is_active=true and p.deleted=0 and d.deleted=0 and ds.deleted=0 and p.dataset_id=any(?)) sub where similarity>=? order by similarity desc,id limit ?";
    args.add(datasets);
    args.add(threshold == null ? 0 : Math.max(0, threshold));
    args.add(limit == null ? 10 : Math.max(1, Math.min(50, limit)));
    return Db.find(sql, args.toArray());
  }

  public MaxKbRetrieveResult searchV1(Long[] datasets, Float threshold, Integer limit, String question) {
    return retrieve(datasets, threshold, limit, question);
  }

  public List<ParagraphSearchResultVo> searchV10(Long[] datasets, Float threshold, Integer limit, String question) {
    return retrieve(datasets, threshold, limit, question).getParagraph_list();
  }
}
