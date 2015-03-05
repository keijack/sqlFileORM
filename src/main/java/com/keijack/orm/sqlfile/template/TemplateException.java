package com.keijack.orm.sqlfile.template;

public class TemplateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TemplateException(String message) {
	super(message);
    }

    public TemplateException(Throwable cause) {
	super(cause);
    }

}
