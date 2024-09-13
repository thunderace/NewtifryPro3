/*
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.newtifry.pro3.locale;

import com.newtifry.pro3.NewtifryMessageDetailActivity;
import com.newtifry.pro3.NewtifryMessageDetailFragment;
import com.newtifry.pro3.Preferences;
import com.newtifry.pro3.shared.NewtifryProHelper;
import com.newtifry.pro3.database.NewtifryMessage2;
import com.newtifry.pro3.database.NewtifryProvider;
import com.newtifry.pro3.locale.ui.EditActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class ActionFireReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, final Intent intent) {
        final Bundle bundle = intent.getBundleExtra(LocaleConstants.EXTRA_BUNDLE);
        if (bundle != null) {
            final String action = bundle.getString(EditActivity.BUNDLE_ACTION_STRING);

            switch (action) {
	            case NewtifryProHelper.MESSAGE_SHOW_LAST:
	            	long messageId = NewtifryMessage2.getLastMessageId(context);
	            	if (messageId != -1) {
		    			Intent showIntent  = new Intent(context, NewtifryMessageDetailActivity.class);
		    			showIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		    			showIntent.putExtra(NewtifryMessageDetailFragment.ARG_ITEM_ID, messageId);
		    			context.startActivity(showIntent);
	            	}
	            	break;
//	            case NewtifryProHelper.MESSAGE_SHOW:
//	            	// todo get message id from bundle
//	            	String  mId = bundle.getString(NewtifryProHelper.IntentExtras.ID);
//	            	if (mId == null) {
//	            		return;
//	            	}
//	            	try {
//	            		messageId = Integer.parseInt(mId, 10);
//	            	} catch(NumberFormatException ex) {
//	            		messageId = -1;
//	            	}
//	            	if (messageId != -1 && messageId != 0) {
//		    			Intent showIntent  = new Intent(context, NewtifryMessageDetailActivity.class);
//		    			showIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//		    			showIntent.putExtra(NewtifryMessageDetailFragment.ARG_ITEM_ID, messageId);
//		    			context.startActivity(showIntent);
//	            	}
//	            	break;
	            case NewtifryProHelper.MARK_ALL_SEEN:
	        		NewtifryProvider.markAllItemsRead(context);
	            	break;
	            case NewtifryProHelper.MESSAGE_SEEN_LAST:
	            	messageId = NewtifryMessage2.getLastMessageId(context);
	            	if (messageId != -1) {
	            		NewtifryMessage2 message = NewtifryMessage2.get(context, messageId);
	            		message.setSeen(true);
	            		message.save(context);
	            	}
	            	break;
	            case NewtifryProHelper.SPEAK_OFF:
	        		Preferences.setSpeakMessage(context, false);
	        		break;
	            case NewtifryProHelper.SPEAK_ON:
	        		Preferences.setSpeakMessage(context, true);
	        		break;
	            case NewtifryProHelper.NOTIFICATION_OFF:
	        		Preferences.saveNotificationEnable(false, context);
	        		break;
	            case NewtifryProHelper.NOTIFICATION_ON:
	        		Preferences.saveNotificationEnable(true, context);
	        		break;
                default:
                    break;
            }
        }
    }

    /**
     * This method redirects the incoming broadcast intent to the service, if it's alive. The
     * service cannot be communicated through messages in this class because this BroadcastReceiver
     * is registered through the AndroidManifest {@code <receiver>} tag which means this
     * BroadcastReceiver will no longer exist after return from {@code onReceive()}.
     *
     * @param forceService Force the action, even if the service isn't active.
     * @param intent       The incoming intent through {@code onReceive()}.
     * @param action       The incoming intent action.
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
     * android.content.Intent)
     */
}
