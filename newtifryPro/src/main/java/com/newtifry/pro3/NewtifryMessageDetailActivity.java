package com.newtifry.pro3;


import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;

import com.newtifry.pro3.database.NewtifryProvider;

/**
 * An activity representing a single NewtifryMessage detail screen. This
 * activity is only used on handset devices. On tablet-size devices, item
 * details are presented side-by-side with a list of items in a
 * {@link NewtifryMessageListActivity}.
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a {@link NewtifryMessageDetailFragment}.
 */
public class NewtifryMessageDetailActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_newtifrymessage_detail);

		// Show the Up button in the action bar.
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setTitle(R.string.app_name);
		// savedInstanceState is non-null when there is fragment state
		// saved from previous configurations of this activity
		// (e.g. when rotating the screen from portrait to landscape).
		// In this case, the fragment will automatically be re-added
		// to its container so we don't need to manually add it.
		// For more information, see the Fragments API guide at:
		//
		// http://developer.android.com/guide/components/fragments.html
		//
		if (savedInstanceState == null) {
			// Create the detail fragment and add it to the activity
			// using a fragment transaction.
			Bundle arguments = new Bundle();
			long messageId = getIntent().getLongExtra(NewtifryMessageDetailFragment.ARG_ITEM_ID, -1);
			NewtifryProvider.markItemRead(NewtifryPro2App.getContext(), messageId);
			arguments.putLong(
					NewtifryMessageDetailFragment.ARG_ITEM_ID, messageId);
			NewtifryMessageDetailFragment fragment = new NewtifryMessageDetailFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction()
					.add(R.id.newtifrymessage_detail_container, fragment)
					.commit();
		} 
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				// This ID represents the Home or Up button. In the case of this
				// activity, the Up button is shown. Use NavUtils to allow users
				// to navigate up one level in the application structure. For
				// more details, see the Navigation pattern on Android Design:
				//
				// http://developer.android.com/design/patterns/navigation.html#up-vs-back
				//
				onBackPressed();
//				NavUtils.navigateUpTo(this, new Intent(this,
//						NewtifryMessageListActivity.class));
				return true;

		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		NewtifryMessageDetailFragment fragment = (NewtifryMessageDetailFragment) getSupportFragmentManager()
                .findFragmentById(R.id.newtifrymessage_detail_container);
            if (fragment != null) {
                fragment.cancel();
            }

	}


	
}
