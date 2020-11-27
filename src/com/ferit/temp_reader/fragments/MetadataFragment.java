package com.ferit.temp_reader.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.ferit.temp_reader.R;
import com.ferit.temp_reader.activities.MainActivity;
import com.ferit.temp_reader.activities.ReadMetadataActivity;
import com.ferit.temp_reader.util.AuthStatus;

import java.util.LinkedList;
import java.util.List;

public class MetadataFragment extends Fragment {
    private static List<String> metadata;
    private Button readAllMetadataButton;
    private Button readDeviceMetadataButton;
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
        readAllMetadataButton = v.findViewById(R.id.button_read_metadata);
        readDeviceMetadataButton = v.findViewById(R.id.button_metadata_device);
        metadataView = (ListView) v.findViewById(R.id.metadataList);
        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, metadata);
        metadataView.setAdapter(adapter);
        infoText = (TextView) v.findViewById(R.id.infoText);
        readAllMetadataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                if(MainActivity.authStatus.equals(AuthStatus.UNAUTHENTICATED)){
                    DialogFragment newFragment = new AuthDialogFragment();
                    newFragment.show(getActivity().getSupportFragmentManager(), "auth");
                    return;
                }
                Intent i = new Intent(getActivity(), ReadMetadataActivity.class);
                i.putExtra("mac","");
                startActivity(i);
            }
        });
        readDeviceMetadataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                Intent i = new Intent(getActivity(), ReadMetadataActivity.class);
                i.putExtra("mac", MainActivity.getMacAddr());
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
