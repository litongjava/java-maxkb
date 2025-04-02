package com.litongjava.maxkb.dao;

import com.jfinal.kit.Kv;
import com.litongjava.db.TableInput;
import com.litongjava.db.TableResult;
import com.litongjava.db.activerecord.Row;
import com.litongjava.maxkb.constant.MaxKbTableNames;
import com.litongjava.maxkb.vo.KbDatasetModel;
import com.litongjava.table.services.ApiTable;

public class MaxKbDatasetDao {

  public TableResult<Kv> saveOrUpdate(Long userId, KbDatasetModel model) {
    TableInput record = new TableInput();
    record.set("id", model.getId())
    //
    .set("name", model.getName()).set("desc",model.getDesc()).set("type",model.getType()).set("user_id", userId)
    //
    .set("embedding_mode_id", model.getEmbedding_mode_id());

    return ApiTable.saveOrUpdate(MaxKbTableNames.max_kb_dataset, record);
  }

  public TableResult<Row> get(Long userId, Long id) {
    TableInput tableInput = new TableInput();
    tableInput.set("id",id).set("user_id",userId);
    return ApiTable.get(MaxKbTableNames.max_kb_dataset, tableInput);
  }
}
