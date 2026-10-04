package nexus.io.mosskb.service;

import java.util.List;

import com.jfinal.kit.Kv;

import nexus.io.db.TableInput;
import nexus.io.db.TableResult;
import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.jfinal.aop.Aop;
import nexus.io.kit.RowUtils;
import nexus.io.mosskb.vo.ResultPage;
import nexus.io.mosskb.vo.UserLoginReqVo;
import nexus.io.model.page.Page;
import nexus.io.model.result.ResultVo;
import nexus.io.table.services.ApiTable;
import nexus.io.tio.boot.admin.utils.TioAdminEnvUtils;
import nexus.io.tio.utils.crypto.Md5Utils;
import nexus.io.tio.utils.jwt.JwtUtils;
import nexus.io.tio.utils.token.TokenManager;

public class KbUserService {

  private static final String ACCOUNT_BY_USERNAME = """
      select id, password, token_version
      from moss_kb_user
      where username = ?
        and is_active = true
        and deleted = 0
      """;

  private static final String PROFILE_BY_ID = """
      select id, username, email, phone, nick_name, role
      from moss_kb_user
      where id = ?
      """;

  private static final String PAGE_CONDITION = """
      where deleted = 0
        and (username ilike ? or email ilike ?)
      """;

  private static final String COUNT_USERS = """
      select count(*)
      from moss_kb_user
      """ + PAGE_CONDITION;

  private static final String PAGE_USERS = """
      select id, username, email, phone, is_active, role, nick_name, create_time, update_time, source
      from moss_kb_user
      """ + PAGE_CONDITION + """
      order by create_time desc
      limit ? offset ?
      """;

  public ResultVo login(UserLoginReqVo vo) {
    if (vo == null || vo.getUsername() == null || vo.getPassword() == null) {
      return ResultVo.fail("用户名或者密码不正确");
    }
    Row account = Db.findFirst(ACCOUNT_BY_USERNAME, vo.getUsername());
    if (account == null || !UserPassword.matches(vo.getPassword(), account.getStr("password"))) {
      return ResultVo.fail("用户名或者密码不正确");
    }
    Long userId = account.getLong("id");
    String SECRET_KEY = TioAdminEnvUtils.getAdminSecretKey();
    String token = JwtUtils.createToken(SECRET_KEY, java.util.Map.of("userId", userId, "exp", System.currentTimeMillis() / 1000 + 3600, "token_version", account.getLong("token_version")));
    TokenManager.login(userId, token);
    return ResultVo.ok("成功", token);
  }

  public ResultVo index(Long userId) {
    Row record = Db.findFirst(PROFILE_BY_ID, userId);
    Kv kv = record.toKv();
    List<String> permissions = Aop.get(PermissionsService.class).getPermissionsByRole(kv.getStr("role"), userId);
    kv.set("permissions", permissions);
    return ResultVo.ok(kv);
  }

  public ResultVo logout(Long userId) {
    TokenManager.logout(userId);
    return ResultVo.ok();
  }

  public ResultVo page(Integer pageNo, Integer pageSize) {
    return page(pageNo, pageSize, null);
  }

  public ResultVo page(Integer pageNo, Integer pageSize, String search) {
    int currentPage = Math.max(1, pageNo == null ? 1 : pageNo);
    int currentSize = Math.max(1, Math.min(100, pageSize == null ? 20 : pageSize));
    String term = "%" + (search == null ? "" : search) + "%";
    long total = Db.queryLong(COUNT_USERS, term, term);
    List<Row> rows = Db.find(PAGE_USERS, term, term, currentSize, (long) (currentPage - 1) * currentSize);
    return ResultVo.ok(new ResultPage<Kv>(currentPage, currentSize, (int) total, RowUtils.toKv(rows, false)));
  }
}
