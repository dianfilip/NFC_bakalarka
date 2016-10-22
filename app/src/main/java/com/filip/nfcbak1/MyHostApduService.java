package com.filip.nfcbak1;

import android.content.Intent;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by Filip on 22.10.2016.
 */
public class MyHostApduService extends HostApduService {

    private int messageCounter = 0;
    private String message;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Check if intent has extras
        if(intent.getExtras() != null){

            // Get message
            message = intent.getExtras().getString("message");
        }

        return START_NOT_STICKY;
    }

    @Override
    public byte[] processCommandApdu(byte[] apdu, Bundle extras) {
        if (selectAidApdu(apdu)) {
            Log.i("HCEDEMO", "Application selected");
            return getWelcomeMessage();
        }
        else {
            String apduString = new String(apdu);

            switch (apduString) {
                case "ahoj":
                    Log.i("HCEDEMO", "Received: " + apduString);
                    return new String("Vitaj").getBytes();

                default:
                    Log.i("HCEDEMO", "Invalid APDU");
                    return new String("Invalid APDU").getBytes();
            }

            /*Log.i("HCEDEMO", "Received: " + new String(apdu));
            return getNextMessage();*/
        }
    }

    private byte[] getWelcomeMessage() {
        return message.getBytes();
    }

    private byte[] getNextMessage() {
        return ("Message from android: " + messageCounter++).getBytes();
    }

    private boolean selectAidApdu(byte[] apdu) {
        return apdu.length >= 2 && apdu[0] == (byte)0 && apdu[1] == (byte)0xa4;
    }

    @Override
    public void onDeactivated(int reason) {
        Log.i("HCEDEMO", "Deactivated: " + reason);
    }
}