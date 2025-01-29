package com.litongjava.maxkb.task;

import org.quartz.JobExecutionContext;

import com.litongjava.db.activerecord.Db;
import com.litongjava.tio.utils.quartz.AbstractJobWithLog;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SchduleTaskPerHour extends AbstractJobWithLog {
  @Override
  public void run(JobExecutionContext context) throws Exception {
    log.info("任务执行上下文: {}", context);
    Db.delete("DELETE FROM max_kb_web_page_cache WHERE create_time < NOW() - INTERVAL '1 day'");
  }
}
