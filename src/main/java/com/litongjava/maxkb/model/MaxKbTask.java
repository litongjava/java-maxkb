package com.litongjava.maxkb.model;

import com.litongjava.maxkb.model.base.BaseMaxKbTask;

/**
 * Generated by java-db.
 */
public class MaxKbTask extends BaseMaxKbTask<MaxKbTask> {
  private static final long serialVersionUID = 1L;
	public static final MaxKbTask dao = new MaxKbTask().dao();
	/**
	 * 
	 */
  public static final String tableName = "max_kb_task";
  public static final String primaryKey = "id";
  // private java.lang.Long id
  // private java.lang.Long fileId
  // private java.lang.String fileName
  // private java.lang.Long fileSize
  // private java.lang.Long datasetId
  // private java.lang.Long documentId
  // private java.lang.Integer progress
  // private java.lang.String status
  // private java.lang.String creator
  // private java.util.Date createTime
  // private java.lang.String updater
  // private java.util.Date updateTime
  // private java.lang.Integer deleted
  // private java.lang.Long tenantId

  @Override
  protected String _getPrimaryKey() {
    return primaryKey;
  }

  @Override
  protected String _getTableName() {
    return tableName;
  }
}
