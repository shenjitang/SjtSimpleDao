/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.simple.test;

import org.shenjitang.simple.dao.jdbc.CommonSqlDao;
import org.shenjitang.simple.dao.jdbc.StatementHelper;

/**
 *
 * @author xiaolie33
 */
public class NewHelperTest {
    public static void main(String[] args) throws Exception {
        CommonSqlDao helper = CommonSqlDao.create(QueryBean.class, null);
        System.out.println(helper.toString());
    }
}
