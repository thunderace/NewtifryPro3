

package com.newtifry.pro3;

import com.newtifry.pro3.database.NewtifryMessage2;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;

public class AlarmJobService extends JobService {
	@Override
    public boolean onStartJob(JobParameters params) {
		Context context = this.getApplicationContext();
		NewtifryMessage2.purgeAll(context);
		return true;
    }
 
    @Override
    public boolean onStopJob(JobParameters params) {
		return true;
    }
}
