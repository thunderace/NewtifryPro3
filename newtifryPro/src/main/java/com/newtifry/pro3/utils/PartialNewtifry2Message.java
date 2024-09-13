package com.newtifry.pro3.utils;

import android.util.Log;

import com.newtifry.pro3.database.NewtifryMessage2;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by thunder on 27/12/2016.
 */

public class PartialNewtifry2Message {
    private String hash = null;
    private int partCount = -1;
    private String message[] = null;
    private int partCounter = 0;
    public PartialNewtifry2Message() {
    }

    public boolean init(int _partCount, int _partNumber, String _hash) {
        if (this.hash == null || !this.hash.equals(_hash)) { // new message : discard current
            this.partCount = _partCount;
            this.hash = _hash;
            this.partCounter = 0;
            message = new String[_partCount];
            return true;
        }

        if (this.partCount == _partCount  && this.hash.equals(_hash)) {
            return true;
        }
        return false;
    }

    public NewtifryMessage2 addPart(String part, int partNumber, boolean fromSMS) {
        if (part == null || part.equals("")) {
            return null;
        }
        this.message[partNumber-1] = part;
        this.partCounter++;
        if (this.partCounter == this.partCount) {
            String partMessage = "";
            for (int i = 0; i < this.partCount; i++) {
                partMessage += this.message[i];
            }
            NewtifryMessage2 msg;
            msg = jsonToMessage(partMessage, fromSMS);
            this.reset();
            return msg;
        }
        return null;
    }

    public void reset() {
        this.hash = null;
        this.partCount = -1;
        this.message = null;
        this.partCounter = 0;
    }

    public static NewtifryMessage2 jsonToMessage(String jsonMessage, boolean fromSMS) {
        if (jsonMessage == null || jsonMessage.equals("")) {
            return null;
        }
        if (fromSMS) {
            String smsMsg = NewtifryMessage2.decode(jsonMessage, true);
            jsonMessage = smsMsg;
        }
        JSONObject jsonNP;
        try {
            jsonNP = new JSONObject(jsonMessage);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        String source = "";
        String title= "from SMS"; // default title
        String message = null;
        String url = null;
        String image = null;
        String image1 = null;
        String image2 = null;
        String image3 = null;
        String image4 = null;
        String image5= null;
        String timestamp = null;
        int priority = 0;
        int notify = -1;
        int speak = -1;
        int state = 0;
        int nocache = 0;
        if (fromSMS) {
            source = getJsonDataString(jsonNP, "source");
            title = getJsonDataString(jsonNP, "title");
            message = getJsonDataString(jsonNP, "message");
            url = getJsonDataString(jsonNP, "url");
            image = getJsonDataString(jsonNP, "image");
            image1 = getJsonDataString(jsonNP, "image1");
            image2 = getJsonDataString(jsonNP, "image2");
            image3 = getJsonDataString(jsonNP, "image3");
            image4 = getJsonDataString(jsonNP, "image4");
            image5 = getJsonDataString(jsonNP, "image5");
        } else {
            source = NewtifryMessage2.decode(getJsonDataString(jsonNP, "source"), true);
            title = NewtifryMessage2.decode(getJsonDataString(jsonNP, "title"), true);
            message = NewtifryMessage2.decode(getJsonDataString(jsonNP, "message"), true);
            url = NewtifryMessage2.decode(getJsonDataString(jsonNP, "url"), true);
            image = NewtifryMessage2.decode(getJsonDataString(jsonNP, "image"), true);
            image1 = NewtifryMessage2.decode(getJsonDataString(jsonNP, "image1"), true);
            image2 = NewtifryMessage2.decode(getJsonDataString(jsonNP, "image2"), true);
            image3 = NewtifryMessage2.decode(getJsonDataString(jsonNP, "image3"), true);
            image4 = NewtifryMessage2.decode(getJsonDataString(jsonNP, "image4"), true);
            image5 = NewtifryMessage2.decode(getJsonDataString(jsonNP, "image5"), true);
        }
        timestamp = getJsonDataString(jsonNP, "timestamp");
        priority = getJsonDataInt(jsonNP, "NPpriority", 0);
        notify = getJsonDataInt(jsonNP, "notify", -1);
        speak = getJsonDataInt(jsonNP, "speak", -1);
        state = getJsonDataInt(jsonNP, "state", -1);
        nocache = getJsonDataInt(jsonNP, "nocache", -1);
        NewtifryMessage2 incoming = new NewtifryMessage2();
        incoming.setMessage(message);
        incoming.setTitle(title);
        incoming.setSourceName(source);
        incoming.setUrl(url);
        if (image != null) {
            incoming.setImage(0, image);
        } else {
            incoming.setImage(0, image1);
            incoming.setImage(1, image2);
            incoming.setImage(2, image3);
            incoming.setImage(3, image4);
            incoming.setImage(4, image5);
        }
        incoming.setTimestamp(timestamp); // will set to the current timestamp
        incoming.setPriority(priority);
        incoming.setSticky(state == 1 ? true : false);
        incoming.setLocked(state == 2 ? true : false);
        incoming.setNoCache(nocache == 1 ? true : false);
        incoming.setSpeak(speak);
        incoming.setNotify(notify);
        return incoming;
    }
    public static String getJsonDataString(JSONObject jsonObj, String entry) {
        try {
            return jsonObj.getString(entry);
        } catch (JSONException e) {
            return "";
        }
    }
    public static int getJsonDataInt(JSONObject jsonObj, String entry, int defaultValue) {
        try {
            return jsonObj.getInt(entry);
        } catch (JSONException e) {
            return defaultValue;
        }
    }


}
