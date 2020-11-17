package com.ferit.temp_reader.fragments;

import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.ferit.temp_reader.R;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class GraphFragment extends Fragment {

    private static TextView exampleText;
    private static List<Double> measuredTemperatures;
    public static GraphView graph;
    public static LineGraphSeries<DataPoint> series;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.measuredTemperatures = new LinkedList<Double>();
        setRetainInstance(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_graph, container, false);
        try {
            JSONArray temps = ListFragment.getTempsFromFile();
            for(int i = 0; i < temps.size(); i++){
                JSONObject jsonObject = (JSONObject) temps.get(i);
                String tempAsString = (String) jsonObject.get("tempValue");
                String tempStringWithDot = tempAsString.replace(",",".");
                this.measuredTemperatures.add(Double.parseDouble(tempStringWithDot));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        graph = (GraphView) v.findViewById(R.id.graph);
        if(this.measuredTemperatures.size() > 0){
            series = new LineGraphSeries<>(convertToDataPointArray());
            graph.addSeries(series);
        }
        graph.getViewport().setScalable(true);
        graph.getGridLabelRenderer().setVerticalAxisTitle("temperature");
        graph.getGridLabelRenderer().setHorizontalAxisTitle("measurements");
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(this.measuredTemperatures.size());
        graph.getGridLabelRenderer().setNumHorizontalLabels(this.measuredTemperatures.size()+1);
        graph.getViewport().setXAxisBoundsManual(true);
        return v;
    }

    private DataPoint[] convertToDataPointArray(){
        int count = this.measuredTemperatures.size();
        DataPoint[] array = new DataPoint[count];
        for(int i = 0; i < array.length; i++){
            array[i] = new DataPoint(i, this.measuredTemperatures.get(i));
        }
        return array;
    }

    private static DataPoint convertToDataPoint(Double tempAsNumber) {
        return new DataPoint(measuredTemperatures.size()-1, tempAsNumber);
    }

    public static void addTempToGraph(String temp){
        String tempStringWithDot = temp.replace(",",".");
        Double tempAsNumber = Double.parseDouble(tempStringWithDot);
        boolean wasClearedBefore = measuredTemperatures.isEmpty();
        measuredTemperatures.add(tempAsNumber);
        graph.getViewport().setMaxX(graph.getViewport().getMaxX(false) + 1);
        if(wasClearedBefore){
            series = new LineGraphSeries<>(new DataPoint[] { convertToDataPoint(tempAsNumber)});
            graph.addSeries(series);
        }
        else{
            graph.getViewport().setMaxX(measuredTemperatures.size());
            graph.getGridLabelRenderer().setNumHorizontalLabels(measuredTemperatures.size()+1);
            series.appendData(convertToDataPoint(tempAsNumber), false, measuredTemperatures.size());
        }
        graph.onDataChanged(false,true);
    }

    public static void resetTemperatures(){
        graph.removeAllSeries();
        measuredTemperatures = new LinkedList<Double>();
        graph.getGridLabelRenderer().setNumHorizontalLabels(measuredTemperatures.size()+1);
        graph.getViewport().setMaxX(measuredTemperatures.size());
        graph.onDataChanged(true, true);
    }
}
