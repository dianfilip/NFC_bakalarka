package com.filip.nfcbak1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private ViewPager mViewPager;
    private PagerAdapter adapter;
    private ServiceIntentReceiver serviceIntentReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
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
        });
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

    @Override
    public void onResume() {
        super.onResume();

        if (serviceIntentReceiver == null) serviceIntentReceiver = new ServiceIntentReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(HceService.RECEIVED_MSG_INTENT);
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
            FragmentAuthentication fragmentAut = (FragmentAuthentication) adapter.getRegisteredFragment(0);

            if (intent.getAction().equals(HceService.RECEIVED_MSG_INTENT)) {
                fragmentAut.setReceivedMsgTextView(intent.getExtras().getString("message"));
            }
            if (intent.getAction().equals(HceService.AUTHENTICATION_SUCCESFUL)) {
                fragmentAut.setReceivedMsgTextView("succesful");
            }
        }
    }

}
