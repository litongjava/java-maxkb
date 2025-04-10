package com.litongjava.maxkb.model;

import com.litongjava.maxkb.model.base.BaseMaxKbApplicationChat;

/**
 * Generated by java-db.
 */
public class MaxKbApplicationChat extends BaseMaxKbApplicationChat<MaxKbApplicationChat> {
  private static final long serialVersionUID = 1L;
	public static final MaxKbApplicationChat dao = new MaxKbApplicationChat().dao();
	/**
	 * 
	 */
  public static final String tableName = "max_kb_application_chat";
  public static final String primaryKey = "id";
  //java.lang.Long 
  public static final String id = "id";
  //java.lang.String 
  public static final String _abstract = "abstract";
  //java.lang.Long 
  public static final String applicationId = "application_id";
  //java.lang.Long 
  public static final String clientId = "client_id";
  //java.lang.Integer 
  public static final String chatType = "chat_type";
  //java.lang.Boolean 
  public static final String isDeleted = "is_deleted";
  //java.lang.String 
  public static final String creator = "creator";
  //java.util.Date 
  public static final String createTime = "create_time";
  //java.lang.String 
  public static final String updater = "updater";
  //java.util.Date 
  public static final String updateTime = "update_time";
  //java.lang.Integer 
  public static final String deleted = "deleted";
  //java.lang.Long 
  public static final String tenantId = "tenant_id";

  @Override
  protected String _getPrimaryKey() {
    return primaryKey;
  }

  @Override
  protected String _getTableName() {
    return tableName;
  }
}

