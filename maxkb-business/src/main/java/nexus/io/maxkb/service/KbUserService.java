package nexus.io.maxkb.service;

import java.util.List;

import com.jfinal.kit.Kv;

import nexus.io.db.TableInput;
import nexus.io.db.TableResult;
import nexus.io.db.activerecord.Db;
import nexus.io.db.activerecord.Row;
import nexus.io.jfinal.aop.Aop;
import nexus.io.kit.RowUtils;
import nexus.io.maxkb.constant.MaxKbTableNames;
import nexus.io.maxkb.vo.ResultPage;
import nexus.io.maxkb.vo.UserLoginReqVo;
import nexus.io.model.page.Page;
import nexus.io.model.result.ResultVo;
import nexus.io.table.services.ApiTable;
import nexus.io.tio.boot.admin.utils.TioAdminEnvUtils;
import nexus.io.tio.utils.crypto.Md5Utils;
import nexus.io.tio.utils.jwt.JwtUtils;
import nexus.io.tio.utils.token.TokenManager;

public class KbUserService {

  public ResultVo login(UserLoginReqVo vo) {
    vo.setPassword(Md5Utils.md5Hex(vo.getPassword()));
    String loginSql = String.format("select id from %s where username=? and password=?", MaxKbTableNames.max_kb_user);
    Long userId = Db.queryLong(loginSql, vo.getUsername(), vo.getPassword());
    if (userId == null) {
      return ResultVo.fail("用户名或者密码不正确");
    }
    String SECRET_KEY = TioAdminEnvUtils.getAdminSecretKey();
    String token = JwtUtils.createTokenByUserId(SECRET_KEY, userId);
    TokenManager.login(userId, token);
    return ResultVo.ok("成功", token);
  }

  public ResultVo index(Long userId) {
    String sql = String.format("select id,username,email,phone,nick_name,role from %s where id=?", MaxKbTableNames.max_kb_user);
    Row record = Db.findFirst(sql, userId);
    Kv kv = record.toKv();
    List<String> permissions = Aop.get(PermissionsService.class).getPermissionsByRole(kv.getStr("role"));
    kv.set("permissions", permissions);
    return ResultVo.ok(kv);
  }

  public ResultVo logout(Long userId) {
    TokenManager.logout(userId);
    return ResultVo.ok();
  }

  public ResultVo page(Integer pageNo, Integer pageSize) {
    TableInput tableInput = new TableInput();
    tableInput.setPageNo(pageNo).setPageSize(pageSize).setColumns("id,username,email,phone,is_active,role,nick_name,create_time,update_time,source");
    TableResult<Page<Row>> tableResult = ApiTable.page(MaxKbTableNames.max_kb_user, tableInput);
    Page<Row> page = tableResult.getData();
    int totalRow = page.getTotalRow();
    List<Row> list = page.getList();
    List<Kv> kvs = RowUtils.toKv(list, false);
    ResultPage<Kv> resultPage = new ResultPage<>(pageNo, pageSize, totalRow, kvs);
    return ResultVo.ok(resultPage);
  }
}
