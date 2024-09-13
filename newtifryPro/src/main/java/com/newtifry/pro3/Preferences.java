package com.newtifry.pro3;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import androidx.core.app.NotificationCompat;

import com.h6ah4i.android.compat.content.SharedPreferenceCompat;

public class Preferences {
	public static final String SENDER_ID = "senderId";
	public static final String NOTIFIER_PRO_INSTALLED = "notifierProInstalled";
	public static final String REGISTER_ERROR = "register_error";
	public static final String NOTIFICATION_ENABLE = "notificationEnable";
	
	public static final String SMALL_ROW = "smallRow";
	
	public static final String SHOW_IMAGES = "showImage";
	public static final String PRELOAD_IMAGES = "preloadImage";
	public static final String CACHE_IMAGES = "cacheImages";
	public static final String CACHE_IMAGES_DELAY = "cacheImagesDelay";
	public static final String SHRINK_IMAGES = "shrinkImages";
	public static final String PURGES_IMAGES_CACHE = "purgeImageCache";
	
	
	
	public static final String QUIET_HOURS = "quietHoursEnable";
	public static final String QUIET_HOURS_START = "quietHoursStart";
	public static final String QUIET_HOURS_END = "quietHoursEnd";
	public static final String QUIET_HOURS_APPLICATION = "quitHoursPrioritiesApplication";
	
	
	public static final String AUTO_CLEAN_MESSAGE_DAYS = "autoCleanOldMessage";
	public static final String MAX_MESSAGE_COUNT = "maxMessageCount";
	public static final String CONFIRM_DELETE_ALL = "confirmDeleteAll";
	public static final String PREVENT_DELETE_UNSEEN = "doNotDeleteUnseen";
	public static final String ALLOW_DELETE_PINNED_AND_LOCKED = "deletePinnedAndLocked";
	public static final String ALLOW_DELETE_ALLSEEN_PINNED_AND_LOCKED = "deleteAllSeenPinnedAndLocked";
	
	
	public static final String USE_PRIORITY_COLORS = "usePriorityColors";
	public static final String ALERT_TITLE_COLOR = "alertTitleColor";
	public static final String WARNING_TITLE_COLOR = "warningTitleColor";
	public static final String INFO_TITLE_COLOR = "infoTitleColor";
	public static final String USE_NOTIFIER_PRO = "useNotifierPro";
	public static final String USE_BLACK_ACTION_ICONS = "useBlackActionIcons";

	public static final String VIBRATE_NOTIFY = "vibrateNotify";
	public static final String NOTIFY_EVERYTIME = "notifyEveryTime";
	public static final String LED_FLASH = "ledFlash";
	public static final String MAX_PRIORITY = "maxPriority";
	public static final String SPEAK_MESSAGE = "speakMessage";
	public static final String PREVIEW_SPEECH = "previewSpeech";
	public static final String SPEAK_FORMAT = "speakFormat";
	public static final String DELAY_READING_TIME = "delayReadingTime";
	public static final String GLOBAL_RINGTONE = "choosenNotificationGlobal";
	public static final String USE_BY_PRIORITY_RINGTONE = "soundByPriority";
	public static final String ALERT_PRIORITY_RINGTONE = "choosenNotificationAlert";
	public static final String WARNING_PRIORITY_RINGTONE = "choosenNotificationWarning";
	public static final String NORMAL_PRIORITY_RINGTONE = "choosenNotificationNormal";
	public static final String NONE_PRIORITY_RINGTONE = "choosenNotificationNone";

	public static final String GLOBAL_NOTIFICATION_VISIBILITY = "globalNotificationVisibility";
	public static final String USE_BY_PRIORITY_NOTIFICATION_VISIBILITY = "notificationVisibilityByPriority";
	public static final String ALERT_NOTIFICATION_VISIBILITY = "alertNotificationVisibility";
	public static final String WARNING_NOTIFICATION_VISIBILITY = "warningNotificationVisibility";
	public static final String NORMAL_NOTIFICATION_VISIBILITY = "normalNotificationVisibility";
	public static final String NONE_NOTIFICATION_VISIBILITY = "noneNotificationVisibility";


	public static final String SHAKE_THRESHOLD = "shakeThreshold";
	public static final String SHAKE_TO_STOP = "shakeToStop";
	public static final String SHAKE_WAIT_TIME = "shakeWaitTime";
	public static final String MAX_LENGTH = "maxReadingLength";
	public static final String NOT_SPEAK_LENGTH = "readingLengthNoSpeak";
	public static final String ALERT_PRIORITY_SPEAK = "alertPrioritySpeak";
	public static final String WARNING_PRIORITY_SPEAK = "warningPrioritySpeak";
	public static final String INFO_PRIORITY_SPEAK = "infoPrioritySpeak";
	public static final String NONE_PRIORITY_SPEAK = "nonePrioritySpeak";
	
	public static final String NOTIFICATION_FORMAT = "notificationFormat";
	public static final String NOTIFICATION_ONE_FORMAT = "notificationOneFormat";
	
	public static final String TTS_AUDIO_STREAM = "ttsAudioStream";
	public static final String SPEAK_SETTINGS = "speakSettings";
	public static final String QUIET_SETTINGS = "quietSettings";
	
	public static final String NOTIFICATION_AUDIO_STREAM = "notificationAudioStream";
	
	
	public static final String DISPLAY_SETTINGS = "displaySettings";
	public static final String NOTIFICATION_SETTINGS = "notificationSettings";

	public static final String FCM_REGISTRATION_ID = "newtifry_fcm_registration_id";
	public static final String FCM_PRIVATE_REGISTRATION_ID = "newtifry_fcm_private_registration_id";

	public static final String ONLY_WIFI = "onlyImageWithWifi";
	public static final String SHOW_INVISIBLE = "showInvisibleMessages";
	
	public static final String ENABLE_UNDO = "enableUndo";
	public static final String PREVENT_CPU_SLEEP = "preventCpuSleep";

	public static final String FCM_USER_TOPIC = "fcmUserTopic";
	public static final String EMBEDED_DEBUG = "embededDebug";
	public static final String VERBOSE_LOG_LEVEL = "verboseLogLevel";

	public static final String DEBUG_MESSAGE_ID = "debugMessageId";
	public static final String DATE_TIME_FORMAT = "dateTimeFormatSettings";

	public static final String GROUP_MESSAGES = "groupMessages";


	public static String getDatetimeFormat(Context context) {
		return getSettings(context.getApplicationContext()).getString(DATE_TIME_FORMAT, "SYSTEM");
	}

	public static boolean groupMessages(Context context) {
		return getSettings(context).getBoolean(GROUP_MESSAGES, true);
	}
	public static long getDebugMessageId(Context context) {
		return getSettings(context).getLong(DEBUG_MESSAGE_ID, -1);
	}

	public static void setDebugMessageId(Context context, long messageId) {
		SharedPreferences.Editor editor = getSettings(context).edit();
		editor.putLong(DEBUG_MESSAGE_ID, messageId);
		editor.commit();
	}

	public static boolean isEmbededDebug(Context context) {
		return getSettings(context).getBoolean(EMBEDED_DEBUG, false);
	}
	public static boolean isVerboseLogLevel(Context context) {
		return getSettings(context).getBoolean(VERBOSE_LOG_LEVEL, false);
	}
	public static boolean setEmbededDebug(Context context, boolean debug) {
		SharedPreferences.Editor editor = getSettings(context).edit();
		editor.putBoolean(EMBEDED_DEBUG, debug);
		editor.commit();
		return true;
	}
	public static boolean setVerboseDebug(Context context, boolean verbose) {
		SharedPreferences.Editor editor = getSettings(context).edit();
		editor.putBoolean(VERBOSE_LOG_LEVEL, verbose);
		editor.commit();
		return true;
	}

	public static String getUserTopic(Context context){
		//setLocalTopic(context, android.os.Build.MODEL);
		//setUserTopic(context, android.os.Build.MODEL);
		String localTopic = getSettings(context.getApplicationContext()).getString(FCM_USER_TOPIC, "newtifrypro").replaceAll("\\s+", "");
		final Pattern pattern = Pattern.compile("[a-zA-Z0-9-_.~%]+");
		if (!pattern.matcher(localTopic).matches()) {
			setUserTopic(context, "newtifrypro");
			return "newtifrypro";
		}
		return localTopic.replaceAll("\\s+", "");
		//Build.DEVICE;
	}

	public static void setUserTopic(Context context, String model){
		SharedPreferences.Editor editor = getSettings(context).edit();
		editor.putString(FCM_USER_TOPIC, model);
		editor.commit();
	}


	//// WEAR settings
	public static final String WEAR_SHOW_IMAGES = "wearShowImages";
	public static boolean showImagesInWear(Context context) {
		return getSettings(context).getBoolean(WEAR_SHOW_IMAGES, true);
	}

	public static boolean showInvisibleMessages(Context context) {
		return getSettings(context).getBoolean(SHOW_INVISIBLE, false);
	}
	
	public static boolean onlyWifi(Context context) {
		return getSettings(context).getBoolean(ONLY_WIFI, false);
	}

	public static boolean getNotifyEverytime(Context context) {
		return getSettings(context).getBoolean(NOTIFY_EVERYTIME, false);
	}
	
	public static boolean preventCPUSleep(Context context) {
		return getSettings(context).getBoolean(PREVENT_CPU_SLEEP, false);
	}
	
	public static boolean getSpeakMessage(Context context) {
		return getSettings(context).getBoolean(SPEAK_MESSAGE, true);
	}

	public static void setSpeakMessage(Context context, boolean speak) {
		SharedPreferences.Editor editor = getSettings(context).edit();
		editor.putBoolean(SPEAK_MESSAGE, speak);
		editor.commit();
	}
	
    public static boolean getSpeakByPriority(Context context, int priority) {
    	if (getSettings(context).getBoolean(SPEAK_MESSAGE, true) == false) {
    		return false;
    	}
		switch(priority) {
			case 3:
				return getSettings(context).getBoolean(ALERT_PRIORITY_SPEAK, true);
			case 2:
				return getSettings(context).getBoolean(WARNING_PRIORITY_SPEAK, true);
			case 1:
				return getSettings(context).getBoolean(INFO_PRIORITY_SPEAK, true);
			case 0:
				return getSettings(context).getBoolean(NONE_PRIORITY_SPEAK, true);
		}
		return false;
    }

    public static boolean isUndoEnable(Context context) {
		return getSettings(context).getBoolean(ENABLE_UNDO, true);
    }

	public static boolean getAllowDeletePinnedAndLockedMessages(Context context) {
		return getSettings(context).getBoolean(ALLOW_DELETE_PINNED_AND_LOCKED, false);
				
	}

	public static boolean getAllowAllSeenDeletePinnedAndLockedMessages(Context context) {
		return getSettings(context).getBoolean(ALLOW_DELETE_ALLSEEN_PINNED_AND_LOCKED, false);
				
	}
	
    public static boolean getDeleteUnseenMessages(Context context) {
		return !getSettings(context).getBoolean(PREVENT_DELETE_UNSEEN, false);
				
	}
	public static String getAutoCleanMessagesDays(Context context) {
		return getSettings(context.getApplicationContext()).getString(AUTO_CLEAN_MESSAGE_DAYS, "0"); // disable by default
	}
	public static String getMaxMessageCount(Context context) {
		return getSettings(context.getApplicationContext()).getString(MAX_MESSAGE_COUNT, "0"); // disable by default
	}
	
	public static boolean getShowImages(Context context) {
		return getSettings(context).getBoolean(SHOW_IMAGES, true);
	}
	
	public static boolean getPreloadBitmap(Context context) {
		if (getShowImages(context) == false) {
			return false;
		}
		return getSettings(context).getBoolean(PRELOAD_IMAGES, true);
	}
	
	public static boolean getCacheBitmap(Context context) {
		return getSettings(context).getBoolean(CACHE_IMAGES, true);
	}
	
	public static boolean getShrinkBitmap(Context context) {
		return getSettings(context).getBoolean(SHRINK_IMAGES, false);
	}
	
	
	public static String getCacheBitmapDuration(Context context) {
		return getSettings(context.getApplicationContext()).getString(CACHE_IMAGES_DELAY, "24");
	}

	public static int getCacheBitmapDurationInMs(Context context) {
		Integer retValue = 24;
		try {
			retValue = Integer.parseInt(getSettings(context).getString(CACHE_IMAGES_DELAY, "24"));
		} catch( NumberFormatException ex ) {
			// Not a valid number... ignore.
		}		
		return retValue * 1000 * 60 * 60;
	}
	

	public static String getPassword() {
		return "thunderace";
	}

	public static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    public static void savePrivateToken (String token, Context context) {
		SharedPreferences.Editor editor = context.getSharedPreferences("gcm", Context.MODE_PRIVATE).edit();
        editor.putString(FCM_PRIVATE_REGISTRATION_ID, token);
        editor.commit();
        // erase the old one
        editor = getSettings(context).edit();
        editor.putString(FCM_PRIVATE_REGISTRATION_ID, token);
    }

    public static String getPrivateToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("gcm", Context.MODE_PRIVATE);
        String value = prefs.getString(FCM_PRIVATE_REGISTRATION_ID, "");
        if (value.equals("")) {
            // try the old one
            value = getSettings(context).getString(FCM_PRIVATE_REGISTRATION_ID, "");
        }
        return value;
    }
	
    public static void saveToken (String token, Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences("gcm", Context.MODE_PRIVATE).edit();
        editor.putString(FCM_REGISTRATION_ID, token);
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     editor.commit();
        // erase the old one
        editor = getSettings(context).edit();
        editor.putString(FCM_REGISTRATION_ID, token);
    }

    public static String getToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("gcm", Context.MODE_PRIVATE);
        String value =  prefs.getString(FCM_REGISTRATION_ID, "");
        if (value == "") {
            // try the old one
            value = getSettings(context).getString(FCM_REGISTRATION_ID, "");
        }
        return value;
    }

	public static boolean getQuietHoursEnabled(Context context) {
		return getSettings(context).getBoolean(QUIET_HOURS, false);
	}

	public static String getQuietHoursStartString(Context context) {
		return getSettings(context).getString(QUIET_HOURS_START, "22:00");
	}

	public static String getQuietHoursEndString(Context context) {
		return getSettings(context).getString(QUIET_HOURS_END, "06:00");
	}

	public static Set<String> getQuietHoursPrioritiesApplication(Context context) {
		Set<String> defValues = new HashSet<String>();
		defValues.add("3");
		defValues.add("2");
		defValues.add("1");
		defValues.add("0");
		return SharedPreferenceCompat.getStringSet(getSettings(context), QUIET_HOURS_APPLICATION, defValues);
	}

	
	
//    public static boolean isApplicationBroughtToBackground(Context context) {
//        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
//        List<RunningTaskInfo> tasks = am.getRunningTasks(1);
//        if (!tasks.isEmpty()) {
//            ComponentName topActivity = tasks.get(0).topActivity;
//            if (!topActivity.getPackageName().equals(context.getPackageName())) {
//                return true;
//            }
//        }
//
//        return false;
//    }
	
	
	public static String getSenderId(Context context) {
		return getSettings(context).getString(SENDER_ID, "");
		
	}
	
	public static void saveSenderId(String senderId, Context context) {
		SharedPreferences.Editor editor = getSettings(context).edit();
		editor.putString(SENDER_ID, senderId);
		editor.commit();
	}
/*
	public static void saveNotifierProInstalled(boolean installed, Context context) {
		SharedPreferences.Editor editor = getSettings(context).edit();
		editor.putBoolean(NOTIFIER_PRO_INSTALLED, installed);
		editor.commit();
	}

	public static boolean getNotifierProInstalled(Context context) {
		return getSettings(context).getBoolean(NOTIFIER_PRO_INSTALLED, false);
	}

	public static boolean getUseNotifierPro(Context context) {
		return getSettings(context).getBoolean(USE_NOTIFIER_PRO, false);
	}
*/
	public static boolean getUseBlackActionIcons(Context context) {
		return getSettings(context).getBoolean(USE_BLACK_ACTION_ICONS, false);
	}


	public static void saveRegisterError(String error, Context context) {
		SharedPreferences.Editor editor = getSettings(context).edit();
		editor.putString(REGISTER_ERROR, error);
		editor.commit();
	}

	public static String getRegisterError(Context context) {
		return getSettings(context).getString(REGISTER_ERROR, "");
	}
	
	public static boolean getNotificationsEnable(Context context) {
		return getSettings(context).getBoolean(NOTIFICATION_ENABLE, true);
	}
	
	public static void saveNotificationEnable(boolean enable, Context context) {
		SharedPreferences.Editor editor = getSettings(context).edit();
		editor.putBoolean(NOTIFICATION_ENABLE, enable);
		editor.commit();
	}
	
	public static boolean getVibrateNotify(Context context) {
		return getSettings(context).getBoolean(VIBRATE_NOTIFY, true);
	}
	
	public static boolean getLedFlash(Context context) {
		return getSettings(context).getBoolean(LED_FLASH, true);
	}

	public static boolean getMaxPriority(Context context) {
		return getSettings(context).getBoolean(MAX_PRIORITY, true);
	}

	public static boolean getUseSmallRow(Context context) {
		return getSettings(context).getBoolean(SMALL_ROW, true);
	}
	
	
	
	public static boolean getConfirmDeleteAll(Context context) {
		return getSettings(context).getBoolean(CONFIRM_DELETE_ALL, true);
	}
	
	
	public static int getAlertTitleColor(Context context) {
		return getSettings(context.getApplicationContext()).getInt(ALERT_TITLE_COLOR, 0xffc00101 /* R.integer.COLOR_RED */);
	}

	public static int getWarningTitleColor(Context context) {
		return getSettings(context.getApplicationContext()).getInt(WARNING_TITLE_COLOR, 0xffff8C00 /* R.integer.COLOR_YELLOW */);
	}

	public static int getInfoTitleColor(Context context) {
		return getSettings(context.getApplicationContext()).getInt(INFO_TITLE_COLOR, 0xff6fa209/* R.integer.COLOR_GREEN */);
	}
	
	public static boolean getUsePriorityColor(Context context) {
		return getSettings(context.getApplicationContext()).getBoolean(USE_PRIORITY_COLORS, true);
	}
	
	public static String getGlobalRingtone(Context context) {
		return getSettings(context.getApplicationContext()).getString(GLOBAL_RINGTONE, "");
	}

	public static boolean getUseByPrioritySound(Context context) {
		return getSettings(context).getBoolean(USE_BY_PRIORITY_RINGTONE, false);
	}
	public static String getByPriorityRingtone(Context context, int priority) {
		String entry;
		switch (priority) {
			case 3:
				entry = ALERT_PRIORITY_RINGTONE;
				break;
			case 2:
				entry = WARNING_PRIORITY_RINGTONE;
				break;
			case 1:
				entry = NORMAL_PRIORITY_RINGTONE;
				break;
			case 0:
			default:
				entry = NONE_PRIORITY_RINGTONE;
				break;
		}
		return getSettings(context.getApplicationContext()).getString(entry, "");
	}
	public static String getAlertRingtone(Context context) {
		return getSettings(context.getApplicationContext()).getString(ALERT_PRIORITY_RINGTONE, "");
	}

	public static String getWarningRingtone(Context context) {
		return getSettings(context.getApplicationContext()).getString(WARNING_PRIORITY_RINGTONE, "");
	}

	public static String getNormalRingtone(Context context) {
		return getSettings(context.getApplicationContext()).getString(NORMAL_PRIORITY_RINGTONE, "");
	}
	public static String getNoneRingtone(Context context) {
		return getSettings(context.getApplicationContext()).getString(NONE_PRIORITY_RINGTONE, "");
	}

	public static boolean getUseByPriorityVisibility(Context context) {
		return getSettings(context).getBoolean(USE_BY_PRIORITY_NOTIFICATION_VISIBILITY, false);
	}

	public static int getNotificationVisibility(Context context, int priority) {
		String entry = GLOBAL_NOTIFICATION_VISIBILITY;
		if (getSettings(context).getBoolean(USE_BY_PRIORITY_NOTIFICATION_VISIBILITY, false) == true) {
			switch (priority) {
				case 3:
					entry = ALERT_NOTIFICATION_VISIBILITY;
					break;
				case 2:
					entry = WARNING_NOTIFICATION_VISIBILITY;
					break;
				case 1:
					entry = NORMAL_NOTIFICATION_VISIBILITY;
					break;
				case 0:
				default:
					entry = NONE_NOTIFICATION_VISIBILITY;
					break;
			}
		}
		String value = getSettings(context.getApplicationContext()).getString(entry, "0");
		return string2Int(value, NotificationCompat.VISIBILITY_PRIVATE);
	}

	private static int string2Int(String value, int defaultValue) {
		int val;
		try {
			val = Integer.parseInt(value);
		} catch (NumberFormatException nfe) {
			val = defaultValue;//NotificationCompat.VISIBILITY_PRIVATE;
		}
		return val;
	}
	public static int getGlobalVisibility(Context context) {
		String value = getSettings(context.getApplicationContext()).getString(GLOBAL_NOTIFICATION_VISIBILITY, "0");
		return string2Int(value, NotificationCompat.VISIBILITY_PRIVATE);
	}

	public static int getAlertVisibility(Context context) {
		String value =  getSettings(context.getApplicationContext()).getString(ALERT_NOTIFICATION_VISIBILITY, "0");
		return string2Int(value, NotificationCompat.VISIBILITY_PRIVATE);
	}

	public static int getWarningVisibility(Context context) {
		String value =  getSettings(context.getApplicationContext()).getString(WARNING_NOTIFICATION_VISIBILITY, "0");
		return string2Int(value, NotificationCompat.VISIBILITY_PRIVATE);
	}

	public static int getNormalVisibility(Context context) {
		String value =  getSettings(context.getApplicationContext()).getString(NORMAL_NOTIFICATION_VISIBILITY, "0");
		return string2Int(value, NotificationCompat.VISIBILITY_PRIVATE);
	}

	public static int getNoneVisibility(Context context) {
		String value =  getSettings(context.getApplicationContext()).getString(NONE_NOTIFICATION_VISIBILITY, "0");
		return string2Int(value, NotificationCompat.VISIBILITY_PRIVATE);
	}



	public static String getOneMessageNotificationFormat(Context context) {
		return getSettings(context).getString(NOTIFICATION_ONE_FORMAT, "%s - %t \n%d\n%m");
	}
	
	
	public static String getNotificationFormat(Context context) {
		return getSettings(context).getString(NOTIFICATION_FORMAT, "%d : %s - %t : %m");
	}
	
	public static String getSpeakFormat(Context context) {
		return getSettings(context).getString(SPEAK_FORMAT, "%t. %m");
	}
	
	public static String getDelayReadingTime(Context context) {
		return getSettings(context).getString(DELAY_READING_TIME, "0");
	}
	
	public static String getShakeThreshold(Context context) {
		return getSettings(context).getString(SHAKE_THRESHOLD, "1500");
	}
	
	public static boolean getShakeToStop(Context context) {
		return getSettings(context).getBoolean(SHAKE_TO_STOP, false );
	}
	
	public static String getShakeWaitTime(Context context) {
		return getSettings(context).getString(SHAKE_WAIT_TIME, "60");
	}
	
	public static String getMaxLength(Context context) {
		return getSettings(context).getString(MAX_LENGTH, "0");
	}
	
	public static String getNotSpeakLength(Context context) {
		return getSettings(context).getString(NOT_SPEAK_LENGTH, "0");
	}
	

	public static String getTTSAudioStream(Context context) {
		return getSettings(context).getString(TTS_AUDIO_STREAM, "MUSIC");
	}
	
	public static String getNotificationAudioStream(Context context) {
		return getSettings(context).getString(NOTIFICATION_AUDIO_STREAM, "NOTIFICATION");
	}
	
	
	private static SharedPreferences getSettings(Context activity) {
		return PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext()); 
	}
	
	
////////////////////////////////////////////////////////////////////
/*	
	public static int getAppVersionCode(Context context) {
		try {
			PackageInfo pinfo = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0);
			return pinfo.versionCode;
		} catch (NameNotFoundException e) {
			Log.e(TAG, "unable to read version code", e);
		}
		return 0;
	}

	public static int getLatestVersionCode(Context context) {
		return getSettings(context).getInt(LATEST_VERSION_CODE, 0);
	}

	public static void saveLatestVersionCode(Context context, int latest) {
		SharedPreferences.Editor editor = getSettings(context).edit();
		editor.putInt(LATEST_VERSION_CODE, latest);
		editor.commit();
	}
*/
}
