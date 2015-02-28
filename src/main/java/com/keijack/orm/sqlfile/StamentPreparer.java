package com.keijack.orm.sqlfile;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.keijack.orm.sqlfile.annotations.Entity;

class StamentPreparer {

    private static final int READ_FILE_BUFF_SIZE = 512;

    private String countSql;

    private String sql;

    private List<Object> params;

    private void setCountSql(String countSql) {
	this.countSql = countSql;
    }

    private void setSql(String sql) {
	this.sql = sql;
    }

    private void setParams(List<Object> params) {
	this.params = params;
    }

    public String getCountSql() {
	return countSql;
    }

    public String getSql() {
	return sql;
    }

    public List<Object> getParams() {
	return params;
    }

    public static <T> StamentPreparer prepare(Class<T> clazz, Map<String, Object> model) throws IOException {
	Entity ano = clazz.getAnnotation(Entity.class);
	if (ano == null) {
	    throw new IllegalArgumentException(clazz.getName() + " is not mapped.");
	}
	String path = StamentPreparer.class.getClassLoader().getResource("").getPath() + "/" + ano.path();
	String sqlTemplate = getSqlTemplate(path);

	checkRequriedTags(sqlTemplate, model);

	List<String> allTags = getAllTags(sqlTemplate);

	StamentPreparer preparer = new StamentPreparer();

	prepare(sqlTemplate, allTags, model, preparer);

	return preparer;
    }

    private static void prepare(String sqlTemplate, List<String> allTags, Map<String, Object> model,
	    StamentPreparer preparer) {
	String sql = sqlTemplate;
	List<Object> params = new ArrayList<>(allTags.size());
	for (String tag : allTags) {
	    Matcher matcher = Pattern.
		    compile("--#" + tag + "(?:@required){0,1}:\\s*([\\s\\S]*\\S)\\s*--" + tag + "#")
		    .matcher(sql);
	    while (matcher.find()) {
		if (!model.containsKey(tag) || model.get(tag) == null) {
		    sql = sql.replace(matcher.group(), "");
		    continue;
		}

		String sqlFragment = matcher.group(1);
		Object param = model.get(tag);

		if (param instanceof Collection) {
		    Collection<?> collectionParam = (Collection<?>) param;

		    String actureReplacer = "";
		    for (int i = 0; i < collectionParam.size(); i++) {
			if (!actureReplacer.isEmpty()) {
			    actureReplacer += ",";
			}
			actureReplacer += "?";
		    }
		    sqlFragment = sqlFragment.replaceAll("\\?", actureReplacer);
		    params.addAll(collectionParam);
		} else {
		    params.add(param);
		}

		sql = sql.replace(matcher.group(), sqlFragment);
	    }
	}

	Matcher matcher = Pattern.compile("--#(count\\([^\\n]*)").matcher(sql);
	String countSelect;
	if (matcher.find()) {
	    countSelect = matcher.group(1);
	} else {
	    countSelect = "count(*)";
	}

	preparer.setCountSql(formatSql("select " + countSelect + " from (" + sql + " )"));
	preparer.setSql(formatSql(sql));
	preparer.setParams(params);
    }

    private static String formatSql(String sql) {
	return sql.replaceAll("--[^\\n]*", "")
		.replaceAll("\\/\\*(?:\\s|.)*?\\*\\/", "")
		.replaceAll("\\s+", " ");

    }

    private static List<String> getAllTags(String sqlTemplate) {
	List<String> allTags = new ArrayList<>();
	Matcher matcher = Pattern.compile("--#(\\w*)(?:@required){0,1}:").matcher(sqlTemplate);
	while (matcher.find()) {
	    allTags.add(matcher.group(1));
	}
	return allTags;
    }

    private static void checkRequriedTags(String sqlTemplate, Map<String, Object> model) {
	List<String> missingTags = new ArrayList<>();
	Matcher matcher = Pattern.compile("--#(\\w*)@required:").matcher(sqlTemplate);
	while (matcher.find()) {
	    String tag = matcher.group(1);
	    if (!model.keySet().contains(tag)) {
		missingTags.add(tag);
	    }
	}
	if (!missingTags.isEmpty()) {
	    throw new IllegalArgumentException("Following tags are requried: " + missingTags);
	}
    }

    private static String getSqlTemplate(String path) throws IOException {
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
