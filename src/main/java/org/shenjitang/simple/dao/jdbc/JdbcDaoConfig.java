/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.simple.dao.jdbc;

/**
 *
 * @author xiaolie33
 */
public class JdbcDaoConfig {
    public static final String NAME_SPLIT_UNDERLINE = "underline";
    public static final String NAME_SPLIT_CAMEL = "camel";
    public static final String NAME_SPLIT_ORIGINAL = "original";
    private String name = "default";
    private String trueMapping = "1";
    private String falseMapping = "0";
    private String tableNameSplit = NAME_SPLIT_UNDERLINE;
    private String fieldNameSplit = NAME_SPLIT_UNDERLINE;
    private String delMarkFieldName = "deleted";
    private String delMarkFieldValue = "1";
    private static JdbcDaoConfig config;
    
    public static JdbcDaoConfig getConfig() {
        if (config == null) {
            config = new JdbcDaoConfig();
        }
        return config;
    }

    public JdbcDaoConfig() {
        config = this;
    }

    public String getTrueMapping() {
        return trueMapping;
    }

    public void setTrueMapping(String trueMapping) {
        this.trueMapping = trueMapping;
    }

    public String getFalseMapping() {
        return falseMapping;
    }

    public void setFalseMapping(String falseMapping) {
        this.falseMapping = falseMapping;
    }

    public String getTableNameSplit() {
        return tableNameSplit;
    }

    public void setTableNameSplit(String tableNameSplit) {
        if (NAME_SPLIT_UNDERLINE.equalsIgnoreCase(tableNameSplit)) {
            this.tableNameSplit = NAME_SPLIT_UNDERLINE;
        } else if (NAME_SPLIT_CAMEL.equalsIgnoreCase(tableNameSplit)) {
            this.tableNameSplit = NAME_SPLIT_CAMEL;
        } else  if (NAME_SPLIT_ORIGINAL.equalsIgnoreCase(tableNameSplit)) {
            this.tableNameSplit = NAME_SPLIT_ORIGINAL;
        } else {
            throw new RuntimeException ("not suppert split :" + tableNameSplit);
        }
    }

    public String getFieldNameSplit() {
        return fieldNameSplit;
    }

    public void setFieldNameSplit(String fieldNameSplit) {
        if (NAME_SPLIT_UNDERLINE.equalsIgnoreCase(tableNameSplit)) {
            this.fieldNameSplit = NAME_SPLIT_UNDERLINE;
        } else if (NAME_SPLIT_CAMEL.equalsIgnoreCase(tableNameSplit)) {
            this.fieldNameSplit = NAME_SPLIT_CAMEL;
        } else  if (NAME_SPLIT_ORIGINAL.equalsIgnoreCase(tableNameSplit)) {
            this.fieldNameSplit = NAME_SPLIT_ORIGINAL;
        } else {
            throw new RuntimeException ("not suppert split :" + tableNameSplit);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDelMarkFieldName() {
        return delMarkFieldName;
    }

    public void setDelMarkFieldName(String delMarkFieldName) {
        this.delMarkFieldName = delMarkFieldName;
    }

    public String getDelMarkFieldValue() {
        return delMarkFieldValue;
    }

    public void setDelMarkFieldValue(String delMarkFieldValue) {
        this.delMarkFieldValue = delMarkFieldValue;
    }

}
