/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.shenjitang.simple.dao.mongodb;

import com.mongodb.BasicDBObject;
import org.shenjitang.simple.dao.BaseDao;
import com.mongodb.DBObject;
import org.shenjitang.simple.dao.utils.CamelUnderLineUtils;
import org.shenjitang.mongodbutils.MongoDbOperater;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author xiaolie
 * @param <T>
 */
public abstract class MongodbDao <T> implements BaseDao<T> {
    protected MongoDbOperater mongoDbOperation;
    protected String dbName;
    private String colName;

    public MongodbDao() {
    }

    public MongoDbOperater getMongoDbOperation() {
        return mongoDbOperation;
    }

    public void setMongoDbOperation(MongoDbOperater mongoDbOperation) {
        this.mongoDbOperation = mongoDbOperation;
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
    
    public void insert(T bean) throws Exception {
        mongoDbOperation.insert(dbName, getColName(bean), bean);
    } 
       
    public void remove(Map find) {
        mongoDbOperation.remove(dbName, getColName(), find);
    }
    
    @Override
    public void remove(String key, String value) {
        Map map = new HashMap();
        map.put(key, value);
        remove(map);
    }
    
    @Override
    public void removeAll() {
        mongoDbOperation.remove(dbName, getColName(), new HashMap());
    }
    
    @Override
    public T findOne(Object id) throws Exception {
        HashMap map = new HashMap();
        map.put("id", id);
        return (T)mongoDbOperation.findOneObj(dbName, getColName(), map, getT());
    }
    
    @Override
    public T findOne(String fieldName, Object value) throws Exception {
        HashMap map = new HashMap();
        map.put(fieldName, value);
        return (T)mongoDbOperation.findOneObj(dbName, getColName(), map, getT());
    }

    
    @Override
    public T findOne(Map queryMap) throws Exception {
        return (T)mongoDbOperation.findOneObj(dbName, getColName(), queryMap, getT());
    }
    
    @Override
    public List<T> find(String sql) throws Exception {
        List<Map> list = mongoDbOperation.find(dbName, sql);
        return exchangeList(list);
    }

    @Override
    public List<T> find(String sql, Object... parameters) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

        
    @Override
    public T findOne(String sql) throws Exception {
        return (T)mongoDbOperation.findOneObj(dbName, sql, getT());
    }

    @Override
    public List<T> find(Map queryMap) throws Exception {
        List<Map> list = mongoDbOperation.find(dbName, getColName(), queryMap);
        return exchangeList(list);
    }

    @Override
    public List<T> findAll() throws Exception {
        List<Map> list = mongoDbOperation.findAll(dbName, getColName());
        return exchangeList(list);
    }

    @Override
    public Long count(){
        return  mongoDbOperation.count(dbName,getColName());
    }

    @Override
    public Long count(Map queryMap){
        return  mongoDbOperation.count(dbName,getColName(),queryMap);
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
    
    public void update(Map findObj, Map setMap) throws Exception {
        mongoDbOperation.update(dbName, getColName(), findObj, setMap);
    }
    
    @Override
    public void update(T bean) throws Exception {
        Object id = PropertyUtils.getProperty(bean, "id");
        update(bean, "id", id);
    }
    
    @Override
    public void update(T bean, String findField, Object value) throws Exception {
        Map findMap = new HashMap();
        findMap.put(findField, value);
        DBObject findObj = new BasicDBObject(findMap);
        mongoDbOperation.update(dbName, colName, findObj, bean);
    }
    
    @Override
    public void update(String sql) throws Exception {
        mongoDbOperation.update(dbName, sql);
    }
    
    protected Class getGenericType(int index) {
        Type genType = getClass().getGenericSuperclass();
        if (!(genType instanceof ParameterizedType)) {
            return Object.class;
        }
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
        if (index >= params.length || index < 0) {
            throw new RuntimeException("Index outof bounds");
        }
        if (!(params[index] instanceof Class)) {
            return Object.class;
        }
        return (Class) params[index];
    }
    
    public Class getT() {
        return getGenericType(0);
    }
}
