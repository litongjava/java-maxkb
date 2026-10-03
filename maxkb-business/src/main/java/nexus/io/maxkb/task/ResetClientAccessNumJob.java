package nexus.io.maxkb.task;

import org.quartz.JobExecutionContext;

import lombok.extern.slf4j.Slf4j;
import nexus.io.db.activerecord.Db;
import nexus.io.tio.utils.quartz.AbstractJobWithLog;

/** 每天把分享页访客的当日提问次数归零，次数上限按自然日重新计算。 */
@Slf4j
public class ResetClientAccessNumJob extends AbstractJobWithLog {

  private static final String RESET_INTRADAY_ACCESS_NUM = """
      update max_kb_application_public_access_client
      set intraday_access_num = 0,
          update_time = now()
      where intraday_access_num <> 0
      """;

  @Override
  public void run(JobExecutionContext context) throws Exception {
    int updated = Db.update(RESET_INTRADAY_ACCESS_NUM);
    log.info("reset client access num job finished, updated rows: {}", updated);
  }
}
