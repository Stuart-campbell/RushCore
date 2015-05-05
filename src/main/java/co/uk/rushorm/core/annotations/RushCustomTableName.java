package co.uk.rushorm.core.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Stuart on 05/05/15.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RushCustomTableName {
    public String name();
}
