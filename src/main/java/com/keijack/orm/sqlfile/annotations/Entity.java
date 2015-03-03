package com.keijack.orm.sqlfile.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Entity {
    /**
     * Where's the root folder.
     * 
     * @return
     */
    RootFolder root() default RootFolder.ENTITY_PATH;

    /**
     * path
     * 
     * @return
     */
    String path();

}
