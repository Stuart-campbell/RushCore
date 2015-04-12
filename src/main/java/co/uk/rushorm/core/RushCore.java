package co.uk.rushorm.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import co.uk.rushorm.core.exceptions.RushCoreNotInitializedException;
import co.uk.rushorm.core.exceptions.RushTableMissingEmptyConstructorException;
import co.uk.rushorm.core.implementation.Insert.ConflictSaveStatementGenerator;
import co.uk.rushorm.core.implementation.Insert.RushSqlInsertGenerator;
import co.uk.rushorm.core.implementation.Insert.SqlBulkInsertGenerator;
import co.uk.rushorm.core.implementation.Insert.SqlSingleInsertGenerator;
import co.uk.rushorm.core.implementation.ReflectionClassLoader;
import co.uk.rushorm.core.implementation.ReflectionDeleteStatementGenerator;
import co.uk.rushorm.core.implementation.Insert.ReflectionSaveStatementGenerator;
import co.uk.rushorm.core.implementation.ReflectionJoinStatementGenerator;
import co.uk.rushorm.core.implementation.ReflectionTableStatementGenerator;
import co.uk.rushorm.core.implementation.ReflectionUpgradeManager;
import co.uk.rushorm.core.implementation.ReflectionUtils;
import co.uk.rushorm.core.implementation.RushAnnotationCache;
import co.uk.rushorm.core.implementation.RushColumnBoolean;
import co.uk.rushorm.core.implementation.RushColumnBooleanNumerical;
import co.uk.rushorm.core.implementation.RushColumnDate;
import co.uk.rushorm.core.implementation.RushColumnDouble;
import co.uk.rushorm.core.implementation.RushColumnFloat;
import co.uk.rushorm.core.implementation.RushColumnInt;
import co.uk.rushorm.core.implementation.RushColumnLong;
import co.uk.rushorm.core.implementation.RushColumnShort;
import co.uk.rushorm.core.implementation.RushColumnString;
import co.uk.rushorm.core.implementation.RushColumnsImplementation;
import co.uk.rushorm.core.implementation.RushSqlUtils;

/**
 * Created by Stuart on 10/12/14.
 */
public class RushCore {

    public static void initialize(RushClassFinder rushClassFinder, RushStatementRunner statementRunner, RushQueProvider queProvider, RushConfig rushConfig, RushStringSanitizer rushStringSanitizer, Logger logger, List<RushColumn> columns, RushObjectSerializer rushObjectSerializer, RushObjectDeserializer rushObjectDeserializer) {

        if(rushConfig.usingMySql()) {
            columns.add(new RushColumnBooleanNumerical());
        }else {
            columns.add(new RushColumnBoolean());
        }
        
        columns.add(new RushColumnDate());
        columns.add(new RushColumnDouble());
        columns.add(new RushColumnInt());
        columns.add(new RushColumnLong());
        columns.add(new RushColumnShort());
        columns.add(new RushColumnFloat());
        columns.add(new RushColumnString());

        RushColumns rushColumns = new RushColumnsImplementation(columns);

        RushUpgradeManager rushUpgradeManager = new ReflectionUpgradeManager(logger, rushConfig);
        Map<Class<? extends Rush>, AnnotationCache> annotationCache = new HashMap<>();
        RushSqlInsertGenerator rushSqlInsertGenerator = rushConfig.userBulkInsert() ? new SqlBulkInsertGenerator(rushConfig) : new SqlSingleInsertGenerator(rushConfig);

        RushSaveStatementGenerator saveStatementGenerator = new ReflectionSaveStatementGenerator(rushSqlInsertGenerator);
        RushConflictSaveStatementGenerator conflictSaveStatementGenerator = new ConflictSaveStatementGenerator(rushSqlInsertGenerator);
        RushDeleteStatementGenerator deleteStatementGenerator = new ReflectionDeleteStatementGenerator();
        RushJoinStatementGenerator rushJoinStatementGenerator = new ReflectionJoinStatementGenerator();
        RushTableStatementGenerator rushTableStatementGenerator = new ReflectionTableStatementGenerator(rushConfig);
        RushClassLoader rushClassLoader = new ReflectionClassLoader();

        initialize(rushUpgradeManager, saveStatementGenerator, conflictSaveStatementGenerator, deleteStatementGenerator, rushJoinStatementGenerator, rushClassFinder, rushTableStatementGenerator, statementRunner, queProvider, rushConfig, rushClassLoader, rushStringSanitizer, logger, rushObjectSerializer, rushObjectDeserializer, rushColumns, annotationCache, null);
    }

    public static void initialize(final RushUpgradeManager rushUpgradeManager,
                                  RushSaveStatementGenerator saveStatementGenerator,
                                  RushConflictSaveStatementGenerator rushConflictSaveStatementGenerator,
                                  RushDeleteStatementGenerator deleteStatementGenerator,
                                  RushJoinStatementGenerator rushJoinStatementGenerator,
                                  RushClassFinder rushClassFinder,
                                  RushTableStatementGenerator rushTableStatementGenerator,
                                  final RushStatementRunner statementRunner,
                                  final RushQueProvider queProvider,
                                  final RushConfig rushConfig,
                                  RushClassLoader rushClassLoader,
                                  RushStringSanitizer rushStringSanitizer,
                                  Logger logger,
                                  RushObjectSerializer rushObjectSerializer,
                                  RushObjectDeserializer rushObjectDeserializer,
                                  RushColumns rushColumns,
                                  Map<Class<? extends Rush>, AnnotationCache> annotationCache,
                                  final InitializeListener initializeListener) {

        if(rushCore != null) {
            logger.logError("RushCore has already been initialized, make sure initialize is only called once.");
        }

        rushCore = new RushCore(saveStatementGenerator, rushConflictSaveStatementGenerator, deleteStatementGenerator, rushJoinStatementGenerator, statementRunner, queProvider, rushConfig, rushTableStatementGenerator, rushClassLoader, rushStringSanitizer, logger, rushObjectSerializer, rushObjectDeserializer, rushColumns, annotationCache);
        rushCore.loadAnnotationCache(rushClassFinder);

        final boolean isFirstRun = statementRunner.isFirstRun();
        final RushQue que = queProvider.blockForNextQue();
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (isFirstRun) {
                    rushCore.createTables(new ArrayList<>(rushCore.annotationCache.keySet()), que);
                } else if(rushConfig.inDebug() || statementRunner.requiresUpgrade(rushConfig.dbVersion(), que)){
                    rushCore.upgrade(new ArrayList<>(rushCore.annotationCache.keySet()), rushUpgradeManager, que);
                } else {
                    queProvider.queComplete(que);
                }
                statementRunner.initializeComplete(rushConfig.dbVersion());
                if(initializeListener != null) {
                    initializeListener.initialized(isFirstRun);
                }
            }
        }).start();
    }

    public static RushCore getInstance() {
        if (rushCore == null) {
            throw new RushCoreNotInitializedException();
        }
        return rushCore;
    }

    public RushMetaData getMetaData(Rush rush) {
        return idTable.get(rush);
    }
    
    public String getId(Rush rush) {
        RushMetaData rushMetaData = getMetaData(rush);
        if (rushMetaData == null) {
            return null;
        }
        return rushMetaData.getId();
    }

    public void save(Rush rush) {
        List<Rush> objects = new ArrayList<>();
        objects.add(rush);
        save(objects);
    }

    public void save(List<? extends Rush> objects) {
        RushQue que = queProvider.blockForNextQue();
        save(objects, que);
    }

    public void save(final Rush rush, final RushCallback callback) {
        List<Rush> objects = new ArrayList<>();
        objects.add(rush);
        save(objects, callback);
    }

    public void save(final List<? extends Rush> objects, final RushCallback callback) {
        queProvider.waitForNextQue(new RushQueProvider.RushQueCallback() {
            @Override
            public void callback(RushQue rushQue) {
                save(objects, rushQue);
                if (callback != null) {
                    callback.complete();
                }
            }
        });
    }

    public void join(List<RushJoin> objects) {
        RushQue que = queProvider.blockForNextQue();
        join(objects, que);
    }

    public void join(final List<RushJoin> objects, final RushCallback callback) {
        queProvider.waitForNextQue(new RushQueProvider.RushQueCallback() {
            @Override
            public void callback(RushQue rushQue) {
                join(objects, rushQue);
                if (callback != null) {
                    callback.complete();
                }
            }
        });
    }

    public void deleteJoin(List<RushJoin> objects) {
        RushQue que = queProvider.blockForNextQue();
        deleteJoin(objects, que);
    }

    public void deleteJoin(final List<RushJoin> objects, final RushCallback callback) {
        queProvider.waitForNextQue(new RushQueProvider.RushQueCallback() {
            @Override
            public void callback(RushQue rushQue) {
                deleteJoin(objects, rushQue);
                if (callback != null) {
                    callback.complete();
                }
            }
        });
    }

    public void clearChildren(Class<? extends Rush> parent, String field, Class<? extends Rush> child, String id) {
        final RushQue que = queProvider.blockForNextQue();
        rushJoinStatementGenerator.deleteAll(parent, field, child, id, new RushJoinStatementGenerator.Callback() {
            @Override
            public void runSql(String sql) {
                logger.logSql(sql);
                statementRunner.runRaw(sql, que);
            }
        }, annotationCache);
        queProvider.queComplete(que);
    }

    public long count(String sql) {
        final RushQue que = queProvider.blockForNextQue();
        logger.logSql(sql);
        RushStatementRunner.ValuesCallback valuesCallback = statementRunner.runGet(sql, que);
        List<String> results = valuesCallback.next();
        long count = Long.parseLong(results.get(0));
        queProvider.queComplete(que);
        return count;
    }

    public <T extends Rush> List<T> load(Class<T> clazz, String sql) {
        RushQue que = queProvider.blockForNextQue();
        return load(clazz, sql, que);
    }

    public <T extends Rush> void load(final Class<T> clazz, final String sql, final RushSearchCallback<T> callback) {
        queProvider.waitForNextQue(new RushQueProvider.RushQueCallback() {
            @Override
            public void callback(RushQue rushQue) {
                callback.complete(load(clazz, sql, rushQue));
            }
        });
    }

    public void delete(Rush rush) {
        List<Rush> objects = new ArrayList<>();
        objects.add(rush);
        delete(objects);
    }

    public void delete(List<? extends Rush> objects) {
        RushQue que = queProvider.blockForNextQue();
        delete(objects, que);
    }

    public void delete(final Rush rush, final RushCallback callback) {
        List<Rush> objects = new ArrayList<>();
        objects.add(rush);
        delete(objects, callback);
    }

    public void delete(final List<? extends Rush> objects, final RushCallback callback) {
        queProvider.waitForNextQue(new RushQueProvider.RushQueCallback() {
            @Override
            public void callback(RushQue rushQue) {
                delete(objects, rushQue);
                if (callback != null) {
                    callback.complete();
                }
            }
        });
    }

    public List<RushConflict> saveOnlyWithoutConflict(Rush rush) {
        List<Rush> objects = new ArrayList<>();
        objects.add(rush);
        return saveOnlyWithoutConflict(objects);
    }

    public List<RushConflict> saveOnlyWithoutConflict(List<? extends Rush> objects) {
        RushQue que = queProvider.blockForNextQue();
        return saveOnlyWithoutConflict(objects, que);
    }

    public void saveOnlyWithoutConflict(final Rush rush, final RushConflictCallback callback) {
        List<Rush> objects = new ArrayList<>();
        objects.add(rush);
        saveOnlyWithoutConflict(objects, callback);
    }

    public void saveOnlyWithoutConflict(final List<? extends Rush> objects, final RushConflictCallback callback) {
        queProvider.waitForNextQue(new RushQueProvider.RushQueCallback() {
            @Override
            public void callback(RushQue rushQue) {
                List<RushConflict> conflicts = saveOnlyWithoutConflict(objects, rushQue);
                if (callback != null) {
                    callback.complete(conflicts);
                }
            }
        });
    }

    public String serialize(List<? extends Rush> rush) {
        return serialize(rush, RushSqlUtils.RUSH_ID);
    }

    public String serialize(List<? extends Rush> rush, String idName) {
        return serialize(rush, idName, RushSqlUtils.RUSH_VERSION);
    }

    public String serialize(List<? extends Rush> rush, String idName, String versionName) {
        return rushObjectSerializer.serialize(rush, idName, versionName, rushColumns, annotationCache, new RushObjectSerializer.Callback() {
            @Override
            public RushMetaData getMetaData(Rush rush) {
                return idTable.get(rush);
            }
        });
    }

    public List<Rush> deserialize(String string) {
        return deserialize(string, RushSqlUtils.RUSH_ID);
    }

    public List<Rush> deserialize(String string, String idName) {
        return deserialize(string, idName, RushSqlUtils.RUSH_VERSION);
    }

    public List<Rush> deserialize(String string, String idName, String versionName) {
        return deserialize(string, idName, versionName, Rush.class);
    }

    public <T extends Rush> List<T> deserialize(String string,  Class<T> clazz) {
        return deserialize(string, RushSqlUtils.RUSH_ID, clazz);
    }

    public <T extends Rush> List<T> deserialize(String string, String idName, Class<T> clazz) {
        return deserialize(string, idName, RushSqlUtils.RUSH_VERSION, clazz);
    }

    public <T extends Rush> List<T> deserialize(String string, String idName, String versionName, Class<T> clazz) {
        return rushObjectDeserializer.deserialize(string, idName, versionName, rushColumns, annotationCache, clazz, new RushObjectDeserializer.Callback() {
            @Override
            public void addRush(Rush rush, RushMetaData rushMetaData) {
                idTable.put(rush, rushMetaData);
            }
        });
    }

    public void registerObjectWithId(Rush rush, String id) {
        RushMetaData rushMetaData = new RushMetaData(id, 0);
        registerObjectWithMetaData(rush, rushMetaData);
    }

    public void registerObjectWithMetaData(Rush rush, RushMetaData rushMetaData) {
        idTable.put(rush, rushMetaData);
    }

    public Map<Class<? extends Rush>, AnnotationCache> getAnnotationCache() {
        return annotationCache;
    }

    /* protected */
    protected String sanitize(String string) {
        return rushStringSanitizer.sanitize(string);
    }

    /* private */
    private static RushCore rushCore;
    private final Map<Rush, RushMetaData> idTable = new WeakHashMap<>();

    private final RushSaveStatementGenerator saveStatementGenerator;
    private final RushConflictSaveStatementGenerator rushConflictSaveStatementGenerator;
    private final RushDeleteStatementGenerator deleteStatementGenerator;
    private final RushJoinStatementGenerator rushJoinStatementGenerator;
    private final RushStatementRunner statementRunner;
    private final RushQueProvider queProvider;
    private final RushConfig rushConfig;
    private final RushTableStatementGenerator rushTableStatementGenerator;
    private final RushClassLoader rushClassLoader;
    private final Logger logger;
    private final RushStringSanitizer rushStringSanitizer;
    private final RushObjectSerializer rushObjectSerializer;
    private final RushObjectDeserializer rushObjectDeserializer;
    private final RushColumns rushColumns;
    private final Map<Class<? extends Rush>, AnnotationCache> annotationCache;


    private RushCore(RushSaveStatementGenerator saveStatementGenerator,
                     RushConflictSaveStatementGenerator rushConflictSaveStatementGenerator,
                     RushDeleteStatementGenerator deleteStatementGenerator,
                     RushJoinStatementGenerator rushJoinStatementGenerator, RushStatementRunner statementRunner,
                     RushQueProvider queProvider,
                     RushConfig rushConfig,
                     RushTableStatementGenerator rushTableStatementGenerator,
                     RushClassLoader rushClassLoader,
                     RushStringSanitizer rushStringSanitizer,
                     Logger logger,
                     RushObjectSerializer rushObjectSerializer,
                     RushObjectDeserializer rushObjectDeserializer,
                     RushColumns rushColumns,
                     Map<Class<? extends Rush>, AnnotationCache> annotationCache) {

        this.saveStatementGenerator = saveStatementGenerator;
        this.rushConflictSaveStatementGenerator = rushConflictSaveStatementGenerator;
        this.deleteStatementGenerator = deleteStatementGenerator;
        this.rushJoinStatementGenerator = rushJoinStatementGenerator;
        this.statementRunner = statementRunner;
        this.queProvider = queProvider;
        this.rushConfig = rushConfig;
        this.rushTableStatementGenerator = rushTableStatementGenerator;
        this.rushClassLoader = rushClassLoader;
        this.rushStringSanitizer = rushStringSanitizer;
        this.logger = logger;
        this.rushObjectSerializer = rushObjectSerializer;
        this.rushObjectDeserializer = rushObjectDeserializer;
        this.rushColumns = rushColumns;
        this.annotationCache = annotationCache;
    }
    
    private void loadAnnotationCache(RushClassFinder rushClassFinder) {
        for(Class<? extends Rush> clazz : rushClassFinder.findClasses(rushConfig)) {
            List<Field> fields = new ArrayList<>();
            ReflectionUtils.getAllFields(fields, clazz);
            annotationCache.put(clazz, new RushAnnotationCache(clazz, fields, rushConfig));
        }       
    }

    private void createTables(List<Class<? extends Rush>> classes, final RushQue que) {
        rushTableStatementGenerator.generateStatements(classes, rushColumns, new RushTableStatementGenerator.StatementCallback() {
            @Override
            public void statementCreated(String statement) {
                logger.logSql(statement);
                statementRunner.runRaw(statement, que);
            }
        }, annotationCache);
        queProvider.queComplete(que);
    }

    private void upgrade(List<Class<? extends Rush>> classes, RushUpgradeManager rushUpgradeManager, final RushQue que) {
        rushUpgradeManager.upgrade(classes, new RushUpgradeManager.UpgradeCallback() {
            @Override
            public RushStatementRunner.ValuesCallback runStatement(String sql) {
                logger.logSql(sql);
                return statementRunner.runGet(sql, que);
            }

            @Override
            public void runRaw(String sql) {
                logger.logSql(sql);
                statementRunner.runRaw(sql, que);
            }

            @Override
            public void createClasses(List<Class<? extends Rush>> missingClasses) {
                createTables(missingClasses, que);
            }
        }, annotationCache);
        queProvider.queComplete(que);
    }

    private void save(List<? extends Rush> objects, final RushQue que) {
        statementRunner.startTransition(que);
        saveStatementGenerator.generateSaveOrUpdate(objects, annotationCache, rushStringSanitizer, rushColumns, new RushSaveStatementGeneratorCallback() {
            @Override
            public void addRush(Rush rush, RushMetaData rushMetaData) {
                registerObjectWithMetaData(rush, rushMetaData);
            }

            @Override
            public void createdOrUpdateStatement(String sql) {
                logger.logSql(sql);
                statementRunner.runRaw(sql, que);
            }

            @Override
            public void deleteStatement(String sql) {
                logger.logSql(sql);
                statementRunner.runRaw(sql, que);
            }

            @Override
            public RushMetaData getMetaData(Rush rush) {
                return idTable.get(rush);
            }
        });
        statementRunner.endTransition(que);
        queProvider.queComplete(que);
    }

    private List<RushConflict> saveOnlyWithoutConflict(List<? extends Rush> objects, final RushQue que) {
        final List<RushConflict> conflicts = new ArrayList<>();
        statementRunner.startTransition(que);
        rushConflictSaveStatementGenerator.conflictsFromGenerateSaveOrUpdate(objects, annotationCache, rushStringSanitizer, rushColumns, new RushConflictSaveStatementGenerator.Callback() {
            @Override
            public void conflictFound(RushConflict conflict) {
                conflicts.add(conflict);
            }

            @Override
            public <T extends Rush> T load(Class T, String sql) {
                List<T> objects = RushCore.this.load(T, sql, que);
                return objects.size() > 0 ? objects.get(0) : null;
            }

            @Override
            public void addRush(Rush rush, RushMetaData rushMetaData) {
                registerObjectWithMetaData(rush, rushMetaData);
            }

            @Override
            public void createdOrUpdateStatement(String sql) {
                logger.logSql(sql);
                statementRunner.runRaw(sql, que);
            }

            @Override
            public void deleteStatement(String sql) {
                logger.logSql(sql);
                statementRunner.runRaw(sql, que);
            }

            @Override
            public RushMetaData getMetaData(Rush rush) {
                return idTable.get(rush);
            }
        });
        statementRunner.endTransition(que);
        queProvider.queComplete(que);
        return conflicts;
    }

    private void delete(List<? extends Rush> objects, final RushQue que) {
        statementRunner.startTransition(que);
        deleteStatementGenerator.generateDelete(objects, annotationCache, new RushDeleteStatementGenerator.Callback() {

            @Override
            public void removeRush(Rush rush) {
                RushCore.this.removeRush(rush);
            }

            @Override
            public void deleteStatement(String sql) {
                logger.logSql(sql);
                statementRunner.runRaw(sql, que);
            }

            @Override
            public RushMetaData getMetaData(Rush rush) {
                return idTable.get(rush);
            }
        });
        statementRunner.endTransition(que);
        queProvider.queComplete(que);
    }

    private <T extends Rush> List<T> load(Class<T> clazz, String sql, final RushQue que) {
        logger.logSql(sql);
        RushStatementRunner.ValuesCallback values = statementRunner.runGet(sql, que);
        List<T> objects = rushClassLoader.loadClasses(clazz, rushColumns, annotationCache, values, new RushClassLoader.LoadCallback() {
            @Override
            public RushStatementRunner.ValuesCallback runStatement(String string) {
                logger.logSql(string);
                return statementRunner.runGet(string, que);
            }

            @Override
            public void didLoadObject(Rush rush, RushMetaData rushMetaData) {
                registerObjectWithMetaData(rush, rushMetaData);
            }
        });
        values.close();
        queProvider.queComplete(que);
        if(objects == null) {
            throw new RushTableMissingEmptyConstructorException(clazz);
        }
        return objects;
    }

    private void removeRush(Rush rush) {
        idTable.remove(rush);
    }

    private void join(List<RushJoin> objects, final RushQue que) {
        statementRunner.startTransition(que);
        rushJoinStatementGenerator.createJoins(objects, new RushJoinStatementGenerator.Callback() {
            @Override
            public void runSql(String sql) {
                logger.logSql(sql);
                statementRunner.runRaw(sql, que);
            }
        }, annotationCache);
        queProvider.queComplete(que);
    }

    private void deleteJoin(List<RushJoin> objects, final RushQue que) {
        statementRunner.startTransition(que);
        rushJoinStatementGenerator.deleteJoins(objects, new RushJoinStatementGenerator.Callback() {
            @Override
            public void runSql(String sql) {
                logger.logSql(sql);
                statementRunner.runRaw(sql, que);
            }
        }, annotationCache);
        queProvider.queComplete(que);
    }
}
