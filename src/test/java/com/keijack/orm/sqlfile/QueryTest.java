package com.keijack.orm.sqlfile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.beans.PropertyVetoException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class QueryTest {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");

    private SessionFactory fac;

    private Map<String, Object> params = new HashMap<>();

    @Before
    public void before() {
	// factory
	ComboPooledDataSource ds = new ComboPooledDataSource();
	try {
	    ds.setDriverClass("org.h2.Driver");
	} catch (PropertyVetoException e) {
	    fail();
	    e.printStackTrace();
	}
	ds.setJdbcUrl("jdbc:h2:sqlFileOrm");
	ds.setUser("keijack");
	ds.setPassword("keijack");

	fac = new SessionFactory();
	fac.setDataSource(ds);
	// params
	params.clear();
	List<Integer> idin = new ArrayList<>(2);
	idin.add(1);
	idin.add(2);
	params.put("idIn", idin);
	params.put("orderbyid", true);
    }

    @Test
    public void testQuery() {
	try {
	    Session session = fac.getCurrentSession();
	    List<TestEntity> res = session.query(TestEntity.class, params);

	    assertEquals(2, res.size());
	    assertEquals(new Integer(1), res.get(0).getId());
	    assertEquals("Hello", res.get(0).getName());
	    assertEquals("2015-02-28 11:05:40.10",
		    DATE_FORMAT.format(res.get(0).getCreateTime()));
	} catch (Throwable t) {
	    t.printStackTrace();
	    fail();
	}
    }

    @Test
    public void testCount() {
	try {
	    long size = fac.getCurrentSession().count(TestEntity.class, params);
	    assertEquals(2, size);
	} catch (Throwable t) {
	    t.printStackTrace();
	    fail();
	}
    }

    @Test
    public void testQueryInPage() {
	try {
	    Session session = fac.getCurrentSession();
	    List<TestEntity> res = session.query(TestEntity.class, params, 1, 1);

	    assertEquals(1, res.size());
	    assertEquals(new Integer(2), res.get(0).getId());
	    assertEquals("World", res.get(0).getName());
	    assertEquals("2015-02-28 11:05:40.61",
		    DATE_FORMAT.format(res.get(0).getCreateTime()));
	} catch (Throwable t) {
	    t.printStackTrace();
	    fail();
	}
    }
}
