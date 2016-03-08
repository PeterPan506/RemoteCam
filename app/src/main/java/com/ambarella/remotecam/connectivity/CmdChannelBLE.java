package com.ambarella.remotecam.connectivity;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class CmdChannelBLE extends CmdChannel {
    private final static String TAG="CmdChannelBLE";

    private final static UUID AMBA_SERVICE_0 = 
            UUID.fromString("00000000-0000-1000-8000-00805f9b34fb");
    private final static UUID AMBA_CHARACTERISTIC_OUTPUT_0 = 
            UUID.fromString("00001111-0000-1000-8000-00805f9b34fb");
    private final static UUID AMBA_CHARACTERISTIC_INPUT_0 = 
            UUID.fromString("00003333-0000-1000-8000-00805f9b34fb");
    
    private final static UUID AMBA_SERVICE_1 = 
            UUID.fromString("00000000-616d-6261-5f69-645f62617365");
    private final static UUID AMBA_CHARACTERISTIC_OUTPUT_1 = 
            UUID.fromString("11111111-616d-6261-5f69-645f62617365");
    private final static UUID AMBA_CHARACTERISTIC_INPUT_1 = 
            UUID.fromString("33333333-616d-6261-5f69-645f62617365");
    
    
    private final static UUID CLIENT_CHARACTERISTIC_CONFIG = 
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final ScheduledExecutorService worker = 
            Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> mScheduledFuture;
    private String mDeviceAddr;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mGattInput;
    private BluetoothGattCharacteristic mGattOutput;
    private final Object mGattLock;
    private Context mContext;
    private LinkedBlockingQueue<byte[]> mNotificationQueue = new
            LinkedBlockingQueue<byte[]>(32);

    private byte[] mOutgoingMsg;
    private int mOutgoingIndex;
    private int mOutgoingTotal;
    private int mMtuSize;

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.e(TAG, "Gattcallback onConnectionStateChanged, status(" 
                    + status + "), newState(" + newState + ")");
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.e(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.e(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e(TAG, "Disconnected from GATT server.");
                disConnect();
                mListener.onChannelEvent(IChannelListener.CMD_CHANNEL_ERROR_BROKEN_CHANNEL, null);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mScheduledFuture.cancel(true);
                getAmbaCharacristics();
            } else {
                Log.e(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "onCharacteristicRead " + new String(characteristic.getValue()));
                Log.e(TAG, "onCharacteristicRead " + Arrays.toString(characteristic.getValue()));
            }
        }

        public void onCharacteristicWrite(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "onCharacteristicWrite " + characteristic.getValue().length);
                // check if there are more bytes to send
                mOutgoingIndex += mMtuSize;
                if (mOutgoingIndex < mOutgoingTotal) {
                    int bytesLeft = mOutgoingTotal - mOutgoingIndex;
                    mGattOutput.setValue(Arrays.copyOfRange(mOutgoingMsg,
                            mOutgoingIndex, mOutgoingIndex + Math.min(bytesLeft, mMtuSize)));
                    mBluetoothGatt.writeCharacteristic(mGattOutput);
                }
            } else
                Log.e(TAG, "onCharacteristicWrite failure " + status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            try {
                byte[] bytes = characteristic.getValue();
                //mListener.onChannelEvent(IChannelListener.CMD_CHANNEL_EVENT_SHOW_ALERT, 
                //        "Get Notification: " + Arrays.toString(bytes));
                mNotificationQueue.put(bytes);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    public CmdChannelBLE(IChannelListener listener) {
        super(listener);
        mGattLock = new Object();
    }

    public CmdChannelBLE setContext(Context context) {
        mContext = context;
        return this;
    }
    
    public boolean connectTo(String addr) {
        // check if we have a valid BLE address
        if (addr.equals("00:00:00:00:00:00")) {
            mListener.onChannelEvent(
                IChannelListener.CMD_CHANNEL_ERROR_BLE_INVALID_ADDR, null);
            return false;
        }

        Log.e(TAG, "Connecting to " + addr);
        if (mDeviceAddr != null && !mDeviceAddr.equals(addr)) {
            disConnect();
        }

        return connect(addr);
    }

    private boolean connect(String addr) {
        BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
        if (!bta.isEnabled()) {
            mListener.onChannelEvent(
                    IChannelListener.CMD_CHANNEL_ERROR_BLE_DISABLED, null);
            return false;
        }

        mBluetoothDevice = bta.getRemoteDevice(addr);
        mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, mGattCallback);
        mListener.onChannelEvent(IChannelListener.CMD_CHANNEL_EVENT_START_CONNECT, null);
        /*
         * This is a workaround for Amba BLE device: The first-time connection
         * takes forever to finish. We wait for a few seconds, close the 
         * connection then reconnect again. 
         */
        mScheduledFuture = worker.schedule(new Runnable() {
            public void run() {
                disConnect();                
                mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, mGattCallback);
                mScheduledFuture = worker.schedule(new Runnable() {
                    public void run() {
                        disConnect();
                        mListener.onChannelEvent(IChannelListener.CMD_CHANNEL_ERROR_CONNECT, null);
                    }
                }, 30, TimeUnit.SECONDS); 
            }
        }, 5, TimeUnit.SECONDS);
        
        /*
         * wait for connection to be established
         */
        try {
            synchronized (mGattLock) {
                mGattLock.wait();
            }
        } catch (InterruptedException e) {
            return false;
        }

        if (mGattOutput == null || mGattInput == null) {
            Log.e(TAG, "Can't find target amba characteristics");
            return false;
        }
        
        mListener.onChannelEvent(IChannelListener.CMD_CHANNEL_EVENT_CONNECTED, null);
        Log.e(TAG, "connnection established");
        
        // enable notification for mGattInput;
        mBluetoothGatt.setCharacteristicNotification(mGattInput, true);
        /*BluetoothGattDescriptor descriptor = mGattInput.getDescriptor(
                CLIENT_CHARACTERISTIC_CONFIG);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {e.printStackTrace();}*/
        
        mDeviceAddr = addr;
        startIO();
        return true;
    }

    private void disConnect() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }
    
    private void getAmbaCharacristics() {
        List<BluetoothGattService> services = mBluetoothGatt.getServices();
        
        mGattOutput = null;
        mGattInput = null;
        for (BluetoothGattService service : services) {
            Log.e(TAG, "Service: " + service.getUuid().toString());
            if (service.getUuid().equals(AMBA_SERVICE_0)) {
                List<BluetoothGattCharacteristic> characs = service.getCharacteristics();
                for (BluetoothGattCharacteristic charac : characs) {
                    Log.e(TAG, "characteristic0: " + charac.getUuid().toString());
                    if (charac.getUuid().equals(AMBA_CHARACTERISTIC_OUTPUT_0))
                        mGattOutput = charac;
                    if (charac.getUuid().equals(AMBA_CHARACTERISTIC_INPUT_0))
                        mGattInput = charac;
                }
                mMtuSize = 509;
                break;
            }
            else if (service.getUuid().equals(AMBA_SERVICE_1)) {
                List<BluetoothGattCharacteristic> characs = service.getCharacteristics();
                for (BluetoothGattCharacteristic charac : characs) {
                    Log.e(TAG, "characteristic1: " + charac.getUuid().toString());
                    if (charac.getUuid().equals(AMBA_CHARACTERISTIC_OUTPUT_1))
                        mGattOutput = charac;
                    if (charac.getUuid().equals(AMBA_CHARACTERISTIC_INPUT_1))
                        mGattInput = charac;
                }
                mMtuSize = 18;
                break;
            }
        }
        
        synchronized (mGattLock) {
            mGattLock.notify();
        }
    }
    
    protected void writeToChannel(byte[] buffer) {
        //byte[] fake = new byte[32];
        //mBluetoothGatt.readCharacteristic(mGattOutput);
        
        if (mGattOutput != null) {
            mOutgoingMsg = Arrays.copyOf(buffer, buffer.length);
            mOutgoingIndex = 0;
            mOutgoingTotal = mOutgoingMsg.length;
            mGattOutput.setValue(Arrays.copyOfRange(mOutgoingMsg, 
                    0, Math.min(mOutgoingTotal, mMtuSize)));
            mBluetoothGatt.writeCharacteristic(mGattOutput);
        }
    }

    protected String readLenFromBuf(){
        return null;
    }

    protected String readFromChannel(int size) {
        try {
            return new String(mNotificationQueue.take());
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    } 
}