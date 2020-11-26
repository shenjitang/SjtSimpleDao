/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.simple.test;

/**
 *
 * @author xiaolie33
 */
public class FkBean {
    private Integer id;
    private String name;
    private Integer baseBeanId;

    public FkBean() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getBaseBeanId() {
        return baseBeanId;
    }

    public void setBaseBeanId(Integer baseBeanId) {
        this.baseBeanId = baseBeanId;
    }

}
