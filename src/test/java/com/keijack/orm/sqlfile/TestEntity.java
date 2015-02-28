package com.keijack.orm.sqlfile;

import java.util.Date;

import com.keijack.orm.sqlfile.annotations.Column;
import com.keijack.orm.sqlfile.annotations.Entity;

@Entity(path = "test.sql")
public class TestEntity {

    @Column(label = "ID")
    private Integer id;

    @Column(label = "NAME")
    private String name;

    @Column(label = "CT")
    private Date createTime;

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

    public Date getCreateTime() {
	return createTime;
    }

    public void setCreateTime(Date createTime) {
	this.createTime = createTime;
    }

}
