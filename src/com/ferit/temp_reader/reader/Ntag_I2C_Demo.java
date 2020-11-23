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
import java.util.concurrent.TimeUnit;


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
import com.ferit.temp_reader.fragments.TemperatureSeriesFragment;
import com.ferit.temp_reader.listeners.WriteEEPROMListener;
import com.ferit.temp_reader.types.Temperature;

import org.json.JSONException;
import org.json.simple.parser.ParseException;


/**
 * Class for the different Demos.
 *
 * @author NXP67729, Moritz Pflügner
 *
 */

public class Ntag_I2C_Demo implements WriteEEPROMListener {

	private I2C_Enabled_Commands reader;
	private Activity main;
	private Tag tag;
	private final String tagHash =  "17mwGzT5x67eUpxP+MBThJ/fIfN9lZkxpD7gwZQiCbc=";
	private static final int DELAY_TIME = 100;
	TemperatureReadTask tTask;

	/**
	 * Constructor.
	 * @param tag Tag with which the Demos should be performed
	 * @param main MainActivity
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
	public void temp(boolean seriesMeasurement, int period) throws IOException, FormatException {
		// Reset UI
		ListFragment.setTemperatureC(0);
		ListFragment.setTemperatureF(0);

		// The demo is executed in a separate thread to let the GUI run
		tTask = new TemperatureReadTask(seriesMeasurement, period);
		tTask.execute();
	}

	@Override
	public void onWriteEEPROM(int bytes) {

	}

	private class TemperatureReadTask extends AsyncTask<Void, Byte[], Void> {

		private Boolean exit = false;
		private final byte noTransfer = 0;
		private boolean seriesMeasurement;
		private int period;

		public TemperatureReadTask(boolean seriesMeasurement, int period){
			this.seriesMeasurement = seriesMeasurement;
			this.period = period;
		}

		@Override
		protected void onCancelled() {
			if(seriesMeasurement){
				TemperatureSeriesFragment.status.setText("Tag lost or measurement finished.");
				TemperatureSeriesFragment.saveButton.setClickable(true);
			}
		}

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
				if(this.seriesMeasurement){
					while(true){
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
						Thread.sleep(period);
					}
				}
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

		@RequiresApi(api = Build.VERSION_CODES.KITKAT)
		@Override
		protected void onProgressUpdate(Byte[]... bytes) {
			if (bytes[0][0] == noTransfer) {
			}else {
				if(seriesMeasurement){
					TemperatureSeriesFragment.status.setText("measuring...");
				}
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
					Date date = new Date();
					DateFormat dateFormatForView = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss");
					DateFormat dateFormatForEEPROM = new SimpleDateFormat("ddMMyyyyHHmmss");
					String dateView = dateFormatForView.format(date);
					String dateTag = dateFormatForEEPROM.format(date);
					if(!seriesMeasurement){
						try {
							increaseCountRecord();
							writeNewIdRecord(dateTag);
						} catch (IOException e) {
							e.printStackTrace();
						} catch (CommandNotSupportedException e) {
							e.printStackTrace();
						} catch (FormatException e) {
							e.printStackTrace();
						}
					}
					// Set the values on the screen
					String tempValue = calcTempCelsius(temp);
					String timestamp = dateView;
					try {
						if(!this.seriesMeasurement){
							ListFragment.addTempToList(new Temperature(timestamp, tempValue, false));
						}
						else{
							TemperatureSeriesFragment.addTempToList(new Temperature(timestamp, tempValue, true));
						}
					} catch (JSONException e) {
						e.printStackTrace();
					} catch (ParseException e) {
						e.printStackTrace();
					}
					if(!this.seriesMeasurement){
						GraphFragment.addTempToGraph(tempValue);
					}
				} else {
					if(seriesMeasurement){
						TemperatureSeriesFragment.status.setText("Tag lost, try again!");
					}
					else{
						ListFragment.setTemperatureC(0);
						ListFragment.setTempCallback("no temperature measured");
					}
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
				break;
			}
		}
		NdefMessage updatedMessage = new NdefMessage(records);
		reader.writeNDEF(updatedMessage, this);
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

	public void resetTemp() {
		ResetTempTask resetTempTask = new ResetTempTask(this);
		resetTempTask.execute();
	}

	public class ResetTempTask extends AsyncTask<Void, String, NdefMessage> {
		private WriteEEPROMListener listener;
		private NdefMessage updatedMessage;

		public ResetTempTask(Ntag_I2C_Demo ntag_i2C_demo) {
			this.listener = ntag_i2C_demo;
		}

		@Override
		protected NdefMessage doInBackground(Void... voids) {
			try {
				updatedMessage = createDefaultNdefMessage();
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
	 * Creates the default NDEF Message
	 * @return NDEF Message
	 * @throws UnsupportedEncodingException
	 */
	private NdefMessage createDefaultNdefMessage()
			throws IOException {
		String passId = "ps";
		String countId = "ct";
		byte[] textBytes = tagHash.getBytes();
		int textLength = textBytes.length;
		byte[] payload = new byte[1 + textLength];
		System.arraycopy(textBytes, 0, payload, 1, textLength);
		NdefRecord pass = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
				NdefRecord.RTD_TEXT, passId.getBytes(), payload);
		NdefRecord aar = NdefRecord.createApplicationRecord("com.ferit.temp_reader");
		NdefRecord countRec = getNdefTextRecord("0",countId);
		NdefRecord[] records = { pass,aar,countRec };
		NdefMessage message = new NdefMessage(records);
		return message;
	}

	/**
	 * Creates a NDEF Text-Record with id and payload.
	 * @param payloadString The string of the NDEF Text message.
	 * @param id The id of the record.
	 * @return NDEF Message
	 */
	private NdefRecord getNdefTextRecord(String payloadString, String id) {
		byte[] payloadBytes = payloadString.getBytes();
		int textLength = payloadBytes.length;
		byte[] payload = new byte[1 + textLength];
		System.arraycopy(payloadBytes, 0, payload, 1, textLength);
		NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
				NdefRecord.RTD_TEXT, id.getBytes(), payload);
		return record;
	}
}
