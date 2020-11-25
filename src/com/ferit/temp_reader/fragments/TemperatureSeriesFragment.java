package com.ferit.temp_reader.fragments;

import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.ferit.temp_reader.R;
import com.ferit.temp_reader.activities.AddSeriesActivity;
import com.ferit.temp_reader.reader.Ntag_I2C_Jobs;
import com.ferit.temp_reader.types.Temperature;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class TemperatureSeriesFragment extends Fragment {
    public static TextView status;
    private static List<Temperature> measuredTemperatures;
    public static GraphView graph;
    public static LineGraphSeries<DataPoint> series;
    private Button loadDataButton;
    public static Button startButton;
    public static Button saveButton;
    private Button clearButton;
    private static ListView tempListView;
    private EditText interval;
    private static ArrayAdapter<String> adapter;
    public static Tag tag;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.measuredTemperatures = new LinkedList<Temperature>();
        setRetainInstance(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_series, container, false);
        loadDataButton = v.findViewById(R.id.button_load_existing_data);
        startButton = v.findViewById(R.id.start_button);
        saveButton = v.findViewById(R.id.save);
        clearButton = v.findViewById(R.id.clear);
        interval = v.findViewById(R.id.interval);
        graph = (GraphView) v.findViewById(R.id.graph2);
        adapter = new ArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, measuredTemperatures);
        status = (TextView) v.findViewById(R.id.status_text);
        tempListView = (ListView) v.findViewById(R.id.tempList);
        tempListView.setAdapter(adapter);
        loadDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                graph.removeAllSeries();
                measuredTemperatures.clear();
                loadDataFromStorage();
            }
        });
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    startSeriesMeasurement();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (FormatException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        saveButton.setOnClickListener(new View.OnClickListener(){
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view){
                ArrayList<Temperature> seriesTemps = new ArrayList<Temperature>();
                seriesTemps = (ArrayList) measuredTemperatures.stream().filter(temp -> temp.getSeriesTemp()).collect(Collectors.toList());
                Intent i = new Intent(getActivity(), AddSeriesActivity.class);
                i.putParcelableArrayListExtra("seriesTemps", seriesTemps);
                startActivity(i);
            }
        });
        clearButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                series = new LineGraphSeries<>();
                graph.removeAllSeries();
                graph.addSeries(series);
                measuredTemperatures.clear();
                graph.getGridLabelRenderer().setNumHorizontalLabels(measuredTemperatures.size()+1);
                graph.getViewport().setMaxX(measuredTemperatures.size());
                graph.onDataChanged(true, true);
                adapter.notifyDataSetChanged();
            }
        });
        return v;
    }

    private void startSeriesMeasurement() throws IOException, FormatException, InterruptedException {
        Ntag_I2C_Jobs demo = new Ntag_I2C_Jobs(tag, getActivity());
        int intervalNumber = interval.getText().toString().equals("") ? 0 : Integer.parseInt(interval.getText().toString());
        demo.temp(true, intervalNumber);
    }

    public static void addTempToList(Temperature temperature){
        measuredTemperatures.add(temperature);
        String temperatureString = temperature.getTemperatureValue().replace(",",".");
        Double tempDouble = Double.parseDouble(temperatureString);
        graph.getViewport().setMaxX(measuredTemperatures.size());
        if(series == null){
            series = new LineGraphSeries<>(new DataPoint[] { convertToDataPoint(tempDouble)});
            graph.getViewport().setMinX(0);
            graph.getViewport().setMaxX(measuredTemperatures.size());
            graph.getGridLabelRenderer().setNumHorizontalLabels(measuredTemperatures.size()+1);
            graph.getViewport().setXAxisBoundsManual(true);
            graph.addSeries(series);
        }
        else{
            graph.getViewport().setMaxX(measuredTemperatures.size());
            graph.getGridLabelRenderer().setNumHorizontalLabels((measuredTemperatures.size() % 7) + 2);
            series.appendData(convertToDataPoint(tempDouble), false, measuredTemperatures.size());
        }
        adapter.notifyDataSetChanged();
        graph.onDataChanged(false,true);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void loadDataFromStorage(){
        try {
            JSONArray temps = ListFragment.getTempsFromFile();
            for(int i = 0; i < temps.size(); i++){
                JSONObject jsonObject = (JSONObject) temps.get(i);
                String timestamp = (String) jsonObject.get("timestamp");
                String tempValue = (String) jsonObject.get("tempValue");
                Temperature temperature = new Temperature(timestamp, tempValue, false);
                measuredTemperatures.add(temperature);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if(this.measuredTemperatures.size() > 0){
            series = new LineGraphSeries<>(convertToDataPointArray());
            adapter = new ArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, measuredTemperatures);
            adapter.notifyDataSetChanged();
            tempListView.setAdapter(adapter);
        }
        else{
            series = new LineGraphSeries<>();
        }
        graph.addSeries(series);
        graph.getGridLabelRenderer().setVerticalAxisTitle("temperature");
        graph.getGridLabelRenderer().setHorizontalAxisTitle("measurements");
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(this.measuredTemperatures.size());
        graph.getGridLabelRenderer().setNumHorizontalLabels(this.measuredTemperatures.size()+1);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.onDataChanged(false, false);
    }

    private DataPoint[] convertToDataPointArray(){
        int count = this.measuredTemperatures.size();
        DataPoint[] array = new DataPoint[count];
        for(int i = 0; i < array.length; i++){
            String tempStringWithDot = measuredTemperatures.get(i).getTemperatureValue().replace(",",".");
            Double tempAsDouble = Double.parseDouble(tempStringWithDot);
            array[i] = new DataPoint(i, tempAsDouble);
        }
        return array;
    }

    private static DataPoint convertToDataPoint(Double tempAsNumber) {
        return new DataPoint(measuredTemperatures.size()-1, tempAsNumber);
    }
}
