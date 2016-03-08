package com.ambarella.remotecam.connectivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.ambarella.remotecam.CommonUtility;

/**
 * Created by jli on 9/8/14.
 */
public class CmdChannelBT extends CmdChannel {
    private static final String TAG="CmdChannelBT";
    private static final UUID UUID_CTRL = UUID.fromString("eeeccf74-680a-c514-d804-765fa0f7c5c9");

    private String mDeviceAddr;
    private BluetoothSocket mSocket;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private byte[] mBuffer = new byte[2048];//缓冲区扩大一倍

    public CmdChannelBT(IChannelListener listener) {
        super(listener);
    }

    public boolean connectTo(String addr) {
        if (mDeviceAddr != null && !mDeviceAddr.equals(addr)) {
            closeConnection();
        }

        mDeviceAddr = addr;
        return openConnection();
    }

    private boolean openConnection() {
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        if (!bta.isEnabled()) return false;

        Set<BluetoothDevice> bondedDevices = bta.getBondedDevices();
        for (BluetoothDevice device : bondedDevices) {
            if (!device.getAddress().equals(mDeviceAddr))
                continue;

            try {
                BluetoothSocket socket =
                        device.createInsecureRfcommSocketToServiceRecord(UUID_CTRL);

                socket.connect();
                if (socket.isConnected()) {
                    mSocket = socket;
                    mInputStream = mSocket.getInputStream();
                    mOutputStream = mSocket.getOutputStream();
                    startIO();
                    return true;
                } else {
                    Log.e(CommonUtility.LOG_TAG, "BT ctrl channel can't connect");
                }
            } catch (IOException e) {
                Log.e(CommonUtility.LOG_TAG, "openConnection: " + e.getMessage());
            }
        }

        return false;
    }

    private void closeConnection() {
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException ioe) {}
            mSocket = null;
        }
    }

    protected void writeToChannel(byte[] buffer) {
        try {
            if (mOutputStream != null)
                mOutputStream.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    protected String readLenFromBuf(){
        return null;
    }

    protected String readFromChannel(int size) {
        try {
            if (mInputStream != null) {
                //int size = mInputStream.read(mBuffer);
                mInputStream.read(mBuffer, 0, size);
                return new String(mBuffer, 0 , size);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
