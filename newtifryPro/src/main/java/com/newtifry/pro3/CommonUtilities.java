
package com.newtifry.pro3;


import java.io.File;
import java.net.URL;
import java.util.List;

import com.newtifry.pro3.database.NewtifryMessage2;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Helper class providing methods and constants common to other classes in the
 * app.
 */
public final class CommonUtilities {
    public static final String TAG = "NewtifryPro3";
	public static final String MESSAGE_DDB_CHANGE_INTENT = "com.newtifry.pro3.DDBDeleteItem(s)";
    public static final String REGISTRATION_COMPLETE = "registrationComplete";
    public static final String STOP_SPEAK = "stopNow";
	public static final String SLEEP_NOTIFICATION_CHANNEL = "NPSleepChannel";
	public static final String ALERT_NOTIFICATION_CHANNEL = "NPAlertChannel";
	public static final String WARNING_NOTIFICATION_CHANNEL = "NPWarningChannel";
	public static final String INFORMATION_NOTIFICATION_CHANNEL = "NPInformationChannel";
	public static final String NO_PRIORITY_NOTIFICATION_CHANNEL = "NPNoPriorityChannel";

	// startforegroung notification id
	private static final int SLEEP_NOTIFICATION_ID = 0XDEAD;
	public static final int ID_SPEAK_SERVICE = 1101;
	public static final int ID_NOTIFICATION_SERVICE = 101;
	public final String PRIORITY_NONE = "0";
	public final String PRIORITY_INFO = "1";
	public final String PRIORITY_WARNING = "2";
	public final String PRIORITY_ALERT = "3";
	//public static long newNotificationMessageId = -1;

	// Setting.Global "zen_mode"
	public static final int ZENMODE_ALL = 0; // DNd is off
	public static final int ZENMODE_PRIORITY = 1; // only by priority so???
	public static final int ZENMODE_NONE = 2; // total silence
	public static final int ZENMODE_ALARMS = 3; /// alarm only

	public static File getPublicMediaDir() {
		File dir = new File(Environment.getExternalStorageDirectory(),
				"NewtifryPro2/images");
		if(!dir.exists()) {
			if (dir.mkdirs()) {
				File noMedia = new File(dir, ".nomedia");
				if (!noMedia.exists()) {
					noMedia.mkdirs();
				}
			}
		}
		return dir;
	}

	public static boolean okToDownloadData(Context context) {
		if (!Preferences.onlyWifi(context)) {
			return true;
		}
		ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE) ;
		NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return networkInfo.isConnected();
	}

	public static boolean isActivityCallable( Context context, String packageName, String className) {
		final Intent intent = new Intent();
		intent.setClassName(packageName, className);
		List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	public static String getOutputMessage(NewtifryMessage2 message, Context context) {
		String format = Preferences.getSpeakFormat(context);
		
		StringBuffer buffer = new StringBuffer(format);
		CommonUtilities.formatString(buffer, "%t", message.getTitle());
		CommonUtilities.formatString(buffer, "%m", message.getTextMessage());
		CommonUtilities.formatString(buffer, "%s", message.getSourceName());
		CommonUtilities.formatString(buffer, "%%", "%");
		try {
			int maxLength = Integer.parseInt(Preferences.getMaxLength(context));
			if (maxLength != 0) {
				return buffer.toString().substring(0, maxLength);
			}
		} catch (Exception ex) {
			
		}

		return buffer.toString();
	}

	
	public static boolean checkGoogleAccount(Context context) {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			return true;
		}
		// check if we have a google account here
		AccountManager manager = AccountManager.get(context);
		Account[] accounts = manager.getAccountsByType("com.google");
        return accounts.length != 0;
    }

	public static void formatString( StringBuffer buffer, String keyword, String value )	{
		int position = -1;
		while( (position = buffer.indexOf(keyword)) != -1 ) {
			buffer.replace(position, position + keyword.length(), value);
		}
	}

	public static String getURLWithoutCredentials(String url) {
        try {
        	final URL u = new URL(url);
            String userInfo = u.getUserInfo(); 
           	if ( userInfo != null) {
           		return url.replace(userInfo + "@", "");
           	} else {
           		return url;
           	}
        } catch (final Throwable e) {
            return url;
        }
	}
	
	public static boolean isEmpty(String str) {
		if (str == null)
			return true;
        return str.isEmpty();

    }
	
	public static void stopSpeak(Context context) {
		Intent intentData = new Intent(context, NewSpeakService.class);
		intentData.putExtra(CommonUtilities.STOP_SPEAK, true);
		Log.d("DBG", "from stop Speak");
		ContextCompat.startForegroundService(context,intentData);
	}

	public static void speak(Context context, String message) {
		Intent intentData = new Intent(context, NewSpeakService.class);
		intentData.putExtra("text", message);
		Log.d("DBG", "from speak");
		ContextCompat.startForegroundService(context,intentData);
	}

    public static int getAudioStream(Context context, boolean TTSAudioStream) {
		String desiredStream;
		if (TTSAudioStream) {
			desiredStream = Preferences.getTTSAudioStream(context);
		} else {
			desiredStream = Preferences.getNotificationAudioStream(context);
		}
		int stream = AudioManager.STREAM_NOTIFICATION;
		if( desiredStream.equals("ALARM") )	{
			stream = AudioManager.STREAM_ALARM;
		} else { 
			if( desiredStream.equals("MUSIC") )	{
				stream = AudioManager.STREAM_MUSIC;
			}
		}
		return stream;
	}
	

	public static String getNotificationChannel(Context context, long messageId) {
		int messagePriority = NewtifryMessage2.get(context, messageId).getPriority();
		switch (messagePriority) {
			case 1:
				return INFORMATION_NOTIFICATION_CHANNEL;
			case 2:
				return WARNING_NOTIFICATION_CHANNEL;
			case 3:
				return ALERT_NOTIFICATION_CHANNEL;
			default:
				return NO_PRIORITY_NOTIFICATION_CHANNEL;
		}
	}

	public static void sleepNotification(Context context, boolean show) {
		NotificationManager mNotificationManager =
			    (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
		if (!show) {
			mNotificationManager.cancel(SLEEP_NOTIFICATION_ID);
			return;
		}
		NotificationCompat.Builder mBuilder =
		        new NotificationCompat.Builder(context, SLEEP_NOTIFICATION_CHANNEL)
		        .setSmallIcon(R.drawable.ic_stat_statusbar_newtifrypro2)
		        .setContentTitle(context.getString(R.string.app_name))
		        .setContentText(context.getString(R.string.notificationNoCPUSleepMode))
		        .setAutoCancel(false)
		        .setPriority(NotificationCompat.PRIORITY_MAX)
				.setOngoing(true);
		
		mNotificationManager.notify(SLEEP_NOTIFICATION_ID, mBuilder.build());
	}	

	private static  PowerManager pm = null;
	private static WakeLock wl = null;
	private static boolean preventSleep;
	@SuppressLint("Wakelock")
	public static void updatePreventSleepMode(Context context, boolean invert) {
		if (pm == null) {
			pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
			wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NewtifryPro2:Tag");
			preventSleep = false;
		}
		boolean prevent = Preferences.preventCPUSleep(context);
		if (invert) {
			prevent = !prevent;
		}
		if (prevent) {
			if (!preventSleep) {
				sleepNotification(context, true);
				wl.acquire();
				preventSleep = true;
			}
		} else {
			if (preventSleep) {
				sleepNotification(context, false);
				wl.release();
				preventSleep = false;
			}
		}
	}

    public static  void lockScreenOrientation(Activity activity) {
        int currentOrientation = activity.getBaseContext().getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    public static void unlockScreenOrientation(Activity activity) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }

	public static boolean isVersion(int version) {
		return Build.VERSION.SDK_INT >= version;
	}
	public static final int LOG_ERROR_LEVEL = 1;
	public static final int LOG_VERBOSE_LEVEL = 3;

	public static void log(int level, String title, String message) {
		Log.d(title, message);
        Context context = NewtifryPro2App.getContext();
		if (level == LOG_VERBOSE_LEVEL && !Preferences.isVerboseLogLevel(context)) {
			return;
		}
		if(!Preferences.isEmbededDebug(context)) {
			return;
		}
		long debugMessageId = Preferences.getDebugMessageId(context);

		NewtifryMessage2 debugMessage = NewtifryMessage2.get(context, debugMessageId);
		if (debugMessage == null || debugMessage.isDeleted()) {
			debugMessage = new NewtifryMessage2();
			debugMessage.setMessage("<p>"+ debugMessage.getDisplayTimestamp() + ":" + title + "-" + message + "</p>");
			debugMessage.setTitle("Messages");
			debugMessage.setSourceName("Debug");
			debugMessage.setTimestamp(null); // will set to the current timestampTextView
			debugMessage.setPriority(3);
			debugMessage.setSeen(false);
			debugMessage.setNoCache(false);
			debugMessage.setSticky(true);
			debugMessage.setDeleted(false);
			debugMessage.setNotify(0);
			debugMessage.save(context);
			Preferences.setDebugMessageId(context, debugMessage.getId());
		} else {
			String debugText = debugMessage.getMessage();
			debugMessage.setTimestamp(null); // will set to the current timestampTextView
			debugText = "<p>"+ debugMessage.getDisplayTimestamp() + ":" + title + "-" + message + "</p>"  + debugText;
			debugMessage.setMessage(debugText);
			debugMessage.save(context);
		}
	}


}
