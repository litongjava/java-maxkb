package com.litongjava.maxkb.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.litongjava.db.activerecord.Db;
import com.litongjava.maxkb.constant.MaxKbTableNames;

public class PermissionsService {

  public static final String APPLICATION = "APPLICATION";
  public static final String DATASET = "DATASET";

  public static final String MANAGE = "MANAGE";
  public static final String USE = "USE";
  public static final String DELETE = "DELETE";

  public static final List<String> defaultAdminPermissions = Arrays.asList("USER:READ", "USER:EDIT",
      //
      "DATASET:CREATE", "DATASET:READ", "DATASET:EDIT",
      //
      "APPLICATION:READ", "APPLICATION:CREATE", "APPLICATION:DELETE", "APPLICATION:EDIT",
      //
      "SETTING:READ",
      //
      "MODEL:READ", "MODEL:EDIT", "MODEL:DELETE", "MODEL:CREATE",
      //
      "TEAM:READ", "TEAM:CREATE", "TEAM:DELETE", "TEAM:EDIT");

  public List<String> getPermissionsByRole(String role) {
    // Switch-case to return the permissions based on the role
    switch (role.toLowerCase()) {
    case "admin":
      return getAdminPermissions();
    case "user":
      return new ArrayList<>();
    default:
      return new ArrayList<>();
    }
  }

  private List<String> getAdminPermissions() {
    //dataset ids
    List<Long> datasetIds = Db.query(String.format("select id from %s", MaxKbTableNames.max_kb_dataset));
    //appliction id
    List<Long> applicationIds = Db.query(String.format("select id from %s", MaxKbTableNames.max_kb_application));

    List<String> permissions = new ArrayList<>();
    permissions.addAll(defaultAdminPermissions);

    permissions.addAll(genPermissions(APPLICATION, MANAGE, applicationIds));
    permissions.addAll(genPermissions(APPLICATION, USE, applicationIds));
    permissions.addAll(genPermissions(APPLICATION, DELETE, applicationIds));

    permissions.addAll(genPermissions(DATASET, MANAGE, datasetIds));
    permissions.addAll(genPermissions(DATASET, USE, datasetIds));
    permissions.addAll(genPermissions(DATASET, DELETE, datasetIds));

    return permissions;
  }

  public List<String> genPermissions(String moduleName, String permissionName, List<Long> targetList) {
    List<String> permissions = new ArrayList<>();
    for (Long id : targetList) {
      permissions.add(moduleName + ":" + permissionName + ":" + id);
    }
    return permissions;
  }
}
