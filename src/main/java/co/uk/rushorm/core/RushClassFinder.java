package co.uk.rushorm.core;

import java.util.List;

/**
 * Created by stuartc on 11/12/14.
 */
public interface RushClassFinder {
    public List<Class<? extends Rush>> findClasses(RushConfig rushConfig);
}
