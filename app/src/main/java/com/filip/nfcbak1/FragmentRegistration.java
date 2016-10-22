package com.filip.nfcbak1;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
//import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Filip on 26.08.2016.
 */
public class FragmentRegistration extends Fragment /*implements NfcAdapter.CreateNdefMessageCallback,
        NfcAdapter.OnNdefPushCompleteCallback*/ {

    private Activity mainActivity;
    private NfcAdapter nfcAdpt;
    private PendingIntent nfcIntent;

    private TextView receivedMsgTextView;

    private static final int MESSAGE_SENT = 1;

    private String message;

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
        //receivedMsgTextView = (TextView) getView().findViewById(R.id.receivedMsgTextView);

        //nfcAdpt.setNdefPushMessageCallback(this,mainActivity);
        //nfcAdpt.setOnNdefPushCompleteCallback(this, mainActivity);
    }

    /*void processIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        message = new String(msg.getRecords()[0].getPayload());
        receivedMsgTextView.setText(message);
    }*/

    @Override
    public void onResume() {
        super.onResume();

        // Check to see that the Activity started due to an Android Beam
        /*if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(mainActivity.getIntent().getAction())) {
            processIntent(mainActivity.getIntent());
        }*/
        //dispatch();
    }

    public void dispatch() {
        nfcIntent = PendingIntent.getActivity(mainActivity.getApplicationContext(), 0, new Intent(mainActivity.getApplicationContext(), mainActivity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        /*nfcIntentFilters = new IntentFilter[1];
        nfcIntentFilters[0] = new IntentFilter();
        nfcIntentFilters[0].addAction(NfcAdapter.ACTION_TAG_DISCOVERED);*/

        nfcAdpt.enableForegroundDispatch(getActivity(), nfcIntent, null, null);
    }

    /*@Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        return null;
    }

    @Override
    public void onNdefPushComplete(NfcEvent event) {

    }*/
}
