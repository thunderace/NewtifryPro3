package com.newtifry.pro3.preferences;


import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.MenuItem;

import com.afollestad.materialdialogs.prefs.MaterialEditTextPreference;
import com.newtifry.pro3.CommonUtilities;
import com.newtifry.pro3.Preferences;
import com.newtifry.pro3.R;
import com.newtifry.pro3.utils.AppCompatPreferenceActivity;

// See PreferenceActivity for warning suppression justification
@SuppressWarnings("deprecation")
public class SpeakPreferenceActivity extends AppCompatPreferenceActivity {

	private MaterialEditTextPreference delayReading;
	private MaterialEditTextPreference shakeThreshold;
	private MaterialEditTextPreference shakeWaitTime;
	private MaterialEditTextPreference maxLength;
	private MaterialEditTextPreference noSpeakLength;
	private Preference previewSpeech;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		addPreferencesFromResource(R.xml.speak_preferences);

		delayReading = (MaterialEditTextPreference)this.findPreference(Preferences.DELAY_READING_TIME);
		delayReading.setOnPreferenceChangeListener(delayReadingHandler);
		updateDelaySummary(Preferences.getDelayReadingTime(this));

		shakeThreshold = (MaterialEditTextPreference)this.findPreference(Preferences.SHAKE_THRESHOLD);
		shakeThreshold.setOnPreferenceChangeListener(shakeThresholdHandler);
		updateThresholdSummary(Preferences.getShakeThreshold(this));
		
		shakeWaitTime = (MaterialEditTextPreference)this.findPreference(Preferences.SHAKE_WAIT_TIME);
		shakeWaitTime.setOnPreferenceChangeListener(shakeWaitTimeHandler);
		updateShakeWaitTimeSummary(Preferences.getShakeWaitTime(this));
		
		maxLength = (MaterialEditTextPreference)this.findPreference(Preferences.MAX_LENGTH);
		maxLength.setOnPreferenceChangeListener(maxLengthHandler);
		updateMaxLength(Preferences.getMaxLength(this));
		
		
		noSpeakLength = (MaterialEditTextPreference)this.findPreference(Preferences.NOT_SPEAK_LENGTH);
		noSpeakLength.setOnPreferenceChangeListener(noSpeakLengthHandler);
		updateNoSpeakLength(Preferences.getNotSpeakLength(this));
		
		previewSpeech = findPreference(Preferences.PREVIEW_SPEECH);
		previewSpeech.setOnPreferenceClickListener(previewSpeechHandler);
	}

	// On click handler for previewing speech.
	OnPreferenceClickListener previewSpeechHandler = new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference)	{
			CommonUtilities.speak(getBaseContext(), getString(R.string.preview_speak));
			return true;
		}
	};

	// On Preference change listener to update the delay summary.
	OnPreferenceChangeListener delayReadingHandler = new OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			updateDelaySummary((String) newValue);
			return true;
		}
	};

	// On Preference change listener to update the shake threshold summary.
	OnPreferenceChangeListener shakeThresholdHandler = new OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			updateThresholdSummary((String) newValue);
			return true;
		}
	};	

	// On Preference change listener to update the shake wait time summary.
	OnPreferenceChangeListener shakeWaitTimeHandler = new OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			updateShakeWaitTimeSummary((String) newValue);
			return true;
		}
	};
	
	OnPreferenceChangeListener noSpeakLengthHandler = new OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			updateNoSpeakLength((String) newValue);
			return true;
		}
	};

	OnPreferenceChangeListener maxLengthHandler = new OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			updateMaxLength((String) newValue);
			return true;
		}
	};

	// Helper function to update the delay summary.
	private void updateDelaySummary(String value) {
		String template = getString(R.string.delay_readout_summary);

		try {
			Integer intValue = Integer.parseInt(value);
			String result; 
			if (intValue == 0) {
				result = getString(R.string.delay_readout_no_delay_summary);
			} else {
				String plural = "s";
				if (intValue == 1) {
					plural = "";
				}
				result = String.format(template, intValue, plural);
			}
			delayReading.setSummary(result);
		} catch( NumberFormatException ex ) {
			delayReading.setSummary(getString(R.string.delay_readout_no_delay_summary));
			// Not a valid number... ignore.
		}
	}
	
	// Helper function to update the threshold summary.
	private void updateThresholdSummary(String value) {
		String template = getString(R.string.shakethreshhold_summary);

		try	{
			Integer intValue = Integer.parseInt(value);
			String result = String.format(template, intValue);
			shakeThreshold.setSummary(result);
		} catch( NumberFormatException ex ) {
			// Not a valid number... ignore.
		}
	}
	
	// Helper function to update the wait time summary.
	private void updateShakeWaitTimeSummary(String value) {
		String template = getString(R.string.shakewaittime_summary);

		try {
			Integer intValue = Integer.parseInt(value);

			String plural = "s";
			if (intValue == 1) {
				plural = "";
			}

			String result = String.format(template, intValue, plural);
			shakeWaitTime.setSummary(result);
		} catch( NumberFormatException ex ) {
			// Not a valid number... ignore.
		}
	}

	private void updateMaxLength(String value) {
		String template = getString(R.string.max_length_summary);
		
		try	{
			Integer intValue = Integer.parseInt(value);
			if (intValue == 0) {
				maxLength.setSummary(getString(R.string.max_length_summary_no_limit));
			} else {
				maxLength.setSummary(String.format(template, intValue));
			}
		} catch( NumberFormatException ex )	{
			maxLength.setSummary(getString(R.string.max_length_summary_no_limit));
		}
	}
	
	private void updateNoSpeakLength(String value) {
		String template = getString(R.string.limit_length_to_speak_summary);
		
		try	{
			Integer intValue = Integer.parseInt(value);
			if (intValue == 0) {
				noSpeakLength.setSummary(getString(R.string.limit_length_to_speak_summary_no_limit));
			} else {
				noSpeakLength.setSummary(String.format(template, intValue));
			}
		} catch( NumberFormatException ex )	{
			maxLength.setSummary(getString(R.string.limit_length_to_speak_summary_no_limit));
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
