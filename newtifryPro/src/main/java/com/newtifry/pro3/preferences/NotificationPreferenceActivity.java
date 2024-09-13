package com.newtifry.pro3.preferences;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.view.MenuItem;

import com.newtifry.pro3.Preferences;
import com.newtifry.pro3.R;
import com.newtifry.pro3.utils.AppCompatPreferenceActivity;

// See PreferenceActivity for warning suppression justification
@SuppressWarnings("deprecation")
public class NotificationPreferenceActivity extends AppCompatPreferenceActivity {

	private Preference useNotifierProPreference;
	private CheckBoxPreference useNotifierProCheckbox;
	private Preference vibrateNotify;
	private Preference ledFlash;
	private Preference speakMessagePreference;
	private Preference quietHoursPreference;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		addPreferencesFromResource(R.xml.notification_preferences);
		vibrateNotify = this.findPreference(Preferences.VIBRATE_NOTIFY);
		ledFlash = this.findPreference(Preferences.LED_FLASH);
		useNotifierProPreference = findPreference(Preferences.USE_NOTIFIER_PRO);
		useNotifierProCheckbox = (CheckBoxPreference)useNotifierProPreference;
		quietHoursPreference = findPreference(Preferences.QUIET_SETTINGS);
		speakMessagePreference = findPreference(Preferences.SPEAK_MESSAGE);
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
