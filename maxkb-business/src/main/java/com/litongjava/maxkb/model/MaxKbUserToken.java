package com.litongjava.maxkb.model;

import com.litongjava.maxkb.model.base.BaseMaxKbUserToken;

/**
 * Generated by java-db.
 */
public class MaxKbUserToken extends BaseMaxKbUserToken<MaxKbUserToken> {
  private static final long serialVersionUID = 1L;
	public static final MaxKbUserToken dao = new MaxKbUserToken().dao();
	/**
	 * 
	 */
  public static final String tableName = "max_kb_user_token";
  public static final String primaryKey = "id";
  //java.lang.Long 
  public static final String id = "id";
  //java.lang.String 
  public static final String token = "token";
  //java.lang.String 
  public static final String remark = "remark";
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

