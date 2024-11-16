package com.litongjava.maxkb.model;

import com.litongjava.db.activerecord.ActiveRecordPlugin;

/**
 * Generated by JFinal, do not modify this file.
 * <pre>
 * Example:
 * public void configPlugin(Plugins me) {
 *     ActiveRecordPlugin arp = new ActiveRecordPlugin(...);
 *     _MappingKit.mapping(arp);
 *     me.add(arp);
 * }
 * </pre>
 */
public class _MappingKit {
	
	public static void mapping(ActiveRecordPlugin arp) {
		arp.addMapping("max_kb_application", "id", MaxKbApplication.class);
		arp.addMapping("max_kb_application_access_token", "application_id", MaxKbApplicationAccessToken.class);
		arp.addMapping("max_kb_application_chat", "id", MaxKbApplicationChat.class);
		arp.addMapping("max_kb_application_chat_record", "id", MaxKbApplicationChatRecord.class);
		arp.addMapping("max_kb_application_dataset_mapping", "id", MaxKbApplicationDatasetMapping.class);
		arp.addMapping("max_kb_application_temp_setting", "id", MaxKbApplicationTempSetting.class);
		arp.addMapping("max_kb_dataset", "id", MaxKbDataset.class);
		arp.addMapping("max_kb_document", "id", MaxKbDocument.class);
		arp.addMapping("max_kb_document_markdown_page_cache", "id", MaxKbDocumentMarkdownPageCache.class);
		arp.addMapping("max_kb_embedding_cache", "id", MaxKbEmbeddingCache.class);
		arp.addMapping("max_kb_file", "id", MaxKbFile.class);
		arp.addMapping("max_kb_model", "id", MaxKbModel.class);
		arp.addMapping("max_kb_paragraph", "id", MaxKbParagraph.class);
		arp.addMapping("max_kb_problem", "id", MaxKbProblem.class);
		arp.addMapping("max_kb_problem_paragraph_mapping", "id", MaxKbProblemParagraphMapping.class);
		arp.addMapping("max_kb_task", "id", MaxKbTask.class);
		arp.addMapping("max_kb_user", "id", MaxKbUser.class);
		arp.addMapping("max_kb_user_token", "id", MaxKbUserToken.class);
	}
}

