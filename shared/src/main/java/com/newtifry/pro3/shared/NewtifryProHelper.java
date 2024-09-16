package com.newtifry.pro3.shared;

public class NewtifryProHelper {
    public static final String MESSAGE_NEW = "com.newtifry.pro3.intent.action.ACTION_MESSAGE_NEW";
    public static final String MESSAGE_CREATE = "com.newtifry.pro3.intent.action.ACTION_MESSAGE_CREATE";
    // sent to smartwatch
    public static final String MESSAGE_SEEN = "com.newtifry.pro3.intent.action.ACTION_MESSAGE_SEEN";
    public static final String MESSAGE_DELETED = "com.newtifry.pro3.intent.action.ACTION_MESSAGE_DELETED";

    // from Tasker
    public static final String MESSAGE_SEEN_LAST = "com.newtifry.pro3.intent.action.ACTION_MESSAGE_SEEN_LAST";

    public static final String MESSAGE_UNSEEN = "com.newtifry.pro3.intent.action.ACTION_MESSAGE_UNSEEN";
	public final static String MESSAGE_SHOW = "com.newtifry.pro3.intent.action.ACTION_SHOW_MESSAGE";
	public final static String MESSAGE_SHOW_LAST = "com.newtifry.pro3.intent.action.ACTION_SHOW_MESSAGE_LAST";
	//public final static String MESSAGE_GET_IMAGES = "com.newtifry.pro3.intent.action.ACTION_MESSAGE_GET_IMAGES";
	
	public final static String MARK_ALL_SEEN = "com.newtifry.pro3.intent.action.ACTION_MARK_ALL_SEEN";
	public final static String SPEAK_OFF = "com.newtifry.pro3.intent.action.ACTION_SPEAK_OFF";
	public final static String SPEAK_ON = "com.newtifry.pro3.intent.action.ACTION_SPEAK_ON";
	public final static String NOTIFICATION_OFF = "com.newtifry.pro3.intent.action.ACTION_NOTIFICATION_OFF";
	public final static String NOTIFICATION_ON = "com.newtifry.pro3.intent.action.ACTION_NOTIFICATION_ON";
	
	public final static String NOTIFICATION_DELETE = "com.newtifry.pro3.NotificationDelete";
	public final static String NOTIFICATION_UNDO_DELETE = "com.newtifry.pro3.NotificationUndoDelete";
	public final static String NOTIFICATION_CANCEL = "com.newtifry.pro3.NotificationCancel";
	public final static String NOTIFICATION_SEEN = "com.newtifry.pro3.NotificationSeen";
	public final static String NOTIFICATION_SEEN_ALL = "com.newtifry.pro3.NotificationSeenAll";
	public final static String NOTIFICATION_CANCEL_UNDO = "com.newtifry.pro3.NotificationCancelUndo";

    public interface IntentExtras {
    	String ID = "com.newtifry.pro3.ID";
    	//String IMG_ID = "com.newtifry.pro3.IMG_ID";
    	String SOURCE = "com.newtifry.pro3.SOURCE";
    	String TITLE = "com.newtifry.pro3.TITLE";
    	String TIMESTAMP = "com.newtifry.pro3.TIMESTAMP";
    	String MESSAGE = "com.newtifry.pro3.MESSAGE";
    	String PRIORITY = "com.newtifry.pro3.PRIORITY";
    	String URL = "com.newtifry.pro3.URL";
    	String IMAGE_COUNT = "com.newtifry.pro3.IMAGECOUNT";
    	String IMAGE_LIST = "com.newtifry.pro3.IMAGELIST";
        String FROM_WEAR = "com.newtifry.wear.FROMWEAR";
        String WEAR_NOTIFICATION_ID = "com.newtifry.wear.NOTIFICATIONID";
    }
    // for WEAR notification support
	public final static String GROUP_KEY_MESSAGES = "newtifrypro3_group_key_messages";
	//public final static String GROUP_KEY_WEAR_MESSAGES = "wear4newtifrypro_group_key_messages";
    public static final String WEAR_MESSAGE_DELETE = "com.newtifry.wear.intent.action.ACTION_MESSAGE_DELETE";
	public final static String WEAR_NOTIFICATION_CANCEL = "com.newtifry.pro3.WEAR_NOTIFICATION_CANCEL";

    // TASKER event
	public final static String EVENT_TYPE = "com.newtifry.pro3.EVENT_TYPE";
	//public final static String EVENT_DATA = "com.newtifry.pro3.EVENT_DATA";
	public final static int EVENT_NEW_MESSAGE = 0;
	// TO SMARTWATCH PLUGIN
	public final static String MESSAGE_IMAGES = "com.newtifry.pro3.intent.action.MESSAGE_IMAGES";
}
