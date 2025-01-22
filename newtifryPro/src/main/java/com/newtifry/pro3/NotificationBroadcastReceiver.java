package com.newtifry.pro3;

import com.newtifry.pro3.database.NewtifryMessage2;
import com.newtifry.pro3.database.NewtifryProvider;
import com.newtifry.pro3.fcm.MyFcmListenerService;
import com.newtifry.pro3.shared.NewtifryProHelper;
import com.newtifry.pro3.utils.UniversalNotificationManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class NotificationBroadcastReceiver extends BroadcastReceiver {

	//private static long lastDeletedMessageId = -1;
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
        boolean fromWear = intent.getBooleanExtra(NewtifryProHelper.IntentExtras.FROM_WEAR, false);
        int wearNotificationId = intent.getIntExtra(NewtifryProHelper.IntentExtras.WEAR_NOTIFICATION_ID, -1);
        long messageId = intent.getLongExtra(NewtifryProHelper.IntentExtras.ID, -1);

///////////////////// UNDO management
        assert action != null;
        if (action.equals(NewtifryProHelper.NOTIFICATION_UNDO_DELETE)) {
			NewtificationService.cancelUndoTimeout(context);
            if (messageId != -1) {
                NewtifryMessage2 msg = NewtifryMessage2.get(context, messageId);
                if (msg != null) {
                    msg.setDeleted(false);
                    msg.save(context);
					UniversalNotificationManager.getInstance(context).incNewMessagesCount();
					UniversalNotificationManager.reSendNotification(context, msg.getId());
				}
            }
            return;
        }

        if (action.equals(NewtifryProHelper.NOTIFICATION_CANCEL_UNDO)) {
			NewtificationService.cancelUndoTimeout(context);
            if (messageId != -1) {
                NewtifryProvider.deleteItem(context, messageId);
	            UniversalNotificationManager.getInstance(context).resetNewMessagesCount();
            }
            return;
        }
///////////////////////////////////////////////////////////////////
        if (action.equals(NewtifryProHelper.MESSAGE_CREATE)) { // From tasker
			Bundle extras = intent.getExtras();
			Map<String, Object> data = new HashMap<String, Object>();
            assert extras != null;
            for (String key : extras.keySet()) {
				data.put(key, Objects.requireNonNull(extras.get(key)).toString());
			}

			NewtifryMessage2 message = NewtifryMessage2.fromFCM(data, false);
			if (message.getPriority() > -1 || message.getNotify() == 1 || Preferences.showInvisibleMessages(context)) {
				UniversalNotificationManager.getInstance(context).incNewMessagesCount();
			}
            //MyFcmListenerService.messageProcess(context, message);
			return;
		}

		if (action.equals(NewtifryProHelper.MESSAGE_SHOW)) {  // from wear or smartwatch
			NewtifryMessage2 msg = NewtifryMessage2.get(context, messageId);
  			if (msg != null) {
				// will remove all notifications
				Intent showIntent  = new Intent(context, NewtifryMessageDetailActivity.class);
				showIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				showIntent.putExtra(NewtifryMessageDetailFragment.ARG_ITEM_ID, messageId);
				context.startActivity(showIntent);
			} else {
				// remove this one from wear
				if (fromWear) {
					UniversalNotificationManager.getInstance(context).cancelWearNotification(wearNotificationId);
				}
			}
			return;
		}
			
		if (action.equals(NewtifryProHelper.NOTIFICATION_CANCEL)) { // from mobile
			UniversalNotificationManager.getInstance(context).resetNewMessagesCount();
			return;
		}
		if (action.equals(NewtifryProHelper.WEAR_NOTIFICATION_CANCEL)) { // from wear
			UniversalNotificationManager.getInstance(context).cancelWearNotification(wearNotificationId);
			return;
		}
		if (action.equals(NewtifryProHelper.NOTIFICATION_SEEN_ALL)) { // from mobile notification
    		NewtifryProvider.markAllItemsRead(context);
			UniversalNotificationManager.getInstance(context).resetNewMessagesCount();
			return;
		}

		if (action.equals(NewtifryProHelper.NOTIFICATION_DELETE)) { // from mobile only
			NewtifryMessage2 message = NewtifryMessage2.get(context,  messageId);
			if (message != null) {
				if (Preferences.isUndoEnable(context)) {
					message.setDeleted(true);
					message.save(context);
					NewtificationService.createUndoNotification(context, messageId);
					UniversalNotificationManager.getInstance(context).resetNewMessagesCountFromUndo();
                    // possible only with only one new message so remove all wear notifications
                    //UniversalNotificationManager.getInstance(context).cancelAllWearNotifications();
				} else {
					NewtifryProvider.deleteItem(context, messageId);
					UniversalNotificationManager.getInstance(context).resetNewMessagesCount();
				}

			}
            //
			return;
		}
		if (action.equals(NewtifryProHelper.NOTIFICATION_SEEN)) {
			NewtifryMessage2 message = NewtifryMessage2.get(context, messageId);
			if (message != null) {
				message.setSeen(true);
				message.save(context);
			}
            if (fromWear) {
	            UniversalNotificationManager.getInstance(context).cancelWearNotification(wearNotificationId);
            } else {
				// possible only with a single message
	            UniversalNotificationManager.getInstance(context).resetNewMessagesCount();
            }
			return;
		}
        // from WEAR only
        if (action.equals(NewtifryProHelper.WEAR_MESSAGE_DELETE)) {
            NewtifryMessage2 message = NewtifryMessage2.get(context,  messageId);
            if (message != null) {
                NewtifryProvider.deleteItem(context, messageId);
            }
	        UniversalNotificationManager.getInstance(context).cancelWearNotification(wearNotificationId);
        }
    }
}
