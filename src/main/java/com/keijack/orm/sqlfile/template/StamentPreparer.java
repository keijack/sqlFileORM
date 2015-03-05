package com.keijack.orm.sqlfile.template;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.keijack.orm.sqlfile.MappingException;
import com.keijack.orm.sqlfile.SqlAndParams;
import com.keijack.orm.sqlfile.annotations.Entity;

enum StamentPreparer {
    INSTANCE;

    private static final int READ_FILE_BUFF_SIZE = 512;

    public SqlAndParams prepare(Class<?> clazz, Map<String, Object> parameters) throws IOException {

	String sqlTemplate = getSqlTemplate(clazz);
	Map<String, Object> model = ignoreNullValue(parameters);

	checkRequriedTags(sqlTemplate, model);

	Map<String, List<Tag>> validRows = getValidRows(sqlTemplate, model);

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

    private List<String> replaceAllTags(Map<String, List<Tag>> validRows, Map<String, Object> model, List<Object> params) {
	List<String> rows = new ArrayList<>(validRows.size());
	for (String row : validRows.keySet()) {
	    rows.add(replaceTags(row, validRows.get(row), model, params));
	}
	return rows;
    }

    private String replaceTags(String row, List<Tag> tags, Map<String, Object> model, List<Object> params) {
	String r = row;
	for (Tag tag : tags) {
	    if (TagType.FRAGMENT.equals(tag.getType())) {
		r = replaceSqlTag(model, r, tag);
	    } else {
		r = replaceObjectTag(r, tag, model, params);
	    }
	}
	return r;
    }

    private String replaceSqlTag(Map<String, Object> model, String r, Tag tag) {
	Object obj = model.get(tag.getName());
	String sql = obj == null ? "" : obj.toString();
	return r.replace(tag.getTagString(), sql);
    }

    private String replaceObjectTag(String r, Tag tag, Map<String, Object> model,
	    List<Object> params) {
	String row = r;
	Object val = model.get(tag.getName());
	if (val instanceof Collection) {
	    String replacement = "";
	    Collection<?> col = (Collection<?>) val;
	    for (int i = 0; i < col.size(); i++) {
		if (!replacement.isEmpty()) {
		    replacement += ",";
		}
		replacement += "?";
	    }
	    row = row.replace(tag.getTagString(), replacement);
	    params.addAll(col);
	} else {
	    row = row.replace(tag.getTagString(), "?");
	    params.add(val);
	}
	return row;
    }

    private Map<String, List<Tag>> getValidRows(String sqlTemplate, Map<String, Object> model) {
	List<String> rowsInSqlTemplate = getAllRows(sqlTemplate);
	Map<String, List<Tag>> rows = new LinkedHashMap<>(rowsInSqlTemplate.size());
	for (String row : rowsInSqlTemplate) {
	    List<Tag> allTags = getTags(row);
	    List<String> noramlTagsInRow = getValideTags(row, allTags);
	    if (model.keySet().containsAll(noramlTagsInRow)) {
		rows.put(row, allTags);
	    }
	}
	return rows;
    }

    private List<String> getAllRows(String sqlTemplate) {
	return Arrays.asList(sqlTemplate.split("\\n"));
    }

    private List<String> getValideTags(String sqlFragment, Collection<Tag> tags) {
	List<String> valideTags = new ArrayList<>();
	for (Tag tag : tags) {
	    if (!ModifierType.OPTIONAL.equals(tag.getModifier())) {
		valideTags.add(tag.getName());
	    }
	}
	return valideTags;
    }

    private List<Tag> getTags(String sqlFragment) {
	List<Tag> tags = new ArrayList<>();
	Matcher matcher = Pattern.compile(":\\w*(?:@\\w*)?(?:\\[\\w*\\])?").matcher(sqlFragment);
	while (matcher.find()) {
	    Tag tag = new Tag(matcher.group());
	    tags.add(tag);
	}
	return tags;
    }

    private void checkRequriedTags(String sqlFragment, Map<String, Object> model) {
	List<String> requiredTags = getRequiredTags(sqlFragment);
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

    private List<String> getRequiredTags(String sqlFragment) {
	List<String> tags = new ArrayList<>();
	Matcher matcher = Pattern.compile(":(\\w*)(?:@\\w*)?\\[required\\]").matcher(sqlFragment);
	while (matcher.find()) {
	    tags.add(matcher.group(1));
	}
	return tags;
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
