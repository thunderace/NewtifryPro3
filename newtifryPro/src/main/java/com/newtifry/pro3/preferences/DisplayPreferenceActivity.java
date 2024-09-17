package com.newtifry.pro3.preferences;


import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.MenuItem;

import com.afollestad.materialdialogs.prefs.MaterialEditTextPreference;
import com.newtifry.pro3.Preferences;
import com.newtifry.pro3.R;
import com.newtifry.pro3.urlimageviewhelper.UrlImageViewHelper;
import com.newtifry.pro3.utils.AppCompatPreferenceActivity;
import com.newtifry.pro3.preference.colorpicker.ColorPickerPreference;

// See PreferenceActivity for warning suppression justification
@SuppressWarnings("deprecation")
public class DisplayPreferenceActivity extends AppCompatPreferenceActivity {
	private Context context;
	private CheckBoxPreference usePriorityColorsCheckbox;
	private CheckBoxPreference showImagesCheckbox;
	private CheckBoxPreference preloadImagesCheckbox;
	private CheckBoxPreference smallRowCheckbox;
	private CheckBoxPreference shrinkImagesCheckbox;
	private CheckBoxPreference cacheImagesCheckbox;
	private MaterialEditTextPreference cacheImageDurationEditText;
	private ColorPickerPreference alertColorListPreference;
	private ColorPickerPreference infoColorListPreference;
	private ColorPickerPreference warningColorListPreference;
	private Preference purgeCachePreference;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.context = this;
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		addPreferencesFromResource(R.xml.display_preferences);
		
		usePriorityColorsCheckbox = (CheckBoxPreference)findPreference(Preferences.USE_PRIORITY_COLORS);
		usePriorityColorsCheckbox.setChecked(Preferences.getUsePriorityColor(this));
		usePriorityColorsCheckbox.setOnPreferenceChangeListener(onUsePriorityColorsCheckListener);
		
		smallRowCheckbox = (CheckBoxPreference)findPreference(Preferences.SMALL_ROW);
		smallRowCheckbox.setChecked(Preferences.getUseSmallRow(this));
		
		showImagesCheckbox = (CheckBoxPreference)findPreference(Preferences.SHOW_IMAGES);
		showImagesCheckbox.setChecked(Preferences.getShowImages(this));
		showImagesCheckbox.setOnPreferenceChangeListener(showImageChangeHandler);

		preloadImagesCheckbox = (CheckBoxPreference)findPreference(Preferences.PRELOAD_IMAGES);
		preloadImagesCheckbox.setChecked(Preferences.getPreloadBitmap(this));
		
		shrinkImagesCheckbox = (CheckBoxPreference)findPreference(Preferences.SHRINK_IMAGES);
		shrinkImagesCheckbox.setChecked(Preferences.getShrinkBitmap(this));
		shrinkImagesCheckbox.setOnPreferenceChangeListener(shrinkImageChangeHandler);
		
		cacheImagesCheckbox = (CheckBoxPreference)findPreference(Preferences.CACHE_IMAGES);
		cacheImagesCheckbox.setChecked(Preferences.getCacheBitmap(this));
		cacheImagesCheckbox.setOnPreferenceChangeListener(cacheImageChangeHandler);
		cacheImageDurationEditText = (MaterialEditTextPreference)findPreference(Preferences.CACHE_IMAGES_DELAY);
		cacheImageDurationEditText.setText(Preferences.getCacheBitmapDuration(this));
		cacheImageDurationEditText.setOnPreferenceChangeListener(cacheImageDurationChangeHandler);

		
		
		setCacheImageDurationSummary(Preferences.getCacheBitmapDuration(this));
		
		
		alertColorListPreference = (ColorPickerPreference)this.findPreference(Preferences.ALERT_TITLE_COLOR);
		warningColorListPreference = (ColorPickerPreference)this.findPreference(Preferences.WARNING_TITLE_COLOR);
		infoColorListPreference = (ColorPickerPreference)this.findPreference(Preferences.INFO_TITLE_COLOR);
		alertColorListPreference.setAlphaSliderEnabled(true);
		infoColorListPreference.setAlphaSliderEnabled(true);
		warningColorListPreference.setAlphaSliderEnabled(true);
		toggleUsePriorityColors(Preferences.getUsePriorityColor(this));

		
		purgeCachePreference = findPreference(Preferences.PURGES_IMAGES_CACHE);
		purgeCachePreference.setOnPreferenceClickListener(purgeImageCacheClickHandler);
		setImageCacheNumber(-1);
		if (UrlImageViewHelper.getCacheCount(this) == 0) {
			purgeCachePreference.setEnabled(false);
		}
	}

	public void setImageCacheNumber(int value) {		
		String template = getString(R.string.cache_images_purge_summary);
		if (value == -1) {
			value = UrlImageViewHelper.getCacheCount(this);
		}
		purgeCachePreference.setSummary(String.format(template, value));
	}
	
	OnPreferenceClickListener purgeImageCacheClickHandler = new OnPreferenceClickListener() {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			UrlImageViewHelper.cleanupCache(context);
	        setImageCacheNumber(0);
			return true;
		}
	};
	
	OnPreferenceChangeListener showImageChangeHandler = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			showImageChange((Boolean)newValue);
			return true;
		}
	};

	private void showImageChange(boolean showImage) {
		shrinkImagesCheckbox.setEnabled(showImage);
		cacheImagesCheckbox.setEnabled(showImage);
		preloadImagesCheckbox.setEnabled(showImage);
        cacheImageDurationEditText.setEnabled(showImage && cacheImagesCheckbox.isChecked());
	}
	
	OnPreferenceChangeListener shrinkImageChangeHandler = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			shrinkImageChange((Boolean)newValue);
			return true;
		}
	};

	private void shrinkImageChange(boolean shrinkImage) {
		UrlImageViewHelper.setUseBitmapScaling(shrinkImage);
	}

	OnPreferenceChangeListener cacheImageChangeHandler = new OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			cacheImageChange((Boolean)newValue);
			return true;
		}
	};

	private void cacheImageChange(boolean cacheImage) {
		cacheImageDurationEditText.setEnabled(cacheImage);
	}
	
	
	
	
	OnPreferenceChangeListener cacheImageDurationChangeHandler = new OnPreferenceChangeListener()
	{
		public boolean onPreferenceChange(Preference preference, Object newValue)
		{
			setCacheImageDurationSummary((String) newValue);
			return true;
		}
	};

	public void setCacheImageDurationSummary(String value) {
		String template = getString(R.string.cache_images_delay_summary);

		try {
			Integer intValue = Integer.parseInt(value);
			String plural = "s";
			if (intValue == 1) {
				plural = "";
			}

			cacheImageDurationEditText.setSummary(String.format(template, intValue, plural));
		} catch( NumberFormatException ex ) {
			// Not a valid number... ignore.
		}		
	}
	
	OnPreferenceChangeListener onUsePriorityColorsCheckListener = new OnPreferenceChangeListener()
	{
		public boolean onPreferenceChange(Preference preference, Object newValue)
		{
			toggleUsePriorityColors((Boolean)newValue);
			return true;
		}
	};
	public void toggleUsePriorityColors(boolean checked) {
		alertColorListPreference.setEnabled(checked);
		warningColorListPreference.setEnabled(checked);
		infoColorListPreference.setEnabled(checked);
	}
	
	
//	@Override
//	public void onResume()
//	{
//		super.onResume();
//	}	
	
	
//	@Override
//	protected void onPause() {
//		super.onPause();
//		// Unregister the listener whenever a key changes
//		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
//				this);
//	}

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

	
}
