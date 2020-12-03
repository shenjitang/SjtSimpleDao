/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.simple.dao.jdbc;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang.StringUtils;
import org.shenjitang.simple.dao.PageDataResult;
import static org.shenjitang.simple.dao.jdbc.JdbcDaoConfig.NAME_SPLIT_UNDERLINE;
import org.shenjitang.simple.dao.utils.CamelUnderLineUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author xiaolie33
 */
public class CommonSqlDao <T> {
    private static final Logger logger = LoggerFactory.getLogger(CommonSqlDao.class);
    private SqlBeanParser<T> sqlBeanParser;
    private QueryRunner queryRunner;
    private boolean haveOrderby = false;
    //private List<String> tableList = new ArrayList();
    private JdbcDaoConfig config;
    private StringBuilder whereStr = new StringBuilder();
    private StringBuilder tailStr = new StringBuilder();
    
    private CommonSqlDao(Class<T> clazz, QueryRunner queryRunner) throws SQLException {
        try {
            this.sqlBeanParser = new SqlBeanParser(clazz);
        } catch (Exception e) {
            throw new SQLException(e.getMessage(), e);
        }
        this.queryRunner = queryRunner;
        config = JdbcDaoConfig.getConfig();        
    }
        
    public static <B> CommonSqlDao create(Class<B> clazz, QueryRunner queryRunner) throws SQLException {
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
    
    public CommonSqlDao and(Map map) {
        if (whereStr.length() == 0) {
            whereStr.append(" where ");
        } else {
            whereStr.append(" and ");
        }
        int count = 0;
        for (Object key : map.keySet()) {
            if (count++ > 0) {
                and();
            }
            eq(key.toString(), map.get(key));
        }
        return this;
    }
    
    public CommonSqlDao and(String field, Object value) {
        if (whereStr.length() == 0) {
            whereStr.append(" where ");
        } else if (whereStr.length() > 7) {
            whereStr.append(" and ");
        }
        eq(field, value);
        return this;
    }

    public CommonSqlDao or() {
        whereStr.append(" or ");
        return this;
    }
    
    public CommonSqlDao orderBy(String field) {
        if (!haveOrderby) {
            haveOrderby = true;
            tailStr.append(" order by ");
        } else {
            tailStr.append(",");
        }
        tailStr.append(field);
        return this;
    }
    
    public CommonSqlDao desc() {
        tailStr.append(" desc");
        return this;
    }
    
    public CommonSqlDao limit(Integer limit) {
        tailStr.append(" limit ").append(limit);
        return this;
    }
    
    public CommonSqlDao limit(Integer offset, Integer limit) {
        tailStr.append(" limit ").append(offset).append(", ").append(limit);
        return this;
    }
    
    public CommonSqlDao addTable(String aTable, String alias) {
        sqlBeanParser.addTable(aTable, alias);
        return this;
    }

    private void findLinkedList(T bean) throws SQLException {
        try {
            for (LinkFieldInfo linkInfo : sqlBeanParser.getLinkList()) {
                Object v = BeanUtils.getProperty(bean, linkInfo.getTablePkFieldName());
                Class claz = Class.forName(linkInfo.getType().getTypeName());
                CommonSqlDao dao = CommonSqlDao.create(claz, queryRunner);
                List list = null;
                if (linkInfo.isBridge()) {
                    String bridge = linkInfo.getBridgeTable();
                    list = dao.addTable(bridge, null).where()
                        .link(bridge + "." + linkInfo.getBridgeRight(), dao.getTableName() + "." + linkInfo.getLinkFieldName()).
                        and().eq(bridge + "." + linkInfo.getBridgeLeft(), v).find();
                } else {
                    list = dao.where().eq(linkInfo.getLinkFieldName(), v).find();
                }
                Method write = linkInfo.getPropDesc().getWriteMethod();
                write.invoke(bean, list);
            }
        } catch (Exception e) {
            throw new SQLException("", e);
        }
    }
    
    public List<T> find() throws SQLException {
        BeanListHandler<T> listHandler = new BeanListHandler<>(sqlBeanParser.getClazz());
        String sql = toString();
        logger.debug(sql);
        List<T> list = queryRunner.query(sql, listHandler);
        if (sqlBeanParser.haveLinkList()) {
            for (T t : list) {
                findLinkedList(t);
            }
        }
        return list;
    }
    
    public T findOne() throws SQLException {
        BeanHandler<T> beanHandler = new BeanHandler<>(sqlBeanParser.getClazz());
        String sql = toString();
        logger.debug(sql);
        T bean = queryRunner.query(sql, beanHandler);
        findLinkedList(bean);
        return bean;
    }
    
    public Long count() throws SQLException {
        String sql = sqlBeanParser.toCountString() + whereStr.toString();
        ScalarHandler<Long> countHandler = new ScalarHandler<>("c");
       return queryRunner.query(sql, countHandler);
    }
    
    public PageDataResult<T> findAndCount() throws SQLException {
        Long count = count();
        List<T> list = find();
        return new PageDataResult(count, list);
    }
    
    public void insert(T bean) throws Exception {
        Object id = null;
        boolean haveId = true;
        int size = sqlBeanParser.getFieldList().size();
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
                values[i] = PropertyUtils.getProperty(bean, sqlBeanParser.getField(i).getFieldName());
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
                DbFieldInfo fieldInfo = sqlBeanParser.getField(i);
                if (!"id".equalsIgnoreCase(fieldInfo.getFieldName())) {
                    values[i-find] = PropertyUtils.getProperty(bean, fieldInfo.getFieldName());
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
            sb.append("insert into `").append(getTableName()).append("` (`").append(sqlBeanParser.getField(0).getColumnName()).append("`");
            for (int i = 1; i < sqlBeanParser.getFieldSize(); i++) {
                if (!"id".equalsIgnoreCase(sqlBeanParser.getField(i).getColumnName())) {
                    sb.append(", ").append("`").append(sqlBeanParser.getField(i).getColumnName()).append("`");
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
            sb.append("insert into `").append(getTableName()).append("` (`").append(sqlBeanParser.getField(0).getColumnName()).append("`");
            for (int i = 1; i < sqlBeanParser.getFieldSize(); i++) {
                sb.append(", ").append("`").append(sqlBeanParser.getField(i).getColumnName()).append("`");
                tailSb.append(",?");
            }
            sb.append(") values (").append(tailSb).append(")");
            insertSql = sb.toString();
        }
        return insertSql;
    }
    
    public void delete() throws SQLException {
        String sql = "delete from " + getTableName() + whereStr.toString();
        queryRunner.update(sql);
    }
    
    public void update(T bean) throws Exception {
        if (whereStr.length() > 0) {
            int size = sqlBeanParser.getFieldSize();
            Object[] values = new Object[size];
            for (int i = 0; i < size; i++) {
                values[i] = PropertyUtils.getProperty(bean, sqlBeanParser.getField(i).getFieldName());
            }
            String sql = getUpdateSql() + whereStr.toString();
            logger.debug(sql);
            queryRunner.update(sql, values);
        } else {
            Object id = PropertyUtils.getProperty(bean, "id");
            where().eq("id", id).update(bean);
        }
    }
    
    public void update(String field, String value) throws SQLException {
        DbFieldInfo fInfo = getFieldInfo(field, value.getClass());
        StringBuilder sb = new StringBuilder();
        sb.append("update `").append(getTableName()).append("` set `").append(fInfo.getColumnName())
            .append("`=?");
        String sql = sb.toString() + whereStr.toString();
        logger.debug(sql);
        queryRunner.update(sql, value);
    }

    public void update(Map map) throws SQLException {
        if (whereStr.length() > 0) {
            Object[] values = new Object[map.size()];
            StringBuilder sb = new StringBuilder();
            sb.append("update `").append(getTableName()).append("` set ");
            int count = 0;
            for (Object key : map.keySet()) {
                String field = key.toString();
                Object value = map.get(key);
                values[count] = value;
                if (count++ > 0) {
                    sb.append(" and ");
                }
                DbFieldInfo fInfo = getFieldInfo(field, value.getClass());
                sb.append("`").append(fInfo.getColumnName()).append("`=?");
            }
            String sql = sb.toString() + whereStr.toString();
            logger.debug(sql);
            queryRunner.update(sql, values);
        } else {
            throw new SQLException("update map must be set where statement");
        }
    }
         
    
    protected String getUpdateSql() {
        int size = sqlBeanParser.getFieldSize();
        StringBuilder sb = new StringBuilder();
        sb.append("update `").append(getTableName()).append("` set `").append(sqlBeanParser.getField(0).getColumnName()).append("`=?");
        for (int i = 1; i < size; i++) {
            sb.append(", `").append(sqlBeanParser.getField(i).getColumnName()).append("`=?");
        }
        return sb.toString();
    }
    
    protected String getUpdateSql(String findField) {
        int size = sqlBeanParser.getFieldSize();
        StringBuilder sb = new StringBuilder();
        sb.append("update `").append(getTableName()).append("` set `").append(sqlBeanParser.getField(0).getColumnName()).append("`=?");
        for (int i = 1; i < size; i++) {
            sb.append(", `").append(sqlBeanParser.getField(i).getColumnName()).append("`=?");
        }
        sb.append(" where `").append(findField).append("`=?");
        return sb.toString();
    }
    protected String getUpdateSql(String findField, List fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("update `").append(getTableName()).append("` set `").append(fields.get(0)).append("`=?");
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
    
    public DbFieldInfo getFieldInfo(String name, Class valueClass) {
        for (DbFieldInfo field : sqlBeanParser.getFieldList()) {
            if (field.getFieldName().equals(name) || field.getColumnName().equals(name) || field.getColumnNameInSql().equals(name)) {
                return field;
            }
        }
        String tName = getTableName();
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
        if (SqlBeanParser.numberTypes.contains(type)) {
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
    
    public String getTableName() {
        return sqlBeanParser.getTableName();
    }

    @Override
    public String toString() {
        return sqlBeanParser.toString() + whereStr.toString() + tailStr.toString();
    }
    
    
}
