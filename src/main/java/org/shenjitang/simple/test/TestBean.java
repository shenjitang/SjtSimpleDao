/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.simple.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bson.Document;
import org.shenjitang.simple.dao.mongodb.MongodbDao;

/**
 *
 * @author xiaolie33
 */
public class TestBean {
    private String id;
    private Set s1;
    private Set<String> s2;

    public TestBean() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set getS1() {
        return s1;
    }

    public void setS1(Set s1) {
        this.s1 = s1;
    }

    public Set<String> getS2() {
        return s2;
    }

    public void setS2(Set<String> s2) {
        this.s2 = s2;
    }
    
    public String toString() {
        return id + ":" + s1 + " " + s2;
    }
    
    public static void main(String args[]) throws Exception {
        List l1 = new ArrayList();
        l1.add("aaa");
        l1.add("bbb");
        Set s1 = new HashSet();
        s1.add("112");
        s1.add("221");
        Document doc1 = new Document();
        doc1.put("id", "1");
        doc1.put("s1", l1);
        doc1.put("s2", s1);
        List values = new ArrayList();
        values.add(doc1);
        List<TestBean> result = MongodbDao.exchangeList(values, TestBean.class);
        for (TestBean bean : result) {
            System.out.println(bean.toString());
        }
    }
}
