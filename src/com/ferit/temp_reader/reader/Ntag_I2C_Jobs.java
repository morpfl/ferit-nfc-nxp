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

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.icu.text.AlphabeticIndex;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.ferit.temp_reader.activities.GetPassActivity;
import com.ferit.temp_reader.activities.MainActivity;
import com.ferit.temp_reader.exceptions.CommandNotSupportedException;
import com.ferit.temp_reader.fragments.GraphFragment;
import com.ferit.temp_reader.fragments.ListFragment;
import com.ferit.temp_reader.fragments.MetadataFragment;
import com.ferit.temp_reader.fragments.TemperatureSeriesFragment;
import com.ferit.temp_reader.listeners.WriteEEPROMListener;
import com.ferit.temp_reader.types.Temperature;
import com.ferit.temp_reader.util.NtagUtil;
import com.ferit.temp_reader.util.RecordId;

import org.json.JSONException;
import org.json.simple.parser.ParseException;


/**
 * Class for the different Demos.
 *
 * @author NXP67729, Moritz Pflügner
 *
 */

public class Ntag_I2C_Jobs implements WriteEEPROMListener {

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
	public Ntag_I2C_Jobs(Tag tag, final Activity main) {
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

	@Override
	public void onWriteEEPROM(int bytes) { }

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
		return tag != null && reader != null;
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

	private class TemperatureReadTask extends AsyncTask<Void, Byte[], Void> {

		private final Boolean exit = false;
		private final byte noTransfer = 0;
		private final boolean seriesMeasurement;
		private final int period;

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
			if(!isReady()){
				cancel(true);
			}
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

		@RequiresApi(api = Build.VERSION_CODES.N)
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
					DateFormat dateFormatForEEPROM = new SimpleDateFormat("ddMMyyHHmmss");
					String dateView = dateFormatForView.format(date);
					String dateTag = dateFormatForEEPROM.format(date);
					if(!seriesMeasurement){
						try {
							increaseCountRecord();
							//writeNewIdRecord(dateTag);
							appendIdDataToRecord(dateTag);
						} catch (IOException e) {
							e.printStackTrace();
						} catch (CommandNotSupportedException e) {
							e.printStackTrace();
						} catch (FormatException e) {
							e.printStackTrace();
						}
					}
					// Set the values on the screen
					String tempValue = NtagUtil.calcTempCelsius(temp);
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

	public void readMetadata(String mac){
		GetMetadataTask metadataTask = new GetMetadataTask(mac);
		metadataTask.execute();
	}

	public class GetMetadataTask extends AsyncTask<Void, String, NdefMessage> {
		private final List<String> metadataStrings = new LinkedList<String>();
		private boolean noMetadataAvailable = false;
		// index declarations, see memory design chapter in documentation
		private final int METADATA_RECORD_SIZE = 11;
		private final int MAC_ADDRESS_LAST_INDEX = 5;
		private final int TIMESTAMP_LAST_INDEX = 10;
		private String mac;

		public GetMetadataTask(String mac) {
			this.mac = mac;
		}

		@RequiresApi(api = Build.VERSION_CODES.N)
		@Override
		protected NdefMessage doInBackground(Void... voids) {
			try {
				NdefMessage message = reader.readNDEF();
				NdefRecord[] records = message.getRecords();
				List<NdefRecord> recordsAsList = Arrays.asList(records);
				Optional<NdefRecord> metadataRecordOpt = recordsAsList.stream()
						.filter(record -> record.getId().length > 0 && new String(record.getId()).equals(RecordId.MD.toString()))
						.findFirst();
				if(!metadataRecordOpt.isPresent()){
					noMetadataAvailable = true;
					cancel(true);
				}
				NdefRecord metadataRecord = metadataRecordOpt.get();
				byte[] payload = metadataRecord.getPayload();
				String mac = "";
				String timestamp = "";
				for(int currentByteNumber = 0; currentByteNumber < payload.length; currentByteNumber++){
					// read current byte and convert it to a readable hex string
					Byte currentByte = payload[currentByteNumber];
					Integer test = Integer.parseInt(currentByte.toString());
					char[] hexCharAr = Integer.toHexString(test).toCharArray();
					String hexString;
					// some formatting, because sometimes the hex value is interpreted as ffffffxx, where xx is the actual value that is of interest.
					if(hexCharAr.length > 2){
						hexString = String.valueOf(hexCharAr).substring(6);
					}
					else if (hexCharAr.length == 1){
						hexString = String.valueOf(hexCharAr);
						hexString = "0" + hexString;
					}
					else {
						hexString = String.valueOf(hexCharAr);
					}
					// determine the content of the byte, see memory design in documentation
					if((currentByteNumber % METADATA_RECORD_SIZE) == 0) {
						mac = "";
						timestamp = "";
					}
					if((currentByteNumber % METADATA_RECORD_SIZE) < MAC_ADDRESS_LAST_INDEX){
						mac = mac.concat(hexString + ":");
					}
					if((currentByteNumber % METADATA_RECORD_SIZE) == MAC_ADDRESS_LAST_INDEX){
						mac = mac.concat(hexString);
					}
					if((currentByteNumber % METADATA_RECORD_SIZE) > MAC_ADDRESS_LAST_INDEX){
						timestamp = timestamp.concat(hexString);
					}
					if((currentByteNumber % METADATA_RECORD_SIZE) == TIMESTAMP_LAST_INDEX){
						Long timestampLong = Long.parseLong(timestamp,16);
						String formattedForView = NtagUtil.formatMetadata(mac, String.valueOf(timestampLong));
						if(this.mac.equals("")){
							metadataStrings.add(formattedForView);
						}
						else if(mac.equals(this.mac)){
							metadataStrings.add(formattedForView);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (FormatException e) {
				e.printStackTrace();
			} catch (CommandNotSupportedException e) {
				e.printStackTrace();
			} catch (java.text.ParseException e) {
				e.printStackTrace();
			}
			cancel(true);
			return null;
		}

		@Override
		protected void onCancelled() {
			MetadataFragment.setMetadata(metadataStrings);
			if(noMetadataAvailable){
				MetadataFragment.infoText.setText("No metadata stored on the tag.");
			}
		}
	}

	public void getPassId(){
		GetPassIdTask passIdTask = new GetPassIdTask();
		passIdTask.execute();
	}

	public class GetPassIdTask extends AsyncTask<Void, String, NdefMessage> {

		@RequiresApi(api = Build.VERSION_CODES.N)
		@Override
		protected NdefMessage doInBackground(Void... voids) {
			try {
				NdefMessage message = reader.readNDEF();
				NdefRecord[] records = message.getRecords();
				List<NdefRecord> recordsAsList = Arrays.asList(records);
				Optional<NdefRecord> recordOpt = recordsAsList.stream().filter(record -> new String(record.getId()).equals(RecordId.PS.toString())).findFirst();
				if(!recordOpt.isPresent()){
					cancel(true);
				}
				NdefRecord record = recordOpt.get();
				char[] payloadArray = new String(record.getPayload()).toCharArray();
				char[] hashArray = new char[payloadArray.length - 1];
				System.arraycopy(payloadArray,1,hashArray,0,hashArray.length);
				String hash = new String(hashArray);
				GetPassActivity.authenticate(hash);
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
		private final WriteEEPROMListener listener;
		private NdefMessage updatedMessage;

		public ResetTempTask(Ntag_I2C_Jobs ntag_i2C_jobs) {
			this.listener = ntag_i2C_jobs;
		}

		@Override
		protected NdefMessage doInBackground(Void... voids) {
			try {
				updatedMessage = NtagUtil.createDefaultNdefMessage(tagHash);
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

	@RequiresApi(api = Build.VERSION_CODES.N)
	private void appendIdDataToRecord(String timestamp) throws CommandNotSupportedException, FormatException, IOException {
		String mac = MainActivity.getMacAddr();
		NdefMessage message = reader.readNDEF();
		List<NdefRecord> records = Arrays.asList(message.getRecords());
		Optional<NdefRecord> optIdRecord = records.stream().filter(record -> record.getId().length != 0 && new String(record.getId()).equals(RecordId.MD.toString())).findFirst();
		if(optIdRecord.isPresent()){
			NdefRecord idRecord = optIdRecord.get();
			int index = records.indexOf(idRecord);
			byte[] oldPayload = idRecord.getPayload();
			byte[] payloadToAppend = NtagUtil.getMetadataRecordPayloadBytes(mac,timestamp);
			records.set(index, NtagUtil.appendNewMetadataPayload(oldPayload,payloadToAppend));
			NdefMessage newMessage = new NdefMessage((NdefRecord[]) records.toArray());
			reader.writeNDEF(newMessage,this);
		}
		else{
			byte[] payload = NtagUtil.getMetadataRecordPayloadBytes(mac,timestamp);
			NdefRecord newRecord = NtagUtil.createNewRecord(RecordId.MD.toString(), payload);
			NdefRecord[] newRecords = new NdefRecord[message.getRecords().length + 1];
			System.arraycopy(message.getRecords(),0,newRecords,0,message.getRecords().length);
			newRecords[newRecords.length - 1] = newRecord;
			NdefMessage newMessage = new NdefMessage(newRecords);
			reader.writeNDEF(newMessage,this);
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.N)
	private void increaseCountRecord() throws CommandNotSupportedException, FormatException, IOException {
		NdefMessage message = reader.readNDEF();
		NdefRecord[] records = message.getRecords();
		List<NdefRecord> recordsAsList = Arrays.asList(records);
		Optional<NdefRecord> countRecordOpt = recordsAsList.stream()
				.filter(record -> new String(record.getId()).equals(RecordId.CT.toString()))
				.findFirst();
		if(!countRecordOpt.isPresent()){
			return;
		}
		NdefRecord countRecord = countRecordOpt.get();
		String countAsHex = "";
		for(int byteCount = 0; byteCount < 4; byteCount++){
			Byte currentByte = countRecord.getPayload()[byteCount];
			Integer integer = Integer.parseInt(currentByte.toString());
			countAsHex = countAsHex.concat(Integer.toHexString(integer));
		}
		Integer newCountAsInt = Integer.parseInt(countAsHex,16) + 1;
		String newHexString = Integer.toHexString(newCountAsInt);
		String newHexStringWithPadding = String.format("%8s", newHexString).replace(' ', '0');
		byte[] newPayloadForRecord = NtagUtil.convertHexStringToByteArray(newHexStringWithPadding);
		NdefRecord updatedRecord = NtagUtil.createNewRecord(RecordId.CT.toString(), newPayloadForRecord);
		recordsAsList.set(recordsAsList.indexOf(countRecord),updatedRecord);
		NdefMessage updatedMessage = new NdefMessage((NdefRecord[]) recordsAsList.toArray());
		reader.writeNDEF(updatedMessage, this);
	}
}
