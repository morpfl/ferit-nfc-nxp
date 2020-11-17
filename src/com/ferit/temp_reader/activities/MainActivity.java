/*
****************************************************************************
* Copyright(c) 2014 NXP Semiconductors                                     *
* All rights are reserved.                                                 *
*                                                                          *
* Software that is described herein is for illustrative purposes only.     *
* This software is supplied "AS IS" without any warranties of any kind,    *
* and NXP Semiconductors disclaims any and all warranties, express or      *
* implied, including all implied warranties of merchantability,            *
* fitness for a particular purpose and non-infringement of intellectual    *
* property rights.  NXP Semiconductors assumes no responsibility           *
* or liability for the use of the software, conveys no license or          *
* rights under any patent, copyright, mask work right, or any other        *
* intellectual property rights in or to any products. NXP Semiconductors   *
* reserves the right to make changes in the software without notification. *
* NXP Semiconductors also makes no representation or warranty that such    *
* application will be suitable for the specified use without further       *
* testing or modification.                                                 *
*                                                                          *
* Permission to use, copy, modify, and distribute this software and its    *
* documentation is hereby granted, under NXP Semiconductors' relevant      *
* copyrights in the software, without fee, provided that it is used in     *
* conjunction with NXP Semiconductor products(UCODE I2C, NTAG I2C).        *
* This  copyright, permission, and disclaimer notice must appear in all    *
* copies of this code.                                                     *
****************************************************************************
*/
package com.ferit.temp_reader.activities;

import java.net.NetworkInterface;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.DialogFragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;

import com.ferit.temp_reader.adapters.TabsAdapter;
import com.ferit.temp_reader.crypto.SHA256Encryptor;
import com.ferit.temp_reader.fragments.AuthDialogFragment;
import com.ferit.temp_reader.fragments.GraphFragment;
import com.ferit.temp_reader.fragments.ListFragment;
import com.ferit.temp_reader.reader.Ntag_I2C_Demo;
import com.ferit.temp_reader.R;
import com.ferit.temp_reader.util.AuthStatus;

public class MainActivity extends FragmentActivity implements AuthDialogFragment.NoticeDialogListener {
	public final static String EXTRA_MESSAGE = "com.nxp.nfc_demo.MESSAGE";
	public final static int AUTH_REQUEST = 0;
	public static Ntag_I2C_Demo demo;
	private TabHost mTabHost;
	private ViewPager mViewPager;
	private TabsAdapter mTabsAdapter;
	private PendingIntent mPendingIntent;
	private NfcAdapter mAdapter;
	private static MenuItem authItem;
	private static AuthStatus authStatus;
	public static String PACKAGE_NAME;

	private static Intent mIntent;

	public static Intent getmIntent() {
		return mIntent;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Application package name to be used by the AAR record
		PACKAGE_NAME = getApplicationContext().getPackageName();
		String languageToLoad = "en";
		Locale locale = new Locale(languageToLoad);
		Locale.setDefault(locale);
		Configuration config = new Configuration();
		config.locale = locale;
		getBaseContext().getResources().updateConfiguration(config,
				getBaseContext().getResources().getDisplayMetrics());
		setContentView(R.layout.activity_main);
		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		authStatus = AuthStatus.UNAUTHENTICATED;
		mTabHost.setup();
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);
		mTabsAdapter.addTab(
				mTabHost.newTabSpec("temperatureList").setIndicator("LIST"), ListFragment.class, null);
		mTabsAdapter.addTab(
				mTabHost.newTabSpec("tempList2").setIndicator("GRAPH"), GraphFragment.class, null);

		if (savedInstanceState != null) {
			mTabHost.setCurrentTabByTag("temperatureList");
		}

		// Notifier to be used for the tab changing
		mTabHost.setOnTabChangedListener(new OnTabChangeListener() {
			@Override
			public void onTabChanged(String tabId) {
				mTabsAdapter.onTabChanged(tabId);
			}
		});

		// Initialize the demo in order to handle tab change events
		demo = new Ntag_I2C_Demo(null, this);
		mAdapter = NfcAdapter.getDefaultAdapter(this);
		setNfcForeground();
		checkNFC();
	}

	@SuppressLint("InlinedApi")
	private void checkNFC() {
		if (mAdapter != null) {
			if (!mAdapter.isEnabled()) {
				new AlertDialog.Builder(this)
						.setTitle("NFC not enabled")
						.setMessage("Go to Settings?")
						.setPositiveButton("Yes",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										 if (android.os.Build.VERSION.SDK_INT >= 16) {
											 startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS));
										 } else {
											 startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
										 }
									}
								})
						.setNegativeButton("No",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										System.exit(0);
									}
								}).show();
			}
		} else {
			new AlertDialog.Builder(this)
					.setTitle("No NFC available. App is going to be closed.")
					.setNeutralButton("Ok",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									System.exit(0);
								}
							}).show();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mAdapter != null) {
			mAdapter.disableForegroundDispatch(this);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mAdapter != null) {
			mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
		}
		System.out.println("back at main: " + authStatus);
		if(this.authStatus.equals(AuthStatus.AUTHENTICATED)){
			authItem.setIcon(R.drawable.unlock);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mIntent = null;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    // Check which request we're responding to
		// Make sure the request was successful
		if (requestCode == AUTH_REQUEST
			&& resultCode == RESULT_OK
			&& demo != null
			&& demo.isReady()) {
			String currTab = mTabHost.getCurrentTabTag();
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
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);

		// Get the reference to the menu item
		authItem = menu.findItem(R.id.action_auth);
		return true;
	}

	public static String getMacAddr() {
		try {
			List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface nif : all) {
				if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

				byte[] macBytes = nif.getHardwareAddress();
				if (macBytes == null) {
					return "";
				}

				StringBuilder res1 = new StringBuilder();
				for (byte b : macBytes) {
					res1.append(Integer.toHexString(b & 0xFF) + ":");
				}

				if (res1.length() > 0) {
					res1.deleteCharAt(res1.length() - 1);
				}
				return res1.toString();
			}
		} catch (Exception ex) {
			//handle exception
		}
		return "";
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		Intent intent = null;
		switch (item.getItemId()) {
			case R.id.action_new:
				if(authStatus.equals(AuthStatus.UNAUTHENTICATED)){
					DialogFragment newFragment = new AuthDialogFragment();
					newFragment.show(getSupportFragmentManager(), "auth");
					break;
				}
				intent = new Intent(this, ReadTempActivity.class);
				break;
			case R.id.action_reset:
				intent = new Intent(this, ResetTempActivity.class);
				break;
			case R.id.action_auth:
				DialogFragment newFragment = new AuthDialogFragment();
				newFragment.show(getSupportFragmentManager(), "auth");
				break;

		}

		if(MainActivity.getmIntent() != null)
			intent.putExtras(MainActivity.getmIntent());

		if(intent != null){
			startActivity(intent);
		}
		return true;
	}

	public void setNfcForeground() {
		// Create a generic PendingIntent that will be delivered to this
		// activity. The NFC stack will fill
		// in the intent with the details of the discovered tag before
		// delivering it to this activity.
		mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(
				getApplicationContext(), getClass())
				.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
	}

	@RequiresApi(api = Build.VERSION_CODES.O)
	@Override
	public void onDialogPositiveClick(String password) throws NoSuchAlgorithmException {
		SHA256Encryptor encryptor = new SHA256Encryptor();
		String userPassword = encryptor.encryptMessage(password);
		Intent intent = new Intent(this, GetPassActivity.class);
		intent.putExtra("userpw", userPassword);
		if(MainActivity.getmIntent() != null)
			intent.putExtras(MainActivity.getmIntent());

		if(intent != null){
			startActivity(intent);
		}
	}

	public static void setAuthStatus(AuthStatus newStatus){
		authStatus = newStatus;
	}
}
