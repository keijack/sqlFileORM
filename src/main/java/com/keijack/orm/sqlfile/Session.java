package com.keijack.orm.sqlfile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.keijack.orm.sqlfile.annotations.Column;

public class Session {

    private final DataSource dataSource;

    public Session(DataSource dataSource) {
	this.dataSource = dataSource;
    }

    /**
     * 
     * @param clazz
     *            需要查询的类，该类必须使用@Entity标注
     * @param params
     *            查询条件，map的key必须对应sql文件中的 --#[标签]:
     * @return
     */
    public <T> List<T> query(Class<T> clazz, Map<String, Object> params) {
	return this.query(clazz, params, 0, 0);
    }

    /**
     * 
     * @param clazz
     *            需要查询的类，该类必须使用@Entity标注
     * @param params
     *            查询条件，map的key必须对应sql文件中的 --#[标签]:
     * @return
     */
    public <T> long count(Class<T> clazz, Map<String, Object> params) {
	try (Connection con = dataSource.getConnection()) {
	    SqlAndParams sqlAndParams = StamentPreparer.getInstance().prepare(clazz, params);

	    PreparedStatement statement = con.prepareStatement(sqlAndParams.getCountSql(),
		    ResultSet.TYPE_SCROLL_INSENSITIVE,
		    ResultSet.CONCUR_READ_ONLY);
	    for (int i = 1; i <= sqlAndParams.getParams().size(); i++) {
		statement.setObject(i, sqlAndParams.getParams().get(i - 1));
	    }

	    statement.setMaxRows(1);

	    ResultSet result = statement.executeQuery();

	    result.first();
	    return result.getLong(1);
	} catch (SQLException | IOException e) {
	    throw new QueryException(e);
	}
    }

    /**
     * 
     * @param clazz
     *            需要查询的类，该类必须使用@Entity标注
     * @param params
     *            查询条件，map的key必须对应sql文件中的 --#[标签]:
     * @param firstResult
     *            查询的第一行
     * @param maxResults
     *            最大查询的内容，0表示无限制
     * @return
     */
    public <T> List<T> query(Class<T> clazz, Map<String, Object> params, int firstResult, int maxResults) {
	try (Connection con = dataSource.getConnection()) {
	    List<T> res = new ArrayList<>();
	    SqlAndParams sqlAndParams = StamentPreparer.getInstance().prepare(clazz, params);

	    PreparedStatement statement = con.prepareStatement(sqlAndParams.getSql(),
		    ResultSet.TYPE_SCROLL_INSENSITIVE,
		    ResultSet.CONCUR_READ_ONLY);
	    for (int i = 1; i <= sqlAndParams.getParams().size(); i++) {
		statement.setObject(i, sqlAndParams.getParams().get(i - 1));
	    }

	    statement.setMaxRows(firstResult + maxResults);

	    Map<String, Method> lableSetMethodMap = getAllMappedSetMethods(clazz);

	    ResultSet result = statement.executeQuery();
	    result.relative(firstResult);
	    while (result.next()) {
		ResultSetMetaData row = result.getMetaData();
		T obj = clazz.newInstance();
		for (int i = 1; i <= row.getColumnCount(); i++) {
		    String label = row.getColumnLabel(i);
		    Object value = result.getObject(i);

		    if (!lableSetMethodMap.containsKey(label)) {
			continue;
		    }

		    Method method = lableSetMethodMap.get(label);
		    method.invoke(obj, value);
		}
		res.add(obj);
	    }
	    return res;
	} catch (InstantiationException
		| IllegalAccessException
		| IllegalArgumentException
		| InvocationTargetException e) {
	    throw new MappingException(e);
	} catch (SQLException | IOException e) {
	    throw new QueryException(e);
	}

    }

    private <T> Map<String, Method> getAllMappedSetMethods(Class<T> clazz) {
	Map<String, Method> lableSetMethodMap = new HashMap<>();
	Field[] fields = clazz.getDeclaredFields();
	for (Field field : fields) {
	    Column ano = field.getAnnotation(Column.class);
	    if (ano == null)
		continue;
	    String label = ano.label();
	    String fieldName = field.getName();
	    if (label.isEmpty()) {
		label = fieldName;
	    }

	    String setMethodName = "set" + Character.toUpperCase(fieldName.charAt(0));
	    if (fieldName.length() > 1) {
		setMethodName = setMethodName + fieldName.substring(1);
	    }
	    try {
		Method setMethod = clazz.getMethod(setMethodName, field.getType());
		lableSetMethodMap.put(label, setMethod);
	    } catch (NoSuchMethodException | SecurityException e) {
		throw new MappingException(e);
	    }
	}
	return lableSetMethodMap;
    }
}
