/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.simple.dao.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author xiaolie
 */
public class DataSourceService {

	protected static final Log logger = LogFactory.getLog(DataSourceService.class);
    private Map<String, QueryRunner> dataSourceMap = new HashMap();
    private Map<String, String> DB_TYPE_DRIVER_MAP = new HashMap() {{put("mysql", "com.mysql.jdbc.Driver");}};
    
    public DataSourceService() {
    }
    
    public QueryRunner getDataSource(String name, Map properties) {
        QueryRunner dataSource = dataSourceMap.get(name);
        if (dataSource == null) {
            dataSource = buildDataSource(properties);
            dataSourceMap.put(name, dataSource);
        }
        return dataSource;
    }

    private QueryRunner buildDataSource(Map source){
        Properties props = new Properties();
        props.put("url", source.get("url"));
        props.put("driverClassName", DB_TYPE_DRIVER_MAP.get(source.get("type")));
        props.put("username", source.get("username"));
        props.put("password", source.get("password"));
        props.put("initialSize", 50);
        props.put("maxActive", 200);
        props.put("maxIdle", 100);
        props.put("minIdle", 50);
       
        if(!"Oracle".equalsIgnoreCase((String)source.get("type"))){
            props.put("validationQuery", "Select 1");
        }
        DataSource ds = null;
        try {
            ds = BasicDataSourceFactory.createDataSource(props);
            QueryRunner runner = new QueryRunner(ds);
            return runner;
        } catch (Exception e) {
            logger.warn("创建数据源失败", e);
            return null;
        }
    }
}
