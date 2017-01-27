package co.uk.rushorm.core;

/**
 * Created by stuartc on 11/12/14.
 */
public interface RushConfig {
    String dbName();
    int dbVersion();
    boolean inDebug();
    boolean log();
    boolean usingMySql();
    boolean userBulkInsert();
    boolean orderColumnsAlphabetically();
}
