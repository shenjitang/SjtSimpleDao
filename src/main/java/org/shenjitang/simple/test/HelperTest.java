/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.simple.test;

import org.shenjitang.simple.dao.jdbc.StatementHelper;

/**
 *
 * @author xiaolie33
 */
public class HelperTest {
    public static void main(String[] args) {
        FkBeanDao dao = new FkBeanDao();
        StatementHelper helper = StatementHelper.create(dao);
        helper.select("a.id", "a.name", "b.create_time")
            .from("fk_bean", "a")
            .leftJoin("base_bean", "b", "base_bean_id", "id")
            .where().eq("b.name", "打哈欠")
            .and().ge("b.id", 100)
            .orderBy("b.create_time").limit(0, 20);
        System.out.println(helper.toString());
    }
}
