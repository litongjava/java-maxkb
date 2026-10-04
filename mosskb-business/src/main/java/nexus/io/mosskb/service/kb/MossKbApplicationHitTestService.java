package nexus.io.mosskb.service.kb;

import java.util.List;
import nexus.io.db.activerecord.Db;
import nexus.io.jfinal.aop.Aop;
import nexus.io.kit.RowUtils;
import nexus.io.model.result.ResultVo;

public class MossKbApplicationHitTestService {
  public ResultVo hitTest(Long user, Long application, String question, Double threshold, Integer limit, String mode) {
    if (!ApplicationAccess.owns(user, application)) {
      return ResultVo.fail("应用不存在或无权访问");
    }
    List<Long> datasets = Db.queryListLong("select dataset_id from moss_kb_application_dataset_mapping where application_id=?", application);
    return ResultVo.ok(RowUtils.toKv(Aop.get(MossKbParagraphRetrieveService.class).searchRows(
        datasets.toArray(new Long[0]), threshold == null ? 0f : threshold.floatValue(), limit, question, mode), false));
  }
}
