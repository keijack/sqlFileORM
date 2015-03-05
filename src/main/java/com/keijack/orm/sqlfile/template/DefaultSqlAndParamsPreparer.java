package com.keijack.orm.sqlfile.template;

import java.util.Map;

import com.keijack.orm.sqlfile.SqlAndParams;
import com.keijack.orm.sqlfile.SqlAndParamsPreparer;

public class DefaultSqlAndParamsPreparer implements SqlAndParamsPreparer {

    @Override
    public SqlAndParams prepare(Class<?> clazz, Map<String, Object> params) {
	try {
	    return StamentPreparer.INSTANCE.prepare(clazz, params);
	} catch (Throwable t) {
	    throw new TemplateException(t);
	}
    }

}
