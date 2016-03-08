package com.ambarella.remotecam.fragment;

import java.util.ArrayList;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ambarella.remotecam.R;

public class AmbaBTListAdapter extends BaseAdapter {
    private ArrayList<BluetoothDevice> mDevices;
    Context mContext;
    
    public AmbaBTListAdapter(Context context) {
        super();
        mContext = context;
        mDevices = new ArrayList<BluetoothDevice>();
    }

    public void addDevice(BluetoothDevice device) {
        if(!mDevices.contains(device)) {
            mDevices.add(device);
        }
    }

    public BluetoothDevice getDevice(int position) {
        return mDevices.get(position);
    }

    public void clear() {
        mDevices.clear();
    }

    @Override
    public int getCount() {
        return mDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return mDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.listview_bt_device, parent, false);

        BluetoothDevice device = mDevices.get(i);
        TextView nameView = (TextView) view.findViewById(R.id.textViewBTDeviceName);
        String name = device.getName();
        if (name == null) 
            name = "unknown device";
        nameView.setText(name);

        TextView addrView = (TextView) view.findViewById(R.id.textViewBTDeviceAddr);
        addrView.setText(device.getAddress());

        return view;
    }
}
