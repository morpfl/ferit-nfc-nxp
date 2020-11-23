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
package com.ferit.temp_reader.fragments;


import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.ferit.temp_reader.R;
import com.ferit.temp_reader.types.Temperature;

import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ListFragment extends Fragment {

    private static double temperatureC;
    private static double temperatureF;
    private static TextView tempCallback;
    private static String option;
    private static List<Temperature> measuredTemperatures;
    private static ListView mListView;
    private static ArrayAdapter<String> adapter;
    private static String filesDir;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        option = "L2";
        measuredTemperatures = new LinkedList<Temperature>();
        filesDir = getActivity().getFilesDir().toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.measuredTemperatures = new LinkedList<Temperature>();
        View v = inflater.inflate(R.layout.fragment_list, container, false);
        tempCallback = (TextView) v.findViewById(R.id.tempCallback);
        mListView = (ListView) v.findViewById(R.id.list);
        adapter = new ArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, this.measuredTemperatures);
        mListView.setAdapter(adapter);
        try {
            File dir = new File(filesDir, "temperatures");
            if(!dir.exists()){
                dir.mkdir();
            }
            File file = new File(filesDir + "/temperatures/temperatures.json");
            file.createNewFile();
            JSONArray array = getTempsFromFile();
            for(int i = 0; i < array.size(); i++){
                JSONObject jsonObj = (JSONObject) array.get(i);
                String timestamp = (String) jsonObj.get("timestamp");
                String tempValue = (String) jsonObj.get("tempValue");
                Temperature temperature = new Temperature(timestamp, tempValue, false);
                measuredTemperatures.add(temperature);
            }
            adapter.notifyDataSetChanged();
            if(!measuredTemperatures.isEmpty()){
                tempCallback.setText("measured temperatures: ");
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
        return v;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static void writeFileOnInternalStorage(Temperature newTemperature) throws JSONException, ParseException {
        JSONObject newTempJsonObject = new JSONObject();
        newTempJsonObject.put("timestamp", newTemperature.getTimestamp());
        newTempJsonObject.put("tempValue", newTemperature.getTemperatureValue());
        JSONArray existingTemps = getTempsFromFile();
        if(existingTemps == null){
            existingTemps = new JSONArray();
        }
        existingTemps.add(newTempJsonObject);
        try{
            FileWriter writer = new FileWriter("/data/data/com.ferit.temp_reader/files/temperatures/temperatures.json");
            writer.write(existingTemps.toJSONString());
            writer.flush();
            writer.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static JSONArray getTempsFromFile() throws ParseException {
        File file = new File("/data/data/com.ferit.temp_reader/files/temperatures/temperatures.json");
        if(file.length() == 0){
            return new JSONArray();
        }
        JSONParser parser = new JSONParser();
        try {
            FileReader reader = new FileReader(file);
            JSONArray array = (JSONArray) parser.parse(reader);
            reader.close();
            return array;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JSONArray();
    }

    public static double getTemperatureC() {
        return temperatureC;
    }
    public static double getTemperatureF() {
        return temperatureF;
    }
    public static void setTemperatureC(double t) {
        temperatureC = t;
    }
    public static void setTemperatureF(double t) {
        temperatureF = t;
    }
    public static void setTempCallback(String c){ tempCallback.setText(c); }
    public static String getOption() {
        return option;
    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static void addTempToList(Temperature temp) throws JSONException, ParseException {
        tempCallback.setText("measured temperatures: ");
        writeFileOnInternalStorage(temp);
        measuredTemperatures.add(temp);
        adapter.notifyDataSetChanged();
    }

    public static void resetTemperatures(){
        measuredTemperatures.removeAll(measuredTemperatures);
        File dir = new File(filesDir, "temperatures");
        try {
            File gpxfile = new File(dir, "temperatures.json");
            FileWriter writer = new FileWriter(gpxfile);
            writer.flush();
            writer.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        adapter.notifyDataSetChanged();
        tempCallback.setText("No temperatures have been measured yet.");

    }
}
