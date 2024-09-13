package com.newtifry.pro3.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class NewtifryDatabase extends SQLiteOpenHelper
{
	public static final String KEY_ID = "_id";
	public static final String KEY_TITLE = "title";
	public static final String KEY_SOURCENAME = "source_name";
	public static final String KEY_TIMESTAMP = "timestamp";
	public static final String KEY_OTHERTIMESTAMP = "othertimestamp";
	public static final String KEY_MESSAGE = "message";
	public static final String KEY_URL = "url";
	public static final String KEY_IMAGE1 = "image";
	public static final String KEY_IMAGE2 = "image1";
	public static final String KEY_IMAGE3 = "image2";
	public static final String KEY_IMAGE4 = "image3";
	public static final String KEY_IMAGE5 = "image4";
	public static final String KEY_SEEN = "seen";
	public static final String KEY_PRIORITY = "priority";
	public static final String KEY_FORCEIMAGE = "force_image";
	public static final String KEY_NOCACHE = "no_cache";
	public static final String KEY_DELETED = "deleted";
	public static final String KEY_STICKY = "sticky";
	public static final String KEY_LOCKED = "locked";
	public static final String KEY_EPOCH = "epoch";
	public static final String KEY_HASH = "hash";
	public static final String KEY_HASHCOUNT = "hash_count";

	// deleted
	// public static final String KEY_TAGCOUNT = "tag_count";
	// public static final String KEY_TAG = "tag";


	public static final int DATABASE_VERSION = 17;
	private static final String DATABASE_NAME = "newtifryPro";
	public static final String DATABASE_TABLE_MESSAGES = "messages";

	private static final String DATABASE_CREATE_MESSAGES = "create table " + DATABASE_TABLE_MESSAGES + " ("
			+ KEY_ID + " integer primary key autoincrement,"
			+ KEY_TIMESTAMP + " text not null,"
			+ KEY_TITLE + " text not null,"
			+ KEY_SOURCENAME + " text,"
			+ KEY_MESSAGE + " text not null,"
			+ KEY_URL + " text,"
			+ KEY_IMAGE1 + " text,"
			+ KEY_IMAGE2 + " text,"
			+ KEY_IMAGE3 + " text,"
			+ KEY_IMAGE4 + " text,"
			+ KEY_IMAGE5 + " text,"
			+ KEY_SEEN + " integer not null,"
			+ KEY_PRIORITY + " integer not null,"
			+ KEY_FORCEIMAGE + " integer not null,"
			+ KEY_NOCACHE + " integer not null,"
			+ KEY_DELETED + " integer not null,"
			+ KEY_STICKY + " integer not null,"
//			+ KEY_TAGCOUNT + " integer,"
//			+ KEY_TAG + " text,"
			+ KEY_LOCKED + " integer,"
			+ KEY_EPOCH + " integer,"
			+ KEY_HASH + " integer,"
			+ KEY_HASHCOUNT + " integer,"
			+ KEY_OTHERTIMESTAMP + " text"
			+ ");";

	public static final String[] MESSAGE_PROJECTION = new String[] { 
		KEY_ID, 
		KEY_TIMESTAMP, 
		KEY_TITLE, 
		KEY_SOURCENAME, 
		KEY_MESSAGE, 
		KEY_URL, 
		KEY_IMAGE1, 
		KEY_IMAGE2, 
		KEY_IMAGE3, 
		KEY_IMAGE4, 
		KEY_IMAGE5, 
		KEY_SEEN, 
		KEY_PRIORITY, 
		KEY_FORCEIMAGE, 
		KEY_NOCACHE,
		KEY_DELETED,
		KEY_STICKY,		
//		KEY_TAGCOUNT,
//		KEY_TAG,
		KEY_LOCKED,
		KEY_EPOCH,
		KEY_HASH,
		KEY_HASHCOUNT,
		KEY_OTHERTIMESTAMP
	};

	NewtifryDatabase( Context context ) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate( SQLiteDatabase db ) {
		db.execSQL(DATABASE_CREATE_MESSAGES); 
	}

	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// DO nothing
	}
	@Override
	public void onUpgrade( SQLiteDatabase db, int oldVersion, int newVersion ) {
		if( oldVersion < 2 ) { // Upgrading from dev1 to dev2
			db.execSQL("ALTER TABLE messages ADD COLUMN "+ KEY_FORCEIMAGE + " integer not null default 0;");
		}
		if( oldVersion < 3 ) { // Upgrading from dev2 to dev3
			db.execSQL("ALTER TABLE messages ADD COLUMN " + KEY_NOCACHE + " integer not null default 0;");
		}
		if( oldVersion < 4 ) { // Upgrading from dev3 to dev4
			db.execSQL("ALTER TABLE messages ADD COLUMN " + KEY_DELETED + " integer not null default 0;");
		}
		if( oldVersion < 5) { // Upgrading from 1.0.1 to 1.0.2
			db.execSQL("ALTER TABLE messages ADD COLUMN " + KEY_STICKY + " integer not null default 0;");
		}
		if( oldVersion < 11) { // Ufrom 1.0.1 to 1.1.0
			db.execSQL("ALTER TABLE messages ADD COLUMN " + KEY_LOCKED + " integer default 0;");
		}
		if( oldVersion < 12) { // Upgrading from 1.1.0 to 1.1.1
			db.execSQL("ALTER TABLE messages ADD COLUMN " + KEY_IMAGE2 + " text;");
			db.execSQL("ALTER TABLE messages ADD COLUMN " + KEY_IMAGE3 + " text;");
			db.execSQL("ALTER TABLE messages ADD COLUMN " + KEY_IMAGE4 + " text;");
			db.execSQL("ALTER TABLE messages ADD COLUMN " + KEY_IMAGE5 + " text;");
		}
		/*
		if( oldVersion < 13) { // Upgrading from 1.2.0 to 1.2.1
			db.execSQL("ALTER TABLE messages ADD COLUMN " + KEY_TAGCOUNT + " integer default 0;");
			db.execSQL("ALTER TABLE messages ADD COLUMN " + KEY_TAG + " text;");
		}
		*/
		if( oldVersion < 14) { // Upgrading from 1.3.2 to 1.3.3.x
			db.execSQL("ALTER TABLE messages ADD COLUMN " + KEY_EPOCH + " integer default 0;");
		}
		if( oldVersion < 15) { // add hash entry
			db.execSQL("ALTER TABLE messages ADD COLUMN " + KEY_HASH + " integer default 0;");
			db.execSQL("ALTER TABLE messages ADD COLUMN " + KEY_HASHCOUNT + " integer default 0;");
		}
		if( oldVersion < 17) { // add timestamp entries
			db.execSQL("ALTER TABLE messages ADD COLUMN " + KEY_OTHERTIMESTAMP + " text;");
		}

	}
}