package com.ambarella.remotecam.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.ambarella.remotecam.R;

public class BLESelectFragment extends Fragment {
    private static final long SCAN_PERIOD = 32000;

    private static boolean isScanning = false;
    private static AmbaBTListAdapter mListAdapter;
   
    private IFragmentListener mListener;
    private Button mButtonCancel;
    private Button mButtonScan;
    private ListView mListView;
    private ProgressBar mProgressBar;
    private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothAdapter.LeScanCallback mLeScanCallback;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_ble_select, container, false);

        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBarBLEScan);
        mProgressBar.setVisibility(isScanning ? View.VISIBLE : View.INVISIBLE);
        
        mButtonCancel = (Button) view.findViewById(R.id.buttonBLECancel);
        mButtonCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onFragmentAction(IFragmentListener.ACTION_BT_CANCEL, 0);
                }
            }
        });

        mButtonScan = (Button) view.findViewById(R.id.buttonBLEScan);
        mButtonScan.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                scanLeDevice();
            }
        });

        mListView = (ListView) view.findViewById(R.id.listViewBLEDevice);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mListener != null) {
                    stopScan();
                    String device[] = new String[2];
                    BluetoothDevice item = (BluetoothDevice) parent.getItemAtPosition(position);
                    device[0] = item.getName();
                    device[1] = item.getAddress();
                    mListener.onFragmentAction(IFragmentListener.ACTION_BT_SELECTED, device);
                }
            }
        });
        mListView.setAdapter(mListAdapter);

        if (!isScanning && mListAdapter.isEmpty())
            scanLeDevice();
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (IFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        
        mHandler = new Handler();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        if (mListAdapter == null) 
            mListAdapter = new AmbaBTListAdapter(activity);

        mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
  
            @Override
            public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                Log.e("OnLeScan", "get device: " + device.getName());
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        mListAdapter.addDevice(device);
                        mListAdapter.notifyDataSetChanged();
                    }
                });
            }
        };
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        stopScan();
    }

    private void scanLeDevice() {
        if (!isScanning) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScan();
               }
            }, SCAN_PERIOD);
            startScan();
        } else {
            stopScan();
        }
    }
    
    private void startScan() {
        isScanning = true;
        mListAdapter.clear();
        mProgressBar.setVisibility(View.VISIBLE);
        mButtonScan.setText("Stop");
        mBluetoothAdapter.startLeScan(mLeScanCallback);        
    }
    
    private void stopScan() {
        isScanning = false;
        mProgressBar.setVisibility(View.INVISIBLE);
        mButtonScan.setText("Scan");
        mBluetoothAdapter.stopLeScan(mLeScanCallback);        
    }
}
