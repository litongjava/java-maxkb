package com.litongjava.maxkb.model;

import com.litongjava.maxkb.model.base.BaseMaxKbApplicationAccessToken;

/**
 * Generated by java-db.
 */
public class MaxKbApplicationAccessToken extends BaseMaxKbApplicationAccessToken<MaxKbApplicationAccessToken> {
  private static final long serialVersionUID = 1L;
	public static final MaxKbApplicationAccessToken dao = new MaxKbApplicationAccessToken().dao();
	/**
	 * 
	 */
  public static final String tableName = "max_kb_application_access_token";
  public static final String primaryKey = "application_id";
  // private java.lang.Long applicationId
  // private java.lang.String accessToken
  // private java.lang.Boolean isActive
  // private java.lang.Integer accessNum
  // private java.lang.Boolean whiteActive
  // private java.lang.String[] whiteList
  // private java.lang.Long[] longList
  // private java.lang.Integer[] intList
  // private java.lang.Boolean showSource
  // private java.lang.String remark
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
