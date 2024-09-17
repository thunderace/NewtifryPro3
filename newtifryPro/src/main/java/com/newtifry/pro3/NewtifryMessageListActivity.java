package com.newtifry.pro3;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.newtifry.pro3.preferences.NewtifryPreferenceActivity;
import com.newtifry.pro3.shared.NewtifryProHelper;
import com.newtifry.pro3.urlimageviewhelper.UrlImageViewHelper;
import com.newtifry.pro3.about.AboutActivity;
import com.newtifry.pro3.database.NewtifryMessage2;
import com.newtifry.pro3.database.NewtifryProvider;
import com.newtifry.pro3.utils.ShortcutHelper;
import com.newtifry.pro3.utils.UniversalNotificationManager;
import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;

import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import static com.newtifry.pro3.CommonUtilities.isActivityCallable;

public class NewtifryMessageListActivity extends AppCompatActivity implements
	NewtifryMessageDetailFragment.Callbacks , NewtifryMessageListUndoFragment.Callbacks {
    public static Context context;
    private BroadcastReceiver mRegistrationBroadcastReceiver;

	public final static int PREFERENCES_MENU_ID = 0;
	private final static int DELETE_ALL = 1;
	private final static int DELETE_SEEN = 2;
	private final static int MARK_ALL_AS_SEEN = 3;
	private final static int SORT_BY_PRIORITY = 4;
	private final static int SORT_BY_SOURCE = 5;
	public final static int START_SPEAK = 6;
	public final static int STOP_SPEAK = 7;
	public final static int SHOW_IMAGE = 8;
	public final static int ABOUT_MENU = 9;
	public final static int MARK_UNREAD_MENU_ID =11;
	public final static int STICK_MENU_ID = 12;
	public final static int UNLOCK_MENU_ID = 13;
	
	private final static int DEBUG_MENU1 = 66;
	private final static int DEBUG_MENU2 = 67;
	private final static int DEBUG_MENU3 = 68;
	
	private NewtifryMessageDetailFragment messageDetailfragment;

	private boolean sortByPriority = false;
	private boolean sortBySource = false;

	private MenuItem deleteSeenMenu = null;
	private MenuItem markAllSeenMenu = null;
	// for two pane mod
	private MenuItem showImageMenu;
	private MenuItem stopSpeakMenu;
	private MenuItem startSpeakMenu;
	private MenuItem stickMenu;
	private MenuItem unlockMenu;
	private MenuItem markUnseenMenu;
	
	private View snackBarParentView;
	private Snackbar mSnackBar;
	MaterialDialog progressDialog = null;

	private boolean deleteAllOption = false;
	/**
	 * Whether or not the activity is in two-pane mode, i.e. running on a tablet
	 * device.
	 */
	private static final boolean mTwoPane = false;
	
	public static boolean isTwoPane() {
		return mTwoPane;
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_newtifrymessage_list);
		getSupportActionBar().setTitle(R.string.app_name);
		snackBarParentView = findViewById(R.id.coordinatorLayout);
		context = getApplicationContext();

		if (savedInstanceState == null) {
	        // check if we have a google account here
		    if (!NewtifryPro2App.isDebug()) {
				if (!CommonUtilities.checkGoogleAccount(this)) {
					showDialogAndQuit(getString(R.string.fatal_error), getString(R.string.google_account_needed));
					return;
				}
		    }
		}
        UrlImageViewHelper.cleanup(this, Preferences.getCacheBitmapDurationInMs(this));
		// TTS init
		if (isActivityCallable(this, "TextToSpeech.engine", TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)) {
			// Figure out if we have the TTS installed.
			Intent checkIntent = new Intent();
			checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
			startActivityForResult(checkIntent, 0x1010);
		}
	    NewtifryProvider.upgradeDDB14(this);
		NewtifryMessage2.purgeAll(this);

	    scheduleAlarm();
		FirebaseMessaging.getInstance().getToken()
				.addOnCompleteListener(new OnCompleteListener<String>() {
					@Override
					public void onComplete(@NonNull Task<String> task) {
						if (!task.isSuccessful()) {
							Log.w("New2", "Fetching FCM registration token failed", task.getException());
							return;
						}

						// Get new FCM registration token
						String token = task.getResult();

						// Log and toast
						//Toast.makeText(NewtifryMessageListActivity.this, token, Toast.LENGTH_SHORT).show();
						String curToken = Preferences.getToken(NewtifryMessageListActivity.this);
						if (!curToken.equals(token)) {
							Preferences.saveToken(token, NewtifryMessageListActivity.this);
							final Intent emailIntent = new Intent(Intent.ACTION_SEND);
							emailIntent.setType("plain/text");
							String subject = String.format(getString(R.string.fcm_id_email_subject), Build.MODEL);
							emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
							emailIntent.putExtra(Intent.EXTRA_TEXT,
									String.format(getString(R.string.fcm_id_email_body),
											Preferences.getToken(NewtifryMessageListActivity.this)));
							startActivity(Intent.createChooser(emailIntent, "Send key via email"));

						}
					}
				});
		askNotificationPermission();
 	}

	/**
	 * Callback function for checking if the Text to Speech is installed. If
	 * not, it will redirect the user to download the text data.
	 */
	@Override
	protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
		super.onActivityResult(requestCode, resultCode, data);
	}


	private void scheduleAlarm() {
		ComponentName serviceComponent = new ComponentName(context, AlarmJobService.class);
		JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
		builder.setPeriodic(1000 * 60 * 60 * 48 ); // every 2 days
		JobScheduler jobScheduler = (JobScheduler)context.getSystemService(JOB_SCHEDULER_SERVICE);
		jobScheduler.schedule(builder.build());
	}
	
	public void showDialogAndQuit(String title, String message) {
		new MaterialDialog.Builder(this)
				.title(title)
				.content(message)
				.positiveText(R.string.dialog_ok)
				.onPositive(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						finish();
					}
				}).show();
	}

	public void confirmDelete2(String title, String message) {
		new MaterialDialog.Builder(this)
				.title(title)
				.content(message)
				.positiveText(R.string.dialog_ok)
				.negativeText(R.string.dialog_cancel)
				.onPositive(new MaterialDialog.SingleButtonCallback() {
					@Override
					public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
						deleteAllAction(deleteAllOption);
					}
				}).show();
	}

	private final BroadcastReceiver DDBChangeReceiver = new BroadcastReceiver()	{
		@Override
		public void onReceive( Context context, Intent intent )	{
            //updateSubTitle();
			updateMenu();
		}
	};

	@Override
	public void onResume() {
		super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(CommonUtilities.REGISTRATION_COMPLETE));

        LocalBroadcastManager.getInstance(this).registerReceiver(DDBChangeReceiver, new IntentFilter(CommonUtilities.MESSAGE_DDB_CHANGE_INTENT));
		updateSubTitle();
		this.updateMenu();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (progressDialog != null  && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(DDBChangeReceiver);
	}

	private void updateSubTitle() {
		String template = getString(R.string.recent_messages_withcount);
		int totalMessages = NewtifryMessage2.count(this);

		if (totalMessages == 0) {
			getSupportActionBar().setSubtitle(R.string.no_message);
		} else {
			getSupportActionBar().setSubtitle(String.format(template, totalMessages, NewtifryMessage2.countUnread(this)));
		}
	}
 	
	private long lastMessageDetailId = -1;
	@Override
	public void onItemSelected(long id) {
		Log.d("MessageListActivity", "Item selected id : " + id);
		lastMessageDetailId = id;
		this.updateMenu();
		if (id == -1) {// it's a message detail deletion : update menu only
			return;
		}
		NewtifryMessage2 message = NewtifryMessage2.get(NewtifryMessageListActivity.context, id);
		if (message != null) {
			lastMessageDetailId = id;
			if (mTwoPane) {
				// In two-pane mode, show the detail view in this activity by
				// adding or replacing the detail fragment using a
				// fragment transaction.
				Bundle arguments = new Bundle();
				arguments.putLong(NewtifryMessageDetailFragment.ARG_ITEM_ID, id);
				messageDetailfragment = new NewtifryMessageDetailFragment();
				messageDetailfragment.setArguments(arguments);
				getSupportFragmentManager().beginTransaction()
						.replace(R.id.newtifrymessage_detail_container, messageDetailfragment)
						.commit();
			} else {
				// In single-pane mode, simply start the detail activity
				// for the selected item ID.
				Intent detailIntent = new Intent(this,
						NewtifryMessageDetailActivity.class);
				detailIntent
						.putExtra(NewtifryMessageDetailFragment.ARG_ITEM_ID, id);
				startActivity(detailIntent);
			}
		}
	}
	
	private void updateMenu() {
		int unreadMessages = NewtifryMessage2.countUnread(this);
		int totalMessages = NewtifryMessage2.count(this);
		if (markAllSeenMenu != null  && deleteSeenMenu != null) {
			markAllSeenMenu.setEnabled(unreadMessages != 0);
			deleteSeenMenu.setEnabled((totalMessages - unreadMessages)  != 0);
		}
		updateSubTitle();
	
		if (stopSpeakMenu != null) {
            stopSpeakMenu.setVisible(Preferences.getSpeakMessage(this));
		}

		if (mTwoPane && showImageMenu != null && startSpeakMenu != null) {
			NewtifryMessage2 message = NewtifryMessage2.get(this, lastMessageDetailId);
			if (message == null) {
				onShowImageMenuSetVisible(false);
				startSpeakMenu.setVisible(false);
				stickMenu.setVisible(false);
				unlockMenu.setVisible(false);
				markUnseenMenu.setVisible(false);
				return; 
			} else {
				markUnseenMenu.setVisible(true);
				onShowImageMenuSetVisible(true);
                startSpeakMenu.setVisible(Preferences.getSpeakMessage(this));
			}
			if (message.isLocked()) {
				stickMenu.setVisible(false);
				unlockMenu.setVisible(true);
				unlockMenu.setTitle(R.string.unlock_menu_entry);
			} else {
				stickMenu.setVisible(true);
				if (message.getSticky()) {
					unlockMenu.setVisible(false);
					stickMenu.setTitle(R.string.unstick_menu_entry);
				} else {
					unlockMenu.setVisible(true);
					unlockMenu.setTitle(R.string.lock_menu_entry);
					stickMenu.setTitle(R.string.stick_menu_entry);
				}
			}
		}		
	}
	
	@Override
	public boolean onPrepareOptionsMenu( Menu menu ) {
		updateMenu();
		return true;
	}
	
	public void onShowImageMenuSetVisible(boolean show) {
		if (showImageMenu != null) {
			showImageMenu.setEnabled(show);
			showImageMenu.getIcon().setAlpha(show ? 255: 64);

		}
	}
	@Override
	public boolean onCreateOptionsMenu( Menu menu )	{
		boolean result = super.onCreateOptionsMenu(menu);
		deleteSeenMenu = menu.add(0, DELETE_SEEN, Menu.NONE, R.string.delete_read);
		deleteSeenMenu.setIcon(R.drawable.ic_delete_white_24dp/* R.drawable.ic_menu_trash_white */);
		MenuItemCompat.setShowAsAction(deleteSeenMenu,MenuItem.SHOW_AS_ACTION_IF_ROOM);
		markAllSeenMenu = menu.add(0, MARK_ALL_AS_SEEN, Menu.NONE, R.string.mark_all_as_seen);
		markAllSeenMenu.setIcon(R.drawable.ic_visibility_white_24dp  /*.ic_menu_read_white*/);
		MenuItemCompat.setShowAsAction(markAllSeenMenu, MenuItem.SHOW_AS_ACTION_IF_ROOM);
		menu.add(1, SORT_BY_PRIORITY, Menu.NONE, R.string.sort_by_priority).setIcon(android.R.drawable.ic_menu_sort_by_size).setCheckable(true);
		menu.setGroupCheckable(1, true, false);
		menu.add(0, SORT_BY_SOURCE, Menu.NONE, R.string.sort_by_source).setIcon(android.R.drawable.ic_menu_sort_by_size);
		menu.add(0, DELETE_ALL, Menu.NONE, R.string.delete_all).setIcon(android.R.drawable.ic_delete);
		stopSpeakMenu = menu.add(0, STOP_SPEAK, Menu.NONE, R.string.stop_speak_now);
		stopSpeakMenu.setIcon(R.drawable.ic_menu_stop_speak_white_24dp);
		MenuItemCompat.setShowAsAction(stopSpeakMenu, MenuItem.SHOW_AS_ACTION_IF_ROOM);
		if (NewtifryPro2App.isDebug()) {
			menu.add(0, DEBUG_MENU1, Menu.NONE, "Debug1");
			menu.add(0, DEBUG_MENU2, Menu.NONE, "Debug2");
			menu.add(0, DEBUG_MENU3, Menu.NONE, "Debug3");
 		}
		
		if (mTwoPane) {
			startSpeakMenu = menu.add(0, START_SPEAK, Menu.NONE, R.string.start_speak);
			startSpeakMenu.setIcon(R.drawable.ic_menu_start_speak_white_24dp);
			MenuItemCompat.setShowAsAction(startSpeakMenu, MenuItem.SHOW_AS_ACTION_IF_ROOM);
			showImageMenu = menu.add(0, SHOW_IMAGE, Menu.NONE, R.string.show_image);
			showImageMenu.setIcon(R.drawable.ic_image_white_24dp);
			MenuItemCompat.setShowAsAction(showImageMenu, MenuItem.SHOW_AS_ACTION_IF_ROOM);
			stickMenu = menu.add(0, STICK_MENU_ID, Menu.NONE, R.string.stick_menu_entry);
			unlockMenu = menu.add(0, UNLOCK_MENU_ID, Menu.NONE, R.string.unlock_menu_entry);
			markUnseenMenu = menu.add(0, MARK_UNREAD_MENU_ID, Menu.NONE, R.string.mark_unread_menu_entry).setIcon(R.drawable.ic_visibility_white_24dp/*ic_menu_read_white*/);
		}
		
		menu.add(0, PREFERENCES_MENU_ID, Menu.NONE, R.string.preference_menu_entry).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, ABOUT_MENU, Menu.NONE, R.string.about_title).setIcon(android.R.drawable.ic_menu_info_details);
		return result;
	}
	private PendingIntent getDeleteOnePendingIntent(long messageId) {
		Intent intent = new Intent(NewtifryProHelper.NOTIFICATION_DELETE)
				.setClass(this, NotificationBroadcastReceiver.class);
		intent.putExtra(NewtifryProHelper.IntentExtras.ID, messageId);
		return PendingIntent.getBroadcast(this, 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
	}

	private PendingIntent getSeenOnePendingIntent(long messageId) {
		Intent intent = new Intent(NewtifryProHelper.NOTIFICATION_SEEN)
				.setClass(this, NotificationBroadcastReceiver.class);
		intent.putExtra(NewtifryProHelper.IntentExtras.ID, messageId);
		return PendingIntent.getBroadcast(this, 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
	}
/*
	public void testnot(Context context)
	{
		Notification.Builder notif;
		NotificationManager nm;
		notif = new Notification.Builder(context);
		notif.setSmallIcon(R.drawable.ic_stat_statusbar_newtifrypro2);
		notif.setContentTitle("Nouveau message");
		Uri path = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		notif.setSound(path);
		nm = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);


		Notification.Action deleteAction = new Notification.Action.Builder(R.drawable.ic_delete_white_24dp, context.getString(R.string.notificationDeleteLabel), getDeleteOnePendingIntent(1)).build();
		Notification.Action seenAction = new Notification.Action.Builder(R.drawable.ic_visibility_white_24dp, context.getString(R.string.notificationSeenLabel), getSeenOnePendingIntent(1)).build();

		Intent yesReceive = new Intent();
		yesReceive.setAction(NewtifryProHelper.NOTIFICATION_DELETE);
		PendingIntent pendingIntentYes = PendingIntent.getBroadcast(context, 12345, yesReceive, PendingIntent.FLAG_UPDATE_CURRENT);
		notif.addAction(deleteAction);


		Intent yesReceive2 = new Intent();
		yesReceive2.setAction(NewtifryProHelper.NOTIFICATION_SEEN);
		PendingIntent pendingIntentYes2 = PendingIntent.getBroadcast(context, 12345, yesReceive2, PendingIntent.FLAG_UPDATE_CURRENT);
		notif.addAction(seenAction);



		nm.notify(10, notif.getNotification());
	}
 */
	@Override
	public boolean onOptionsItemSelected( MenuItem item ) {
		switch( item.getItemId()) {
			case android.R.id.home:
		 		return true;
			case DEBUG_MENU1:
				// speak default
				NewtifryMessage2 message;

				message = NewtifryMessage2.fromDebug("haberet", "opulenti", 1, 0, 0);
				message.setMessage("The less than (<) and <font color='green'>empersand</font> (&) must be <b>escaped</b> before using them in html");
				message.save(this);
				UniversalNotificationManager.getInstance(context).incNewMessagesCount();
				UniversalNotificationManager.createNotification(this, message.getId(), 0, -1); // speak & notify default -> nothing

				return true;
			case DEBUG_MENU2:
				// priorit� 2
				// force speak
				message = NewtifryMessage2.fromDebug("expetendas", "amicitiarum", 2, 0, 0);
	    		message.save(this);
				UniversalNotificationManager.getInstance(context).incNewMessagesCount();
				UniversalNotificationManager.createNotification(this, message.getId(),-1, -1);
				break;
			case DEBUG_MENU3:
				// priorit� 3
				// force notify
				message = NewtifryMessage2.fromDebug("haberet", "opulenti", 1, 0, 0);
				message.save(this);
				UniversalNotificationManager.getInstance(context).incNewMessagesCount();
				UniversalNotificationManager.createNotification(this, message.getId(), -1, -1); // speak & notify default -> nothing
				message = NewtifryMessage2.fromDebug("expetendas", "amicitiarum", 2, 0, 0);
				message.save(this);
				UniversalNotificationManager.getInstance(context).incNewMessagesCount();
				UniversalNotificationManager.createNotification(this, message.getId(),-1, -1);
				message = NewtifryMessage2.fromDebug("benevolentiae", "praesidii", 3, 0, 0);
	    		message.save(this);
				UniversalNotificationManager.getInstance(context).incNewMessagesCount();
				UniversalNotificationManager.createNotification(this, message.getId(), -1, -1);
				break;
			case DELETE_ALL:
				deleteAll(false);
				return true;
			case DELETE_SEEN:
				deleteAll(true);
				return true;
			case SORT_BY_SOURCE:
				sortBySource(item);
				return true;
			case MARK_ALL_AS_SEEN:
				markAllAsSeen();
				return true;
			case ABOUT_MENU:
				Intent aboutIntent = new Intent(this, AboutActivity.class);
				startActivity(aboutIntent);
				return true;
			case SORT_BY_PRIORITY:
				sortByPriority(item);
				return true;
			case PREFERENCES_MENU_ID:
				Intent intent = new Intent(this, NewtifryPreferenceActivity.class);
				startActivity(intent);
				return true;
			case STOP_SPEAK:
				CommonUtilities.stopSpeak(this);
				return true;
		}

		if (mTwoPane) {
			NewtifryMessage2 message = NewtifryMessage2.get(NewtifryMessageListActivity.context, lastMessageDetailId);
			if (message == null) { // fix 1.3.0 to avoid crash in two pane mode
				return true;
			}
			switch( item.getItemId()) {
				case SHOW_IMAGE:
					onShowImageMenuSetVisible(false);
					messageDetailfragment.forceLoadImage();
					return true;
				case START_SPEAK:
					CommonUtilities.speak(this,CommonUtilities.getOutputMessage(message, this));
					return true;
				case MARK_UNREAD_MENU_ID:
					message.setSeen(false);
					message.save(this);
					return true;
				case STICK_MENU_ID:
					message.setSticky(!message.getSticky());
					message.save(this);
					// OK ; the left panel is refreshed but the last selected item is selected again
					// TODO : fix this
					messageDetailfragment.updateStickyLockMessage();
					return true;
				case UNLOCK_MENU_ID:
					message.setLocked(!message.isLocked());
					message.save(this);
					messageDetailfragment.updateStickyLockMessage();
					// OK ; the left panel is refreshed but the last selected item is selected again
					// TODO : FIX this
					return true;
			}
		}

		
		return super.onOptionsItemSelected(item);
	}
	
	public void sortBySource(MenuItem item)	{
		if (!this.sortBySource) {
			item.setTitle(R.string.sort_by_date);
			this.sortBySource = true;
		} else {
			item.setTitle(R.string.sort_by_source);
			this.sortBySource = false;
		}
		NewtifryMessageListUndoFragment listFragment = (NewtifryMessageListUndoFragment) getSupportFragmentManager()
				.findFragmentById(R.id.newtifrymessage_list);
		listFragment.setSort(sortByPriority, sortBySource);
	}

	
	public void deleteAll( boolean onlySeen ) {
		if (Preferences.getConfirmDeleteAll(this)) {
			deleteAllOption = onlySeen;
			if (onlySeen) {
				confirmDelete2(getString(R.string.confirm_delete_title), getString(R.string.confirm_delete_read_messages));
			} else {
				confirmDelete2(getString(R.string.confirm_delete_title), getString(R.string.confirm_delete_all_messages));
			}
		} else {
			deleteAllAction(onlySeen);
		}
	}
	
	public void deleteAllAction( boolean onlySeen ) {
		if (mTwoPane) {
			// clear message detail fragment
			if (onlySeen) {
				NewtifryMessage2 message = NewtifryMessage2.get(NewtifryMessageListActivity.context, lastMessageDetailId);
				if (message != null) {
					return;
				} 
			}
			Bundle arguments = new Bundle();
			NewtifryMessageDetailFragment fragment = new NewtifryMessageDetailFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.newtifrymessage_detail_container, fragment)
					.commit();
		}
        NewtifryProvider.deleteItems(this, onlySeen);

	}
	
	public void markAllAsSeen()	{
		NewtifryProvider.markAllItemsRead(context);
	}

	public void sortByPriority(MenuItem item) {
		if (!this.sortByPriority) {
			item.setChecked(true);
			this.sortByPriority = true;
		} else {
			item.setChecked(false);
			this.sortByPriority = false;
		}
		NewtifryMessageListUndoFragment listFragment = (NewtifryMessageListUndoFragment) getSupportFragmentManager()
				.findFragmentById(R.id.newtifrymessage_list);
		listFragment.setSort(sortByPriority, sortBySource);
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
	private final ActivityResultLauncher<String> requestPermissionLauncher =
			registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
				if (isGranted) {
					Toast.makeText(this, "Notifications permission granted",Toast.LENGTH_SHORT)
							.show();
				} else {
					Toast.makeText(this, "FCM can't post notifications without POST_NOTIFICATIONS permission",
							Toast.LENGTH_LONG).show();
				}
			});

	private void askNotificationPermission() {
		// This is only necessary for API Level > 33 (TIRAMISU)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
					PackageManager.PERMISSION_GRANTED) {
				// FCM SDK (and your app) can post notifications.
			} else {
				// Directly ask for the permission
				requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
			}
		}
	}
}
