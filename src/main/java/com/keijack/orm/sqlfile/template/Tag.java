package com.keijack.orm.sqlfile.template;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Tag {

    private Logger logger = Logger.getLogger(Tag.class.toString());

    private final String tagString;

    private String name;

    private TagType type = TagType.OBJECT;

    private ModifierType modifier = ModifierType.NORMAL;

    public Tag(String tagString) {
	this.tagString = tagString;
	decode(this.tagString);
    }

    private void decode(String tagString) {
	Matcher matcher = Pattern.compile(":(\\w*)(?:@(\\w*))?(?:\\[(\\w*)\\])?").matcher(tagString);
	if (matcher.find()) {
	    this.setName(matcher.group(1));
	    this.setType(matcher.group(2));
	    this.setModifier(matcher.group(3));
	}
	if (ModifierType.OPTIONAL.equals(this.modifier)
		&& !TagType.FRAGMENT.equals(this.type)) {
	    throw new TemplateException("Only sql can be optional");
	}
    }

    private void setName(String name) {
	this.name = name;
    }

    private void setType(String type) {
	if (type != null && !type.isEmpty()) {
	    try {
		this.type = TagType.valueOf(type.toUpperCase());
	    } catch (Throwable t) {
		logger.info("[" + type + "] is not supported yet, using [object] instead.");
		this.type = TagType.OBJECT;
	    }
	}
    }

    public void setModifier(String modifier) {
	if (modifier != null && !modifier.isEmpty()) {
	    try {
		this.modifier = ModifierType.valueOf(modifier.toUpperCase());
	    } catch (Throwable t) {
		logger.info("[" + modifier + "] is not supported yet, using [normal] instead.");
		this.modifier = ModifierType.NORMAL;
	    }
	}
    }

    public String getTagString() {
	return tagString;
    }

    public String getName() {
	return name;
    }

    public TagType getType() {
	return type;
    }

    public ModifierType getModifier() {
	return modifier;
    }

}
