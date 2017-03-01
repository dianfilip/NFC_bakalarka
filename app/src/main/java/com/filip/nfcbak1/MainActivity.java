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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.*;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    public static int NOT_LOGGED_IN_STATE = 1;
    public static int NOT_REGISTERED_STATE = 2;
    public static int REGISTERED_STATE = 3;

    private ServiceIntentReceiver serviceIntentReceiver;
    private NfcAdapter nfcAdpt;

    private boolean loggedIn = false;
    private int loggedInId = 0;
    private String usersFileContent = null;
    private int status = NOT_LOGGED_IN_STATE;
    private String userInfo = null;

    private TextView statusText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = (TextView) findViewById(R.id.statusText);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle("Neprihlásený");

        nfcAdpt = NfcAdapter.getDefaultAdapter(this);
        checkNFC();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Prihlásenie");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText loginBox = new EditText(this);
        loginBox.setHint("Login");
        layout.addView(loginBox);

        final EditText passwordBox = new EditText(this);
        passwordBox.setHint("Heslo");
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
                String user = loginBox.getText().toString();

                if (user == null || user.equals("")) {
                    return;
                }

                getSupportActionBar().setTitle("Prihlásený: " + user);

                loadUsersFile();
                if (checkIfRegistered(user)) {
                    status = REGISTERED_STATE;
                    statusText.setText("Status: pripravene k pouzitiu");
                } else {
                    status = NOT_REGISTERED_STATE;
                    statusText.setText("Status: prihlaseny uzivatel nie je registrovany, priloz k terminalu pre dokoncenie registracie");

                    startServiceForRegistration(user);
                }

                dialog.dismiss();
            }
        });

        Button testBtn = (Button) findViewById(R.id.testBtn);
        assert testBtn != null;
        testBtn.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        AsymmetricEncryption.testMethod();
                    }
                }
        );


        /*TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.addTab(tabLayout.newTab().setText("Autentification"));
        tabLayout.addTab(tabLayout.newTab().setText("Registration"));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        mViewPager = (ViewPager) findViewById(R.id.container);
        adapter = new PagerAdapter
                (getSupportFragmentManager(), tabLayout.getTabCount());
        mViewPager.setAdapter(adapter);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });*/
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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    public void checkNFC() {
        if (nfcAdpt == null) {
            Toast.makeText(this, "NFC not supported", Toast.LENGTH_LONG).show();
            //checkNfcBox.setChecked(false);
        }

        if (!nfcAdpt.isEnabled()) {
            Toast.makeText(this, "Enable NFC before using the app", Toast.LENGTH_LONG).show();
            //checkNfcBox.setChecked(false);
        }

        if(nfcAdpt.isEnabled()) {
            //checkNfcBox.setChecked(true);
            Toast.makeText(this, "Enabled..", Toast.LENGTH_LONG).show();
        }
    }

    public void startServiceForRegistration(String user){
        Intent intent = new Intent(this, HceService.class);

        intent.putExtra("status", NOT_REGISTERED_STATE);
        intent.putExtra("user", user);
        this.startService(intent);
    }

    public void loadUsersFile() {
        //AssetManager am = this.getAssets();

        FileInputStream fis = null;

        try {
            this.openFileInput("users");
        } catch (FileNotFoundException e) {
            createUsersFile();
        }

        try {
            fis = this.openFileInput("users");

            String str = "";
            StringBuffer buf = new StringBuffer();

            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));

            if (fis != null) {
                while ((str = reader.readLine()) != null) {
                    buf.append(str + "\n");
                }
            }

            fis.close();
            System.out.println(buf.toString());
            usersFileContent = buf.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkIfRegistered(String userName) {
        boolean isRegistered = false;
        System.out.println("Looking for " + userName);

        String lines[] = usersFileContent.split("\\r?\\n");

        for(String l : lines) {
            //System.out.println(l);

            String[] fields = l.split(" ");

            if(fields[1].equals(userName)) {
                userInfo = l;
                isRegistered = true;
            }
        }

        System.out.println(userInfo);
        return isRegistered;
    }

    public void createUsersFile() {
        String fileName = "users";
        String content = "id user uuid private_key last_number\n" +
                         "1 test_user 123456 test_key 100\n";

        FileOutputStream outputStream = null;

        try {
            outputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(content.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public int getLoggedInId() {
        return loggedInId;
    }

    public void setLoggedInId(int loggedInId) {
        this.loggedInId = loggedInId;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (serviceIntentReceiver == null) serviceIntentReceiver = new ServiceIntentReceiver();
        IntentFilter intentFilter = new IntentFilter();
        /*intentFilter.addAction(HceService.REGISTRATION_SUCCESFUL);
        intentFilter.addAction(HceService.AUTHENTICATION_SUCCESFUL);*/
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
            if(intent.getAction().equals(HceService.REGISTRATION_SUCCESFUL)) {
                statusText.setText("Status: Registracia dokoncena, pripravene pouzitiu");
            }
        }
    }

}
