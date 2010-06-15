package twetailer.dao;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import domderrien.i18n.DateUtils;

/**
 * Set of utility functions moving data from one data structure to another one
 *
 * @author Dom Derrien
 */
public class StorageConverter {

    /**
     * Helper transferring the identified value from a <code>JSONObject</code> instance to a <code>ContentValues</code> instance
     *
     * @param in Source of information
     * @param out Target container
     * @param key Identifier of the value to transfer
     * @param clazz Class of the value to transfer
     * @param defaultValue Default value to rely on if the value is not found in the Map instance
     */
    public static void putValue(JSONObject in, ContentValues out, String key, Class<?> clazz, Object defaultValue) {
        if (in.isNull(key)) {
            // No transfer of a null value
        }
        else if (clazz == Long.class) {
            out.put(key, in.optLong(key, (Long) defaultValue));
        }
        else if (clazz == Double.class) {
            out.put(key, in.optDouble(key, (Double) defaultValue));
        }
        else if (clazz == String.class) {
            out.put(key, in.optString(key, (String) defaultValue));
        }
        else if (clazz == Date.class) {
            try {
                // Convert the give ISO formatted String
                out.put(key, DateUtils.isoToMilliseconds(in.getString(key)));
            }
            catch (Exception ex) {
                // Or fallback on the given Date in milliseconds
                out.put(key,(Long) defaultValue);
            }
        }
        else {
            throw new IllegalArgumentException("Unexpected Class type '" + clazz.getSimpleName() + "' to be inserted into the database");
        }
    }

    /**
     * Helper serialising the content of an array into a <code>String</code> with each items separated as specified
     *
     * @param in Source of information
     * @param out Target container
     * @param key Identifier of the value to transfer
     * @param clazz Class of the value to transfer
     * @param defaultValue Default value to rely on if the value is not found in the Map instance
     * @param separator Character to insert between each value
     */
    public static void putArrayOfValuesAsString(JSONObject in, ContentValues out, String key, Class<?> clazz, Object defaultValue, String separator) {
        JSONArray arrayOfValues = in.optJSONArray(key);
        if (arrayOfValues == null) {
            return;
        }
        StringBuilder value = new StringBuilder();
        for (int i=0; i<arrayOfValues.length(); i++) {
            if (arrayOfValues.isNull(i)) {
                // No transfer of a null value
            }
            else if (clazz == Long.class) {
                value.append(String.valueOf(arrayOfValues.optLong(i, (Long) defaultValue))).append(separator);
            }
            else if (clazz == Double.class) {
                value.append(String.valueOf(arrayOfValues.optDouble(i, (Double) defaultValue))).append(separator);
            }
            else if (clazz == String.class) {
                value.append(String.valueOf(arrayOfValues.optString(i, (String) defaultValue))).append(separator);
            }
            else if (clazz == Date.class) {
                try {
                    // Convert the give ISO formatted String
                    out.put(key, DateUtils.isoToMilliseconds(in.getString(key)));
                }
                catch (Exception ex) {
                    // Or fallback on the given Date in milliseconds
                    out.put(key,(Long) defaultValue);
                }
            }
            else {
                throw new IllegalArgumentException("Unexpected Class type '" + clazz.getSimpleName() + "' to be inserted into the database");
            }
        }
        if (0 < value.length()) {
            out.put(key, value.substring(0, value.length() - separator.length()));
        }
    }

    /**
     * Helper transforming a JSONArray from an HTTP request or from an Itent into a byte[] for the SQLite database
     *
     * @param data Data set to transform
     * @return Transformed data set
     */
    public static byte[] jsonToByte(JSONArray data) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            for(int idx=0; idx<data.length(); idx++) {
                dos.writeLong(data.getLong(idx));
            }
            dos.flush();
            return bos.toByteArray();
        }
        catch(Exception ex) {
            throw new IllegalArgumentException("Cannot convert JSONArray to byte[]", ex);
        }
    }

    /**
     * Helper transferring the identified value from a SQLite database <code>Cursor</code> instance to <code>Bundle</code> instance
     *
     * @param in Source of information
     * @param out Target container
     * @param key Identifier of the value to transfer
     * @param clazz Class of the value to transfer
     */
    public static void putValue(Cursor in, Bundle out, String key, Class<?> clazz) {
        int columnIdx = in.getColumnIndex(key);
        if (columnIdx == -1 || in.isNull(columnIdx)) {
            // Means: the column is not part of the query filter
            return;
        }
        if (clazz == Long.class) {
            out.putLong(key, in.getLong(columnIdx));
        }
        else if (clazz == Double.class) {
            out.putDouble(key, in.getDouble(columnIdx));
        }
        else if (clazz == String.class) {
            out.putString(key, in.getString(columnIdx));
        }
        else if (clazz == Date.class) {
            // out.putLong(key, in.getLong(columnIdx));
            throw new IllegalArgumentException("Give the time in milliseconds and a Long class name");
        }
        else {
            throw new IllegalArgumentException("Unexpected Class type '" + clazz.getSimpleName() + "' to be extracted from the database");
        }
    }

    /**
     * Helper transforming a byte[] from the SQLite database into a long[] for Bundle send to an Intent
     *
     * @param data Data set to transform
     * @return Transformed data set
     */
    public static long[] byteToLong(byte[] data) {
        try {
            ByteArrayInputStream bos = new ByteArrayInputStream(data);
            DataInputStream dos = new DataInputStream(bos);
            int dataNb = data.length / 8;
            long[] out = new long[dataNb];
            while (0 < dataNb) {
                -- dataNb;
                out[dataNb] = dos.readLong();
            }
            return out;
        }
        catch(Exception ex) {
            throw new IllegalArgumentException("Cannot convert JSONArray to byte[]", ex);
        }
    }

    /**
     * Serialise the content of the <code>Map</code> instance in a <code>JSONObject</code> one.
     * The returned container is ready to be used as a set of URL parameters
     *
     * @param in Source of information
     * @return URL encoded series of parameters
     *
     * @throws JSONException If the data extraction fails...
     */
    @SuppressWarnings("unchecked")
    public static String prepareDataToURL(JSONObject in) throws JSONException {
        if (in == null || in.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        Iterator<String> keyIterator = in.keys();
        while (keyIterator.hasNext()) {
            String key = keyIterator.next();
            Object value = in.get(key);
            try {
                out.append("&").append(URLEncoder.encode(key, "UTF-8")).append("=");
                if (value == null) {
                    // No value to transmit
                }
                else if (value instanceof Long) {
                    out.append(value != null ? Long.toString((Long) value) : "");
                }
                else if (value instanceof Double) {
                    out.append(value != null ? Double.toString((Double) value) : "");
                }
                else if (value instanceof String) {
                    out.append(value != null ? URLEncoder.encode((String) value, "UTF-8") : "");
                }
                else if (value instanceof Date) {
                    // Just copy the Unicode formatted Date
                    out.append(value != null ? URLEncoder.encode((String) value, "UTF-8") : "");

                }
            }
            catch (UnsupportedEncodingException e) {
                throw new RuntimeException("This method requires UTF-8 encoding support", e);
            }
        }
        return out.substring(1).toString();
    }

    /**
     * De-serialise URL encoded parameters into a <code>Map</code> instance
     *
     * @param in Source of information
     * @param out Target container
     * @return Updated target container
     */
    public static Map<String, String> fromURLParameters(String in, Map<String, String> out) {
        String[] nameValuePairs = in.split("&");
        for (String nameValuePair : nameValuePairs) {
            String[] nameValue = nameValuePair.split("=");
            try {
                out.put(
                        URLDecoder.decode(nameValue[0], "UTF-8"),
                        nameValue.length > 1 ? URLDecoder.decode(nameValue[1], "UTF-8") : ""
                );
            }
            catch (UnsupportedEncodingException e) {
                throw new RuntimeException("This method requires UTF-8 encoding support", e);
            }
        }
        return out;
    }
}
