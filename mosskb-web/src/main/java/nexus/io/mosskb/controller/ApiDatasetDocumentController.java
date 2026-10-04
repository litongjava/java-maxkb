package nexus.io.mosskb.controller;

import java.util.ArrayList;
import java.util.List;

import com.jfinal.kit.Kv;

import nexus.io.annotation.Get;
import nexus.io.annotation.Post;
import nexus.io.annotation.RequestPath;
import nexus.io.jfinal.aop.Aop;
import nexus.io.mosskb.service.SystemFileService;
import nexus.io.mosskb.service.kb.MossKbDocumentSplitService;
import nexus.io.mosskb.service.kb.MossKbDocumentSplitTaskService;
import nexus.io.mosskb.vo.MossKbDocumentSplitTaskVo;
import nexus.io.model.result.ResultVo;
import nexus.io.model.upload.UploadFile;
import nexus.io.model.upload.UploadResult;
import nexus.io.tio.boot.http.TioRequestContext;

@RequestPath("/api/dataset/document")
public class ApiDatasetDocumentController {

  /**
   * 分段预览：只负责上传与建任务，解析在后台线程逐页推进。
   *
   * <p>大文档解析要几分钟，请求线程不再等待，前端拿 task_id 轮询 {@link #splitTask(Long)} 取结果。
   */
  @Post("/split")
  public ResultVo split(nexus.io.tio.http.common.HttpRequest request) {
    Object[] files = request.getParams().get("file");
    if (files == null || files.length == 0) {
      return ResultVo.fail("请求体中未找到文件");
    }
    Long userId = TioRequestContext.getUserIdLong();
    List<Long> taskIds = new ArrayList<>();
    List<Kv> failures = new ArrayList<>();
    for (Object item : files) {
      if (!(item instanceof UploadFile)) {
        return ResultVo.fail("文件格式不正确");
      }
      UploadFile file = (UploadFile) item;
      UploadResult uploaded = Aop.get(SystemFileService.class).upload(file, "default", "default");
      if (uploaded == null) {
        failures.add(Kv.by("name", file.getName()).set("message", "文件保存失败"));
        continue;
      }
      try {
        taskIds.add(Aop.get(MossKbDocumentSplitService.class).splitAsync(file.getData(), uploaded, userId));
      } catch (Exception e) {
        failures.add(Kv.by("name", file.getName()).set("message", e.getMessage()));
      }
    }
    if (taskIds.isEmpty()) {
      return ResultVo.fail(failures.isEmpty() ? "没有可解析的文件" : String.valueOf(failures.get(0).get("message")));
    }
    return ResultVo.ok(Kv.by("task_id_list", taskIds).set("failures", failures));
  }

  /**
   * 分段预览任务状态。
   *
   * <p>running 时 data 为空，前端继续轮询；success 时 data 是分段结果；failed 时按失败返回，前端直接提示。
   */
  @Get("/split/task/{taskId}")
  public ResultVo splitTask(Long taskId) {
    Long userId = TioRequestContext.getUserIdLong();
    MossKbDocumentSplitTaskVo task = Aop.get(MossKbDocumentSplitTaskService.class).get(userId, taskId);
    if (task == null) {
      return ResultVo.fail("任务不存在或无权访问");
    }
    if (MossKbDocumentSplitTaskService.STATUS_FAILED.equals(task.getStatus())) {
      return ResultVo.fail(task.getErrorMessage() == null ? "文档解析失败" : task.getErrorMessage());
    }
    if (MossKbDocumentSplitTaskService.STATUS_SUCCESS.equals(task.getStatus())) {
      return ResultVo.ok(task.getResult() == null ? new ArrayList<>() : task.getResult());
    }
    return ResultVo.ok();
  }

  @Get("/split_pattern")
  public ResultVo split_pattern() {
    return ResultVo.ok(java.util.List.of(
        com.jfinal.kit.Kv.by("key", "\\n\\n").set("value", "段落"),
        com.jfinal.kit.Kv.by("key", "\\n").set("value", "换行")));
  }
}
