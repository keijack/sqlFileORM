package com.keijack.orm.sqlfile;

import java.util.List;

/**
 * @author Keijack
 *
 */
class SqlAndParams {

    private String countSql;

    private String sql;

    private List<Object> params;

    public void setCountSql(String countSql) {
	this.countSql = countSql;
    }

    public void setSql(String sql) {
	this.sql = sql;
    }

    public void setParams(List<Object> params) {
	this.params = params;
    }

    public String getCountSql() {
	return countSql;
    }

    public String getSql() {
	return sql;
    }

    public List<Object> getParams() {
	return params;
    }
}
