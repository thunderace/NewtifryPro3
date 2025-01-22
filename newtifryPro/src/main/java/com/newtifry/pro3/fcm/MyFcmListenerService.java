package com.newtifry.pro3.fcm;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.newtifry.pro3.CommonUtilities;
import com.newtifry.pro3.NewtifryMessageListActivity;
import com.newtifry.pro3.Preferences;
import com.newtifry.pro3.R;
import com.newtifry.pro3.database.NewtifryMessage2;
import com.newtifry.pro3.urlimageviewhelper.UrlImageViewHelper;
import com.newtifry.pro3.utils.PartialNewtifry2Message;
import com.newtifry.pro3.utils.UniversalNotificationManager;

import java.util.Map;
import java.util.Objects;

import static com.newtifry.pro3.CommonUtilities.LOG_ERROR_LEVEL;
import static com.newtifry.pro3.CommonUtilities.LOG_VERBOSE_LEVEL;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;


public class MyFcmListenerService extends FirebaseMessagingService {

    private static final String TAG = "MyFcmListenerService";
    private static PartialNewtifry2Message partialMessage;

    public void messageProcess(Context context, NewtifryMessage2 message) {
        if (Preferences.getShowImages(context) &&
                Preferences.getPreloadBitmap(context)) {
            for (int i = 0; i < 5; i++) {
                String image = message.getImage(i);
                if (image != null && !image.isEmpty()) {
                    UrlImageViewHelper.loadUrl(context, message.getId(), i, image, null, Preferences.getCacheBitmap(context) && !message.getNoCache());
                }
            }
        }
        Log.d("DBG", "received message");
        Log.d("DBG", String.format("%s %s", message.getTitle(), message.getSourceName()));
        // Send a notification to the notification service, which will then
        // dispatch and handle everything else.
        UniversalNotificationManager.createNotification(context, message.getId(), message.getSpeak(), message.getNotify());
    }

    @Override
    public void onMessageReceived(RemoteMessage inMessage) {
        Map<String, String> data = inMessage.getData();
        // The server would have sent a message type.
        String type;
        try {
            type = data.get("type");
        } catch (Exception e) {
            CommonUtilities.log(LOG_ERROR_LEVEL, "onMessageReceived", "No type entry in msg");
            return;
        }
        assert type != null;
        if (type.equals("ntp_message_multi")) {
            String multipartHash = data.get("hash");
            int partCount = Integer.parseInt(Objects.requireNonNull(data.get("partcount")));
            int currentPart = Integer.parseInt(Objects.requireNonNull(data.get("index")));
            CommonUtilities.log(LOG_VERBOSE_LEVEL, TAG, "New multi message : part " + currentPart + " of " + partCount);
            String multiPartMessage = data.get("body");
            if (partialMessage == null) {
                partialMessage = new PartialNewtifry2Message();
            }
            NewtifryMessage2 incoming;
            if (partialMessage.init(partCount, currentPart, multipartHash)) {
                incoming = partialMessage.addPart(multiPartMessage, currentPart);
                if (incoming == null) { // msg not complete
                    return;
                }
                CommonUtilities.log(LOG_VERBOSE_LEVEL, TAG, "onMessageReceived : Multipart Message complete");
            } else {
                CommonUtilities.log(LOG_VERBOSE_LEVEL, TAG, "onMessageReceived : init error");
                return;
            }
            // Persist this message to the database.
            incoming.save(this);
            if (incoming.getPriority() > -1 || incoming.getNotify() == 1 || Preferences.showInvisibleMessages(this)) {
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
            if (message.getPriority() > -1 || message.getNotify() == 1 || Preferences.showInvisibleMessages(this)) {
                UniversalNotificationManager.getInstance(this).incNewMessagesCount();
            }
            messageProcess(this, message);
        }
    }

    // [END receive_message]
    @Override
    public void onNewToken(@NonNull String token) {
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