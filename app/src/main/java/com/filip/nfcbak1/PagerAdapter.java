package com.filip.nfcbak1;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

/**
 * Created by Filip on 26.07.2016.
 */

public class PagerAdapter extends SmartFragmentStatePagerAdapter {
    int mNumOfTabs;

    public PagerAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                FragmentAuthentication tab1 = new FragmentAuthentication();
                return tab1;
            case 1:
                FragmentRegistration tab2 = new FragmentRegistration();
                return tab2;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}