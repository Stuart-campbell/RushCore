package co.uk.rushorm.core;

import java.util.List;
import java.util.Map;

/**
 * Created by Stuart on 12/04/15.
 */
public interface RushJoinStatementGenerator {

    public interface Callback {
        public void runSql(String sql);
    }

    public void createJoins(List<RushJoin> joins, Callback callback, Map<Class<? extends Rush>, AnnotationCache> annotationCache);
    public void deleteJoins(List<RushJoin> joins, Callback callback, Map<Class<? extends Rush>, AnnotationCache> annotationCache);
    public void deleteAll(Class<? extends Rush> parent, String field, Class<? extends Rush> child, String id, Callback callback, Map<Class<? extends Rush>, AnnotationCache> annotationCache);
}
