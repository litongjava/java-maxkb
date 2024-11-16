package com.litongjava.maxkb.service;

import java.util.List;

import com.jfinal.kit.Kv;
import com.litongjava.db.TableInput;
import com.litongjava.db.TableResult;
import com.litongjava.db.activerecord.Db;
import com.litongjava.db.activerecord.Record;
import com.litongjava.jfinal.aop.Aop;
import com.litongjava.kit.RecordUtils;
import com.litongjava.maxkb.constant.AppConstant;
import com.litongjava.maxkb.constant.TableNames;
import com.litongjava.maxkb.vo.ResultPage;
import com.litongjava.maxkb.vo.UserLoginReqVo;
import com.litongjava.model.page.Page;
import com.litongjava.model.result.ResultVo;
import com.litongjava.table.services.ApiTable;
import com.litongjava.tio.utils.crypto.Md5Utils;
import com.litongjava.tio.utils.jwt.JwtUtils;
import com.litongjava.tio.utils.token.TokenManager;

public class UserService {

  public ResultVo login(UserLoginReqVo vo) {
    vo.setPassword(Md5Utils.getMD5(vo.getPassword()));
    String loginSql = String.format("select id from %s where username=? and password=?", TableNames.max_kb_user);
    Long userId = Db.queryLong(loginSql, vo.getUsername(), vo.getPassword());
    if (userId == null) {
      return ResultVo.fail("用户名或者密码不正确");
    }
    String token = JwtUtils.createTokenByUserId(AppConstant.SECRET_KEY, userId);
    TokenManager.login(userId, token);
    return ResultVo.ok("成功", token);
  }

  public ResultVo index(Long userId) {
    String sql = String.format("select id,username,email,phone,nick_name,role from %s where id=?", TableNames.max_kb_user);
    Record record = Db.findFirst(sql, userId);
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
    TableResult<Page<Record>> tableResult = ApiTable.page(TableNames.max_kb_user, tableInput);
    Page<Record> page = tableResult.getData();
    int totalRow = page.getTotalRow();
    List<Record> list = page.getList();
    List<Kv> kvs = RecordUtils.recordsToKv(list, false);
    ResultPage<Kv> resultPage = new ResultPage<>(pageNo, pageSize, totalRow, kvs);
    return ResultVo.ok(resultPage);
  }
}
