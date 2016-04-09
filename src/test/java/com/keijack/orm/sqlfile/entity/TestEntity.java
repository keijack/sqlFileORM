package com.keijack.orm.sqlfile.entity;

import java.util.Date;

import com.keijack.orm.sqlfile.annotations.Column;
import com.keijack.orm.sqlfile.annotations.Entity;
import com.keijack.orm.sqlfile.annotations.Temporal;

@Entity(path = "test.sql")
public class TestEntity {

    @Temporal
    private Integer id;

    private String NAME;

    @Column(label = "CT")
    private Date createTime;

    public Integer getId() {
	return id;
    }

    public void setId(Integer id) {
	this.id = id;
    }

    public String getNAME() {
	return NAME;
    }

    public void setNAME(String nAME) {
	NAME = nAME;
    }

    public Date getCreateTime() {
	return createTime;
    }

    public void setCreateTime(Date createTime) {
	this.createTime = createTime;
    }

}
