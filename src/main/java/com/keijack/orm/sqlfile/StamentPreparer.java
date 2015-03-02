package com.keijack.orm.sqlfile;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.keijack.orm.sqlfile.annotations.Entity;

class StamentPreparer {

    private static final int READ_FILE_BUFF_SIZE = 512;

    private static class SingletonHolder {
	private static StamentPreparer instance = new StamentPreparer();
    }

    public static StamentPreparer getInstance() {
	return SingletonHolder.instance;
    }

    private StamentPreparer() {
	super();
    }

    public <T> SqlAndParams prepare(Class<T> clazz, Map<String, Object> model) throws IOException {
	Entity ano = clazz.getAnnotation(Entity.class);
	if (ano == null) {
	    throw new IllegalArgumentException(clazz.getName() + " is not mapped.");
	}
	String path = StamentPreparer.class.getClassLoader().getResource("").getPath() + "/" + ano.path();
	String sqlTemplate = getSqlTemplate(path);

	List<String> requiredTags = getRequiredTags(sqlTemplate);
	checkRequriedTags(requiredTags, model);

	return prepare(sqlTemplate, model);
    }

    private SqlAndParams prepare(String sqlTemplate, Map<String, Object> model) {

	List<Object> params = new ArrayList<>(model.size());

	List<String> validRows = getValidRows(sqlTemplate, model);

	validRows = replaceNormalTags(validRows, model, params);

	validRows = replaceStringTags(validRows, model);

	validRows = replaceOptionalStringTags(validRows, model);

	String sql = join(validRows);

	String countSql = getCountSql(sql);

	SqlAndParams stamentPreparer = new SqlAndParams();
	stamentPreparer.setSql(formatSql(sql));
	stamentPreparer.setParams(params);

	stamentPreparer.setCountSql(formatSql(countSql));

	return stamentPreparer;
    }

    private String getCountSql(String sql) {
	Matcher matcher = Pattern.compile("--#(count\\([^\\n]*)").matcher(sql);
	String countSelect;
	if (matcher.find()) {
	    countSelect = matcher.group(1);
	} else {
	    countSelect = "count(*)";
	}

	return "select " + countSelect + " from (\n" + sql + "\n)";
    }

    private String join(List<String> validRows) {
	String sql = "";
	for (String row : validRows) {
	    sql += row + "\n";
	}
	return sql;
    }

    private List<String> replaceOptionalStringTags(List<String> validRows, Map<String, Object> model) {
	List<String> rows = new ArrayList<>();
	for (String row : validRows) {
	    List<String> stringTags = getOptionalStringTags(row);
	    for (String tag : stringTags) {
		if (model.containsKey(tag)) {
		    row = row.replaceFirst(":" + tag + "@optionalString", model.get(tag).toString());
		} else {
		    row = row.replaceFirst(":" + tag + "@optionalString", "");
		}
	    }
	    rows.add(row);
	}
	return rows;
    }

    private List<String> replaceStringTags(List<String> validRows, Map<String, Object> model) {
	List<String> rows = new ArrayList<>();
	for (String row : validRows) {
	    List<String> stringTags = getStringTags(row);
	    for (String tag : stringTags) {
		row = row.replaceFirst(":" + tag + "@string", model.get(tag).toString());
	    }
	    rows.add(row);
	}
	return rows;
    }

    private List<String> replaceNormalTags(List<String> validRows, Map<String, Object> model, List<Object> params) {
	List<String> rows = new ArrayList<>();
	for (String row : validRows) {
	    List<String> normalTags = getNormalTags(row);
	    rows.add(replaceNormalTags(row, normalTags, model, params));
	}
	return rows;
    }

    private String replaceNormalTags(String row, List<String> normalTags, Map<String, Object> model,
	    List<Object> params) {
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

    private String formatSql(String sql) {
	return sql.replaceAll("--[^\\n]*", "")
		.replaceAll("\\/\\*(?:\\s|.)*?\\*\\/", "")
		.replaceAll("\\s+", " ");

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

    private String getSqlTemplate(String path) throws IOException {
	try (FileReader in = new FileReader(path);
		StringWriter out = new StringWriter()) {
	    char[] cbuf = new char[READ_FILE_BUFF_SIZE];
	    while (in.read(cbuf) != -1) {
		out.write(cbuf);
	    }
	    return out.toString();
	}
    }
}
