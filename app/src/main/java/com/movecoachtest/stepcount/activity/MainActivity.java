package com.movecoachtest.stepcount.activity;
/**
 * Created by rajeesh on 17/6/17.
 */
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;

import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.DataType;
import com.movecoachtest.stepcount.R;
import com.movecoachtest.stepcount.alarmreciever.AlarmReceiver;

import java.text.DecimalFormat;
import java.util.Calendar;
import static com.movecoachtest.stepcount.config.config.NOTIFY_STEP_COUNT;
import static com.movecoachtest.stepcount.config.config.mClient;
import static com.movecoachtest.stepcount.config.config.TAG;

public class MainActivity extends AppCompatActivity {
    private TextView mTextStepCount;
    int mStartBhour=0;
    int mStartBminute=0;
    int mcurrentHour=0;
    int mcurrentMinute=0;
    long total = 0;
    private PendingIntent pendingIntent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(this, 0, alarmIntent, 0);
        mTextStepCount = (TextView) findViewById(R.id.txtStep);
        FloatingActionButton myFab = (FloatingActionButton) findViewById(R.id.fab);

        mTextStepCount.setText(commaFormat(total)+" Steps");
        buildFitnessClient();
        Calendar calendar = Calendar.getInstance();
        //calendar.setTimeInMillis(System.currentTimeMillis());
        mStartBhour=calendar.get(Calendar.HOUR_OF_DAY);
        mStartBminute=calendar.get(Calendar.MINUTE);
        Log.i(TAG, "hhhh"+mStartBhour);
        Toast.makeText(getApplicationContext(), "uuu"+mStartBhour , Toast.LENGTH_SHORT).show();

        myFab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
               showAbout();
            }
        });

    }

    /**
     * Build a {@link GoogleApiClient} to authenticate the user and allow the application
     * to connect to the Fitness APIs. The included scopes should match the scopes needed
     * by your app (see the documentation for details).
     * Use the {@link GoogleApiClient.OnConnectionFailedListener}
     * to resolve authentication failures (for example, the user has not signed in
     * before, or has multiple accounts and must specify which account to use).
     */
    private void buildFitnessClient() {
        // Create the Google API Client
        mClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.RECORDING_API)
                .addApi(Fitness.HISTORY_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {

                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.i(TAG, "Connected!!!");
                                // Subscribe to some data sources!
                                subscribe();
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                // If your connection to the sensor gets lost at some point,
                                // you'll be able to determine the reason and react to it here.
                                if (i == ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                    Log.w(TAG, "Connection lost.  Cause: Network Lost.");
                                } else if (i == ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                    Log.w(TAG, "Connection lost.  Reason: Service Disconnected");
                                }
                            }
                        }
                )
                .enableAutoManage(this, 0, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.w(TAG, "Google Play services connection failed. Cause: " +
                                result.toString());
                    }
                })
                .build();
    }

    /**
     * Record step data by requesting a subscription to background step data.
     */
    public void subscribe() {
        // To create a subscription, invoke the Recording API. As soon as the subscription is
        // active, fitness data will start recording.
        Fitness.RecordingApi.subscribe(mClient, DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            if (status.getStatusCode()
                                    == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                                Log.i(TAG, "Existing subscription for activity detected.");
                            } else {
                                Log.i(TAG, "Successfully subscribed!");
                            }
                        } else {
                            Log.w(TAG, "There was a problem subscribing.");
                        }
                    }
                });
    }


    @Override
    protected void onResume() {
        super.onResume();
          IntentFilter customFilter =
                new IntentFilter(NOTIFY_STEP_COUNT);
        LocalBroadcastManager.getInstance(this).registerReceiver(mLocalReceiver,
                customFilter);
        cancel();
        readStepInForeground();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Toast.makeText(this, "OnPause", Toast.LENGTH_SHORT).show();
        cancel();
        readStepInBackground();

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancel();
    }

    public void readStepInForeground() {
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        int interval = 1000*60*5;
          /* Repeating on every 5 minutes  interval */
        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
      //  Toast.makeText(this, "Alarm Set", Toast.LENGTH_SHORT).show();
    }

    public void cancel() {
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pendingIntent);
        Toast.makeText(this, "Alarm Canceled", Toast.LENGTH_SHORT).show();
    }

    public void readStepInBackground() {
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        int interval = 1000*60*60;
        int hour = 0;

        /* Set the alarm to start */
        Calendar calendar = Calendar.getInstance();
        mcurrentHour=calendar.get(Calendar.HOUR_OF_DAY);
        mcurrentMinute=calendar.get(Calendar.MINUTE);
        if(mcurrentHour>=mStartBhour && mcurrentMinute>=mStartBminute){
            hour=mcurrentHour+1;
        }else {
            hour=mcurrentHour;
        }
        Toast.makeText(getApplicationContext(), mcurrentHour+"uuu"+mStartBhour+"zzz"+mStartBminute+"ddd"+mcurrentMinute+"ss" +hour, Toast.LENGTH_SHORT).show();
        calendar.setTimeInMillis(System.currentTimeMillis());

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, mStartBminute);
        Log.i(TAG, "alarm started");

        /* Repeating on every one hour interval */
        manager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                interval, pendingIntent);
    }
    private BroadcastReceiver mLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            total=intent.getLongExtra("stepcount",0);
            mTextStepCount.setText(commaFormat(total)+" Steps");
        }
    };
    private String commaFormat(long number){
        DecimalFormat formatter = new DecimalFormat("#,###,###");
        return formatter.format(number);
    }
    private void showAbout() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage("Developed by Rajeesh adambil")
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })

                .create();
        dialog.show();
    }

}