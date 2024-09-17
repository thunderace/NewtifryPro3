package com.newtifry.pro3.database;


import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.newtifry.pro3.urlimageviewhelper.UrlImageViewHelper;
import com.newtifry.pro3.CommonUtilities;
import com.newtifry.pro3.NewtifryPro2App;
import com.newtifry.pro3.Preferences;
import com.newtifry.pro3.shared.NewtifryProHelper;
import com.newtifry.pro3.utils.UniversalNotificationManager;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.Base64;
import android.util.Log;

public class NewtifryMessage2 {
	public final static int IMAGE_NOT_LOADED = 0;
	public final static int IMAGE_LOADING    = 1;
	public final static int IMAGE_LOADING_ERROR = 2;
	public final static int IMAGE_LOADED = 3;

	private static long lastMessageId = -1;
	
	private long id = -1;
	
	private String title;
	private String timestamp;
	private String otherTimestamp;
	private String messageContent;
	private String url;
	private boolean seen = false;
	private int priority;
	private boolean noCache = false;
	private String sourceName;
	private final boolean crypted = false;
	private boolean deleted = false;
	private boolean sticky = false;
	private boolean locked = false;
	private String[] imageList = null;
	private int[] imageLoadStatus = null;
	// not persistent
	private int notify = -1; // -1 configuration - 0 : don't notify - 1 : force notify  
	private int speak = -1;  // -1 configuration - 0 : don't speak - 1 : force speak
	private int imageCount = 0;
	private long epoch = 0;
	private int hash = 0;
	private int hashCount = 0;

//	public static String md5(String input) {
//		String result = input;
//		if (input != null) {
//			MessageDigest md;
//			try {
//				md = MessageDigest.getInstance("MD5");
//				md.update(input.getBytes("UTF-8"));
//			} catch (NoSuchAlgorithmException e) {
//				return null;
//			}
//			catch (UnsupportedEncodingException e) {
//				return null;
//			}
//			BigInteger hash = new BigInteger(1, md.digest());
//			result = hash.toString();
//			if ((result.length() %2) != 0) {
//				result = "0" + result;
//			}
//		}
//		return result;
//	}
	
	public void deleteCachedImages(Context context) {
		if (imageList == null) {
			return;
		}
		for (int i = 0; i < 5; i++) {
			if (imageList[i] != null) {
				UrlImageViewHelper.remove(context, imageList[i]);
			}
		}
	}
	
	public boolean isLocked() {
		return 	this.locked;
	}
	
	public void setLocked(boolean lock) {
		this.locked = lock;
	}
	
	public void setNotify(int notify) {
		if (notify == -1 || notify == 0 || notify == 1) {
			this.notify = notify;
		}
	}

	public int getNotify() {
		return this.notify;
	}
	
	public String getSourceName() {
		return this.sourceName;
	}

	public void setSourceName(String sourceName) {
		this.sourceName = sourceName;
	}

	public boolean getCrypted() {
		return this.crypted;
	}
	
	public void setNoCache(boolean noCache) {
		this.noCache = noCache;
	}

	public boolean getNoCache() {
		return this.noCache;
	}
	
	public void setDeleted(boolean deleted) {
		if (this.deleted != deleted && !deleted && this.getId() != -1) {
			Intent smartwatchIntent = new Intent(NewtifryProHelper.MESSAGE_NEW);
			smartwatchIntent.putExtra(NewtifryProHelper.IntentExtras.ID,this.getId());
			smartwatchIntent.putExtra(NewtifryProHelper.IntentExtras.TITLE,this.getSourceName() + "-" + this.getTitle());
			smartwatchIntent.putExtra(NewtifryProHelper.IntentExtras.MESSAGE,this.getMessage());
			smartwatchIntent.putExtra(NewtifryProHelper.IntentExtras.TIMESTAMP,this.getTimestamp());
			NewtifryPro2App.getContext().sendBroadcast(smartwatchIntent);
		}
		this.deleted = deleted;
	}

	public boolean isDeleted() {
		return this.deleted;
	}

	public void setSticky(boolean sticky) {
		this.sticky = sticky;
	}

	public boolean getSticky() {
		return this.sticky;
	}
	
	public String getTitle() {
		return this.title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		if (priority > 3) 
			priority = 3;
		if (priority < -1)
			priority = -1;
		this.priority = priority;
	}
	
	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		if (timestamp == null) {
			SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
			ISO8601DATEFORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
			this.timestamp = ISO8601DATEFORMAT.format(new Date()); 
		} else {
			this.timestamp = timestamp;
		}
	}
	public boolean setEpoch(long value) {
		if (value == 0 ) {
			try {
				this.epoch = parseISO8601String(this.timestamp).getTime();
			} catch (ParseException e) {
				this.epoch = 0;
				return false;
			}
//			save(getApplicationContext());
		} else {
			this.epoch = value;
		}
		return true;
	}
	
	public long getEpoch() {
		if (this.epoch == 0) {
			setEpoch(0);
		}
		return this.epoch;
	}
/*
	public long getSimpleEpoch() {
		return this.epoch;
	}
*/
	public int computeHash() {
		String data = "";
		if (getSourceName() != null) {
			data=getSourceName();
		}
		if (getTitle() != null) {
			data+=getTitle();
		}
		if (getMessage() != null) {
			data+=getMessage();
		}
		if (getUrl() != null) {
			data+=getUrl();
		}
		for (int i = 0; i < 5; i++) {
			if (getImage(i) != "") {
				data += getImage(i);
			}
		}
		return data.hashCode();
	}

	public int getHash() {
		if (this.hash == 0) {
			this.hash = computeHash();
		}
		return this.hash;
	}

	public void setHash(int hash) {
		this.hash = hash;
	}

	public int getHashCount() {
		return this.hashCount;
	}

	public void setHashCount(int hashCount) {
		this.hashCount = hashCount;
	}


	public static Date parseISO8601String(String isoString) throws ParseException {
		Date date;
		SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
		try {
			ISO8601DATEFORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
			date = ISO8601DATEFORMAT.parse(isoString); 
		} catch(Exception e) {
			// invalid date : use the local date
			date = ISO8601DATEFORMAT.parse(ISO8601DATEFORMAT.format(new Date())); 
		}
		return date;
	}
/*
	public static String formatUTCAsLocal(Date date) {
		DateFormat formatter = DateFormat.getDateTimeInstance();
		formatter.setTimeZone(TimeZone.getDefault());
		return formatter.format(date);
	}
	*/
	public static String formatUTCAsLocal(Date date) {
		boolean format24 = android.text.format.DateFormat.is24HourFormat(NewtifryPro2App.getContext());
		SimpleDateFormat simpleDateFormat;
		if (!format24) {
			simpleDateFormat = new SimpleDateFormat("d MMM yyyy h:mm:ss a");
		} else {
			simpleDateFormat = new SimpleDateFormat("d MMM yyyy HH:mm:ss");
		}
		return simpleDateFormat.format(date);
	}

/*
        DateFormat formatter = DateFormat.getDateTimeInstance();
        formatter.setTimeZone(TimeZone.getDefault());
        return formatter.format(date);
*/

	private static final DateFormat TWELVE_TF = new SimpleDateFormat("hh:mm:ss a");
	// Replace with kk:mm if you want 1-24 interval
	private static final DateFormat TWENTY_FOUR_TF = new SimpleDateFormat("HH:mm:ss");

	public static String formatUTCAsLocal(String isoString) throws ParseException {
		Date date;
		SimpleDateFormat ISO8601DATEFORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		try {
			ISO8601DATEFORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
			date = ISO8601DATEFORMAT.parse(isoString);
		} catch(Exception e) {
			// invalid date : use the local date
			date = ISO8601DATEFORMAT.parse(ISO8601DATEFORMAT.format(new Date()));
		}
        DateFormat formatter = DateFormat.getDateTimeInstance();
        formatter.setTimeZone(TimeZone.getDefault());
        String oldString = formatter.format(date);
		DateFormat dateformatter = DateFormat.getDateInstance();
		String day = dateformatter.format(date);
		String H24 =  TWENTY_FOUR_TF.format(date);
		String H12AMPM =  TWELVE_TF.format(date);
		DateFormat timeformatter = DateFormat.getTimeInstance();
		timeformatter.setTimeZone(TimeZone.getDefault());
		String defaulTime = timeformatter.format(date);

		String datetimeformat = Preferences.getDatetimeFormat(NewtifryPro2App.getContext());
		String finalString;
		finalString =  day + " " + defaulTime;
		if (datetimeformat.equals("24H")) {
			finalString =  day + " " + H24;
		}
		if (datetimeformat.equals("12H AM/PM")) {
			finalString = day + " " + H12AMPM;
		}
		return finalString;
		/*
		DateFormat formatter = DateFormat.getDateTimeInstance();
		formatter.setTimeZone(TimeZone.getDefault());
		return formatter.format(date);
		*/
	}

	public String getDisplayTimestamp() {
		try {
			return NewtifryMessage2.formatUTCAsLocal(NewtifryMessage2.parseISO8601String(this.timestamp));
		} catch( ParseException e ) {
			return "";
		}
	}
/*
	public String getDisplayTimestampNew() {
		try {
			return NewtifryMessage2.formatUTCAsLocal(this.timestamp);
		} catch( ParseException e ) {
			return "";
		}
	}
*/
	public Spanned getSpannedMessage() {
		try {
			return Html.fromHtml(this.messageContent);
		} catch (Exception ex) {
			Log.d("NP", "not html message");
			return new SpannableString(messageContent);
		}
	}

	public String getMessage() {
		if (this.messageContent == null) {
			return "";
		}
		return this.messageContent;
	}

	public String getTextMessage() {
		try {
			return Html.fromHtml(this.messageContent).toString();
		} catch (Exception ex) {
			return this.messageContent;
		}
	}

	public void setMessage(String message) {
		this.messageContent = message;
	}

	public String getUrl() {
		return this.url;
	}
	
	public void setUrl(String url) {
		if (url != null) {
			this.url = url;
		}
	}

	public void setOtherTimestamp(String timestamp) {
			otherTimestamp = timestamp;
	}
	public String getOtherTimestamp() {
		return otherTimestamp;
	}

	public void setImage(int index, String image) {
		if (imageList == null) {
			imageList = new String[5];
			imageLoadStatus = new int[5];
		}
		imageLoadStatus[index] = IMAGE_NOT_LOADED;
		if (!CommonUtilities.isEmpty(image)) {
			imageList[index] = image;
			imageCount++;
		}
	}

	public void setImageLoadingStatus(int index, int status) {
		if (imageLoadStatus == null) {
			return;
		}
		if (status < IMAGE_NOT_LOADED || status > IMAGE_LOADED) {
			return;
		}
		imageLoadStatus[index] = status;
	}

	
	public int getImageLoadingStatus(int index) {
		if (imageLoadStatus == null) {
			return IMAGE_NOT_LOADED;
		}
		return imageLoadStatus[index];
	}
	public String getImage(int index) {
		if (imageList == null) {
			return "";
		}
		return this.imageList[index];
	}

	public String[] getImageList() {
		return imageList;
	}

	public int getImageCount() {
		return imageCount;
	}
	
	private Boolean getSeen() {
		return seen;
	}

	public void setSeen(Boolean seen) {
		if (this.seen != seen && this.getId() != -1) {
			Intent smartwatchIntent = new Intent(seen ? NewtifryProHelper.MESSAGE_SEEN : NewtifryProHelper.MESSAGE_UNSEEN);
			smartwatchIntent.putExtra(NewtifryProHelper.IntentExtras.ID,this.getId());
			NewtifryPro2App.getContext().sendBroadcast(smartwatchIntent);
		}
		this.seen = seen;
	}

	public int getSpeak() {
		return this.speak;
	}

	public void setSpeak(int speak) {
		if (speak == -1 || speak == 0 || speak == 1) {
			this.speak = speak;
		}
	}

//	public String decrypt(String input) {
//		if (this.crypted == false || input == null) {
//			return input;
//		}
//		byte[] result = null;
//		try {
//			result = Base64.decode(new String(NewtifryProSecure.decrypt(input, Preferences.getPassword(), CommonUtilities.iv ), "UTF-8"), Base64.DEFAULT);
//		} catch (UnsupportedEncodingException e) {
//			result = input.getBytes();
//		} catch (Exception e) {
//			result = input.getBytes();
//		}
//		return new String(result);
//	}

	public static String decode(String input, boolean base64) {
        if (!base64) {
            return input;
        }
		if( input != null ) {
			byte[] result = null;
			try {
				result = Base64.decode(input, Base64.DEFAULT);
			} catch (Exception ex) {
				return null;
			}
			return new String(result);
		} else {
			return null;
		}
	}


	public static NewtifryMessage2 fromDebug(String source, String title, int priority, int sticky, int deleted) {
		NewtifryMessage2 incoming = new NewtifryMessage2();
		incoming.setMessage("message de test interne");
		incoming.setTitle(title);
		incoming.setSourceName(source);
		incoming.setMessage("<h3quam ut esse</h3><p>ex fieri quaerant <b><font color='red'>mulierculae</font> benevolentiae </b>perstrinxi <font color='blue'>ut quam firmitatis ii ii ex </font></p><p>firmitatis etiam</p>");
		incoming.setUrl("http://newtifry.appspot.com");	
		incoming.setImage(0, "https://raw.githubusercontent.com/thunderace/NewtifryPro/master/images/test_newtifry2.png");
		incoming.setTimestamp(null); // will set to the current timestamp
		incoming.setPriority(priority);
		incoming.setSeen(false);
		incoming.setSticky(false);
		incoming.setNoCache(false);
		if (sticky == 1) {
			incoming.setSticky(true);
		}
		incoming.setDeleted(false);
		return incoming;
	}

	
	public static NewtifryMessage2 fromFCM(Map data, boolean base64) {
		NewtifryMessage2 incoming = new NewtifryMessage2();
//		boolean crypted = false;
//		if (extras.containsKey("crypted")){
//			crypted = true;
//		}
		if (!data.containsKey("title")) {
			return null;
		}
		incoming.setTitle(NewtifryMessage2.decode(data.get("title").toString(), base64));
		if (data.containsKey("message")) {
			incoming.setMessage(NewtifryMessage2.decode(data.get("message").toString(), base64));
		}
		if (data.containsKey("url")) {
			incoming.setUrl(NewtifryMessage2.decode(data.get("url").toString(), base64));
		}
		if (data.containsKey("image")) {
			incoming.setImage(0, NewtifryMessage2.decode(data.get("image").toString(), base64));
		} else {
			int idx = 0;
			for (int i = 1; i < 6; i++) {
				if (data.containsKey("image" + i)) {
					incoming.setImage(idx, NewtifryMessage2.decode(data.get("image" + i).toString(), base64));
					idx++;
				}
			}
		}
		if (data.containsKey("timestamp")) {
			incoming.setTimestamp(data.get("timestamp").toString());
		} else {
			incoming.setTimestamp(null);
		}
		
		if (data.containsKey("nocache")){
			incoming.setNoCache(true);
		}

		if (data.containsKey("state")){
			int state = 0;
			try {
				state = Integer.parseInt(NewtifryMessage2.decode(data.get("state").toString(), base64));
			}catch( NumberFormatException ex ) {
			}
			switch (state) {
				case 1:
					incoming.setSticky(true);
					break;
				case 2:
					incoming.setLocked(true);
					break;
				default:
				case 0:
					break;
			}
		}

		if (data.containsKey("speak")){
			try {
				int speak = Integer.parseInt(NewtifryMessage2.decode(data.get("speak").toString(), base64));
				if (speak == 0 || speak == 1) {
					incoming.setSpeak(speak);
				}
			}catch( NumberFormatException ex ) {
			}
		}
		
		if (data.containsKey("NPpriority")){
			try {
				int priority = Integer.parseInt(NewtifryMessage2.decode(data.get("NPpriority").toString(), base64));
				incoming.setPriority(priority);
			}catch( NumberFormatException ex ) {
				incoming.setPriority(0);
			}
		}

		if (data.containsKey("notify")){
			try {
				int notify = Integer.parseInt(NewtifryMessage2.decode(data.get("notify").toString(), base64));
				if (notify == 1 || notify == 0) {
					incoming.setNotify(notify);
				}
			}catch( NumberFormatException ex ) {
			}
		}
		
		// try to get sourcename
		if (data.containsKey("source")) {
			incoming.setSourceName(NewtifryMessage2.decode(data.get("source").toString(), base64));
		}
		incoming.setSeen(false);
		incoming.setDeleted(false);

		return incoming;
	}

	public static ArrayList<NewtifryMessage2> getUnreadMessages(Context context, int limit) {
		
		return NewtifryMessage2.genericList(context, 
													NewtifryDatabase.KEY_SEEN + "=0 AND " + NewtifryDatabase.KEY_DELETED + "=0", 
													null, 
													NewtifryDatabase.KEY_ID + " DESC LIMIT " + limit);
	}
	
	private static int countInvisible( Context context ) {
		String query = NewtifryDatabase.KEY_PRIORITY + " < 0 ";
		return NewtifryMessage2.genericCount(context, query, null);
	}

	public int countSticky( Context context ) {
		String query = NewtifryDatabase.KEY_STICKY + " = 1 ";
		if (!Preferences.showInvisibleMessages(context)) {
			query +=  " AND "+ NewtifryDatabase.KEY_PRIORITY + " >= 0";
		}
		return NewtifryMessage2.genericCount(context, query, null);
	}
		
	public static int countUnread( Context context ) {
		String query = NewtifryDatabase.KEY_SEEN + " = 0 AND " + NewtifryDatabase.KEY_DELETED + "= 0";
		if (!Preferences.showInvisibleMessages(context)) {
			query +=  " AND "+ NewtifryDatabase.KEY_PRIORITY + " >= 0";
		}

		return NewtifryMessage2.genericCount(context, query, null);
	}

	public static int count( Context context ) {
		String query = NewtifryDatabase.KEY_DELETED + " = 0 ";
		if (!Preferences.showInvisibleMessages(context)) {
			query +=  " AND "+ NewtifryDatabase.KEY_PRIORITY + " >= 0";
		}
		return NewtifryMessage2.genericCount(context, query, null);
	}


	private ContentValues flatten() {
		ContentValues values = new ContentValues();
		values.put(NewtifryDatabase.KEY_TITLE, this.getTitle());
		values.put(NewtifryDatabase.KEY_SOURCENAME, this.getSourceName());
		values.put(NewtifryDatabase.KEY_MESSAGE, this.getMessage());
		values.put(NewtifryDatabase.KEY_URL, this.getUrl());
		values.put(NewtifryDatabase.KEY_IMAGE1, this.getImage(0));
		values.put(NewtifryDatabase.KEY_IMAGE2, this.getImage(1));
		values.put(NewtifryDatabase.KEY_IMAGE3, this.getImage(2));
		values.put(NewtifryDatabase.KEY_IMAGE4, this.getImage(3));
		values.put(NewtifryDatabase.KEY_IMAGE5, this.getImage(4));
		values.put(NewtifryDatabase.KEY_TIMESTAMP, this.getTimestamp());
		values.put(NewtifryDatabase.KEY_PRIORITY, this.getPriority());
		values.put(NewtifryDatabase.KEY_SEEN, this.getSeen() ? 1 : 0);
		values.put(NewtifryDatabase.KEY_FORCEIMAGE, 0);
		values.put(NewtifryDatabase.KEY_NOCACHE, this.getNoCache() ? 1 : 0);
		values.put(NewtifryDatabase.KEY_DELETED, this.isDeleted() ? 1 : 0);
		values.put(NewtifryDatabase.KEY_STICKY, this.getSticky() ? 1 : 0);
		values.put(NewtifryDatabase.KEY_LOCKED, this.isLocked() ? 1 : 0);
		values.put(NewtifryDatabase.KEY_EPOCH, this.getEpoch());
		values.put(NewtifryDatabase.KEY_HASH, this.getHash());
		values.put(NewtifryDatabase.KEY_HASHCOUNT, this.getHashCount());
		values.put(NewtifryDatabase.KEY_OTHERTIMESTAMP, this.getOtherTimestamp());
		return values;
	}

	protected static NewtifryMessage2 inflate(Cursor cursor )	{
		NewtifryMessage2 message = new NewtifryMessage2();
		message.setId(cursor.getLong(cursor.getColumnIndex(NewtifryDatabase.KEY_ID)));
		message.setTitle(cursor.getString(cursor.getColumnIndex(NewtifryDatabase.KEY_TITLE)));
		message.setSourceName(cursor.getString(cursor.getColumnIndex(NewtifryDatabase.KEY_SOURCENAME)));
		message.setMessage(cursor.getString(cursor.getColumnIndex(NewtifryDatabase.KEY_MESSAGE)));
		message.setUrl(cursor.getString(cursor.getColumnIndex(NewtifryDatabase.KEY_URL)));
		message.setImage(0, cursor.getString(cursor.getColumnIndex(NewtifryDatabase.KEY_IMAGE1)));
		message.setImage(1, cursor.getString(cursor.getColumnIndex(NewtifryDatabase.KEY_IMAGE2)));
		message.setImage(2, cursor.getString(cursor.getColumnIndex(NewtifryDatabase.KEY_IMAGE3)));
		message.setImage(3, cursor.getString(cursor.getColumnIndex(NewtifryDatabase.KEY_IMAGE4)));
		message.setImage(4, cursor.getString(cursor.getColumnIndex(NewtifryDatabase.KEY_IMAGE5)));
		message.setSeen(cursor.getLong(cursor.getColumnIndex(NewtifryDatabase.KEY_SEEN)) != 0);
		message.setTimestamp(cursor.getString(cursor.getColumnIndex(NewtifryDatabase.KEY_TIMESTAMP)));
		message.setPriority(cursor.getInt(cursor.getColumnIndex(NewtifryDatabase.KEY_PRIORITY)));
		message.setNoCache(cursor.getLong(cursor.getColumnIndex(NewtifryDatabase.KEY_NOCACHE)) != 0);
		message.setDeleted(cursor.getLong(cursor.getColumnIndex(NewtifryDatabase.KEY_DELETED)) != 0);
		message.setSticky(cursor.getLong(cursor.getColumnIndex(NewtifryDatabase.KEY_STICKY)) != 0);
		message.setLocked(cursor.getInt(cursor.getColumnIndex(NewtifryDatabase.KEY_LOCKED)) != 0);
		message.setEpoch(cursor.getLong(cursor.getColumnIndex(NewtifryDatabase.KEY_EPOCH)));
		message.setHash(cursor.getInt(cursor.getColumnIndex(NewtifryDatabase.KEY_HASH)));
		message.setHashCount(cursor.getInt(cursor.getColumnIndex(NewtifryDatabase.KEY_HASHCOUNT)));
		message.setOtherTimestamp(cursor.getString(cursor.getColumnIndex(NewtifryDatabase.KEY_OTHERTIMESTAMP)));

		return message;
	}

	public static long getLastMessageId(Context context) {
		if ( NewtifryMessage2.lastMessageId == -1) {
			String query = NewtifryDatabase.KEY_DELETED + " = 0";
			if (!Preferences.showInvisibleMessages(context)) {
				query +=  " AND "+ NewtifryDatabase.KEY_PRIORITY + " >= 0";
			}

			Cursor cursor = context.getContentResolver().query(NewtifryProvider.CONTENT_URI_MESSAGES,  NewtifryDatabase.MESSAGE_PROJECTION, query, null, null);
			if (cursor != null && cursor.moveToFirst()) {
			    long id = cursor.getLong(0); //The 0 is the column index, we only have 1 column, so the index is 0
				cursor.close();
				return id;
			}
		}
		return NewtifryMessage2.lastMessageId;
	}
	
	//ORM
	public void save( Context context ) {
		if( this.getId() == -1 ) { // new message
			getHash();
			// search for same hashcode
			// count same hash

			//String query = NewtifryDatabase.KEY_HASH + " = " + Integer.toString(this.hash) + " AND " + NewtifryDatabase.KEY_DELETED + "=0";
			//int count = NewtifryMessage2.genericCount(context, query, null);
			//this.setHashCount(count);
			// delete old ones
			//NewtifryMessage2.genericDelete(context,  NewtifryDatabase.KEY_HASH + "=" + Integer.toString(this.hash) , null);
			if (Preferences.groupMessages(context)) {
				ArrayList<NewtifryMessage2> messageList =
						NewtifryMessage2.genericList(context,
								NewtifryDatabase.KEY_HASH + "=" + this.hash + " AND " + NewtifryDatabase.KEY_DELETED + "=0",
								null,
								NewtifryDatabase.KEY_ID + " DESC LIMIT 1");
				if (messageList.size() == 1) {
					this.setHashCount(messageList.get(0).getHashCount() + 1);
					String otherTimestamp = messageList.get(0).getDisplayTimestamp();
					if (messageList.get(0).getOtherTimestamp() != null) {
						otherTimestamp += "\n" + messageList.get(0).getOtherTimestamp();
					}
					this.setOtherTimestamp(otherTimestamp);
				} else {
					this.setHashCount(1);
				}
				// delete and count unread message with same hash
				int unreadDeletedCount = NewtifryMessage2.genericDelete(context, NewtifryDatabase.KEY_HASH + "=" + this.hash + " AND " + NewtifryDatabase.KEY_SEEN + "=0", null);
				// delete over
				NewtifryMessage2.genericDelete(context, NewtifryDatabase.KEY_HASH + "=" + this.hash + " AND " + NewtifryDatabase.KEY_SEEN + "=1", null);
				UniversalNotificationManager.getInstance(context).decreaseNewMessagesCount(unreadDeletedCount);
			} else {
				this.setHashCount(1);
			}

			// Insert.
			ContentValues values = this.flatten();
			Uri uri = context.getContentResolver().insert(NewtifryProvider.CONTENT_URI_MESSAGES, values);
			NewtifryMessage2.lastMessageId = Long.parseLong(uri.getPathSegments().get(1)); //colonne 1
			this.setId(lastMessageId);
			
			// message cleanup
//			purge(context);
//			deleteOlderThan(context);
//			purgeInvisible(context);
		} else {
			// Update.
			ContentValues values = this.flatten();
			context.getContentResolver().update(ContentUris.withAppendedId(NewtifryProvider.CONTENT_URI_MESSAGES, this.getId()), values, NewtifryDatabase.KEY_ID + "=" + this.getId(), null);
		}
	}
	
	/**
	 * Get the ID of this object - NULL if not yet saved.
	 * @return
	 */
	public Long getId()	{
		return this.id;
	}

	/**
	 * Set the ID of this object.
	 * @param id
	 */
	protected void setId( Long id )	{
		this.id = id;
	}
	
	private static int  genericCount( Context context, String selection, String[] selectionArgs )	{
		Cursor cursor = context.getContentResolver().query(NewtifryProvider.CONTENT_URI_MESSAGES,  NewtifryDatabase.MESSAGE_PROJECTION, selection, selectionArgs, null);
		int count = cursor.getCount();
		cursor.close();
		return count;
	}	

	private static int genericDelete( Context context, String selection, String[] selectionArgs )	{
		return context.getContentResolver().delete(NewtifryProvider.CONTENT_URI_MESSAGES, selection, selectionArgs);
	}	
	
	
	public static NewtifryMessage2 get( Context context, Long id ) {
		ArrayList<NewtifryMessage2> list = NewtifryMessage2.genericList(context, NewtifryDatabase.KEY_ID + "=" + id, null, null);
		
		if( list.size() == 0 ) {
			return null;
		} else {
			return list.get(0);
		}
	}

	private static ArrayList<NewtifryMessage2> genericList( Context context, String selection, String[] selectionArgs, String sortOrder )
	{
		Cursor cursor = context.getContentResolver().query(NewtifryProvider.CONTENT_URI_MESSAGES, NewtifryDatabase.MESSAGE_PROJECTION, selection, selectionArgs, sortOrder);
		ArrayList<NewtifryMessage2> result = new ArrayList<NewtifryMessage2>();
		if(cursor.moveToFirst()) {
			do {
				result.add(NewtifryMessage2.inflate(cursor));
			}
			while( cursor.moveToNext() );
		}
		cursor.close();
		return result;
	}
	
	protected static void purgeInvisible (Context context) {
		if (Preferences.showInvisibleMessages(context)) {
			return;
		}
		int totalCount = countInvisible(context);
		if (totalCount > 20 ) {
			int toDelete = totalCount - 10;
			ArrayList<NewtifryMessage2> messageList = 
					NewtifryMessage2.genericList(context, 
														NewtifryDatabase.KEY_PRIORITY + "<0", 
														null, 
														NewtifryDatabase.KEY_ID + " ASC LIMIT " + toDelete);
	        for (NewtifryMessage2 message: messageList) {
	        	NewtifryProvider.deleteItem(context, message.getId());
	        }
		}
	}

    public static void purgeAll(Context context) {
	    try {
		    NewtifryMessage2.purge(context);
		    NewtifryMessage2.deleteOlderThan(context);
		    NewtifryMessage2.purgeInvisible(context);
	    } catch (Exception e) {

	    }
	}
	
	public static void deleteOlderThan( Context context) {
		// now purge
		Integer maxDays = 0;
		try {
			maxDays = Integer.parseInt(Preferences.getAutoCleanMessagesDays(context));
		} catch( NumberFormatException ex ) {
			return;
		}	
		if (maxDays <= 0) { 
			return;
		}
		
		Date olderThan = new Date();
		olderThan.setTime(olderThan.getTime() - (maxDays *  86400 * 1000));

		// Parse it, and display in LOCAL timezone.
		// So, everything older than formattedDate should be removed.
		NewtifryMessage2.genericDelete(context,  NewtifryDatabase.KEY_LOCKED + "=0 AND " + NewtifryDatabase.KEY_STICKY + "=0 AND " + NewtifryDatabase.KEY_EPOCH + "!=0 AND " + NewtifryDatabase.KEY_EPOCH + " <" + olderThan.getTime(), null);
	}	
	public static void purge( Context context) {
		Integer maxMessageCount = 0;
		try {
			maxMessageCount = Integer.parseInt(Preferences.getMaxMessageCount(context));
		} catch( NumberFormatException ex ) {
			return;
		}
		if (maxMessageCount == 0) {
			return;
		}
		int totalCount = NewtifryMessage2.count(context);
		if (totalCount > maxMessageCount) {
			// we have to cleanup message list
			// get genericCount(context, "", null) - intValue first messages ordered by DESC id
			int toDelete = totalCount - maxMessageCount;
			ArrayList<NewtifryMessage2> messageList = 
					NewtifryMessage2.genericList(context, 
//														NewtifryDatabase.KEY_TAGCOUNT + "=0 AND " + NewtifryDatabase.KEY_STICKY + "=0", 
														NewtifryDatabase.KEY_LOCKED + "=0 AND " + NewtifryDatabase.KEY_STICKY + "=0", 
														null, 
														NewtifryDatabase.KEY_ID + " ASC LIMIT " + toDelete);
	        for (NewtifryMessage2 message: messageList) {
	        	NewtifryProvider.deleteItem(context, message.getId());
	        }
			// request refresh (DDB change)
		}
	}

}



