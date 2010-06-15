package twetailer.dao;

import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import twetailer.dto.Command;
import twetailer.dto.Demand;
import twetailer.dto.Entity;
import twetailer.dto.Location;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import domderrien.i18n.DateUtils;

/**
 * Set of utility functions moving Demand attributes from one data structure to another one
 *
 * @author Dom Derrien
 */
public class DemandStorageConverter {

    /**
     * Transfer the Demand attributes from a <code>JSONObject</code> instance to a <code>ContentValues</code> one.
     * The returned value is ready to be inserted into a SQLite database.
     *
     * @param in Source of information
     * @param out Target container
     * @return Updated target container
     * @throws JSONException
     */
    public static ContentValues prepareDemand(JSONObject in, ContentValues out) {

        // The putValue() helper takes care of the null values, no specific test here.
        StorageConverter.putValue(in, out, Entity.KEY, Long.class, 0L);
        StorageConverter.putValue(in, out, Entity.STATE, String.class, "open");
        StorageConverter.putArrayOfValuesAsString(in, out, Command.CC, String.class, "", " ");
        StorageConverter.putArrayOfValuesAsString(in, out, Command.CRITERIA, String.class, "", " ");
        StorageConverter.putArrayOfValuesAsString(in, out, Command.HASH_TAGS, String.class, "", " ");
        StorageConverter.putValue(in, out, Command.QUANTITY, Long.class, 1L);
        StorageConverter.putValue(in, out, Demand.RANGE, Double.class, 25.0D);
        StorageConverter.putValue(in, out, Demand.RANGE_UNIT, String.class, "KM");
        StorageConverter.putValue(in, out, Command.LOCATION_KEY, Long.class, 0L);
        StorageConverter.putValue(in, out, Location.POSTAL_CODE, String.class, null);
        StorageConverter.putValue(in, out, Location.COUNTRY_CODE, String.class, null);
        final Long now = Calendar.getInstance().getTimeInMillis();
        StorageConverter.putValue(in, out, Entity.CREATION_DATE, Date.class, now);
        StorageConverter.putValue(in, out, Entity.MODIFICATION_DATE, Date.class, now);
        StorageConverter.putValue(in, out, Command.DUE_DATE, Date.class, now);
        StorageConverter.putValue(in, out, Demand.EXPIRATION_DATE, Date.class, now);

        if (in.opt(Demand.PROPOSAL_KEYS) != null) {
            JSONArray proposalKeys = in.optJSONArray(Demand.PROPOSAL_KEYS);
            out.put(Demand.PROPOSAL_KEYS, StorageConverter.jsonToByte(proposalKeys));
        }

        return out;
    }

    /**
     * Transfer the Demand attributes from a SQLite database <code>Cursor</code> instance to a <code>Bundle</code> one.
     * The returned value is ready to be added to an <code>Intent</code> for a manipulation by another <code>Dialog</code>.
     *
     * @param in Source of information
     * @param out Target container
     * @return Updated target container
     */
    public static Bundle prepareDemand(Cursor in, Bundle out) {

        // The putValue() helper takes care of the null values, no specific test here.
        StorageConverter.putValue(in, out, Entity.KEY, Long.class);
        StorageConverter.putValue(in, out, Entity.STATE, String.class);
        StorageConverter.putValue(in, out, Command.CC, String.class);
        StorageConverter.putValue(in, out, Command.CRITERIA, String.class);
        StorageConverter.putValue(in, out, Command.HASH_TAGS, String.class);
        StorageConverter.putValue(in, out, Command.QUANTITY, Long.class);
        StorageConverter.putValue(in, out, Demand.RANGE, Double.class);
        StorageConverter.putValue(in, out, Demand.RANGE_UNIT, String.class);
        StorageConverter.putValue(in, out, Command.LOCATION_KEY, Long.class);
        StorageConverter.putValue(in, out, Location.POSTAL_CODE, String.class);
        StorageConverter.putValue(in, out, Location.COUNTRY_CODE, String.class);
        StorageConverter.putValue(in, out, Entity.CREATION_DATE, Long.class);
        StorageConverter.putValue(in, out, Entity.MODIFICATION_DATE, Long.class);
        StorageConverter.putValue(in, out, Command.DUE_DATE, Long.class);
        StorageConverter.putValue(in, out, Demand.EXPIRATION_DATE, Long.class);

        byte[] proposalKeys = in.getBlob(in.getColumnIndex(Demand.PROPOSAL_KEYS));
        if (proposalKeys != null && 0 < proposalKeys.length) {
            out.putLongArray(Demand.PROPOSAL_KEYS, StorageConverter.byteToLong(proposalKeys));
        }

        return out;
    }

    /**
     * Transfer the Demand attributes from a <code>Bundle</code> instance to a <code>JSONObject</code> one.
     *
     * @param in Source of information
     * @param out Target container
     * @return Updated target container
     *
     * @throws JSONException If the data insertion in the JSONObject fails
     */
    public static JSONObject prepareDemand(Bundle in, JSONObject out) throws JSONException {

        if (in.containsKey(Entity.KEY)) { out.put(Entity.KEY, in.getLong(Entity.KEY)); }
        if (in.containsKey(Entity.STATE)) { out.put(Entity.STATE, in.getString(Entity.STATE)); }
        if (in.containsKey(Command.CC)) {
            String cc = in.getString(Command.CC);
            JSONArray ccArray = new JSONArray();
            if (cc != null && 0 < cc.length()) {
                String[] ccParts = cc.split(" ");
                for (int i=0; i<ccParts.length; i++) {
                    if (0 < ccParts[i].length()) {
                        ccArray.put(ccParts[i]);
                    }
                }
            }
            out.put(Command.CC, ccArray);
        }
        if (in.containsKey(Command.CRITERIA)) {
            String criteria = in.getString(Command.CRITERIA);
            JSONArray criterionArray = new JSONArray();
            if (criteria != null && 0 < criteria.length()) {
                String[] criteriaParts = criteria.split(" ");
                for (int i=0; i<criteriaParts.length; i++) {
                    if (0 < criteriaParts[i].length()) {
                        criterionArray.put(criteriaParts[i]);
                    }
                }
            }
            out.put(Command.CRITERIA, criterionArray);
        }
        if (in.containsKey(Command.HASH_TAGS)) {
            String criteria = in.getString(Command.HASH_TAGS);
            JSONArray criterionArray = new JSONArray();
            if (criteria != null && 0 < criteria.length()) {
                String[] criteriaParts = criteria.split(" ");
                for (int i=0; i<criteriaParts.length; i++) {
                    if (0 < criteriaParts[i].length()) {
                        criterionArray.put(criteriaParts[i]);
                    }
                }
            }
            out.put(Command.HASH_TAGS, criterionArray);
        }
        if (in.containsKey(Command.QUANTITY)) { out.put(Command.QUANTITY, in.getLong(Command.QUANTITY)); }
        if (in.containsKey(Demand.RANGE)) { out.put(Demand.RANGE, in.getDouble(Demand.RANGE)); }
        if (in.containsKey(Demand.RANGE_UNIT)) { out.put(Demand.RANGE_UNIT, in.getString(Demand.RANGE_UNIT)); }
        if (in.containsKey(Command.LOCATION_KEY)) { out.put(Command.LOCATION_KEY, in.getLong(Command.LOCATION_KEY)); }
        if (in.containsKey(Location.POSTAL_CODE)) { out.put(Location.POSTAL_CODE, in.getString(Location.POSTAL_CODE)); }
        if (in.containsKey(Location.COUNTRY_CODE)) { out.put(Location.COUNTRY_CODE, in.getString(Location.COUNTRY_CODE)); }
        if (in.containsKey(Entity.CREATION_DATE)) { out.put(Entity.CREATION_DATE, DateUtils.millisecondsToISO(in.getLong(Entity.CREATION_DATE))); }
        if (in.containsKey(Entity.MODIFICATION_DATE)) { out.put(Entity.MODIFICATION_DATE, DateUtils.millisecondsToISO(in.getLong(Entity.MODIFICATION_DATE))); }
        if (in.containsKey(Command.DUE_DATE)) { out.put(Command.DUE_DATE, DateUtils.millisecondsToISO(in.getLong(Command.DUE_DATE))); }
        if (in.containsKey(Demand.EXPIRATION_DATE)) { out.put(Demand.EXPIRATION_DATE, DateUtils.millisecondsToISO(in.getLong(Demand.EXPIRATION_DATE))); }

        // No need to transfer the CommandDef.PROPOSAL_KEYS because they cannot be update in the Demand Edit pane

        return out;
    }
}
