package com.newtifry.pro3;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import com.newtifry.pro3.database.NewtifryMessage2;
import com.newtifry.pro3.locale.TaskerPlugin;
import com.newtifry.pro3.locale.ui.EventEditActivity;
import com.newtifry.pro3.shared.NewtifryProHelper;
import com.newtifry.pro3.utils.UniversalNotificationManager;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import static com.newtifry.pro3.CommonUtilities.ID_NOTIFICATION_SERVICE;

public class NewtificationService extends Service {
	
	protected static final Intent INTENT_REQUEST_REQUERY =
            new Intent(com.twofortyfouram.locale.Intent.ACTION_REQUEST_QUERY).putExtra(com.twofortyfouram.locale.Intent.EXTRA_ACTIVITY,
                                                                                       EventEditActivity.class.getName());
    private Intent cancelIntent;
    private PendingIntent cancelPendingIntent;
    private Intent seenAllIntent;
    private PendingIntent seenAllPendingIntent;
    private static UniversalNotificationManager wearNotification;
	private NotificationCompat.Action seenAllAction;
	private NotificationCompat.Action seenAllActionBlack;
	private Bitmap bigNotificationIcon;
	private Notification foregroundNotification;
	@Override
	public IBinder onBind( Intent arg0 ) {
		return null;
	}

    private static long sUndoTimeoutMillis = -1;

    /**
     * Registers a timeout for the undo notification such that when it expires, the undo bar will
     * disappear, and the action will be performed.
     */
    public static void registerUndoTimeout(final Context context, long messageId) {
    	if (sUndoTimeoutMillis == -1) {
            sUndoTimeoutMillis =
                    context.getResources().getInteger(R.integer.undo_notification_timeout);
        }

        final AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        final long triggerAtMills = SystemClock.elapsedRealtime() + sUndoTimeoutMillis;


	    Intent intent = new Intent(NewtifryProHelper.NOTIFICATION_CANCEL_UNDO)
			    .setClass(context, NotificationBroadcastReceiver.class);
	    intent.putExtra(NewtifryProHelper.IntentExtras.ID, messageId);
	    PendingIntent cancelUndoPendingIntent = PendingIntent.getBroadcast(context, 0, intent,
			    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME, triggerAtMills, cancelUndoPendingIntent);
    }

    /**
     * Cancels the undo timeout for a notification action. This should be called if the undo
     * notification is clicked (to prevent the action from being performed anyway) or cleared (since
     * we have already performed the action).
     */
    public static void cancelUndoTimeout(final Context context) {
        final AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

	    Intent intent = new Intent(NewtifryProHelper.NOTIFICATION_CANCEL_UNDO)
			    .setClass(context, NotificationBroadcastReceiver.class);
	    PendingIntent cancelUndoPendingIntent = PendingIntent.getBroadcast(context, 0, intent,
			    PendingIntent.FLAG_UPDATE_CURRENT| PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(cancelUndoPendingIntent);
    }

    /**
     * Creates a {@link PendingIntent} to be used for creating and canceling the undo timeout
     * alarm.
     */
    public static void createUndoNotification(Context context, long messageId) {
		final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CommonUtilities.getNotificationChannel(context, messageId))
		        .setLocalOnly(true);
        // Sets the small icon for the ticker
        builder.setSmallIcon(R.drawable.ic_stat_statusbar_newtifrypro2);
        builder.setWhen(System.currentTimeMillis());
        builder.setAutoCancel(true);

		// Cancel the notification when clicked
	    final Intent undoCancelIntent = new Intent(NewtifryProHelper.NOTIFICATION_CANCEL_UNDO)
			    .setClass(context, NotificationBroadcastReceiver.class);
	    undoCancelIntent.putExtra(NewtifryProHelper.IntentExtras.ID, messageId);
	    final PendingIntent cancelPendingIntent = PendingIntent.getBroadcast(context, 0, undoCancelIntent,
			    PendingIntent.FLAG_UPDATE_CURRENT| PendingIntent.FLAG_IMMUTABLE);
        builder.setDeleteIntent(cancelPendingIntent);

        // Inflate the notification layout as RemoteViews
        final RemoteViews undoView = new RemoteViews(context.getPackageName(), R.layout.undo_notification);

	    final Intent undoDeleteIntent = new Intent(NewtifryProHelper.NOTIFICATION_UNDO_DELETE)
			    .setClass(context, NotificationBroadcastReceiver.class);
		undoDeleteIntent.putExtra(NewtifryProHelper.IntentExtras.ID, messageId);
	    final PendingIntent undoPendingIntent = PendingIntent.getBroadcast(context, 0, undoDeleteIntent,
			    PendingIntent.FLAG_UPDATE_CURRENT| PendingIntent.FLAG_IMMUTABLE);

		undoView.setOnClickPendingIntent(R.id.notification_undo_delete_bar, undoPendingIntent);

        /* Workaround: Need to set the content view here directly on the notification.
         * NotificationCompatBuilder contains a bug that prevents this from working on platform
         * versions HoneyComb.
         * See https://code.google.com/p/android/issues/detail?id=30495
         */
	    builder.setContent(undoView);
	    NotificationManagerCompat.from(context).notify(UniversalNotificationManager.getMobileNotificationID(), builder.build());
	    registerUndoTimeout(context, messageId);
    }

    private boolean shouldNotifyMessage(int messagePriority, int messageNotify) {
		if (messageNotify == 1) {
			return true;
		}
		if (messageNotify == 0) {
			return false;
		}
    	if (messagePriority < 0) {
    		return false;
    	}
        return Preferences.getNotificationsEnable(this);
    }

    private boolean shouldSpeakMessage(NewtifryMessage2 message, int messagePriority, int speakMessage) {
		if (speakMessage == 0) {
			return false;
		}
		if (speakMessage == 1) {
			return true;
		}
    	if (messagePriority < 0) {
    		return false;
    	}

/*
		if (android.os.Build.VERSION.SDK_INT >= 23) {
			int interruptFilter = ((NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE)).getCurrentInterruptionFilter();
			if (interruptFilter == NotificationManager.INTERRUPTION_FILTER_ALARMS ||
					interruptFilter == NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
				return false;
			}
		}

		if (android.os.Build.VERSION.SDK_INT >= 21 && android.os.Build.VERSION.SDK_INT < 23) {
			int zenMode = Settings.Global.getInt(this.getContentResolver(), "zen_mode", CommonUtilities.ZENMODE_ALL);
			if (zenMode == CommonUtilities.ZENMODE_PRIORITY ||
					zenMode == CommonUtilities.ZENMODE_ALARMS) {
				return false;
			}
		}
*/
    	if (isQuietHour(messagePriority)) {
    		return false;
    	}
 		int noSpeakLength = 0;
		try {
			noSpeakLength = Integer.parseInt(Preferences.getNotSpeakLength(this));
		} catch (Exception ex) {
		}
		
		if (noSpeakLength != 0 && message.getTextMessage().length() > noSpeakLength) {
			return false;
		}

        return Preferences.getSpeakMessage(this) && Preferences.getSpeakByPriority(this, messagePriority);
    }

    @SuppressLint("InlinedApi")
	public void onCreate() {
		Log.d("DBG", "onCreate start");

		super.onCreate();
        seenAllIntent = new Intent(this, NotificationBroadcastReceiver.class);
        seenAllIntent.setAction(NewtifryProHelper.NOTIFICATION_SEEN_ALL);
        seenAllPendingIntent = PendingIntent.getBroadcast(this.getApplicationContext(), 0, seenAllIntent, PendingIntent.FLAG_IMMUTABLE);
        cancelIntent = new Intent(NewtifryProHelper.NOTIFICATION_CANCEL)
		        .setClass(this, NotificationBroadcastReceiver.class);
        cancelPendingIntent = PendingIntent.getBroadcast(this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
		int height = (int) this.getResources().getDimension(android.R.dimen.notification_large_icon_height);
		int width = (int) this.getResources().getDimension(android.R.dimen.notification_large_icon_width);
		this.bigNotificationIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(this.getResources(),R.drawable.ic_launcher), width, height, false);
        if (wearNotification == null) {
            wearNotification = UniversalNotificationManager.getInstance(this);
        }
		seenAllAction = new NotificationCompat.Action.Builder(R.drawable.ic_visibility_white_24dp , getString(R.string.notificationSeenAllLabel), seenAllPendingIntent).build();
		seenAllActionBlack = new NotificationCompat.Action.Builder(R.drawable.ic_visibility_black_24dp , getString(R.string.notificationSeenAllLabel), seenAllPendingIntent).build();

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(notificationManager) : "";
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
		foregroundNotification = notificationBuilder.setOngoing(true)
				.setSmallIcon(R.drawable.ic_stat_statusbar_newtifrypro2)
				.setPriority(NotificationCompat.PRIORITY_MIN)
				.setCategory(NotificationCompat.CATEGORY_SERVICE)
				.build();
		Log.d("DBG", "onCreate end");
	}
	@RequiresApi(Build.VERSION_CODES.O)
	private String createNotificationChannel(NotificationManager notificationManager){
		String channelId = "my_service_channelid";
		String channelName = "My Foreground Service";
		NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
		// omitted the LED color
		channel.setImportance(NotificationManager.IMPORTANCE_NONE);
		channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
		notificationManager.createNotificationChannel(channel);
		return channelId;
	}
    private PendingIntent getDeleteOnePendingIntent(long messageId) {
	    Intent intent = new Intent(NewtifryProHelper.NOTIFICATION_DELETE)
			    .setClass(this, NotificationBroadcastReceiver.class);
	    intent.putExtra(NewtifryProHelper.IntentExtras.ID, messageId);
	    return PendingIntent.getBroadcast(this, 0, intent,
			    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private PendingIntent getSeenOnePendingIntent(long messageId) {
	    Intent intent = new Intent(NewtifryProHelper.NOTIFICATION_SEEN)
			    .setClass(this, NotificationBroadcastReceiver.class);
	    intent.putExtra(NewtifryProHelper.IntentExtras.ID, messageId);
	    return PendingIntent.getBroadcast(this, 0, intent,
			    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

	private boolean isQuietHour(int _messagePriority ) {
		String messagePriority = String.valueOf(_messagePriority);
		Set<String> quietApplySet = Preferences.getQuietHoursPrioritiesApplication(this);

		if (Preferences.getQuietHoursEnabled(this) && quietApplySet.contains(messagePriority)) {
			String quietHourStart = Preferences.getQuietHoursStartString(this);
			String quietHourEnd = Preferences.getQuietHoursEndString(this);
			// take a calendar and current time
			Calendar calendarStart = buildCalendar(quietHourStart);
			Calendar calendarEnd = buildCalendar(quietHourEnd);
			Calendar currentCalendar = Calendar.getInstance();
			currentCalendar.setTimeInMillis(System.currentTimeMillis());
			
			if (currentCalendar.after(calendarStart)){
				if (calendarEnd.getTimeInMillis() < calendarStart.getTimeInMillis()) {
					calendarEnd.add(Calendar.DATE, 1);
				}
                return currentCalendar.before(calendarEnd);
			} else {
				if (calendarEnd.getTimeInMillis() < calendarStart.getTimeInMillis()) {
                    return currentCalendar.before(calendarEnd);
				}
			}
		}
		return false;
	}
	public String getOutputMessage(NewtifryMessage2 message) {
		String format = Preferences.getSpeakFormat(this);
		
		StringBuffer buffer = new StringBuffer(format);
		
		CommonUtilities.formatString(buffer, "%t", message.getTitle());
		CommonUtilities.formatString(buffer, "%m", message.getTextMessage());
		String source = message.getSourceName();
		if (source != null && !source.equals("")) {
			CommonUtilities.formatString(buffer, "%s", message.getSourceName());
		} else {
			CommonUtilities.formatString(buffer, "%s", "");
		}
		CommonUtilities.formatString(buffer, "%%", "%");
		try {
			int maxLength = Integer.parseInt(Preferences.getMaxLength(this));
			if (maxLength != 0) {
				return buffer.toString().substring(0, maxLength);
			}
		} catch (Exception ex) {
		}

		return buffer.toString();
	}

	public String getFormatedMessage(NewtifryMessage2 message, int newMessageCount) {
		String format;
		if (newMessageCount == 1) {
			format = Preferences.getOneMessageNotificationFormat(this);
		} else {
			format = Preferences.getNotificationFormat(this);
		}
		StringBuffer buffer = new StringBuffer(format);
		CommonUtilities.formatString(buffer, "%d", message.getDisplayTimestamp());
		CommonUtilities.formatString(buffer, "%t", message.getTitle());
		CommonUtilities.formatString(buffer, "%m", message.getTextMessage());
		String source = message.getSourceName();
		if (source != null && !source.equals("")) {
			CommonUtilities.formatString(buffer, "%s", source);
		} else {
			CommonUtilities.formatString(buffer, "%s", "");
		}
		CommonUtilities.formatString(buffer, "%%", "%");
		return buffer.toString();
	}
	
	
	private Calendar buildCalendar(String hour) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(System.currentTimeMillis());
		calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour.substring(0, 2)));
		calendar.set(Calendar.MINUTE, Integer.parseInt(hour.substring(3, 5)));
		return calendar;
	}
	private void displayOneMessage(long messageId) {
		NewtifryMessage2 message = NewtifryMessage2.get(this, messageId);
		NotificationCompat.Builder notification =
				new NotificationCompat.Builder(this, CommonUtilities.getNotificationChannel(this, messageId))
						.setCategory(NotificationCompat.CATEGORY_MESSAGE)
						.setSmallIcon(R.drawable.ic_stat_statusbar_newtifrypro2)
						.setOnlyAlertOnce(Preferences.getNotifyEverytime(this) ? false : true)
						.setWhen(System.currentTimeMillis())
						.setDeleteIntent(cancelPendingIntent)
						.setLargeIcon(bigNotificationIcon)
						.setLocalOnly(true)
						.setGroup(NewtifryProHelper.GROUP_KEY_MESSAGES)
						.setGroupSummary(false)
						.setContentInfo("1");
		notification.setVisibility(Preferences.getNotificationVisibility(this, message.getPriority()));
		if (Preferences.getMaxPriority(this)) {
			notification.setPriority(NotificationCompat.PRIORITY_MAX);
		} else {
			notification.setPriority(NotificationCompat.PRIORITY_DEFAULT);
		}

		NotificationCompat.Action deleteAction;
		NotificationCompat.Action seenAction;
		if (Preferences.getUseBlackActionIcons(this)) {
			deleteAction = new NotificationCompat.Action.Builder(R.drawable.ic_delete_black_24dp, getString(R.string.notificationDeleteLabel), getDeleteOnePendingIntent(messageId)).build();
			seenAction = new NotificationCompat.Action.Builder(R.drawable.ic_visibility_black_24dp, getString(R.string.notificationSeenLabel), getSeenOnePendingIntent(messageId)).build();
		} else {
			deleteAction = new NotificationCompat.Action.Builder(R.drawable.ic_delete_white_24dp, getString(R.string.notificationDeleteLabel), getDeleteOnePendingIntent(messageId)).build();
			seenAction = new NotificationCompat.Action.Builder(R.drawable.ic_visibility_white_24dp, getString(R.string.notificationSeenLabel), getSeenOnePendingIntent(messageId)).build();
		}
		notification.addAction(deleteAction);
		notification.addAction(seenAction);
		Intent notificationIntent;
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		notificationIntent = new Intent(this, NewtifryMessageDetailActivity.class);
		notificationIntent.putExtra(NewtifryMessageDetailFragment.ARG_ITEM_ID, messageId);
		stackBuilder.addParentStack(NewtifryMessageDetailActivity.class);
		stackBuilder.addNextIntent(notificationIntent);
		PendingIntent contentIntent = stackBuilder.getPendingIntent(0,  PendingIntent.FLAG_UPDATE_CURRENT| PendingIntent.FLAG_IMMUTABLE);
		notification.setContentIntent(contentIntent);
		notification.setContentTitle(getString(R.string.notificationFormatOne));
		String source = message.getSourceName();
		if (source != null && !source.equals("")) {
			notification.setContentText(source + " - " + message.getTitle());
		} else {
			notification.setContentText(message.getTitle());
		}
		NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
		bigTextStyle.bigText(message.getTextMessage());
		bigTextStyle.setSummaryText(getString(R.string.app_name));
		notification.setStyle(bigTextStyle);
		NotificationManagerCompat.from(this).notify(UniversalNotificationManager.getMobileNotificationID(), notification.build());
		if (CommonUtilities.isVersion(android.os.Build.VERSION_CODES.KITKAT)) {
			wearNotification.newMessage(message, false);
		}

	}

	@Override
	public int onStartCommand( Intent intent, int flags, int startId ) {
		startForeground(ID_NOTIFICATION_SERVICE, foregroundNotification);
		int result = super.onStartCommand(intent, flags, startId);
		Log.d("DBG", "onStartCommand");

		if( intent == null ) {
			Log.d("DBG", "onStartCommand : intent NULL");
			stopForeground(true);
			return result;
		}
		
		long messageId = intent.getLongExtra("messageId", -1);
		NewtifryMessage2 message = NewtifryMessage2.get(this, messageId);
		String action = intent.getAction();
		boolean updateNotification = action != null && action.equals("UPDATE");
        if (message == null && !updateNotification) {
			stopForeground(true);
			return result;
		}

		int messageSpeak = intent.getIntExtra("speak", -1);
		int messageNotify = intent.getIntExtra("notify", -1);
        boolean speak = false;
        int messagePriority = -1;
        if (!updateNotification) {
            messagePriority = message.getPriority();
            speak = shouldSpeakMessage(message, messagePriority, messageSpeak);
            if (speak) {
                CommonUtilities.speak(getBaseContext(), getOutputMessage(message));
            }
        }

        if (!updateNotification) {
            notifyExternalsAppz(message, messageNotify);
        }
		
		if(shouldNotifyMessage(messagePriority, messageNotify) || updateNotification) {
			NewtificationService.cancelUndoTimeout(this);
			int unreadMessages = NewtifryMessage2.countUnread(this);
			int newMessages = UniversalNotificationManager.getInstance(this).getNewMessagesCount();
	        if (newMessages == 0) {
				stopForeground(true);
                return result;
            }
			List<String> inboxStyleStringArray = new ArrayList<String>();
			String contentTitle;
			if (newMessages == 1) {
				contentTitle = getString(R.string.notificationFormatOne);
				/*
				displayOneMessage(messageId);
				return;
				*/
			} else {
				contentTitle = String.format(getString(R.string.notificationFormat), newMessages);
			}
			int limit = newMessages;
			String lastString = null;
			if (newMessages > 5) {
				limit = 4; // keep one line for the 'X more messages'
				lastString = String.format(getString(R.string.notificationXMoreMessages), newMessages - 4);
			}
			ArrayList<NewtifryMessage2> list = NewtifryMessage2.getUnreadMessages(this, limit);
			for (NewtifryMessage2 msg : list) {
				inboxStyleStringArray.add(getFormatedMessage(msg, newMessages));
			}
			if (lastString != null) {
				inboxStyleStringArray.add(lastString);
			}

			NotificationCompat.Builder notification =
			        new NotificationCompat.Builder(this, CommonUtilities.getNotificationChannel(this, messageId))
						.setSmallIcon(R.drawable.ic_stat_statusbar_newtifrypro2)
						.setOnlyAlertOnce(Preferences.getNotifyEverytime(this) ? false : true)
	//       				.setOngoing(true)  // for sticky notification
						.setWhen(System.currentTimeMillis())
	//			        .setAutoCancel(true)
						.setDeleteIntent(cancelPendingIntent)
						.setLargeIcon(bigNotificationIcon)
				        .setLocalOnly(true)
						.setGroup(NewtifryProHelper.GROUP_KEY_MESSAGES)
						.setGroupSummary(true)
						.setCategory(NotificationCompat.CATEGORY_MESSAGE)
						.setContentInfo(Integer.toString(unreadMessages));
			notification.setVisibility(Preferences.getNotificationVisibility(this, messagePriority));
			//notification.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
			if (Preferences.getMaxPriority(this)) {
				notification.setPriority(NotificationCompat.PRIORITY_MAX);
			} else {
				notification.setPriority(NotificationCompat.PRIORITY_DEFAULT);
			}
			int defaults = 0;

            if(newMessages == 1 && updateNotification) {
                // TODO : we have to get the last received messageId
                // and update the message value
                messageId = NewtifryMessage2.getLastMessageId(this);
                message = NewtifryMessage2.get(this, messageId);
            }

            if(newMessages == 1) {
				NotificationCompat.Action deleteAction;
				NotificationCompat.Action seenAction;
				if (Preferences.getUseBlackActionIcons(this)) {
					deleteAction = new NotificationCompat.Action.Builder(R.drawable.ic_delete_black_24dp, getString(R.string.notificationDeleteLabel), getDeleteOnePendingIntent(messageId)).build();
					seenAction = new NotificationCompat.Action.Builder(R.drawable.ic_visibility_black_24dp, getString(R.string.notificationSeenLabel), getSeenOnePendingIntent(messageId)).build();
				} else {
					deleteAction = new NotificationCompat.Action.Builder(R.drawable.ic_delete_white_24dp, getString(R.string.notificationDeleteLabel), getDeleteOnePendingIntent(messageId)).build();
					seenAction = new NotificationCompat.Action.Builder(R.drawable.ic_visibility_white_24dp, getString(R.string.notificationSeenLabel), getSeenOnePendingIntent(messageId)).build();
				}
				notification.addAction(deleteAction);
				notification.addAction(seenAction);
			} else {
				if (Preferences.getUseBlackActionIcons(this)) {
					notification.addAction(seenAllActionBlack);
				} else {
					notification.addAction(seenAllAction);
				}
			}

			Intent notificationIntent;
			TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
			if(newMessages == 1) {
				notificationIntent = new Intent(this, NewtifryMessageDetailActivity.class);
				notificationIntent.putExtra(NewtifryMessageDetailFragment.ARG_ITEM_ID, messageId);
				stackBuilder.addParentStack(NewtifryMessageDetailActivity.class);
			} else {
				notificationIntent = new Intent(this, NewtifryMessageListActivity.class);
				stackBuilder.addParentStack(NewtifryMessageListActivity.class);
			}
			stackBuilder.addNextIntent(notificationIntent);
			PendingIntent contentIntent = stackBuilder.getPendingIntent(0,  PendingIntent.FLAG_UPDATE_CURRENT| PendingIntent.FLAG_IMMUTABLE);
			notification.setContentIntent(contentIntent);

			if( Preferences.getLedFlash(this)) {
				//notification.setLights( 0xff00ff00, 300, 1000);
				defaults |= Notification.DEFAULT_LIGHTS;
			}
            boolean vibrate = false;
			boolean  noVibrate = intent.getBooleanExtra("novibrate", false);
			if (!updateNotification && !isQuietHour(messagePriority)) {  // dont vibrate on notification update
				if( !noVibrate && Preferences.getVibrateNotify(this) ) {
					/*
					long[] vibratePattern = { 0, 500, 250, 500 };
					notification.setVibrate(vibratePattern);
					*/
					defaults |= Notification.DEFAULT_VIBRATE;
                    vibrate = true;
				}
				if(!speak) {
					String tone;
					if (Preferences.getUseByPrioritySound(this)) {
						tone = Preferences.getByPriorityRingtone(this, messagePriority);
					} else {
						tone = Preferences.getGlobalRingtone(this);
					}
					if (!tone.equals("")) {
						notification.setSound(Uri.parse(tone), CommonUtilities.getAudioStream(this, false));
					}
				}
			}
			notification.setContentTitle(contentTitle);
			if (newMessages == 1) {
				String source = message.getSourceName();
				if (source != null && !source.equals("")) {
					notification.setContentText(source + " - " + message.getTitle());
				} else {
					notification.setContentText(message.getTitle());
				}
				NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
				bigTextStyle.bigText(inboxStyleStringArray.get(0));
				bigTextStyle.setSummaryText(getString(R.string.app_name));
				notification.setStyle(bigTextStyle);
			} else {
				notification.setContentText(getString(R.string.app_name));
				NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
				for (String str : inboxStyleStringArray) {
					inboxStyle.addLine(str);
				}
				inboxStyle.setSummaryText(getString(R.string.app_name));
				notification.setStyle(inboxStyle);
			}

			notification.setDefaults(defaults);
			Notification notif = notification.build();
			NotificationManagerCompat.from(this).notify(UniversalNotificationManager.getMobileNotificationID(), notif);
			if (CommonUtilities.isVersion(android.os.Build.VERSION_CODES.KITKAT) && !updateNotification && Build.VERSION.SDK_INT < 26 /* Oreo */) {
                wearNotification.newMessage(message, vibrate);
            }
		}
		stopForeground(true);
		return result;
	}
	
	private void notifyExternalsAppz(NewtifryMessage2 message, int notify) {
		// for SmartWatch 2
		String source = message.getSourceName();
		String messageContent = message.getTextMessage();
		String title = message.getTitle();
		int priority = message.getPriority();
		int imageCount = message.getImageCount();
		String[] imgArray = message.getImageList();
		String url = message.getUrl();
		if (shouldNotifyMessage(priority, notify)) {
			Intent smartwatchIntent = new Intent(NewtifryProHelper.MESSAGE_NEW);
			smartwatchIntent.putExtra(NewtifryProHelper.IntentExtras.ID,message.getId());
			if (source != null && !source.equals("")) {
				smartwatchIntent.putExtra(NewtifryProHelper.IntentExtras.SOURCE,source);
			}
			smartwatchIntent.putExtra(NewtifryProHelper.IntentExtras.TITLE, title);
			smartwatchIntent.putExtra(NewtifryProHelper.IntentExtras.MESSAGE, messageContent);
			smartwatchIntent.putExtra(NewtifryProHelper.IntentExtras.TIMESTAMP,message.getDisplayTimestamp());
			smartwatchIntent.putExtra(NewtifryProHelper.IntentExtras.PRIORITY, priority);
			smartwatchIntent.putExtra(NewtifryProHelper.IntentExtras.URL,url);
			smartwatchIntent.putExtra(NewtifryProHelper.IntentExtras.IMAGE_COUNT, imageCount);
			smartwatchIntent.putExtra(NewtifryProHelper.IntentExtras.IMAGE_LIST, imgArray);
			getBaseContext().sendBroadcast(smartwatchIntent);
		}
		// for tasker
		// build a bundle with data
		Bundle bundleVars = new Bundle();
		if (source != null && !source.equals("")) {
			bundleVars.putString("%npsource", source);
		} else {
			bundleVars.putString("%npsource", "No source");
		}
		bundleVars.putString("%nptitle", title);
		if (messageContent != null && !messageContent.equals("")) {
			bundleVars.putString("%npmessage", messageContent);
		} else {
			bundleVars.putString("%npmessage", "No message");
		}
		bundleVars.putString("%nppriority", Integer.toString(priority));
		if (url != null && !url.equals("")) {
			bundleVars.putString("%npurl", url);
		} else {
			bundleVars.putString("%npurl", "No url");
		}
		
		bundleVars.putString("%npimgcount", Integer.toString(imageCount));
		if (imgArray != null) {
			for (int i = 0; i < imageCount; i++) {
				bundleVars.putString("%npimg" + (i + 1), imgArray[i]);
			}
			for (int i = imageCount -1; i < 5; i++) {
				bundleVars.putString("%npimg" + (i + 1), "no image");
			}
		}
		
		TaskerPlugin.Event.addPassThroughData(INTENT_REQUEST_REQUERY, bundleVars);
		getBaseContext().sendBroadcast(INTENT_REQUEST_REQUERY);
	}
}