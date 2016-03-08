package com.ambarella.remotecam.fragment;

import java.util.Set;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.ambarella.remotecam.R;

public class BTSelectFragment extends Fragment {
    private IFragmentListener mListener;
    private Button mButtonCancel;
    private Button mButtonSettings;
    private ListView mListView;
    private AmbaBTListAdapter mListAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_bt_select, container, false);

        mButtonCancel = (Button) view.findViewById(R.id.buttonBTCancel);
        mButtonCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onFragmentAction(IFragmentListener.ACTION_BT_CANCEL, 0);
                }
            }
        });

        mButtonSettings = (Button) view.findViewById(R.id.buttonBTSettings);
        mButtonSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(intent);
            }
        });

        mListView = (ListView) view.findViewById(R.id.listViewBTDevice);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mListener != null) {
                    String device[] = new String[2];
                    BluetoothDevice item = (BluetoothDevice) parent.getItemAtPosition(position);
                    device[0] = item.getName();
                    device[1] = item.getAddress();
                    mListener.onFragmentAction(IFragmentListener.ACTION_BT_SELECTED, device);
                }
            }
        });
        mListView.setAdapter(mListAdapter);
        listPairedDevices();

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        Log.e("CAM", "onAttach");
        super.onAttach(activity);
        try {
            mListener = (IFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        mListAdapter = new AmbaBTListAdapter(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void listPairedDevices() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        mListAdapter.clear();
        for (BluetoothDevice device : pairedDevices) {
            mListAdapter.addDevice(device);
        }
        mListAdapter.notifyDataSetChanged();
    }
}
