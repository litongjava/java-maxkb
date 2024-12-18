package com.litongjava.maxkb.model;

import com.litongjava.maxkb.model.base.BaseMaxKbApplication;

/**
 * Generated by java-db.
 */
public class MaxKbApplication extends BaseMaxKbApplication<MaxKbApplication> {
  private static final long serialVersionUID = 1L;
	public static final MaxKbApplication dao = new MaxKbApplication().dao();
	/**
	 * 
	 */
  public static final String tableName = "max_kb_application";
  public static final String primaryKey = "id";
  // private java.lang.Long id
  // private java.lang.Integer code
  // private java.lang.String courseName
  // private java.lang.String ownerName
  // private java.lang.Integer state
  // private java.lang.String name
  // private java.lang.String desc
  // private java.lang.String prompt
  // private java.lang.String prologue
  // private java.lang.Integer dialogueNumber
  // private java.lang.String datasetSetting
  // private java.lang.String modelSetting
  // private java.lang.Boolean problemOptimization
  // private java.lang.Long modelId
  // private java.lang.Long userId
  // private java.lang.String icon
  // private java.lang.String type
  // private java.lang.String workFlow
  // private java.lang.String modelParamsSetting
  // private java.lang.Long sttModelId
  // private java.lang.Boolean sttModelEnable
  // private java.lang.Long ttsModelId
  // private java.lang.Boolean ttsModelEnable
  // private java.lang.String ttsType
  // private java.lang.String problemOptimizationPrompt
  // private java.lang.String ttsModelParamsSetting
  // private java.lang.Integer cleanTime
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

