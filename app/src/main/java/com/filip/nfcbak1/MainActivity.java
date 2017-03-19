package com.filip.nfcbak1;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.*;
import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    public static int NOT_LOGGED_IN_STATE = 1;
    public static int NOT_REGISTERED_STATE = 2;
    public static int REGISTERED_STATE = 3;

    private ServiceIntentReceiver serviceIntentReceiver;
    private NfcAdapter nfcAdpt;

    private int status = NOT_LOGGED_IN_STATE;

    private String usersFileContent = null;
    private String userInfo = null;
    private String key = null;
    private String uuid = null;
    private String user = null;
    private String password = null;

    private TextView statusText = null;
    private ImageView statusImg = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = (TextView) findViewById(R.id.statusText);
        statusImg = (ImageView) findViewById(R.id.statusImg);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Neprihlásený");

        Button testBtn = (Button) findViewById(R.id.testBtn);
        assert testBtn != null;
        testBtn.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        user = "test_user";
                        uuid = "test_uuid";
                        key = "test_key";

                        saveUser();
                        loadUsersFile();
                    }
                }
        );

        nfcAdpt = NfcAdapter.getDefaultAdapter(this);
        checkNFC();

        startServiceWithNotLoggedUser();
        login();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_info) {
            infoDialog();
        } else if(id == R.id.action_logout) {
            logoutAlert();
        }

        return super.onOptionsItemSelected(item);
    }



    public void checkNFC() {
        if (nfcAdpt == null) {
            //Toast.makeText(this, "NFC not supported", Toast.LENGTH_LONG).show();

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
            //Toast.makeText(this, "Enable NFC before using the app", Toast.LENGTH_LONG).show();

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

    public void startServiceForRegistration(String user){
        Intent intent = new Intent(this, HceService.class);

        intent.putExtra("status", NOT_REGISTERED_STATE);
        intent.putExtra("user", user);
        this.startService(intent);
    }

    public void startServiceForAuthentication(){
        Intent intent = new Intent(this, HceService.class);

        intent.putExtra("status", REGISTERED_STATE);
        intent.putExtra("uuid", uuid);
        intent.putExtra("key", key);
        this.startService(intent);
    }

    public void startServiceWithNotLoggedUser() {
        Intent intent = new Intent(this, HceService.class);

        intent.putExtra("status", NOT_LOGGED_IN_STATE);
        this.startService(intent);
    }

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
            //System.out.println(buf.toString());
            usersFileContent = buf.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkIfRegistered(String userName) {
        boolean isRegistered = false;
        System.out.println("Looking for " + userName);

        String lines[] = usersFileContent.split("\\r?\\n");

        try {
            for (String l : lines) {
                System.out.println(l);

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

        System.out.println("najdeny user: " + userInfo);

        if(isRegistered) {
            uuid = userInfo.split(" ")[2];
            System.out.println(uuid);
            key = decryptKey(userInfo.split(" ")[3]);
        }

        return isRegistered;
    }

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

        System.out.println(buf.toString());

        String lines[] = buf.toString().split("\\r?\\n");
        int lastId;

        if(lines[lines.length - 1].charAt(0) == '-') {
            lastId = 0;
        } else {
            lastId = Integer.parseInt(lines[lines.length - 1].split(" ")[0]);
        }

        //System.out.println(lastId);
        int newId = lastId + 1;

        key = key.replace("\n", "");
        String encryptedKey = this.encryptKey();
        //String decryptedKey = this.decryptKey(encryptedKey);

        String newUserLine = newId + " " + user + " " + uuid + " " + encryptedKey.replace("\n", "") + " 12345";
        userInfo = newUserLine;
        System.out.println("new user: " + newUserLine);

        String newContent = buf.toString() + newUserLine;
        String fileName = "users";

        try {
            FileOutputStream outputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(newContent.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return newId;
    }

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

    public void login() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Prihlásenie");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText loginBox = new EditText(this);
        loginBox.setHint("Login");
        loginBox.setText("test1");
        layout.addView(loginBox);

        final EditText passwordBox = new EditText(this);
        passwordBox.setHint("Heslo");
        passwordBox.setText("heslo");
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
                finish();
                System.exit(0);
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

                getSupportActionBar().setTitle("Prihlásený: " + user);

                loadUsersFile();
                if (checkIfRegistered(user)) {
                    status = REGISTERED_STATE;
                    statusText.setText("Zariadenie je pripravené");
                    statusImg.setImageResource(R.drawable.checkmark);

                    startServiceForAuthentication();
                } else {
                    status = NOT_REGISTERED_STATE;
                    statusText.setText("Zariadenie nie je registrované, kontaktujte administrátora");
                    statusImg.setImageResource(R.drawable.cancel);

                    startServiceForRegistration(user);
                }

                dialog.dismiss();
            }
        });
    }

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

    public void logout() {
        userInfo = null;
        getSupportActionBar().setTitle("Neprihlásený");
        startServiceWithNotLoggedUser();
        statusImg.setImageResource(android.R.color.transparent);
        statusText.setText("");
        login();
    }

    public void wrongPasswordAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage("Bolo zadané zlé heslo, je potrebné sa znova prihlásiť!");

        builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                login();
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.show();
    }

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

    public String encryptKey() {

        String encryptedString = null;

        try {
            byte[] key = password.getBytes("UTF-8");
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // use only first 128 bit

            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

            byte[] encrypted = cipher.doFinal((this.key).getBytes("UTF-8"));
            encryptedString = Base64.encodeToString(encrypted, Base64.DEFAULT);

            System.out.println("encrypted key: " + encryptedString);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return encryptedString;
        }
    }

    public String decryptKey(String encryptedKey) {

        String decryptedKey = null;

        try {
            byte[] key = password.getBytes("UTF-8");
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // use only first 128 bit

            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

            byte[] decrypted = cipher.doFinal(Base64.decode(encryptedKey, Base64.DEFAULT));
            decryptedKey = new String(decrypted, "UTF-8");

            System.out.println("decrypted key: " + decryptedKey);
        } catch (BadPaddingException e) {
            wrongPasswordAlert();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return decryptedKey;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (serviceIntentReceiver == null) serviceIntentReceiver = new ServiceIntentReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(HceService.REGISTRATION_SUCCESFUL);
        intentFilter.addAction(HceService.AUTHENTICATION_SUCCESFUL);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(serviceIntentReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (serviceIntentReceiver != null)  LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(serviceIntentReceiver);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        System.out.println("novy intent");
    }

    private class ServiceIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println(intent);

            if(intent.getAction().equals(HceService.REGISTRATION_SUCCESFUL)) {
                statusText.setText("Zariadenie je pripravené");
                statusImg.setImageResource(R.drawable.checkmark);

                key = intent.getExtras().getString("privateKey");
                uuid = intent.getExtras().getString("uuid");

                int newId = saveUser();
                userInfo = newId + " " + user + " " + uuid + " " + key + " 12345";

                startServiceForAuthentication();
            }
        }
    }

}
