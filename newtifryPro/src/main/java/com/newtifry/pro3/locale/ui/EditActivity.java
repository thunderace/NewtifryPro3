/*
 * Copyright (C) 2010-2014 The MPDroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.newtifry.pro3.locale.ui;


import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import com.newtifry.pro3.R;
import com.newtifry.pro3.shared.NewtifryProHelper;
import com.newtifry.pro3.locale.LocaleConstants;

public class EditActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

//    public static final String BUNDLE_ACTION_EXTRA = "ACTION_EXTRA";
//    public static final String BUNDLE_ACTION_LABEL = "ACTION_LABEL";
    public static final String BUNDLE_ACTION_STRING = "ACTION_STRING";

    private List<ActionItem> mItems;

    private void finishWithAction(final ActionItem action, final String extra,
            final String overrideLabel) {
        final Intent intent = new Intent();
        final Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_ACTION_STRING, action.mActionString);
//        if (extra != null) {
//            bundle.putString(BUNDLE_ACTION_EXTRA, extra);
//        }
//        if (TaskerPlugin.Setting.hostSupportsOnFireVariableReplacement(this) && 
//        		action.mActionString.equals(NewtifryProHelper.MESSAGE_SHOW)) {
//        	TaskerPlugin.Setting.setVariableReplaceKeys(bundle, new String [] {NewtifryProHelper.IntentExtras.ID});
//            bundle.putString(NewtifryProHelper.IntentExtras.ID, "%npid");
//        }
        intent.putExtra(LocaleConstants.EXTRA_BUNDLE, bundle);
        intent.putExtra(LocaleConstants.EXTRA_STRING_BLURB,
                overrideLabel == null ? action.mLabel : overrideLabel);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locale_edit);
        setResult(RESULT_CANCELED);

        final ListView list = findViewById(R.id.listView);
        mItems = new ArrayList<>();
        mItems.add(new ActionItem(NewtifryProHelper.MESSAGE_SHOW_LAST, getString(R.string.showLastMessage)));
//        mItems.add(new ActionItem(NewtifryProHelper.MESSAGE_SHOW, getString(R.string.showMessage)));
        mItems.add(new ActionItem(NewtifryProHelper.MARK_ALL_SEEN, getString(R.string.markAllSeen)));
        mItems.add(new ActionItem(NewtifryProHelper.MESSAGE_SEEN_LAST, getString(R.string.markLastMessageSeen)));
        mItems.add(new ActionItem(NewtifryProHelper.SPEAK_OFF, getString(R.string.setSpeakOff)));
        mItems.add(new ActionItem(NewtifryProHelper.SPEAK_ON, getString(R.string.setSpeakOn)));
        mItems.add(new ActionItem(NewtifryProHelper.NOTIFICATION_OFF, getString(R.string.setNotificationOff)));
        mItems.add(new ActionItem(NewtifryProHelper.NOTIFICATION_ON, getString(R.string.setNotificationOn)));
        final ListAdapter adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, mItems);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
    	final ActionItem item = mItems.get(position);
    	
    	finishWithAction(item, null, null);
    }

    /**
     * Class for listview population
     */
    private static class ActionItem {

        private final String mActionString;

        private final String mLabel;

        private ActionItem(final String actionString, final String label) {
            super();
            mActionString = actionString;
            mLabel = label;
        }

        @Override
        public String toString() {
            return mLabel;
        }
    }
}
