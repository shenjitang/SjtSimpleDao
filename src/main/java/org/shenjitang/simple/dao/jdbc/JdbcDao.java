/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.shenjitang.simple.dao.jdbc;

import org.shenjitang.simple.dao.BaseDao;
import org.shenjitang.simple.dao.utils.CamelUnderLineUtils;
import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang.StringUtils;
import org.shenjitang.simple.dao.utils.NestedBeanProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *
 * @author xiaolie
 * @param <T>
 */
public abstract class JdbcDao <T> implements BaseDao<T> {
    private static final Logger logger = LoggerFactory.getLogger(JdbcDao.class);
    protected QueryRunner  queryRunner;
    protected String dbName;
    protected String colName;
    protected Class<T> entityClass;
    protected String[] fieldNames;
    protected String[] columnNames;
    protected Map<String,String> columnToPropertyOverrides;
    protected NestedBeanProcessor processor;
    protected String insertSql;
    protected String insertSqlNoId;
    protected BeanListHandler listHandler;
    protected BeanHandler beanHandler;

    public JdbcDao() {
        columnToPropertyOverrides = new HashMap();
        entityClass = getT();
        PropertyDescriptor[] pdArray = PropertyUtils.getPropertyDescriptors(entityClass);
        fieldNames = new String[pdArray.length - 1];
        columnNames = new String[pdArray.length - 1];
        int i = 0;
        for (PropertyDescriptor pd : pdArray) {
            if ("class".equalsIgnoreCase(pd.getName())) {
                continue;
            }
            fieldNames[i] = pd.getName();
            columnNames[i] =  CamelUnderLineUtils.camelToUnderline(fieldNames[i]);
            //columnNames[i] = fieldNames[i];
            columnToPropertyOverrides.put(columnNames[i], fieldNames[i]);
            i++;
        }
        processor = new NestedBeanProcessor(columnToPropertyOverrides);
        listHandler = new BeanListHandler<>(entityClass, new BasicRowProcessor(processor));
        beanHandler = new BeanHandler<>(entityClass, new BasicRowProcessor(processor));
    }

    public QueryRunner getQueryRunner() {
        return queryRunner;
    }

    public void setQueryRunner(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }



    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getColName() {
        if (StringUtils.isBlank(colName)) {
            colName = CamelUnderLineUtils.camelToUnderline(getT().getSimpleName());
        }
        return colName;
    }
    
    protected String getColName(T bean) {
        if (StringUtils.isBlank(colName)) {
            return CamelUnderLineUtils.camelToUnderline(bean.getClass().getSimpleName());
        } else {
            return colName;
        }
        
    }

    public void setColName(String colName) {
        this.colName = colName;
    }
    
    @Override
    public void insert(T bean) throws Exception {
        Object id = null;
        boolean haveId = true;
        try {
            id = PropertyUtils.getProperty(bean, "id");
        } catch (Exception e) {
            logger.info("bean 没有id属性");
            //logger.warn("bean 没有id属性", e);
            haveId = false;
        }
        if (id != null) {
            Object[] values = new Object[fieldNames.length];
            for (int i = 0; i < fieldNames.length; i++) {
                values[i] = PropertyUtils.getProperty(bean, fieldNames[i]);
                if (values[i] instanceof List) {
                    values[i] = processor.getXstream().toXML(values[i]);
                }
            }
            String sql = getInsertSql();
            logger.debug(sql);
            queryRunner.update(sql, values);
        } else {
            int size = fieldNames.length;
            if (haveId) {
                size = fieldNames.length - 1;
            }
            Object[] values = new Object[size];
            int find = 0;
            for (int i = 0; i < fieldNames.length; i++) {
                if (!"id".equalsIgnoreCase(fieldNames[i])) {
                    values[i-find] = PropertyUtils.getProperty(bean, fieldNames[i]);
                    if (values[i-find] instanceof List) {
                        values[i-find] = processor.getXstream().toXML(values[i]);
                    }
                } else {
                    find = 1;
                }
            }
            String sql = getInsertSqlNoId();
            logger.debug(sql);
            queryRunner.update(sql, values);
        }
    } 
    
    @Override
    public void update(T bean) throws Exception {
        Object id = PropertyUtils.getProperty(bean, "id");
        update(bean, "id", id);
    }
    
    @Override
    public void update(T bean, String findFiled, Object value) throws Exception {
        Object[] values = new Object[fieldNames.length + 1];
        for (int i = 0; i < fieldNames.length; i++) {
            values[i] = PropertyUtils.getProperty(bean, fieldNames[i]);
            if (values[i] instanceof List) {
                values[i] = processor.getXstream().toXML(values[i]);
            }
        }
        values[fieldNames.length] = value;
        String sql = getUpdateSql(findFiled);
        logger.debug(sql);
        queryRunner.update(sql, values);
    }

    public void update(T bean, String findFiled, Object value, String findFiled2, Object value2) throws Exception {
        Object[] values = new Object[fieldNames.length + 2];
        for (int i = 0; i < fieldNames.length; i++) {
            values[i] = PropertyUtils.getProperty(bean, fieldNames[i]);
            if (values[i] instanceof List) {
                values[i] = processor.getXstream().toXML(values[i]);
            }
        }
        values[fieldNames.length] = value;
        values[fieldNames.length + 1] = value2;
        String sql = getUpdateSql(findFiled,findFiled2);
        logger.debug(sql);
        queryRunner.update(sql, values);
    }
       
    @Override
    public void update(String sql) throws Exception {
        logger.debug(sql);
        queryRunner.update(sql);
    }
    
    @Override
    public void remove(String key, String value) throws SQLException {
        String sql = "delete from " + getColName() + " where " + key + "='" + value + "'";
        logger.debug(sql);
        queryRunner.update(sql);
    }
    
    public void remove(Map map) throws SQLException {
        String sql = "delete from " + getColName() + " where " + createConditionSegment(map);
        logger.debug(sql);
        queryRunner.update(sql);
    }

    @Override
    public void removeAll() throws SQLException {
        String sql = "delete from " + getColName();
        logger.debug(sql);
        queryRunner.update(sql);
    }

    
    @Override
    public List<T> find(String sql) throws Exception {
        logger.debug(sql);
        return (List<T>) queryRunner.query(sql, listHandler);
    }
    
    @Override
    public List<T> find(String sql, Object... parameters) throws Exception {
        logger.debug(sql);
        return (List<T>) queryRunner.query(sql, listHandler, parameters);
    }

    @Override
    public List<T> find(Map map) throws Exception {
        String sql = "select * from " + getColName() + " where " + createConditionSegment(map);
        return find(sql);
    }
    
    @Override 
    public T get(Object id) throws Exception {
        String sql = "select * from " + getColName() + " where id='" + id + "'"; 
        logger.debug(sql);
        return (T)queryRunner.query(sql, beanHandler);
    }
    
    @Override
    public T findOne(Object id) throws Exception {
        return get(id);
    }
    
    @Override
    public T findOne(String fieldName, Object value) throws Exception {
        String sql = "select * from " + getColName() + " where " + fieldName + "='" + value + "'"; 
        logger.debug(sql);
        return (T)queryRunner.query(sql, beanHandler);
    }

    public T findOne(String fieldName1, Object value1, String fieldName2, Object value2) throws Exception {
        String sql = "select * from " + getColName() + " where " + 
                fieldName1 + "=? and " + fieldName2 + "=?"; 
        logger.debug(sql);
        return (T)queryRunner.query(sql, beanHandler, value1, value2);
    }

    @Override
    public T findOne(Map map) throws Exception {
        String sql = "select * from " + getColName() + " where " + createConditionSegment(map);
        return findOne(sql);
    }
    
    @Override
    public T findOne(String sql) throws Exception {
        logger.debug(sql);
        if (sql.toLowerCase().startsWith("select ")) {
            return (T)queryRunner.query(sql, beanHandler);
        } else {
            String rsql = "select * from " + getColName() + " where id='" + sql + "'"; 
            logger.debug(rsql);
            return (T)queryRunner.query(rsql, beanHandler);
        }     
    }

    public List<T> findAll() throws Exception {
        String sql = "select * from " + getColName();
        logger.debug(sql);
        return (List<T>) queryRunner.query(sql, listHandler);
    }

    @Override
    public Long count() throws SQLException{
        ScalarHandler<Long> countHandler = new ScalarHandler<>("c");
        String sql = "select count(*) as c from " + getColName();
        logger.debug(sql);
        return queryRunner.query(sql, countHandler);
    }

    @Override
    public Long count(Map map) throws SQLException{
        ScalarHandler<Long> countHandler = new ScalarHandler<>("c");
        String sql = "select count(*) as c from " + getColName() + " where " + createConditionSegment(map);
        logger.debug(sql);
        return queryRunner.query(sql, countHandler);
    }
    
    public Long count(String field, Object value) throws SQLException {
        ScalarHandler<Long> countHandler = new ScalarHandler<>("c");
        String sql = "select count(*) as c from " + getColName() + " where " + field + "=?";
        logger.debug(sql);
        return queryRunner.query(sql, countHandler, value);
    }

    public Long count(String field1, Object value1, String field2, Object value2) throws SQLException {
        ScalarHandler<Long> countHandler = new ScalarHandler<>("c");
        String sql = "select count(*) as c from " + getColName() + " where " + field1 + "=? and " + field2 + "=?";
        logger.debug(sql);
        return queryRunner.query(sql, countHandler, value1, value2);
    }

    protected List<T> exchangeList(List<Map> list) throws Exception {
        List<T> returnList = new ArrayList();
        for (Map map : list) {
            T bean = (T)getT().newInstance();
            BeanUtils.populate(bean, map);
            returnList.add(bean);
        }
        return returnList;
    }
    
    public final Class getT() {
        Type genType = getClass().getGenericSuperclass();  
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();  
        return (Class) params[0];  
    }
    
    protected String getInsertSql() {
        if (insertSql == null) {
            StringBuilder sb = new StringBuilder();
            StringBuilder tailSb = new StringBuilder("?");
            sb.append("insert into ").append(getColName()).append(" (`").append(columnNames[0]).append("`");
            for (int i = 1; i < columnNames.length; i++) {
                sb.append(", ").append("`").append(columnNames[i]).append("`");
                tailSb.append(",?");
            }
            sb.append(") values (").append(tailSb).append(")");
            insertSql = sb.toString();
        }
        return insertSql;
    }
    
    protected String getInsertSqlNoId() {
        if (insertSqlNoId == null) {
            StringBuilder sb = new StringBuilder();
            StringBuilder tailSb = new StringBuilder("?");
            sb.append("insert into ").append(getColName()).append(" (`").append(columnNames[0]).append("`");
            for (int i = 1; i < columnNames.length; i++) {
                if (!"id".equalsIgnoreCase(columnNames[i])) {
                    sb.append(", ").append("`").append(columnNames[i]).append("`");
                    tailSb.append(",?");
                }
            }
            sb.append(") values (").append(tailSb).append(")");
            insertSqlNoId = sb.toString();
        }
        return insertSqlNoId;
    }

    protected String getUpdateSql(String findField) {
        StringBuilder sb = new StringBuilder();
        sb.append("update ").append(getColName()).append(" set ").append(columnNames[0]).append("=?");
        for (int i = 1; i < columnNames.length; i++) {
            sb.append(", ").append(columnNames[i]).append("=?");
        }
        sb.append(" where ").append(findField).append("=?");
        return sb.toString();
    }

    protected String getUpdateSql(String findField, String findField2) {
        StringBuilder sb = new StringBuilder();
        sb.append("update ").append(getColName()).append(" set ").append(columnNames[0]).append("=?");
        for (int i = 1; i < columnNames.length; i++) {
            sb.append(", ").append(columnNames[i]).append("=?");
        }
        sb.append(" where ").append(findField).append("=? and ").append(findField2).append("=?");
        return sb.toString();
    }

    protected String createConditionSegment(Map map) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Object key : map.keySet()) {
            if (i++ > 0) {
                sb.append(" and ");
            }
            sb.append(key.toString()).append("='").append(map.get(key).toString()).append("'");
        }
        return sb.toString();
    }
}
