package twetailer.console.golf;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Application preferences controller
 *
 * @author Dom Derrien
 */
public class Preferences extends PreferenceActivity {

    public final static String CONSUMER_KEY = "consumerKey";
    public static Long consumerKey = 0L;

    /**
     * Helper loading the consumer key from the preferences, or initializing for future references
     *
     * @param preferences Bag to use for the lookup
     * @return Value of the consumer key
     */
    protected static Long loadConsumerKey(SharedPreferences preferences) {
        try {
            consumerKey = Long.valueOf(preferences.getString(Preferences.CONSUMER_KEY, "0"));
        }
        catch(Exception ex) {
            consumerKey = 0L;

            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(Preferences.CONSUMER_KEY, String.valueOf(consumerKey));
            editor.commit();
        }
        return consumerKey;
    }

    protected void setConsumerKeyTitle(SharedPreferences preferences) {
        findPreference(CONSUMER_KEY).setTitle(
                getString(R.string.preference_consumer_key_base_name).replaceFirst(
                        "\\{0\\}",
                        String.valueOf(loadConsumerKey(preferences))
                )
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences preferences = getPreferenceScreen().getSharedPreferences();
        preferences.registerOnSharedPreferenceChangeListener(listener);
        if (consumerKey != 0L) {
            setConsumerKeyTitle(preferences);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
    }

    protected OnSharedPreferenceChangeListener listener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (CONSUMER_KEY.equals(key)) {
                setConsumerKeyTitle(sharedPreferences);
            }
        }
    };
}
