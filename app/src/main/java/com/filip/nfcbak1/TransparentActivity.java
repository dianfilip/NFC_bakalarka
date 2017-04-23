package com.filip.nfcbak1;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class TransparentActivity extends AppCompatActivity {

    public static String TAG = "TRANSPARENT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = this.getIntent();
        Log.i(TAG, "" + intent);

        finish();
    }

    @Override
    public void onDestroy() {
        Intent mServiceIntent = new Intent(this, HceService.class);

        stopService(mServiceIntent);
        Log.i(TAG, "onDestroy!");
        super.onDestroy();
    }
}
