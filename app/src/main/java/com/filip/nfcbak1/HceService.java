package com.filip.nfcbak1;

import android.content.Intent;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Filip on 22.10.2016.
 */
public class HceService extends HostApduService {

    public static final String TAG = "HCE";

    public static String RECEIVED_MSG_INTENT = "receivedMsgIntent";
    public static String AUTHENTICATION_SUCCESFUL = "authenticationSuccesful";

    private int messageCounter = 0;
    private String message;
    private StringBuilder msgBuilder;

    private AsymmetricEncryption encryption = new AsymmetricEncryption();

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
            Log.i(TAG, "Application selected");
            msgBuilder = new StringBuilder();
            return sendUid();
           // return getWelcomeMessage();
        }
        else {
            /*String apduString = new String(apdu);

            Log.i(TAG, "Received: " + apduString);*/

            String keyStringPrivate = "MIICXQIBAAKBgQCh0+deNV2127KGdvxF8YKnMVzBNNm0R4BUeYNBJwMSiYRN9yDl\n" +
                    "DJS3MXYrQ91zSDxQcRBd7pteutNXdo8dtn3Q+NdLv48gVX6JwxZ0e+KyOJz0JMQS\n" +
                    "otjLIIZSsfVNoRBSsxEsZuuy/ccWPsleeDS1gfMSxxU7YxH8IgI1yHx5UQIDAQAB\n" +
                    "AoGAOh8TcCCWoaRggC7n+G7/T/FIsRO8RSWRD8X8wD+0uMmvPRlPNTTJjOo02OEs\n" +
                    "/iSplPKmwDXck69iDH3GdROAKCWTfeUzdFk3yY4ohYSPgLM51u/AmwIITaAms+r/\n" +
                    "KhpFXXl97txosZKwF8Vyy9PV7c2yZuRx+Kexv8kFS+lTXAECQQDRcbBBr4HML5LI\n" +
                    "W0aPNEAxaU6hwwKxxEF7perJGWTOONb/66hyQGK5q1jvykIahoTjB+1U1Lpqe/eu\n" +
                    "mFD+30OxAkEAxcyhgm1AeR+aJGhDmX2OCW+zw4aXA9wYRHI1jo3wKQHnCXGDaID2\n" +
                    "ZKzBZygqMYEwV5HN3QBumBx499821F4XoQJAO04Cx4anrSZnXJ4jy5bS+mrEHh+2\n" +
                    "2pkkpZtkcM7k8VO85ThYOQmsKsCu7S8LKrGeXR64gAXARziU+HYesRyM8QJBAIx7\n" +
                    "Y5JQqePc0AtfifNvuvt0vEX4RzVUkl+6hdMzeAiH82E/n8cPIPArykjLu/vg90aa\n" +
                    "pY17CxE516ikfjqigUECQQC0zx98bPTR2vY0s0A8TVhQZCi02baHhLbTcFSt4/Vh\n" +
                    "4hfqSA0ubCMoOTZ/H4TFzmNzfIkRFKh7S9SX9K00zXSB";

            //byte[] encodedKey = Base64.decode(keyStringPrivate, Base64.DEFAULT);

            byte[] decoded = null;

            try {
                PrivateKey pk = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.decode(keyStringPrivate, Base64.DEFAULT)));
                Cipher ci = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
                ci.init(Cipher.DECRYPT_MODE,pk);
                decoded = ci.doFinal(apdu);
                Log.d(TAG, "decrypted: " + new String(decoded, "UTF-8"));

                ci.init(Cipher.ENCRYPT_MODE,pk);
                decoded = ci.doFinal("ahoj".getBytes());
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (NoSuchProviderException e) {
                e.printStackTrace();
            } catch (NoSuchPaddingException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (BadPaddingException e) {
                e.printStackTrace();
            } catch (IllegalBlockSizeException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            return decoded;
        }
    }

    private byte[] getWelcomeMessage() {
        return message.getBytes();
    }

    private byte[] getNextMessage() {
        return ("Message from android: " + messageCounter++).getBytes();
    }

    private byte[] sendUid() {
        return "15555".getBytes();
    }

    private boolean selectAidApdu(byte[] apdu) {
        return apdu.length >= 2 && apdu[0] == (byte)0 && apdu[1] == (byte)0xa4 && apdu[5] == (byte)0xF0 &&
                apdu[6] == (byte)0x01 && apdu[7] == (byte)0x02 && apdu[8] == (byte)0x03 && apdu[9] == (byte)0x04 &&
                apdu[10] == (byte)0x05 && apdu[11] == (byte)0x06;
    }

    @Override
    public void onDeactivated(int reason) {
        Log.i(TAG, "Deactivated: " + reason);
    }

    public void sendMsgToAcitvity(String msg) {
        Intent i = new Intent(RECEIVED_MSG_INTENT);
        i.putExtra("message", msg);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
    }

}