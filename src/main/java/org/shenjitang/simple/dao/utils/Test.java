/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.simple.dao.utils;

import java.beans.PropertyDescriptor;
import org.apache.commons.beanutils.PropertyUtils;

/**
 *
 * @author xiaolie33
 */
public class Test {
    
    public static void main(String[] args) throws Exception {
        Integer ii = 100;
        System.out.println(ii.getClass().getName());
        Test ex = new Test();
        PropertyDescriptor pd = PropertyUtils.getPropertyDescriptor(ex, "intv");
        System.out.println(pd.getPropertyType().getName());
        PropertyDescriptor pdx = PropertyUtils.getPropertyDescriptor(ex, "intx");
        System.out.println(pdx.getPropertyType().getName());
        Boolean b = true;
        System.out.println(Boolean.valueOf(b.toString()));
    }
    
    private int intv = 10;
    private Integer intx = 10;
    private boolean bbb = true;
    private Boolean bbq = false;
    private String abc = "aaa";
    
    public int getIntv() {
        return intv;
    }
    public void setIntv(int intv) {
        this.intv = intv;
    }

    public Integer getIntx() {
        return intx;
    }

    public void setIntx(Integer intx) {
        this.intx = intx;
    }

    public boolean isBbb() {
        return bbb;
    }

    public void setBbb(boolean bbb) {
        this.bbb = bbb;
    }

    public Boolean getBbq() {
        return bbq;
    }

    public void setBbq(Boolean bbq) {
        this.bbq = bbq;
    }

    public String getAbc() {
        return abc;
    }

    public void setAbc(String abc) {
        this.abc = abc;
    }
    
    
}
