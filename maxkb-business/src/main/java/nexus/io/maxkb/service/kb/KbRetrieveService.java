package nexus.io.maxkb.service.kb;

import java.util.List;

import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.jfinal.aop.Aop;
import nexus.io.maxkb.vo.KbRetrieveInput;
import nexus.io.openai.consts.OpenAiModels;

public class KbRetrieveService {

  public List<String> retrieveText(KbRetrieveInput input) {
    Long vectorId = Aop.get(KbEmbeddingService.class).getVectorId(input.getInput(), OpenAiModels.TEXT_EMBEDDING_3_LARGE);
    String sql = generateRetrieveSql(input.getTable(), input.getColumns());
    return Db.queryListString(sql, vectorId, input.getSimilarity(), input.getTop_n());
  }

  public List<Row> retrieve(KbRetrieveInput input) {
    Long vectorId = Aop.get(KbEmbeddingService.class).getVectorId(input.getInput(), OpenAiModels.TEXT_EMBEDDING_3_LARGE);
    String sql = generateRetrieveSql(input.getTable(), input.getColumns());
    List<Row> records = Db.find(sql, vectorId, input.getSimilarity(), input.getTop_n());
    return records;
  }

  private String generateRetrieveSql(String table, String columns) {
    StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append("SELECT " + columns)
        //
        .append(",(1 - (t.embedding <=> c.v)) AS similarity FROM " + table + " AS t")
        //
        .append(" JOIN max_kb_embedding_cache AS c ON c.id = ?")
        //
        .append(" WHERE  t.deleted = 0  AND (1 - (t.embedding <=> c.v)) > ? ORDER BY  similarity DESC LIMIT ?");
    return stringBuffer.toString();
  }

}