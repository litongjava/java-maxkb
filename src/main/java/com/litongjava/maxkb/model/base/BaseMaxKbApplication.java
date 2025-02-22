package com.litongjava.maxkb.model.base;

import com.litongjava.db.activerecord.Model;
import com.litongjava.model.db.IBean;

/**
 * Generated by java-db, do not modify this file.
 */
@SuppressWarnings({"serial", "unchecked"})
public abstract class BaseMaxKbApplication<M extends BaseMaxKbApplication<M>> extends Model<M> implements IBean {

	public M setId(java.lang.Long id) {
		set("id", id);
		return (M)this;
	}
	
	public java.lang.Long getId() {
		return getLong("id");
	}
	
	public M setCode(java.lang.Integer code) {
		set("code", code);
		return (M)this;
	}
	
	public java.lang.Integer getCode() {
		return getInt("code");
	}
	
	public M setCourseName(java.lang.String courseName) {
		set("course_name", courseName);
		return (M)this;
	}
	
	public java.lang.String getCourseName() {
		return getStr("course_name");
	}
	
	public M setOwnerName(java.lang.String ownerName) {
		set("owner_name", ownerName);
		return (M)this;
	}
	
	public java.lang.String getOwnerName() {
		return getStr("owner_name");
	}
	
	public M setState(java.lang.Integer state) {
		set("state", state);
		return (M)this;
	}
	
	public java.lang.Integer getState() {
		return getInt("state");
	}
	
	public M setName(java.lang.String name) {
		set("name", name);
		return (M)this;
	}
	
	public java.lang.String getName() {
		return getStr("name");
	}
	
	public M setDesc(java.lang.String desc) {
		set("desc", desc);
		return (M)this;
	}
	
	public java.lang.String getDesc() {
		return getStr("desc");
	}
	
	public M setPrompt(java.lang.String prompt) {
		set("prompt", prompt);
		return (M)this;
	}
	
	public java.lang.String getPrompt() {
		return getStr("prompt");
	}
	
	public M setPrologue(java.lang.String prologue) {
		set("prologue", prologue);
		return (M)this;
	}
	
	public java.lang.String getPrologue() {
		return getStr("prologue");
	}
	
	public M setDialogueNumber(java.lang.Integer dialogueNumber) {
		set("dialogue_number", dialogueNumber);
		return (M)this;
	}
	
	public java.lang.Integer getDialogueNumber() {
		return getInt("dialogue_number");
	}
	
	public M setDatasetSetting(java.lang.String datasetSetting) {
		set("dataset_setting", datasetSetting);
		return (M)this;
	}
	
	public java.lang.String getDatasetSetting() {
		return getStr("dataset_setting");
	}
	
	public M setModelSetting(java.lang.String modelSetting) {
		set("model_setting", modelSetting);
		return (M)this;
	}
	
	public java.lang.String getModelSetting() {
		return getStr("model_setting");
	}
	
	public M setProblemOptimization(java.lang.Boolean problemOptimization) {
		set("problem_optimization", problemOptimization);
		return (M)this;
	}
	
	public java.lang.Boolean getProblemOptimization() {
		return getBoolean("problem_optimization");
	}
	
	public M setModelId(java.lang.Long modelId) {
		set("model_id", modelId);
		return (M)this;
	}
	
	public java.lang.Long getModelId() {
		return getLong("model_id");
	}
	
	public M setUserId(java.lang.Long userId) {
		set("user_id", userId);
		return (M)this;
	}
	
	public java.lang.Long getUserId() {
		return getLong("user_id");
	}
	
	public M setIcon(java.lang.String icon) {
		set("icon", icon);
		return (M)this;
	}
	
	public java.lang.String getIcon() {
		return getStr("icon");
	}
	
	public M setType(java.lang.String type) {
		set("type", type);
		return (M)this;
	}
	
	public java.lang.String getType() {
		return getStr("type");
	}
	
	public M setWorkFlow(java.lang.String workFlow) {
		set("work_flow", workFlow);
		return (M)this;
	}
	
	public java.lang.String getWorkFlow() {
		return getStr("work_flow");
	}
	
	public M setShowSource(java.lang.Boolean showSource) {
		set("show_source", showSource);
		return (M)this;
	}
	
	public java.lang.Boolean getShowSource() {
		return getBoolean("show_source");
	}
	
	public M setMultipleRoundsDialogue(java.lang.Boolean multipleRoundsDialogue) {
		set("multiple_rounds_dialogue", multipleRoundsDialogue);
		return (M)this;
	}
	
	public java.lang.Boolean getMultipleRoundsDialogue() {
		return getBoolean("multiple_rounds_dialogue");
	}
	
	public M setModelParamsSetting(java.lang.String modelParamsSetting) {
		set("model_params_setting", modelParamsSetting);
		return (M)this;
	}
	
	public java.lang.String getModelParamsSetting() {
		return getStr("model_params_setting");
	}
	
	public M setSttModelId(java.lang.Long sttModelId) {
		set("stt_model_id", sttModelId);
		return (M)this;
	}
	
	public java.lang.Long getSttModelId() {
		return getLong("stt_model_id");
	}
	
	public M setSttModelEnable(java.lang.Boolean sttModelEnable) {
		set("stt_model_enable", sttModelEnable);
		return (M)this;
	}
	
	public java.lang.Boolean getSttModelEnable() {
		return getBoolean("stt_model_enable");
	}
	
	public M setTtsModelId(java.lang.Long ttsModelId) {
		set("tts_model_id", ttsModelId);
		return (M)this;
	}
	
	public java.lang.Long getTtsModelId() {
		return getLong("tts_model_id");
	}
	
	public M setTtsModelEnable(java.lang.Boolean ttsModelEnable) {
		set("tts_model_enable", ttsModelEnable);
		return (M)this;
	}
	
	public java.lang.Boolean getTtsModelEnable() {
		return getBoolean("tts_model_enable");
	}
	
	public M setTtsType(java.lang.String ttsType) {
		set("tts_type", ttsType);
		return (M)this;
	}
	
	public java.lang.String getTtsType() {
		return getStr("tts_type");
	}
	
	public M setProblemOptimizationPrompt(java.lang.String problemOptimizationPrompt) {
		set("problem_optimization_prompt", problemOptimizationPrompt);
		return (M)this;
	}
	
	public java.lang.String getProblemOptimizationPrompt() {
		return getStr("problem_optimization_prompt");
	}
	
	public M setTtsModelParamsSetting(java.lang.String ttsModelParamsSetting) {
		set("tts_model_params_setting", ttsModelParamsSetting);
		return (M)this;
	}
	
	public java.lang.String getTtsModelParamsSetting() {
		return getStr("tts_model_params_setting");
	}
	
	public M setCleanTime(java.lang.Integer cleanTime) {
		set("clean_time", cleanTime);
		return (M)this;
	}
	
	public java.lang.Integer getCleanTime() {
		return getInt("clean_time");
	}
	
	public M setRemark(java.lang.String remark) {
		set("remark", remark);
		return (M)this;
	}
	
	public java.lang.String getRemark() {
		return getStr("remark");
	}
	
	public M setCreator(java.lang.String creator) {
		set("creator", creator);
		return (M)this;
	}
	
	public java.lang.String getCreator() {
		return getStr("creator");
	}
	
	public M setCreateTime(java.util.Date createTime) {
		set("create_time", createTime);
		return (M)this;
	}
	
	public java.util.Date getCreateTime() {
		return getDate("create_time");
	}
	
	public M setUpdater(java.lang.String updater) {
		set("updater", updater);
		return (M)this;
	}
	
	public java.lang.String getUpdater() {
		return getStr("updater");
	}
	
	public M setUpdateTime(java.util.Date updateTime) {
		set("update_time", updateTime);
		return (M)this;
	}
	
	public java.util.Date getUpdateTime() {
		return getDate("update_time");
	}
	
	public M setDeleted(java.lang.Integer deleted) {
		set("deleted", deleted);
		return (M)this;
	}
	
	public java.lang.Integer getDeleted() {
		return getInt("deleted");
	}
	
	public M setTenantId(java.lang.Long tenantId) {
		set("tenant_id", tenantId);
		return (M)this;
	}
	
	public java.lang.Long getTenantId() {
		return getLong("tenant_id");
	}
	
}

