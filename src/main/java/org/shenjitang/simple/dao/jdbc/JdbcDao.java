/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.shenjitang.simple.dao.jdbc;

import com.mongodb.annotations.Immutable;
import org.shenjitang.simple.dao.BaseDao;
import org.shenjitang.simple.dao.utils.CamelUnderLineUtils;
import java.beans.PropertyDescriptor;
import java.io.StringReader;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.dbutils.BasicRowProcessor;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang.StringUtils;
import org.shenjitang.common.properties.PropertiesUtils;
import org.shenjitang.simple.dao.PageDataResult;
import static org.shenjitang.simple.dao.jdbc.JdbcDaoConfig.NAME_SPLIT_UNDERLINE;
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
    protected Boolean logicDeleted = Boolean.FALSE;
    protected QueryRunner  queryRunner;
    private SqlBeanParser<T> sqlBeanParser;
    private JdbcDaoConfig config;
    protected String dbName;
    //protected String tableName;
    protected Class<T> entityClass;
    //protected String[] fieldNames;
    //protected String[] columnNames;
    //protected Map<String,String> columnToPropertyOverrides;
    //protected Map<String,String> PropertyToColumnOverrides;
    //protected Map<String,PropertyDescriptor> propertyDesriptorMap;
    //protected Map<String,PropertyDescriptor> columnDesriptorMap;
    //protected NestedBeanProcessor processor;
    protected String insertSql;
    protected String insertSqlNoId;
    protected BeanListHandler listHandler;
    protected BeanHandler<T> beanHandler;
    protected String delMarkFieldName;
    protected String delMarkFieldValue;
    
    public JdbcDao() throws Exception {
        entityClass = getT();
        this.sqlBeanParser = new SqlBeanParser(entityClass);
        config = JdbcDaoConfig.getConfig();
        listHandler = new BeanListHandler<>(entityClass);
        beanHandler = new BeanHandler<>(entityClass);
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

    @Override
    public void insert(T bean) throws Exception {
        CommonSqlDao.create(entityClass, queryRunner).insert(bean);
    } 
    
    @Override
    public void update(T bean) throws Exception {
        CommonSqlDao.create(entityClass, queryRunner).update(bean);
    }
    
    @Override
    public void update(Map map, String findField, Object findValue) throws Exception {
        CommonSqlDao.create(entityClass, queryRunner).where().eq(findField, findValue).update(map);
    }
    
    @Override
    public void update(T bean, String findFiled, Object value) throws Exception {
        CommonSqlDao.create(entityClass, queryRunner).where().eq(findFiled, value).update(bean);
    }

    public void update(T bean, String findFiled, Object value, String findFiled2, Object value2) throws Exception {
        CommonSqlDao.create(entityClass, queryRunner).where().eq(findFiled, value).and().eq(findFiled2, value2).update(bean);
    }
       
    @Override
    public void update(String sql) throws Exception {
        logger.debug(sql);
        queryRunner.update(sql);
    }
    
    @Override
    public void remove(String key, Object value) throws SQLException {
        CommonSqlDao dao = CommonSqlDao.create(entityClass, queryRunner).where().eq(key, value);
        if (logicDeleted) {
            dao.update(getDelMarkFieldName(), getDelMarkFieldValue());
        } else {
            dao.delete();
        }
    }
    
    @Override
    public void remove(Object id) throws SQLException {
        CommonSqlDao dao = CommonSqlDao.create(entityClass, queryRunner).where().eq("id", id);
        if (logicDeleted) {
            dao.update(getDelMarkFieldName(), getDelMarkFieldValue());
        } else {
            dao.delete();
        }
    }
    
    public void remove(Map map) throws SQLException {
        CommonSqlDao dao = CommonSqlDao.create(entityClass, queryRunner).where().and(map);
        if (logicDeleted) {
            dao.update(getDelMarkFieldName(), getDelMarkFieldValue());
        } else {
            dao.delete();
        }
    }

    @Override
    public void removeAll() throws SQLException {
        CommonSqlDao dao = CommonSqlDao.create(entityClass, queryRunner);
        if (logicDeleted) {
            dao.update(getDelMarkFieldName(), getDelMarkFieldValue());
        } else {
            dao.delete();
        }
    }

    
    @Override
    public List<T> find(String sql) throws SQLException {
        logger.debug(sql);
        return (List<T>) queryRunner.query(sql, listHandler);
    }
    
    public <B> List<B> findBeanList(String sql, Class<B> clazz) throws SQLException {
        BeanListHandler handler = new BeanListHandler(clazz);
        logger.debug(sql);
        return (List<B>) queryRunner.query(sql, handler);
    }
    
    public List<Map<String, Object>> findMapList(String sql) throws SQLException {
        MapListHandler handler = new MapListHandler();
        logger.debug(sql);
        return queryRunner.query(sql, handler);
    }
    
    public <B> B findBean(String sql, Class<B> clazz) throws SQLException {
        BeanHandler<B> handler = new BeanHandler(clazz);
        logger.debug(sql);
        return queryRunner.query(sql, handler);
    }
    
    public Map<String, Object> findMap(String sql) throws SQLException {
        MapHandler handler = new MapHandler();
        logger.debug(sql);
        return queryRunner.query(sql, handler);
    }
    
    public PageDataResult<T> find(int offset, int limit, String sql) throws SQLException {
        Long count = count(sql);
        if (offset >= 0) {
            sql += " limit " + offset + ", " + limit;
        }
        List<T> data = find(sql);
        return new PageDataResult(count, data);
    }
    
    @Override
    public List<T> find(String sql, Object... parameters) throws Exception {
        logger.debug(sql);
        return (List<T>) queryRunner.query(sql, listHandler, parameters);
    }
    
    public PageDataResult<T> find(int offset, int limit, String sql, Object... parameters) throws Exception {
        Long count = count(sql);
        if (offset >= 0) {
            sql += " limit " + offset + ", " + limit;
        }
        logger.debug(sql);
        List<T> data = (List<T>)queryRunner.query(sql, listHandler, parameters);
        return new PageDataResult(count, data);
    }

    @Override
    public List<T> find(Map map) throws SQLException {
        CommonSqlDao dao = CommonSqlDao.create(entityClass, queryRunner).where().and(map);
        if (logicDeleted) {
            dao.and().ne(getDelMarkFieldName(), getDelMarkFieldValue());
        }
        return dao.find();
    }
    
    public PageDataResult<T> find(int offset, int limit, Map map) throws SQLException {
        CommonSqlDao dao = CommonSqlDao.create(entityClass, queryRunner).where().and(map);
        if (logicDeleted) {
            dao.and().ne(getDelMarkFieldName(), getDelMarkFieldValue());
        }
        return dao.limit(offset, limit).findAndCount();
    }

    @Override 
    public T get(Object id) throws Exception {
        return (T)CommonSqlDao.create(entityClass, queryRunner).where().eq("id", id).findOne();
    }
    
    @Override
    public T findOne(Object id) throws Exception {
        CommonSqlDao<T> dao = CommonSqlDao.create(entityClass, queryRunner).where().eq("id", id);
        if (logicDeleted) {
            dao.and(getDelMarkFieldName(), getDelMarkFieldValue());
        }
        return dao.findOne();
    }
    
    @Override
    public T findOne(String fieldName, Object value) throws Exception {
        CommonSqlDao<T> dao = CommonSqlDao.create(entityClass, queryRunner).where().eq(fieldName, value);
        if (logicDeleted && !fieldName.equals(getDelMarkFieldName())) {
            dao.and().ne(getDelMarkFieldName(), getDelMarkFieldValue());
        }
        return dao.findOne();
    }

    public T findOne(String fieldName1, Object value1, String fieldName2, Object value2) throws Exception {
        CommonSqlDao<T> dao = CommonSqlDao.create(entityClass, queryRunner).where().eq(fieldName1, value1).and()
            .eq(fieldName2, value2);
        if (logicDeleted && !fieldName1.equals(getDelMarkFieldName()) && !fieldName2.equals(getDelMarkFieldName())) {
            dao.and().ne(getDelMarkFieldName(), getDelMarkFieldValue());
        }
        return dao.findOne();
    }

    @Override
    public T findOne(Map map) throws Exception {
        CommonSqlDao<T> dao = CommonSqlDao.create(entityClass, queryRunner).where().and(map);
        if (logicDeleted && !map.containsKey(getDelMarkFieldName())) {
            dao.and().ne(getDelMarkFieldName(), getDelMarkFieldValue());
        }        
        return dao.findOne();
    }
    
    @Override
    public T findOne(String sql) throws Exception {
        logger.debug(sql);
        if (sql.toLowerCase().startsWith("select ")) {
            return (T)queryRunner.query(sql, beanHandler);
        } else {
            String rsql = "select * from `" + sqlBeanParser.getTableName() + "` where id='" + sql + "'"; 
            logger.debug(rsql);
            return (T)queryRunner.query(rsql, beanHandler);
        }     
    }
    
    public List<T> find(String fieldName, Object value) throws Exception {
        CommonSqlDao<T> dao = CommonSqlDao.create(entityClass, queryRunner).where().eq(fieldName, value);
        if (logicDeleted && !fieldName.equals(getDelMarkFieldName())) {
            dao.and().ne(getDelMarkFieldName(), getDelMarkFieldValue());
        }
        return dao.find();
    }
    
    public PageDataResult<T> find(int offset, int limit, String fieldName, Object value) throws Exception {
        CommonSqlDao<T> dao = CommonSqlDao.create(entityClass, queryRunner).where().eq(fieldName, value);
        if (logicDeleted && !fieldName.equals(getDelMarkFieldName())) {
            dao.and().ne(getDelMarkFieldName(), getDelMarkFieldValue());
        }
        return dao.limit(offset, limit).findAndCount();
    }

    public List<T> findNotEquals(String fieldName, Object value) throws Exception {
        CommonSqlDao<T> dao = CommonSqlDao.create(entityClass, queryRunner).where().ne(fieldName, value);
        if (logicDeleted && !fieldName.equals(getDelMarkFieldName())) {
            dao.and().ne(getDelMarkFieldName(), getDelMarkFieldValue());
        }
        return dao.find();
    }

    public PageDataResult<T> findNotEquals(int offset, int limit, String fieldName, Object value) throws Exception {
        CommonSqlDao<T> dao = CommonSqlDao.create(entityClass, queryRunner).where().ne(fieldName, value);
        if (logicDeleted && !fieldName.equals(getDelMarkFieldName())) {
            dao.and().ne(getDelMarkFieldName(), getDelMarkFieldValue());
        }
        return dao.limit(offset, limit).findAndCount();
    }
    
    @Override
    public List<T> findAll() throws Exception {
        CommonSqlDao<T> dao = CommonSqlDao.create(entityClass, queryRunner);
        if (logicDeleted) {
            dao.where().ne(getDelMarkFieldName(), getDelMarkFieldValue());
        }
        return dao.find();
    }
    
    public PageDataResult<T> findAll(int offset, int limit) throws Exception {
        CommonSqlDao<T> dao = CommonSqlDao.create(entityClass, queryRunner);
        if (logicDeleted) {
            dao.where().ne(getDelMarkFieldName(), getDelMarkFieldValue());
        }
        return dao.limit(offset, limit).findAndCount();
    }

    public List<T> findAll(Boolean includeDeleted) throws Exception {
        return includeDeleted?CommonSqlDao.create(entityClass, queryRunner).find():findAll();
    }

    public PageDataResult<T> findAll(int offset, int limit, Boolean includeDeleted) throws Exception {
        return includeDeleted?
            CommonSqlDao.create(entityClass, queryRunner).limit(offset, limit).findAndCount()
            : findAll(offset, limit);
    }
    
    @Override
    public Long count() throws SQLException{
        return CommonSqlDao.create(entityClass, queryRunner).where().ne(getDelMarkFieldName(), getDelMarkFieldValue()).count();
    }
    
    public Long count(Boolean includeDeleted) throws Exception {
        return includeDeleted?CommonSqlDao.create(entityClass, queryRunner).count():count();
    }
    
    @Override
    public Long count(Map map) throws SQLException{
        CommonSqlDao dao = CommonSqlDao.create(entityClass, queryRunner).where().and(map);
        return logicDeleted?dao.and().ne(getDelMarkFieldName(), getDelMarkFieldValue()).count():dao.count();
    }
    
    public Long count(String field, Object value) throws SQLException {
        CommonSqlDao dao = CommonSqlDao.create(entityClass, queryRunner).where().eq(field, value);
        return logicDeleted?dao.and().ne(getDelMarkFieldName(), getDelMarkFieldValue()).count():dao.count();
    }

    public Long countNotEquals(String field, Object value) throws SQLException {
        CommonSqlDao dao = CommonSqlDao.create(entityClass, queryRunner).where().ne(field, value);
        return logicDeleted?dao.and().ne(getDelMarkFieldName(), getDelMarkFieldValue()).count():dao.count();
    }

    public Long count(String field1, Object value1, String field2, Object value2) throws SQLException {
        CommonSqlDao dao = CommonSqlDao.create(entityClass, queryRunner).where().eq(field1, value1).and(field2, value2);
        return logicDeleted?dao.and().ne(getDelMarkFieldName(), getDelMarkFieldValue()).count():dao.count();
    }
    
    public Long count(String sql) throws SQLException {
        try {
            String whereStatement = getWhereStatement(sql);
            ScalarHandler<Long> countHandler = new ScalarHandler<>("c");
            //sql = logicDeleted?"select count(*) as c from " + getTableName() + 
            //        " where " + (StringUtils.isBlank(whereStatement)?"":whereStatement + " ") + " and " + getDelMarkFieldName() + "!='" + getDelMarkFieldValue() + "'":
            //    "select count(*) as c from " + getTableName() + (StringUtils.isBlank(whereStatement)?"":" where " + whereStatement);
            sql = "select count(*) as c from `" + sqlBeanParser.getTableName() + "`" + (StringUtils.isBlank(whereStatement)?"":" where " + whereStatement);
            logger.debug(sql);
            return queryRunner.query(sql, countHandler);
        } catch (JSQLParserException e) {
            throw new SQLException("parse sql err:" + sql, e);
        }        
    }

    /*
    protected List<T> exchangeList(List<Map> list) throws Exception {
        List<T> returnList = new ArrayList();
        for (Map map : list) {
            T bean = (T)getT().newInstance();
            BeanUtils.populate(bean, map);
            returnList.add(bean);
        }
        return returnList;
    }*/
    
    public final Class getT() {
        Type genType = getClass().getGenericSuperclass();  
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();  
        return (Class) params[0];  
    }

    
    public Boolean getLogicDeleted() {
        return logicDeleted;
    }

    public void setLogicDeleted(Boolean logicDeleted) {
        this.logicDeleted = logicDeleted;
    }
    
    public String getDelMarkFieldName() {
        return StringUtils.isBlank(delMarkFieldName)?
            JdbcDaoConfig.getConfig().getDelMarkFieldName()
            : delMarkFieldName;
    }

    public void setDelMarkFieldName(String delMarkFieldName) {
        this.delMarkFieldName = delMarkFieldName;
    }

    public String getDelMarkFieldValue() {
        return StringUtils.isBlank(delMarkFieldValue)?
            JdbcDaoConfig.getConfig().getDelMarkFieldValue()
            : delMarkFieldValue;
    }

    public void setDelMarkFieldValue(String delMarkFieldValue) {
        this.delMarkFieldValue = delMarkFieldValue;
    }
    public Map getDelConditionMap () {
        String name = getDelMarkFieldName();
        String value = getDelMarkFieldValue();
        Map map = new HashMap();
        map.put(name, value);
        return map;
    }
    
    private static String getWhereStatement(String sql) throws JSQLParserException {
        CCJSqlParserManager parserManager = new CCJSqlParserManager();
        Statement statement = parserManager.parse(new StringReader(sql));
        Expression whereExpression = null;
        if (statement instanceof Select) {
            Select select = (Select) statement;
            PlainSelect selectBody = (PlainSelect) select.getSelectBody();
            whereExpression = selectBody.getWhere();
        } else if (statement instanceof Delete) {
            Delete delete = (Delete) statement;
            whereExpression = delete.getWhere();
        } else if (statement instanceof Update) {
            Update update = (Update) statement;
            whereExpression = update.getWhere();
        } else if (statement instanceof Insert) {
            throw new JSQLParserException("insert 不支持");
        } else {
            throw new JSQLParserException("不支持的sql语句:" + sql);
        }
        return whereExpression.toString();
    }
    /*
    public static void main(String[] args) throws Exception {
        String sql = "select * from sss where a=3 and b!=5 and ccc=true order by dd limit 1, 10";
        System.out.println(getWhereStatement(sql));        
    }
*/
    

}
