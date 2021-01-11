/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.shenjitang.simple.dao.mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.shenjitang.simple.dao.BaseDao;
import org.shenjitang.simple.dao.utils.CamelUnderLineUtils;
import org.shenjitang.mongodbutils.MongoDbOperater;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.jsqlparser.JSQLParserException;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.shenjitang.mongodbutils.QueryInfo;
import org.shenjitang.simple.dao.jdbc.JdbcDaoConfig;
import org.shenjitang.simple.dao.jdbc.SqlBeanParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author xiaolie
 * @param <T>
 */
public abstract class MongodbDao <T> implements BaseDao<T> {
    private static final Logger logger = LoggerFactory.getLogger(MongodbDao.class);
    protected SqlBeanParser<T> sqlBeanParser;
    protected MongoDbOperater mongoDbOperation;
    protected String dbName;
    protected String colName;
    private Class tClazz;

    public MongodbDao() {
        try {
            JdbcDaoConfig.getConfig().setFieldNameSplit(JdbcDaoConfig.NAME_SPLIT_ORIGINAL);
            this.sqlBeanParser = new SqlBeanParser(getT());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    
    public MongoDatabase getDatabase() {
        return mongoDbOperation.getDatabase(dbName);
    }
    
    public MongoCollection getCollection() {
        return getDatabase().getCollection(getColName());
    }
    public MongoCollection getTypedCollection() {
        return getDatabase().getCollection(getColName(), getT());
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
            colName = CamelUnderLineUtils.camelToUnderline(bean.getClass().getSimpleName());
        }
        return colName;
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
    public void remove(String key, Object value) {
        Map map = new HashMap();
        map.put(key, value);
        remove(map);
    }
    
    @Override
    public void remove(Object id) {
        Map map = new HashMap();
        map.put("_id", id);
        remove(map);
    }

    @Override
    public void removeAll() {
        mongoDbOperation.remove(dbName, getColName(), new HashMap());
    }
    
    @Override
    public T get(Object id) throws Exception {
        return (T)mongoDbOperation.findOne(getT(), dbName, getColName(), Filters.eq("_id", id));
    }
        
    /**
     * 根据 id 查询数据
     */
    public <V> V get(String id ,Class<V> clazz) throws Exception {
        return this.mongoDbOperation.findOne(clazz, this.dbName, this.getColName(), Filters.eq("_id", id));
    }

    @Override
    public T findOne(String fieldName, Object value) throws Exception {
        return (T)mongoDbOperation.findOne(getT(), dbName, getColName(), Filters.eq(fieldName, value));
    }

    
    @Override
    public T findOne(Map queryMap) throws Exception {
        return (T)mongoDbOperation.findOne(getT(), dbName, getColName(), queryMap);
    }
    
    @Override
    public List<T> find(String sql) throws Exception {
        return mongoDbOperation.find(getT(), dbName, sql);
    }

    @Override
    public List<T> find(String sql, Object... parameters) throws Exception {
        return mongoDbOperation.find(getT(), dbName, sql, parameters);
    }

        
    @Override
    public T findOne(String sql) throws Exception {
        return (T)mongoDbOperation.findOne(getT(), dbName, sql);
    }

    @Override
    public List<T> find(Map queryMap) throws Exception {
        List<Document> list = mongoDbOperation.find(dbName, getColName(), queryMap);
        return exchangeList(list);
    }
    /**
     * Map 条件查询
     */
    public <V> List<V> find(Map queryMap , Class<V> clazz) throws Exception {
        if (queryMap.containsKey("id") && !queryMap.containsKey("_id")) {
            queryMap.put("_id", queryMap.get("id"));
        }
        List<Document> list = this.mongoDbOperation.find(this.dbName, this.getColName(), queryMap);
        return exchangeList(list,clazz);
    }

    @Override
    public List<T> findAll() throws Exception {
        List<Document> list = mongoDbOperation.findAll(dbName, getColName());
        return exchangeList(list);
    }

    public <V> List<V> find(Bson query, Bson order, int start, int limit, Class<V> clazz) throws Exception {
        List<Document> list = this.mongoDbOperation.find(getCollection(), query, order, start, limit);
        return exchangeList(list, clazz);
    }

    /**
     * 无指定排序，分页
     */
    public <V> List<V> find(Bson query, int start, int limit, Class<V> clazz) throws Exception {
        return find(query,null, start, limit, clazz);
    }

    /**
     * 指定排序，查询所有
     */
    public <V> List<V> findAll(Bson query, Bson order, Class<V> clazz) throws Exception {
        return find(query, order, 0, 0, clazz);
    }

    /**
     * 无指定排序，查询所有
     */
    public <V> List<V> findAll(Bson query, Class<V> clazz) throws Exception {
        return find(query,null, 0, 0, clazz);
    }


    /**
     * 查询，分页
     */
    public List<T> find(Bson query, Bson order, int start, int limit) throws Exception {
        List<Document> list = this.mongoDbOperation.find(getCollection(), query, order, start, limit);
        return exchangeList(list);
    }

    /**
     * 无指定排序，分页
     */
    public  List<T> find(Bson query, int start, int limit) throws Exception {
        return find(query,null, start, limit);
    }

    /**
     * 指定排序，查询所有
     */
    public  List<T> findAll(Bson query, Bson order) throws Exception {
        return find(query,order, 0, 0);
    }

    /**
     * 无指定排序，查询所有
     */
    public  List<T> findAll(Bson query) throws Exception {
        return find(query,null, 0, 0);
    }
    
    @Override
    public Long count(){
        return  mongoDbOperation.count(dbName,getColName());
    }

    public Long count(Bson bson)throws Exception{
        if (bson==null){
            return  getCollection().countDocuments();
        }else {
            return getCollection().countDocuments(bson);
        }
    }

    @Override
    public Long count(Map queryMap){
        return  mongoDbOperation.count(dbName,getColName(),queryMap);
    }
    
    public Long count(String sql) throws JSQLParserException {
        QueryInfo queryInfo = mongoDbOperation.sql2QueryInfo(dbName, sql);
        return mongoDbOperation.getDatabase(dbName).getCollection(getColName()).countDocuments(queryInfo.query);
    }
    
    public Long count(String sql, Object... parameters) throws JSQLParserException {
        QueryInfo queryInfo = mongoDbOperation.sql2QueryInfo(dbName, sql, parameters);
        return mongoDbOperation.getDatabase(dbName).getCollection(getColName()).countDocuments(queryInfo.query);
    }

    protected List<T> exchangeList(List<Document> list) throws Exception {
        List<T> returnList = new ArrayList();
        for (Document map : list) {
            T bean = (T)getT().newInstance();
            if (map.containsKey("_id") && (!map.containsKey("id"))) {
                map.put("id", map.get("_id"));
            }
            BeanUtils.populate(bean, map);
            returnList.add(bean);
        }
        return returnList;
    }
    
    /**
     * 集合类型转换
     */
    protected  <V> List<V> exchangeList(List<Document> list,Class<V> clazz) throws Exception {
        List<V> returnList = new ArrayList<>();
        if (clazz == null) {
            clazz = getT();
        }
        for (Document map : list) {
            V bean = clazz.newInstance();
            if (map.containsKey("_id") && !map.containsKey("id")) {
                map.put("id", map.get("_id"));
            }
            BeanUtils.populate(bean, map);
            returnList.add(bean);
        }
        return returnList;
    }

    public void update(Map findObj, Map setMap) throws Exception {
        mongoDbOperation.update(dbName, getColName(), findObj, checkColumnName(setMap));
    }
    
    @Override
    public void update(T bean) throws Exception {
        Object id = PropertyUtils.getProperty(bean, "id");
        update(bean, "_id", id);
    }
    
    @Override
    public void update(T bean, String findField, Object value) throws Exception {
        mongoDbOperation.updateByBean(dbName, colName, Filters.eq(findField, value), bean);
    }
    
    @Override
    public void update(String sql) throws Exception {
        mongoDbOperation.update(dbName, sql);
    }
    
    @Override
    public void update(Map map ,String findField, Object findValue) throws Exception {
        mongoDbOperation.update(dbName, colName, Filters.eq(findField, findValue), checkColumnName(map));
    }
    
    public void updateOne(Map map ,String findField, Object findValue) throws Exception {
        mongoDbOperation.updateOne(dbName, colName, Filters.eq(findField, findValue), checkColumnName(map));
    }

    public void updateById(Map map ,Object id) throws Exception {
        mongoDbOperation.updateOne(dbName, colName, Filters.eq("_id", id), checkColumnName(map));
    }

    public void updateById(String field, String value ,Object id) throws Exception {
        Map map = new HashMap();
        map.put(field, value);
        mongoDbOperation.updateOne(dbName, colName, Filters.eq("_id", id), checkColumnName(map));
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
        if (tClazz == null) {
            tClazz = getGenericType(0);
        }
        return tClazz;
    }
    
    public Map checkColumnName(Map map) {
        Map resultMap = new HashMap();
        for (Object field : map.keySet()) {
            String fieldStr = (String)field;
            if (sqlBeanParser.containsField(fieldStr)) {
                resultMap.put(field, map.get(field));
            } else {
                logger.warn(dbName + "." + colName + "." + fieldStr + " not exist!");
            }
        }
        return resultMap;
    }
    public String checkColumnName(String name) {
        if (sqlBeanParser.containsField(name)) {
            return name;
        } else {
            throw new RuntimeException("no such field:" + name);
        }
    }
}
