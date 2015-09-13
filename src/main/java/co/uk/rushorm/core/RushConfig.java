package co.uk.rushorm.core;

/**
 * Created by stuartc on 11/12/14.
 */
public interface RushConfig {
    public String dbName();
    public int dbVersion();
    public boolean inDebug();
    public boolean log();
    public boolean requireTableAnnotation();
    public boolean usingMySql();
    public boolean userBulkInsert();
    public boolean orderColumnsAlphabetically();
}
