package com.filip.nfcbak1;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.*;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Autor: Filip Dian
 *
 * Trieda predstavujuca hlavnu obrazovku aplikacie. Obsahuje kontrolu NFC, spracovavanie suboru, kontrolu registracie a prihlasenia,
 * sifrovanie pouzivatelskeho hesla a ine pomocne metody na startovanie HCE apod.
 */
public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    private ServiceIntentReceiver serviceIntentReceiver = null;
    private NfcAdapter nfcAdpt = null;

    //informacie o pouzivateloch
    private String usersFileContent = null;
    private String userInfo = null;
    private String key = null;
    private String uuid = null;
    private String user = null;
    private String password = null;

    //prvky rozhrania
    private TextView statusText = null;
    private ImageView statusImg = null;
    private ImageButton infoImgButton = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = this.getIntent();
        Log.i(TAG, "Started activity with intent: " + intent);

        if(intent.getAction().equals(Constants.NOTIFICATION_START) && intent.getExtras() != null) {
            password = intent.getExtras().getString("password");
            System.out.println("Password from notification: " + password);
        }

        statusText = (TextView) findViewById(R.id.statusText);
        statusImg = (ImageView) findViewById(R.id.statusImg);
        infoImgButton = (ImageButton) findViewById(R.id.infoImgButton);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        nfcAdpt = NfcAdapter.getDefaultAdapter(this);
        checkNFC();

        loadUsersFile();

        loginProcess();
    }

    /**
     * Zavedenie dizajnu vytvoreneho menu.
     *
     * @param menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Ovladanie menu.
     *
     * @param item
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_info) {
            infoDialog();
        } else if(id == R.id.action_logout) {
            logoutAlert();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Kontrola dostupnosti NFC.
     */
    public void checkNFC() {
        if (nfcAdpt == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle("Upozornenie");
            builder.setMessage("Zariadenie nepodporuje NFC, aplikácia nemôže pokračovať ďalej!");

            builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    finish();
                    System.exit(0);
                }
            });

            builder.setCancelable(false);
            final AlertDialog dialog = builder.create();
            dialog.show();
        }

        if (!nfcAdpt.isEnabled()) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle("Upozornenie");
            builder.setMessage("NFC nie je zapnuté, skontrolujte nastavenia!");

            builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    checkNFC();
                }
            });

            builder.setCancelable(false);
            final AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    /**
     * Startovanie HCE v mode registracie.
     *
     * @param user - prihlaseny pouzivatel
     */
    public void startServiceForRegistration(String user){
        Intent intent = new Intent(this, HceService.class);
        intent.setAction(Constants.STARTED_FROM_ACTIVITY);

        intent.putExtra("status", Constants.NOT_REGISTERED_STATE);
        intent.putExtra("user", user);

        this.startService(intent);
    }

    /**
     * Startovanie HCE v mode autentifikacie.
     */
    public void startServiceForAuthentication(){
        Intent intent = new Intent(this, HceService.class);
        intent.setAction(Constants.STARTED_FROM_ACTIVITY);

        intent.putExtra("status", Constants.REGISTERED_STATE);
        intent.putExtra("uuid", uuid);
        intent.putExtra("key", key);
        intent.putExtra("password", password);
        intent.putExtra("loggedInTime", System.currentTimeMillis());

        this.startService(intent);
    }

    /**
     * Startovanie HCE v necinnom mode.
     */
    public void startServiceWithNotLoggedUser() {
        Intent intent = new Intent(this, HceService.class);
        intent.setAction(Constants.STARTED_FROM_ACTIVITY);

        intent.putExtra("status", Constants.NOT_LOGGED_IN_STATE);

        this.startService(intent);
    }

    /**
     * Nacitanie obsahu suboru do pamate.
     * Ak sa nacitava po prvom spusteni aplikacie, subor sa najprv sam vyytvori.
     */
    public void loadUsersFile() {
        try {
            this.openFileInput("users");
        } catch (FileNotFoundException e) {
            createUsersFile();
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
     * Kontrola registracie zadaneho pouzivatela.
     *
     * @param userName - prihlaseny pouzivatel
     */
    public boolean checkIfRegistered(String userName) throws BadPaddingException {
        boolean isRegistered = false;
       Log.i(TAG, "Looking for " + userName);

        String lines[] = usersFileContent.split("\\r?\\n");

        try {
            for (String l : lines) {
                Log.i(TAG, l);

                if (l.equals("-")) {
                    continue;
                }

                String[] fields = l.split(" ");

                if (fields[1].equals(userName)) {
                    userInfo = l;
                    isRegistered = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.i(TAG, "Found user: " + userInfo);

        if(isRegistered) {
            uuid = userInfo.split(" ")[2];
            Log.i(TAG, "Registered UUID: " + uuid);

            try {
                key = decryptKey(userInfo.split(" ")[3]);
            } catch (BadPaddingException e) {
                throw new BadPaddingException();
            }
        }

        return isRegistered;
    }

    /**
     * Vycistenie prihlasenia.
     */
    public void clearLoggedUserInFile() {
        Log.i(TAG, "\nClearing logged user from file...");

        Log.i(TAG, "Users file: \n" + usersFileContent);
        Log.i(TAG, "Logged user: " + userInfo);

        String lines[] = usersFileContent.toString().split("\\r?\\n");

        StringBuilder newUsersFileContent = new StringBuilder("-");

        for(int i = 1; i < lines.length; i++) {
            newUsersFileContent.append("\n" + lines[i]);
        }

        Log.i(TAG, "New content: \n" + newUsersFileContent);

        this.saveFile(newUsersFileContent.toString());
    }

    /**
     * Zapisanie prihlasenia.
     */
    public void saveLoggedUserToFile() {
        Log.i(TAG, "\nSaving logged user to file...");

        Log.i(TAG, "Users file: \n" + usersFileContent);
        Log.i(TAG, "Logged user: " + userInfo);

        String newUserInfoFields[] = userInfo.split(" ");
        StringBuilder newUserInfoBuilder = new StringBuilder("- ");
        newUserInfoBuilder.append(newUserInfoFields[1] + " ");
        newUserInfoBuilder.append(newUserInfoFields[2] + " ");
        newUserInfoBuilder.append(newUserInfoFields[3]);

        Long tsLong = System.currentTimeMillis();
        Date loggedDate = new Date(tsLong);
        Log.i(TAG, "Logged time:" + loggedDate);

        StringBuilder newUsersFileContent = new StringBuilder(newUserInfoBuilder.toString() + " " + tsLong);

        String lines[] = usersFileContent.toString().split("\\r?\\n");

        for(int i = 1; i < lines.length; i++) {
            newUsersFileContent.append("\n" + lines[i]);
        }

        Log.i(TAG, "New content: \n" + newUsersFileContent);

        this.saveFile(newUsersFileContent.toString());
    }

    /**
     * Ulozenie novoregistrovaneho pouzivatela do suboru.
     *
     * @return newId - id noveho pouzivatela
     */
    public int saveUser() {
        String str = "";
        StringBuffer buf = new StringBuffer();

        try {
            FileInputStream fis = this.openFileInput("users");
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));

            if (fis != null) {
                while ((str = reader.readLine()) != null) {
                    buf.append(str + "\n");
                }
            }

            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String lines[] = buf.toString().split("\\r?\\n");
        int lastId;

        if(lines[lines.length - 1].charAt(0) == '-') {
            lastId = 0;
        } else {
            lastId = Integer.parseInt(lines[lines.length - 1].split(" ")[0]);
        }

        int newId = lastId + 1;

        key = key.replace("\n", "");
        String encryptedKey = this.encryptKey();

        String newUserLine = newId + " " + user + " " + uuid + " " + encryptedKey.replace("\n", "") ;
        userInfo = newUserLine;
        Log.i(TAG, "New user: " + newUserLine);

        String newContent = buf.toString() + newUserLine;
        usersFileContent = newContent;

        saveLoggedUserToFile();

        return newId;
    }

    /**
     * Ulozenie urceneho obsahu do suboru.
     *
     * @param content - novy obsah suboru na zapis
     */
    public void saveFile(String content) {
        String fileName = "users";

        try {
            FileOutputStream outputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(content.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Vytvorenie suboru po prvom spusteni aplikacie.
     */
    public void createUsersFile() {
        String fileName = "users";
        String content = "-";

        try {
            FileOutputStream outputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(content.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Nastavenie prihlasenia.
     */
    public void loginProcess() {

        startServiceWithNotLoggedUser();
        getSupportActionBar().setTitle("Neprihlásený");
        clearLoggedUserInFile();
        loginAlert();
    }

    /**
     * Vytvorenie prihlasovacieho okna.
     */
    public void loginAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Prihlásenie");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText loginBox = new EditText(this);
        loginBox.setHint("Login");
        loginBox.setText("test_1");
        loginBox.setSingleLine(true);
        layout.addView(loginBox);

        final EditText passwordBox = new EditText(this);
        passwordBox.setHint("Heslo");
        passwordBox.setSingleLine(true);
        passwordBox.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(passwordBox);

        builder.setView(layout);

        builder.setPositiveButton("Potvrdiť", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        builder.setNegativeButton("Zrušiť", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                clearLoggedUserInFile();
                finish();
            }
        });

        builder.setCancelable(false);
        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                user = loginBox.getText().toString();
                password = passwordBox.getText().toString();

                if (user == null || user.equals("") || password == null || password.equals("")) {
                    return;
                }

                if (user.length() < 5 || password.length() < 5 || user.contains(" ") || password.contains(" ")) {
                    Toast.makeText(getApplicationContext(), "Zlý formát mena alebo hesla", Toast.LENGTH_LONG).show();
                    return;
                }

                getSupportActionBar().setTitle("Prihlásený: " + user);

                loadUsersFile();

                try {

                    if (checkIfRegistered(user)) {
                        saveLoggedUserToFile();

                        statusText.setText("Zariadenie je pripravené");
                        statusImg.setImageResource(R.drawable.checkmark);
                        infoImgButton.setImageResource(android.R.color.transparent);
                        infoImgButton.setClickable(false);

                        startServiceForAuthentication();
                    } else {
                        statusText.setText("Zariadenie nie je registrované, kontaktujte administrátora");
                        statusImg.setImageResource(R.drawable.cancel);
                        infoImgButton.setImageResource(R.drawable.infobutton);
                        infoImgButton.setClickable(true);

                        infoImgButton.setOnClickListener(
                                new Button.OnClickListener() {
                                    public void onClick(View v) {
                                        infoDialog();
                                    }
                                }
                        );

                        startServiceForRegistration(user);
                    }
                } catch (BadPaddingException e) {
                    wrongPasswordAlert();
                }

                dialog.dismiss();
            }
        });
    }

    /**
     * Zobrazenie odhlasovacieho okna.
     */
    public void logoutAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Odhlásenie");
        builder.setMessage("Naozaj sa chcete odhlásiť?");

        builder.setPositiveButton("Áno", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                logout();
            }
        });

        builder.setNegativeButton("Nie", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Odhlasenie.
     */
    public void logout() {
        userInfo = null;
        getSupportActionBar().setTitle("Neprihlásený");
        startServiceWithNotLoggedUser();
        statusImg.setImageResource(android.R.color.transparent);
        statusText.setText("");
        infoImgButton.setImageResource(android.R.color.transparent);
        infoImgButton.setClickable(false);
        clearLoggedUserInFile();
        loginAlert();
    }

    /**
     * Zobrazenie upozornenia po zadani zleho hesla.
     */
    public void wrongPasswordAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Bolo zadané zlé heslo, je potrebné znova zadať prihlasovacie údaje!");

        builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                logout();
            }
        });

        builder.setCancelable(false);
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Zobrazenie okna s informaciami.
     */
    public void infoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Informácie");

        LayoutInflater inflater = getLayoutInflater();
        View alertLayout = inflater.inflate(R.layout.info_layout, null);

        builder.setView(alertLayout);

        builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Zasifrovanie privatneho kluca pouzivatela pre ulozenie do suboru.
     *
     * @return encryptedString - zasifrovany kluc
     */
    public String encryptKey() {

        String encryptedString = null;
        Log.i(TAG, "To encrypt: " + key);

        try {
            byte[] key = password.getBytes("UTF-8");
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // iba prvych 128 bitov

            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

            byte[] encrypted = cipher.doFinal((this.key).getBytes("UTF-8"));
            encryptedString = Base64.encodeToString(encrypted, Base64.DEFAULT);

            Log.i(TAG, "Encrypted key: " + encryptedString);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return encryptedString;
    }

    /**
     * Odsifrovanie privatneho kluca pouzivatela po nacitani zo suboru.
     *
     * @param encryptedKey - nacitany zasifrovany kluc
     * @return decryptedKey - odsifrovany kluc vhodny na pouzitie
     * @throws BadPaddingException
     */
    public String decryptKey(String encryptedKey) throws BadPaddingException {

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
        } catch (BadPaddingException e) {
            throw new BadPaddingException(); // nastane v pripade zadania zleho hesla
        } catch (Exception e) {
            e.printStackTrace();
        }

        return decryptedKey;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (serviceIntentReceiver == null) serviceIntentReceiver = new ServiceIntentReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.REGISTRATION_SUCCESFUL);
        intentFilter.addAction(Constants.AUTHENTICATION_SUCCESFUL);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(serviceIntentReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (serviceIntentReceiver != null)  LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(serviceIntentReceiver);
    }

    /**
     * Metoda volana pri vypnuti aplikacie, vypina beziacu HCE sluzbu, ktora sa nasledne restartuje.
     */
    @Override
    public void onDestroy() {

        Log.i(TAG, "onDestroy!");

        Intent mServiceIntent = new Intent(this, HceService.class);

        stopService(mServiceIntent);

        super.onDestroy();
    }

    /**
     * Receiver pre prijimanie sprav odoslanych HCE sluzbou.
     */
    private class ServiceIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Received intent from HCE: " + intent);

            if(intent.getAction().equals(Constants.REGISTRATION_SUCCESFUL)) {
                statusText.setText("Zariadenie je pripravené");
                statusImg.setImageResource(R.drawable.checkmark);
                infoImgButton.setImageResource(android.R.color.transparent);
                infoImgButton.setClickable(false);

                key = intent.getExtras().getString("privateKey");
                uuid = intent.getExtras().getString("uuid");

                int newId = saveUser();
                userInfo = newId + " " + user + " " + uuid + " " + key;

                startServiceForAuthentication();
            }
        }
    }

}
