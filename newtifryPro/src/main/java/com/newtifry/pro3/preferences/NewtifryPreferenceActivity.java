package com.newtifry.pro3.preferences;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.lang.ref.WeakReference;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import androidx.annotation.NonNull;

import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.prefs.MaterialEditTextPreference;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.newtifry.pro3.CommonUtilities;
import com.newtifry.pro3.NewtifryPro2App;
import com.newtifry.pro3.Preferences;
import com.newtifry.pro3.R;
import com.newtifry.pro3.utils.AppCompatPreferenceActivity;

@SuppressWarnings("deprecation")
public class NewtifryPreferenceActivity extends AppCompatPreferenceActivity {
	static NewtifryPreferenceActivity context;
	MaterialEditTextPreference userTopicPreference;
	MaterialEditTextPreference senderIdPreference;
	private BroadcastReceiver mRegistrationBroadcastReceiver;
	MaterialDialog progressDialog = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		addPreferencesFromResource(R.xml.preferences);
		Preference emailFCMIDPreference = (Preference) findPreference("mailFCMId");
		emailFCMIDPreference.setOnPreferenceClickListener(onMailFCMIDClickListener);
		Preference testPreference = (Preference) findPreference("preference_test");
		testPreference.setOnPreferenceClickListener(onPreferenceTestClickListener);
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

	OnPreferenceClickListener onMailFCMIDClickListener = new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
			// User wants to email the key to someone.
			final Intent emailIntent = new Intent(Intent.ACTION_SEND);
			emailIntent.setType("plain/text");
			String subject = String.format(getString(R.string.fcm_id_email_subject), Build.MODEL);
			emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
			emailIntent.putExtra(Intent.EXTRA_TEXT, String.format(getString(R.string.fcm_id_email_body), Preferences.getToken(NewtifryPreferenceActivity.this)));
			startActivity(Intent.createChooser(emailIntent, "Send key via email"));
			return true;
		}
	};

	OnPreferenceClickListener onPreferenceTestClickListener = new OnPreferenceClickListener() {
		public boolean onPreferenceClick(Preference preference) {
			sendFCMTestMessage();
			return true;
		}
	};
	private static class sendFCMMessageTask extends AsyncTask<Void, Void, String> {
		private MaterialDialog progressDialog = null;
		private WeakReference<NewtifryPreferenceActivity> activityReference;
		String regId;
		// only retain a weak reference to the activity
		sendFCMMessageTask(NewtifryPreferenceActivity context) {
			activityReference = new WeakReference<>(context);
		}
		protected void onPreExecute() {
			final AsyncTask<Void, Void, String> me = this;
			NewtifryPreferenceActivity activity = activityReference.get();
			if (activity == null || activity.isFinishing()) return;

			CommonUtilities.lockScreenOrientation(activity);
			progressDialog = new MaterialDialog.Builder(context)
					.content(context.getString(R.string.fcm_send_text))
					.progress(true, 0)
					.progressIndeterminateStyle(false)
					.negativeText(R.string.dialog_cancel)
					.onNegative(new MaterialDialog.SingleButtonCallback() {
						@Override
						public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
							me.cancel(true);
						}
					}).show();
			//regId = Preferences.getPrivateToken(context);
			regId = Preferences.getToken(context);
		}

		@Override
		protected String doInBackground(Void... params) {
			try {
				JSONObject jFCMData = new JSONObject();
				JSONObject data = new JSONObject();
				data.put("type", "ntp_message");
				data.put("message", Base64.encodeToString(context.getString(R.string.test_message_message).getBytes(), Base64.DEFAULT));
				data.put("source", Base64.encodeToString(context.getString(R.string.test_message_source).getBytes(), Base64.DEFAULT));
				SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
				ISO8601DATEFORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
				String timestamp = ISO8601DATEFORMAT.format(new Date());
				data.put("timestamp", timestamp);
				data.put("NPpriority", "3");
				data.put("url", Base64.encodeToString("https://github.com/thunderace/NewtifryPro".getBytes(), Base64.DEFAULT));
				data.put("image1", Base64.encodeToString("https://raw.githubusercontent.com/thunderace/NewtifryPro/master/images/test_newtifry1.jpg".getBytes(), Base64.DEFAULT));
				data.put("image2", Base64.encodeToString("https://raw.githubusercontent.com/thunderace/NewtifryPro/master/images/test_newtifry2.png".getBytes(), Base64.DEFAULT));
				data.put("image3", Base64.encodeToString("https://raw.githubusercontent.com/thunderace/NewtifryPro/master/images/test_newtifry3.jpg".getBytes(), Base64.DEFAULT));
				data.put("image4", Base64.encodeToString("https://raw.githubusercontent.com/thunderace/NewtifryPro/master/images/test_newtifry4.jpg".getBytes(), Base64.DEFAULT));
				data.put("image5", Base64.encodeToString("https://raw.githubusercontent.com/thunderace/NewtifryPro/master/images/test_newtifry5.jpg".getBytes(), Base64.DEFAULT));
				data.put("title", Base64.encodeToString(context.getString(R.string.test_message_title).getBytes(), Base64.DEFAULT));
				// Where to send FCM message.
				jFCMData.put("to", regId);
				// What to send in FCM message.
				jFCMData.put("data", data);

				// Create connection to send FCM Message request.
				URL url = new URL("https://fcm.googleapis.com/fcm/send");
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestProperty("Authorization", "key=" + CommonUtilities.FCM_AUTH_KEY);
				conn.setRequestProperty("Content-Type", "application/json");
				conn.setRequestMethod("POST");
				conn.setDoOutput(true);

				// Send FCM message content.
				OutputStream outputStream = conn.getOutputStream();
				outputStream.write(jFCMData.toString().getBytes());

				// Read FCM response.
				InputStream inputStream = conn.getInputStream();
				String resp = IOUtils.toString(inputStream);
				//System.out.println(resp);
			} catch (JSONException e) {
				return e.getMessage();
			} catch (IOException e) {
				e.printStackTrace();
				return e.getMessage();
			}
			return null;
 		}

		@Override
		protected void onPostExecute(String msg) {
			Context context = NewtifryPro2App.getContext();
			if (progressDialog.isShowing()) {
				ProgressBar progressBar = (ProgressBar) progressDialog.findViewById(android.R.id.progress);
				if (progressBar != null) {
					progressBar.setVisibility(View.GONE);
				}
				progressDialog.setActionButton(DialogAction.NEGATIVE, context.getString(R.string.dialog_ok));
				if (msg == null) {
					msg = context.getString(R.string.test_message_success);
				} else {
					msg = context.getString(R.string.send_test_message_error) + msg;
				}
				progressDialog.setContent(msg);
			}
			NewtifryPreferenceActivity activity = activityReference.get();
			if (activity == null || activity.isFinishing()) return;
			CommonUtilities.unlockScreenOrientation(activity);
		}
	}

	public void sendFCMTestMessage() {
		new sendFCMMessageTask(this).execute();
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
