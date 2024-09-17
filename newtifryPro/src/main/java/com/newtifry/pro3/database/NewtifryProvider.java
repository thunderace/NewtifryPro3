package com.newtifry.pro3.database;


import com.newtifry.pro3.CommonUtilities;
import com.newtifry.pro3.NewtifryPro2App;
import com.newtifry.pro3.Preferences;
import com.newtifry.pro3.shared.NewtifryProHelper;
import com.newtifry.pro3.utils.UniversalNotificationManager;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

public class NewtifryProvider extends ContentProvider 
{
	public static final String PROVIDER_NAME_MESSAGES = "com.newtifry.pro3.provider.NewtifryMessages";
    private static final String NEWTIFRYPRO_BASE_PATH = "messages";

    public static final Uri CONTENT_URI_MESSAGES = Uri.parse("content://"+ PROVIDER_NAME_MESSAGES + "/" + NEWTIFRYPRO_BASE_PATH);
    
    private static final int MESSAGES = 1;
    private static final int MESSAGE_ID = 2;    
    
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
    	uriMatcher.addURI(PROVIDER_NAME_MESSAGES, NEWTIFRYPRO_BASE_PATH, MESSAGES);
    	uriMatcher.addURI(PROVIDER_NAME_MESSAGES, NEWTIFRYPRO_BASE_PATH + "/#", MESSAGE_ID);
    }

	private NewtifryDatabase db;
	
    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/vnd.newtifry.messages";
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
            + "/vnd.newtifry.messages";
	
	@Override
	public String getType( Uri uri ) {
		switch( uriMatcher.match(uri)) {
			case MESSAGES:
				return CONTENT_TYPE;
			case MESSAGE_ID:
				return CONTENT_ITEM_TYPE;				
			default:
				throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}
	
	@Override
	public boolean onCreate() {
		Context context = getContext();
		db = new NewtifryDatabase(context);
		return true;
	}
	
	@Override
	public Cursor query( Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) 	{
		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		sqlBuilder.setTables(NewtifryDatabase.DATABASE_TABLE_MESSAGES);
		
		// If it's a single ID matcher, limit to a single 
		switch( uriMatcher.match(uri)) {
			case MESSAGE_ID:
				sqlBuilder.appendWhere(NewtifryDatabase.KEY_ID + " = " + uri.getPathSegments().get(1));
				break;
			case MESSAGES:
				break;
	        default:
	            throw new IllegalArgumentException("Unknown URI");
		}
        
		// Perform the query.
		Cursor cursor = sqlBuilder.query(
                this.db.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);

		// Tell the cursor to listen for changes.
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}
	
	@Override
	public Uri insert( Uri uri, ContentValues values )	{
        int uriType = uriMatcher.match(uri);
        if (uriType != MESSAGES) {
            throw new IllegalArgumentException("Invalid URI for insert");
        }

        SQLiteDatabase sqlDB = this.db.getWritableDatabase();

        try {// Insert into the database...
        	long rowID = sqlDB.insert(NewtifryDatabase.DATABASE_TABLE_MESSAGES, "", values);
        	// And on success...
        	if( rowID > 0 )	{
				onChangeDDB(uri);
        		// Create our return URI.
        		Uri newUri = ContentUris.withAppendedId(uri, rowID);
        		// And notify anyone watching that it's changed.
        		//getContext().getContentResolver().notifyChange(uri, null);
        		//Intent updateUIIntent = new Intent(CommonUtilities.MESSAGE_DDB_CHANGE_INTENT);
                //LocalBroadcastManager.getInstance(getContext()).sendBroadcast(updateUIIntent);
        		return newUri;
        	} else {
                throw new SQLException("Failed to insert row into " + uri);
        	}
        } catch (SQLiteConstraintException e) {
            Log.i(CommonUtilities.TAG, "Ignoring constraint failure.");
        }
        return null;
	}
	
	@Override
	public int update( Uri uri, ContentValues values, String selection, String[] selectionArgs ) {
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = this.db.getWritableDatabase();

        int rowsAffected;

        switch (uriType) {
	        case MESSAGE_ID:
	            String id = uri.getLastPathSegment();
	            StringBuilder modSelection = new StringBuilder(NewtifryDatabase.KEY_ID + "=" + id);
	
	            if (!TextUtils.isEmpty(selection)) {
	                modSelection.append(" AND " + selection);
	            }
	
	            rowsAffected = sqlDB.update(NewtifryDatabase.DATABASE_TABLE_MESSAGES,
	                    values, modSelection.toString(), null);
	            break;
	        case MESSAGES:
	            rowsAffected = sqlDB.update(NewtifryDatabase.DATABASE_TABLE_MESSAGES,
	                    values, selection, selectionArgs);
	            break;
	        default:
	            throw new IllegalArgumentException("Unknown or Invalid URI");
        }
        if (rowsAffected != 0) {
			onChangeDDB(uri);
            //getContext().getContentResolver().notifyChange(uri, null);
            //Intent updateUIIntent = new Intent(CommonUtilities.MESSAGE_DDB_CHANGE_INTENT);
            //LocalBroadcastManager.getInstance(getContext()).sendBroadcast(updateUIIntent);
        }
        return rowsAffected; 		
	}	
	
	@Override
	public int delete( Uri uri, String selection, String[] selectionArgs ) {
		
        int uriType = uriMatcher.match(uri);
        SQLiteDatabase sqlDB = this.db.getWritableDatabase();
        int rowsAffected = 0;
        switch (uriType) {
        case MESSAGES:
            rowsAffected = sqlDB.delete(NewtifryDatabase.DATABASE_TABLE_MESSAGES,
                    selection, selectionArgs);
            break;
        case MESSAGE_ID :
            String id = uri.getLastPathSegment();
            if (TextUtils.isEmpty(selection)) {
                rowsAffected = sqlDB.delete(NewtifryDatabase.DATABASE_TABLE_MESSAGES,
                		NewtifryDatabase.KEY_ID + "=" + id, null);
            } else {
                rowsAffected = sqlDB.delete(NewtifryDatabase.DATABASE_TABLE_MESSAGES,
                        selection + " and " + NewtifryDatabase.KEY_ID + "=" + id,
                        selectionArgs);
            }
            break;
        default:
            throw new IllegalArgumentException("Unknown or Invalid URI " + uri);
        }
        if (rowsAffected != 0) {
			onChangeDDB(uri);
            //getContext().getContentResolver().notifyChange(uri, null);
            //Intent updateUIIntent = new Intent(CommonUtilities.MESSAGE_DDB_CHANGE_INTENT);
            //LocalBroadcastManager.getInstance(getContext()).sendBroadcast(updateUIIntent);
        }

        return rowsAffected;		
	}
	
	
    public static void markAllItemsRead(Context context) {
        ContentValues values = new ContentValues();
        values.put(NewtifryDatabase.KEY_SEEN, "1");
        int updated = context.getContentResolver().update(NewtifryProvider.CONTENT_URI_MESSAGES, values,
        		NewtifryDatabase.KEY_SEEN + "=0", null);
        
		Intent smartwatchIntent = new Intent(NewtifryProHelper.MARK_ALL_SEEN);
		NewtifryPro2App.getContext().sendBroadcast(smartwatchIntent);
		UniversalNotificationManager.getInstance(context).resetNewMessagesCount();
        Log.d(CommonUtilities.TAG, "markAllItemsRead - Rows updated: " + updated);
		//TODO : remove all notifications no?
    }

	 public static void upgradeDDB14(Context context) {
		 try {
			 Cursor cursor = context.getContentResolver().query(NewtifryProvider.CONTENT_URI_MESSAGES, NewtifryDatabase.MESSAGE_PROJECTION, NewtifryDatabase.KEY_EPOCH + "=0", null, null);
			 if (cursor != null && cursor.moveToFirst()) {
				 do {
					 NewtifryMessage2 message = NewtifryMessage2.inflate(cursor);
					 message.getEpoch(); // update epoch with timestamp
					 message.save(context);
					 //						ContentValues values = message.flatten();
					 //						context.getContentResolver().update(ContentUris.withAppendedId(NewtifryProvider.CONTENT_URI_MESSAGES, message.getId()),
					 //														values, NewtifryDatabase.KEY_ID + "=" + message.getId(),
					 //														null);
				 } while (cursor.moveToNext());
			 }
			 cursor.close();
		 } catch (Exception e) {

		 }
	 }

    
    /**
     * Marks a single item, referenced by Uri, as read
     * 
     * @param context
     *            A valid context
     * @param item
     *            An individual item
     */
    public static int markItemRead(Context context, long item) {
        Uri viewedMessage = Uri.withAppendedPath(NewtifryProvider.CONTENT_URI_MESSAGES,
                String.valueOf(item));
        ContentValues values = new ContentValues();
        values.put(NewtifryDatabase.KEY_SEEN, "1");
        return context.getContentResolver().update(viewedMessage, values,
                null, null);
    }

    /**
     * Delete a single item, referenced by Uri,
     * 
     * @param context
     *            A valid context
     * @param item
     *            An individual item
     */
    public static void deleteItem(Context context, long item) {
    	// new delete image if any
		NewtifryMessage2 message = NewtifryMessage2.get(context, item);
		if (message == null) {
			return;
		}
		long messageId = message.getId();
		message.deleteCachedImages(context);
        Uri itemToDelete = Uri.withAppendedPath(NewtifryProvider.CONTENT_URI_MESSAGES,
                String.valueOf(item));
        int deleted = context.getContentResolver().delete(itemToDelete, null, null);
        Log.d(CommonUtilities.TAG, deleted + " row deleted.");
		Intent smartwatchIntent = new Intent(NewtifryProHelper.MESSAGE_DELETED);
		smartwatchIntent.putExtra(NewtifryProHelper.IntentExtras.ID,messageId);
		NewtifryPro2App.getContext().sendBroadcast(smartwatchIntent);
	}

    /**
     * Delete a multiple items, read or not,
     * 
     * @param context
     *            A valid context
     * @param onlyRead
     *            Only red items
     */
    public static void deleteItems(Context context, boolean onlyRead) {
    	String query = null;
    	if (Preferences.getAllowAllSeenDeletePinnedAndLockedMessages(context)) {
    		// delete sticky and locked too
        	if (onlyRead) {
        		query = NewtifryDatabase.KEY_SEEN + "=1";
        	}
    	} else { 
			query = NewtifryDatabase.KEY_STICKY + "=0 AND " + NewtifryDatabase.KEY_LOCKED + "=0";
	    	if (onlyRead) {
	    		query += " AND " + NewtifryDatabase.KEY_SEEN + "=1";
	    	}
    	}
        Log.d(CommonUtilities.TAG, "deleteItems - " + query);
        int updated = context.getContentResolver().delete(NewtifryProvider.CONTENT_URI_MESSAGES, query, null);
        Log.d(CommonUtilities.TAG, "deleteItems - Rows updated: " + updated);
    }

	private void onChangeDDB(Uri uri) {
		getContext().getContentResolver().notifyChange(uri, null);
		Intent updateUIIntent = new Intent(CommonUtilities.MESSAGE_DDB_CHANGE_INTENT);
		LocalBroadcastManager.getInstance(getContext()).sendBroadcast(updateUIIntent);
	}
}