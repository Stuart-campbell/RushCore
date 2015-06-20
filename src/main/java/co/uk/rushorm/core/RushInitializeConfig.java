package co.uk.rushorm.core;

import java.util.ArrayList;
import java.util.List;

import co.uk.rushorm.core.implementation.Insert.ConflictSaveStatementGenerator;
import co.uk.rushorm.core.implementation.Insert.ReflectionSaveStatementGenerator;
import co.uk.rushorm.core.implementation.Insert.RushSqlInsertGenerator;
import co.uk.rushorm.core.implementation.Insert.SqlBulkInsertGenerator;
import co.uk.rushorm.core.implementation.Insert.SqlSingleInsertGenerator;
import co.uk.rushorm.core.implementation.ReflectionClassLoader;
import co.uk.rushorm.core.implementation.ReflectionDeleteStatementGenerator;
import co.uk.rushorm.core.implementation.ReflectionJoinStatementGenerator;
import co.uk.rushorm.core.implementation.ReflectionTableStatementGenerator;
import co.uk.rushorm.core.implementation.ReflectionUpgradeManager;
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

/**
 * Created by Stuart on 20/06/15.
 */
public abstract class RushInitializeConfig {

    protected RushUpgradeManager rushUpgradeManager;
    protected RushSaveStatementGenerator saveStatementGenerator;
    protected RushConflictSaveStatementGenerator rushConflictSaveStatementGenerator;
    protected RushDeleteStatementGenerator rushDeleteStatementGenerator;
    protected RushJoinStatementGenerator rushJoinStatementGenerator;
    protected RushClassFinder rushClassFinder;
    protected RushStatementRunner rushStatementRunner;
    protected RushQueProvider rushQueProvider;
    protected RushConfig rushConfig;
    protected RushTableStatementGenerator rushTableStatementGenerator;
    protected RushClassLoader rushClassLoader;
    protected Logger rushLogger;
    protected RushStringSanitizer rushStringSanitizer;
    protected RushObjectSerializer rushObjectSerializer;
    protected RushObjectDeserializer rushObjectDeserializer;
    protected RushColumns rushColumns;
    protected InitializeListener initializeListener;
    protected RushSqlInsertGenerator rushSqlInsertGenerator;
    protected List<RushColumn> rushColumnList = new ArrayList<>();

    public RushUpgradeManager getRushUpgradeManager() {
        if(rushUpgradeManager == null) {
            rushUpgradeManager = new ReflectionUpgradeManager(getRushLogger(), getRushConfig());
        }
        return rushUpgradeManager;
    }

    public void setRushUpgradeManager(RushUpgradeManager rushUpgradeManager) {
        this.rushUpgradeManager = rushUpgradeManager;
    }

    public RushSaveStatementGenerator getSaveStatementGenerator() {
        if(saveStatementGenerator == null) {
            saveStatementGenerator = new ReflectionSaveStatementGenerator(getRushSqlInsertGenerator());
        }
        return saveStatementGenerator;
    }

    public void setSaveStatementGenerator(RushSaveStatementGenerator saveStatementGenerator) {
        this.saveStatementGenerator = saveStatementGenerator;
    }

    public RushConflictSaveStatementGenerator getRushConflictSaveStatementGenerator() {
        if(rushConflictSaveStatementGenerator == null){
            rushConflictSaveStatementGenerator = new ConflictSaveStatementGenerator(getRushSqlInsertGenerator());
        }
        return rushConflictSaveStatementGenerator;
    }

    public void setRushConflictSaveStatementGenerator(RushConflictSaveStatementGenerator rushConflictSaveStatementGenerator) {
        this.rushConflictSaveStatementGenerator = rushConflictSaveStatementGenerator;
    }

    public RushDeleteStatementGenerator getRushDeleteStatementGenerator() {
        if(rushDeleteStatementGenerator == null){
            rushDeleteStatementGenerator = new ReflectionDeleteStatementGenerator();
        }
        return rushDeleteStatementGenerator;
    }

    public void setRushDeleteStatementGenerator(RushDeleteStatementGenerator rushDeleteStatementGenerator) {
        this.rushDeleteStatementGenerator = rushDeleteStatementGenerator;
    }

    public RushJoinStatementGenerator getRushJoinStatementGenerator() {
        if(rushJoinStatementGenerator == null){
            rushJoinStatementGenerator = new ReflectionJoinStatementGenerator();
        }
        return rushJoinStatementGenerator;
    }

    public void setRushJoinStatementGenerator(RushJoinStatementGenerator rushJoinStatementGenerator) {
        this.rushJoinStatementGenerator = rushJoinStatementGenerator;
    }

    public abstract RushClassFinder getRushClassFinder();

    public void setRushClassFinder(RushClassFinder rushClassFinder) {
        this.rushClassFinder = rushClassFinder;
    }

    public abstract RushStatementRunner getRushStatementRunner();

    public void setRushStatementRunner(RushStatementRunner rushStatementRunner) {
        this.rushStatementRunner = rushStatementRunner;
    }

    public abstract RushQueProvider getRushQueProvider();

    public void setRushQueProvider(RushQueProvider rushQueProvider) {
        this.rushQueProvider = rushQueProvider;
    }

    public abstract RushConfig getRushConfig();

    public void setRushConfig(RushConfig rushConfig) {
        this.rushConfig = rushConfig;
    }

    public RushTableStatementGenerator getRushTableStatementGenerator() {
        if(rushTableStatementGenerator == null) {
            rushTableStatementGenerator = new ReflectionTableStatementGenerator(getRushConfig());
        }
        return rushTableStatementGenerator;
    }

    public void setRushTableStatementGenerator(RushTableStatementGenerator rushTableStatementGenerator) {
        this.rushTableStatementGenerator = rushTableStatementGenerator;
    }

    public RushClassLoader getRushClassLoader(){
        if(rushClassLoader == null){
            rushClassLoader = new ReflectionClassLoader();
        }
        return rushClassLoader;
    }

    public void setRushClassLoader(RushClassLoader rushClassLoader) {
        this.rushClassLoader = rushClassLoader;
    }

    public abstract Logger getRushLogger();

    public void setRushLogger(Logger rushLogger) {
        this.rushLogger = rushLogger;
    }

    public abstract RushStringSanitizer getRushStringSanitizer();

    public void setRushStringSanitizer(RushStringSanitizer rushStringSanitizer) {
        this.rushStringSanitizer = rushStringSanitizer;
    }

    public abstract RushObjectSerializer getRushObjectSerializer();

    public void setRushObjectSerializer(RushObjectSerializer rushObjectSerializer) {
        this.rushObjectSerializer = rushObjectSerializer;
    }

    public abstract RushObjectDeserializer getRushObjectDeserializer();

    public void setRushObjectDeserializer(RushObjectDeserializer rushObjectDeserializer) {
        this.rushObjectDeserializer = rushObjectDeserializer;
    }

    public RushColumns getRushColumns() {
        if(rushColumns == null) {
            if(getRushConfig().usingMySql()) {
                addRushColumn(new RushColumnBooleanNumerical());
            }else {
                addRushColumn(new RushColumnBoolean());
            }
            addRushColumn(new RushColumnDate());
            addRushColumn(new RushColumnDouble());
            addRushColumn(new RushColumnInt());
            addRushColumn(new RushColumnLong());
            addRushColumn(new RushColumnShort());
            addRushColumn(new RushColumnFloat());
            addRushColumn(new RushColumnString());
            rushColumns = new RushColumnsImplementation(rushColumnList);
        }
        return rushColumns;
    }

    public void addRushColumn(RushColumn rushColumn){
        rushColumnList.add(rushColumn);
    }

    public void setRushColumns(RushColumns rushColumns) {
        this.rushColumns = rushColumns;
    }

    public InitializeListener getInitializeListener() {
        return initializeListener;
    }

    public void setInitializeListener(InitializeListener initializeListener) {
        this.initializeListener = initializeListener;
    }

    public RushSqlInsertGenerator getRushSqlInsertGenerator() {
        if(rushSqlInsertGenerator == null) {
            rushSqlInsertGenerator = getRushConfig().userBulkInsert() ? new SqlBulkInsertGenerator(getRushConfig()) : new SqlSingleInsertGenerator(getRushConfig());
        }
        return rushSqlInsertGenerator;
    }

    public void setRushSqlInsertGenerator(RushSqlInsertGenerator rushSqlInsertGenerator) {
        this.rushSqlInsertGenerator = rushSqlInsertGenerator;
    }

    public void setClasses(final List<Class<? extends Rush>> classes) {
        rushClassFinder = new RushClassFinder() {
            @Override
            public List<Class<? extends Rush>> findClasses(RushConfig rushConfig) {
                return classes;
            }
        };
    }
}
