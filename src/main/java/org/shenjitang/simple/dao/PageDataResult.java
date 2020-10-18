/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shenjitang.simple.dao;

import java.util.List;

/**
 *
 * @author xiaolie33
 */
public class PageDataResult<T> {
    private Long amount;
    private Integer startIndex;
    private List<T> data;

    public PageDataResult() {
    }

    public PageDataResult(Long amount, Integer startIndex, List<T> data) {
        this.amount = amount;
        this.startIndex = startIndex;
        this.data = data;
    }


    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Integer getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(Integer startIndex) {
        this.startIndex = startIndex;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }


}
