package com.keijack.orm.sqlfile;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.keijack.orm.sqlfile.annotations.Entity;

enum StamentPreparer {
    INSTANCE;

    private static final int READ_FILE_BUFF_SIZE = 512;

    public SqlAndParams prepare(Class<?> clazz, Map<String, Object> parameters) throws IOException {

	String sqlTemplate = getSqlTemplate(clazz);
	Map<String, Object> model = ignoreNullValue(parameters);

	List<String> requiredTags = getRequiredTags(sqlTemplate);
	checkRequriedTags(requiredTags, model);

	List<String> validRows = getValidRows(sqlTemplate, model);

	List<Object> params = new ArrayList<>(model.size());
	List<String> rows = replaceAllTags(validRows, model, params);

	String sql = join(rows);

	SqlAndParams stamentPreparer = new SqlAndParams();
	stamentPreparer.setSql(formatSql(sql));
	stamentPreparer.setCountSql(formatSql(getCountSql(sql)));
	stamentPreparer.setParams(params);

	return stamentPreparer;
    }

    private Map<String, Object> ignoreNullValue(Map<String, Object> parameters) {
	Map<String, Object> model = new HashMap<>(parameters.size());
	for (String key : parameters.keySet()) {
	    Object val = parameters.get(key);
	    if (val != null) {
		model.put(key, val);
	    }
	}
	return model;
    }

    private String formatSql(String sql) {
	return sql.replaceAll("--[^\\n]*", "")
		.replaceAll("\\/\\*(?:\\s|.)*?\\*\\/", "")
		.replaceAll("\\s+", " ").trim();

    }

    private String getCountSql(String sql) {
	Matcher matcher = Pattern.compile("--#(count\\([^\\n]*)").matcher(sql);
	String countSelect;
	if (matcher.find()) {
	    countSelect = matcher.group(1);
	} else {
	    countSelect = "count(*)";
	}

	return "select " + countSelect + " from (\n" + sql + "\n) RESULT";
    }

    private String join(List<String> validRows) {
	String sql = "";
	for (String row : validRows) {
	    sql += row + "\n";
	}
	return sql;
    }

    private List<String> replaceAllTags(List<String> validRows, Map<String, Object> model, List<Object> params) {
	List<String> rows = new ArrayList<>(validRows.size());
	for (String row : validRows) {
	    row = replaceNormalTags(row, model, params);
	    row = replaceStringTags(row, model);
	    row = replaceOptionalStrinTags(row, model);
	    rows.add(row);
	}
	return rows;
    }

    private String replaceOptionalStrinTags(String row, Map<String, Object> model) {
	List<String> stringTags = getOptionalStringTags(row);
	for (String tag : stringTags) {
	    if (model.containsKey(tag)) {
		row = row.replaceFirst(":" + tag + "@optionalString", model.get(tag).toString());
	    } else {
		row = row.replaceFirst(":" + tag + "@optionalString", "");
	    }
	}
	return row;
    }

    private String replaceStringTags(String row, Map<String, Object> model) {
	List<String> stringTags = getStringTags(row);
	for (String tag : stringTags) {
	    row = row.replaceFirst(":" + tag + "@string", model.get(tag).toString());
	}
	return row;
    }

    private String replaceNormalTags(String row, Map<String, Object> model,
	    List<Object> params) {
	List<String> normalTags = getNormalTags(row);
	for (String tag : normalTags) {
	    Object val = model.get(tag);
	    if (val instanceof Collection) {
		String replacement = "";
		Collection<?> col = (Collection<?>) val;
		for (int i = 0; i < col.size(); i++) {
		    if (!replacement.isEmpty()) {
			replacement += ",";
		    }
		    replacement += "?";
		}
		row = row.replaceFirst(":" + tag + "(?:@required)?", replacement);
		params.addAll(col);
	    } else {
		row = row.replaceFirst(":" + tag + "(?:@required)?", "?");
		params.add(val);
	    }
	}
	return row;
    }

    private List<String> getValidRows(String sqlTemplate, Map<String, Object> model) {
	List<String> rowsInSqlTemplate = getAllRows(sqlTemplate);
	List<String> rows = new ArrayList<>(rowsInSqlTemplate.size());
	for (String row : rowsInSqlTemplate) {
	    List<String> noramlTagsInRow = getValideTags(row);
	    if (model.keySet().containsAll(noramlTagsInRow)) {
		rows.add(row);
	    }
	}
	return rows;
    }

    private List<String> getAllRows(String sqlTemplate) {
	return Arrays.asList(sqlTemplate.split("\\n"));
    }

    private List<String> getValideTags(String sqlFragment) {
	List<String> normalTags = getNormalTags(sqlFragment);
	List<String> stringTags = getStringTags(sqlFragment);
	List<String> valideTags = new ArrayList<>(normalTags.size() + stringTags.size());
	valideTags.addAll(normalTags);
	valideTags.addAll(stringTags);
	return valideTags;
    }

    private List<String> getNormalTags(String sqlFragment) {
	List<String> tags = new ArrayList<>();
	Matcher matcher = Pattern.compile(":(\\w*)(?:@required)?(@*)").matcher(sqlFragment);
	while (matcher.find()) {
	    if (matcher.group(2).isEmpty()) {
		tags.add(matcher.group(1));
	    }
	}
	return tags;
    }

    private List<String> getStringTags(String sqlFragment) {
	List<String> tags = new ArrayList<>();
	Matcher matcher = Pattern.compile(":(\\w*)@string").matcher(sqlFragment);
	while (matcher.find()) {
	    tags.add(matcher.group(1));
	}
	return tags;
    }

    private List<String> getRequiredTags(String sqlFragment) {
	List<String> tags = new ArrayList<>();
	Matcher matcher = Pattern.compile(":(\\w*)@required").matcher(sqlFragment);
	while (matcher.find()) {
	    tags.add(matcher.group(1));
	}
	return tags;
    }

    private List<String> getOptionalStringTags(String sqlFragment) {
	List<String> tags = new ArrayList<>();
	Matcher matcher = Pattern.compile(":(\\w*)@optionalString").matcher(sqlFragment);
	while (matcher.find()) {
	    tags.add(matcher.group(1));
	}
	return tags;
    }

    private void checkRequriedTags(List<String> requiredTags, Map<String, Object> model) {
	List<String> missingTags = new ArrayList<>();
	for (String tag : requiredTags) {
	    if (!model.containsKey(tag) || model.get(tag) == null) {
		missingTags.add(tag);
	    }
	}
	if (!missingTags.isEmpty()) {
	    throw new IllegalArgumentException("The following query conditions is requried: " + missingTags);
	}
    }

    private String getSqlTemplate(Class<?> clazz) throws IOException {
	try (InputStreamReader in = new InputStreamReader(getSqlFileInputStream(clazz));
		StringWriter out = new StringWriter()) {
	    char[] cbuf = new char[READ_FILE_BUFF_SIZE];
	    int len;
	    while ((len = in.read(cbuf)) != -1) {
		out.write(cbuf, 0, len);
	    }
	    return out.toString();
	}
    }

    private InputStream getSqlFileInputStream(Class<?> clazz) throws FileNotFoundException {
	Entity ano = clazz.getAnnotation(Entity.class);
	if (ano == null) {
	    throw new MappingException(clazz.getName() + " is not mapped.");
	}
	switch (ano.root()) {
	case ABSOLUTE:
	    return new FileInputStream(ano.path());
	case CLASSPATH:
	    return clazz.getClassLoader().getResourceAsStream(ano.path());
	case ENTITY_PATH:
	    return clazz.getResourceAsStream(ano.path());
	default:
	    throw new MappingException("Cannot find root folder.");
	}
    }
}
