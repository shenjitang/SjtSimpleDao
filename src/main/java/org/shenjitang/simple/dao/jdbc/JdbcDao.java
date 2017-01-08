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
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang.StringUtils;
import org.shenjitang.simple.dao.utils.NestedBeanProcessor;

/**
 *
 * @author xiaolie
 * @param <T>
 */
public abstract class JdbcDao <T> implements BaseDao<T> {
    protected QueryRunner  queryRunner;
    protected String dbName;
    protected String colName;
    protected Class<T> entityClass;
    protected String[] fieldNames;
    protected String[] columnNames;
    protected Map<String,String> columnToPropertyOverrides;
    protected NestedBeanProcessor processor;
    protected String insertSql;
    protected BeanListHandler listHandler;

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
        Object[] values = new Object[fieldNames.length];
        for (int i = 0; i < fieldNames.length; i++) {
            values[i] = PropertyUtils.getProperty(bean, fieldNames[i]);
            if (values[i] instanceof List) {
                values[i] = processor.getXstream().toXML(values[i]);
            }
        }
        queryRunner.update(getInsertSql(), values);
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
        queryRunner.update(getUpdateSql(findFiled), values);
    }

       
    public void update(String sql) throws Exception {
        queryRunner.update(sql);
    }
    
    @Override
    public void remove(String key, String value) throws SQLException {
        String sql = "delete from " + colName + " where " + key + "='" + value + "'";
        queryRunner.update(sql);
    }
    
    public void remove(Map map) throws SQLException {
        String sql = "delete from " + colName + " where " + createConditionSegment(map);
        queryRunner.update(sql);
    }

    @Override
    public void removeAll() throws SQLException {
         String sql = "delete from " + colName;
         queryRunner.update(sql);
    }

    
    public List<T> find(String sql) throws Exception {
        return (List<T>) queryRunner.query(sql, listHandler);
    }
    
    public List<T> find(Map map) throws Exception {
        String sql = "select * from " + getColName() + " where " + createConditionSegment(map);
        return find(sql);
    }
    
    public T findOne(Map map) throws Exception {
        List<T> list = find(map);
        if (list.size() > 0) {
            return list.get(0);
        } else {
            return null;
        }
    }
    
    public T findOne(String sql) throws Exception {
        List<T> list = (List<T>) queryRunner.query(sql, listHandler);
        if (list != null && list.size() > 0) {
            return list.get(0);
        } else {
            return null;
        }
    }

    public List<T> findAll() throws Exception {
        return (List<T>) queryRunner.query("select * from " + colName, listHandler);
    }

    @Override
    public Long count() throws SQLException{
        ScalarHandler<Long> countHandler = new ScalarHandler<>("c");
        return queryRunner.query("select count(*) as c from " + colName, countHandler);
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
            sb.append("insert into ").append(colName).append(" (").append(columnNames[0]);
            for (int i = 1; i < columnNames.length; i++) {
                sb.append(", ").append(columnNames[i]);
                tailSb.append(",?");
            }
            sb.append(") values (").append(tailSb).append(")");
            insertSql = sb.toString();
        }
        return insertSql;
    }
    
    protected String getUpdateSql(String findField) {
        StringBuilder sb = new StringBuilder();
        sb.append("update ").append(colName).append(" set ").append(columnNames[0]).append("=?");
        for (int i = 1; i < columnNames.length; i++) {
            sb.append(", ").append(columnNames[i]).append("=?");
        }
        sb.append(" where ").append(findField).append("=?");
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
