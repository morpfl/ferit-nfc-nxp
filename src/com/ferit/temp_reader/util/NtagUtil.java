package com.ferit.temp_reader.util;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NtagUtil {

    public static String formatMetadata(String mac, String timestamp) throws java.text.ParseException {
        DateFormat dateFormatForEEPROM = new SimpleDateFormat("ddMMyyHHmmss");
        DateFormat dateFormatForView = new SimpleDateFormat("dd.MM.yyyy - HH:mm:ss");
        Date date = dateFormatForEEPROM.parse(timestamp);
        String timestampString = dateFormatForView.format(date);
        return mac.concat(" | " + timestampString);
    }

    /**
     * Calculates the Temperature in Celsius.
     *
     * @param temp
     *            Temperature
     * @return String of Temperature in Dez
     */
    public static String calcTempCelsius(int temp) {
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
    /**
     * Creates the default NDEF Message
     * @return NDEF Message
     */
    public static NdefMessage createDefaultNdefMessage(String tagHash)
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
    public static NdefRecord getNdefTextRecord(String payloadString, String id) {
        byte[] payloadBytes = payloadString.getBytes();
        int textLength = payloadBytes.length;
        byte[] payload = new byte[1 + textLength];
        System.arraycopy(payloadBytes, 0, payload, 1, textLength);
        NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_TEXT, id.getBytes(), payload);
        return record;
    }


    public static byte[] getMetadataRecordPayloadBytes(String macAddress, String timestamp){
        long timestampAsNumber = Long.parseLong(timestamp);
        int bytesMac = 6;
        int bytesTs = 5;
        String[] macAddressParts = macAddress.split(":");
        byte[] payloadBytes = new byte[bytesMac + bytesTs];
        for(int i = 0; i < bytesMac; i++){
            Integer hex = Integer.parseInt(macAddressParts[i],16);
            payloadBytes[i] = hex.byteValue();
        }
        char[] timestampHexStringArray = Long.toHexString(timestampAsNumber).toCharArray();
        byte[] timestampBytes = new byte[bytesTs];
        int hexStringCounter = 0;
        for(int i = 0; i < timestampBytes.length; i++){
            String byteString = String.valueOf(timestampHexStringArray[hexStringCounter]) + String.valueOf(timestampHexStringArray[hexStringCounter+1]);
            Integer hex = Integer.parseInt(byteString,16);
            hexStringCounter=hexStringCounter+2;
            timestampBytes[i] = hex.byteValue();
        }
        System.arraycopy(timestampBytes, 0, payloadBytes,bytesMac,bytesTs);
        return payloadBytes;
    }

    public static NdefRecord appendNewMetadataPayload(byte[] oldPayload, byte[] newMeasurementData){
        byte[] idBytes = new byte[1];
        idBytes[0] = new Integer(1).byteValue();
        byte[] newPayload = new byte[oldPayload.length + newMeasurementData.length];
        System.arraycopy(oldPayload,0,newPayload,0,oldPayload.length);
        System.arraycopy(newMeasurementData,0,newPayload,oldPayload.length,newMeasurementData.length);
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, idBytes,newPayload);
    }

    public static NdefRecord createNewMetadataRecord(byte[] newMeasurementData){
        byte[] idBytes = new byte[1];
        idBytes[0] = new Integer(1).byteValue();
        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT,idBytes,newMeasurementData);
    }
}
