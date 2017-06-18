package com.movecoachtest.stepcount.alarmreciever;

/**
 * Created by rajeesh on 17/6/17.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.result.DailyTotalResult;
import java.util.concurrent.TimeUnit;
import static com.movecoachtest.stepcount.config.config.NOTIFY_STEP_COUNT;
import static com.movecoachtest.stepcount.config.config.mClient;
import static com.movecoachtest.stepcount.config.config.TAG;
public class AlarmReceiver extends BroadcastReceiver {
   private long total = 0;
   private Context mContext;
    @Override
    public void onReceive(Context context, Intent intent) {
        // For our recurring task, we'll just display a message
        mContext=context;
        Log.i("StepCounter", "alarm running");
       // Toast.makeText(mContext, "I'm running", Toast.LENGTH_LONG).show();
        readData();
    }

    /**
     * Read the current daily step total, computed from midnight of the current day
     * on the device's current timezone.
     */
    private class VerifyDataTask extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... params) {

            com.google.android.gms.common.api.PendingResult<DailyTotalResult> result = Fitness.HistoryApi.readDailyTotal(mClient, DataType.TYPE_STEP_COUNT_DELTA);
            DailyTotalResult totalResult = result.await(30, TimeUnit.SECONDS);
            if (totalResult.getStatus().isSuccess()) {
                DataSet totalSet = totalResult.getTotal();
                total = totalSet.isEmpty()
                        ? 0
                        : totalSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
            } else {
                Log.w(TAG, "There was a problem getting the step count.");
            }


            Log.i(TAG, "Total steps: " + total);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            try{
                Intent localIntent = new Intent(NOTIFY_STEP_COUNT);
                localIntent.putExtra("stepcount",total);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(localIntent);
            }catch (IllegalStateException e){

            }

        }
    }

    private void readData() {
        new VerifyDataTask().execute();
    }

}
