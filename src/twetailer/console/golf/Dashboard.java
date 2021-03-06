package twetailer.console.golf;

import java.io.IOException;
import java.io.InputStream;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import twetailer.dao.CommonStorage;
import twetailer.dao.DemandStorage;
import twetailer.dao.DemandStorageConverter;
import twetailer.dto.Command;
import twetailer.dto.Demand;
import twetailer.dto.Entity;
import twetailer.http.HttpConnector;
import twetailer.j2ee.OAuthVerifierServlet;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import domderrien.i18n.DateUtils;

/**
 * Controller of the main application panel
 *
 * @author Dom Derrien
 */
public class Dashboard extends ListActivity {

    private OAuthConsumer consumer;
    private OAuthProvider provider;

    private DemandStorage localStorage;
    private Cursor cursor = null;

    public final static int LOAD_DEMANDS_ID = 1;
    public final static int CREATE_DEMAND_ID = 2;
    public final static int UPDATE_DEMAND_ID = 3;
    public final static int DELETE_DEMAND_ID = 4;
    private ProgressDialog progressDialog;

    /**
     * Helper launching a thread that will get the identified demand from the Twetailer REST API
     *
     * @param lastModificationDate Optional date, used to get only the newly updated demands
     *
     * @see Dashboard#messageHandler
     */
    protected void loadDemands(final Long lastModificationDate) {
        progressDialog = ProgressDialog.show(Dashboard.this, "", getString(R.string.dashboard_fetching_data_message));
        new Thread() {
            public void run() {
                Message message = Message.obtain();
                message.what = LOAD_DEMANDS_ID;
                Bundle bundle = new Bundle();
                try {
                    String urlParameters = "related=Location";
                    if (lastModificationDate != null) {
                        urlParameters += "&" + Entity.MODIFICATION_DATE + "=" + DateUtils.millisecondsToISO(lastModificationDate);
                    }
                    String response = HttpConnector.getHelper(
                            Command.DEMAND_ENTITY,
                            null,
                            urlParameters
                    );
                    bundle.putString("response", "" + response);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    bundle.putString(Command.ERROR_MESSAGE, "Error while getting data from back-end: " + ex.getMessage());
                }
                message.setData(bundle);
                messageHandler.sendMessage(message);

            }
        }.start();
    }

    /**
     * Helper launching a thread that will get all demand from the Twetailer REST API
     *
     * @see Dashboard#loadDemands(Long)
     */
    protected void reloadAllDemands() {
        localStorage.clearDemands();
        loadDemands(null);
    }

    /**
     * Handler processing the response generated by the HTTP calls to the Twetailer REST API
     */
    private Handler messageHandler = new Handler() {
        public void handleMessage(Message message) {
            super.handleMessage(message);

            String errorMessage = message.getData().getString(Command.ERROR_MESSAGE);
            if (errorMessage == null) {
                try {
                    String response = message.getData().getString("response");
                    JSONObject data = (JSONObject) new JSONTokener(response).nextValue();
                    if (data.getBoolean("success")) {
                        switch (message.what) {
                        case LOAD_DEMANDS_ID:
                            if (data.opt("related") != null) {
                            	JSONObject related = data.getJSONObject("related");
                            	if (related.opt("Location") != null) {
                            		City.consolidate(related.getJSONArray("relatedResources"));
                            	}
                            }
                            localStorage.refreshDemands(data.getJSONArray("resources"));
                            break;
                        case CREATE_DEMAND_ID:
                            localStorage.createDemand(data.getJSONObject("resource"));
                            break;
                        case UPDATE_DEMAND_ID:
                            localStorage.updateDemand(data.getJSONObject("resource"));
                            break;
                        case DELETE_DEMAND_ID:
                            localStorage.deleteDemand(data.getLong("resourceId"));
                            break;
                        }
                        fillData(Dashboard.this);
                    }
                    else {
                        errorMessage = "Error extracting received data for the ListView -- " + data.getString("reason");
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    errorMessage = "Error extracting received data for the ListView -- " + ex.getMessage();
                }
            }

            if (errorMessage != null) {
                new AlertDialog.Builder(Dashboard.this)
                .setTitle("Alert!")
                .setMessage(errorMessage)
                .show();
            }

            progressDialog.dismiss();
        }
    };

    /**
     * Handler customizing the rows inserted in the ListView embedded in this ListActivity
     */
    public class DemandViewBinder implements SimpleCursorAdapter.ViewBinder {
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            int stateColumn = cursor.getColumnIndex(Entity.STATE);
            int proposalKeysColumn = cursor.getColumnIndex(Demand.PROPOSAL_KEYS);
            int locationKeyColumn = cursor.getColumnIndex(Command.LOCATION_KEY);
            int dueDateColumn = cursor.getColumnIndex(Command.DUE_DATE);

            if (columnIndex == stateColumn) {
                ImageView typeControl = (ImageView)view;
                int type = R.drawable.sport_golf;
                byte[] proposalKeys = cursor.getBlob(proposalKeysColumn);
                String state = cursor.getString(cursor.getColumnIndex(Entity.STATE));
                if (proposalKeys != null && 0 < proposalKeys.length) {
                    type = Entity.STATE_CONFIRMED.equals(state) ? R.drawable.sport_golf_green : R.drawable.sport_golf_blue;
                }
                typeControl.setImageResource(type);
                return true;
            }
            else if (columnIndex == locationKeyColumn) {
                Long locationKey = cursor.getLong(locationKeyColumn);
                City city = new City(locationKey, null, null, null);
                city = City.lookupCity(city); // To try to get the corresponding city from the list of registered ones
                ((TextView) view).setText(city.getName());
                return true;
            }
            else if (columnIndex == dueDateColumn) {
                long dueDate = cursor.getLong(dueDateColumn);
                ((TextView) view).setText(
                        // DateFormat.getMediumDateFormat(Dashboard.this).format(dueDate) +
                        DateFormat.getDateFormat(Dashboard.this).format(dueDate) +
                        " " + DateFormat.getTimeFormat(Dashboard.this).format(dueDate)
                );
                return true;
            }
            return false;
        }
    }

    /**
     * Cursor adapter for the ListView embedded in this ListActivity
     *
     */
    public class DemandAdapter extends SimpleCursorAdapter {
        public DemandAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
            super(context, layout, c, from, to);
            setViewBinder(new DemandViewBinder());
        }
    }

    private static String[] SOURCE_ATTRIBUTE_LIST = new String[] {
        Entity.STATE,
        Command.CRITERIA,
        Command.QUANTITY,
        Command.LOCATION_KEY,
        Command.DUE_DATE
    };
    private static int[] TARGET_COLUMN_ID_LIST = new int[] {
        R.id.stateIcon,
        R.id.criteria,
        R.id.quantity,
        R.id.locationKey,
        R.id.dueDate
    };

    /**
     * Helper querying the database and fetching the ListView embedded in this ListActivity
     *
     * @param context Dashboard context
     */
    private void fillData(Context context) {
        // Get all of the notes from the database and create the item list
        cursor = localStorage.getDemands(DemandStorage.SUMMARY_ATTRIBUT_LIST);
        startManagingCursor(cursor);

        if (cursor != null) {
            // Now create an array adapter and set it to display using our row
            setListAdapter(new DemandAdapter(
                    context,
                    R.layout.dashboard_list_row,
                    cursor,
                    SOURCE_ATTRIBUTE_LIST,
                    TARGET_COLUMN_ID_LIST
            ));

            // No need to close this cursor thanks to the effects of the called startManagingCursor() function
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("onCreate", "application create !");
        onNewIntent(getIntent());

        // Attach the view
        setContentView(R.layout.dashboard);

        // Get the local storage and fill the ListView with the stored data
        localStorage = new DemandStorage(this);
        localStorage.open();
        fillData(this);

        // Register a contextual menu for the ListView
        registerForContextMenu(getListView());

        // Register an event handler for the buttons
        findViewById(R.id.create_demand).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Dashboard.this, DemandEdit.class);
                startActivityForResult(intent, DemandEdit.MODE_CREATE);
                // Note: result of this activity will be passed to onActivityResult()
            }
        });
        findViewById(R.id.dashboard_list).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshActivityList();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("onDestroy", "application destroyed :(");

        localStorage.close();
}

    @Override
    public void onStart() {
        super.onStart();

        // Sources:
        //   http://developer.android.com/resources/samples/ApiDemos/res/xml/preferences.html
        //   http://www.old.kaloer.com/android-preferences/
        Preferences.loadConsumerKey(PreferenceManager.getDefaultSharedPreferences(getBaseContext()));

        City.setDefaultName(getString(R.string.preference_region_list_need_update));

        // Create a new HTTP-RequestQueue.
        // TODO: identify in which version the RequestQueue is available
        // android.net.http.RequestQueue rQueue = new RequestQueue(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);

        // Add an item to the Activity menu to refresh the list
        menu.add(0, Menu.FIRST + 0, Menu.NONE, R.string.create_demand);
        menu.add(0, Menu.FIRST + 1, Menu.NONE, R.string.dashboard_reset_list);
        menu.add(0, Menu.FIRST + 3, Menu.NONE, R.string.dashboard_preferences);
        menu.add(0, Menu.FIRST + 4, Menu.NONE, R.string.dashboard_authorization);

        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle the menu item selection
        Intent intent;
        switch (item.getItemId()) {
        case Menu.FIRST:
            intent = new Intent(this, DemandEdit.class);
            startActivityForResult(intent, DemandEdit.MODE_CREATE);
            // Note: result of this activity will be passed to onActivityResult()
            return true;
        case Menu.FIRST+1:
            reloadAllDemands();
            return true;
        case Menu.FIRST+2:
            refreshActivityList();
            return true;
        case Menu.FIRST+3:
            intent = new Intent(this, Preferences.class);
            startActivity(intent);
            return true;
        case Menu.FIRST+4:
            String appId = "twetailer";
            
            consumer = new CommonsHttpOAuthConsumer(
                    OAuthVerifierServlet.getOAuthKey(appId),
                    OAuthVerifierServlet.getOAuthSecret(appId));

            provider = new CommonsHttpOAuthProvider(
                    OAuthVerifierServlet.getRequestTokenUrl(appId),
                    OAuthVerifierServlet.getAccessTokenUrl(appId),
                    OAuthVerifierServlet.getAuthorizeUrl(appId));

            // 2. Request token & authorization URL build up
            try {
                Log.e("Menu", "Trying to get the Token URL");
                String requestTokenUrl = provider.retrieveRequestToken(consumer, "ase://oauthresponse");
                Log.e("Menu", "Request token URL: " + requestTokenUrl);
                
                // The response is going to be handled by the <intent/> registered for that custom protocol
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(requestTokenUrl));
                // startActivityForResult(intent, Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                Log.e("Menu", "Activity view started");
            }
            catch(OAuthCommunicationException ex) {
                Toast.makeText(Dashboard.this, "Communication exception -- " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
            catch (OAuthMessageSignerException ex) {
                Toast.makeText(Dashboard.this, "Message signer exception -- " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
            catch (OAuthNotAuthorizedException ex) {
                Toast.makeText(Dashboard.this, "Not authorized exception -- " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
            catch (OAuthExpectationFailedException ex) {
                Toast.makeText(Dashboard.this, "Expectation failed exception -- " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    static long key = 111L;

    /**
     * Use the modification date of the most recently update Demand to get the
     * newly updated ones. If no Demand has been fetched yet, a full reload from
     * the Twetailer back-end is ordered.
     */
    private void refreshActivityList() {
        Cursor cursor = localStorage.getLastModifiedDemand(DemandStorage.SHORT_ATTRIBUT_LIST);
        if (cursor == null || cursor.getCount() == 0) {
            reloadAllDemands();
        }
        else {
            loadDemands(cursor.getLong(cursor.getColumnIndex(Entity.MODIFICATION_DATE)));
        }
        cursor.close();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        // Add an item to the ListView contextual menu
        menu.add(0, Menu.FIRST + 0, Menu.NONE, R.string.update_demand);
        menu.add(0, Menu.FIRST + 2, Menu.NONE, R.string.dashboard_view_proposals);
        menu.add(0, Menu.FIRST + 1, Menu.NONE, R.string.cancel_demand);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // Get an handle on the current data
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor cursor = localStorage.getDemandByRowId(info.id, DemandStorage.FULL_ATTRIBUT_LIST);

        try {
            switch(item.getItemId()) {
            case Menu.FIRST:

                // Open the Demand Edition pane
                Intent intent = new Intent(this, DemandEdit.class);
                intent.putExtra(CommonStorage.MANDATORY_LIST_VIEW_COLUMN, info.id);
                intent.putExtras(DemandStorageConverter.prepareDemand(cursor, new Bundle())) ;
                startActivityForResult(intent, DemandEdit.MODE_EDIT);
                // Note: result of this activity will be passed to onActivityResult()

                return true;

            case Menu.FIRST + 1:

                final Long key = cursor.getLong(cursor.getColumnIndex(Entity.KEY));

                progressDialog = ProgressDialog.show(Dashboard.this, "", getString(R.string.dashboard_fetching_data_message));
                new Thread() {
                    public void run() {
                        Message message = Message.obtain();
                        message.what = DELETE_DEMAND_ID;
                        Bundle bundle = new Bundle();
                        try {
                            String response = HttpConnector.deleteHelper(Command.DEMAND_ENTITY, String.valueOf(key));
                            bundle.putString("response", response);
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                            bundle.putString(Command.ERROR_MESSAGE, "Error while getting data from back-end: " + ex.getMessage());
                        }
                        message.setData(bundle);
                        messageHandler.sendMessage(message);

                    }
                }.start();

                return true;

            case Menu.FIRST + 2:

                Toast.makeText(this, "Not yet implemented!", Toast.LENGTH_SHORT).show();

                return true;
            }
        }
        finally {
            cursor.close();
        }

        return super.onContextItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        // Get an handle on the current data
        Cursor cursor = localStorage.getDemandByRowId(id, DemandStorage.SHORT_ATTRIBUT_LIST);

        // Open the Demand Edition pane
        Intent intent = new Intent(this, DemandEdit.class);
        intent.putExtra(CommonStorage.MANDATORY_LIST_VIEW_COLUMN, id);
        intent.putExtras(DemandStorageConverter.prepareDemand(cursor, new Bundle())) ;
        startActivityForResult(intent, DemandEdit.MODE_EDIT);
        // Note: result of this activity will be passed to onActivityResult()

        cursor.close();
    }

    /**
     * Process the data produced by the Demand Edit pane, to create or update a Demand.
     * A thread is created to persist the information through the Twetailer REST API and
     * to save a copy of the just created/updated demand in the local database.
     *
     * @param requestCode Code originally supplied to startActivityForResult(), allowing you to identify who this result came from.
     * @param resultCode Code returned by the child activity through its setResult().
     * @param Intent which can return result data to the caller.
     */
    @Override
    protected void onActivityResult(final int requestCode, int resultCode, final Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (intent != null) {
            Log.e("onActivityResult", "New intent called");
            Uri uri = intent.getData();
            Log.e("onActivityResult", "URI: " + uri);

            final Bundle extras = intent.getExtras();
            if (extras != null) {
                progressDialog = ProgressDialog.show(Dashboard.this, "", getString(R.string.dashboard_fetching_data_message));
                new Thread() {
                    public void run() {
                        Message message = Message.obtain();
                        Bundle bundle = new Bundle();
                        try {
                            JSONObject data = DemandStorageConverter.prepareDemand(extras, new JSONObject());
                            try {
                                JSONArray hashTags = data.optJSONArray(Command.HASH_TAGS);
                                if (hashTags == null || hashTags.length() == 0) {
                                    // TODO: fix this dirty hack ;)
                                    hashTags = new JSONArray();
                                    hashTags.put("golf");
                                    data.put(Command.HASH_TAGS, hashTags);
                                }
                            }
                            catch(JSONException ex) { }
                            switch(requestCode) {
                            case DemandEdit.MODE_CREATE:
                                message.what = CREATE_DEMAND_ID;
                                String postResponse = HttpConnector.postHelper(Command.DEMAND_ENTITY, data.toString());
                                bundle.putString("response", postResponse);
                                break;
                            case DemandEdit.MODE_EDIT:
                                message.what = UPDATE_DEMAND_ID;
                                Long key = intent.getLongExtra(Entity.KEY, 0L);
                                String putResponse = HttpConnector.putHelper(Command.DEMAND_ENTITY, String.valueOf(key), data.toString());
                                bundle.putString("response", putResponse);
                                break;
                            }
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                            bundle.putString(Command.ERROR_MESSAGE, "Error while getting data from back-end: " + ex.getMessage());
                        }
                        message.setData(bundle);
                        messageHandler.sendMessage(message);

                    }
                }.start();
            }
        }
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        Uri uri = intent.getData();

        Log.e("onNewIntent", "URI: " + uri);
        if (uri != null && uri.toString().startsWith("ase://oauthresponse")) {
            String code = uri.getQueryParameter("oauth_verifier");
            Log.e("onNewIntent", "verfier: " + code);
            
            try {
                provider.retrieveAccessToken(consumer, code);
            }
            catch(OAuthCommunicationException ex) {
                Toast.makeText(Dashboard.this, "Communication exception -- " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
            catch (OAuthMessageSignerException ex) {
                Toast.makeText(Dashboard.this, "Message signer exception -- " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
            catch (OAuthNotAuthorizedException ex) {
                Toast.makeText(Dashboard.this, "Not authorized exception -- " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
            catch (OAuthExpectationFailedException ex) {
                Toast.makeText(Dashboard.this, "Expectation failed exception -- " + ex.getMessage(), Toast.LENGTH_LONG).show();
            }
            //new AlertDialog.Builder(Dashboard.this).setTitle("New intent called ;)").setMessage("URI: " + uri.toString() + "\nCode: " + code).show();
            new AlertDialog.Builder(Dashboard.this)
                .setTitle("New intent called ;)")
                .setMessage("Key: " + consumer.getToken() + "\n\nSecret: " + consumer.getTokenSecret())
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            String appId = "twetailer";
                            HttpGet request = new HttpGet("http://" + appId + ".appspot.com/oauth");
                            // consumer.sign(request);
                            HttpClient httpClient = new DefaultHttpClient();
                            HttpResponse response = httpClient.execute(request);
                            String message = "response code: " + response.getStatusLine().getStatusCode();
                            message += "\n" + response.getEntity().getContentType();
                            InputStream in = response.getEntity().getContent();
                            StringBuilder jsonBag = new StringBuilder(2048);
                            try {
                                int byteReadNb;
                                byte[] buffer = new byte[2048];
                                while ((byteReadNb = in.read(buffer)) != -1) {
                                    jsonBag.append(new String(buffer, 0, byteReadNb));
                                }
                            }
                            finally {
                                in.close();
                            }
                            new AlertDialog.Builder(Dashboard.this).setTitle("New intent called ;)").setMessage(message+"\n"+jsonBag).show();

                            /*
                            java.net.URL url = new java.net.URL("http://" + appId + ".appspot.com/oauth");
                            java.net.HttpURLConnection twetailerRequest = (java.net.HttpURLConnection) url.openConnection();
                            consumer.setTokenWithSecret(consumer.getToken(), consumer.getTokenSecret());
                            consumer.sign(twetailerRequest);
                            twetailerRequest.connect();

                            int statusCode = twetailerRequest.getResponseCode();
                            new AlertDialog.Builder(Dashboard.this).setTitle("New intent called ;)").setMessage("Response code: " + statusCode).show();
                            new AlertDialog.Builder(Dashboard.this).setTitle("New intent called ;)").setMessage("Content type: " + twetailerRequest.getContentType()).show();
                            InputStream in = twetailerRequest.getInputStream();
                            StringBuilder jsonBag = new StringBuilder(2048);
                            try {
                                int byteReadNb;
                                byte[] buffer = new byte[2048];
                                while ((byteReadNb = in.read(buffer)) != -1) {
                                    jsonBag.append(new String(buffer, 0, byteReadNb));
                                }
                            }
                            finally {
                                in.close();
                            }
                            */
                            }
                            catch(IOException ex) {
                                Toast.makeText(Dashboard.this, "Conection to Twetailer exception -- " + ex.getMessage(), Toast.LENGTH_LONG).show();
                            }
                            catch(Exception ex) {
                                Log.e("onNewIntent", "exception: " + ex.getClass().getName());
                                Log.e("onNewIntent", "message: " + ex.getMessage());
                                Toast.makeText(Dashboard.this, "Request cannot be signed exception -- " + ex.getMessage(), Toast.LENGTH_LONG).show();
                            }
                    }
                })
                .show();

            // consumer.setTokenWithSecret("1/dX7EdE6S7cNuVBWlIBYsl9C3r080ntR7l5VYrRr6wy0", "M_Bx1sNlG53qhTSD2A_aG23V");

        }
    }
}
