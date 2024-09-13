package com.newtifry.pro3;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.fragment.app.Fragment;
import androidx.core.view.MenuItemCompat;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ms.square.android.expandabletextview.ExpandableTextView;
import com.newtifry.pro3.preferences.NewtifryPreferenceActivity;
import com.newtifry.pro3.urlimageviewhelper.UrlImageViewCallback;
import com.newtifry.pro3.urlimageviewhelper.UrlImageViewHelper;
import com.newtifry.pro3.about.AboutActivity;
import com.newtifry.pro3.database.NewtifryMessage2;
import com.newtifry.pro3.utils.UniversalNotificationManager;

import java.text.ParseException;

/**
 * A fragment representing a single NewtifryMessage detail screen. This fragment
 * is either contained in a {@link NewtifryMessageListActivity} in two-pane mode
 * (on tablets) or a {@link NewtifryMessageDetailActivity} on handsets.
 */
public class NewtifryMessageDetailFragment extends Fragment implements OnClickListener, ViewPager.OnPageChangeListener {
	public boolean[] urlImageViewCallbackEnabled = new boolean[5];
	public static final String ARG_ITEM_ID = "messageId";

	public NewtifryMessage2 message = null;
	private int showImageMenuInitialState = -1;

	protected boolean forceImageDisplay = false;
	private ViewPager viewPager;
	private int viewPagerPosition = 0;
	TextView sourceNameTextView;
	ExpandableTextView timestampTextView;
	//TextView timestampTextView;
	TextView messageTextView;
	TextView url;

	View rootView;
	protected ImageView[] imageView = new ImageView[5];
	protected TextView[] imageUrlTextView = new TextView[5];
	protected ProgressBar[] progressBar = new ProgressBar[5];
	protected TextView[] errorTextView = new TextView[5];
	
	private MenuItem showImageMenu;
	private MenuItem stopSpeakMenu;
	private MenuItem startSpeakMenu;
	private MenuItem stickMenu;
	private MenuItem unlockMenu;
	private long messageId;

	private Callbacks activityCallbacks = sDummyCallbacks;
	public interface Callbacks {
		public void onShowImageMenuSetVisible(boolean show);
	}

	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void onShowImageMenuSetVisible(boolean show) {
		}
	};

	public NewtifryMessageDetailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle arguments = getArguments();
		if (arguments.containsKey(ARG_ITEM_ID)) {
			messageId = arguments.getLong(ARG_ITEM_ID);
			message = NewtifryMessage2.get(getActivity(), messageId);
		}
		if (NewtifryMessageListActivity.isTwoPane() == false) {
			setHasOptionsMenu(true); // this will call onCreateOptionsMenu (Sherlock action)
		}

		
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (message == null || message.getImageCount() == 0) {
			return inflater.inflate(R.layout.message_detail_scrollview, container, false);
		}
		return inflater.inflate(R.layout.message_detail2, container, false);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (NewtifryMessageListActivity.isTwoPane() == true) {
			// Activities containing this fragment must implement its callbacks.
			if (!(activity instanceof Callbacks)) {
				throw new IllegalStateException(
						"Activity must implement fragment's callbacks.");
			}
			activityCallbacks = (Callbacks) activity;
		}
	}	
	
	@Override
	public void onCreateOptionsMenu( android.view.Menu menu,  MenuInflater inflater )	{
		// only called if in two pane mode
		stopSpeakMenu = menu.add(0, NewtifryMessageListActivity.STOP_SPEAK, Menu.NONE, R.string.stop_speak_now);
		stopSpeakMenu.setIcon(R.drawable.ic_menu_stop_speak_white_24dp);
		MenuItemCompat.setShowAsAction(stopSpeakMenu, MenuItem.SHOW_AS_ACTION_IF_ROOM);
		startSpeakMenu = menu.add(0, NewtifryMessageListActivity.START_SPEAK, Menu.NONE, R.string.start_speak);
		startSpeakMenu.setIcon(R.drawable.ic_menu_start_speak_white_24dp);
		MenuItemCompat.setShowAsAction(startSpeakMenu, MenuItem.SHOW_AS_ACTION_IF_ROOM);
		showImageMenu = menu.add(0, NewtifryMessageListActivity.SHOW_IMAGE, Menu.NONE, R.string.show_image);
		showImageMenu.setIcon(R.drawable.ic_image_white_24dp);
		MenuItemCompat.setShowAsAction(showImageMenu, MenuItem.SHOW_AS_ACTION_IF_ROOM);
		if (showImageMenuInitialState != -1) {
			showImageMenuSetVisible(viewPagerPosition, showImageMenuInitialState == 1 ? true : false);
		}
		
		stickMenu = menu.add(0, NewtifryMessageListActivity.STICK_MENU_ID, Menu.NONE, R.string.stick_menu_entry);
		unlockMenu = menu.add(0, NewtifryMessageListActivity.UNLOCK_MENU_ID, Menu.NONE, R.string.unlock_menu_entry);
		menu.add(0, NewtifryMessageListActivity.MARK_UNREAD_MENU_ID, Menu.NONE, R.string.mark_unread_menu_entry).setIcon(R.drawable.ic_visibility_white_24dp/*ic_menu_read_white*/);
		menu.add(0, NewtifryMessageListActivity.PREFERENCES_MENU_ID, Menu.NONE, R.string.preference_menu_entry).setIcon(android.R.drawable.ic_menu_preferences);
		menu.add(0, NewtifryMessageListActivity.ABOUT_MENU, Menu.NONE, R.string.about_title).setIcon(android.R.drawable.ic_menu_info_details);
	}
	
	@Override
	public void onPrepareOptionsMenu( Menu menu ) {
		updateMenus();
	}

	private void updateMenus() {
//		if ((Preferences.getShowImages(getSherlockActivity()) == false && 
//		!CommonUtilities.isEmpty(message.getImage(viewPagerPosition))) 
//		|| message.getImageLoadingStatus(viewPagerPosition) == NewtifryMessage2.IMAGE_LOADING_ERROR) {
//	// display image if image exists in message and not loaded
//	showImageMenu.setVisible(true);
//} else {
//	showImageMenu.setVisible(false);
//}
		if (stopSpeakMenu == null) {
			return; // for call from onResume();
		}
		if (Preferences.getSpeakMessage(getActivity()) == true) {
			stopSpeakMenu.setVisible(true);
			startSpeakMenu.setVisible(true);
		} else {
			stopSpeakMenu.setVisible(false);
			startSpeakMenu.setVisible(false);
		}

		if (message.isLocked()) {
			stickMenu.setVisible(false);
			unlockMenu.setVisible(true);
			unlockMenu.setTitle(R.string.unlock_menu_entry);
//			if (message.isTagged() == false || Preferences.allowUnlockTaggedMessages(getSherlockActivity()) == true) {
//				unlockMenu.setVisible(true);
//				unlockMenu.setTitle(R.string.unlock_menu_entry);
//			} else {
//				unlockMenu.setVisible(false);
//			}
			
		} else {
			stickMenu.setVisible(true);
			if (message.getSticky()) {
				unlockMenu.setVisible(false);
				stickMenu.setTitle(R.string.unstick_menu_entry);
			} else {
				unlockMenu.setVisible(true);
				unlockMenu.setTitle(R.string.lock_menu_entry);
				stickMenu.setTitle(R.string.stick_menu_entry);
			}
		}

	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case NewtifryMessageListActivity.STOP_SPEAK:
				CommonUtilities.stopSpeak(getActivity());
				return true;
			case NewtifryMessageListActivity.SHOW_IMAGE:
				// reload
				forceLoadImage();
				return true;
			case NewtifryMessageListActivity.START_SPEAK:
				CommonUtilities.speak(getActivity(), CommonUtilities.getOutputMessage(message, getActivity()));
				return true;
			case NewtifryMessageListActivity.ABOUT_MENU:
				Intent aboutIntent = new Intent(getActivity(), AboutActivity.class);
				startActivity(aboutIntent);
				return true;
			case NewtifryMessageListActivity.PREFERENCES_MENU_ID:
				Intent intent = new Intent(getActivity(), NewtifryPreferenceActivity.class);
//				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				return true;
			case NewtifryMessageListActivity.MARK_UNREAD_MENU_ID:
				message.setSeen(false);
				message.save(getActivity());
				return true;
			case NewtifryMessageListActivity.STICK_MENU_ID:
				message.setSticky(!message.getSticky());
				message.save(getActivity());
				manageStickyLockedIcon();
				return true;
			case NewtifryMessageListActivity.UNLOCK_MENU_ID:
				message.setLocked(!message.isLocked());
				message.save(getActivity());
				manageStickyLockedIcon();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onResume() {
		super.onResume();
        UniversalNotificationManager.getInstance(getActivity()).resetNewMessagesCount();
		updateMenus();
		refresh();
	}

	@Override
	public void onPause() {
		super.onPause();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		cancel();
	}
	
	public void cancel() {
		// we are leaving this fragment
		for (int i = 0; i < 5; i++) {
			urlImageViewCallbackEnabled[i] = false;
		}
	}
	
	
	public void forceLoadImage() {
		if (viewPager.getVisibility() != View.VISIBLE) {
			this.forceImageDisplay = true;
			refresh();
		} else {
			loadImage(viewPagerPosition, true);
		}
	}
	
	private void showImageMenuSetVisible(int position, boolean show) {
		if (position != viewPagerPosition) {
			return;
		}
		if (NewtifryMessageListActivity.isTwoPane() == true) {
			// delegate the menu to main activity
			activityCallbacks.onShowImageMenuSetVisible(show);
		} else {
			if (showImageMenu != null) {
				showImageMenu.setEnabled(show);
				showImageMenu.getIcon().setAlpha(show ? 255: 64);
//				showImageMenu.setVisible(show); // fix 1.0.1
			} else {
				showImageMenuInitialState = (show == true) ? 1 : 0;
			}
		}
	}

	public void updateStickyLockMessage() {
		message = NewtifryMessage2.get(getActivity(), messageId);
		manageStickyLockedIcon();
	}

	public void onClick(View v) {
		if(message.getSticky()) {
			message.setSticky(false);
			message.save(getActivity());
			manageStickyLockedIcon();
			return;
		}
		if (message.isLocked()) {
			message.setLocked(false);
			message.save(getActivity());
			manageStickyLockedIcon();
		}
	}

	
	private void manageStickyLockedIcon() {

		View rootView = getView();
		ImageView sticky = (ImageView) rootView.findViewById(R.id.message_detail_sticky);
			sticky.setOnClickListener(this);	
		if (message == null) {
			sticky.setVisibility(View.INVISIBLE);
			return;
		}
		if (message.getSticky()) {
			sticky.setVisibility(View.VISIBLE);
			sticky.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.icon_sticky));
			
		} else {
			if (message.isLocked()) {
				sticky.setVisibility(View.VISIBLE);
				sticky.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.icon_lock));
			} else {
				sticky.setVisibility(View.GONE);
			}
		}
	}
	private void refresh() {
		rootView = getView();
		viewPager = (ViewPager)rootView.findViewById(R.id.pager);
		viewPager.setOnPageChangeListener(this);
		imageUrlTextView[0] = (TextView) rootView.findViewById(R.id.message_detail_image_url1);
		imageUrlTextView[1] = (TextView) rootView.findViewById(R.id.message_detail_image_url2);
		imageUrlTextView[2] = (TextView) rootView.findViewById(R.id.message_detail_image_url3);
		imageUrlTextView[3] = (TextView) rootView.findViewById(R.id.message_detail_image_url4);
		imageUrlTextView[4] = (TextView) rootView.findViewById(R.id.message_detail_image_url5);
		sourceNameTextView = (TextView) rootView.findViewById(R.id.message_detail_title_or_source);
		timestampTextView = (ExpandableTextView) rootView.findViewById(R.id.message_detail_timestamp);
				//.findViewById(R.id.expand_text_view);
		//timestampTextView = (TextView) rootView.findViewById(R.id.message_detail_timestamp);

		messageTextView = (TextView) rootView.findViewById(R.id.message_detail_content);
		url = (TextView) rootView.findViewById(R.id.message_detail_url);
		
		if (message != null) {
			this.viewPager.setAdapter(new ImagePagerAdapter(message));
			int imageCount = message.getImageCount();
			if (imageCount == 0) {
				for (int i = 0; i < 5; i++) {
					imageUrlTextView[i].setVisibility(View.GONE);
				}
				showImageMenuSetVisible(viewPagerPosition, false);
				viewPager.setVisibility(View.GONE);
				
			} else {
				if (Preferences.getShowImages(getActivity()) == true || this.forceImageDisplay) {
					for (int i = 0; i < 5; i++) {
						imageUrlTextView[i].setVisibility(View.GONE);
					}
					viewPager.setVisibility(View.VISIBLE);
					this.viewPager.setCurrentItem(viewPagerPosition);
				} else {
					showImageMenuSetVisible(viewPagerPosition, true);

					viewPager.setVisibility(View.GONE);
					for (int i = 0; i < imageCount; i++) {
						imageUrlTextView[i].setVisibility(View.VISIBLE);
						imageUrlTextView[i].setText(CommonUtilities.getURLWithoutCredentials(message.getImage(i)));
					}
					for (int i = imageCount; i < 5; i++) { // hide other not used
						imageUrlTextView[i].setVisibility(View.GONE);
					}
				}
			}
			manageStickyLockedIcon();
			String sourceName = message.getSourceName();
			if (sourceName != null && !sourceName.equals("")) {
				sourceName += " - " + message.getTitle();
				//sourceNameTextView.setText(sourceName + " - " + message.getTitle());
			} else {
				sourceName = message.getTitle();
				//sourceNameTextView.setText(message.getTitle());
			}
			int hashCount = message.getHashCount();
			if (hashCount > 1) {
				sourceName += " (" + Integer.toString(hashCount)+")";
			}
			sourceNameTextView.setText(sourceName);
			int priority = message.getPriority();
			if (Preferences.getUsePriorityColor(getActivity()) == true && priority > 0) {
				int color = -1;
				switch (priority) {
					case 1 :
						color = Preferences.getInfoTitleColor(getActivity());
						break;
					case 2 : 
						color = Preferences.getWarningTitleColor(getActivity());
						break;
					case 3 :
						color = Preferences.getAlertTitleColor(getActivity());
						break;
				}
				sourceNameTextView.setTextColor(color);
			} else {
				// when it's a redraw after parameter change, the title color may cahnge so restore the default color
				
				sourceNameTextView.setTextColor(messageTextView.getCurrentTextColor());
			}
			String timestamp = message.getDisplayTimestamp();
			String otherTimestamp = message.getOtherTimestamp();
			if (otherTimestamp != null && !otherTimestamp.isEmpty()) {
				timestamp += "\n" + otherTimestamp;
			}
			timestampTextView.setText(timestamp);

			if( message.getUrl() != null ) {
				url.setText(CommonUtilities.getURLWithoutCredentials(message.getUrl()));
			} else {
				url.setVisibility(View.GONE);
			}
			
			messageTextView.setText(message.getSpannedMessage());			
		} else {
			Log.d("NewtifryMessageDetailFr", "Empty message : ");
			manageStickyLockedIcon();
			// hide all
			sourceNameTextView.setVisibility(View.INVISIBLE);
			timestampTextView.setVisibility(View.INVISIBLE);
			messageTextView.setVisibility(View.INVISIBLE);
			url.setVisibility(View.INVISIBLE);
			for (int i = 0; i < 5; i++) {
				imageUrlTextView[i].setVisibility(View.INVISIBLE);
			}
			viewPager.setVisibility(View.INVISIBLE);
		}
	}

	
	UrlImageViewCallback urlImageViewCallback = new UrlImageViewCallback() {
        @Override
        public boolean isEnable(int position) {
        	return urlImageViewCallbackEnabled[position];
        }

	    @Override
	    public void onLoaded(Context context, 
	    						ImageView imageView, 
	    						Bitmap loadedBitmap, 
	    						long messageId, 
	    						int imageId, 
	    						String url, 
	    						boolean loadedFromCache) {
			urlImageViewCallbackEnabled[imageId] = false;
	    	progressBar[imageId].setVisibility(View.INVISIBLE);
	    	if (loadedBitmap == null) {
	    		showImageMenuSetVisible(imageId, true);
	    		
	    		if (CommonUtilities.okToDownloadData(getActivity()) == false) {
		    		if (message.getImageCount() == 1) {
		    			errorTextView[imageId].setText(R.string.image_alone_load_no_wifi);
		    		} else {
		    			String template = getString(R.string.image_load_no_wifi);
		    			errorTextView[imageId].setText(String.format(template, imageId+1));
		    		}
		    		message.setImageLoadingStatus(imageId, NewtifryMessage2.IMAGE_NOT_LOADED);
		    		message.save(getActivity());
	    		} else {
		    		if (message.getImageCount() == 1) {
		    			errorTextView[imageId].setText(R.string.image_alone_load_error);
		    		} else {
		    			String template = getString(R.string.image_load_error);
		    			errorTextView[imageId].setText(String.format(template, imageId+1));
		    		}
		    		message.setImageLoadingStatus(imageId, NewtifryMessage2.IMAGE_LOADING_ERROR);
		    		message.save(getActivity());
	    		}
	    		errorTextView[imageId].setVisibility(View.VISIBLE);
	        } else {
	        	showImageMenuSetVisible(imageId, false);
	        	message.setImageLoadingStatus(imageId, NewtifryMessage2.IMAGE_LOADED);
	    		message.save(getActivity());
	        	if (!loadedFromCache) {
	        		ScaleAnimation scale = new ScaleAnimation(0, 1, 0, 1, ScaleAnimation.RELATIVE_TO_SELF, .5f, ScaleAnimation.RELATIVE_TO_SELF, .5f);
	        		scale.setDuration(300);
	        		scale.setInterpolator(new OvershootInterpolator());
	        		imageView.startAnimation(scale);
	        	}
	        }
	    }
	};
	
	private class ImagePagerAdapter extends PagerAdapter {

		private LayoutInflater inflater;
		NewtifryMessage2 message;

		ImagePagerAdapter(NewtifryMessage2 message) {
			this.message = message;
			inflater = getActivity().getLayoutInflater();
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
		}

		@Override
		public int getCount() {
			return message.getImageCount();
		}

		@Override
		public Object instantiateItem(ViewGroup view, final int position) {
			View imageLayout = inflater.inflate(R.layout.message_detail_pager_item, view, false);
			imageView[position] = (ImageView) imageLayout.findViewById(R.id.image);
			progressBar[position] = (ProgressBar) imageLayout.findViewById(R.id.loading);
			errorTextView[position] = (TextView)imageLayout.findViewById(R.id.loading_error);
            loadImage(position, false);
			view.addView(imageLayout, 0);
			return imageLayout;
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view.equals(object);
		}

		@Override
		public void restoreState(Parcelable state, ClassLoader loader) {
		}

		@Override
		public Parcelable saveState() {
			return null;
		}
	}

	public void loadImage(int position, boolean force) {
		if (force == false && message.getImageLoadingStatus(position) == NewtifryMessage2.IMAGE_LOADING_ERROR ) {
			// dont try to load again
    		if (message.getImageCount() == 1) {
    			errorTextView[position].setText(R.string.image_alone_load_error);
    		} else {
    			String template = getString(R.string.image_load_error);
    			errorTextView[position].setText(String.format(template, position+1));
    		}
	        errorTextView[position].setVisibility(View.VISIBLE);
	        progressBar[position].setVisibility(View.INVISIBLE);
			showImageMenuSetVisible(position, true);  // fix 1.3.0 : show loadimage menu
	        return;
		}
        errorTextView[position].setVisibility(View.INVISIBLE);
        progressBar[position].setVisibility(View.VISIBLE);
		String imageURL = message.getImage(position);
		urlImageViewCallbackEnabled[position] = true;
		message.setImageLoadingStatus(position, NewtifryMessage2.IMAGE_LOADING);
		message.save(getActivity());
		showImageMenuSetVisible(position, false);
		if (Preferences.getCacheBitmap(getActivity()) == true && message.getNoCache() == false) {
            UrlImageViewHelper.setUrlDrawable(getActivity(),
            									imageView[position], 
            									imageURL, 
            									message.getId(),
            									position, 	
            									urlImageViewCallback,
            									true);
		} else {
            UrlImageViewHelper.setUrlDrawable(getActivity(), 
            									imageView[position],
            									imageURL, 
            									message.getId(),
            									position, 
            									urlImageViewCallback, 
            									false);
		}
	}
	
	@Override
	public void onPageScrollStateChanged(int arg0) {
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
	}

	@Override
	public void onPageSelected(int position) {
		viewPagerPosition = position;
        if (message.getImageLoadingStatus(position) == NewtifryMessage2.IMAGE_NOT_LOADED || 
        		message.getImageLoadingStatus(position) == NewtifryMessage2.IMAGE_LOADING_ERROR) {
        	showImageMenuSetVisible(position, true);
        } else {
        	showImageMenuSetVisible(position, false);
        }
	}
}
