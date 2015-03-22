package co.uk.rushorm.core.search;

import co.uk.rushorm.core.Rush;
import co.uk.rushorm.core.RushCore;
import co.uk.rushorm.core.implementation.ReflectionUtils;
import co.uk.rushorm.core.implementation.RushSqlUtils;

/**
 * Created by Stuart on 22/03/15.
 */
public class RushWhereHasChild extends RushWhere {

    private static final String JOIN = "JOIN %s on (%s." + RushSqlUtils.RUSH_ID + "=%s.parent)";

    private final String field;
    private final String id;
    private final Class<? extends Rush> clazz;
    private final String className;
    private final String modifier;

    public RushWhereHasChild(String field, String id, Class<? extends Rush> clazz, String modifier) {
        this.field = field;
        this.id = id;
        this.clazz = clazz;
        className = RushCore.getInstance().getAnnotationCache().get(clazz).getSerializationName();
        this.modifier = modifier;
    }

    public String getStatement(Class<? extends Rush> parentClazz, StringBuilder joinString){
        String joinTable = ReflectionUtils.joinTableNameForClass(parentClazz, clazz, field, RushCore.getInstance().getAnnotationCache());
        String parentTable = ReflectionUtils.tableNameForClass(parentClazz, RushCore.getInstance().getAnnotationCache());
        joinString.append("\n").append(String.format(JOIN, joinTable, parentTable, joinTable));
        return joinTable + ".child" + modifier + "'" + id + "'";
    }

    @Override
    public String toString() {
        return "{\"field\":\"" + field + "\"," +
                "\"modifier\":\"" + modifier + "\"," +
                "\"id\":\"" + id + "\"," +
                "\"class\":\"" + className + "\"," +
                "\"type\":\"whereParent\"}";
    }
}
