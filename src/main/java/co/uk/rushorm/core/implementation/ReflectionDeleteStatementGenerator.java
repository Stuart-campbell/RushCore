package co.uk.rushorm.core.implementation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.uk.rushorm.core.AnnotationCache;
import co.uk.rushorm.core.Rush;
import co.uk.rushorm.core.RushConfig;
import co.uk.rushorm.core.RushDeleteStatementGenerator;
import co.uk.rushorm.core.RushListField;

/**
 * Created by Stuart on 16/02/15.
 */
public class ReflectionDeleteStatementGenerator implements RushDeleteStatementGenerator {

    private final RushConfig rushConfig;

    public ReflectionDeleteStatementGenerator(RushConfig rushConfig) {
        this.rushConfig = rushConfig;
    }

    @Override
    public void generateDelete(List<? extends Rush> objects, Map<Class<? extends Rush>, AnnotationCache> annotationCache, RushDeleteStatementGenerator.Callback callback) {
        Map<String, List<String>> joinDeletes = new HashMap<>();
        Map<String, List<String>> deletes = new HashMap<>();

        for (Rush object : objects) {
            generateDelete(object, annotationCache, deletes, joinDeletes, callback);
        }

        ReflectionUtils.deleteManyJoins(joinDeletes, callback);
        deleteMany(deletes, callback);
    }

    @Override
    public void generateDeleteAll(Class<? extends Rush> clazz, Map<Class<? extends Rush>, AnnotationCache> annotationCache, Callback deleteCallback) {
        List<Field> fields = new ArrayList<>();
        ReflectionUtils.getAllFields(fields, clazz, rushConfig.orderColumnsAlphabetically());

        for (Field field : fields) {
            if (!annotationCache.get(clazz).getFieldToIgnore().contains(field.getName())) {
                String joinTableName = null;
                if (Rush.class.isAssignableFrom(field.getType())) {
                    joinTableName = ReflectionUtils.joinTableNameForClass(annotationCache.get(clazz).getTableName(), annotationCache.get((Class<? extends Rush>) field.getType()).getTableName(), field.getName());
                } else if (annotationCache.get(clazz).getListsClasses().containsKey(field.getName())) {
                    joinTableName = ReflectionUtils.joinTableNameForClass(annotationCache.get(clazz).getTableName(), annotationCache.get(annotationCache.get(clazz).getListsClasses().get(field.getName())).getTableName(), field.getName());
                }
                if(joinTableName != null) {
                    deleteCallback.deleteStatement("DELETE FROM " + joinTableName + ";");
                }
            }
        }
        deleteCallback.deleteStatement("DELETE FROM " + annotationCache.get(clazz).getTableName() + ";");
        deleteCallback.deleteStatement("VACUUM;");
    }

    public void generateDelete(Rush rush, Map<Class<? extends Rush>, AnnotationCache> annotationCache, Map<String, List<String>> deletes, Map<String, List<String>> joinDeletes, RushDeleteStatementGenerator.Callback callback) {

        if (rush.getId() == null) {
            return;
        }

        String id = rush.getId();
        callback.removeRush(rush);

        List<Field> fields = new ArrayList<>();
        ReflectionUtils.getAllFields(fields, rush.getClass(), rushConfig.orderColumnsAlphabetically());

        for (Field field : fields) {
            field.setAccessible(true);
            if (!annotationCache.get(rush.getClass()).getFieldToIgnore().contains(field.getName())) {
                String joinTableName = null;
                if (Rush.class.isAssignableFrom(field.getType())) {
                    try {
                        Rush child = (Rush) field.get(rush);
                        if (child != null) {
                            joinTableName = ReflectionUtils.joinTableNameForClass(annotationCache.get(rush.getClass()).getTableName(), annotationCache.get(child.getClass()).getTableName(), field.getName());
                            if (!annotationCache.get(rush.getClass()).getDisableAutoDelete().contains(field.getName())) {
                                generateDelete(child, annotationCache, deletes, joinDeletes, callback);
                            }
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else if (annotationCache.get(rush.getClass()).getListsClasses().containsKey(field.getName())) {
                    try {
                        if(List.class.isAssignableFrom(field.getType())) {
                            List<Rush> fieldChildren = (List<Rush>) field.get(rush);
                            if (fieldChildren != null && fieldChildren.size() > 0) {
                                joinTableName = ReflectionUtils.joinTableNameForClass(annotationCache.get(rush.getClass()).getTableName(), annotationCache.get(annotationCache.get(rush.getClass()).getListsClasses().get(field.getName())).getTableName(), field.getName());
                                if (!annotationCache.get(rush.getClass()).getDisableAutoDelete().contains(field.getName())) {
                                    for (Rush child : fieldChildren) {
                                        generateDelete(child, annotationCache, deletes, joinDeletes, callback);
                                    }
                                }
                            }
                        } else if(RushListField.class.isAssignableFrom(field.getType())) {
                            // TODO delete values
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                if(joinTableName != null) {
                    if (!joinDeletes.containsKey(joinTableName)) {
                        joinDeletes.put(joinTableName, new ArrayList<String>());
                    }
                    joinDeletes.get(joinTableName).add(id);
                }
            }
        }

        String table = annotationCache.get(rush.getClass()).getTableName();
        if(!deletes.containsKey(table)) {
            deletes.put(table, new ArrayList<String>());
        }
        deletes.get(table).add(id);

    }

    private void deleteMany(Map<String, List<String>> deletes, final RushDeleteStatementGenerator.Callback saveCallback) {

        for (final Map.Entry<String, List<String>> entry : deletes.entrySet()) {
            final StringBuilder columnsString = new StringBuilder();
            final List<String> values = entry.getValue();

            ReflectionUtils.doLoop(values.size(), ReflectionUtils.GROUP_SIZE, new ReflectionUtils.LoopCallBack() {
                @Override
                public void start() {
                    columnsString.delete(0, columnsString.length());
                }

                @Override
                public void actionAtIndex(int index) {
                    columnsString.append(RushSqlUtils.RUSH_ID)
                            .append("='")
                            .append(values.get(index))
                            .append("'");
                }

                @Override
                public void join() {
                    columnsString.append(" OR ");
                }

                @Override
                public void doAction() {
                    String sql = String.format(RushSqlUtils.MULTIPLE_DELETE_TEMPLATE, entry.getKey(),
                            columnsString.toString());
                    saveCallback.deleteStatement(sql);
                }
            });
        }
    }

}
