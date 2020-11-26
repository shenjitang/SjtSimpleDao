/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.simple.dao.jdbc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author xiaolie33
 */
public class StatementHelper {
    private JdbcDao dao;
    private StringBuilder sb = new StringBuilder();
    private List<String> tableAliasList = new ArrayList();
    private boolean haveOrderby = false;

    private StatementHelper(JdbcDao dao) {
        this.dao = dao;
    }
    
    public static StatementHelper create(JdbcDao dao) {
        return new StatementHelper(dao);
    }
    
    public StatementHelper select(String... fields) {
        sb.append("select ");
        if (fields.length > 0) {
            sb.append(StringUtils.join(fields, ","));
        } else {
            sb.append ("*");
        }
        return this;
    }
    
    public StatementHelper from (String table) {
        String name = table;
        if (tableAliasList.isEmpty()) {
            sb.append(" from ");
        } else {
            sb.append(" ," + name);
        }
        sb.append(name);
        tableAliasList.add(name);
        return this;
    }
    
    public StatementHelper from (String table, String alias) {
        String name = table;
        if (tableAliasList.isEmpty()) {
            sb.append(" from ");
        } else {
            sb.append(", ");
        }
        sb.append(name + " " + alias);
        tableAliasList.add(alias);
        return this;
    }
    
    public StatementHelper innerJoin(String table, String alias, String onField1, String onField2) {
        String mainTable = tableAliasList.get(0);
        tableAliasList.add(alias);
        sb.append(" inner join " + table + " " + alias);
        sb.append(" on " + mainTable + "." + onField1 + "=" + alias + "." + onField2);
        return this;
    }

    public StatementHelper leftJoin(String table, String alias, String onField1, String onField2) {
        String mainTable = tableAliasList.get(0);
        tableAliasList.add(alias);
        sb.append(" left join " + table + " " + alias);
        sb.append(" on " + mainTable + "." + onField1 + "=" + alias + "." + onField2);
        return this;
    }
    
    public StatementHelper rightJoin(String table, String alias, String onField1, String onField2) {
        String mainTable = tableAliasList.get(0);
        tableAliasList.add(alias);
        sb.append(" right join " + table + " " + alias);
        sb.append(" on " + mainTable + "." + onField1 + "=" + alias + "." + onField2);
        return this;
    }
    
    public StatementHelper where() {
        sb.append(" where ");
        return this;
    }
    
    public StatementHelper leftParentheses() {
        sb.append("(");
        return this;
    }

    public StatementHelper rightParentheses() {
        sb.append(")");
        return this;
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
        sb.append(wrapField(fieldName)).append(" in (");
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
    
    public StatementHelper orderBy(String field) {
        if (!haveOrderby) {
            sb.append(" order by ");
        } else {
            sb.append(",");
        }
        sb.append(field);
        return this;
    }
    
    public StatementHelper desc() {
        sb.append(" desc");
        return this;
    }
    
    public StatementHelper limit(Integer limit) {
        sb.append(" limit ").append(limit);
        return this;
    }
    
    public StatementHelper limit(Integer limit1, Integer limit2) {
        sb.append(" limit ").append(limit1).append(", ").append(limit2);
        return this;
    }
    private String wrapField(String fieldName) {
        return "`" + fieldName + "`";
    }
    private StatementHelper express(String ope, String fieldName, Object value) {
        Class clazz = dao.getFieldType(fieldName);
        sb.append(wrapField(fieldName)).append(ope).append(dao.whereValueInSql(clazz, value));
        return this;
    }
    
    public JdbcDao getDao() {
        return dao;
    }

    @Override
    public String toString() {
        return sb.toString();
    }
    
    
}
