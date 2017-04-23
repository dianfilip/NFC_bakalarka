package com.filip.nfcbak1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Filip on 01.04.2017.
 */
public class RestartServiceReceiver extends BroadcastReceiver {

    public static String TAG = "RESTARTER";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "restarting service..");

        Intent newIntent = new Intent(context, HceService.class);
        newIntent.setAction(Constants.RESTARTED_FROM_BROADCAST);

        if(intent.getExtras() != null && intent.getExtras().getString("password") != null) {
            newIntent.putExtra("password", intent.getExtras().getString("password"));
        }

        System.out.println("Restarting intent: " + newIntent);

        context.startService(newIntent);
    }
}
