package com.keijack.orm.sqlfile;

import javax.sql.DataSource;

public class SessionFactory {

    private DataSource dataSource;

    private Session session;

    public DataSource getDataSource() {
	return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
	this.dataSource = dataSource;
    }

    public Session getCurrentSession() {
	if (session == null) {
	    synchronized (this) {
		if (session == null) {
		    session = new Session(dataSource);
		}
	    }
	}
	return session;
    }
}
