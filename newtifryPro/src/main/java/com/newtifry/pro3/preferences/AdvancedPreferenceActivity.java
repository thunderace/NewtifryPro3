package com.newtifry.pro3.preferences;

import static com.newtifry.pro3.CommonUtilities.LOG_ERROR_LEVEL;

import com.afollestad.materialdialogs.prefs.MaterialEditTextPreference;
import com.afollestad.materialdialogs.prefs.MaterialListPreference;
import com.newtifry.pro3.CommonUtilities;
import com.newtifry.pro3.Preferences;
import com.newtifry.pro3.R;
import com.newtifry.pro3.utils.AppCompatPreferenceActivity;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.view.MenuItem;

//import io.explod.android.sqllog.ui.activity.LogViewerActivity;

@SuppressWarnings("deprecation")
public class AdvancedPreferenceActivity extends AppCompatPreferenceActivity {

	MaterialEditTextPreference autoCleanEditText;
	MaterialEditTextPreference maxMessageEditText;
	private CheckBoxPreference preventCPUSleepCheckbox;
	MaterialListPreference dateTimeformatListPreference;
	private CheckBoxPreference debugMode;
	private CheckBoxPreference verboseDebugMode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		addPreferencesFromResource(R.xml.advanced_preferences);
		autoCleanEditText = (MaterialEditTextPreference)findPreference(Preferences.AUTO_CLEAN_MESSAGE_DAYS);
		setAutoCleanSummary(Preferences.getAutoCleanMessagesDays(this));
		autoCleanEditText.setOnPreferenceChangeListener(autoCleanChangeHandler);
		maxMessageEditText = (MaterialEditTextPreference)findPreference(Preferences.MAX_MESSAGE_COUNT);
		setMaxMessageSummary(Preferences.getMaxMessageCount(this));
		maxMessageEditText.setOnPreferenceChangeListener(maxMessageChangeHandler);
		preventCPUSleepCheckbox = (CheckBoxPreference)this.findPreference(Preferences.PREVENT_CPU_SLEEP);
		preventCPUSleepCheckbox.setOnPreferenceChangeListener(onPreventCPUSleepPreferenceChanged);
		dateTimeformatListPreference = (MaterialListPreference)findPreference(Preferences.DATE_TIME_FORMAT);
		dateTimeformatListPreference.setOnPreferenceChangeListener(dateTimeFormatChangeHandler);
		setDateTimeFormatSummary(Preferences.getDatetimeFormat(this));
		debugMode = (CheckBoxPreference)this.findPreference(Preferences.EMBEDED_DEBUG);
		debugMode.setOnPreferenceChangeListener(onDebugModeChanged);
		verboseDebugMode = (CheckBoxPreference)this.findPreference(Preferences.VERBOSE_LOG_LEVEL);
		//verboseDebugMode.setOnPreferenceChangeListener(onVerboseDebugModeChanged);

	}
	OnPreferenceChangeListener onDebugModeChanged = new OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue)
		{
			Boolean state = (Boolean)newValue;
			verboseDebugMode.setEnabled(state);
			Preferences.setEmbededDebug(AdvancedPreferenceActivity.this, state);
			CommonUtilities.log(LOG_ERROR_LEVEL, "Test", "Test");
			return true;
		}
	};

	OnPreferenceChangeListener onVerboseDebugModeChanged = new OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue)
		{
			Preferences.setVerboseDebug(AdvancedPreferenceActivity.this, (Boolean)newValue);
			return true;
		}
	};


	OnPreferenceChangeListener onPreventCPUSleepPreferenceChanged = new OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue)
		{
			CommonUtilities.updatePreventSleepMode(AdvancedPreferenceActivity.this, true);
			return true;
		}
	};

	OnPreferenceChangeListener autoCleanChangeHandler = new OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			setAutoCleanSummary((String) newValue);
			return true;
		}
	};

	public void setAutoCleanSummary(String value) {
		String template = getString(R.string.auto_clean_old_messages_summary);

		try {
			Integer intValue = Integer.parseInt(value);
			if (intValue == 0) {
				autoCleanEditText.setSummary(getString(R.string.no_auto_clean_old_messages_summary));
			} else {
				String plural = "";
				if (intValue > 1) {
					plural = "s";
				}

				autoCleanEditText.setSummary(String.format(template, intValue, plural));
			}
		} catch( NumberFormatException ex ) {
			autoCleanEditText.setSummary(getString(R.string.no_auto_clean_old_messages_summary));
		}		
	}

	OnPreferenceChangeListener maxMessageChangeHandler = new OnPreferenceChangeListener()
	{
		public boolean onPreferenceChange(Preference preference, Object newValue) {
		setMaxMessageSummary((String) newValue);
		return true;
		}
	};

	public void setDateTimeFormatSummary(String value) {
		dateTimeformatListPreference.setSummary(value);
	}


	OnPreferenceChangeListener dateTimeFormatChangeHandler = new OnPreferenceChangeListener()
	{
		public boolean onPreferenceChange(Preference preference, Object newValue) {
		setDateTimeFormatSummary((String) newValue);
		return true;
		}
	};

	public void setMaxMessageSummary(String value) {
		String template = getString(R.string.message_count_limit_summary);

		try {
			Integer intValue = Integer.parseInt(value);
			if (intValue == 0) {
				maxMessageEditText.setSummary(getString(R.string.message_count_no_limit_summary));
			} else {
				String plural = "";
				if (intValue > 1) {
					plural = "s";
				}

				maxMessageEditText.setSummary(String.format(template, intValue, plural));
			}
		} catch( NumberFormatException ex ) {
			maxMessageEditText.setSummary(getString(R.string.message_count_no_limit_summary));
		}		
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
