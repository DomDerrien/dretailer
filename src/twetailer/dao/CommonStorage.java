package twetailer.dao;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Base class providing a share-able database controller
 *
 * @author Dom Derrien
 */
public abstract class CommonStorage {

    public static final String MANDATORY_LIST_VIEW_COLUMN = "_id";

    private static final int DATABASE_VERSION = 4;
    private static final String DATABASE_NAME = "twetailerActivities";

    private static List<String> tableNames = new ArrayList<String>();
    private static List<String> createTableStatements = new ArrayList<String>();

    /**
     * Static registration entry point
     *
     * @param tableName Name of the table to consider
     * @param createTableStatement Full SQL statement used to create the table
     */
    public static void registerTable(String tableName, String createTableStatement) {
        tableNames.add(tableName);
        createTableStatements.add(createTableStatement);
    }

    protected Context context;

    /**
     * Constructor - takes the context to allow the database to be opened/created
     *
     * @param context Context within which to work
     */
    public CommonStorage(Context context) {
        this.context = context;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            for (String createTableStatement: createTableStatements) {
                db.execSQL(createTableStatement);
            }

        }

        public void dropTable(SQLiteDatabase db) {
            for (String tableName: tableNames) {
                db.execSQL("DROP TABLE IF EXISTS " + tableName);
            }

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(
                    CommonStorage.class.getName(),
                    "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data"
            );
            dropTable(db);
            onCreate(db);
        }
    }

    private DatabaseHelper databaseHelper;
    private SQLiteDatabase sqliteDatabase;

    /**
     * Singleton returning a database controller
     *
     * @return Controller on the local SQLite database
     */
    public SQLiteDatabase getDatabase() {
        if (sqliteDatabase == null) {
            databaseHelper = new DatabaseHelper(this.context);
            sqliteDatabase = databaseHelper.getWritableDatabase();
        }
        return sqliteDatabase;
    }

    private static int accessNb = 0;

    /**
     * Initializes the database access.
     *
     * @return this (self reference, allowing this to be chained in an initialisation call)
     */
    public CommonStorage open() {
        ++accessNb;
        getDatabase(); // To trigger the possible database setup if it has not been yet done
        return this;
    }

    /**
     * Close the database opened by <code>open()</code>
     *
     * @see CommonStorage#open()
     */
    public void close() {
        --accessNb;
        if (accessNb == 0) {
            databaseHelper.close();
        }
    }
}
