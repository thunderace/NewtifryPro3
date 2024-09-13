package com.newtifry.pro3.preferences;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import androidx.core.app.NotificationCompat;
import android.view.MenuItem;

import com.newtifry.pro3.Preferences;
import com.newtifry.pro3.R;
import com.newtifry.pro3.utils.AppCompatPreferenceActivity;

public class NotificationVisibilityPreferenceActivity extends AppCompatPreferenceActivity {
    private CheckBoxPreference useVisibilityByPriorityCheckbox;
    //	private CheckBoxPreference speakMessageCheckbox;
    private Preference globalNotificationVisibility;
    private Preference alertNotificationVisibility;
    private Preference warningNotificationVisibility;
    private Preference normalNotificationVisibility;
    private Preference noneNotificationVisibility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        addPreferencesFromResource(R.xml.notification_visibility_preferences);

        globalNotificationVisibility = this.findPreference(Preferences.GLOBAL_NOTIFICATION_VISIBILITY);
        globalNotificationVisibility.setOnPreferenceChangeListener(onNotificationPreferenceChanged);
        alertNotificationVisibility = this.findPreference(Preferences.ALERT_NOTIFICATION_VISIBILITY);
        alertNotificationVisibility.setOnPreferenceChangeListener(onNotificationPreferenceChanged);
        warningNotificationVisibility = this.findPreference(Preferences.WARNING_NOTIFICATION_VISIBILITY);
        warningNotificationVisibility.setOnPreferenceChangeListener(onNotificationPreferenceChanged);
        normalNotificationVisibility = this.findPreference(Preferences.NORMAL_NOTIFICATION_VISIBILITY);
        normalNotificationVisibility.setOnPreferenceChangeListener(onNotificationPreferenceChanged);
        noneNotificationVisibility = this.findPreference(Preferences.NONE_NOTIFICATION_VISIBILITY);
        noneNotificationVisibility.setOnPreferenceChangeListener(onNotificationPreferenceChanged);
        useVisibilityByPriorityCheckbox = (CheckBoxPreference)this.findPreference(Preferences.USE_BY_PRIORITY_NOTIFICATION_VISIBILITY);
        useVisibilityByPriorityCheckbox.setOnPreferenceChangeListener(useByPriorityVisibilityCheckListener);
        updateVisibilitySummary(globalNotificationVisibility, Preferences.getGlobalVisibility(this));
        updateVisibilitySummary(alertNotificationVisibility, Preferences.getAlertVisibility(this));
        updateVisibilitySummary(warningNotificationVisibility, Preferences.getWarningVisibility(this));
        updateVisibilitySummary(normalNotificationVisibility, Preferences.getNormalVisibility(this));
        updateVisibilitySummary(noneNotificationVisibility, Preferences.getNoneVisibility(this));
        toggleUseVisibilityByPriorityPreferences(Preferences.getUseByPriorityVisibility(this));
    }

    Preference.OnPreferenceChangeListener useByPriorityVisibilityCheckListener = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            toggleUseVisibilityByPriorityPreferences((Boolean)newValue);
            return true;
        }
    };

    public void toggleUseVisibilityByPriorityPreferences( boolean useByProritySound ) {
        if (useByProritySound) {
            globalNotificationVisibility.setEnabled(false);
        } else {
            globalNotificationVisibility.setEnabled(true);
        }
    }

    Preference.OnPreferenceChangeListener onNotificationPreferenceChanged = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            try {
                int value = Integer.parseInt((String)newValue);
                updateVisibilitySummary(preference, value);
            } catch( NumberFormatException ex ) {
                // Not a valid number... ignore.
            }
            return true;
        }
    };

    public void updateVisibilitySummary(Preference preference, int visibility) {
        String template = getString(R.string.choose_notification_visibility_summary);
        String visibilityString;
        switch (visibility) {
            case NotificationCompat.VISIBILITY_PUBLIC:
                visibilityString = getString(R.string.notification_visibility_public);
                break;
            default:
            case NotificationCompat.VISIBILITY_PRIVATE:
                visibilityString = getString(R.string.notification_visibility_private);
                break;
            case NotificationCompat.VISIBILITY_SECRET:
                visibilityString = getString(R.string.notification_visibility_secret);
                break;
        }
        String result = String.format(template, visibilityString);
        preference.setSummary(result);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
