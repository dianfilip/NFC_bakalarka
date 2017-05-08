package com.filip.nfcbak1;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Autor: Filip Dian
 *
 * Sluzba obsluhujica komunikaciu s terminalom.
 * Dokaze bezat aj na pozadi, bez zapnutej aplikacie.
 */
public class HceService extends HostApduService {

    public static final String TAG = "HCE";

    //informacie o prihlasenom pouzivatelovi
    private String usersFileContent;
    private String user;
    private String uuid;
    private String password;
    private Long loggedInTime = null;

    //stav sluzby
    private boolean isActivityRunning = false;
    private int status = Constants.NOT_LOGGED_IN_STATE;

    //krok spracovavania APDU sprav
    private int step;

    //kluc a jeho spracovavanie
    private String privateKey;
    private StringBuilder keyBuilder;
    private Integer keyPartsCounter;

    private AsymmetricEncryption encryption = new AsymmetricEncryption();

    /**
     * Nastavenie pri nastartovani sluzby.
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        step = 0;

        Log.i(TAG, "Started with intent: " + intent);

        if(intent != null) {

            if (intent.getAction().equals(Constants.STARTED_FROM_ACTIVITY)) {

                isActivityRunning = true;

                status = intent.getExtras().getInt("status");

                if (status == Constants.REGISTERED_STATE) {

                    uuid = intent.getExtras().getString("uuid");
                    privateKey = intent.getExtras().getString("key");
                    password = intent.getExtras().getString("password");
                    loggedInTime = intent.getExtras().getLong("loggedInTime");

                    Log.i(TAG, "Started for authentication with step " + step);

                    this.pushNotification("Zariadenie je pripravené");
                } else if (status == Constants.NOT_REGISTERED_STATE) {
                    user = intent.getExtras().getString("user");

                    Log.i(TAG, "Started for registration with step " + step);

                    this.pushNotification("Čaká sa na registráciu");
                } else if (status == Constants.NOT_LOGGED_IN_STATE) {
                    Log.i(TAG, "User not logged in");

                    this.pushNotification("Používateľ nie je prihlásený");
                }
            } else {

                if (intent.getExtras() != null) {
                    password = intent.getExtras().getString("password");
                }

                try {
                    loadUsersFile();

                    if (checkIfLoggedIn()) {
                        status = Constants.REGISTERED_STATE;

                        this.pushNotification("Prihlásený používateľ: " + user);
                    } else {
                        status = Constants.NOT_LOGGED_IN_STATE;

                        this.pushNotification("Používateľ nie je prihlásený");
                    }
                } catch (Exception e) {
                    status = Constants.NOT_LOGGED_IN_STATE;

                    this.pushNotification("Nie je registrovaný žiadny používateľ");
                }
            }
        }

        return START_STICKY;
    }

    /**
     * Skontrolovanie prihlasenia a jeho platnosti.
     *
     * @return isLoggedIn - ci je pouzivatel prihlaseny a jeho platnost nevyprsala
     * @throws Exception
     */
    public boolean checkIfLoggedIn() throws Exception {
        String lines[] = usersFileContent.split("\\r?\\n");

        if(lines.length == 1) {
            throw new Exception();
        }

        String firstLine = lines[0];

        Log.i(TAG, "Checking logged user...");
        Log.i(TAG, firstLine);

        if(firstLine.length() == 1) {
            return false;
        }

        String msString = firstLine.split(" ")[4];

        loggedInTime = new Long(msString);

        if(System.currentTimeMillis() - loggedInTime > Constants.VALID_LOGIN_TIME) {
            return false;
        } else {

            user = firstLine.split(" ")[1];
            uuid = firstLine.split(" ")[2];
            privateKey = this.decryptKey(firstLine.split(" ")[3]);

            Log.i(TAG, "UUID: " + uuid);

            return true;
        }
    }

    /**
     * Nacitanie obsahu suboru do pamate.
     *
     * @throws Exception
     */
    public void loadUsersFile() throws Exception {
        try {
            this.openFileInput("users");
        } catch (FileNotFoundException e) {
            throw new Exception();
        }

        try {
            FileInputStream fis = this.openFileInput("users");

            String str = "";
            StringBuffer buf = new StringBuffer();

            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));

            if (fis != null) {
                while ((str = reader.readLine()) != null) {
                    buf.append(str + "\n");
                }
            }

            fis.close();
            usersFileContent = buf.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Desifrovanie ulozeneho kluca.
     *
     * @param encryptedKey - nacitany zasifrovany kluc
     */
    public String decryptKey(String encryptedKey) {

        String decryptedKey = null;

        try {
            byte[] key = password.getBytes("UTF-8");
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // iba prvych 128 bitov

            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

            byte[] decrypted = cipher.doFinal(Base64.decode(encryptedKey, Base64.DEFAULT));
            decryptedKey = new String(decrypted, "UTF-8");

            Log.i(TAG, "Decrypted key: " + decryptedKey);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return decryptedKey;
    }

    /**
     * Spracovanie prijimanych APDU sprav.
     *
     * @param apdu - prijata bajtova sprava
     * @param extras
     * @return byteArray - odoslana bajtova sprava
     */
    @Override
    public byte[] processCommandApdu(byte[] apdu, Bundle extras) {

        if (selectAidApdu(apdu)) {

            if (status == Constants.REGISTERED_STATE) {

                Log.i(TAG, "Logged uuid: " + uuid);

                if(loggedInTime == null || (System.currentTimeMillis() - loggedInTime > Constants.VALID_LOGIN_TIME)) {

                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setAction(Constants.LOGIN_TIMEOUT_START);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                    Log.i(TAG, "Login timed out!");

                    return "Device not ready".getBytes();
                }

                Log.i(TAG, "Application selected, starting authentication");

                step = 1;

                return uuid.getBytes();
            } else if (status == Constants.NOT_REGISTERED_STATE) {
                Log.i(TAG, "Application selected, starting registration");

                step = 1;

                return user.getBytes();
            } else {
                return "Device not ready".getBytes();
            }
        } else {

            if (status == Constants.NOT_REGISTERED_STATE) {
                if (step == 1) {
                    uuid = new String(apdu);

                    Log.i(TAG, "uuid: " + uuid);

                    step = 2;
                    keyPartsCounter = 0;
                    keyBuilder = new StringBuilder();

                    return new String("ok\0").getBytes();
                } else if (step == 2) {
                    String keyPart = new String(apdu);

                    Log.i(TAG, "key part: " + keyPart + " size: " + keyPart.length());

                    keyBuilder.append(keyPart);
                    keyPartsCounter++;

                    if (keyPartsCounter == 9) {
                        privateKey = keyBuilder.toString().replace("\\r", "").replace("\\n", "").replace("-----BEGIN RSA PRIVATE KEY-----", "").replace("-----END RSA PRIVATE KEY-----", "");

                        Log.i(TAG, "final key: " + privateKey + " size: " + privateKey.length());

                        registrationSuccesful();

                        return new String("end\0").getBytes();
                    }

                    return new String(keyPartsCounter.toString()).getBytes();
                }
            } else if (status == Constants.REGISTERED_STATE) {

                if (step == 1) {
                    Log.i(TAG, privateKey);

                    encryption.setPrivateKey(privateKey);

                    String decoded = encryption.decodeWithPrivate(apdu);

                    Log.i(TAG, "decoded: " + decoded);

                    Integer number = Integer.parseInt(decoded);

                    byte[] encoded = encryption.encodeWithPrivate((++number).toString());

                    if(!isActivityRunning) {

                        Log.i(TAG, "Starting transparent activity...");

                        Intent intent = new Intent(this, TransparentActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("password", password);

                        startActivity(intent);
                    }

                    return encoded;
                }
            }


        }

        return new String("Invalid APDU").getBytes();
    }

    /**
     * Zobrazenie notifikacie na status bare.
     *
     * @param message - pozadovana sprava
     */
    public void pushNotification(String message) {

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.locked))
                        .setSmallIcon(R.drawable.locked)
                        .setContentTitle(getApplicationContext().getString(R.string.app_name))
                        .setOngoing(true)
                        .setContentText(message);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Constants.NOTIFICATION_START);
        notificationIntent.putExtra("password", password);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setContentIntent(contentIntent);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }

    /**
     * Kontrola prijateho AID aplikacie.
     *
     * @param apdu - prijate AID
     * @return isCorrect - spravnost AID
     */
    private boolean selectAidApdu(byte[] apdu) {
         return apdu.length >= 2 && apdu[0] == (byte)0 && apdu[1] == (byte)0xa4 && apdu[5] == (byte)0xF0 &&
                  apdu[6] == (byte)0x01 && apdu[7] == (byte)0x02 && apdu[8] == (byte)0x03 && apdu[9] == (byte)0x04 &&
                               apdu[10] == (byte)0x05 && apdu[11] == (byte)0x06;
    }

    @Override
    public void onDeactivated(int reason) {
        Log.i(TAG, "Deactivated: " + reason);
    }

    /**
     * Odoslanie spravy aktivite po spravnom dokonceni registracie.
     */
    public void registrationSuccesful() {

        Intent i = new Intent(Constants.REGISTRATION_SUCCESFUL);
        i.putExtra("uuid", uuid);
        i.putExtra("privateKey", privateKey);

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "onDestroy!");

        if(status == Constants.REGISTERED_STATE && password != null) {

            Intent broadcastIntent = new Intent("RESTART_SERVICE");
            broadcastIntent.putExtra("password", password);

            sendBroadcast(broadcastIntent);
        }
    }
}