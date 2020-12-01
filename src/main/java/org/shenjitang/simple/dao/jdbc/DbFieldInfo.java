/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.simple.dao.jdbc;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author xiaolie33
 */
public class DbFieldInfo {
    private String tableName;
    private String fieldName;
    private String columnName;
    private Class fieldType;
    public DbFieldInfo() {
    }

    public DbFieldInfo(String tableName, String columnName, String fieldName, Class fieldType) {
        this.tableName = tableName;
        this.columnName = columnName;
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Class getFieldType() {
        return fieldType;
    }

    public void setFieldType(Class fieldType) {
        this.fieldType = fieldType;
    }
    
    public String name() {
        return "`" + tableName + "`.`" + columnName + "` as `" + fieldName + "`";
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
    
    public String getColumnNameInSql() {
        if (StringUtils.isBlank(tableName)) {
            return columnName;
        } else {
            return tableName + "." + columnName;
        }
    }
    
}
