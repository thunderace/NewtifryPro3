package com.newtifry.pro3.locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


public class QueryReceiver extends BroadcastReceiver {
	private static String LOG_TAG = "QueryReceiver";
	// Since the lifetime of a BroadcastReceiver object is very short, this Map needs to
	// have static lifetime instead of object lifetime.
//	private static SparseArray <Bundle> mDetectedEvents = new SparseArray<Bundle>();

	@Override
	public void onReceive(Context context, Intent intent) {
		if (!com.twofortyfouram.locale.Intent.ACTION_QUERY_CONDITION.equals(intent.getAction())) {
			return;
		}
		Bundle varBundle = TaskerPlugin.Event.retrievePassThroughData(intent);
		if (varBundle != null) {
			Log.d(LOG_TAG, "Found return variables for event");
			if (TaskerPlugin.Condition.hostSupportsVariableReturn(intent.getExtras())) {
				TaskerPlugin.addVariableBundle(getResultExtras(true), varBundle);
			}
			setResultCode(com.twofortyfouram.locale.Intent.RESULT_CONDITION_SATISFIED);
		} else {
			setResultCode(com.twofortyfouram.locale.Intent.RESULT_CONDITION_UNSATISFIED);
		}
	}
  }
