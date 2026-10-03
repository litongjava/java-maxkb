package nexus.io.maxkb.task;

import org.quartz.JobExecutionContext;

import lombok.extern.slf4j.Slf4j;
import nexus.io.jfinal.aop.Aop;
import nexus.io.maxkb.service.kb.ChatLogCleanupService;
import nexus.io.tio.utils.quartz.AbstractJobWithLog;

/** 每天按应用保留天数清理过期对话日志。 */
@Slf4j
public class CleanChatLogJob extends AbstractJobWithLog {
  @Override
  public void run(JobExecutionContext context) throws Exception {
    int deleted = Aop.get(ChatLogCleanupService.class).cleanExpired();
    log.info("clean chat log job finished, deleted chat records: {}", deleted);
  }
}
