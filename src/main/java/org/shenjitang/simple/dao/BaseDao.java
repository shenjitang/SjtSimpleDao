/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.simple.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author xiaolie
 */
public interface BaseDao<T> {
    public void insert(T bean) throws Exception;
    public void remove(String key, String value) throws SQLException;
    public void removeAll() throws SQLException;
    public Long count() throws SQLException;
    public T findOne(Map map) throws Exception;
    public T findOne(String sql) throws Exception;
    public List<T> findAll() throws Exception;
    public List<T> find(String sql) throws Exception;
    public List<T> find(Map map) throws Exception;
    public void update(String sql) throws Exception;
    public void update(T bean, String findFiled, Object value) throws Exception;
}
