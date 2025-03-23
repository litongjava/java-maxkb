//package com.litongjava.maxkb.service;
//import java.util.ArrayList;
//import java.util.List;
//
//public class PermissionConverter {
//
//    public static List<Permission> toDynamicsPermission(String groupType, List<String> operate, String dynamicTag) {
//        List<Permission> permissions = new ArrayList<>();
//        for (String op : operate) {
//            // Assuming Group and Operate are enums or classes with methods to get enum values.
//            Group group = Group.valueOf(groupType);
//            Operate operation = Operate.valueOf(op);
//            permissions.add(new Permission(group, operation, dynamicTag));
//        }
//        return permissions;
//    }
//
//    public static void main(String[] args) {
//        // Example usage
//        List<Permission> result = new ArrayList<>();
//        List<String> memberPermissionList = List.of("MANAGE", "USE", "DELETE");
//
//        // Adding permissions for a dataset
//        result.addAll(toDynamicsPermission("DATASET", memberPermissionList, "b57cc7aa-7289-11ef-bc3a-0242ac110005"));
//        result.addAll(toDynamicsPermission("DATASET", memberPermissionList, "ec4bdc8e-7316-11ef-9335-0242ac110005"));
//        result.addAll(toDynamicsPermission("APPLICATION", memberPermissionList, "599c7a0a-7317-11ef-99ae-0242ac110005"));
//        
//        // Output the results
//        for (Permission permission : result) {
//            System.out.println(permission);
//        }
//    }
//}
//
//// Example Permission class
//class Permission {
//    private Group group;
//    private Operate operate;
//    private String dynamicTag;
//
//    public Permission(Group group, Operate operate, String dynamicTag) {
//        this.group = group;
//        this.operate = operate;
//        this.dynamicTag = dynamicTag;
//    }
//
//    @Override
//    public String toString() {
//        return group + ":" + operate + ":" + dynamicTag;
//    }
//}
//
//// Example enums for Group and Operate
//enum Group {
//    DATASET,
//    APPLICATION,
//    USER,
//    SETTING,
//    MODEL,
//    TEAM
//}
//
//enum Operate {
//    MANAGE,
//    USE,
//    DELETE,
//    READ,
//    EDIT,
//    CREATE
//}
