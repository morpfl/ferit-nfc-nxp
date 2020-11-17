package com.ferit.temp_reader.activities;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Vibrator;

import com.ferit.temp_reader.reader.I2C_Enabled_Commands;
import com.ferit.temp_reader.reader.Ntag_I2C_Demo;
import com.ferit.temp_reader.R;

import java.io.IOException;

public class ReadTempActivity extends Activity {

    private static Context mContext;
    private Ntag_I2C_Demo demo;
    private NfcAdapter mAdapter;
    private I2C_Enabled_Commands reader;
    private PendingIntent pendingIntent;
    private static Intent mIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_readtemp);

        // Capture intent to check whether the operation should be automatically launch or not
        Tag tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
        // && demo.isTagPresent(tag)
        if (tag != null) {
            try {
                startDemo();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (FormatException e) {
                e.printStackTrace();
            }
        }

        // Add Foreground dispatcher
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        return; // end onCreate
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAdapter != null) {
            mAdapter.disableForegroundDispatch(this);
        }
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter != null) {
            mAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }

    @Override
    protected void onNewIntent(Intent nfc_intent) {
        super.onNewIntent(nfc_intent);
        // Set the pattern for vibration
        long pattern[] = { 0, 100 };

        // Vibrate on new Intent
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(pattern, -1);
        try {
            doProcess(nfc_intent);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void doProcess(Intent nfc_intent) throws InterruptedException, FormatException, IOException {
        mIntent = nfc_intent;
        Tag tag = nfc_intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        demo = new Ntag_I2C_Demo(tag, this);
        if (demo != null) {
            startDemo();
        }
    }

    private void startDemo() throws IOException, InterruptedException, FormatException {
        demo.temp();
    }
}
