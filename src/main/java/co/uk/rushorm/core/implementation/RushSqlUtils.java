package co.uk.rushorm.core.implementation;

/**
 * Created by Stuart on 08/03/15.
 */
public class RushSqlUtils {

    public static final String RUSH_TABLE_PREFIX = "rush_";
    public static final String RUSH_ID = "rush_id";
    public static final String RUSH_CREATED = "rush_created";
    public static final String RUSH_UPDATED = "rush_updated";
    public static final String RUSH_VERSION = "rush_version";
    public static final String TEMP_PREFIX = "_temp";

    // Upgrade
    public static final String MOVE_JOIN_ROWS = "INSERT INTO %s(" + RUSH_ID + "%s)\n" +
            "SELECT " + RUSH_ID + "%s\n" +
            "FROM %s;";

    public static final String MOVE_ROWS = "INSERT INTO %s(" + RUSH_ID + "," + RUSH_CREATED + "," + RUSH_UPDATED + "," + RUSH_VERSION + "%s)\n" +
            "SELECT " + RUSH_ID + "," + RUSH_CREATED + "," + RUSH_UPDATED + "," + RUSH_VERSION + "%s\n" +
            "FROM %s;";

    public static final String SELECT_TEMPLATE = "SELECT * from %s" +
            "\nWHERE " + RUSH_ID + "='%s';";

    // Create
    public static final String JOIN_TEMPLATE_SQLITE = "CREATE TABLE %s (" +
            "\n" + RUSH_ID + " integer primary key autoincrement" +
            ",\nparent varchar(255) NOT NULL" +
            ",\nchild varchar(255) NOT NULL" +
            ",\nFOREIGN KEY (parent) REFERENCES %s(" + RUSH_ID +")" +
            ",\nFOREIGN KEY (child) REFERENCES %s(" + RUSH_ID + ")" +
            "\n);";

    public static final String JOIN_TEMPLATE_MYSQL = "CREATE TABLE %s (" +
                    "\n" + RUSH_ID + " int primary key auto_increment" +
                    ",\nparent varchar(255) NOT NULL" +
                    ",\nchild varchar(255) NOT NULL" +
                    "\n);";

    public static final String TABLE_TEMPLATE = "CREATE TABLE %s (" +
            "\n" + RUSH_ID + " varchar(255) primary key," +
            "\n" + RUSH_CREATED + " long," +
            "\n" + RUSH_UPDATED + " long," +
            "\n" + RUSH_VERSION + " long" +
            "%s" +
            "\n);";

    public static final String CREATE_INDEX = "CREATE INDEX %s_idx ON %s(child);";

    // Delete
    public static final String MULTIPLE_DELETE_TEMPLATE = "DELETE FROM %s \n" +
            "WHERE %s;";

    // Insert
    public static final String MULTIPLE_INSERT_UPDATE_TEMPLATE_MYSQL = "REPLACE INTO %s " +
            "(%s)\n" +
            "VALUES %s;";

    public static final String MULTIPLE_INSERT_UPDATE_TEMPLATE_SQLITE = "INSERT OR REPLACE INTO %s " +
                    "(%s)\n" +
                    "VALUES %s;";

    public static final String MULTIPLE_INSERT_JOIN_TEMPLATE = "INSERT INTO %s " +
                            "(parent, child)\n" +
                            "VALUES %s;";

    // Meta data
    public static final String COLUMNS_INFO_SQLITE = "PRAGMA table_info(%s)";
    public static final String COLUMNS_INFO_MYSQL = "DESCRIBE %s";
    public static final String RENAME_TABLE = "ALTER TABLE %s RENAME TO %s";
    public static final String TABLE_INFO_SQLITE = "SELECT name FROM sqlite_master WHERE type='table';";
    public static final String TABLE_INFO_MYSQL = "select TABLE_NAME from information_schema.tables where TABLE_SCHEMA='%s';";
    public static final String DROP = "DROP TABLE %s";
    public static final String DELETE_INDEX = "DROP INDEX %s;";
}
