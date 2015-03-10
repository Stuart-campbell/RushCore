package co.uk.rushorm.core.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import co.uk.rushorm.core.Rush;

/**
 * Created by stuartc on 11/12/14.
 */

@Retention(RetentionPolicy.RUNTIME)
public @interface RushList {
    public Class<? extends Rush> classType();
    public Class<? extends List> listType() default ArrayList.class;
}
