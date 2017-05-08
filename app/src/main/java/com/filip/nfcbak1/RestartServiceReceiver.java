package com.filip.nfcbak1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Autor: Filip Dian
 *
 * Receiver pre prijimanie sprav odoslanych HCE sluzbou pri jej vypnuti.
 * Restartuje HCE sluzbu.
 */
public class RestartServiceReceiver extends BroadcastReceiver {

    public static String TAG = "RESTARTER";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Restarting service..");

        Intent newIntent = new Intent(context, HceService.class);
        newIntent.setAction(Constants.RESTARTED_FROM_BROADCAST);

        if(intent.getExtras() != null && intent.getExtras().getString("password") != null) {
            newIntent.putExtra("password", intent.getExtras().getString("password"));
        }

        Log.i(TAG, "Restarting intent: " + newIntent);

        context.startService(newIntent);
    }
}
