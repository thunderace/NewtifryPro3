package com.newtifry.pro3.utils;

import static androidx.appcompat.graphics.drawable.DrawableContainerCompat.Api21Impl.getResources;
import static com.newtifry.pro3.CommonUtilities.ID_NOTIFICATION_SERVICE;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;

import com.newtifry.pro3.CommonUtilities;
import com.newtifry.pro3.NewtificationService;
import com.newtifry.pro3.NewtifryMessageDetailActivity;
import com.newtifry.pro3.NewtifryMessageDetailFragment;
import com.newtifry.pro3.NewtifryMessageListActivity;
import com.newtifry.pro3.NotificationBroadcastReceiver;
import com.newtifry.pro3.Preferences;
import com.newtifry.pro3.R;
import com.newtifry.pro3.database.NewtifryMessage2;
import com.newtifry.pro3.shared.NewtifryProHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class UniversalNotificationManager {
    private final int maxStackedWearNotificationCount =  20;
    private final static int BASE_WEAR_NOTIFICATION_ID = 2;
    private final static int MOBILE_NOTIFICATION_ID = 8;
    private final static Object lock = new Object();
    private static UniversalNotificationManager sInstance = null;
    private final Context mContext;
    private final Bitmap background;

	private int newMessageCount = 0;
    private Intent cancelIntent;
    private PendingIntent cancelPendingIntent;
    private Intent seenAllIntent;
    private PendingIntent seenAllPendingIntent;

    public  int getNewMessagesCount() {
		return newMessageCount;
	}
    public  int decreaseNewMessagesCount(int count) {
        newMessageCount -= count;
        if (newMessageCount == 0 || newMessageCount < 0) {
            resetNewMessagesCount();
        }
        return newMessageCount;
    }
	public  int decreaseNewMessagesCount() {
		newMessageCount--;
		if (newMessageCount == 0 || newMessageCount < 0) {
			resetNewMessagesCount();
		}
		return newMessageCount;
	}
	public void resetNewMessagesCountFromUndo() {
		newMessageCount = 0;
		//cancelAllNotifications();
	}

	public void resetNewMessagesCount() {
		newMessageCount = 0;
		// remove mobile notification
		cancelAllNotifications();
	}
	public void incNewMessagesCount() {
		newMessageCount++;
	}
    private Calendar buildCalendar(String hour) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour.substring(0, 2)));
        calendar.set(Calendar.MINUTE, Integer.parseInt(hour.substring(3, 5)));
        return calendar;
    }
    private boolean isQuietHour(int _messagePriority ) {
        String messagePriority = String.valueOf(_messagePriority);
        Set<String> quietApplySet = Preferences.getQuietHoursPrioritiesApplication(mContext);

        if (Preferences.getQuietHoursEnabled(mContext) && quietApplySet.contains(messagePriority)) {
            String quietHourStart = Preferences.getQuietHoursStartString(mContext);
            String quietHourEnd = Preferences.getQuietHoursEndString(mContext);
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
        if (isQuietHour(messagePriority)) {
            return false;
        }
        int noSpeakLength = 0;
        try {
            noSpeakLength = Integer.parseInt(Preferences.getNotSpeakLength(mContext));
        } catch (Exception ex) {
        }

        if (noSpeakLength != 0 && message.getTextMessage().length() > noSpeakLength) {
            return false;
        }

        return Preferences.getSpeakMessage(mContext) && Preferences.getSpeakByPriority(mContext, messagePriority);
    }

    public String getOutputMessage(NewtifryMessage2 message) {
        String format = Preferences.getSpeakFormat(mContext);

        StringBuffer buffer = new StringBuffer(format);
        CommonUtilities.formatString(buffer, "%t", message.getTitle());
        CommonUtilities.formatString(buffer, "%m", message.getTextMessage());
        CommonUtilities.formatString(buffer, "%s", message.getSourceName());
        CommonUtilities.formatString(buffer, "%%", "%");
        try {
            int maxLength = Integer.parseInt(Preferences.getMaxLength(mContext));
            if (maxLength != 0) {
                return buffer.toString().substring(0, maxLength);
            }
        } catch (Exception ex) {
        }
        return buffer.toString();
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
        return Preferences.getNotificationsEnable(mContext);
    }
    public String getFormatedMessage(NewtifryMessage2 message, int newMessageCount) {
        String format;
        if (newMessageCount == 1) {
            format = Preferences.getOneMessageNotificationFormat(mContext);
        } else {
            format = Preferences.getNotificationFormat(mContext);
        }
        StringBuffer buffer = new StringBuffer(format);
        CommonUtilities.formatString(buffer, "%d", message.getDisplayTimestamp());
        CommonUtilities.formatString(buffer, "%t", message.getTitle());
        CommonUtilities.formatString(buffer, "%m", message.getTextMessage());
        String source = message.getSourceName();
        if (source != null && !source.isEmpty()) {
            CommonUtilities.formatString(buffer, "%s", source);
        } else {
            CommonUtilities.formatString(buffer, "%s", "");
        }
        CommonUtilities.formatString(buffer, "%%", "%");
        return buffer.toString();
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

	public void createNotification(Context context, long messageId, int messageSpeak, int messageNotify) {
        if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NewtifryMessage2 message = NewtifryMessage2.get(mContext, messageId);
        boolean speak = shouldSpeakMessage(message, message.getPriority(), messageSpeak);
        int messagePriority = message.getPriority();
        if (speak) {
            CommonUtilities.speak(mContext, getOutputMessage(message));
        }

        if(shouldNotifyMessage(messagePriority, messageNotify)) {
            NewtificationService.cancelUndoTimeout(mContext);
            int unreadMessages = NewtifryMessage2.countUnread(mContext);
            int newMessages = UniversalNotificationManager.getInstance(mContext).getNewMessagesCount();
            if (newMessages == 0) {
                return;
            }
            List<String> inboxStyleStringArray = new ArrayList<String>();
            String contentTitle;
            if (newMessages == 1) {
                contentTitle = mContext.getString(R.string.notificationFormatOne);
				/*
				displayOneMessage(messageId);
				return;
				*/
            } else {
                contentTitle = String.format(mContext.getString(R.string.notificationFormat), newMessages);
            }
            int limit = newMessages;
            String lastString = null;
            if (newMessages > 5) {
                limit = 4; // keep one line for the 'X more messages'
                lastString = String.format(mContext.getString(R.string.notificationXMoreMessages), newMessages - 4);
            }
            ArrayList<NewtifryMessage2> list = NewtifryMessage2.getUnreadMessages(mContext, limit);
            for (NewtifryMessage2 msg : list) {
                inboxStyleStringArray.add(getFormatedMessage(msg, newMessages));
            }
            if (lastString != null) {
                inboxStyleStringArray.add(lastString);
            }

            NotificationCompat.Builder notification =
                    new NotificationCompat.Builder(mContext, CommonUtilities.getNotificationChannel(mContext, messageId))
                            .setSmallIcon(R.drawable.ic_stat_statusbar_newtifrypro2)
                            .setOnlyAlertOnce(Preferences.getNotifyEverytime(mContext) ? false : true)
                            //       				.setOngoing(true)  // for sticky notification
                            .setWhen(System.currentTimeMillis())
                            //			        .setAutoCancel(true)
                            .setDeleteIntent(cancelPendingIntent)
                            .setLargeIcon(bigNotificationIcon)
                            .setLocalOnly(true)
                            .setGroup(NewtifryProHelper.GROUP_KEY_MESSAGES)
                            .setGroupSummary(true)
                            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                            .setNumber(newMessages)
                            .setContentInfo(Integer.toString(unreadMessages));
            notification.setVisibility(Preferences.getNotificationVisibility(mContext, messagePriority));
            //notification.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            if (Preferences.getMaxPriority(mContext)) {
                notification.setPriority(NotificationCompat.PRIORITY_MAX);
            } else {
                notification.setPriority(NotificationCompat.PRIORITY_DEFAULT);
            }
            int defaults = 0;

            if(newMessages == 1) {
                NotificationCompat.Action deleteAction;
                NotificationCompat.Action seenAction;
                if (Preferences.getUseBlackActionIcons(mContext)) {
                    deleteAction = new NotificationCompat.Action.Builder(R.drawable.ic_delete_black_24dp, mContext.getString(R.string.notificationDeleteLabel), getDeleteOnePendingIntent(messageId)).build();
                    seenAction = new NotificationCompat.Action.Builder(R.drawable.ic_visibility_black_24dp, mContext.getString(R.string.notificationSeenLabel), getSeenOnePendingIntent(messageId)).build();
                } else {
                    deleteAction = new NotificationCompat.Action.Builder(R.drawable.ic_delete_white_24dp, mContext.getString(R.string.notificationDeleteLabel), getDeleteOnePendingIntent(messageId)).build();
                    seenAction = new NotificationCompat.Action.Builder(R.drawable.ic_visibility_white_24dp, mContext.getString(R.string.notificationSeenLabel), getSeenOnePendingIntent(messageId)).build();
                }
                notification.addAction(deleteAction);
                notification.addAction(seenAction);
            } else {
                if (Preferences.getUseBlackActionIcons(mContext)) {
                    notification.addAction(seenAllActionBlack);
                } else {
                    notification.addAction(seenAllAction);
                }
            }

            Intent notificationIntent;
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
            if(newMessages == 1) {
                notificationIntent = new Intent(mContext, NewtifryMessageDetailActivity.class);
                notificationIntent.putExtra(NewtifryMessageDetailFragment.ARG_ITEM_ID, messageId);
                stackBuilder.addParentStack(NewtifryMessageDetailActivity.class);
            } else {
                notificationIntent = new Intent(mContext, NewtifryMessageListActivity.class);
                stackBuilder.addParentStack(NewtifryMessageListActivity.class);
            }
            stackBuilder.addNextIntent(notificationIntent);
            PendingIntent contentIntent = stackBuilder.getPendingIntent(0,  PendingIntent.FLAG_UPDATE_CURRENT| PendingIntent.FLAG_IMMUTABLE);
            notification.setContentIntent(contentIntent);

            if( Preferences.getLedFlash(mContext)) {
                //notification.setLights( 0xff00ff00, 300, 1000);
                defaults |= Notification.DEFAULT_LIGHTS;
            }
            boolean vibrate = false;
            boolean  noVibrate = intent.getBooleanExtra("novibrate", false);
            if (!isQuietHour(messagePriority)) {  // dont vibrate on notification update
                if( !noVibrate && Preferences.getVibrateNotify(mContext) ) {
					/*
					long[] vibratePattern = { 0, 500, 250, 500 };
					notification.setVibrate(vibratePattern);
					*/
                    defaults |= Notification.DEFAULT_VIBRATE;
                    vibrate = true;
                }
                if(!speak) {
                    String tone;
                    if (Preferences.getUseByPrioritySound(mContext)) {
                        tone = Preferences.getByPriorityRingtone(mContext, messagePriority);
                    } else {
                        tone = Preferences.getGlobalRingtone(mContext);
                    }
                    if (!tone.equals("")) {
                        notification.setSound(Uri.parse(tone), CommonUtilities.getAudioStream(mContext, false));
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
                bigTextStyle.setSummaryText(mContext.getString(R.string.app_name));
                notification.setStyle(bigTextStyle);
            } else {
                notification.setContentText(mContext.getString(R.string.app_name));
                NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
                for (String str : inboxStyleStringArray) {
                    inboxStyle.addLine(str);
                }
                inboxStyle.setSummaryText(mContext.getString(R.string.app_name));
                notification.setStyle(inboxStyle);
            }

            notification.setDefaults(defaults);
            Notification notif = notification.build();
            NotificationManagerCompat.from(mContext).notify(UniversalNotificationManager.getMobileNotificationID(), notif);
        }



        /*
		Intent intentData = new Intent(context, NewtificationService.class);
		intentData.putExtra("messageId", messageId);
		intentData.putExtra("speak", speak);
        intentData.putExtra("notify", notify);
        Log.d("DBG", "from createNotification");
        ContextCompat.startForegroundService(context,intentData);
*/
	}

    public static void reSendNotification(Context context, long messageId) {
        Intent intentData = new Intent(context, NewtificationService.class);
        intentData.putExtra("messageId", messageId);
        intentData.putExtra("speak", -1);
        intentData.putExtra("notify", 1);
        intentData.putExtra("novibrate", true);
        Log.d("DBG", "from reSendNotification");

        ContextCompat.startForegroundService(context,intentData);
    }

    public static void updateNotification(Context context) {
		Intent intentData = new Intent(context, NewtificationService.class);
		intentData.setAction("UPDATE");
        Log.d("DBG", "from updateNotification");
        ContextCompat.startForegroundService(context,intentData);
	}


	private static final LinkedList<Integer> freeWearNotificationId = new LinkedList<Integer>();
	private static final LinkedList<Integer> usedWearNotificationId = new LinkedList<Integer>();
    public static UniversalNotificationManager getInstance(Context context) {
        if (sInstance == null) {
            // When storing a reference to a context, use the application context.
            // Never store the context itself, which could be a component.
            sInstance = new UniversalNotificationManager(context.getApplicationContext());
            sInstance.createNotificationChannel();
        }
        return sInstance;
    }

    public static int getMobileNotificationID() {
        return MOBILE_NOTIFICATION_ID;
    }


    private UniversalNotificationManager(Context context) {
	    mContext = context;
        initWearNotificationIdList();
        background = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
        newMessageCount = 0;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = mContext.getString(R.string.alert_notification_channel_name);
            NotificationChannel channel = new NotificationChannel(CommonUtilities.ALERT_NOTIFICATION_CHANNEL, name, NotificationManager.IMPORTANCE_HIGH);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = mContext.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            name = mContext.getString(R.string.warning_notification_channel_name);
            channel = new NotificationChannel(CommonUtilities.WARNING_NOTIFICATION_CHANNEL, name, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);

            name = mContext.getString(R.string.information_notification_channel_name);
            channel = new NotificationChannel(CommonUtilities.INFORMATION_NOTIFICATION_CHANNEL, name, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);

            name = mContext.getString(R.string.no_priority_notification_channel_name);
            channel = new NotificationChannel(CommonUtilities.NO_PRIORITY_NOTIFICATION_CHANNEL, name, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
        seenAllIntent = new Intent(this, NotificationBroadcastReceiver.class);
        seenAllIntent.setAction(NewtifryProHelper.NOTIFICATION_SEEN_ALL);
        seenAllPendingIntent = PendingIntent.getBroadcast(this.mContext, 0, seenAllIntent, PendingIntent.FLAG_IMMUTABLE);
        cancelIntent = new Intent(NewtifryProHelper.NOTIFICATION_CANCEL)
                .setClass(this, NotificationBroadcastReceiver.class);
        cancelPendingIntent = PendingIntent.getBroadcast(this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        int height = (int) getResources().getDimension(android.R.dimen.notification_large_icon_height);
        int width = (int) getResources().getDimension(android.R.dimen.notification_large_icon_width);
        this.bigNotificationIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(this.getResources(),R.drawable.ic_launcher), width, height, false);
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

    }

    private void initWearNotificationIdList() {
        synchronized (lock) {
            NotificationManagerCompat.from(mContext).cancelAll();
            freeWearNotificationId.clear();
            usedWearNotificationId.clear();
            for (int i = 0; i < maxStackedWearNotificationCount; i++) {
                NotificationManagerCompat.from(mContext).cancel(i + BASE_WEAR_NOTIFICATION_ID);
                freeWearNotificationId.add(i);
            }
        }
    }
/*
    public void setMaxWearNotificationCount(int maxCount) {
        if (maxCount != maxStackedWearNotificationCount) {
            maxStackedWearNotificationCount = maxCount;
            initWearNotificationIdList();
        }
    }
*/
    private PendingIntent getWearShowPendingIntent(Context context, int notificationId, long messageId) {
        Intent intent = new Intent(NewtifryProHelper.MESSAGE_SHOW)
                .setClass(context, NotificationBroadcastReceiver.class);
        intent.putExtra(NewtifryProHelper.IntentExtras.FROM_WEAR, true);
        intent.putExtra(NewtifryProHelper.IntentExtras.WEAR_NOTIFICATION_ID, notificationId);
        intent.putExtra(NewtifryProHelper.IntentExtras.ID, messageId);
        return PendingIntent.getBroadcast(context, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
    private PendingIntent getWearSeenPendingIntent(Context context, int notificationId, long messageId) {
        Intent intent = new Intent(NewtifryProHelper.NOTIFICATION_SEEN)
                .setClass(context, NotificationBroadcastReceiver.class);
        intent.putExtra(NewtifryProHelper.IntentExtras.FROM_WEAR, true);
        intent.putExtra(NewtifryProHelper.IntentExtras.WEAR_NOTIFICATION_ID, notificationId);
	    intent.putExtra(NewtifryProHelper.IntentExtras.ID, messageId);
        return PendingIntent.getBroadcast(context, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

//    private PendingIntent getSeenPendingIntent(Context context, int notificationId, long messageId) {
//        Intent intent = new Intent(NewtifryProHelper.NOTIFICATION_SEEN)
//                .setClass(context, NotificationBroadcastReceiver.class);
//        intent.putExtra(NewtifryProHelper.IntentExtras.WEAR_NOTIFICATION_ID, notificationId);
//        intent.putExtra(NewtifryProHelper.IntentExtras.ID, messageId);
//        return PendingIntent.getBroadcast(context, notificationId, intent,
//                PendingIntent.FLAG_UPDATE_CURRENT);
//    }

//    private PendingIntent getDeletePendingIntent(Context context, int notificationId, long messageId) {
//        Intent intent = new Intent(NewtifryProHelper.WEAR_MESSAGE_DELETE)
//                .setClass(context, NotificationBroadcastReceiver.class);
//        intent.putExtra(NewtifryProHelper.IntentExtras.WEAR_NOTIFICATION_ID, notificationId);
//        intent.putExtra(NewtifryProHelper.IntentExtras.ID, messageId);
//        return PendingIntent.getBroadcast(context, notificationId, intent,
//                PendingIntent.FLAG_UPDATE_CURRENT);
//    }

	private PendingIntent getWearCancelPendingIntent(Context context, int notificationId, long messageId) {
		Intent intent = new Intent(NewtifryProHelper.WEAR_NOTIFICATION_CANCEL)
				.setClass(context, NotificationBroadcastReceiver.class);
        intent.putExtra(NewtifryProHelper.IntentExtras.FROM_WEAR, true);
        intent.putExtra(NewtifryProHelper.IntentExtras.WEAR_NOTIFICATION_ID, notificationId);
		intent.putExtra(NewtifryProHelper.IntentExtras.ID, messageId);
		return PendingIntent.getBroadcast(context, notificationId, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
	}

	private PendingIntent getWearDeletePendingIntent(Context context, int notificationId, long messageId) {
		Intent intent = new Intent(NewtifryProHelper.WEAR_MESSAGE_DELETE)
				.setClass(context, NotificationBroadcastReceiver.class);
        intent.putExtra(NewtifryProHelper.IntentExtras.FROM_WEAR, true);
        intent.putExtra(NewtifryProHelper.IntentExtras.WEAR_NOTIFICATION_ID, notificationId);
		intent.putExtra(NewtifryProHelper.IntentExtras.ID, messageId);
		return PendingIntent.getBroadcast(context, notificationId, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
	}

	private void cancelAllWearNotifications() {
        initWearNotificationIdList();
    }


	private void cancelMobileNotification() {
        NotificationManagerCompat.from(mContext).cancel(MOBILE_NOTIFICATION_ID);
    }


    private void cancelAllNotifications() {
	    // always remove the mobile notification : it will remove wear notifications
	    cancelMobileNotification();
	    initWearNotificationIdList();
    }

	public void cancelWearNotification(int id) {
        if (!isValidNotificationId(id)) {
            return;
        }
		if (decreaseNewMessagesCount() != 0) {
			updateNotification(mContext);
		}
		NotificationManagerCompat.from(mContext).cancel(id + BASE_WEAR_NOTIFICATION_ID);
        releaseWearNotificationId(id);
	}

    private boolean isValidNotificationId(int notificationId) {
        //return true;
        //return usedWearNotificationId.contains((Integer)notificationId);
        return notificationId >= 0 && notificationId < maxStackedWearNotificationCount;
    }

	private void releaseWearNotificationId(int id) {
        synchronized (lock) {
            if (!isValidNotificationId(id)) {
                return;
            }
            // remove from used id
            usedWearNotificationId.remove((Integer) id);
            //add on top of the free list
            freeWearNotificationId.addLast(id);
        }
	}

	private int getNextWearNotificationId() {
        synchronized (lock) {
            int id;
            if (freeWearNotificationId.size() == 0) {
                // no more id : get the oldest one in the used list
                 id = usedWearNotificationId.removeFirst();
                // put again in the end of the used list
            } else {
                id = freeWearNotificationId.removeFirst();
            }
            usedWearNotificationId.add(id);
            return id;
        }
	}

    private SpannableString getColoredMessageHeader(NewtifryMessage2 msg) {
        String source = msg.getSourceName();
        String title = msg.getTitle();
        String str;
        if (source != null && source.length() != 0) {
            str = String.format("%s-%s", source, title);
        } else {
            str = title;
        }
        final SpannableString spannableString;
        spannableString = new SpannableString(str);
	    int priority = msg.getPriority();
        if (Preferences.getUsePriorityColor(mContext) &&  priority > 0 ) {
            int bg = -1;
            switch (priority) {
                case 1:
                    bg = Preferences.getInfoTitleColor(mContext);
                    break;
                case 2:
                    bg = Preferences.getWarningTitleColor(mContext);
                    break;
                case 3:
                    bg = Preferences.getAlertTitleColor(mContext);
                    break;
            }
            spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, str.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableString.setSpan(new ForegroundColorSpan(bg), 0, str.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return spannableString;
    }
	
    private void appendStyled(SpannableStringBuilder builder, String str, Object... spans) {
		builder.append(str);
        for (Object span : spans) {
            builder.setSpan(span, builder.length() - str.length(), builder.length(), 0);
        }
    }

    private SpannableStringBuilder getNotificationContent(NewtifryMessage2 msg) {
        // timestamp
        // url
        // number of images
        // text
		SpannableStringBuilder content = new SpannableStringBuilder(msg.getDisplayTimestamp());
        content.append("\n");
        String url = msg.getUrl();
        if (url != null && !url.isEmpty()) {
			appendStyled(content, url, new ForegroundColorSpan(Color.BLUE));
            content.append("\n");
        }
        int imgCount = msg.getImageCount();
        if (imgCount != 0) {
            String template = mContext.getString(R.string.wear_notification_images_count);
            String plural = "s";
            if (imgCount == 1) {
                plural = "";
            }
			appendStyled(content, String.format(template, imgCount, plural), new ForegroundColorSpan(Color.GREEN));
        }
        String text = msg.getTextMessage();
        if (text != null) {
            content.append("\n");
            content.append(msg.getSpannedMessage());
        }
        return content;
    }

    public void newMessage(NewtifryMessage2 message, boolean vibrate) {
        // Separate notifications that will be visible on the watch
        int notifId = getNextWearNotificationId();
        long messageId = message.getId();
        boolean useBlackActionIcons = Preferences.getUseBlackActionIcons(mContext);
        NotificationCompat.WearableExtender wearableExtender =
                new NotificationCompat.WearableExtender()
		                .setBackground(background);
	    NotificationCompat.Action actionSeen =
			    new NotificationCompat.Action.Builder(R.drawable.ic_visibility_white_24dp,
                        mContext.getString(R.string.notificationSeenLabel), getWearSeenPendingIntent(mContext, notifId, messageId))
					    .build();
//        NotificationCompat.Action actionSeenMobile =
//                new NotificationCompat.Action.Builder(useBlackActionIcons == true ? R.drawable.ic_visibility_white_24dp : R.drawable.ic_visibility_white_24dp,
//                        mContext.getString(R.string.notificationSeenLabel), getSeenPendingIntent(mContext, notifId, messageId))
//                        .build();

        NotificationCompat.Action actionDelete =
			    new NotificationCompat.Action.Builder(R.drawable.ic_delete_white_24dp,
                        mContext.getString(R.string.notificationDeleteLabel), getWearDeletePendingIntent(mContext, notifId,  messageId))
					    .build();
//        NotificationCompat.Action actionDeleteMobile =
//                new NotificationCompat.Action.Builder(useBlackActionIcons == true ? R.drawable.ic_delete_white_24dp : R.drawable.ic_delete_black_24dp,
//                        mContext.getString(R.string.notificationDeleteLabel), getDeletePendingIntent(mContext, notifId,  messageId))
//                        .build();
	    NotificationCompat.Action actionShow =
			    new NotificationCompat.Action.Builder(R.drawable.ic_wear_result_open,
                        mContext.getString(R.string.wear_notification_show_on_mobile), getWearShowPendingIntent(mContext, notifId,  messageId))
					    .build();
/*
		// image management
	    if (Preferences.showImagesInWear(mContext) && message.getImageCount() != 0) {
		    for (int i = 0; i < message.getImageCount(); i++) {
			    String imageUri = message.getImage(i);
			    Bitmap bitmap = UrlImageViewHelper.getCachedBitmap(imageUri);
			    if (bitmap != null) {
				    // TODO : resize?
				    // TODO add page to notification
			    }
		    }
	    }
*/
	    wearableExtender.addAction(actionSeen);
	    wearableExtender.addAction(actionDelete);
	    wearableExtender.addAction(actionShow);
        NotificationCompat.Builder wearNotification = new NotificationCompat.Builder(mContext, CommonUtilities.NO_PRIORITY_NOTIFICATION_CHANNEL)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .extend(wearableExtender)
                .setContentTitle(getColoredMessageHeader(message))
                .setContentText(getNotificationContent(message))
                .setSmallIcon(R.drawable.ic_launcher)
                .setGroup(NewtifryProHelper.GROUP_KEY_MESSAGES)
                .setGroupSummary(false)
                .setOngoing(false)
                .setOnlyAlertOnce(true)
		        .setSortKey(Long.toString(messageId))
		        .setDeleteIntent(getWearCancelPendingIntent(
				        mContext, notifId, messageId));
        if (vibrate) {
            wearNotification.setDefaults(NotificationCompat.DEFAULT_VIBRATE);
        }
//        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // Nougat
//            wearNotification.addAction(actionSeenMobile);
//            wearNotification.addAction(actionDeleteMobile);
//        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(mContext).notify(notifId + BASE_WEAR_NOTIFICATION_ID, wearNotification.build());
    }
}
