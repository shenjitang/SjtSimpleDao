/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.simple.dao;

import java.util.List;
import java.util.Map;

/**
 *
 * @author xiaolie
 */
public interface BaseDao<T> {
    public void insert(T bean) throws Exception;
    public void remove(String key, Object value) throws Exception;
    public void remove(Object id) throws Exception;
    public void removeAll() throws Exception;
    public Long count() throws Exception;
    public Long count(Map map) throws Exception;
    public T get(Object id) throws Exception;
    public T findOne(Object id) throws Exception;
    public T findOne(String fieldName, Object value) throws Exception;
    public T findOne(Map map) throws Exception;
    public T findOne(String sql) throws Exception;      
    public List<T> findAll() throws Exception;
    public List<T> find(String sql) throws Exception;
    public List<T> find(String sql, Object... parameters) throws Exception;
    public List<T> find(Map map) throws Exception;
    public void update(String sql) throws Exception;
    public void update(T bean, String findFiled, Object value) throws Exception;
    public void update(T bean) throws Exception;
    public void update(Map map, String findField, Object findValue) throws Exception;
}
