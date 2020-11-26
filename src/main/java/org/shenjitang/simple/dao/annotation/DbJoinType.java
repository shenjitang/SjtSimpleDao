/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.simple.dao.annotation;

/**
 *
 * @author xiaolie33
 */
public enum DbJoinType {    
    INNER("inner join"), LEFT("left join"), RIGHT("right join");
    private final String value;
    private DbJoinType(String value) {
        this.value = value;
    }
    public String value() {
        return value;
    }
}
