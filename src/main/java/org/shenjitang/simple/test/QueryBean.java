package org.shenjitang.simple.test;


import java.util.List;
import org.shenjitang.simple.dao.annotation.DbField;
import org.shenjitang.simple.dao.annotation.DbJoin;
import org.shenjitang.simple.dao.annotation.DbJoinType;
import org.shenjitang.simple.dao.annotation.DbLink;
import org.shenjitang.simple.dao.annotation.DbNoMap;
import org.shenjitang.simple.dao.annotation.DbTable;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author xiaolie33
 */
@DbTable(value = "user_table", alias = "u")
@DbJoin(type=DbJoinType.INNER, table = "role_table", alias = "r", on = "id", eq = "role_id")
@DbJoin(type=DbJoinType.INNER, table = "class_table", alias = "c", on = "id", eq = "class_id")
public class QueryBean {
    @DbField("id")
    private int userId;
    private String name;
    @DbField(value = "name", table = "r")
    private String roleName;
    @DbField(value = "name", table = "c")
    private String className;
    @DbLink(value = "userId", thisField = "userId")
    private List<Contact> contact;
    @DbNoMap
    private String lalala;
    public QueryBean() {
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<Contact> getContact() {
        return contact;
    }

    public void setContact(List<Contact> contact) {
        this.contact = contact;
    }

    public String getLalala() {
        return lalala;
    }

    public void setLalala(String lalala) {
        this.lalala = lalala;
    }
    
}
