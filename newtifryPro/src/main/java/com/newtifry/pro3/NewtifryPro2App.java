package com.newtifry.pro3;


import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.google.firebase.FirebaseApp;
//import io.explod.android.sqllog.data.LogEntryProvider;

public class NewtifryPro2App extends Application {
	private static NewtifryPro2App instance;
	@Override
	public void onCreate() {
		super.onCreate();
		FirebaseApp.initializeApp(this);
		CommonUtilities.updatePreventSleepMode(this, false);
	}
	public static boolean isDebug() {
		return BuildConfig.DEBUG;
	}
    public static Context getContext() {
    	return instance;
    }
	// uncaught exception handler variable
	private Thread.UncaughtExceptionHandler defaultUEH;

	// handler listener
	private final Thread.UncaughtExceptionHandler _unCaughtExceptionHandler =
			new Thread.UncaughtExceptionHandler() {
				@Override
				public void uncaughtException(Thread thread, Throwable ex) {
					// here I do logging of exception to a db
					CommonUtilities.log( CommonUtilities.LOG_ERROR_LEVEL, "Crash", ex.getMessage());
					PendingIntent myActivity = PendingIntent.getActivity(getContext(),
							192837, new Intent(getContext(), NewtifryMessageListActivity.class),
                            PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

					AlarmManager alarmManager;
					alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
					alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
							5000, myActivity );
					System.exit(2);
					// re-throw critical exception further to the os (important)
					defaultUEH.uncaughtException(thread, ex);
				}
			};

	public NewtifryPro2App() {
		instance = this;
		/*
		defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
		// setup handler for uncaught exception
		Thread.setDefaultUncaughtExceptionHandler(_unCaughtExceptionHandler);
		*/
	}
}
