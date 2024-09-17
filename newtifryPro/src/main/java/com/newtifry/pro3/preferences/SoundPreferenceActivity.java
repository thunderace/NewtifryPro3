package com.newtifry.pro3.preferences;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.view.MenuItem;

import com.newtifry.pro3.Preferences;
import com.newtifry.pro3.R;
import com.newtifry.pro3.utils.AppCompatPreferenceActivity;

/**
 * Created by thunder on 19/08/2016.
 */
public class SoundPreferenceActivity extends AppCompatPreferenceActivity {
    private CheckBoxPreference useSoundByPriorityCheckbox;
    //	private CheckBoxPreference speakMessageCheckbox;
    private Preference globalNotification;
    private Preference alertPriorityNotification;
    private Preference warningPriorityNotification;
    private Preference normalPriorityNotification;
    private Preference nonePriorityNotification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        addPreferencesFromResource(R.xml.sound_preferences);

        globalNotification = this.findPreference(Preferences.GLOBAL_RINGTONE);
        globalNotification.setOnPreferenceChangeListener(onNotificationPreferenceChanged);
        alertPriorityNotification = this.findPreference(Preferences.ALERT_PRIORITY_RINGTONE);
        alertPriorityNotification.setOnPreferenceChangeListener(onNotificationPreferenceChanged);
        warningPriorityNotification = this.findPreference(Preferences.WARNING_PRIORITY_RINGTONE);
        warningPriorityNotification.setOnPreferenceChangeListener(onNotificationPreferenceChanged);
        normalPriorityNotification = this.findPreference(Preferences.NORMAL_PRIORITY_RINGTONE);
        normalPriorityNotification.setOnPreferenceChangeListener(onNotificationPreferenceChanged);
        nonePriorityNotification = this.findPreference(Preferences.NONE_PRIORITY_RINGTONE);
        nonePriorityNotification.setOnPreferenceChangeListener(onNotificationPreferenceChanged);
        useSoundByPriorityCheckbox = (CheckBoxPreference)this.findPreference(Preferences.USE_BY_PRIORITY_RINGTONE);
        useSoundByPriorityCheckbox.setOnPreferenceChangeListener(useByPrioritySoundCheckListener);
        updateRingtoneSummary(globalNotification, Preferences.getGlobalRingtone(this));
        updateRingtoneSummary(alertPriorityNotification, Preferences.getAlertRingtone(this));
        updateRingtoneSummary(warningPriorityNotification, Preferences.getWarningRingtone(this));
        updateRingtoneSummary(normalPriorityNotification, Preferences.getNormalRingtone(this));
        updateRingtoneSummary(nonePriorityNotification, Preferences.getNoneRingtone(this));
        toggleUseSoundByPriorityPreferences(Preferences.getUseByPrioritySound(this));
    }

    Preference.OnPreferenceChangeListener useByPrioritySoundCheckListener = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            toggleUseSoundByPriorityPreferences((Boolean)newValue);
            return true;
        }
    };

    public void toggleUseSoundByPriorityPreferences( boolean useByProritySound ) {
        globalNotification.setEnabled(!useByProritySound);
    }

    Preference.OnPreferenceChangeListener onNotificationPreferenceChanged = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            updateRingtoneSummary(preference, (String)newValue);
            return true;
        }
    };

    public void updateRingtoneSummary(Preference preference, String tone) {
        String template = getString(R.string.choose_notification_tone_summary);

        if (tone.equals("")) {
            tone = getString(R.string.silent);
        } else {
            Uri uriTone = Uri.parse(tone);
            Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), uriTone);
            if (ringtone == null) {
                tone = getString(R.string.default_tone);
            } else {
                tone = ringtone.getTitle(getApplicationContext());
            }
        }
        String result = String.format(template, tone);
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
