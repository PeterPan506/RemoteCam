package com.ambarella.remotecam.connectivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.util.Log;

/**
 * Created by jli on 9/19/14.
 */
public class DataChannel {
    private final static String TAG = "DataChannel";
    private final static int PROGRESS_MIN_STEP = 1;
    
    protected IChannelListener mListener;
    protected InputStream mInputStream;
    protected OutputStream mOutputStream;
    protected boolean mContinueRx;
    
    protected boolean mContinueTx;
    protected int mTxBytes;
    protected final Object mTxLock = new Object();
    
    private static final ExecutorService worker = 
            Executors.newSingleThreadExecutor();

    public DataChannel(IChannelListener listener) {
        mListener = listener;
    }

    public DataChannel setStream(InputStream input, OutputStream output) {
        mInputStream = input;
        mOutputStream = output;
        return this;
    }

    public void getFile(final String dstPath, final int size) {
        mContinueRx = true;
        worker.execute(new Runnable() {
            public void run() {
                rxStream(dstPath, size);
            }
        });
    }

    public void cancelGetFile() {
        mContinueRx = false;
    }
    
    public void putFile(final String srcPath) {
        mContinueTx = true;
        worker.execute(new Runnable() {
            public void run() {
                txStream(srcPath);
            }
        });
    }

    public int cancelPutFile() {
        mContinueTx = false;
        synchronized(mTxLock) {
            try {
                mTxLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return mTxBytes;
    }
    
    private void txStream(String srcPath) {
        int total = 0;
        int prev = 0;
        
        try {
            byte[] buffer = new byte[1024];
            File file = new File(srcPath);
            FileInputStream in = new FileInputStream(file);
            final int size = (int)file.length();
            
            mTxBytes = 0;
            mListener.onChannelEvent(
                IChannelListener.DATA_CHANNEL_EVENT_PUT_START, srcPath);
            while (mContinueTx) {
                int read = in.read(buffer);
                if (read <= 0)
                    break;
                mOutputStream.write(buffer, 0, read);
                mTxBytes += read;

                total += read;
                int curr = (int)(((long)total*100) / size);
                if (curr - prev >= PROGRESS_MIN_STEP) {
                    mListener.onChannelEvent(
                        IChannelListener.DATA_CHANNEL_EVENT_PUT_PROGRESS, curr);
                    prev = curr;
                }
            }
            in.close();
            
            if (mContinueTx) {
                mListener.onChannelEvent(
                    IChannelListener.DATA_CHANNEL_EVENT_PUT_FINISH, srcPath);
            } else {
                synchronized (mTxLock) {
                    mTxLock.notify();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rxStream(String dstPath, int size) {
        int total = 0;
        int prev = 0;
        try {
            byte[] buffer = new byte[1024];
            FileOutputStream out = new FileOutputStream(dstPath);
            int bytes;

            mListener.onChannelEvent(IChannelListener.DATA_CHANNEL_EVENT_GET_START, dstPath);
            while (total < size) {
                try {
                    bytes = mInputStream.read(buffer);
                    //Log.e(TAG, "read bytes " + bytes);
                    out.write(buffer, 0, bytes);
                } catch (SocketTimeoutException e) {
                    if (!mContinueRx) {
                        Log.e(TAG, "RX canceled");
                        out.close();
                        return;
                    }
                    continue;
                }

                total += bytes;
                int curr = (int)(((long)total*100) / size);
                if (curr - prev >= PROGRESS_MIN_STEP) {
                    mListener.onChannelEvent(IChannelListener.DATA_CHANNEL_EVENT_GET_PROGRESS,
                            curr);
                    prev = curr;
                }
            }
            out.close();
            mListener.onChannelEvent(IChannelListener.DATA_CHANNEL_EVENT_GET_FINISH, dstPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
