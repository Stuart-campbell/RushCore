package co.uk.rushorm.core.search;

import java.util.Map;

import co.uk.rushorm.core.AnnotationCache;
import co.uk.rushorm.core.Rush;
import co.uk.rushorm.core.RushCore;
import co.uk.rushorm.core.implementation.ReflectionUtils;
import co.uk.rushorm.core.implementation.RushSqlUtils;

/**
 * Created by Stuart on 22/03/15.
 */
public class RushWhereChild extends RushWhere {

    private static final String JOIN_CHILD = "JOIN %s on (%s." + RushSqlUtils.RUSH_ID + "=%s.child)";

    private final String field;
    private final String id;
    private final Class<? extends Rush> clazz;
    private final String className;
    private final String modifier;

    public RushWhereChild(String field, String id, Class<? extends Rush> clazz, String modifier) {
        this.field = field;
        this.id = id;
        this.clazz = clazz;
        className = RushCore.getInstance().getAnnotationCache().get(clazz).getSerializationName();
        this.modifier = modifier;
    }

    public String getStatement(Class<? extends Rush> childClazz, StringBuilder joinString){
        Map<Class<? extends Rush>, AnnotationCache> annotationCache = RushCore.getInstance().getAnnotationCache();
        String joinTable = ReflectionUtils.joinTableNameForClass(annotationCache.get(clazz).getTableName(), annotationCache.get(childClazz).getTableName(), field);
        String parentTable = annotationCache.get(childClazz).getTableName();
        joinString.append("\n").append(String.format(JOIN_CHILD, joinTable, parentTable, joinTable));
        return joinTable + ".parent" + modifier + "'" + id + "'";
    }

    @Override
    public String toString() {
        return "{\"field\":\"" + field + "\"," +
                "\"modifier\":\"" + modifier + "\"," +
                "\"id\":\"" + id + "\"," +
                "\"class\":\"" + className + "\"," +
                "\"type\":\"whereChild\"}";
    }
}
