package com.keijack.orm.sqlfile;

import java.util.Map;

/**
 * 
 * @author keijack.wu
 *
 */
public interface SqlAndParamsPreparer {

    /**
     * 准备相关的 SQL 和查询条件
     * 
     * @param clazz
     *            entity 对象的类名
     * @param params
     *            需要替换的参数
     * @return
     */
    SqlAndParams prepare(Class<?> clazz, Map<String, Object> params);
}
