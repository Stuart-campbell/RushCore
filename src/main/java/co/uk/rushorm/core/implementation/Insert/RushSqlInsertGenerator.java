package co.uk.rushorm.core.implementation.Insert;

import java.util.List;
import java.util.Map;

import co.uk.rushorm.core.AnnotationCache;
import co.uk.rushorm.core.Rush;
import co.uk.rushorm.core.RushSaveStatementGeneratorCallback;

/**
 * Created by Stuart on 03/04/15.
 */
public interface RushSqlInsertGenerator {

    public void createManyJoins(Map<String, List<BasicJoin>> joinValues, final RushSaveStatementGeneratorCallback saveCallback);
    public void createOrUpdateObjects(Map<Class<? extends Rush>, List<BasicUpdate>> valuesMap, final Map<Class<? extends Rush>, List<String>> columnsMap, final Map<Class<? extends Rush>, AnnotationCache> annotationCache, final RushSaveStatementGeneratorCallback saveCallback);

}
