package com.newtifry.pro3;

import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

import static com.newtifry.pro3.CommonUtilities.TAG;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.speech.tts.UtteranceProgressListener;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

@SuppressWarnings("deprecation")
public class NewSpeakService extends Service implements SensorEventListener, TextToSpeech.OnInitListener, OnAudioFocusChangeListener {
	private static TextToSpeech tts = null;
	private static AudioManager audioManager;
	private static Notification speakNotification;
	private final Vector<String> queue = new Vector<String>();
	private boolean initialized = false;
	private boolean temporaryDisable = false;
	private SensorManager sensorMgr = null;
	private long lastUpdate = -1;
	private float x, y, z;
	private float last_x, last_y, last_z;
	private boolean shakeSensingOn = false;
	private final NewSpeakService alternateThis = this;
	private int shakeThreshold = 1500;
	private final HashMap<String, String> parameters = new HashMap<String, String>();
	
	private final String UTTERANCE_ID = "NPUTID";

	
	@Override
	public IBinder onBind( Intent arg0 ) {
		return null;
	}

	@SuppressLint("NewApi")
	public void onInit( int status ) {
		// Prepare parameters.
		this.parameters.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(CommonUtilities.getAudioStream(this, true)));
		this.parameters.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, this.UTTERANCE_ID);

		// Ready.
		Log.d(TAG, "TTS init complete...");
		if( status == TextToSpeech.SUCCESS ) {
			// new
			Locale locale = Locale.getDefault();
            // check if language is available
            switch (tts.isLanguageAvailable(locale)) {
                case TextToSpeech.LANG_AVAILABLE:
                case TextToSpeech.LANG_COUNTRY_AVAILABLE:
                case TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE:
                    Log.d(TAG, "SUPPORTED");
                    tts.setLanguage(locale);
                    //pass the tts back to the main
                    //activity for use
                    break;
                case TextToSpeech.LANG_MISSING_DATA:
                    Log.d(TAG, "MISSING_DATA");
					Log.d(TAG, "require data...");
					Intent installIntent = new Intent();
					installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
					installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(installIntent);
					return;
                case TextToSpeech.LANG_NOT_SUPPORTED:
                    Log.d(TAG, "NOT SUPPORTED");
					return;
            }
//////
	        if (Build.VERSION.SDK_INT >= 15) {
	            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
	                @Override
	                public void onDone(String utteranceId) {
	                    onDoneSpeaking(utteranceId);
	                }

	                @Override
	                public void onError(String utteranceId) {
	                }

	                @Override
	                public void onStart(String utteranceId) {
	                }
	            });
	        } else {
	            tts.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {
	                @Override
	                public void onUtteranceCompleted(String utteranceId) {
	                    onDoneSpeaking(utteranceId);
	                }
	            });
	        }
/////////			
			// Empty out the queue.
			synchronized( queue ) {
				initialized = true;

				for( String message : queue ) {
					// Log.d("Newtifry", "Speaking queued message: " + message);
					this.speak(message);
				}

				queue.clear();
			}
		} else {
			Log.d(TAG, "Init failure - probably missing data.");
		}
	}

    private void onDoneSpeaking(String utteranceId) {
		if (utteranceId.equals(this.UTTERANCE_ID)) {
			audioManager.abandonAudioFocus(this);
		}
    }
    
    private void speak(String message) {
        if (audioManager.requestAudioFocus(this,
        		CommonUtilities.getAudioStream(this, true),
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
    		tts.speak(message, TextToSpeech.QUEUE_ADD, this.parameters);
        }
    }
    
    private void stopSpeak() {
		tts.stop();
		audioManager.abandonAudioFocus(this);
		stopForeground(true);
	}

	private final Handler sensorOffHandler = new Handler() {
		public void handleMessage( Message msg ) {
			if( sensorMgr != null ) {
				sensorMgr.unregisterListener(alternateThis);
				sensorMgr = null;
			}
			shakeSensingOn = false;
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(notificationManager) : "";
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
		speakNotification = notificationBuilder.setOngoing(true)
				.setSmallIcon(R.drawable.ic_stat_statusbar_newtifrypro2)
				.setPriority(NotificationCompat.PRIORITY_MIN)
				.setCategory(NotificationCompat.CATEGORY_SERVICE)
				.build();

		// Listen to the phone call state.
		//TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		//tm.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);

		// Create the TTS object.
		tts = new TextToSpeech(this, this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
	}
	@RequiresApi(Build.VERSION_CODES.O)
	private String createNotificationChannel(NotificationManager notificationManager){
		String channelId = "new_speak_service_channelid";
		String channelName = "NewSpeakForegroundService";
		NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
		// omitted the LED color
		channel.setImportance(NotificationManager.IMPORTANCE_NONE);
		channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
		notificationManager.createNotificationChannel(channel);
		return channelId;
	}


	@Override
	public int onStartCommand( final Intent intent, int flags, int startId )	{
		int result = super.onStartCommand(intent, flags, startId);
		startForeground(CommonUtilities.ID_SPEAK_SERVICE, speakNotification);
		// Deal with the weird null intent issue.
		if( intent == null ) {
			stopForeground(true);
			return result;
		}
		
		if( temporaryDisable) {
			// Temporarily disabled - don't speak.
			Log.d(TAG, "IN CALL - NOT READING OUT");
			stopForeground(true);
			return result;
		}


		boolean stopNow = intent.getBooleanExtra(CommonUtilities.STOP_SPEAK, false);
		if( stopNow) {
			if (tts.isSpeaking()) {
				// Stop reading now.
				Log.d(TAG, "Got stop request... asking TTS to stop.");
				stopSpeak();
			}
			stopForeground(true);
			return result;
		}

		if(Preferences.getShakeToStop(this) ) {
			// Shake to stop is on - kick off the listener for N seconds,
			// if not already running.
			if( !this.shakeSensingOn ) {
				try	{
					this.shakeThreshold = Integer.parseInt(Preferences.getShakeThreshold(this));
				} catch( NumberFormatException ex )	{
					this.shakeThreshold = 1500;
				}
				Integer shakeWaitTime = 60;
				try {
					shakeWaitTime = Integer.parseInt(Preferences.getShakeWaitTime(this));
				} catch( NumberFormatException ex )	{
					// Invalid - ignore.
				}
				this.sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
				boolean accelSupported = this.sensorMgr.registerListener(
						this,
						this.sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
						SensorManager.SENSOR_DELAY_GAME,
						null);

				if( !accelSupported ) {
					// no accelerometer on this device
					this.sensorMgr.unregisterListener(this);
					Log.d(TAG, "No acceleromters on this device.");
				} else {
					// Register a task to stop us in N seconds.
					this.shakeSensingOn = true;
					sensorOffHandler.sendMessageDelayed(Message.obtain(), shakeWaitTime * 1000);
				}
			}
		}

		// Send along the text.
		String text = intent.getExtras().getString("text");
		Log.d(TAG, "Got intent to read message: " + text);
	
		// Also change the audio stream if needed.
		this.parameters.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(CommonUtilities.getAudioStream(this, true)));

		// Why do we do this weird queue thing for the onInit call to work with?
		// That's because if we call tts.speak() before it's initialized nothing
		// happens. So the first message would 'disappear' and not be spoken.
		// So, we queue those until the onInit() has run, and onInit() then
		// clears the queue out.
		// Once it's initialized, we then speak it normally without adding
		// it to the queue.
		synchronized( this.queue ) {
			if(!this.initialized) {
				this.queue.add(text);
				Log.d(TAG, "Not initialised, so queueing.");
				stopForeground(true);
			} else {
				Log.d(TAG, "Initialized, reading out...");
				this.speak(text);
				stopForeground(true);
			}
		}
		return result;
	}

	@Override
	public void onDestroy() {
		synchronized( this.queue ) {
			tts.stop();
			tts.shutdown();
			this.initialized = false;
		}
	}

	public void onAccuracyChanged( Sensor sensor, int accuracy ) {
		// Oh well!
	}

	public void onSensorChanged( SensorEvent event ) {
		// This detection routine is heavily based on that found at:
		// http://www.codeshogun.com/blog/2009/04/17/how-to-detect-shake-motion-in-android-part-i/
		// Log.d("Newtifry", "onSensorChanged: " + event);
		// Log.d("Newtifry", "type " + event.sensor.getType() + " wanted " +
		// Sensor.TYPE_ACCELEROMETER);
		// Log.d("Newtifry", "data " + event.values[SensorManager.DATA_X] + " " +
		// event.values[SensorManager.DATA_Y] + " " +
		// event.values[SensorManager.DATA_Z]);
		if( event.sensor.getType() == Sensor.TYPE_ACCELEROMETER ) {
			long curTime = System.currentTimeMillis();
			// only allow one update every 100ms.
			if( (curTime - lastUpdate) > 100 ) {
				long diffTime = (curTime - lastUpdate);
				lastUpdate = curTime;
				x = event.values[SensorManager.DATA_X];
				y = event.values[SensorManager.DATA_Y];
				z = event.values[SensorManager.DATA_Z];

				float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

				// Log.d("Newtifry", "diff: " + diffTime + " - speed: " + speed);
				if( speed > this.shakeThreshold ) {
					Log.i("NewtifryPro", "Shake detected with speed: " + speed);

					// Tell this service to stop.
					CommonUtilities.stopSpeak(getBaseContext());
					// Stop detected shake.
					sensorOffHandler.sendMessageDelayed(Message.obtain(), 0);
				}
				last_x = x;
				last_y = y;
				last_z = z;
			}
		}
	}

	// Based on
	// http://www.androidsoftwaredeveloper.com/2009/04/20/how-to-detect-call-state/
	private final PhoneStateListener mPhoneListener = new PhoneStateListener() {
		public void onCallStateChanged( int state, String incomingNumber ) {
			switch( state ) {
				case TelephonyManager.CALL_STATE_RINGING:
				case TelephonyManager.CALL_STATE_OFFHOOK:
					// Stop any speaking now!
					stopSpeak();
					// And temporarily disable.
					temporaryDisable = true;
					// Log.d("Newtifry", "DISABLING - In call state.");
					break;
				case TelephonyManager.CALL_STATE_IDLE:
					// Re-enable the service.
					// Log.d("Newtifry", "Enabling temp disable again...");
					temporaryDisable = false;
					break;
				default:
					break;
			}
		}
	};

	@Override
	public void onAudioFocusChange(int focusChange) {
		switch(focusChange ) {
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
		case AudioManager.AUDIOFOCUS_LOSS:
			// Stop playback as we cannot pause tts
			stopSpeak();
			break;
		case AudioManager.AUDIOFOCUS_GAIN :
			break;
        }		
	}
}