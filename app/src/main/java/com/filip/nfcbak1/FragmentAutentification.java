package com.filip.nfcbak1;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.IBinder;
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
public class FragmentAutentification extends Fragment {

    private Activity mainActivity;
    private NfcAdapter nfcAdpt;
    private PendingIntent nfcIntent;
    private IntentFilter[] nfcIntentFilters;

    private Button checkNfcButton;
    private CheckBox checkNfcBox;
    private TextView sendMsgTextView;
    private TextView receivedMsgTextView;
    private Button setMsgButton;

    private PackageManager packetManager;

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
                        sendDataToService();
                    }
                }
        );
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        checkNFC();

        packetManager = mainActivity.getPackageManager();
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
        }
    }

    public void sendDataToService(){
        Intent intent = new Intent(mainActivity, MyHostApduService.class);
        intent.putExtra("message", sendMsgTextView.getText().toString());
        mainActivity.startService(intent);
    }

    public void dispatch() {
        nfcIntent = PendingIntent.getActivity(mainActivity.getApplicationContext(), 0, new Intent(mainActivity.getApplicationContext(), mainActivity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        nfcIntentFilters = new IntentFilter[1];
        nfcIntentFilters[0] = new IntentFilter();
        nfcIntentFilters[0].addAction(NfcAdapter.ACTION_TAG_DISCOVERED);

        nfcAdpt.enableForegroundDispatch(getActivity(), nfcIntent, null, null);
    }

    @Override
    public void onResume() {
        super.onResume();

        //nfcAdpt.enableReaderMode(mainActivity, this, NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null);

        //dispatch();
    }

    @Override
    public void onPause() {
        super.onPause();
        //nfcAdpt.disableReaderMode(mainActivity);
    }

}
