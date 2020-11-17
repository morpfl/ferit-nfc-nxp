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


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.ferit.temp_reader.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class ListFragment extends Fragment {

    private static double temperatureC;
    private static double temperatureF;
    private static TextView tempCallback;
    private static String option;
    private static List<String> measuredTemperatures;
    private static ListView mListView;
    private static ArrayAdapter<String> adapter;
    private static String filesDir;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        option = "L2";
        measuredTemperatures = new LinkedList<String>();
        filesDir = getActivity().getFilesDir().toString();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_list, container, false);
        tempCallback = (TextView) v.findViewById(R.id.tempCallback);
        mListView = (ListView) v.findViewById(R.id.list);
        adapter = new ArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, this.measuredTemperatures);
        mListView.setAdapter(adapter);
        try {
            InputStream is = new FileInputStream( "/data/data/com.ferit.temp_reader/files/temperatures/temperatures.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            if(is != null){
                while (reader.ready()){
                    this.measuredTemperatures.add(reader.readLine());
                }
                if(!this.measuredTemperatures.isEmpty()){
                    this.tempCallback.setText("measured temperatures: ");
                }
                adapter.notifyDataSetChanged();
            }
            is.close();
        } catch (
                FileNotFoundException e) {
            File dir = new File(filesDir, "temperatures");
            if(!dir.exists()){
                dir.mkdir();
            }
        } catch (
                IOException e) {
            e.printStackTrace();
        }
        return v;
    }

    public static void writeFileOnInternalStorage(String newTemperature){
        File dir = new File(filesDir, "temperatures");
        try {
            File file = new File(dir, "temperatures.txt");
            FileWriter writer = new FileWriter(file, true);
            writer.append(newTemperature + "\n");
            writer.close();
        } catch (Exception e){
            e.printStackTrace();
        }
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
    public static void addTempToList(String temp) {
        tempCallback.setText("measured temperatures: ");
        measuredTemperatures.add(temp);
        adapter.notifyDataSetChanged();
        writeFileOnInternalStorage(temp);
    }

    public static void resetTemperatures(){
        measuredTemperatures.removeAll(measuredTemperatures);
        File dir = new File(filesDir, "temperatures");
        try {
            File gpxfile = new File(dir, "temperatures.txt");
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
