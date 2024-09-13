package com.newtifry.pro3.utils;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.newtifry.pro3.NotificationBroadcastReceiver;
import com.newtifry.pro3.shared.NewtifryProHelper;

import static com.newtifry.pro3.shared.NewtifryProHelper.NOTIFICATION_SEEN_ALL;

/**
 * Created by alaurent on 20/10/2017.
 */

public class ShortcutsActivity  extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //context = getApplicationContext();
        // Get the intent that started this activity
        ShortcutHelper mHelper;
        mHelper = new ShortcutHelper(this);
        mHelper.reportShortcutUsed(NOTIFICATION_SEEN_ALL);
        Intent intent = getIntent();
        //intent.set (getApplicationContext(), NotificationBroadcastReceiver.class );
        Intent seenAllIntent = new Intent(this, NotificationBroadcastReceiver.class);
        seenAllIntent.setAction(intent.getAction());
        //pass all intents to broadcast receiver
        sendBroadcast(seenAllIntent);
        setResult(1);
        //finish();
    }
}
