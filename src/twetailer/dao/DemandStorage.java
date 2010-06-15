package twetailer.dao;

import org.json.JSONArray;
import org.json.JSONObject;

import twetailer.dto.Command;
import twetailer.dto.Demand;
import twetailer.dto.Entity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;

/**
 * Class controlling the database accesses for the Demand resource
 *
 * @author Dom Derrien
 */
public class DemandStorage extends CommonStorage {

    private static final String DATABASE_TABLE = "demands";

    private static final String DATABASE_CREATION_STATEMENT =
        "CREATE TABLE " + DATABASE_TABLE + "(" +
                CommonStorage.MANDATORY_LIST_VIEW_COLUMN + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                Entity.KEY + " LONG," +               // Read-only, set by Twetailer back-end
                Entity.STATE + " TEXT," +             // Read-only, set by Twetailer back-end
                Command.CC + " TEXT," +
                Command.CRITERIA + " TEXT," +
                Command.HASH_TAGS + " TEXT," +
                Command.QUANTITY + " LONG," +
                Command.LOCATION_KEY + " LONG," +     // Should match an existing Location, so with a lookup first or a creation
                Demand.RANGE + " DOUBLE," +           // Should stay on Earth!
                Demand.RANGE_UNIT + " TEXT," +        // Value in {KM. MI}
                Entity.CREATION_DATE + " LONG," +     // Read-only, set by Twetailer back-end
                Entity.MODIFICATION_DATE + " LONG," + // Read-only, set by Twetailer back-end
                Command.DUE_DATE + " LONG," +         // Cannot be more than one year in the future
                Demand.EXPIRATION_DATE + " LONG," +   // Cannot exceed DUE_DATE
                Demand.PROPOSAL_KEYS + " BLOB" +      // Read-only, set by Twetailer back-end
        ");";

    static {
        CommonStorage.registerTable(DATABASE_TABLE, DATABASE_CREATION_STATEMENT);
    }

    public static final String[] SHORT_ATTRIBUT_LIST = new String[] {
        CommonStorage.MANDATORY_LIST_VIEW_COLUMN,
        Entity.KEY,
        Entity.MODIFICATION_DATE
    };

    public static final String[] SUMMARY_ATTRIBUT_LIST = new String[] {
        CommonStorage.MANDATORY_LIST_VIEW_COLUMN,
        Entity.KEY,
        Entity.STATE,
        Command.CRITERIA,
        Command.QUANTITY,
        Command.LOCATION_KEY,
        Command.DUE_DATE,
        Demand.PROPOSAL_KEYS
    };

    public static final String[] FULL_ATTRIBUT_LIST = new String[] {
        CommonStorage.MANDATORY_LIST_VIEW_COLUMN,
        Entity.KEY,
        Entity.STATE,
        Command.CC,
        Command.CRITERIA,
        Command.HASH_TAGS,
        Command.QUANTITY,
        Command.LOCATION_KEY,
        Demand.RANGE,
        Demand.RANGE_UNIT,
        Entity.CREATION_DATE,
        Entity.MODIFICATION_DATE,
        Command.DUE_DATE,
        Demand.EXPIRATION_DATE,
        Demand.PROPOSAL_KEYS
    };

    /**
     * Constructor - takes the context to allow the database to be opened/created
     *
     * @param context Context within which to work
     */
    public DemandStorage(Context context) {
        super(context);
    }

    /**
     * Reset the Demand table content
     */
    public void clearDemands() {
        getDatabase().execSQL("DELETE FROM " + DATABASE_TABLE);
    }

    /**
     * Initialise the database with the given data
     */
    public void refreshDemands(JSONArray demands) {
        int demandNb = demands.length();
        for (int idx = 0; idx < demandNb; idx ++) {
            try {
                JSONObject demand = demands.getJSONObject(idx);
                Long key = demand.getLong(Entity.KEY);
                Cursor cursor = fetchDemandByKey(key, SHORT_ATTRIBUT_LIST);
                if (cursor == null || cursor.getCount() == 0) {
                    createDemand(demand);
                }
                else {
                    updateDemand(demand);
                }
                cursor.close();
            }
            catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Return a Cursor over the list of all Demands in the database
     *
     * @param attributList List of columns to load from the database
     * @return Cursor over all Demands
     */
    public Cursor getDemands(String[] attributList) {
        Cursor cursor = getDatabase().query(
                DATABASE_TABLE,
                attributList,
                null,
                null,
                null,
                null,
                Entity.MODIFICATION_DATE + " DESC"
        );
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    /**
     * Return a Cursor over the list of all Demands in the database
     *
     * @param attributList List of columns to load from the database
     * @return Cursor over all Demands
     */
    public Cursor getLastModifiedDemand(String[] attributList) {
        Cursor cursor = getDatabase().query(
                DATABASE_TABLE,
                attributList,
                null,
                null,
                null,
                null,
                Entity.MODIFICATION_DATE + " DESC",
                "1" // Limit
        );
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    /**
     * Return a Cursor positioned at the Demand that matches the given row identifier
     *
     * @param id row identifier of Demand to retrieve
     * @param attributList List of columns to load from the database
     * @return Cursor positioned to matching Demand, if found
     *
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor getDemandByRowId(Long id, String[] attributList) {
        return getDemand(CommonStorage.MANDATORY_LIST_VIEW_COLUMN + "=" + id.toString(), attributList);
    }

    /**
     * Return a Cursor positioned at the Demand that matches the given item key
     *
     * @param key item key of Demand to retrieve
     * @param attributList List of columns to load from the database
     * @return Cursor positioned to matching Demand, if found
     *
     * @throws SQLException if note could not be found/retrieved
     */
    public Cursor fetchDemandByKey(Long key, String[] attributList) {
        return getDemand(Entity.KEY + "=" + key.toString(), attributList);
    }

    /**
     * Helper returning a Cursor positioned at the Demand that matches the given query
     *
     * @param selection Request selector
     * @param attributList List of columns to load from the database
     * @return Cursor positioned to matching Demand, if found
     *
     * @throws SQLException if note could not be found/retrieved
     */
    private Cursor getDemand(String selection, String[] attributList) {
        Cursor cursor = getDatabase().query(
                true,
                DATABASE_TABLE,
                attributList,
                selection,
                null,
                null,
                null,
                Entity.MODIFICATION_DATE + " DESC",
                "1" // Limit
        );
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    /**
     * Create a new Demand using the provided information. If the entity is
     * successfully created return the new rowId, otherwise return
     * a -1 to indicate failure.
     *
     * @param parameters Attributes of the new Demand instance
     * @return rowId or -1 if failed
     */
    public long createDemand(JSONObject parameters) {
        ContentValues values = DemandStorageConverter.prepareDemand(parameters, new ContentValues());
        return getDatabase().insert(DATABASE_TABLE, null, values);
    }

    /**
     * Update the Demand using the details provided. The Demand to be updated is
     * specified using the key, and it is altered to use the given parameters
     *
     * @param parameters Attributes of the updated Demand instance
     * @return true if the note was successfully updated, false otherwise
     */
    public boolean updateDemand(JSONObject parameters) {
        Long key = parameters.optLong(Entity.KEY);

        // TODO: how to verify the target "modificationDate" field is older than the given one?
        // => This verification can avoid a non necessary resource consuming memory write...

        ContentValues values = DemandStorageConverter.prepareDemand(parameters, new ContentValues());
        return getDatabase().update(DATABASE_TABLE, values, Entity.KEY + "=" + key, null) > 0;
    }

    /**
     * Delete the Demand with the given key
     *
     * @param rowId id of Demand to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteDemand(long key) {
        return getDatabase().delete(DATABASE_TABLE, Entity.KEY + "=" + key, null) > 0;
    }
}
