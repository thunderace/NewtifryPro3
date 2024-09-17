package com.newtifry.pro3;

import java.text.ParseException;

import com.newtifry.pro3.database.NewtifryDatabase;
import com.newtifry.pro3.database.NewtifryMessage2;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class MessageAdapter extends SimpleCursorAdapter {
	private final Context context;
	private final int layoutBigRow;
	private final LayoutInflater layoutInflater;
	private int defaultTextColor = 0;
	private Bitmap lockBitmap = null;
	private Bitmap stickyBitmap = null;
	
	private final class ViewHolder {
	    public TextView sourceTextView;
	    public TextView timestampTextView;
	    public TextView twoLinesMessageTextView;
	    public ImageView urlImageView;
	    public ImageView imageImageView;
	    public ImageView stickyImageView;
	}
	
	public MessageAdapter(Context context, int layoutBigRow,  Cursor c, String[] from,
			int[] to, int flag) {
		super(context, layoutBigRow, c, from, to, flag);
		this.context = context;
	    this.layoutBigRow = layoutBigRow;
	    this.layoutInflater = LayoutInflater.from(mContext);
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	    if (mCursor.moveToPosition(position)) {
	        ViewHolder viewHolder;

	        
	        if (convertView == null) {
        		convertView = this.layoutInflater.inflate(this.layoutBigRow, null);

        		viewHolder = new ViewHolder();
	            viewHolder.sourceTextView = convertView.findViewById(R.id.message_row_title_or_source);
	            viewHolder.timestampTextView = convertView.findViewById(R.id.message_row_timestamp);
	            viewHolder.twoLinesMessageTextView = convertView.findViewById(R.id.message_row_message_2_lines);
	            viewHolder.urlImageView = convertView.findViewById(R.id.message_row_url);
	            viewHolder.imageImageView = convertView.findViewById(R.id.message_row_image);
	            viewHolder.stickyImageView = convertView.findViewById(R.id.message_row_sticky);
	            convertView.setTag(viewHolder);
	        }
	        else {
	            viewHolder = (ViewHolder) convertView.getTag();
	        }
	        
			if (this.defaultTextColor == 0) {
				this.defaultTextColor = viewHolder.sourceTextView.getCurrentTextColor();
			}

			if (this.lockBitmap == null) {
				this.lockBitmap = BitmapFactory.decodeResource(this.context.getResources(), R.drawable.icon_lock);
			}
			if (this.stickyBitmap == null) {
				this.stickyBitmap = BitmapFactory.decodeResource(this.context.getResources(), R.drawable.icon_sticky);
			}
			String sourceName = mCursor.getString(mCursor.getColumnIndex(NewtifryDatabase.KEY_SOURCENAME));
			String title = mCursor.getString(mCursor.getColumnIndex(NewtifryDatabase.KEY_TITLE));
			int seen = mCursor.getInt(mCursor.getColumnIndex(NewtifryDatabase.KEY_SEEN));
			int priority = mCursor.getInt(mCursor.getColumnIndex(NewtifryDatabase.KEY_PRIORITY));
			if (Preferences.getUsePriorityColor(NewtifryMessageListActivity.context) && priority > 0 ) {
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
				viewHolder.sourceTextView.setTextColor(bg);
			} else {
				// set to default color!!!
				viewHolder.sourceTextView.setTextColor(this.defaultTextColor);
			}
			
			String sourceTxt = "";
			if (sourceName == null || sourceName.equals("")) {
				sourceTxt = title;
			} else {
				sourceTxt = sourceName + " - " + title;
			}
			
			if(seen == 0) {
				SpannableString spanString = new SpannableString(sourceTxt);
				spanString.setSpan(new UnderlineSpan(),  0,  spanString.length(), 0);
				spanString.setSpan(new StyleSpan(Typeface.BOLD),  0,  spanString.length(), 0);
				spanString.setSpan(new StyleSpan(Typeface.ITALIC),  0,  spanString.length(), 0);
				viewHolder.sourceTextView.setText(spanString);
				
			} else {
				viewHolder.sourceTextView.setTypeface(Typeface.DEFAULT);
				viewHolder.sourceTextView.setText(sourceTxt);
			}
    		String timestamp = "";
			try {
				//timestampTextView = NewtifryMessage2.formatUTCAsLocal(NewtifryMessage2.parseISO8601String(mCursor.getString(mCursor.getColumnIndex(NewtifryDatabase.KEY_TIMESTAMP))));
				timestamp = NewtifryMessage2.formatUTCAsLocal(mCursor.getString(mCursor.getColumnIndex(NewtifryDatabase.KEY_TIMESTAMP)));
			} catch( ParseException ex ) {
			}
			viewHolder.timestampTextView.setText(timestamp);

			String msg = mCursor.getString(mCursor.getColumnIndex(NewtifryDatabase.KEY_MESSAGE));
			if (!Preferences.getUseSmallRow(context)) {
				viewHolder.twoLinesMessageTextView.setText(msg + "\n");
			} else {
				viewHolder.twoLinesMessageTextView.setText(msg);
			}
			String url = mCursor.getString(mCursor.getColumnIndex(NewtifryDatabase.KEY_URL));
			if (url == null || url.equals("")) {
				viewHolder.urlImageView.setVisibility(View.INVISIBLE);
			} else {
				viewHolder.urlImageView.setVisibility(View.VISIBLE);
			}
        	String imageUrl = mCursor.getString(mCursor.getColumnIndex(NewtifryDatabase.KEY_IMAGE1));
			if (imageUrl == null || imageUrl.equals("")) {
				viewHolder.imageImageView.setVisibility(View.INVISIBLE);
			} else {
				viewHolder.imageImageView.setVisibility(View.VISIBLE);
			}
			boolean locked = mCursor.getInt(mCursor.getColumnIndex(NewtifryDatabase.KEY_LOCKED)) != 0;
			if(locked) {
				viewHolder.stickyImageView.setImageBitmap(this.lockBitmap);
				viewHolder.stickyImageView.setVisibility(View.VISIBLE);
			} else {
            	int sticky = mCursor.getInt(mCursor.getColumnIndex(NewtifryDatabase.KEY_STICKY));
    			if (sticky == 0) {
    				viewHolder.stickyImageView.setVisibility(View.INVISIBLE);
    			} else {
    				viewHolder.stickyImageView.setImageBitmap(this.stickyBitmap);
    				viewHolder.stickyImageView.setVisibility(View.VISIBLE);
	    		}
			}
	    }

	    return convertView;
	}
}
