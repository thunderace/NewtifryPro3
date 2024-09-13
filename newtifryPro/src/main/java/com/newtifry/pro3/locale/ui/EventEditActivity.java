package com.newtifry.pro3.locale.ui;

import com.newtifry.pro3.R;
import com.newtifry.pro3.shared.NewtifryProHelper;
import com.newtifry.pro3.locale.TaskerPlugin;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

public class EventEditActivity extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.event_edit_empty);
		final Button okButton = (Button)findViewById(R.id.tasker_edit_event_OK);
		okButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				endConfiguration();
			}
		});
	}

	public void endConfiguration() {
		Resources resources = getResources();
		String event_name = null;
		String[] relevantVariables = null;
		if (TaskerPlugin.hostSupportsRelevantVariables(getIntent().getExtras())) {
			int rv_id = resources.getIdentifier("np_new_message_variables",
					"array",
					getPackageName());
			if (rv_id != 0) {
				relevantVariables = resources.getStringArray(rv_id);
			}
		}	
		try {
			event_name = resources.getString(R.string.np_new_message);
		} catch (Resources.NotFoundException rnfe) {
		}
			
		if (event_name != null) {
			final Intent resultIntent = new Intent();
			final Bundle resultBundle = new Bundle();
		  
			resultBundle.putInt(NewtifryProHelper.EVENT_TYPE, NewtifryProHelper.EVENT_NEW_MESSAGE);
			resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE, resultBundle);
			resultIntent.putExtra(com.twofortyfouram.locale.Intent.EXTRA_STRING_BLURB, event_name);
	
			if (TaskerPlugin.hostSupportsRelevantVariables(getIntent().getExtras()) &&
					relevantVariables != null) {
				TaskerPlugin.addRelevantVariableList(resultIntent, relevantVariables);
			}
			setResult(RESULT_OK, resultIntent);
		}
		
		finish();
	}
}
