package com.newtifry.pro3.fcm;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.newtifry.pro3.CommonUtilities;
import com.newtifry.pro3.Preferences;
import com.newtifry.pro3.R;
import com.newtifry.pro3.database.NewtifryMessage2;
import com.newtifry.pro3.urlimageviewhelper.UrlImageViewHelper;
import com.newtifry.pro3.utils.PartialNewtifry2Message;
import com.newtifry.pro3.utils.UniversalNotificationManager;

import java.util.Map;

import static com.newtifry.pro3.CommonUtilities.LOG_ERROR_LEVEL;
import static com.newtifry.pro3.CommonUtilities.LOG_VERBOSE_LEVEL;

public class MyFcmListenerService extends FirebaseMessagingService {

    private static final String TAG = "MyFcmListenerService";
    private static boolean enable = true;
    private static PartialNewtifry2Message partialMessage;
    private static NewtifryMessage2 incoming = null;
    public static void messageProcess(Context context, NewtifryMessage2 message) {
        if (Preferences.getShowImages(context) == true &&
                Preferences.getPreloadBitmap(context) == true) {
            for (int i = 0; i < 5; i++) {
                String image = message.getImage(i);
                if (image != null && !image.equals("")) {
                    if (Preferences.getCacheBitmap(context) == true && message.getNoCache() == false) {
                        UrlImageViewHelper.loadUrl(context, message.getId(), i, image, null, true);
                    } else {
                        UrlImageViewHelper.loadUrl(context, message.getId(), i, image, null, false);
                    }
                }
            }
        }
        Log.d("DBG", "received message");
        Log.d("DBG", String.format("%s %s", message.getTitle(), message.getSourceName()));
        // Send a notification to the notification service, which will then
        // dispatch and handle everything else.
        UniversalNotificationManager.createNotification(context, message.getId(), message.getSpeak(), message.getNotify());
    }

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage inMessage) {
        Map data = inMessage.getData();
        // The server would have sent a message type.
        String type;
        try {
            type = data.get("type").toString();
        } catch (Exception e) {
            CommonUtilities.log(LOG_ERROR_LEVEL, "onMessageReceived", "No type entry in msg");
            return;
        }
        if (type.equals("ntp_message_multi")) {
            CommonUtilities.log(LOG_VERBOSE_LEVEL, TAG, "New multi message");
            String multipartHash = data.get("hash").toString();
            int partCount = Integer.parseInt(data.get("partcount").toString());
            int currentPart = Integer.parseInt(data.get("index").toString());
            String multiPartMessage = data.get("body").toString();
            if (partialMessage == null) {
                partialMessage = new PartialNewtifry2Message();
            }
            if (partialMessage.init(partCount, currentPart, multipartHash) == true) {
                incoming = partialMessage.addPart(multiPartMessage, currentPart, false);
                if (incoming == null) {
                    return;
                }
                partialMessage = null;
            } else {
                return;
            }
            // Persist this message to the database.
            incoming.save(this);
            if (incoming.getPriority() > -1 || incoming.getNotify() == 1 || Preferences.showInvisibleMessages(this) == true) {
                UniversalNotificationManager.getInstance(this).incNewMessagesCount();
            }
            messageProcess(this, incoming);

        }
        if (type.equals("ntp_message")) {
            // Fetch the message out into a NewtifryMessage object.
            //CommonUtilities.log(LOG_VERBOSE_LEVEL, TAG, "New message");
            NewtifryMessage2 message = NewtifryMessage2.fromFCM(data, true);
            if (message == null) {
                CommonUtilities.log(LOG_ERROR_LEVEL, "onMessageReceived", "data is null");
                return;
            }
            // Persist this message to the database.
            message.save(this);
            if (message.getPriority() > -1 || message.getNotify() == 1 || Preferences.showInvisibleMessages(this) == true) {
                UniversalNotificationManager.getInstance(this).incNewMessagesCount();
            }
            messageProcess(this, message);
            //throw new RuntimeException("Boom!");
        }
    }

    // [END receive_message]
    @Override
    public void onNewToken(String token) {
        // do nothing
        String curToken = Preferences.getToken(this);
        if (curToken != token) {
            Preferences.saveToken(token, this);
            final Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("plain/text");
            String subject = String.format(getString(R.string.fcm_id_email_subject), Build.MODEL);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT,
                    String.format(getString(R.string.fcm_id_email_body), token));
            startActivity(Intent.createChooser(emailIntent, "Send key via email"));
        }
    }
}