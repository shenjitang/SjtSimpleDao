/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.simple.dao.jdbc;

import java.util.Collection;

/**
 *
 * @author xiaolie33
 */
public class StatementHelper {
    private JdbcDao dao;
    private StringBuilder sb = new StringBuilder();

    public StatementHelper(JdbcDao dao) {
        this.dao = dao;
    }

    public StatementHelper eq(String fieldName, Object value) {
        return express("=", fieldName, value);
    }
    
    public StatementHelper ne(String fieldName, Object value) {
        return express("!=", fieldName, value);
    }
    
    public StatementHelper gt(String fieldName, Object value) {
        return express(">", fieldName, value);
    }

    public StatementHelper ge(String fieldName, Object value) {
        return express(">=", fieldName, value);
    }

    public StatementHelper lt(String fieldName, Object value) {
        return express("<", fieldName, value);
    }

    public StatementHelper le(String fieldName, Object value) {
        return express("<=", fieldName, value);
    }

    public StatementHelper in(String fieldName, Collection fieldValue) {
        sb.append(wrapField(dao.amendFieldName(fieldName))).append(" in (");
        Class clazz = dao.getFieldType(fieldName);
        for (Object v : (Collection) fieldValue) {
            sb.append(dao.whereValueInSql(clazz, v)).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(")");
        return this;
    }

    public StatementHelper and() {
        sb.append(" and ");
        return this;
    }
    public StatementHelper or() {
        sb.append(" or ");
        return this;
    }
    
    private String wrapField(String fieldName) {
        return "`" + fieldName + "`";
    }
    private StatementHelper express(String ope, String fieldName, Object value) {
        Class clazz = dao.getFieldType(fieldName);
        sb.append(wrapField(dao.amendFieldName(fieldName))).append(ope).append(dao.whereValueInSql(clazz, value));
        return this;
    }

    @Override
    public String toString() {
        return sb.toString();
    }
    
    
}
