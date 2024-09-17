package com.newtifry.pro3.preferences;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.MenuItem;
import com.newtifry.pro3.Preferences;
import com.newtifry.pro3.R;
import com.newtifry.pro3.utils.AppCompatPreferenceActivity;

@SuppressWarnings("deprecation")
public class NewtifryPreferenceActivity extends AppCompatPreferenceActivity {
	static NewtifryPreferenceActivity context;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		addPreferencesFromResource(R.xml.preferences);
		Preference emailFCMIDPreference = findPreference("mailFCMId");
		emailFCMIDPreference.setOnPreferenceClickListener(onMailFCMIDClickListener);
	}

	@Override
	public void onResume() {
		super.onResume();
		NewtifryPreferenceActivity.context = this;
	}

	@Override
	public void onPause() {
		super.onPause();
		NewtifryPreferenceActivity.context = null;
	}

	OnPreferenceClickListener onMailFCMIDClickListener = preference -> {
        // User wants to email the key to someone.
        final Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("plain/text");
        String subject = String.format(getString(R.string.fcm_id_email_subject), Build.MODEL);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, String.format(getString(R.string.fcm_id_email_body), Preferences.getToken(NewtifryPreferenceActivity.this)));
        startActivity(Intent.createChooser(emailIntent, "Send key via email"));
        return true;
    };
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		} else {
				return super.onOptionsItemSelected(item);
		}
	}
}
