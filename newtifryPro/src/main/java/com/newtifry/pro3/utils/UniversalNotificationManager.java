package com.newtifry.pro3.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;

import com.newtifry.pro3.CommonUtilities;
import com.newtifry.pro3.NewtificationService;
import com.newtifry.pro3.NotificationBroadcastReceiver;
import com.newtifry.pro3.Preferences;
import com.newtifry.pro3.R;
import com.newtifry.pro3.database.NewtifryMessage2;
import com.newtifry.pro3.shared.NewtifryProHelper;

import java.util.LinkedList;

public class UniversalNotificationManager {
    private final int maxStackedWearNotificationCount =  20;
    private final static int BASE_WEAR_NOTIFICATION_ID = 2;
    private final static int MOBILE_NOTIFICATION_ID = 8;
    private final static Object lock = new Object();
    private static UniversalNotificationManager sInstance = null;
    private final Context mContext;
    private final Bitmap background;

	private int newMessageCount = 0;
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


	public static void createNotification(Context context, long messageId, int speak, int notify) {
		Intent intentData = new Intent(context, NewtificationService.class);
		intentData.putExtra("messageId", messageId);
		intentData.putExtra("speak", speak);
        intentData.putExtra("notify", notify);
        Log.d("DBG", "from createNotification");
        ContextCompat.startForegroundService(context,intentData);
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
        NotificationManagerCompat.from(mContext).notify(notifId + BASE_WEAR_NOTIFICATION_ID, wearNotification.build());
    }
}
