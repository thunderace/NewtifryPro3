package com.newtifry.pro3.about;


import com.newtifry.pro3.R;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;


public class ChangelogFragment extends Fragment {
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup group,
			Bundle saved) {
		return inflater.inflate(R.layout.about_changelog, group, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		WebView webView = getActivity().findViewById(
				R.id.changelogcontent);
		webView.loadUrl("file:///android_asset/" + getString(R.string.help_changelog_url_to_load) + ".html");
	}
}
