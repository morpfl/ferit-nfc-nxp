package com.ferit.temp_reader.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.ferit.temp_reader.R;
import com.ferit.temp_reader.activities.ReadMetadataActivity;

import java.util.LinkedList;
import java.util.List;

public class MetadataFragment extends Fragment {
    private static List<String> metadata;
    private Button readButton;
    private ListView metadataView;
    private static ArrayAdapter<String> adapter;
    public static TextView infoText;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        metadata = new LinkedList<String>();
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_metadata, container, false);
        readButton = v.findViewById(R.id.button_read_metadata);
        metadataView = (ListView) v.findViewById(R.id.metadataList);
        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, this.metadata);
        metadataView.setAdapter(adapter);
        infoText = (TextView) v.findViewById(R.id.infoText);
        readButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                Intent i = new Intent(getActivity(), ReadMetadataActivity.class);
                startActivity(i);
            }
        });
        return v;
    }

    public static void setMetadata(List<String> formattedMetadata){
        metadata.clear();
        metadata.addAll(formattedMetadata);
        adapter.notifyDataSetChanged();
    }

    public static void resetMetadata(){
        metadata.clear();
        adapter.notifyDataSetChanged();
    }


}
