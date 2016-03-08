package com.ambarella.remotecam.connectivity;

import android.util.Log;

import com.ambarella.remotecam.CommonUtility;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by jli on 9/19/14.
 */
public class DataChannelWIFI extends DataChannel {
    private static final String TAG = "DataChannelWIFI";
    private static final int CONN_TIME_OUT = 3000;
    private static final int READ_TIME_OUT = 1000;

    private Socket mSocket;
    private String mHostName;
    private int mPortNum;

    public DataChannelWIFI(IChannelListener listener) {
        super(listener);
    }

    public DataChannelWIFI setIP(String host, int port) {
        mHostName = host;
        mPortNum = port;
        return this;
    }

    public boolean connect() {
        if (mSocket != null) {
            Log.e(TAG, "close old socket");
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSocket = null;
        }

        Log.e(TAG, "Connecting to new socket...");
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(mHostName, mPortNum), CONN_TIME_OUT);
            socket.setSoTimeout(READ_TIME_OUT);
            setStream(socket.getInputStream(), socket.getOutputStream());
            mSocket = socket;
            return true;
        } catch (IOException e) {
            Log.e(CommonUtility.LOG_TAG, e.getMessage());
            String message = "Can't connect to " + mHostName + "/" + mPortNum;
            mListener.onChannelEvent(IChannelListener.CMD_CHANNEL_EVENT_SHOW_ALERT, message);
        }
        return false;
    }

}
