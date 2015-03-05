package com.keijack.orm.sqlfile;

import javax.sql.DataSource;

import com.keijack.orm.sqlfile.template.DefaultSqlAndParamsPreparer;

/**
 * Session factory
 * 
 * @author keijack.wu
 *
 */
public class SessionFactory {

    private DataSource dataSource;

    private SqlAndParamsPreparer sqlAndParamsPreparer = new DefaultSqlAndParamsPreparer();

    private Session session;

    public void setDataSource(DataSource dataSource) {
	this.dataSource = dataSource;
    }

    public void setSqlAndParamsPreparer(SqlAndParamsPreparer sqlAndParamsPreparer) {
	this.sqlAndParamsPreparer = sqlAndParamsPreparer;
    }

    public Session getCurrentSession() {
	if (session == null) {
	    synchronized (this) {
		if (session == null) {
		    session = new Session(dataSource, sqlAndParamsPreparer);
		}
	    }
	}
	return session;
    }
}
