package com.filip.nfcbak1;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
/*import android.nfc.NfcEvent;*/
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Filip on 26.08.2016.
 */
public class FragmentAutentification extends Fragment /*implements
        NfcAdapter.CreateNdefMessageCallback,
        NfcAdapter.OnNdefPushCompleteCallback*/ {

    private Activity mainActivity;
    private NfcAdapter nfcAdpt;
    private PendingIntent nfcIntent;
    private IntentFilter[] nfcIntentFilters;

    private NdefMessage msg;

    private Button checkNfcButton;
    private CheckBox checkNfcBox;
    private TextView sendMsgTextView;
    private TextView receivedMsgTextView;
    private Button setMsgButton;
    private CheckBox sendingBox;

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
        return inflater.inflate(R.layout.fragment_aut, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        checkNfcButton = (Button) view.findViewById(R.id.buttonCheck);
        checkNfcBox = (CheckBox) getView().findViewById(R.id.nfcBox);
        sendMsgTextView = (TextView) getView().findViewById(R.id.sendMsgText);
        receivedMsgTextView = (TextView) getView().findViewById(R.id.receivedMsgTextView);
        setMsgButton = (Button) getView().findViewById(R.id.setMsgButton);
        sendingBox = (CheckBox) getView().findViewById(R.id.sendingBox);

        assert checkNfcButton != null;
        checkNfcButton.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        checkNFC();
                    }
                }
        );

        assert setMsgButton != null;
        setMsgButton.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                       setMsg();
                    }
                }
        );
    }

    public void checkNFC() {
        if (nfcAdpt == null) {
            Toast.makeText(mainActivity, "NFC not supported", Toast.LENGTH_LONG).show();
            checkNfcBox.setChecked(false);
        }

        if (!nfcAdpt.isEnabled()) {
            Toast.makeText(mainActivity, "Enable NFC before using the app", Toast.LENGTH_LONG).show();
            checkNfcBox.setChecked(false);
        }

        if(nfcAdpt.isEnabled()) {
            checkNfcBox.setChecked(true);
            Toast.makeText(mainActivity, "Enabled..", Toast.LENGTH_LONG).show();

            String text = sendMsgTextView.getText().toString();
           // msg = new NdefMessage(NdefRecord.createMime("text/plain", text.getBytes()));

           /*nfcAdpt.setNdefPushMessageCallback(this, mainActivity);
            nfcAdpt.setOnNdefPushCompleteCallback(this, mainActivity);*/
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SENT:
                    Toast.makeText(mainActivity, "Message sent!", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    /*//@Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        String text = sendMsgTextView.getText().toString();
        NdefMessage msg = new NdefMessage(NdefRecord.createMime("text/plain", text.getBytes()));
        return msg;
    }*/

   /* @Override
    public void onNdefPushComplete(NfcEvent event) {
        mHandler.obtainMessage(MESSAGE_SENT).sendToTarget();
    }*/

    public void setMsg() {
        String text = sendMsgTextView.getText().toString();

        Toast.makeText(mainActivity, "Message to send: " + text, Toast.LENGTH_LONG).show();

        //msg = new NdefMessage(NdefRecord.createMime("text/plain", text.getBytes()));
        NdefRecord record = createRecord(text);
        msg = new NdefMessage(new NdefRecord[] {record});

        enableNdefExchangeMode(msg);
    }

    public void dispatch() {
        nfcIntent = PendingIntent.getActivity(mainActivity.getApplicationContext(), 0, new Intent(mainActivity.getApplicationContext(), mainActivity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        nfcIntentFilters = new IntentFilter[1];
        nfcIntentFilters[0] = new IntentFilter();
        nfcIntentFilters[0].addAction(NfcAdapter.ACTION_TAG_DISCOVERED);

        nfcAdpt.enableForegroundDispatch(getActivity(), nfcIntent, null, null);
    }

    private void enableNdefExchangeMode(NdefMessage msg) {
        nfcAdpt.disableForegroundDispatch(mainActivity);
        nfcAdpt.enableForegroundNdefPush(mainActivity, msg);

        System.out.println("zapol som push");

        nfcIntent = PendingIntent.getActivity(mainActivity.getApplicationContext(), 0, new Intent(mainActivity.getApplicationContext(), mainActivity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        nfcIntentFilters = new IntentFilter[1];
        nfcIntentFilters[0] = new IntentFilter();
        nfcIntentFilters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);

        //nfcAdpt.enableForegroundDispatch(getActivity(), nfcIntent, null, null);
    }

    void processIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        message = new String(msg.getRecords()[0].getPayload());
        receivedMsgTextView.setText(message);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        checkNFC();
    }

    @Override
    public void onResume() {
        super.onResume();

        //dispatch();

        if(sendingBox.isChecked() == false) {
            Toast.makeText(mainActivity, "receiving", Toast.LENGTH_LONG).show();
            dispatch();
        } else {
            enableNdefExchangeMode(msg);
        }
    }

    private static NdefRecord createRecord(final String text)
    {
        final byte[] textBytes = text.getBytes();
        final byte[] recordBytes = new byte[textBytes.length + 1];
        recordBytes[0] = (byte) 0x0;
        System.arraycopy(textBytes, 0, recordBytes, 1, textBytes.length);
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, new byte[0], recordBytes);
    }

}
