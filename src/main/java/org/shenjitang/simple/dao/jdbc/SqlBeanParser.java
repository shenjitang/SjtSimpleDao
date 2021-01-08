/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.simple.dao.jdbc;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.shenjitang.simple.dao.annotation.DbField;
import org.shenjitang.simple.dao.annotation.DbJoin;
import org.shenjitang.simple.dao.annotation.DbJoins;
import org.shenjitang.simple.dao.annotation.DbLink;
import org.shenjitang.simple.dao.annotation.DbNoMap;
import org.shenjitang.simple.dao.annotation.DbTable;
import org.shenjitang.simple.dao.annotation.DbTables;
import static org.shenjitang.simple.dao.jdbc.JdbcDaoConfig.NAME_SPLIT_UNDERLINE;
import org.shenjitang.simple.dao.utils.CamelUnderLineUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author xiaolie33
 */
public class SqlBeanParser <T> {
    private static final Logger logger = LoggerFactory.getLogger(SqlBeanParser.class);
    private StringBuilder selectStr = new StringBuilder();
    private StringBuilder joinStr = new StringBuilder();
    private StringBuilder fromStr = new StringBuilder();
    private Class<T> clazz;
    private String tableName = null;
    private List<DbFieldInfo> fieldList = new ArrayList();
    private Set<String> columnNameSet = new HashSet();
    private Set<String> fieldNameSet = new HashSet();
    private List<LinkFieldInfo> linkList = new ArrayList();
    private JdbcDaoConfig config;
    private Field[] fields = null;
    private List<PropertyDescriptor> pds = new ArrayList();
    //private Set<String> tableAlias = new HashSet();
    public final static Set<String> numberTypes;
    static {
        numberTypes = new HashSet();
        numberTypes.add("int");
        numberTypes.add("long");
        numberTypes.add("flot");
        numberTypes.add("double");
        numberTypes.add("java.lang.Integer");
        numberTypes.add("java.lang.Long");
        numberTypes.add("java.lang.Float");
        numberTypes.add("java.lang.Double");
    }
    
    public SqlBeanParser(Class<T> clazz) throws SQLException {
        this.clazz = clazz;
        config = JdbcDaoConfig.getConfig();
        //判断person对象上是否有DbTables注解
        if (clazz.isAnnotationPresent(DbTables.class)) {
            DbTables tablesAnno = clazz.getAnnotation(DbTables.class);
            DbTable[] tableAnnos = tablesAnno.value();
            tableName = tableAnnos[0].value();
            fromStr.append(" from ").append(wrapField(tableName));
            if (StringUtils.isNotBlank(tableAnnos[0].alias())) {
                tableName = tableAnnos[0].alias();
                fromStr.append(" ").append(wrapField(tableName));
            }
            //tableAlias.add(tableName);
            for (int i = 1; i < tableAnnos.length; i++) {
                fromStr.append(" ,").append(wrapField(tableAnnos[i].value()));
                String tName = tableAnnos[i].value();
                if (StringUtils.isNotBlank(tableAnnos[i].alias())) {
                    tName = tableAnnos[i].alias();
                    fromStr.append(" ").append(wrapField(tName));
                }
                //tableAlias.add(tName);
            }
        } else if (clazz.isAnnotationPresent(DbTable.class)) {
            //获取该对象上Info类型的注解
            DbTable tableAnno = clazz.getAnnotation(DbTable.class);
            tableName = tableAnno.value();
            fromStr.append(" from ").append(wrapField(tableName));
            if (StringUtils.isNotBlank(tableAnno.alias())) {
                tableName = tableAnno.alias();
                fromStr.append(" ").append(wrapField(tableName));
            }
            //tableAlias.add(tableName);
        } else {
            if (config.getTableNameSplit() == NAME_SPLIT_UNDERLINE) {
                tableName = CamelUnderLineUtils.camelToUnderline(clazz.getSimpleName());
            } else {
                tableName = clazz.getSimpleName();
            }
            fromStr.append(" from ").append(wrapField(tableName));
        }
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
            PropertyDescriptor[] pda = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor pd : pda) {
                //logger.debug("property getDisplayName:" + pd.getDisplayName() + " getName:" + pd.getName() + " getShortDescription:" + pd.getShortDescription());
                if (!pd.getName().equalsIgnoreCase("class")) {
                    pds.add(pd);
                }
            }
        } catch (IntrospectionException e) {
            throw new SQLException(e.getMessage(), e);
        }
        
        if (clazz.isAnnotationPresent(DbJoins.class)) {
            DbJoins joinsAnno = clazz.getAnnotation(DbJoins.class);
            DbJoin[] joinAnnos = joinsAnno.value();
            for (DbJoin joinAnno : joinAnnos) {
                addJoinStr(joinAnno);
            }
        } else if (clazz.isAnnotationPresent(DbJoin.class)) {
            DbJoin joinAnno = clazz.getAnnotation(DbJoin.class);
            addJoinStr(joinAnno);
        }

        for (PropertyDescriptor pd : pds){
            String aTable = tableName;
            String columnName = null;
            Field field = null;
            try {
                field = clazz.getDeclaredField(pd.getName());
            } catch (NoSuchFieldException e) {
                throw new SQLException(e.getMessage(), e);
            }
            if (field.isAnnotationPresent(DbField.class)) {
                DbField fieldAnno = field.getAnnotation(DbField.class);
                if (StringUtils.isNotBlank(fieldAnno.value())) {
                    columnName = fieldAnno.value();
                } else {
                    columnName = field.getName();
                }
                if (StringUtils.isNotBlank(fieldAnno.table())) {
                    aTable = fieldAnno.table();
                }
                DbFieldInfo fieldInfo = new DbFieldInfo(aTable, columnName, field.getName(), field.getType());
                columnNameSet.add(columnName);
                fieldNameSet.add(field.getName());
                fieldList.add(fieldInfo);
            } else if (field.isAnnotationPresent(DbLink.class)){
                DbLink linkAnno = field.getAnnotation(DbLink.class);
                LinkFieldInfo info = new LinkFieldInfo();
                info.setField(field);
                info.setPropDesc(pd);
                info.setLinkFieldName(linkAnno.value());
                info.setTablePkFieldName(linkAnno.thisField());
                if (StringUtils.isNotBlank(linkAnno.bridge())) {
                    info.setBridge(linkAnno.bridge());
                }
                ParameterizedType type = (ParameterizedType) field.getGenericType();
                Type ctype = type.getActualTypeArguments()[0];
                info.setType(ctype);
                linkList.add(info);
            } else if (field.isAnnotationPresent(DbNoMap.class)){
            } else {
                if (config.getFieldNameSplit() == NAME_SPLIT_UNDERLINE) {
                    columnName = CamelUnderLineUtils.camelToUnderline(field.getName());
                } else {
                    columnName = field.getName();
                }
                DbFieldInfo fieldInfo = new DbFieldInfo(aTable, columnName, field.getName(), field.getType());
                columnNameSet.add(columnName);
                fieldNameSet.add(field.getName());
                fieldList.add(fieldInfo);
            }
        }   
        
        selectStr.append("select ");
        int c = 0;
        for (DbFieldInfo fieldInfo : fieldList) {
            if (c++ > 0) {
                selectStr.append(", ");
            }
            selectStr.append(fieldInfo.name());
        }
    }        
    
    private String wrapField(String fieldName) {
        if(fieldName.contains(".")) {
            String[] array = fieldName.split("[.]");
            return "`" + array[0] + "`.`" + array[1] + "`";
        } else {
            return "`" + fieldName + "`";
        }
    }
    
    private void addJoinStr(DbJoin joinAnno) {
        String table = joinAnno.table();
        joinStr.append(" ").append(joinAnno.type().value()).append(" ").append(table);
        String alias = joinAnno.alias();
        if (StringUtils.isNotBlank(alias)) {
            joinStr.append(" ").append(alias);
            table = alias;
        }
        //tableAlias.add(table);
        String on = joinAnno.on();
        if (!on.contains(".")) {
            on = table + "." + on;
        }
        String eq = joinAnno.eq();
        if (!eq.contains(".")) {
            eq = tableName + "." + eq;
        }
        
        joinStr.append(" on ")
            .append(on).append("=")
            .append(eq).append(" ");
    }

    public DbFieldInfo getFieldInfo(String name, Class valueClass) {
        for (DbFieldInfo field : fieldList) {
            if (field.getFieldName().equals(name) || field.getColumnName().equals(name) || field.getColumnNameInSql().equals(name)) {
                return field;
            }
        }
        String tName = tableName;
        if (name.contains(".")) {
            tName = StringUtils.substringBefore(name, ".");
            name = StringUtils.substringAfter(name, ".");
        }
        String columnName = name;
        if (config.getFieldNameSplit() == NAME_SPLIT_UNDERLINE) {
            columnName = CamelUnderLineUtils.camelToUnderline(name);
        }
        return new DbFieldInfo(tName, columnName, name, valueClass);
    }
    
    public void addTable(String aTable, String alias) {
        fromStr.append(" ,").append(wrapField(aTable));
        if (StringUtils.isNotBlank(alias)) {
            fromStr.append(" ").append(wrapField(alias));
        }
    }

    @Override
    public String toString() {
        return selectStr.toString() + fromStr.toString() + joinStr.toString();
    }
    
    public String toCountString() {
        return "select count(*) as c " + fromStr.toString() + joinStr.toString();
    }

    public Class<T> getClazz() {
        return clazz;
    }

    public List<LinkFieldInfo> getLinkList() {
        return linkList;
    }
    
    public boolean haveLinkList() {
        return !linkList.isEmpty();
    }

    public List<DbFieldInfo> getFieldList() {
        return fieldList;
    }
    
    public DbFieldInfo getField(int i) {
        return fieldList.get(i);
    }
    
    public int getFieldSize() {
        return fieldList.size();
    }

    public String getTableName() {
        return tableName;
    }
    
    public boolean containsColumn(String name) {
        return columnNameSet.contains(name);
    }
    
    public boolean containsField(String name) {
        return fieldNameSet.contains(name);
    }
}
