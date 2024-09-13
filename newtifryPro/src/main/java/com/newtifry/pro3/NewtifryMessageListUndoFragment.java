package com.newtifry.pro3;

import java.text.ParseException;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.text.Html;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.newtifry.pro3.database.NewtifryDatabase;
import com.newtifry.pro3.database.NewtifryMessage2;
import com.newtifry.pro3.database.NewtifryProvider;
import com.newtifry.pro3.swipetodismiss.SwipeToDeleteCursorWrapper;
import com.newtifry.pro3.EnhancedListView.EnhancedListView;
import com.newtifry.pro3.utils.UniversalNotificationManager;

/**
 * A list fragment representing a list of NewtifryMessages. This fragment also
 * supports tablet devices by allowing list items to be given an 'activated'
 * state upon selection. This helps indicate which item is currently being
 * viewed in a {@link NewtifryMessageDetailFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class NewtifryMessageListUndoFragment extends ListFragment implements
LoaderManager.LoaderCallbacks<Cursor> , OnSharedPreferenceChangeListener {
    // An adapter between a Cursor and the MessageFragment's ListView
	private SimpleCursorAdapter adapter;
	private static boolean sortByPriority = false;
	private static boolean sortBySource = false;
	private EnhancedListView mListView;
    static final int NEWTIFRYPRO_LIST_LOADER = 0x01;
    private long lastDeletedMessageId = -1;
    private long lastItemClicked = -1;
    private int lastPositionClicked = -1;
    private Parcelable listState = null;
    
    private static final String LAST_POSITION_KEY = "lastPosition";
    private static final String LAST_ITEM_CLICKED_KEY = "lastItemClicked";
    private static final String LAST_LISTVIEW_STATE = "lastListViewState";
    private static final String FIRST_VISIBLE_POSITION = "firstVisiblePosition";
	private Callbacks activityCallbacks = sDummyCallbacks;
	public interface Callbacks {
		public void onItemSelected(long id);
	}

	private static Callbacks sDummyCallbacks = new Callbacks() {
		@Override
		public void onItemSelected(long id) {
		}
	};

    private static final String[] UI_BINDING_FROM = {
    	NewtifryDatabase.KEY_SOURCENAME,
    	NewtifryDatabase.KEY_TITLE, 
    	NewtifryDatabase.KEY_TIMESTAMP, 
    	NewtifryDatabase.KEY_MESSAGE, 
    	NewtifryDatabase.KEY_PRIORITY, 
    	NewtifryDatabase.KEY_URL, 
    	NewtifryDatabase.KEY_IMAGE1,
    	NewtifryDatabase.KEY_IMAGE2,
    	NewtifryDatabase.KEY_IMAGE3,
    	NewtifryDatabase.KEY_IMAGE4,
    	NewtifryDatabase.KEY_IMAGE5,
    	NewtifryDatabase.KEY_SEEN,
    	NewtifryDatabase.KEY_STICKY,
		NewtifryDatabase.KEY_DELETED ,
		NewtifryDatabase.KEY_HASHCOUNT,
		NewtifryDatabase.KEY_OTHERTIMESTAMP
	};
    
    private static final int[] UI_BINDING_TO = { 
    	R.id.message_row_title_or_source, 
        R.id.message_row_timestamp,
        R.id.message_row_message_2_lines,
    	R.id.message_row_url,
    	R.id.message_row_image,
    	R.id.message_row_sticky
    };

	public NewtifryMessageListUndoFragment() {
	}

	public void setSort(boolean sortByPriority, boolean sortBySource) {
		boolean changed = false;
		if (NewtifryMessageListUndoFragment.sortBySource != sortBySource || NewtifryMessageListUndoFragment.sortByPriority != sortByPriority) {
			changed = true;
		}
		NewtifryMessageListUndoFragment.sortBySource = sortBySource;
		NewtifryMessageListUndoFragment.sortByPriority = sortByPriority;
		if (changed == true) {
            getLoaderManager().restartLoader(NEWTIFRYPRO_LIST_LOADER, null, (LoaderCallbacks<Cursor>) this);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PreferenceManager.getDefaultSharedPreferences(this.getActivity()).registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.list_fragment, container, false);
        mListView = (EnhancedListView)view.findViewById(android.R.id.list);
        mListView.setDismissCallback(new com.newtifry.pro3.EnhancedListView.EnhancedListView.OnDismissCallback() {
            @Override
            public EnhancedListView.Undoable onDismiss(EnhancedListView listView, final int position) {
       			// get message at this position, remove it from ddb and redraw
       			Cursor cursor = (Cursor) adapter.getItem(position);
       			if (lastDeletedMessageId != -1) {
       				// delete prec message
       				NewtifryProvider.deleteItem(getActivity().getApplicationContext(), lastDeletedMessageId);
       			}
       			lastDeletedMessageId =  cursor.getLong(cursor.getColumnIndex(NewtifryDatabase.KEY_ID));
       			NewtifryMessage2 msg = NewtifryMessage2.get(getActivity().getApplicationContext(), lastDeletedMessageId);
       			msg.setDeleted(true);
       			msg.save(getActivity().getApplicationContext());
            	if (NewtifryMessageListActivity.isTwoPane() && lastItemClicked == lastDeletedMessageId) {
            		lastPositionClicked = -1;
            		lastItemClicked = -1;
            		// clear the message detail view
            		Bundle arguments = new Bundle();
            		NewtifryMessageDetailFragment fragment = new NewtifryMessageDetailFragment();
            		fragment.setArguments(arguments);
            		getActivity().getSupportFragmentManager().beginTransaction()
							.replace(R.id.newtifrymessage_detail_container, fragment)
							.commit();
            		// inform the mother activity of the deletion
            		activityCallbacks.onItemSelected(lastItemClicked);
            	}
               	// new for test anti flicker
               	SwipeToDeleteCursorWrapper cursorWrapper = new SwipeToDeleteCursorWrapper(adapter.getCursor(), position);
               	adapter.swapCursor(cursorWrapper);
               	if (Preferences.isUndoEnable(getActivity()) == false) {
               		return null;
               	}
                return new EnhancedListView.Undoable() {
                    @Override
                    public void undo() {
               			NewtifryMessage2 msg = NewtifryMessage2.get(getActivity().getApplicationContext(), lastDeletedMessageId);
               			if (msg != null) {
               				msg.setDeleted(false);
               				msg.save(getActivity().getApplicationContext());
               			}
               			lastDeletedMessageId = -1;
                    }
                    @Override
                    public void discard() {
               			if (lastDeletedMessageId != -1) {
               				NewtifryProvider.deleteItem(getActivity().getApplicationContext(), lastDeletedMessageId);
               				lastDeletedMessageId = -1;
               			}
                    }
                };
            }
        }, view);
        mListView.setShouldSwipeCallback(new com.newtifry.pro3.EnhancedListView.EnhancedListView.OnShouldSwipeCallback() {
            @Override
            public boolean onShouldSwipe(EnhancedListView listView, final int position) {
       			Cursor cursor = (Cursor) adapter.getItem(position);
            	boolean seen = cursor.getInt(cursor.getColumnIndex(NewtifryDatabase.KEY_SEEN)) == 0 ? false : true;
            	boolean locked = cursor.getInt(cursor.getColumnIndex(NewtifryDatabase.KEY_LOCKED)) == 0 ? false : true;
            	boolean sticky = cursor.getInt(cursor.getColumnIndex(NewtifryDatabase.KEY_STICKY)) == 0 ? false : true;
            	if ((locked == true || sticky == true) && Preferences.getAllowDeletePinnedAndLockedMessages(getActivity()) == false) {
        			return false;
        		}
            	if (seen == false && Preferences.getDeleteUnseenMessages(getActivity()) == false) {
        			return false;
        		}
       			return true;
            }
        });
        	
        mListView.enableSwipeToDismiss();
        mListView.setUndoStyle(EnhancedListView.UndoStyle.SINGLE_POPUP);
        mListView.setSwipeDirection(EnhancedListView.SwipeDirection.BOTH);
        mListView.setRequireTouchBeforeDismiss(false);
        mListView.setUndoHideDelay(4000);
		return view;
	}		
	
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListView.setCacheColorHint(Color.TRANSPARENT); // Improves scrolling performance
        getLoaderManager().initLoader(NEWTIFRYPRO_LIST_LOADER, null, this);
        
        if (Preferences.getUseSmallRow(getActivity()) == true) {
	        adapter = new SimpleCursorAdapter(
	        		getActivity().getApplicationContext(), 
	        		R.layout.message_list_row4_1line,
	                null, UI_BINDING_FROM, UI_BINDING_TO,
	                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        } else {
	        adapter = new SimpleCursorAdapter(
	        		getActivity().getApplicationContext(), 
	        		R.layout.message_list_row4,
	                null, UI_BINDING_FROM, UI_BINDING_TO,
	                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        	
        }
        adapter.setViewBinder(new NewtifryProViewBinder());
        mListView.setAdapter(adapter);;
	}
	private String getTextMessage(String msg) {
		try {
			return Html.fromHtml(msg).toString();
		} catch (Exception ex) {
			return msg;
		}
	}

    private class NewtifryProViewBinder implements SimpleCursorAdapter.ViewBinder {
    	private int defaultTextColor = 0;
        @Override
        public boolean setViewValue(View view, Cursor cursor, int index) {
        	if(view.getId() == R.id.message_row_title_or_source) {
    			String sourceName = cursor.getString(cursor.getColumnIndex(NewtifryDatabase.KEY_SOURCENAME));
    			String title = cursor.getString(cursor.getColumnIndex(NewtifryDatabase.KEY_TITLE));
    			int hashCount = cursor.getInt(cursor.getColumnIndex(NewtifryDatabase.KEY_HASHCOUNT));
    			int seen = cursor.getInt(cursor.getColumnIndex(NewtifryDatabase.KEY_SEEN));
    			int priority = cursor.getInt(cursor.getColumnIndex(NewtifryDatabase.KEY_PRIORITY));
    			TextView sourceTextView = (TextView)view;
    			if (defaultTextColor == 0) {
    				defaultTextColor = sourceTextView.getCurrentTextColor();
    			}
    			if (Preferences.getUsePriorityColor(NewtifryMessageListActivity.context) == true && priority > 0 ) {
    				int bg = -1;
    				switch (priority) {
    					case 1 :
    						bg = Preferences.getInfoTitleColor(NewtifryMessageListActivity.context);
    						break;
    					case 2 : 
    						bg = Preferences.getWarningTitleColor(NewtifryMessageListActivity.context);
    						break;
    					case 3 :
    						bg = Preferences.getAlertTitleColor(NewtifryMessageListActivity.context);
    						break;
    				}
    				sourceTextView.setTextColor(bg);
    			} else {
    				// set to default color!!!
    				sourceTextView.setTextColor(defaultTextColor);
    			}
    			
    			String sourceTxt = "";
    			if (sourceName == null || sourceName.equals("")) {
    				sourceTxt = title;
    			} else {
    				sourceTxt = sourceName + " - " + title;
    			}

    			if (hashCount > 1) {
    				sourceTxt += " (" + Integer.toString(hashCount)+")";
				}

    			if(seen == 0) {
    				SpannableString spanString = new SpannableString(sourceTxt);
    				spanString.setSpan(new UnderlineSpan(),  0,  spanString.length(), 0);
    				spanString.setSpan(new StyleSpan(Typeface.BOLD),  0,  spanString.length(), 0);
    				spanString.setSpan(new StyleSpan(Typeface.ITALIC),  0,  spanString.length(), 0);
    				sourceTextView.setText(spanString);
    				
    			} else {
    				sourceTextView.setTypeface(Typeface.DEFAULT);
    				sourceTextView.setText(sourceTxt);
    			}

    			return true;
        	}
        	
        	if(view.getId() == R.id.message_row_timestamp) {
    			TextView timestampTextView = (TextView)view;
        		String timestamp = "";
				try {
					//timestampTextView = NewtifryMessage2.formatUTCAsLocal(NewtifryMessage2.parseISO8601String(cursor.getString(cursor.getColumnIndex(NewtifryDatabase.KEY_TIMESTAMP))));
					timestamp = NewtifryMessage2.formatUTCAsLocal(cursor.getString(cursor.getColumnIndex(NewtifryDatabase.KEY_TIMESTAMP)));
				} catch( ParseException ex ) {
				}
				timestampTextView.setText(timestamp);
				return true;
        	}

        	if(view.getId() == R.id.message_row_message_2_lines) {
    			String msg = cursor.getString(cursor.getColumnIndex(NewtifryDatabase.KEY_MESSAGE));
    			msg = getTextMessage(msg);
    			// TODO use spannable
    			TextView twoLinesMessageTextView = (TextView)view;
    			if (Preferences.getUseSmallRow(getActivity().getApplicationContext()) == false) {
    				twoLinesMessageTextView.setText(msg + "\n");
    			} else {
    				twoLinesMessageTextView.setText(msg);
    			}
				return true;
        	}
        	 
        	if(view.getId() == R.id.message_row_url) {
    			String url = cursor.getString(cursor.getColumnIndex(NewtifryDatabase.KEY_URL));
    			ImageView urlImageView = (ImageView)view;
    			if (url == null || url.equals("")) {
    				urlImageView.setVisibility(View.INVISIBLE);
    			} else {
    				urlImageView.setVisibility(View.VISIBLE);
    			}
        		return true;
        	}        	

        	if(view.getId() == R.id.message_row_image) {
            	String imageUrl = cursor.getString(cursor.getColumnIndex(NewtifryDatabase.KEY_IMAGE1));
    			ImageView imageImageView = (ImageView)view;
    			if (imageUrl == null || imageUrl.equals("")) {
    				imageImageView.setVisibility(View.INVISIBLE);
    			} else {
    				imageImageView.setVisibility(View.VISIBLE);
    			}
        		return true;
        	}

        	if(view.getId() == R.id.message_row_sticky) {
    			ImageView stickyImageView = (ImageView)view;
    			boolean locked = cursor.getInt(cursor.getColumnIndex(NewtifryDatabase.KEY_LOCKED)) == 0 ? false : true;
    			if(locked == true) {
    				stickyImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.icon_lock));
    				stickyImageView.setVisibility(View.VISIBLE);
    			} else {
	            	int sticky = cursor.getInt(cursor.getColumnIndex(NewtifryDatabase.KEY_STICKY));
	    			if (sticky == 0) {
	    				stickyImageView.setVisibility(View.INVISIBLE);
	    			} else {
	    				stickyImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.icon_sticky));
		    			stickyImageView.setVisibility(View.VISIBLE);
		    		}
    			}
        		return true;
        	}
        	return false;
        }
    }

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            lastItemClicked = savedInstanceState.getLong(LAST_ITEM_CLICKED_KEY, -1);
            lastPositionClicked = savedInstanceState.getInt(LAST_POSITION_KEY, -1);
            listState = savedInstanceState.getParcelable(LAST_LISTVIEW_STATE);
            if (listState != null) {
            	mListView.onRestoreInstanceState(listState);
            }
        }
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callbacks.");
		}
		activityCallbacks = (Callbacks) activity;
	}

	
    

	@Override
	public void onDetach() {
		super.onDetach();
		// Reset the active callbacks interface to the dummy implementation.
		activityCallbacks = sDummyCallbacks;
	}

    @Override
    public void onStop() {
        if(mListView != null) {
   			if (lastDeletedMessageId != -1) {
   				// delete prec message
   				NewtifryProvider.deleteItem(getActivity().getApplicationContext(), lastDeletedMessageId);
   			}
            mListView.discardUndo();
        }
        super.onStop();
    }
	
    @Override
    public void onPause() {
        super.onPause();
        refreshLoader = false;
    }
	
	@Override
	public void onResume() {
		super.onResume();
		UniversalNotificationManager.getInstance(getActivity()).resetNewMessagesCount();
		if (refreshLoader == true) {
			getLoaderManager().restartLoader(NEWTIFRYPRO_LIST_LOADER, null, (LoaderCallbacks<Cursor>) this);
		}
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position,
			long id) {
		if (NewtifryMessageListActivity.isTwoPane() && id == lastItemClicked) {
            // not changing selection in dual pane, do nothing
            return;
        }
        NewtifryProvider.markItemRead(getActivity().getApplicationContext(), id);
        
        lastItemClicked = id;

		// Notify the active callbacks interface (the activity, if the
		// fragment is attached to one) that an item has been selected.
		activityCallbacks.onItemSelected(id);
        // v11+ highlights
        lastPositionClicked = position;
        if (NewtifryMessageListActivity.isTwoPane()) {
        	listView.setItemChecked(position, true);
        }
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
        outState.putLong(LAST_ITEM_CLICKED_KEY, lastItemClicked);
        outState.putInt(LAST_POSITION_KEY, lastPositionClicked);
        outState.putInt(FIRST_VISIBLE_POSITION, getListView().getFirstVisiblePosition());

        // get top list position
        listState = getListView().onSaveInstanceState();
        outState.putParcelable(LAST_LISTVIEW_STATE, listState);
	}

	/**
	 * Turns on activate-on-click mode. When this mode is on, list items will be
	 * given the 'activated' state when touched.
	 */
	public void setActivateOnItemClick(boolean activateOnItemClick) {
		// When setting CHOICE_MODE_SINGLE, ListView will automatically
		// give items the 'activated' state when touched.
		getListView().setChoiceMode(
				activateOnItemClick ? ListView.CHOICE_MODE_SINGLE
						: ListView.CHOICE_MODE_NONE);
	}

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
      String sort = NewtifryDatabase.KEY_LOCKED + " DESC, " + NewtifryDatabase.KEY_STICKY + " DESC, ";
    	if (sortByPriority) {
			sort += NewtifryDatabase.KEY_PRIORITY + " DESC, ";
		}
		if (sortBySource) {
			sort += NewtifryDatabase.KEY_SOURCENAME + " ASC ";
		} else {
			sort += NewtifryDatabase.KEY_TIMESTAMP + " DESC ";
		}
		String selectionArgs = NewtifryDatabase.KEY_DELETED + "=0";
		if (Preferences.showInvisibleMessages(getActivity()) == false) {
			selectionArgs +=  " AND "+ NewtifryDatabase.KEY_PRIORITY + " >= 0";
		}
        CursorLoader cursorLoader = new CursorLoader(getActivity(), NewtifryProvider.CONTENT_URI_MESSAGES,
        		NewtifryDatabase.MESSAGE_PROJECTION, selectionArgs, null, sort);
   		return cursorLoader;
    }
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        adapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }
    private boolean refreshLoader = false;
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		// TODO : Handle showPriorityColor, showInvisibleMessages, useSmallRows, cleanOlderThan, cleanMessageLimit
		if (key.equals(Preferences.USE_PRIORITY_COLORS) == true || 
				key.equals(Preferences.SHOW_INVISIBLE) == true || 	
				key.equals(Preferences.SMALL_ROW) == true ||
				key.equals(Preferences.ALERT_TITLE_COLOR) == true ||
				key.equals(Preferences.WARNING_TITLE_COLOR) == true ||
				key.equals(Preferences.INFO_TITLE_COLOR) == true
				) {
			refreshLoader = true;
			return;
		}
		if (key.equals(Preferences.MAX_MESSAGE_COUNT) == true ) {
            NewtifryMessage2.purge(NewtifryPro2App.getContext());
			refreshLoader = true;
			return;
		}
		if (key.equals(Preferences.AUTO_CLEAN_MESSAGE_DAYS) == true ) {
            NewtifryMessage2.deleteOlderThan(NewtifryPro2App.getContext());
			refreshLoader = true;
			return;
		}
	}
}
