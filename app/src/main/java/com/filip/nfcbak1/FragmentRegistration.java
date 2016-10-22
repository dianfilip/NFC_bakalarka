package com.filip.nfcbak1;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Filip on 26.08.2016.
 */
public class FragmentRegistration extends Fragment {

    private Activity mainActivity;
    private NfcAdapter nfcAdpt;
    private PendingIntent nfcIntent;

    private TextView receivedMsgTextView;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            mainActivity = (Activity) context;
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nfcAdpt = NfcAdapter.getDefaultAdapter(mainActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reg, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        receivedMsgTextView = (TextView) getView().findViewById(R.id.receivedMsgTextView);
    }



    @Override
    public void onResume() {
        super.onResume();

       //dispatch();
    }

    public void dispatch() {
        nfcIntent = PendingIntent.getActivity(mainActivity.getApplicationContext(), 0, new Intent(mainActivity.getApplicationContext(), mainActivity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        /*nfcIntentFilters = new IntentFilter[1];
        nfcIntentFilters[0] = new IntentFilter();
        nfcIntentFilters[0].addAction(NfcAdapter.ACTION_TAG_DISCOVERED);*/

        nfcAdpt.enableForegroundDispatch(getActivity(), nfcIntent, null, null);
    }

}
