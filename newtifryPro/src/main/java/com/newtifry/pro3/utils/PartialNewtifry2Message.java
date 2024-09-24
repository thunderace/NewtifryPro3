package com.newtifry.pro3.utils;

import static com.newtifry.pro3.CommonUtilities.LOG_VERBOSE_LEVEL;

import android.util.Log;

import com.newtifry.pro3.CommonUtilities;
import com.newtifry.pro3.database.NewtifryMessage2;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by thunder on 27/12/2016.
 */

public class PartialNewtifry2Message {
    private String hash = null;
    private int partCount = -1;
    private String[] message = null;
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

        return this.partCount == _partCount && this.hash.equals(_hash);
    }

    public NewtifryMessage2 addPart(String part, int partNumber) {
        if (part == null || part.equals("")) {
            CommonUtilities.log(LOG_VERBOSE_LEVEL, "PartialNewtifry2Message", "PartialNewtifry2Message Error 1");
            return null;
        }
        this.message[partNumber-1] = part;
        this.partCounter++;
        if (this.partCounter == this.partCount) { // end of split message
            StringBuilder partMessage = new StringBuilder();
            for (int i = 0; i < this.partCount; i++) {
                partMessage.append(this.message[i]);
            }
            NewtifryMessage2 msg;
            msg = jsonToMessage(partMessage.toString());
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

    public static NewtifryMessage2 jsonToMessage(String jsonMessage) {
        if (jsonMessage == null || jsonMessage.equals("")) {
            return null;
        }
        JSONObject jsonNP;
        try {
            jsonNP = new JSONObject(jsonMessage);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        String source = NewtifryMessage2.decode(getJsonDataString(jsonNP, "source"), true);
        String title = NewtifryMessage2.decode(getJsonDataString(jsonNP, "title"), true);
        String message = NewtifryMessage2.decode(getJsonDataString(jsonNP, "message"), true);
        String url = NewtifryMessage2.decode(getJsonDataString(jsonNP, "url"), true);
        String image = NewtifryMessage2.decode(getJsonDataString(jsonNP, "image"), true);
        String image1 = NewtifryMessage2.decode(getJsonDataString(jsonNP, "image1"), true);
        String image2 = NewtifryMessage2.decode(getJsonDataString(jsonNP, "image2"), true);
        String image3 = NewtifryMessage2.decode(getJsonDataString(jsonNP, "image3"), true);
        String image4 = NewtifryMessage2.decode(getJsonDataString(jsonNP, "image4"), true);
        String image5 = NewtifryMessage2.decode(getJsonDataString(jsonNP, "image5"), true);
        String timestamp = getJsonDataString(jsonNP, "timestamp");
        int priority = getJsonDataInt(jsonNP, "NPpriority", 0);
        int notify = getJsonDataInt(jsonNP, "notify", -1);
        int speak = getJsonDataInt(jsonNP, "speak", -1);
        int state = getJsonDataInt(jsonNP, "state", -1);
        int nocache = getJsonDataInt(jsonNP, "nocache", -1);
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
        incoming.setSticky(state == 1);
        incoming.setLocked(state == 2);
        incoming.setNoCache(nocache == 1);
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
