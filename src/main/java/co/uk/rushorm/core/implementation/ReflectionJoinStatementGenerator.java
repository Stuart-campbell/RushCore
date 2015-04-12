package co.uk.rushorm.core.implementation;

import java.util.List;
import java.util.Map;

import co.uk.rushorm.core.AnnotationCache;
import co.uk.rushorm.core.Rush;
import co.uk.rushorm.core.RushJoin;
import co.uk.rushorm.core.RushJoinStatementGenerator;

/**
 * Created by Stuart on 12/04/15.
 */
public class ReflectionJoinStatementGenerator implements RushJoinStatementGenerator {

    @Override
    public void createJoins(List<RushJoin> joins, Callback callback, Map<Class<? extends Rush>, AnnotationCache> annotationCache) {
        for (RushJoin rushJoin : joins) {
            String tableName = ReflectionUtils.joinTableNameForClass(rushJoin.getParent(), rushJoin.getChild().getClass(), rushJoin.getField(), annotationCache);
            String sql = String.format(RushSqlUtils.INSERT_JOIN_TEMPLATE, tableName, rushJoin.getParentId(), rushJoin.getChild().getId());
            callback.runSql(sql);
        }

    }

    @Override
    public void deleteJoins(List<RushJoin> joins, Callback callback, Map<Class<? extends Rush>, AnnotationCache> annotationCache) {
        for (RushJoin rushJoin : joins) {
            String tableName = ReflectionUtils.joinTableNameForClass(rushJoin.getParent(), rushJoin.getChild().getClass(), rushJoin.getField(), annotationCache);
            String sql = String.format(RushSqlUtils.DELETE_JOIN_TEMPLATE, tableName, rushJoin.getParentId(), rushJoin.getChild().getId());
            callback.runSql(sql);
        }
    }

    @Override
    public void deleteAll(Class<? extends Rush> parent, String field, Class<? extends Rush> child, String id, Callback callback, Map<Class<? extends Rush>, AnnotationCache> annotationCache) {
        String tableName = ReflectionUtils.joinTableNameForClass(parent, child, field, annotationCache);
        String sql = String.format(RushSqlUtils.DELETE_ALL_JOIN_TEMPLATE, tableName, id);
        callback.runSql(sql);
    }
}
