/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.shenjitang.simple.dao.jdbc;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.apache.commons.lang.StringUtils;
import org.shenjitang.simple.dao.PageDataResult;
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
    protected String dbName;
    protected String tableName;
    protected Class<T> entityClass;
    protected String[] fieldNames;
    protected String[] columnNames;
    protected Map<String,String> columnToPropertyOverrides;
    protected Map<String,String> PropertyToPropertyOverrides;
    protected NestedBeanProcessor processor;
    protected String insertSql;
    protected String insertSqlNoId;
    protected BeanListHandler listHandler;
    protected BeanHandler<T> beanHandler;
    protected String delMarkFieldName;
    protected String delMarkFieldValue;

    public JdbcDao() {
        columnToPropertyOverrides = new HashMap();
        PropertyToPropertyOverrides = new HashMap();
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
            if (isNnderlineFieldName()) {
                columnNames[i] =  CamelUnderLineUtils.camelToUnderline(fieldNames[i]);
            } else {
                columnNames[i] = fieldNames[i];
            }
            //columnNames[i] = fieldNames[i];
            columnToPropertyOverrides.put(columnNames[i], fieldNames[i]);
            PropertyToPropertyOverrides.put(fieldNames[i], columnNames[i]);
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

    public String getTableName() {
        if (StringUtils.isBlank(tableName)) {
            tableName = CamelUnderLineUtils.camelToUnderline(getT().getSimpleName());
        }
        return tableName;
    }
    
    protected String getTableName(T bean) {
        if (StringUtils.isBlank(tableName)) {
            return CamelUnderLineUtils.camelToUnderline(bean.getClass().getSimpleName());
        } else {
            return tableName;
        }
        
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
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
            StringBuilder sb = new StringBuilder();
            for (Object v : values) {
                sb.append(v).append(" ");
            }
            logger.debug(sql + " \n params: " + sb.toString());
            queryRunner.update(sql, values);
        } else { //bean中的id是null
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
    
    @Override
    public void update(T bean) throws Exception {
        Object id = PropertyUtils.getProperty(bean, "id");
        update(bean, "id", id);
    }
    
    @Override
    public void update(Map map, String findField, Object findValue) throws Exception {
        List fields = new ArrayList();
        List values = new ArrayList();
        for (int i = 0; i < fieldNames.length; i++) {
            if (map.containsKey(fieldNames[i])) {
                fields.add(columnNames[i]);
                values.add(map.get(fieldNames[i]));
            }
        }
        String sql = getUpdateSql(findField, fields);
        logger.debug(sql);
        values.add(findValue);
        queryRunner.update(sql, values.toArray());
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
        String sql = logicDeleted?
            "update `" + getTableName() + "` set `" + getDelMarkFieldName() + "`='" + getDelMarkFieldValue()  + 
                "' where " + key + "=?":
            "delete from `" + JdbcDao.this.getTableName() + "` where `" + key + "`=?";
        logger.debug(sql);
        queryRunner.update(sql, value);
    }
    
    @Override
    public void remove(Object id) throws SQLException {
        String sql = logicDeleted?
            "update `" + getTableName() + "` set `" + getDelMarkFieldName() + "`='" + getDelMarkFieldValue()  + 
                "' where id=?":
            "delete from `" + JdbcDao.this.getTableName() + "` where id=?";
        logger.debug(sql);
        queryRunner.update(sql, id);
    }
    
    public void remove(Map map) throws SQLException {
        String sql = logicDeleted?
            "update `" + getTableName() + "` set `" + getDelMarkFieldName() + "`='" + getDelMarkFieldValue() + 
                " where " + createConditionSegment(map):
            "delete from `" + JdbcDao.this.getTableName() + "` where " + createConditionSegment(map);
        logger.debug(sql);
        queryRunner.update(sql);
    }

    @Override
    public void removeAll() throws SQLException {
        String sql = logicDeleted?
            "update `" + getTableName() + "` set `" + getDelMarkFieldName() + "`='" + getDelMarkFieldValue():
            "delete from `" + JdbcDao.this.getTableName() + "`";
        logger.debug(sql);
        queryRunner.update(sql);
    }

    
    @Override
    public List<T> find(String sql) throws SQLException {
        logger.debug(sql);
        return (List<T>) queryRunner.query(sql, listHandler);
    }
    
    public PageDataResult<T> find(int offset, int limit, String sql) throws SQLException {
        Long count = count(sql);
        if (offset >= 0) {
            sql += " limit " + offset + ", " + limit;
        }
        List<T> data = find(sql);
        return new PageDataResult(count, offset, data);
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
        return new PageDataResult(count, offset, data);
    }

    @Override
    public List<T> find(Map map) throws SQLException {
        String sql = "select * from `" + JdbcDao.this.getTableName() + "` where " + createConditionSegment(map);
        if (logicDeleted && !map.containsKey(getDelMarkFieldName())) {
            sql += " and `" + getDelMarkFieldName() + "`!='" + getDelMarkFieldValue() + "'";
        }
        return find(sql);
    }
    
    public PageDataResult<T> find(int offset, int limit, Map map) throws SQLException {
        String sql = "select * from `" + JdbcDao.this.getTableName() + "` where " + createConditionSegment(map);
        if (logicDeleted && !map.containsKey(getDelMarkFieldName())) {
            sql += " and `" + getDelMarkFieldName() + "`!='" + getDelMarkFieldValue() + "'";
        }
        return find(offset, limit, sql);
    }

    @Override 
    public T get(Object id) throws Exception {
        String sql = "select * from `" + JdbcDao.this.getTableName() + "` where id='" + id + "'"; 
        logger.debug(sql);
        return (T)queryRunner.query(sql, beanHandler);
    }
    
    @Override
    public T findOne(Object id) throws Exception {
        return get(id);
    }
    
    @Override
    public T findOne(String fieldName, Object value) throws Exception {
        String sql = "select * from `" + JdbcDao.this.getTableName() + "` where `" + amendFieldName(fieldName) + "`='" + value + "'"; 
        if (logicDeleted && !fieldName.equals(getDelMarkFieldName())) {
            sql += " and `" + getDelMarkFieldName() + "`!='" + getDelMarkFieldValue() + "'";
        }
        logger.debug(sql);
        return (T)queryRunner.query(sql, beanHandler);
    }

    public T findOne(String fieldName1, Object value1, String fieldName2, Object value2) throws Exception {
        String sql = "select * from `" + JdbcDao.this.getTableName() + "` where `" + 
                amendFieldName(fieldName1) + "`=? and `" + amendFieldName(fieldName2) + "`=?"; 
        if (logicDeleted && !fieldName1.equals(getDelMarkFieldName()) && !fieldName2.equals(getDelMarkFieldName())) {
            sql += " and `" + getDelMarkFieldName() + "`!='" + getDelMarkFieldValue() + "'";
        }
        logger.debug(sql);
        return (T)queryRunner.query(sql, beanHandler, value1, value2);
    }

    @Override
    public T findOne(Map map) throws Exception {
        String sql = "select * from `" + JdbcDao.this.getTableName() + "` where " + createConditionSegment(map);
        if (logicDeleted && !map.containsKey(getDelMarkFieldName())) {
            sql += " and `" + getDelMarkFieldName() + "`!='" + getDelMarkFieldValue() + "'";
        }        
        return findOne(sql);
    }
    
    @Override
    public T findOne(String sql) throws Exception {
        logger.debug(sql);
        if (sql.toLowerCase().startsWith("select ")) {
            return (T)queryRunner.query(sql, beanHandler);
        } else {
            String rsql = "select * from `" + JdbcDao.this.getTableName() + "` where id='" + sql + "'"; 
            logger.debug(rsql);
            return (T)queryRunner.query(rsql, beanHandler);
        }     
    }
    
    public List<T> find(String fieldName, Object value) throws Exception {
        String sql = "select * from `" + JdbcDao.this.getTableName() + "` where `" + amendFieldName(fieldName) + "`='" + value + "'";
        if (logicDeleted && !fieldName.equals(getDelMarkFieldName())) {
            sql += " and `" + getDelMarkFieldName() + "`!='" + getDelMarkFieldValue() + "'";
        }
        logger.debug(sql);
        return (List<T>) queryRunner.query(sql, listHandler);
    }
    
    public PageDataResult<T> find(int offset, int limit, String fieldName, Object value) throws Exception {
        String sql = "select * from `" + JdbcDao.this.getTableName() + "` where `" + amendFieldName(fieldName) + "`='" + value + "'";
        if (logicDeleted && !fieldName.equals(getDelMarkFieldName())) {
            sql += " and `" + getDelMarkFieldName() + "`!='" + getDelMarkFieldValue() + "'";
        }
        Long count = count(sql);
        if (offset >= 0) {
            sql += " limit " + offset + ", " + limit;
        }
        logger.debug(sql);
        List<T> data = (List < T >)queryRunner.query(sql, listHandler);
        return new PageDataResult<T>(count, offset, data);
    }

    public List<T> findNotEquals(String fieldName, Object value) throws Exception {
        String sql = "select * from `" + JdbcDao.this.getTableName() + "` where `" + amendFieldName(fieldName) + "`!='" + value + "'";
        if (logicDeleted && !fieldName.equals(getDelMarkFieldName())) {
            sql += " and " + getDelMarkFieldName() + "!='" + getDelMarkFieldValue() + "'";
        }
        logger.debug(sql);
        return (List<T>) queryRunner.query(sql, listHandler);
    }

    public PageDataResult<T> findNotEquals(int offset, int limit, String fieldName, Object value) throws Exception {
        String sql = "select * from `" + JdbcDao.this.getTableName() + "` where `" + amendFieldName(fieldName) + "`!='" + value + "'";
        if (logicDeleted && !fieldName.equals(getDelMarkFieldName())) {
            sql += " and `" + getDelMarkFieldName() + "`!='" + getDelMarkFieldValue() + "'";
        }
        Long count = count(sql);
        if (offset >= 0) {
            sql += " limit " + offset + ", " + limit;
        }
        logger.debug(sql);
        List<T> data = (List < T >)queryRunner.query(sql, listHandler);
        return new PageDataResult<T>(count, offset, data);
    }
    
    @Override
    public List<T> findAll() throws Exception {
        String sql = logicDeleted?"select * from `" + JdbcDao.this.getTableName() + 
                "` where " + getDelMarkFieldName() + "!='" + getDelMarkFieldValue() + "'":
            "select * from `" + JdbcDao.this.getTableName() + "`";
        logger.debug(sql);
        return (List<T>) queryRunner.query(sql, listHandler);
    }
    
    public PageDataResult<T> findAll(int offset, int limit) throws Exception {
        String sql = logicDeleted?"select * from `" + JdbcDao.this.getTableName() + 
                "` where `" + getDelMarkFieldName() + "`!='" + getDelMarkFieldValue() + "'":
            "select * from `" + JdbcDao.this.getTableName() + "`";
        Long count = count(sql);
        if (offset >= 0) {
            sql += " limit " + offset + ", " + limit;
        }
        logger.debug(sql);
        List<T> data = (List < T >)queryRunner.query(sql, listHandler);
        return new PageDataResult<T>(count, offset, data);
    }

    public List<T> findAll(Boolean includeDeleted) throws Exception {
        if (includeDeleted) {
            String sql = "select * from `" + JdbcDao.this.getTableName() + "`";
            logger.debug(sql);
            return (List<T>) queryRunner.query(sql, listHandler);
        } else {
            return findAll();
        }
    }

    public PageDataResult<T> findAll(int offset, int limit, Boolean includeDeleted) throws Exception {
        if (includeDeleted) {
            String sql = "select * from `" + JdbcDao.this.getTableName() + "`";
            Long count = count(sql);
            if (offset >= 0) {
                sql += " limit " + offset + ", " + limit;
            }
            logger.debug(sql);
            List<T> data = (List < T >)queryRunner.query(sql, listHandler);
            return new PageDataResult<>(count, offset, data);
        } else {
            return findAll(offset, limit);
        }
    }
    
    @Override
    public Long count() throws SQLException{
        ScalarHandler<Long> countHandler = new ScalarHandler<>("c");
        String sql = logicDeleted?"select count(*) as c from `" + getTableName() + 
                "` where `" + getDelMarkFieldName() + "`!='" + getDelMarkFieldValue() + "'":
            "select count(*) as c from `" + getTableName() + "`";
        logger.debug(sql);
        return queryRunner.query(sql, countHandler);
    }
    
    public Long count(Boolean includeDeleted) throws Exception {
        ScalarHandler<Long> countHandler = new ScalarHandler<>("c");
        if (includeDeleted) {
            String sql = "select count(*) as c from `" + getTableName() + "`";
            logger.debug(sql);
            return queryRunner.query(sql, countHandler);
        } else {
            return count();
        }
    }
    
    protected Long count(String sql) throws SQLException {
        try {
            String whereStatement = getWhereStatement(sql);
            ScalarHandler<Long> countHandler = new ScalarHandler<>("c");
            //sql = logicDeleted?"select count(*) as c from " + getTableName() + 
            //        " where " + (StringUtils.isBlank(whereStatement)?"":whereStatement + " ") + " and " + getDelMarkFieldName() + "!='" + getDelMarkFieldValue() + "'":
            //    "select count(*) as c from " + getTableName() + (StringUtils.isBlank(whereStatement)?"":" where " + whereStatement);
            sql = "select count(*) as c from `" + getTableName() + "`" + (StringUtils.isBlank(whereStatement)?"":" where " + whereStatement);
            logger.debug(sql);
            return queryRunner.query(sql, countHandler);
        } catch (JSQLParserException e) {
            throw new SQLException("parse sql err:" + sql, e);
        }
    }

    @Override
    public Long count(Map map) throws SQLException{
        ScalarHandler<Long> countHandler = new ScalarHandler<>("c");
        String sql = "select count(*) as c from `" + JdbcDao.this.getTableName() + "` where " + createConditionSegment(map);
        if (logicDeleted && !map.containsKey(getDelMarkFieldName())) {
            sql += " and `" + getDelMarkFieldName() + "`!='" + getDelMarkFieldValue() + "'";
        }
        logger.debug(sql);
        return queryRunner.query(sql, countHandler);
    }
    
    public Long count(String field, Object value) throws SQLException {
        ScalarHandler<Long> countHandler = new ScalarHandler<>("c");
        String sql = "select count(*) as c from `" + JdbcDao.this.getTableName() + "` where `" + field + "`=?";
        if (logicDeleted && !field.equals(getDelMarkFieldName())) {
            sql += " and `" + getDelMarkFieldName() + "`!='" + getDelMarkFieldValue() + "'";
        }
        logger.debug(sql);
        return queryRunner.query(sql, countHandler, value);
    }

    public Long countNotEquals(String field, Object value) throws SQLException {
        ScalarHandler<Long> countHandler = new ScalarHandler<>("c");
        String sql = "select count(*) as c from `" + JdbcDao.this.getTableName() + "` where `" + field + "`!=?";
        if (logicDeleted && !field.equals(getDelMarkFieldName())) {
            sql += " and `" + getDelMarkFieldName() + "`!='" + getDelMarkFieldValue() + "'";
        }
        logger.debug(sql);
        return queryRunner.query(sql, countHandler, value);
    }

    public Long count(String field1, Object value1, String field2, Object value2) throws SQLException {
        ScalarHandler<Long> countHandler = new ScalarHandler<>("c");
        String sql = "select count(*) as c from `" + JdbcDao.this.getTableName() + "` where `" + field1 + "`=? and `" + field2 + "`=?";
        if (logicDeleted && !field1.equals(getDelMarkFieldName()) && !field2.equals(getDelMarkFieldName())) {
            sql += " and `" + getDelMarkFieldName() + "`!='" + getDelMarkFieldValue() + "'";
        }
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
            sb.append("insert into `").append(JdbcDao.this.getTableName()).append("` (`").append(columnNames[0]).append("`");
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
            sb.append("insert into `").append(JdbcDao.this.getTableName()).append("` (`").append(columnNames[0]).append("`");
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
        sb.append("update `").append(JdbcDao.this.getTableName()).append("` set `").append(columnNames[0]).append("`=?");
        for (int i = 1; i < columnNames.length; i++) {
            sb.append(", `").append(columnNames[i]).append("`=?");
        }
        sb.append(" where `").append(findField).append("`=?");
        return sb.toString();
    }

    protected String getUpdateSql(String findField, List fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("update `").append(JdbcDao.this.getTableName()).append("` set `").append(fields.get(0)).append("`=?");
        for (int i = 1; i < fields.size(); i++) {
            sb.append(", `").append(fields.get(i)).append("`=?");
        }
        sb.append(" where `").append(findField).append("`=?");
        return sb.toString();
    }

    protected String getUpdateSql(String findField, String findField2) {
        StringBuilder sb = new StringBuilder();
        sb.append("update `").append(JdbcDao.this.getTableName()).append("` set `").append(columnNames[0]).append("`=?");
        for (int i = 1; i < columnNames.length; i++) {
            sb.append(", `").append(columnNames[i]).append("`=?");
        }
        sb.append(" where `").append(findField).append("`=? and `").append(findField2).append("`=?");
        return sb.toString();
    }

    protected String createConditionSegment(Map map) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (Object key : map.keySet()) {
            if (i++ > 0) {
                sb.append(" and ");
            }
            sb.append("`").append(key.toString()).append("`").append("='").append(map.get(key).toString()).append("'");
        }
        return sb.toString();
    }
    
    private boolean isNnderlineFieldName() {
        return JdbcDaoConfig.getConfig().getFieldNameSplit() == JdbcDaoConfig.NAME_SPLIT_UNDERLINE;
    }
    private String amendFieldName(String name) {
        if (columnToPropertyOverrides.containsKey(name)) {
            return name;
        }
        name = PropertyToPropertyOverrides.get(name);
        if (StringUtils.isNotBlank(name)) return name;
        throw new RuntimeException("字段" + name + "不存在");
        //return isNnderlineFieldName()? CamelUnderLineUtils.camelToUnderline(name) : name;
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
    
    public static void main(String[] args) throws JSQLParserException {
        String sql = "select * from sss where a=3 and b!=5 and ccc=true order by dd limit 1, 10";
        System.out.println(getWhereStatement(sql));
    }
    

}
