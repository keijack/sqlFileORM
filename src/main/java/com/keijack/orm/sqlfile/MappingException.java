package com.keijack.orm.sqlfile;

public class MappingException extends RuntimeException {

    public MappingException(String message) {
	super(message);
    }

    public MappingException(Exception e) {
	super(e);
    }

    private static final long serialVersionUID = 1L;

}
