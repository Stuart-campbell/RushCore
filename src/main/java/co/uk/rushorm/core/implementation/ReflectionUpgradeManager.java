package co.uk.rushorm.core.implementation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import co.uk.rushorm.core.AnnotationCache;
import co.uk.rushorm.core.Logger;
import co.uk.rushorm.core.Rush;
import co.uk.rushorm.core.RushConfig;
import co.uk.rushorm.core.RushStatementRunner;
import co.uk.rushorm.core.RushUpgradeManager;
import co.uk.rushorm.core.annotations.RushIgnore;
import co.uk.rushorm.core.annotations.RushList;
import co.uk.rushorm.core.annotations.RushRenamed;

/**
 * Created by stuartc on 11/12/14.
 */
public class ReflectionUpgradeManager implements RushUpgradeManager {

    private class Mapping {
        private String oldName;
        private String newName;
        private Mapping(String oldNames, String newName) {
            this.oldName = oldNames;
            this.newName = newName;
        }
    }

    private class TableMapping {
        private boolean isJoin;
        private Mapping name;
        private List<Mapping> fields = new ArrayList<>();
        private List<String> indexes = new ArrayList<>();
    }

    private class PotentialTableMapping {
        private PotentialMapping name;
        private List<PotentialMapping> fields = new ArrayList<>();
    }

    private class PotentialMapping {
        private String[] oldNames;
        private String newName;
        private PotentialMapping(String[] oldNames, String newName) {
            this.oldNames = oldNames;
            this.newName = newName;
        }
    }

    private final Logger logger;
    private final RushConfig rushConfig;

    public ReflectionUpgradeManager(Logger logger, RushConfig rushConfig) {
        this.logger = logger;
        this.rushConfig = rushConfig;
    }
    
    @Override
    public void upgrade(List<Class<? extends Rush>> classList, UpgradeCallback callback, Map<Class<? extends Rush>, AnnotationCache> annotationCache) {

        try {

            List<PotentialMapping> potentialJoinMappings = new ArrayList<>();
            List<String> currentTables = currentTables(callback);
            List<TableMapping> tableMappings = new ArrayList<>();

            dropAnyObsoleteTempTables(currentTables, callback);
            
            for(Class clazz : classList) {
                PotentialTableMapping potentialTableMapping = potentialMapping(clazz, potentialJoinMappings, annotationCache);
                String tableName = nameExists(currentTables, potentialTableMapping.name.oldNames);
                if(tableName != null) {
                    currentTables.remove(tableName);
                    TableMapping tableMapping = new TableMapping();
                    tableMapping.name = new Mapping(tableName, potentialTableMapping.name.newName);
                    tableMapping.isJoin = false;
                    List<String> columns = tablesFields(tableName, callback);
                    for(PotentialMapping potentialMapping : potentialTableMapping.fields) {
                        String fieldName = nameExists(columns, potentialMapping.oldNames);
                        if(fieldName != null) {
                            columns.remove(fieldName);
                            Mapping fieldMapping = new Mapping(fieldName, potentialMapping.newName);
                            tableMapping.fields.add(fieldMapping);
                        }
                    }
                    tableMappings.add(tableMapping);
                }
            }

            for(PotentialMapping potentialJoinMapping : potentialJoinMappings) {
                String tableName = nameExists(currentTables, potentialJoinMapping.oldNames);
                if(tableName != null) {
                    currentTables.remove(tableName);
                    TableMapping joinMapping = new TableMapping();
                    joinMapping.name = new Mapping(tableName, potentialJoinMapping.newName);
                    joinMapping.fields.add(new Mapping("parent", "parent"));
                    joinMapping.fields.add(new Mapping("child", "child"));
                    joinMapping.indexes.add(tableName + "_idx");
                    joinMapping.isJoin = true;
                    tableMappings.add(joinMapping);
                }
            }

            for (TableMapping tableMapping : tableMappings) {
                if(tableMapping.name.oldName.equals(tableMapping.name.newName)) {
                    tableMapping.name.oldName = tableMapping.name.oldName + RushSqlUtils.TEMP_PREFIX;
                    renameTable(tableMapping.name.oldName, tableMapping.name.newName, callback);
                }
                if(!rushConfig.usingMySql()) {
                    for (String index : tableMapping.indexes) {
                        callback.runRaw(String.format(RushSqlUtils.DELETE_INDEX, index));
                    }
                }
            }

            callback.createClasses(classList);

            for (TableMapping tableMapping : tableMappings) {
                moveRows(tableMapping, callback);
                currentTables.add(tableMapping.name.oldName);
            }

            for(int i = currentTables.size() - 1; i >= 0; i --) {
                dropTable(currentTables.get(i), callback);
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private String nameExists(List<String> columns, String[] names) {
        for(String name : names){
            for(String column : columns) {
                if (column.equals(name)) {
                    return name;
                }
            }
        }
        return null;
    }

    private void dropAnyObsoleteTempTables(List<String> tables, UpgradeCallback callback) {
        List<String> tablesToRemove = new ArrayList<>();
        for (String table : tables) {
            if(table.endsWith(RushSqlUtils.TEMP_PREFIX)) {
                logger.logError("Dropping tmp table \"" + table + "\" from last upgrade, this implies something when wrong during the last upgrade.");
                callback.runRaw(String.format(RushSqlUtils.DROP, table));
                tablesToRemove.add(table);
            }
        }
        for (String table : tablesToRemove) {
            tables.remove(table);
        }
    }
    
    private List<String> currentTables(UpgradeCallback callback) {
        String sql;
        if(rushConfig.usingMySql()) {
            sql = String.format(RushSqlUtils.TABLE_INFO_MYSQL, rushConfig.dbName());
        }else {
            sql = RushSqlUtils.TABLE_INFO_SQLITE;
        }
        RushStatementRunner.ValuesCallback values = callback.runStatement(sql);
        List<String> tables = new ArrayList<>();
        while(values.hasNext()) {
            String table = values.next().get(0);
            if(table.startsWith(RushSqlUtils.RUSH_TABLE_PREFIX)) {
                tables.add(table);
            }
        }
        values.close();
        return tables;
    }

    private List<String> tablesFields(String table, UpgradeCallback callback) {
        RushStatementRunner.ValuesCallback values = callback.runStatement(String.format(rushConfig.usingMySql() ? RushSqlUtils.COLUMNS_INFO_MYSQL : RushSqlUtils.COLUMNS_INFO_SQLITE, table));
        List<String> columns = new ArrayList<>();
        while(values.hasNext()) {
            List<String> columnsInfo = values.next();
            String column = columnsInfo.get(1);
            if(!column.equals(RushSqlUtils.RUSH_ID)) {
                columns.add(columnsInfo.get(1));
            }
        }
        values.close();
        return columns;
    }

    private PotentialTableMapping potentialMapping(Class<? extends Rush> clazz,  List<PotentialMapping> joinMapping, Map<Class<? extends Rush>, AnnotationCache> annotationCache) throws ClassNotFoundException {
        PotentialTableMapping tableMapping = new PotentialTableMapping();

        List<String> oldClassNames = namesForClass(clazz, annotationCache);
        String[] classNames = oldClassNames.toArray(new String[oldClassNames.size()]);
        tableMapping.name = new PotentialMapping(classNames, ReflectionUtils.tableNameForClass(clazz, annotationCache));

        List<Field> fields = new ArrayList<>();
        ReflectionUtils.getAllFields(fields, clazz);
        for (Field field : fields) {
            field.setAccessible(true);
            if (!field.isAnnotationPresent(RushIgnore.class)) {
                if(Rush.class.isAssignableFrom(field.getType())){
                    addJoinMappingIfRequired(joinMapping, clazz, (Class<? extends Rush>) field.getType(), field, annotationCache);
                }else if(field.isAnnotationPresent(RushList.class)) {
                    RushList rushList = field.getAnnotation(RushList.class);
                    Class listClass = rushList.classType();
                    addJoinMappingIfRequired(joinMapping, clazz, listClass, field, annotationCache);
                }else if(field.isAnnotationPresent(RushRenamed.class)){
                    RushRenamed rushRenamed = field.getAnnotation(RushRenamed.class);
                    tableMapping.fields.add(new PotentialMapping(rushRenamed.names(), field.getName()));
                }else {
                    String[] names = new String[]{field.getName()};
                    tableMapping.fields.add(new PotentialMapping(names, field.getName()));
                }
            }
        }
            
        return tableMapping;
    }

    private void addJoinMappingIfRequired(List<PotentialMapping> potentialMappings, Class<? extends Rush> parent, Class<? extends Rush> child, Field field, Map<Class<? extends Rush>, AnnotationCache> annotationCache) {

        String newName = ReflectionUtils.joinTableNameForClass(parent, child, field, annotationCache);
        List<String> possibleOldNames = new ArrayList<>();
        List<String> parentNames = namesForClass(parent, annotationCache);
        List<String> childNames = namesForClass(child, annotationCache);

        List<String> fieldNames = new ArrayList<>();
        if(field.isAnnotationPresent(RushRenamed.class)){
            RushRenamed rushRenamed = field.getAnnotation(RushRenamed.class);
            for(String name : rushRenamed.names()) {
                fieldNames.add(name);
            }
        }
        fieldNames.add(field.getName());

        for(String fieldName : fieldNames){
            for(String childName : childNames){
                for(String parentName : parentNames){
                    possibleOldNames.add(ReflectionUtils.joinTableNameForClass(parentName, childName, fieldName));
                }
            }
        }
        String[] oldNames = new String[possibleOldNames.size()];
        for (int i = 0; i < possibleOldNames.size(); i ++) {
            oldNames[i] = possibleOldNames.get(i);
        }
        potentialMappings.add(new PotentialMapping(oldNames, newName));
    }

    private List<String> namesForClass(Class<? extends Rush> clazz, Map<Class<? extends Rush>, AnnotationCache> annotationCache) {
        List<String> names = new ArrayList<>();
        names.add(ReflectionUtils.tableNameForClass(clazz, annotationCache));
        if(clazz.isAnnotationPresent(RushRenamed.class)){
            RushRenamed rushRenamed = (RushRenamed) clazz.getAnnotation(RushRenamed.class);
            for(String name : rushRenamed.names()) {
                names.add(ReflectionUtils.tableNameForClass(name));
            }
        }
        return names;
    }

    private void renameTable(String newName, String oldName, UpgradeCallback upgradeCallback) {
        upgradeCallback.runRaw(String.format(RushSqlUtils.RENAME_TABLE, oldName, newName));
    }

    private void dropTable(String name, UpgradeCallback upgradeCallback){
        upgradeCallback.runRaw(String.format(RushSqlUtils.DROP, name));
    }

    private void moveRows(TableMapping tableMapping, UpgradeCallback upgradeCallback) {
        StringBuilder fromRows = new StringBuilder();
        StringBuilder toRows = new StringBuilder();
        for(Mapping mapping : tableMapping.fields) {
            fromRows.append(", ")
                    .append(mapping.oldName);
            toRows.append(", ")
                    .append(mapping.newName);
        }
        String sql = String.format(tableMapping.isJoin ? RushSqlUtils.MOVE_JOIN_ROWS : RushSqlUtils.MOVE_ROWS, tableMapping.name.newName, toRows.toString(), fromRows.toString(), tableMapping.name.oldName);
        upgradeCallback.runRaw(sql);
    }
}
