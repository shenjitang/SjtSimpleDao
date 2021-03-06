/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.simple.dao.jdbc;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author xiaolie33
 */
public class LinkFieldInfo {
    private Type type;  //表名
    private Field field;
    private PropertyDescriptor propDesc;
    private String linkFieldName; //本表的外健字段
    private String tablePkFieldName; //连接表的主键字段
    private String bridge; //中间表连接。
    private String bridgeTable;
    private String bridgeLeft;
    private String bridgeRight;

    public LinkFieldInfo() {
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }


    public String getLinkFieldName() {
        return linkFieldName;
    }

    public void setLinkFieldName(String linkFieldName) {
        this.linkFieldName = linkFieldName;
    }

    public String getTablePkFieldName() {
        return tablePkFieldName;
    }

    public void setTablePkFieldName(String tablePkFieldName) {
        this.tablePkFieldName = tablePkFieldName;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public PropertyDescriptor getPropDesc() {
        return propDesc;
    }

    public void setPropDesc(PropertyDescriptor propDesc) {
        this.propDesc = propDesc;
    }

    public String getBridge() {
        return bridge;
    }

    public void setBridge(String bridge) {
        this.bridge = bridge;
        String[] a = bridge.split(":");
        bridgeLeft = a[0];
        bridgeTable = a[1];
        bridgeRight = a[2];
    }

    public String getBridgeTable() {
        return bridgeTable;
    }

    public String getBridgeLeft() {
        return bridgeLeft;
    }

    public String getBridgeRight() {
        return bridgeRight;
    }
    
    public boolean isBridge() {
        return StringUtils.isNoneBlank(bridge);
    }

}
