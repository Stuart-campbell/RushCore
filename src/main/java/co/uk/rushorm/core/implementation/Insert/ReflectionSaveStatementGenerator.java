package co.uk.rushorm.core.implementation.Insert;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.uk.rushorm.core.AnnotationCache;
import co.uk.rushorm.core.Rush;
import co.uk.rushorm.core.RushColumns;
import co.uk.rushorm.core.RushListField;
import co.uk.rushorm.core.RushMetaData;
import co.uk.rushorm.core.RushSaveStatementGenerator;
import co.uk.rushorm.core.RushSaveStatementGeneratorCallback;
import co.uk.rushorm.core.RushStringSanitizer;
import co.uk.rushorm.core.exceptions.RushClassNotFoundException;
import co.uk.rushorm.core.implementation.ReflectionUtils;

/**
 * Created by stuartc on 11/12/14.
 */
public class ReflectionSaveStatementGenerator implements RushSaveStatementGenerator {


    private void addJoin(Map<String, List<BasicJoin>> joins, BasicJoin basicJoin) {
        if(!joins.containsKey(basicJoin.getTable())) {
            joins.put(basicJoin.getTable(), new ArrayList<BasicJoin>());
        }
        joins.get(basicJoin.getTable()).add(basicJoin);
    }

    private final RushSqlInsertGenerator rushSqlInsertGenerator;

    public ReflectionSaveStatementGenerator(RushSqlInsertGenerator rushSqlInsertGenerator) {
        this.rushSqlInsertGenerator = rushSqlInsertGenerator;
    }

    @Override
    public void generateSaveOrUpdate(List<? extends Rush> objects, Map<Class<? extends Rush>, AnnotationCache> annotationCache, RushStringSanitizer rushStringSanitizer, RushColumns rushColumns, RushSaveStatementGeneratorCallback saveCallback) {

        List<Rush> rushObjects = new ArrayList<>();

        Map<Class<? extends Rush>, List<BasicUpdate>> updateValues = new HashMap<>();
        Map<Class<? extends Rush>, List<String>> columns = new HashMap<>();

        Map<String, List<String>> joinDeletes = new HashMap<>();
        Map<String, List<BasicJoin>> joinValues = new HashMap<>();

        for(Rush rush : objects) {
            generateSaveOrUpdate(rush, rushObjects, annotationCache, rushStringSanitizer, rushColumns, updateValues, columns, joinDeletes, joinValues, saveCallback);
        }

        ReflectionUtils.deleteManyJoins(joinDeletes, saveCallback);

        createOrUpdateObjects(updateValues, columns, annotationCache, saveCallback);
        createManyJoins(joinValues, saveCallback);

    }

    private void generateSaveOrUpdate(Rush rush, List<Rush> rushObjects,
                                      Map<Class<? extends Rush>, AnnotationCache> annotationCache,
                                      RushStringSanitizer rushStringSanitizer,
                                      RushColumns rushColumns,
                                      Map<Class<? extends Rush>, List<BasicUpdate>> updateValuesMap,
                                      Map<Class<? extends Rush>, List<String>> columnsMap,
                                      Map<String, List<String>> joinDeletesMap,
                                      Map<String, List<BasicJoin>> joinValuesMap,
                                      RushSaveStatementGeneratorCallback saveCallback) {

        if (rushObjects.contains(rush)) {
            // Exit if object is referenced by child
            return;
        }

        if(annotationCache.get(rush.getClass()) == null) {
            throw new RushClassNotFoundException(rush.getClass());
        }

        rushObjects.add(rush);

        List<String> columns = new ArrayList<>();
        List<String> values = new ArrayList<>();
        List<Field> fields = new ArrayList<>();

        ReflectionUtils.getAllFields(fields, rush.getClass());

        for (Field field : fields) {
            if (!annotationCache.get(rush.getClass()).getFieldToIgnore().contains(field.getName())) {
                field.setAccessible(true);
                List<BasicJoin> joins = new ArrayList<>();
                String joinTableName = joinFromField(joins, rush, field, annotationCache);
                if (joinTableName == null) {
                    if (rushColumns.supportsField(field)) {
                        try {
                            String value = rushColumns.valueFromField(rush, field, rushStringSanitizer);
                            columns.add(field.getName());
                            values.add(value);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    if (rush.getId() != null) {
                        // Clear join tables and re save rows to catch any deleted or changed children
                        if (!joinDeletesMap.containsKey(joinTableName)) {
                            joinDeletesMap.put(joinTableName, new ArrayList<String>());
                        }
                        joinDeletesMap.get(joinTableName).add(rush.getId());
                    }
                    for(BasicJoin join : joins) {
                        generateSaveOrUpdate(join.getChild(), rushObjects, annotationCache, rushStringSanitizer, rushColumns, updateValuesMap, columnsMap, joinDeletesMap, joinValuesMap, saveCallback);
                        addJoin(joinValuesMap, join);
                    }
                }
            }
        }

        if (!columnsMap.containsKey(rush.getClass())) {
            columnsMap.put(rush.getClass(), columns);
        }

        if (!updateValuesMap.containsKey(rush.getClass())) {
            updateValuesMap.put(rush.getClass(), new ArrayList<BasicUpdate>());
        }

        RushMetaData rushMetaData = saveCallback.getMetaData(rush);
        if(rushMetaData == null) {
            rushMetaData = new RushMetaData();
            saveCallback.addRush(rush, rushMetaData);
        }
        updateValuesMap.get(rush.getClass()).add(new BasicUpdate(values, rush, rushMetaData));
    }

    private String joinFromField(List<BasicJoin> joins, Rush rush, Field field, Map<Class<? extends Rush>, AnnotationCache> annotationCache) {

        if (Rush.class.isAssignableFrom(field.getType())) {
            String tableName = ReflectionUtils.joinTableNameForClass(annotationCache.get(rush.getClass()).getTableName(), annotationCache.get((Class<? extends Rush>) field.getType()).getTableName(), field.getName());
            try {
                Rush child = (Rush) field.get(rush);
                if (child != null) {
                    joins.add(new BasicJoin(tableName, rush, child));
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return tableName;
        }

        if(annotationCache.get(rush.getClass()).getListsClasses().containsKey(field.getName())) {

            if(RushListField.class.isAssignableFrom(field.getType())) {
                // return null so that the join table is not cleared
                return null;
            }

            Class listClass = annotationCache.get(rush.getClass()).getListsClasses().get(field.getName());
            String tableName = ReflectionUtils.joinTableNameForClass(annotationCache.get(rush.getClass()).getTableName(), annotationCache.get(listClass).getTableName(), field.getName());
            if (Rush.class.isAssignableFrom(listClass)) {
                try {
                    if(List.class.isAssignableFrom(field.getType())) {
                        List<Rush> children = (List<Rush>) field.get(rush);
                        if (children != null) {
                            for (Rush child : children) {
                                joins.add(new BasicJoin(tableName, rush, child));
                            }
                        }
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            return tableName;
        }
        return null;
    }


    private void createManyJoins(Map<String, List<BasicJoin>> joinValues, final RushSaveStatementGeneratorCallback saveCallback) {
        rushSqlInsertGenerator.createManyJoins(joinValues, saveCallback);
    }

    protected void createOrUpdateObjects(Map<Class<? extends Rush>, List<BasicUpdate>> valuesMap, final Map<Class<? extends Rush>, List<String>> columnsMap, final Map<Class<? extends Rush>, AnnotationCache> annotationCache, final RushSaveStatementGeneratorCallback saveCallback) {
        rushSqlInsertGenerator.createOrUpdateObjects(valuesMap, columnsMap, annotationCache, saveCallback);
    }
}
