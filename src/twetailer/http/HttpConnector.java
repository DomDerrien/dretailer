package twetailer.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import twetailer.console.golf.Preferences;
import twetailer.dao.StorageConverter;

/**
 * Utility class wrapping the
 *
 * @author Dom Derrien
 */
public class HttpConnector {

    /* Temporary hack 1 -- start
     * ========================= */
    // private final static String TWETAILER_URL = "http://twetailer.appspot.com/";
    private final static String TWETAILER_URL = "http://10.0.2.2:9999/"; // to be able to lookup from the Android emulator on the hosting machine
    // private final static String TWETAILER_API_PATH = "API/";
    private final static String TWETAILER_API_PATH = "shortcut/";
    /* =======================
     * Temporary hack 1 -- end */

    private final static String METHOD_GET = "GET";
    private final static String METHOD_POST = "POST";
    private final static String METHOD_PUT = "PUT";
    private final static String METHOD_DELETE = "DELETE";

    /**
     * Wrapper over the HttpUrlConnection class to exchange data with the Twetailer REST API
     *
     * @param resource Name of the resource to manipulate
     * @param resourceKey Identifier of the resource to manipulate (optional)
     * @param parameters Parameters of the request
     * @param method HTTP method request name
     * @return Textual response produced by the Twetailer REST API
     *
     * @throws RestException If the connection cannot be done or if the Twetailer REST API returns an error code
     */
    private static String processRequest(String resource, String resourceKey, String parameters, String method) throws RestException {

        String url = TWETAILER_URL + TWETAILER_API_PATH + resource;

        try {
            // 1. Prepare the HTTP connection
            if (resourceKey != null) {
                url += "/" + resourceKey;
            }
            if (parameters != null && METHOD_GET.equals(method)) {
                url += "?" + parameters;
                /* Temporary hack 2 -- start
                 * ========================= */
                Long consumerKey = Preferences.consumerKey;
                url += "&maximumResults=10&ownerKey=" + consumerKey;
            }
            else {
                Long consumerKey = Preferences.consumerKey;
                url += "?maximumResults=10&ownerKey=" + consumerKey;
                /* =======================
                 * Temporary hack 2 -- end */
            }
            URLConnection urlConn = new URL(url).openConnection();
            if (!(urlConn instanceof HttpURLConnection)) {
                throw new RestException ("URL is not an Http URL: " + url.toString(), RestException.NOT_HTTP_URL);
            }

            // 2. Update the HTTP connection attributes
            HttpURLConnection httpConn = (HttpURLConnection) urlConn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod(method);
            httpConn.setDoInput(true);
            httpConn.setRequestProperty("Accept", "application/json");
            if (METHOD_GET.equals(method)) {
                httpConn.setDoOutput(false);
                httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            }
            else if (METHOD_POST.equals(method) || METHOD_PUT.equals(method)) {
                httpConn.setDoOutput(true);
                httpConn.setRequestProperty("Content-Type", "application/json");
                httpConn.getOutputStream().write(parameters.getBytes("UTF-8"));
            }
            else { // if (METHOD_DELETE.equals(method)) {
                httpConn.setDoOutput(false);
                httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            }
            // httpConn.setIfModifiedSince(...); // TODO: pass the lMD as a parameter for this field!

            // 3. Submit the HTTP request
            httpConn.connect();

            // 4. Process the HTTP response
            int resCode = httpConn.getResponseCode();
            if (resCode == HttpURLConnection.HTTP_OK) {
                InputStream in = httpConn.getInputStream();
                try {
                    byte[] buffer = new byte[2048];
                    int byteReadNb;
                    String jsonBag = "";
                    while ((byteReadNb = in.read(buffer)) != -1) {
                        jsonBag += new String(buffer, 0, byteReadNb);
                    }
                    return jsonBag;
                }
                finally {
                    in.close();
                }
            }
            throw new RestException("Bad result code: " +resCode + " for: " + httpConn.getURL().toString());
        }
        catch(MalformedURLException ex) {
            throw new RestException("Mal formed URL : " + url, RestException.MALFORMED_URL, ex);
        }
        catch(ProtocolException ex) {
            throw new RestException("Unrecognized protocol : " + method, RestException.BAD_PROTOCOL, ex);
        }
        catch(IOException ex) {
            throw new RestException("Error while getting bytes for the data to send, during the connection, while reading the received bytes", RestException.IO_EXCEPTION, ex);
        }
    }

    /**
     * Query the Twetailer back-end for get the identified resource. If the <code>resourceKey</code>
     * value is <code>null</code>, all resources matching the given parameters should be expected.
     *
     * Note that the back-end may apply some limit in terms of returned resources. This limitation
     * can be worked around by query data per page, and by manually loading each pages.
     *
     * @param resource Name of the resource to retrieve
     * @param resourceKey Identifier of the resource to retrieve
     * @param parameters Parameters of the request (see the Twetailer API documentation)
     * @return JSON bag with the operation status and the requested resource(s)
     *
     * @throws RestException If the retrieval failed
     */
    public static JSONObject get(String resource, String resourceKey, JSONObject parameters) throws RestException {
        // Get
        try {
            String jsonBag = getHelper(resource, resourceKey, StorageConverter.prepareDataToURL(parameters));
            return (JSONObject) new JSONTokener(jsonBag).nextValue();
        }
        catch(JSONException ex) {
            throw new RestException("Error while serialising the given data or while un-serialising the received ones", RestException.JSON_PARSING, ex);
        }
    }

    /**
     * Query the Twetailer back-end for get the identified resource. If the <code>resourceKey</code>
     * value is <code>null</code>, all resources matching the given parameters should be expected.
     *
     * Note that the back-end may apply some limit in terms of returned resources. This limitation
     * can be worked around by query data per page, and by manually loading each pages.
     *
     * @param resource Name of the resource to retrieve
     * @param resourceKey Identifier of the resource to retrieve
     * @param parameters Serialised JSON bag with the arameters of the request (see the Twetailer API documentation)
     * @return Serialised JSON bag with the operation status and the requested resource(s)
     *
     * @throws Exception If the retrieval failed
     */
    public static String getHelper(String resource, String resourceKey, String parameters) throws RestException {
        // Get
        return processRequest(resource, resourceKey, parameters, METHOD_GET);
    }

    /**
     * Propose a new resource to the Twetailer back-end
     *
     * @param resource Name of the resource to create
     * @param parameters Resource attributes (see the Twetailer API documentation)
     * @return JSON bag with the operation status and the just created resource
     *
     * @throws Exception If the retrieval failed
     */
    public static JSONObject post(String resource, JSONObject parameters) throws RestException {
        // Create
        try {
            String jsonBag = postHelper(resource, parameters == null ? null : parameters.toString());
            return (JSONObject) new JSONTokener(jsonBag).nextValue();
        }
        catch(JSONException ex) {
            throw new RestException("Error while serialising the given data or while un-serialising the received ones", RestException.JSON_PARSING, ex);
        }
    }

    /**
     * Propose a new resource to the Twetailer back-end
     *
     * @param resource Name of the resource to create
     * @param parameters Serialised resource attributes (see the Twetailer API documentation)
     * @return Serialised JSON bag with the operation status and the just created resource
     *
     * @throws Exception If the retrieval failed
     */
    public static String postHelper(String resource, String parameters) throws RestException {
        // Create
        return processRequest(resource, null, parameters, METHOD_POST);
    }

    /**
     * Propose updates of an existing resource to the Twetailer back-end
     *
     * @param resource Name of the resource to update
     * @param resourceKey Identifier of the resource to retrieve
     * @param parameters Resource attributes (see the Twetailer API documentation)
     * @return JSON bag with the operation status and the just updated resource
     *
     * @throws Exception If the retrieval failed
     */
    public static JSONObject put(String resource, String resourceKey, JSONObject parameters) throws RestException {
        // Update
        try {
            String jsonBag = putHelper(resource, resourceKey, parameters == null ? null : parameters.toString());
            return (JSONObject) new JSONTokener(jsonBag).nextValue();
        }
        catch(JSONException ex) {
            throw new RestException("Error while serialising the given data or while un-serialising the received ones", RestException.JSON_PARSING, ex);
        }
    }

    /**
     * Propose updates of an existing resource to the Twetailer back-end
     *
     * @param resource Name of the resource to update
     * @param resourceKey Identifier of the resource to retrieve
     * @param parameters Serialised resource attributes (see the Twetailer API documentation)
     * @return Serialised JSON bag with the operation status and the just updated resource
     *
     * @throws Exception If the retrieval failed
     */
    public static String putHelper(String resource, String resourceKey, String parameters) throws RestException {
        // Update
        return processRequest(resource, resourceKey, parameters, METHOD_PUT);
    }

    /**
     * Request a resource deletion to the Twetailer back-end
     *
     * @param resource Name of the resource to delete
     * @param resourceKey Identifier of the resource to delete
     * @return JSON bag with the operation status and the identifier of the resource just deleted
     *
     * @throws Exception If the retrieval failed
     */
    public static JSONObject delete(String resource, String resourceKey) throws RestException {
        // Delete
        try {
            String jsonBag = deleteHelper(resource, resourceKey);
            return (JSONObject) new JSONTokener(jsonBag).nextValue();
        }
        catch(JSONException ex) {
            throw new RestException("Error while serialising the given data or while un-serialising the received ones", RestException.JSON_PARSING, ex);
        }
    }

    /**
     * Request a resource deletion to the Twetailer back-end
     *
     * @param resource Name of the resource to delete
     * @param resourceKey Identifier of the resource to delete
     * @return Serialised JSON bag with the operation status and the identifier of the resource just deleted
     *
     * @throws Exception If the retrieval failed
     */
    public static String deleteHelper(String resource, String resourceKey) throws RestException {
        // Delete
        return processRequest(resource, resourceKey, null, METHOD_DELETE);
    }
}
