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
import com.ferit.temp_reader.reader.Ntag_I2C_Demo;
import com.ferit.temp_reader.types.Temperature;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TemperatureSeriesFragment extends Fragment {
    public static TextView status;
    private static List<Temperature> measuredTemperatures;
    public static GraphView graph;
    public static LineGraphSeries<DataPoint> series;
    private Button loadDataButton;
    public static Button startButton;
    private static ListView tempListView;
    private EditText period;
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
        period = v.findViewById(R.id.period_sec);
        graph = (GraphView) v.findViewById(R.id.graph2);
        status = (TextView) v.findViewById(R.id.status_text);
        tempListView = (ListView) v.findViewById(R.id.tempList);
        adapter = new ArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, this.measuredTemperatures);
        tempListView.setAdapter(adapter);
        loadDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
        return v;
    }

    private void startSeriesMeasurement() throws IOException, FormatException, InterruptedException {
        Ntag_I2C_Demo demo = new Ntag_I2C_Demo(tag, getActivity());
        demo.temp(true, Integer.parseInt(period.getText().toString()));
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
        this.measuredTemperatures = new LinkedList<Temperature>();
        try {
            JSONArray temps = ListFragment.getTempsFromFile();
            for(int i = 0; i < temps.size(); i++){
                JSONObject jsonObject = (JSONObject) temps.get(i);
                String timestamp = (String) jsonObject.get("timestamp");
                String tempValue = (String) jsonObject.get("tempValue");
                Temperature temperature = new Temperature(timestamp, tempValue);
                measuredTemperatures.add(temperature);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if(this.measuredTemperatures.size() > 0){
            series = new LineGraphSeries<>(convertToDataPointArray());
            graph.addSeries(series);
        }
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
