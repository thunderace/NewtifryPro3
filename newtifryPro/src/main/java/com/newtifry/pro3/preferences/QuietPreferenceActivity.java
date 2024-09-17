package com.newtifry.pro3.preferences;


import java.util.Set;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.view.MenuItem;

import com.afollestad.materialdialogs.prefs.MaterialMultiSelectListPreference;
import com.newtifry.pro3.Preferences;
import com.newtifry.pro3.R;
import com.newtifry.pro3.utils.AppCompatPreferenceActivity;

// See PreferenceActivity for warning suppression justification
@SuppressWarnings("deprecation")
public class QuietPreferenceActivity extends AppCompatPreferenceActivity {

	private Preference quietHoursPreference;
	private TimePreference quietStartPreference;
	private TimePreference quietEndPreference;
	private MaterialMultiSelectListPreference quietApplyPrioritiesPreference;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		addPreferencesFromResource(R.xml.quiet_preferences);

		quietStartPreference = (TimePreference)this.findPreference(Preferences.QUIET_HOURS_START);
//		quietStartPreference.setTime(Preferences.getQuietHoursStartString(this));
		quietEndPreference = (TimePreference)this.findPreference(Preferences.QUIET_HOURS_END);
//		quietEndPreference.setTime(Preferences.getQuietHoursEndString(this));
		quietHoursPreference = findPreference(Preferences.QUIET_HOURS);
	
//		((CheckBoxPreference)quietHoursPreference).setChecked(Preferences.getQuietHoursEnabled(this)); 
		quietHoursPreference.setOnPreferenceChangeListener(quietsHoursCheckListener);
		quietApplyPrioritiesPreference = (MaterialMultiSelectListPreference)findPreference(Preferences.QUIET_HOURS_APPLICATION);
		quietApplyPrioritiesPreference.setOnPreferenceChangeListener(quietApplyListener);
		toggleQuietHoursPreferences(Preferences.getQuietHoursEnabled(this));
		changeQuietApplyPrioritiesPreferences(Preferences.getQuietHoursPrioritiesApplication(this));
	}

	
	OnPreferenceChangeListener quietApplyListener = new OnPreferenceChangeListener()
	{
		@SuppressWarnings("unchecked")
		public boolean onPreferenceChange(Preference preference, Object newValue)
		{
			changeQuietApplyPrioritiesPreferences((Set<String>)newValue);
			return true;
		}
	};

	
	public void changeQuietApplyPrioritiesPreferences( Set<String> stringSet ) {
		String summary = "";
		boolean first = true;
		if (stringSet.isEmpty()) {
			summary = getString(R.string.none_summary);
	    } else {
	    	if (stringSet.size() == 4) {
				summary = getString(R.string.all_summary);
	    	} else {
		        for (String value : stringSet) {
		        	if (!first) {
		        		summary += ",";
		        	}
		        	if (value.equals("0")) {
		        		summary += getString(R.string.none);
		        	}
		        	if (value.equals("1")) {
		        		summary += getString(R.string.info);
		        	}
		        	if (value.equals("2")) {
		        		summary += getString(R.string.warning);
		        	}
		        	if (value.equals("3")) {
		        		summary += getString(R.string.alert);
		        	}
		        	first = false;
		        }
	    	}
	    }
		quietApplyPrioritiesPreference.setSummary(summary);
	}
	
	
	OnPreferenceChangeListener quietsHoursCheckListener = new OnPreferenceChangeListener()
	{
		public boolean onPreferenceChange(Preference preference, Object newValue)
		{
			toggleQuietHoursPreferences((Boolean)newValue);
			return true;
		}
	};

	public void toggleQuietHoursPreferences( boolean quietHours ) {
		quietStartPreference.setEnabled(quietHours);
		quietEndPreference.setEnabled(quietHours);
		quietApplyPrioritiesPreference.setEnabled(quietHours);
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
