package com.newtifry.pro3.about;

import com.newtifry.pro3.R;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBar.Tab;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;



public class AboutActivity extends AppCompatActivity implements
		ActionBar.TabListener {
	private static final String BUNDLE_KEY_TABINDEX = "tabindex";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		setContentView(R.layout.about_navigation);

		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		ActionBar.Tab tab1 = getSupportActionBar().newTab();
		tab1.setText(R.string.about_about);
		tab1.setTabListener(this);

		ActionBar.Tab tab2 = getSupportActionBar().newTab();
		tab2.setText(R.string.changelog_title);
		tab2.setTabListener(this);

		ActionBar.Tab tab3 = getSupportActionBar().newTab();
		tab3.setText(R.string.help_fcm_title);
		tab3.setTabListener(this);

		ActionBar.Tab tab4 = getSupportActionBar().newTab();
		tab4.setText(R.string.opensource_title);
		tab4.setTabListener(this);

		getSupportActionBar().addTab(tab1);
		getSupportActionBar().addTab(tab2);
		getSupportActionBar().addTab(tab3);
		getSupportActionBar().addTab(tab4);
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putInt(BUNDLE_KEY_TABINDEX, getSupportActionBar()
				.getSelectedTab().getPosition());
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		getSupportActionBar().setSelectedNavigationItem(
				savedInstanceState.getInt(BUNDLE_KEY_TABINDEX));
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction transaction) {
		switch(tab.getPosition()) {
			case 0:
				Fragment fragment0 = new AboutFragment();
				transaction.replace(android.R.id.content, fragment0);
				break;
			case 1:
				Fragment fragment1 = new ChangelogFragment();
				transaction.replace(android.R.id.content, fragment1);
				break;
			case 2:
				Fragment fragment2 = new FCMFragment();
				transaction.replace(android.R.id.content, fragment2);
				break;
			case 3:
				OpenSourceFragment fragment3 = new OpenSourceFragment();
				transaction.replace(android.R.id.content, fragment3);
				break;
			default:
				break;
		}
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onTabReselected(Tab arg0, FragmentTransaction arg1) {
	}

	@Override
	public void onTabUnselected(Tab arg0, FragmentTransaction arg1) {
	}
}
