/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.simple.dao.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.sf.jsqlparser.JSQLParserException;
import org.shenjitang.simple.dao.jdbc.JdbcDao;

/**
 *
 * @author xiaolie33
 */
public class TestDao extends JdbcDao<Test> {

    public TestDao() {
    }
    /*
    public static void main(String[] args) throws Exception {
        TestDao dao = new TestDao();
        Set set = Set.of("ab1",2,3);
        Map map = new HashMap();
        map.put("intv", 2);
        map.put("abc", set);
        map.put("bbb", true);
        //map.put("abc", "bbb");
        String sql = dao.createConditionSegment(map);
        System.out.println(sql);
        
    }*/
}
