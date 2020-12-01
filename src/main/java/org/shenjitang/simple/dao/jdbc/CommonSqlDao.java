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
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.BeanProcessor;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.shenjitang.simple.dao.PageDataResult;
import org.shenjitang.simple.dao.annotation.DbField;
import org.shenjitang.simple.dao.annotation.DbJoin;
import org.shenjitang.simple.dao.annotation.DbJoins;
import org.shenjitang.simple.dao.annotation.DbLink;
import org.shenjitang.simple.dao.annotation.DbNoMap;
import static org.shenjitang.simple.dao.jdbc.JdbcDaoConfig.NAME_SPLIT_UNDERLINE;
import org.shenjitang.simple.dao.utils.CamelUnderLineUtils;
import org.shenjitang.simple.dao.annotation.DbTable;
import org.shenjitang.simple.dao.annotation.DbTables;
import org.shenjitang.simple.dao.utils.NestedBeanProcessor;
import org.shenjitang.simple.dao.utils.SimpleBeanProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author xiaolie33
 */
public class CommonSqlDao <T> {
    private static final Logger logger = LoggerFactory.getLogger(CommonSqlDao.class);
    private StringBuilder selectStr = new StringBuilder();
    private StringBuilder insertStr = new StringBuilder();
    private StringBuilder deleteStr = new StringBuilder();
    private StringBuilder updateStr = new StringBuilder();
    private Class<T> clazz;
    private QueryRunner queryRunner;
    private boolean haveOrderby = false;
    //private List<String> tableList = new ArrayList();
    private String tableName = null;
    private List<DbFieldInfo> fieldList = new ArrayList();
    private List<LinkFieldInfo> linkList = new ArrayList();
    private JdbcDaoConfig config;
    private StringBuilder joinStr = new StringBuilder();
    private StringBuilder fromStr = new StringBuilder();
    private StringBuilder whereStr = new StringBuilder();
    private static Set<String> numberTypes;
    private Field[] fields = null;
    private List<PropertyDescriptor> pds = new ArrayList();
    //private Set<String> tableAlias = new HashSet();
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
    
    private CommonSqlDao(Class<T> clazz, QueryRunner queryRunner) throws Exception {
        this.queryRunner = queryRunner;
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
        BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
        PropertyDescriptor[] pda = beanInfo.getPropertyDescriptors();
        for (PropertyDescriptor pd : pda) {
            //logger.debug("property getDisplayName:" + pd.getDisplayName() + " getName:" + pd.getName() + " getShortDescription:" + pd.getShortDescription());
            if (!pd.getName().equalsIgnoreCase("class")) {
                pds.add(pd);
            }
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
            Field field = clazz.getDeclaredField(pd.getName());
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
        //selectStr.append(fromStr).append(" ").append(joinStr);
        
    }
    
    public CommonSqlDao addTable(String aTable, String alias) {
        fromStr.append(" ,").append(wrapField(aTable));
        if (StringUtils.isNotBlank(alias)) {
            fromStr.append(" ").append(wrapField(alias));
        }
        return this;
    }
    
    public static <B> CommonSqlDao create(Class<B> clazz, QueryRunner queryRunner) throws Exception {
        return new CommonSqlDao(clazz, queryRunner);
    }
    
    public CommonSqlDao where() {
        whereStr.append(" where ");
        return this;
    }
    
    public CommonSqlDao leftParentheses() {
        whereStr.append("(");
        return this;
    }

    public CommonSqlDao rightParentheses() {
        whereStr.append(")");
        return this;
    }

    public CommonSqlDao eq(String fieldName, Object value) {
        return express("=", fieldName, value);
    }
    public CommonSqlDao link(String fieldName, String fieldName2) {
        DbFieldInfo info = getFieldInfo(fieldName, fieldName2.getClass());
        whereStr.append(wrapField(info.getColumnNameInSql())).append("=").append(wrapField(fieldName2));
        return this;
    }
    
    public CommonSqlDao ne(String fieldName, Object value) {
        return express("!=", fieldName, value);
    }
    
    public CommonSqlDao gt(String fieldName, Object value) {
        return express(">", fieldName, value);
    }

    public CommonSqlDao ge(String fieldName, Object value) {
        return express(">=", fieldName, value);
    }

    public CommonSqlDao lt(String fieldName, Object value) {
        return express("<", fieldName, value);
    }

    public CommonSqlDao le(String fieldName, Object value) {
        return express("<=", fieldName, value);
    }

    public CommonSqlDao in(String fieldName, Collection fieldValue) {
        whereStr.append(wrapField(fieldName)).append(" in (");
        Class clazz = getFieldInfo(fieldName, fieldValue.iterator().next().getClass()).getFieldType();
        for (Object v : (Collection) fieldValue) {
            whereStr.append(whereValueInSql(clazz, v)).append(",");
        }
        if (whereStr.charAt(whereStr.length() - 1) == ',') {
            whereStr.deleteCharAt(whereStr.length() - 1);
        }
        whereStr.append(")");
        return this;
    }
    
    public CommonSqlDao and() {
        whereStr.append(" and ");
        return this;
    }
    public CommonSqlDao or() {
        whereStr.append(" or ");
        return this;
    }
    
    public CommonSqlDao orderBy(String field) {
        if (!haveOrderby) {
            whereStr.append(" order by ");
        } else {
            whereStr.append(",");
        }
        whereStr.append(field);
        return this;
    }
    
    public CommonSqlDao desc() {
        whereStr.append(" desc");
        return this;
    }
    
    public CommonSqlDao limit(Integer limit) {
        whereStr.append(" limit ").append(limit);
        return this;
    }
    
    public CommonSqlDao limit(Integer limit1, Integer limit2) {
        whereStr.append(" limit ").append(limit1).append(", ").append(limit2);
        return this;
    }
    
    private void findLinkedList(T bean) throws SQLException {
        try {
            for (LinkFieldInfo linkInfo : linkList) {
                Object v = BeanUtils.getProperty(bean, linkInfo.getTablePkFieldName());
                Class claz = Class.forName(linkInfo.getType().getTypeName());
                CommonSqlDao dao = CommonSqlDao.create(claz, queryRunner);
                List list = null;
                if (linkInfo.isBridge()) {
                    String bridge = linkInfo.getBridgeTable();
                    list = dao.addTable(bridge, null).where()
                        .link(bridge + "." + linkInfo.getBridgeRight(), dao.tableName + "." + linkInfo.getLinkFieldName()).
                        and().eq(bridge + "." + linkInfo.getBridgeLeft(), v).find();
                } else {
                    list = dao.where().eq(linkInfo.getLinkFieldName(), v).find();
                }
                Method write = linkInfo.getPropDesc().getWriteMethod();
                write.invoke(bean, list);
                //linkInfo.getField().set(bean, list); 
                /*
                if (claz.isAnnotationPresent(DbTable.class)) {
                    DbTable tableAnno = clazz.getAnnotation(DbTable.class);
                    tn = tableAnno.value();
                }
                String sql = "select * from " + tn + " where " + linkInfo.getLinkFieldName() + "='" + v + "'";
                BeanListHandler listHandler = new BeanListHandler<>(Class.forName(tn));
                list.addAll((List1)queryRunner.query(sql, listHandler));
                */
            }
        } catch (Exception e) {
            throw new SQLException("", e);
        }
    }
    
    public List<T> find() throws SQLException {
        //BeanProcessor processor = new SimpleBeanProcessor();
        //BeanListHandler<T> listHandler = new BeanListHandler<>(clazz, new BasicRowProcessor(processor));
        BeanListHandler<T> listHandler = new BeanListHandler<>(clazz);
        String sql = selectStr.toString() + fromStr + " " + joinStr + whereStr.toString();
        logger.debug(sql);
        List<T> list = queryRunner.query(sql, listHandler);
        if (linkList.size() > 0) {
            for (T t : list) {
                findLinkedList(t);
            }
        }
        return list;
    }
    
    public T findOne() throws SQLException {
        //BeanProcessor processor = new SimpleBeanProcessor();
        //BeanHandler<T> beanHandler = new BeanHandler<>(clazz, new BasicRowProcessor(processor));
        BeanHandler<T> beanHandler = new BeanHandler<>(clazz);
        String sql = selectStr.toString() + fromStr + " " + joinStr + whereStr.toString();
        logger.debug(sql);
        T bean = queryRunner.query(sql, beanHandler);
        findLinkedList(bean);
        return bean;
    }
    
    public PageDataResult<T> findAndCount() throws SQLException {
        String sql = selectStr.toString() + fromStr + " " + joinStr + whereStr.toString();
        String countSql = "select count(*) as c from " + StringUtils.substringAfter(sql, " from ");
        ScalarHandler<Long> countHandler = new ScalarHandler<>("c");
        Long count = queryRunner.query(countSql, countHandler);
        List<T> list = find();
        return new PageDataResult(count, list);
    }
    
    public void insert(T bean) throws Exception {
        Object id = null;
        boolean haveId = true;
        int size = fieldList.size();
        try {
            id = PropertyUtils.getProperty(bean, "id");
        } catch (Exception e) {
            logger.info("bean 没有id属性");
            //logger.warn("bean 没有id属性", e);
            haveId = false;
        }
        if (id != null) {
            Object[] values = new Object[size];
            for (int i = 0; i < size; i++) {
                values[i] = PropertyUtils.getProperty(bean, fieldList.get(i).getFieldName());
            }
            String sql = getInsertSql();
            StringBuilder sb = new StringBuilder();
            for (Object v : values) {
                sb.append(v).append(" ");
            }
            logger.debug(sql + " \n params: " + sb.toString());
            queryRunner.update(sql, values);
        } else { //bean中的id是null
            Object[] values = haveId?new Object[size-1]:new Object[size];
            int find = 0;
            for (int i = 0; i < size; i++) {
                if (!"id".equalsIgnoreCase(fieldList.get(i).getFieldName())) {
                    values[i-find] = PropertyUtils.getProperty(bean, fieldList.get(i).getFieldName());
                } else {
                    find = 1;
                }
            }
            String sql = getInsertSqlNoId();
            StringBuilder sb = new StringBuilder();
            for (Object v : values) {
                sb.append(v).append(" ");
            }
            logger.debug(sql + " \n params: " + sb.toString());
            queryRunner.update(sql, values);
            if (find == 1) { //表中有id字段
                String sql2 = "select @@identity";
                Object idd = queryRunner.query(sql2,new ScalarHandler(1)); //获得自增长id，类型是BigInteger
                PropertyDescriptor pd = PropertyUtils.getPropertyDescriptor(bean, "id");
                Class idType = pd.getPropertyType();
                Object iddd = idType.getMethod("valueOf", String.class).invoke(null, idd.toString());//转换成Bean中id的类型
                PropertyUtils.setProperty(bean, "id", iddd);
                logger.debug("ID：" + PropertyUtils.getProperty(bean, "id"));
            }
        }
    }
    private String insertSqlNoId;
    protected String getInsertSqlNoId() {
        if (insertSqlNoId == null) {
            StringBuilder sb = new StringBuilder();
            StringBuilder tailSb = new StringBuilder("?");
            sb.append("insert into `").append(tableName).append("` (`").append(fieldList.get(0).getColumnName()).append("`");
            for (int i = 1; i < fieldList.size(); i++) {
                if (!"id".equalsIgnoreCase(fieldList.get(i).getColumnName())) {
                    sb.append(", ").append("`").append(fieldList.get(i).getColumnName()).append("`");
                    tailSb.append(",?");
                }
            }
            sb.append(") values (").append(tailSb).append(")");
            insertSqlNoId = sb.toString();
        }
        return insertSqlNoId;
    }    
    private String insertSql;
    protected String getInsertSql() {
        if (insertSql == null) {
            StringBuilder sb = new StringBuilder();
            StringBuilder tailSb = new StringBuilder("?");
            sb.append("insert into `").append(tableName).append("` (`").append(fieldList.get(0).getColumnName()).append("`");
            for (int i = 1; i < fieldList.size(); i++) {
                sb.append(", ").append("`").append(fieldList.get(i).getColumnName()).append("`");
                tailSb.append(",?");
            }
            sb.append(") values (").append(tailSb).append(")");
            insertSql = sb.toString();
        }
        return insertSql;
    }
    
    public void delete() throws Exception {
        String sql = "delete from " + tableName + whereStr.toString();
        queryRunner.update(sql);
    }
    
    public void update(T bean) throws Exception {
        if (whereStr.length() > 0) {
            int size = fieldList.size();
            Object[] values = new Object[size];
            for (int i = 0; i < size; i++) {
                values[i] = PropertyUtils.getProperty(bean, fieldList.get(i).getFieldName());
            }
            String sql = getUpdateSql() + whereStr.toString();
            logger.debug(sql);
            queryRunner.update(sql, values);
        } else {
            Object id = PropertyUtils.getProperty(bean, "id");
            update(bean, "id", id);
        }
    }
        
    protected void update(T bean, String findFiled, Object value) throws Exception {
        int size = fieldList.size();
        Object[] values = new Object[size + 1];
        for (int i = 0; i < size; i++) {
            values[i] = PropertyUtils.getProperty(bean, fieldList.get(i).getFieldName());
        }
        values[size] = value;
        String sql = getUpdateSql(findFiled);
        logger.debug(sql);
        queryRunner.update(sql, values);
    } 
    
    protected String getUpdateSql() {
        int size = fieldList.size();
        StringBuilder sb = new StringBuilder();
        sb.append("update `").append(tableName).append("` set `").append(fieldList.get(0).getColumnName()).append("`=?");
        for (int i = 1; i < size; i++) {
            sb.append(", `").append(fieldList.get(i).getColumnName()).append("`=?");
        }
        return sb.toString();
    }
    
    protected String getUpdateSql(String findField) {
        int size = fieldList.size();
        StringBuilder sb = new StringBuilder();
        sb.append("update `").append(tableName).append("` set `").append(fieldList.get(0).getColumnName()).append("`=?");
        for (int i = 1; i < size; i++) {
            sb.append(", `").append(fieldList.get(i).getColumnName()).append("`=?");
        }
        sb.append(" where `").append(findField).append("`=?");
        return sb.toString();
    }
    protected String getUpdateSql(String findField, List fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("update `").append(tableName).append("` set `").append(fields.get(0)).append("`=?");
        for (int i = 1; i < fields.size(); i++) {
            sb.append(", `").append(fields.get(i)).append("`=?");
        }
        sb.append(" where `").append(findField).append("`=?");
        return sb.toString();
    }
    
    private String wrapField(String fieldName) {
        if(fieldName.contains(".")) {
            String[] array = fieldName.split("[.]");
            return "`" + array[0] + "`.`" + array[1] + "`";
        } else {
            return "`" + fieldName + "`";
        }
    }
    
    private CommonSqlDao express(String ope, String fieldName, Object value) {
        DbFieldInfo info = getFieldInfo(fieldName, value.getClass());
        whereStr.append(wrapField(info.getColumnNameInSql())).append(ope).append(whereValueInSql(info.getFieldType(), value));
        return this;
    }
    
    private void addJoinStr(DbJoin joinAnno) {
        String table = joinAnno.table();
        joinStr.append(joinAnno.type().value()).append(" ").append(table);
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

    private String whereValueInSql(Class clazz, Object fieldValue) {
        String type = clazz.getName();
        if (numberTypes.contains(type)) {
            return fieldValue.toString();
        } else if ("boolean".equals(type) || "java.lang.Boolean".equals(type)) {
            if (Boolean.valueOf(fieldValue.toString())) {
                return "1";
            } else {
                return "0";
            }
        } else {
            return "'" + fieldValue.toString() + "'";
        }
    } 

    @Override
    public String toString() {
        return selectStr.toString();
    }
    
    
}
