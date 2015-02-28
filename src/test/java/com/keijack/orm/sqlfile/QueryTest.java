package com.keijack.orm.sqlfile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class QueryTest {

    @Test
    public void testQuery() {
	try {
	    ComboPooledDataSource ds = new ComboPooledDataSource();
	    ds.setDriverClass("org.h2.Driver");
	    ds.setJdbcUrl("jdbc:h2:sqlFileOrm");
	    ds.setUser("keijack");
	    ds.setPassword("keijack");

	    SessionFactory fac = new SessionFactory();
	    fac.setDataSource(ds);

	    Map<String, Object> params = new HashMap<>();
	    List<Integer> idin = new ArrayList<>(2);
	    idin.add(1);
	    idin.add(2);
	    params.put("idIn", idin);

	    Session session = fac.getCurrentSession();
	    List<TestEntity> res = session.query(TestEntity.class, params);

	    assertEquals(2, res.size());
	    assertEquals(new Integer(1), res.get(0).getId());
	    assertEquals("Hello", res.get(0).getName());
	    System.out.println(res.get(0).getCreateTime());
	} catch (Throwable t) {
	    t.printStackTrace();
	    fail();
	}
    }
}
