package wseemann.media.romote.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.widget.Toolbar;

import com.jaku.core.JakuRequest;
import com.jaku.core.KeypressKeyValues;
import com.jaku.request.KeypressRequest;

import java.util.List;

import wseemann.media.romote.R;
import wseemann.media.romote.model.Device;
import wseemann.media.romote.tasks.RequestCallback;
import wseemann.media.romote.tasks.RequestTask;
import wseemann.media.romote.utils.CommandHelper;
import wseemann.media.romote.utils.Constants;
import wseemann.media.romote.utils.PreferenceUtils;
import wseemann.media.romote.utils.RokuRequestTypes;

/**
 * Created by wseemann on 2/20/16.
 */
public class SettingsActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        LinearLayout root = (LinearLayout)findViewById(android.R.id.list).getParent().getParent().getParent();
        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
        root.addView(bar, 0); // insert at top
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //getFragmentManager().beginTransaction().replace(android.R.id.content, new Prefs1Fragment()).commit();
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        if (!deviceSupportsFindRemote()) {
            findPreference("find_remote").setEnabled(false);
        }

        findPreference("find_remote").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        performKeypress(KeypressKeyValues.FIND_REMOTE);
                        return true;
                    }
                });

        findPreference("donate").setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(Constants.PAYPAL_DONATION_LINK));
                        startActivity(intent);
                        return true;
                    }
                });
    }

    private void performKeypress(KeypressKeyValues keypressKeyValue) {
        String url = CommandHelper.getDeviceURL(this);

        KeypressRequest keypressRequest = new KeypressRequest(url, keypressKeyValue.getValue());
        JakuRequest request = new JakuRequest(keypressRequest, null);

        new RequestTask(request, new RequestCallback() {
            @Override
            public void requestResult(RokuRequestTypes rokuRequestType, RequestTask.Result result) {

            }

            @Override
            public void onErrorResponse(RequestTask.Result result) {

            }
        }).execute(RokuRequestTypes.keypress);
    }

    private boolean deviceSupportsFindRemote() {
        try {
            Device device = PreferenceUtils.getConnectedDevice(this);

            if (device.getSupportsFindRemote() != null) {
                return Boolean.valueOf(device.getSupportsFindRemote());
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        return false;
    }

    /**
     * Populate the activity with the top-level headers.
     */
    @Override
    public void onBuildHeaders(List<Header> target) {
        //loadHeadersFromResource(R.xml.preference_headers, target);
    }

    @Override
    public boolean isValidFragment(String fragmentName) {
        return true;
    }

    /**
     * This fragment shows the preferences for the first header.
     */
    /**
     * This fragment shows the preferences for the second header.
     */
    public static class Prefs1Fragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Can retrieve arguments from headers XML.
            Log.i("args", "Arguments: " + getArguments());

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }
    }
}