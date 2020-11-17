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
package com.ferit.temp_reader.reader;

import java.io.IOException;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.ferit.temp_reader.activities.GetPassActivity;
import com.ferit.temp_reader.activities.MainActivity;
import com.ferit.temp_reader.exceptions.CommandNotSupportedException;
import com.ferit.temp_reader.fragments.GraphFragment;
import com.ferit.temp_reader.fragments.ListFragment;
import com.ferit.temp_reader.listeners.WriteEEPROMListener;


/**
 * Class for the different Demos.
 *
 * @author NXP67729
 *
 */

public class Ntag_I2C_Demo implements WriteEEPROMListener {

	private I2C_Enabled_Commands reader;
	private Activity main;
	private Tag tag;

	/**
	 *
	 * Taskreferences.
	 *
	 */
	private WriteEmptyNdefTask emptyNdeftask;
	private WriteDefaultNdefTask defaultNdeftask;
	private TemperatureReadTask tTask;
	private ResetTempTask rtask;
	private GetPassIdTask passIdTask;

	/**
	 *
	 * DEFINES.
	 *
	 */
	private static final int LAST_FOUR_BYTES 	= 4;
	private static final int DELAY_TIME 		= 100;
	private static final int TRAILS 			= 300;
	private static final int DATA_SEND_BYTE 	= 12;
	private static final int VERSION_BYTE 		= 63;
	private static final int GET_VERSION_NR 	= 12;
	private static final int GET_FW_NR 			= 28;
	private static final int THREE_BYTES 		= 3;
	private static final int PAGE_SIZE 			= 4096;

	/**
	 * Constructor.
	 *
	 * @param tag
	 *            Tag with which the Demos should be performed
	 * @param main
	 *            MainActivity
	 */
	public Ntag_I2C_Demo(Tag tag, final Activity main) {
		try {
			if (tag == null) {
				this.main = null;
				this.tag = null;
				return;
			}
			this.main = main;
			this.tag = tag;

			reader = I2C_Enabled_Commands.get(tag);

			if (reader == null) {
				String message = "The Tag could not be identified or this NFC device does not "
						+ "support the NFC Forum commands needed to access this tag";
				String title = "Communication failed";
				showAlert(message, title);
			} else {
				reader.connect();
			}

			Ntag_Get_Version.Prod prod = reader.getProduct();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void showAlert(final String message, final String title) {
		main.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				new AlertDialog.Builder(main)
						.setMessage(message)
						.setTitle(title)
						.setPositiveButton("OK",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {

									}
								}).show();
			}
		});

	}

	/**
	 * Checks if the tag is still connected based on the previously detected reader.
	 *
	 * @return Boolean indicating tag connection
	 *
	 */
	public boolean isConnected() {
		return reader.isConnected();
	}

	/**
	 * Checks if the tag is still connected based on the tag.
	 *
	 * @return Boolean indicating tag presence
	 *
	 */
	public static boolean isTagPresent(Tag tag) {
		final Ndef ndef = Ndef.get(tag);
		if (ndef != null && !ndef.getType().equals("android.ndef.unknown")) {
			try {
				ndef.connect();
				final boolean isConnected = ndef.isConnected();
				ndef.close();
				return isConnected;
			} catch (final IOException e) {
				e.printStackTrace();
				return false;
			}
		} else {
			final NfcA nfca = NfcA.get(tag);
			if (nfca != null) {
				try {
					nfca.connect();
					final boolean isConnected = nfca.isConnected();
					nfca.close();

					return isConnected;
				} catch (final IOException e) {
					e.printStackTrace();
					return false;
				}
			} else {
				final NfcB nfcb = NfcB.get(tag);
				if (nfcb != null) {
					try {
						nfcb.connect();
						final boolean isConnected = nfcb.isConnected();
						nfcb.close();
						return isConnected;
					} catch (final IOException e) {
						e.printStackTrace();
						return false;
					}
				} else {
					final NfcF nfcf = NfcF.get(tag);
					if (nfcf != null) {
						try {
							nfcf.connect();
							final boolean isConnected = nfcf.isConnected();
							nfcf.close();
							return isConnected;
						} catch (final IOException e) {
							e.printStackTrace();
							return false;
						}
					} else {
						final NfcV nfcv = NfcV.get(tag);
						if (nfcv != null) {
							try {
								nfcv.connect();
								final boolean isConnected = nfcv.isConnected();
								nfcv.close();
								return isConnected;
							} catch (final IOException e) {
								e.printStackTrace();
								return false;
							}
						} else {
							return false;
						}
					}
				}
			}
		}
	}

	/**
	 *
	 * Finish all tasks.
	 *
	 */
	public void finishAllTasks() {
		WriteEmptyNdefFinish();
	}

	/**
	 * Checks if the demo is ready to be executed.
	 *
	 * @return Boolean indicating demo readiness
	 *
	 */
	public boolean isReady() {
		if (tag != null && reader != null) {
			return true;
		}
		return false;
	}

	/**
	 * Performs the TEMP Demo
	 */
	public void temp() throws IOException, FormatException {
		// Reset UI
		ListFragment.setTemperatureC(0);
		ListFragment.setTemperatureF(0);

		// The demo is executed in a separate thread to let the GUI run
		tTask = new TemperatureReadTask();
		tTask.execute();
	}

	@Override
	public void onWriteEEPROM(int bytes) {

	}

	private class TemperatureReadTask extends AsyncTask<Void, Byte[], Void> {

		private Boolean exit = false;
		private final byte deviceToTag = 1;
		private final byte tagToDevice = 2;
		private final byte noTransfer = 0;
		private final byte invalidTransfer = 4;

		@Override
		protected Void doInBackground(Void... params) {
			byte[] dataTx = new byte[reader.getSRAMSize()];
			byte[] dataRx = new byte[reader.getSRAMSize()];
			Byte[][] result;
			byte[] Led;

			// We have to make sure that the Pass-Through mode is activated
			long RegTimeOutStart = System.currentTimeMillis();
			boolean RTest = false;
			try {
				do {
					if (reader.checkPTwritePossible()) {
						break;
					}
					long RegTimeOut = System.currentTimeMillis();
					RegTimeOut = RegTimeOut - RegTimeOutStart;
					RTest = (RegTimeOut < 5000);
				} while (RTest);

				// Get the color to be transmitted
				Led = ListFragment.getOption().getBytes();

				// Write the color into the block to be transmitted to the
				// NTAG board
				dataTx[reader.getSRAMSize() - 4] = Led[0];
				dataTx[reader.getSRAMSize() - 3] = Led[1];
				// enable temperature and ndef
				dataTx[reader.getSRAMSize() - 9] = 'E';
				dataTx[reader.getSRAMSize() - 11] = 'E';

				double tempC = ListFragment.getTemperatureC();
				double tempF = ListFragment.getTemperatureF();

				if (tempC > 0.0 && tempC < 75.0) {
					DecimalFormat df = new DecimalFormat("00.00");
					byte[] tempB = df.format(tempC).getBytes();

					// The '.' is omitted
					dataTx[reader.getSRAMSize() - 24] = tempB[0];
					dataTx[reader.getSRAMSize() - 23] = tempB[1];
					dataTx[reader.getSRAMSize() - 22] = tempB[3];
					dataTx[reader.getSRAMSize() - 21] = tempB[4];
				}
				if (tempF > 0.0 && tempF < 120.0) {
					DecimalFormat df = new DecimalFormat("000.00");
					byte[] tempB = df.format(tempF).getBytes();

					// The '.' is omitted
					dataTx[reader.getSRAMSize() - 19] = tempB[0];
					dataTx[reader.getSRAMSize() - 18] = tempB[1];
					dataTx[reader.getSRAMSize() - 17] = tempB[2];
					dataTx[reader.getSRAMSize() - 16] = tempB[4];
					dataTx[reader.getSRAMSize() - 15] = tempB[5];
				}

				// wait to prevent that a RF communication is
				// at the same time as µC I2C
				Thread.sleep(10);
				reader.waitforI2Cread(DELAY_TIME);
				reader.writeSRAMBlock(dataTx, null);

				// wait to prevent that a RF communication is
				// at the same time as µC I2C
				Thread.sleep(10);
				reader.waitforI2Cwrite(100);
				dataRx = reader.readSRAMBlock();

				if (exit) {
					// switch off the LED on the µC before terminating
					dataTx[reader.getSRAMSize() - 3] = '0';

					// wait to prevent that a RF communication is
					// at the same time as µC I2C
					Thread.sleep(10);
					reader.waitforI2Cread(100);

					reader.writeSRAMBlock(dataTx, null);

					// wait to prevent that a RF communication is
					// at the same time as µC I2C
					Thread.sleep(10);
					reader.waitforI2Cwrite(100);

					dataRx = reader.readSRAMBlock();

					cancel(true);
					return null;
				}

				// Convert byte[] to Byte[]
				Byte[] bytes = new Byte[dataRx.length];
				for (int i = 0; i < dataRx.length; i++) {
					bytes[i] = Byte.valueOf(dataRx[i]);
				}
				result = new Byte[2][];
				result[0] = new Byte[1];
				result[0][0] = Byte.valueOf((byte) 4);
				result[1] = bytes;

				// Write the result to the UI thread
				publishProgress(result);

			} catch (FormatException e) {
				cancel(true);
				e.printStackTrace();
			} catch (IOException e) {
				cancel(true);
				e.printStackTrace();
			} catch (CommandNotSupportedException e) {
				cancel(true);
				e.printStackTrace();
			} catch (Exception e) {
				cancel(true);
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Byte[]... bytes) {
			if (bytes[0][0] == noTransfer) {

			}else {

				int temp = 0;

				// Adding first "Byte"
				temp = ((bytes[1][reader.getSRAMSize() - 5] >> 5) & 0x00000007);
				System.out.println(temp);
				// Adding second Byte
				temp |= ((bytes[1][reader.getSRAMSize() - 6] << 3) & 0x000007F8);

				// Voltage
				int voltage = 0;
				voltage = ((bytes[1][reader.getSRAMSize() - 7] << 8) & 0xff00)
						+ (bytes[1][reader.getSRAMSize() - 8] & 0x00ff);

				// if Temp is 0 no Temp sensor is on the µC
				if (temp != 0) {
					DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss");
					String date = dateFormat.format(new Date());
					try {
						increaseCountRecord();
						writeNewIdRecord(date);
						//createNdefMessage("17mwGzT5x67eUpxP+MBThJ/fIfN9lZkxpD7gwZQiCbc=");
					} catch (IOException e) {
						e.printStackTrace();
					} catch (CommandNotSupportedException e) {
						e.printStackTrace();
					} catch (FormatException e) {
						e.printStackTrace();
					}
					// Set the values on the screen
					ListFragment.addTempToList(calcTempCelsius(temp) + "°C" + " | " + date);
					GraphFragment.addTempToGraph(calcTempCelsius(temp));
				} else {
					ListFragment.setTemperatureC(0);
					ListFragment.setTempCallback("no temperature measured");
				}
			}
		}
	}

	private void writeNewIdRecord(String date) throws CommandNotSupportedException, FormatException, IOException {
		String mac = MainActivity.getMacAddr();
		String payload = mac + date;
		NdefRecord newRecord = getNdefTextRecord(payload, "id");
		NdefMessage message = reader.readNDEF();
		NdefRecord[] records = message.getRecords();
		NdefRecord[] newRecords = new NdefRecord[records.length + 1];
		System.arraycopy(records,0,newRecords,0,records.length);
		newRecords[records.length] = newRecord;
		NdefMessage updatedMessage = new NdefMessage(newRecords);
		reader.writeNDEF(updatedMessage, this);
	}



	private void increaseCountRecord() throws CommandNotSupportedException, FormatException, IOException {
		NdefMessage message = reader.readNDEF();
		NdefRecord[] records = message.getRecords();
		for(int i = 0; i < records.length; i++){
			if(new String(records[i].getId()).equals("ct")){
				char[] payload = new String(records[i].getPayload()).toCharArray();
				char[] countCharArray = new char[payload.length - 1];
				System.arraycopy(payload,1,countCharArray,0,payload.length - 1);
				String countString = new String(countCharArray);
				Integer payloadAsNumber = Integer.parseInt(countString);
				payloadAsNumber = payloadAsNumber + 1;
				NdefRecord countRecUpdated = getNdefTextRecord(String.valueOf(payloadAsNumber), "ct");
				records[i] = countRecUpdated;
			}
		}
		NdefMessage updatedMessage = new NdefMessage(records);
		reader.writeNDEF(updatedMessage, this);
	}

	/**
	 * Write empty ndef task.
	 */
	private class WriteEmptyNdefTask extends AsyncTask<Void, Void, Void> {

		@SuppressWarnings("unused")
		private Boolean exit = false;

		@Override
		protected Void doInBackground(Void... params) {
			try {
				reader.writeEmptyNdef();
			} catch (Exception e) {
				e.printStackTrace();
				cancel(true);
				return null;
			}
			return null;
		}
	}

	/**
	 * Write empty ndef finish.
	 */
	public void WriteEmptyNdefFinish() {
		if (emptyNdeftask != null && !emptyNdeftask.isCancelled()) {
			emptyNdeftask.exit = true;
			try {
				emptyNdeftask.get();
			} catch (Exception e) {
				e.printStackTrace();
			}
			emptyNdeftask = null;
		}
	}

	/**
	 * Write default ndef task.
	 */
	private class WriteDefaultNdefTask extends AsyncTask<Void, Void, Void> {

		@SuppressWarnings("unused")
		private Boolean exit = false;

		@Override
		protected Void doInBackground(Void... params) {
			try {
				reader.writeDefaultNdef();
			} catch (Exception e) {
				e.printStackTrace();
				cancel(true);
				return null;
			}
			return null;
		}
	}

	@SuppressWarnings("unused")
	private NdefMessage creatNdefDefaultMessage()
			throws UnsupportedEncodingException {
		NdefRecord uri_record = NdefRecord
				.createUri("http://www.nxp.com/products/identification_and_security/"
						+ "smart_label_and_tag_ics/ntag/series/NT3H1101_NT3H1201.html");
		String text = "NTAG I2C Demoboard LPC812";
		String lang = "en";
		byte[] textBytes = text.getBytes();
		byte[] langBytes = lang.getBytes("US-ASCII");
		int langLength = langBytes.length;
		int textLength = textBytes.length;
		byte[] payload = new byte[1 + langLength + textLength];
		payload[0] = (byte) langLength;
		System.arraycopy(langBytes, 0, payload, 1, langLength);
		System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);
		NdefRecord textRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
				NdefRecord.RTD_TEXT, new byte[0], payload);
		NdefRecord[] spRecords = { uri_record, textRecord };
		NdefMessage spMessage = new NdefMessage(spRecords);
		NdefRecord sp_record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
				NdefRecord.RTD_SMART_POSTER, new byte[0],
				spMessage.toByteArray());
		NdefRecord aap_record = NdefRecord.createApplicationRecord(main
				.getPackageName());
		NdefRecord[] records = { sp_record, aap_record };
		NdefMessage message = new NdefMessage(records);
		return message;

	}
	/**
	 * Calculates the Temperature in Celsius.
	 * 
	 * @param temp
	 *            Temperature
	 * @return String of Temperature in Dez
	 */
	private String calcTempCelsius(int temp) {
		double tempDouble = 0;
		String tempString = "";
		// If the 11 Bit is 1 it is negative
		if ((temp & (1 << 11)) == (1 << 11)) {
			// Mask out the 11 Bit
			temp &= ~(1 << 11);
			tempString += "-";
		}
		tempDouble = 0.125 * temp;
		// Update the value on the Led fragment
		DecimalFormat df = new DecimalFormat("#.00");
		tempString = df.format(tempDouble);
		return tempString;
	}

	public void getPassId(){
		GetPassIdTask passIdTask = new GetPassIdTask();
		passIdTask.execute();
	}

	public class GetPassIdTask extends AsyncTask<Void, String, NdefMessage> {

		public GetPassIdTask() { }

		@Override
		protected NdefMessage doInBackground(Void... voids) {
			try {
				NdefMessage message = reader.readNDEF();
				NdefRecord[] records = message.getRecords();
				for(int i = 0; i < records.length; i++){
					if (new String(records[i].getId()).equals("ps")){
						char[] payloadArray = new String(records[i].getPayload()).toCharArray();
						char[] hashArray = new char[payloadArray.length - 1];
						System.arraycopy(payloadArray,1,hashArray,0,hashArray.length);
						String hash = new String(hashArray);
						GetPassActivity.authenticate(hash);
					}
				}
				return message;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (FormatException e) {
				e.printStackTrace();
			} catch (CommandNotSupportedException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	public void resetTemp() throws UnsupportedEncodingException,
			IOException, FormatException {
		ResetTempTask resetTempTask = new ResetTempTask(this);
		resetTempTask.execute();
	}

	public class ResetTempTask extends AsyncTask<Void, String, NdefMessage> {
		private WriteEEPROMListener listener;
		private NdefMessage updatedMessage;

		public ResetTempTask(Ntag_I2C_Demo ntag_i2C_demo) {
			this.listener = ntag_i2C_demo;
		}

		@RequiresApi(api = Build.VERSION_CODES.O)
		@Override
		protected void onPreExecute() {
			try {
				NdefMessage currentMessage = reader.readNDEF();
				NdefRecord[] records = currentMessage.getRecords();
				NdefRecord[] resettedRecords = new NdefRecord[3];
				System.arraycopy(records,0,resettedRecords,0,2);
				NdefRecord updatedRecord = getNdefTextRecord("0", "ct");
				resettedRecords[2] = updatedRecord;
				updatedMessage = new NdefMessage(resettedRecords);
			} catch (FormatException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (CommandNotSupportedException e) {
				e.printStackTrace();
			}
		}

		@Override
		protected NdefMessage doInBackground(Void... voids) {
			try {
				reader.writeNDEF(updatedMessage, listener);
				return updatedMessage;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (FormatException e) {
				e.printStackTrace();
			} catch (CommandNotSupportedException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	/**
	 * Creates a NDEF Message
	 *
	 * @param text
	 *            Text to write
	 * @return NDEF Message
	 * @throws UnsupportedEncodingException
	 */
	private NdefMessage createNdefMessage(String text)
			throws IOException, CommandNotSupportedException, FormatException {
		String passId = "ps";
		String countId = "ct";
		byte[] textBytes = text.getBytes();
		int textLength = textBytes.length;
		byte[] payload = new byte[1 + textLength];
		System.arraycopy(textBytes, 0, payload, 1, textLength);
		NdefRecord pass = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
				NdefRecord.RTD_TEXT, passId.getBytes(), payload);
		NdefRecord aar = NdefRecord.createApplicationRecord("com.ferit.temp_reader");
		NdefRecord countRec = getNdefTextRecord("0",countId);
		NdefRecord[] records = { pass,aar,countRec };
		NdefMessage message = new NdefMessage(records);
		//reader.writeNDEF(message,this);
		return message;
	}

	private NdefRecord getNdefTextRecord(String payloadString, String id) throws UnsupportedEncodingException {
		byte[] payloadBytes = payloadString.getBytes();
		int textLength = payloadBytes.length;
		byte[] payload = new byte[1 + textLength];
		System.arraycopy(payloadBytes, 0, payload, 1, textLength);
		NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
				NdefRecord.RTD_TEXT, id.getBytes(), payload);
		return record;
	}
}
