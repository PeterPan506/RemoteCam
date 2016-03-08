package com.ambarella.remotecam.fragment;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ambarella.remotecam.R;
import com.ambarella.remotecam.connectivity.IChannelListener;
import com.ambarella.streamview.AmbaStreamView;

public class CameraFragment extends Fragment 
	implements View.OnClickListener {
    private final static String TAG = "CameraFrag";
    private final static String ZOOM_NORMAL = "normal";
    private final static String ZOOM_FAST = "fast";
    private final static String ZOOM_SLOW = "slow";
    private final static String ZOOM_JUMP = "jump";
    private final static String ZOOM_INFO_MAX = "max";
    private final static String ZOOM_INFO_CURRENT = "current";
    private final static String ZOOM_INFO_STATUS = "status";

    
    private final static int VIDEOVIEW_COLOR_ON = 0x000000FF;
    private final static int VIDEOVIEW_COLOR_OFF = 0x000000FF;
    
    private final static int STATE_OFF = 0b00;
    private final static int STATE_STARTING = 0b01;
    private final static int STATE_ON = 0b10;
    private final static int STATE_STOPPING = 0b11;

    static private int mStreamingState;
    static private int mRecordingState;
    static private int mPlayingState;
    static private boolean mEnableClock = false;
    static private boolean mEnableMic = true;

    static private final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor();
    static private ScheduledFuture<?> mScheduledTask;

    private IFragmentListener mListener;
    private AmbaStreamView mVideoView;
    
    private ImageView mViewPlayer;
    private TextView  mViewTime;
    private TextView mViewClockEnable;
    private TextView mViewMicEnable;
    private Drawable mDrawable;
    private SeekBar mZoomControl;
    private int mZoomMax = 255;
    private int mZoomCurrent = 0;
    private String mZoomStatus = "idle";
    private int mZoomType = R.id.radioButtonZoomNormal;
    private String mZoomString = ZOOM_NORMAL;

    public void reset() {
        mStreamingState = STATE_OFF;
        mRecordingState = STATE_OFF;
        mPlayingState = STATE_OFF;
        hideTimer();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        ImageView imageView;
        TextView textViewCam;

        Log.e("CAM", "onCreateView");
        
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mVideoView = (AmbaStreamView) view.findViewById(R.id.videoViewFinder);
		mVideoView.setEGLContextClientVersion(2);
		mVideoView.setRendererDensity(displayMetrics.density);
		mVideoView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        textViewCam = (TextView) view.findViewById(R.id.imageViewStartVF);
        textViewCam.setOnClickListener(this);


        textViewCam = (TextView) view.findViewById(R.id.imageViewStopVF);
        textViewCam.setOnClickListener(this);
        
        mViewPlayer = (ImageView) view.findViewById(R.id.imageViewPlayer);
        mViewPlayer.setOnClickListener(this);
        if (mStreamingState != STATE_ON && mStreamingState != STATE_OFF) {
            mViewPlayer.setClickable(false);
        }

        textViewCam = (TextView) view.findViewById(R.id.imageViewStartRecord);
        textViewCam.setOnClickListener(this);

        textViewCam = (TextView) view.findViewById(R.id.imageViewStopRecord);
        textViewCam.setOnClickListener(this);

        textViewCam = (TextView) view.findViewById(R.id.imageViewForceSplit);
        textViewCam.setOnClickListener(this);

        textViewCam = (TextView) view.findViewById(R.id.imageViewStartPhoto);
        textViewCam.setOnClickListener(this);

        textViewCam = (TextView) view.findViewById(R.id.imageViewSyncTime);
        textViewCam.setOnClickListener(this);

        imageView = (ImageView) view.findViewById(R.id.imageViewZoomInfo);
        imageView.setOnClickListener(this);

        mViewMicEnable = (TextView) view.findViewById(R.id.imageViewStartMic);
        mViewMicEnable.setOnClickListener(this);
        //mViewMicEnable.setBackgroundResource(mEnableMic ? R.drawable.ic_mic_on
        //        : R.drawable.ic_mic_off);
        mDrawable = getResources().getDrawable(mEnableMic ? R.drawable.ic_mic_on
                : R.drawable.ic_mic_off);
        mDrawable.setBounds(0, 0, mDrawable.getMinimumWidth(), mDrawable.getMinimumHeight());
        mViewMicEnable.setCompoundDrawables(null, mDrawable, null, null);

        mViewClockEnable = (TextView) view.findViewById(R.id.imageViewClock);
        mViewClockEnable.setOnClickListener(this);
        //mViewClockEnable.setBackgroundResource(mEnableClock ?
        //        R.drawable.ic_clock : R.drawable.ic_no_clock);
        mDrawable = getResources().getDrawable(mEnableClock ? R.drawable.ic_clock
                : R.drawable.ic_no_clock);
        mDrawable.setBounds(0, 0, mDrawable.getMinimumWidth(), mDrawable.getMinimumHeight());
        mViewClockEnable.setCompoundDrawables(null,mDrawable,null,null);

        // seekbar
        mZoomControl = (SeekBar) view.findViewById(R.id.seekBarZoom);
        mZoomControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mZoomCurrent = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.e(TAG, "Touch start");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mListener.onFragmentAction(
                        IFragmentListener.ACTION_SET_ZOOM, mZoomString, mZoomCurrent);
                Log.e(TAG, "Touch stop");
            }
        });
        mListener.onFragmentAction(IFragmentListener.ACTION_GET_ZOOM_INFO, ZOOM_INFO_MAX);
        mListener.onFragmentAction(IFragmentListener.ACTION_GET_ZOOM_INFO, ZOOM_INFO_CURRENT);

		updatePlayerViews();
	    if (mPlayingState == STATE_ON)
        {
	        mVideoView.start();
            Log.e("TAG","call rendererStart");
        }

		mViewTime = (TextView) view.findViewById(R.id.textViewRecordTime);
		if (mRecordingState == STATE_ON)
		    showTimer();
		else 
		    mViewTime.setVisibility(View.INVISIBLE);
        return view;
    }

    @Override
	public void onClick(View v) {
    	switch (v.getId()) {
    	case R.id.imageViewStartVF:
    	    if (0 == (mStreamingState & 0b01)) {
                if (mStreamingState == STATE_OFF)
                    mStreamingState = STATE_STARTING;
    	        mListener.onFragmentAction(IFragmentListener.ACTION_VF_START, null);
    	    }
    		break;
        case R.id.imageViewStopVF:
            if (0 == (mStreamingState & 0b01)) {
                if (mStreamingState == STATE_ON) {
                    mStreamingState = STATE_STOPPING;
                    if (mPlayingState == STATE_ON) {
                        mPlayingState = STATE_OFF;
                        mListener.onFragmentAction(
                                IFragmentListener.ACTION_PLAYER_STOP, null);
                    }
                }
                mListener.onFragmentAction(IFragmentListener.ACTION_VF_STOP, null);
            }
            break;
        case R.id.imageViewPlayer:
            Log.e(TAG, "StartPlayer " + mPlayingState);
            if (mPlayingState == STATE_OFF) {
                mPlayingState = STATE_STARTING;
                updatePlayerViews();
                mListener.onFragmentAction(IFragmentListener.ACTION_PLAYER_START, null);
            } else if (mPlayingState == STATE_ON) {
                mPlayingState = STATE_STOPPING;
                updatePlayerViews();
                mListener.onFragmentAction(IFragmentListener.ACTION_PLAYER_STOP, null);
            }
            break;            
    	case R.id.imageViewStartRecord:
            if (0 == (mRecordingState & 0b01)) {
                if (mRecordingState == STATE_OFF)
                    mRecordingState = STATE_STARTING;
                mListener.onFragmentAction(IFragmentListener.ACTION_RECORD_START, null);
            }
    	    break;
        case R.id.imageViewStopRecord:
            if (0 == (mRecordingState & 0b01)) {
                if (mRecordingState == STATE_ON)
                    mRecordingState = STATE_STOPPING;
                mListener.onFragmentAction(IFragmentListener.ACTION_RECORD_STOP, null);
            }
            break;
        case R.id.imageViewStartMic:
            mEnableMic = !mEnableMic;
            if (mEnableMic) {
                //mViewMicEnable.setBackgroundResource(R.drawable.ic_mic_on);
                mDrawable = getResources().getDrawable(R.drawable.ic_mic_on);
                mDrawable.setBounds(0, 0, mDrawable.getMinimumWidth(), mDrawable.getMinimumHeight());
                mViewMicEnable.setCompoundDrawables(null,mDrawable,null,null);
                mListener.onFragmentAction(IFragmentListener.ACTION_MIC_ON, null);
            }else {
                //mViewMicEnable.setBackgroundResource(R.drawable.ic_mic_off);
                mDrawable = getResources().getDrawable(R.drawable.ic_mic_off);
                mDrawable.setBounds(0, 0, mDrawable.getMinimumWidth(), mDrawable.getMinimumHeight());
                mViewMicEnable.setCompoundDrawables(null,mDrawable,null,null);
                mListener.onFragmentAction(IFragmentListener.ACTION_MIC_OFF, null);
            }
            break;
        case R.id.imageViewForceSplit:
            mListener.onFragmentAction(IFragmentListener.ACTION_FORCE_SPLIT, null);
            break;
    	case R.id.imageViewStartPhoto:
    	    mListener.onFragmentAction(IFragmentListener.ACTION_PHOTO_START, null);
    	    break;
        //case R.id.imageViewStopPhoto:
        //    mListener.onFragmentAction(IFragmentListener.ACTION_PHOTO_STOP, null);
        //    break;
        case R.id.imageViewSyncTime:
            mListener.onFragmentAction(IFragmentListener.ACTION_SYNC_TIME, null);
            break;
        case R.id.imageViewClock:
            Log.e(TAG, "clock is clicked");
            mEnableClock = !mEnableClock;
            if (mEnableClock) {
                //mViewClockEnable.setBackgroundResource(R.drawable.ic_clock);
                mDrawable = getResources().getDrawable(R.drawable.ic_clock);
                mDrawable.setBounds(0, 0, mDrawable.getMinimumWidth(), mDrawable.getMinimumHeight());
                mViewClockEnable.setCompoundDrawables(null,mDrawable,null,null);
                if (mRecordingState == STATE_ON)
                    showTimer();
            } else {
                //mViewClockEnable.setBackgroundResource(R.drawable.ic_no_clock);
                mDrawable = getResources().getDrawable(R.drawable.ic_no_clock);
                mDrawable.setBounds(0, 0, mDrawable.getMinimumWidth(), mDrawable.getMinimumHeight());
                mViewClockEnable.setCompoundDrawables(null,mDrawable,null,null);
                if (mRecordingState == STATE_ON)
                    hideTimer();
            }
            break;
        case R.id.imageViewZoomInfo:
            //mListener.onFragmentAction(IFragmentListener.ACTION_GET_ZOOM_INFO, ZOOM_INFO_MAX);
            mListener.onFragmentAction(IFragmentListener.ACTION_GET_ZOOM_INFO, ZOOM_INFO_CURRENT);
            mListener.onFragmentAction(IFragmentListener.ACTION_GET_ZOOM_INFO, ZOOM_INFO_STATUS);
            break;
    	}
	}

    @Override
    public void onAttach(Activity activity) {
        Log.e("CAM", "onAttach");
        super.onAttach(activity);
        try {
            mListener = (IFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement IFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        Log.e("CAM", "onDetach");
        super.onDetach();
        if (mScheduledTask != null) {
            mScheduledTask.cancel(true);
            mScheduledTask = null;
        }
        mListener = null;
    }
    
    public void onVFReset() {
        mStreamingState = STATE_ON;
        updatePlayerViews();
    }
    
    public void onVFStopped() {
        mStreamingState = STATE_OFF;
        updatePlayerViews();
    }
    
    public void startStreamView() {
        mPlayingState = STATE_ON;
        updatePlayerViews();
        mVideoView.start();
    }
    
    public void stopStreamView() {
        mPlayingState = STATE_OFF;
        updatePlayerViews();
        mVideoView.stop();
        mVideoView.setBackgroundColor(VIDEOVIEW_COLOR_ON);
    }
    
    public void resetStreamView() {
        mPlayingState = STATE_OFF;
        updatePlayerViews();
    }

    public void setZoomDone(int errorCode) {
        if (errorCode != 0) {
            // get real zoom value
            Log.e(TAG, "Set Zoom failed");
            mListener.onFragmentAction(IFragmentListener.ACTION_GET_ZOOM_INFO, ZOOM_INFO_CURRENT);
        }
    }

    public void setZoomInfo(String type, String param) {
        switch (type) {
            case ZOOM_INFO_MAX:
                mZoomMax = Integer.parseInt(param);
                mZoomControl.setMax(mZoomMax);
                break;
            case ZOOM_INFO_CURRENT:
                mZoomCurrent = Integer.parseInt(param);
                mZoomControl.setProgress(mZoomCurrent);
                break;
            default:
                mZoomStatus = param;
                showZoomInfoDialog();
        }
    }

    private void showZoomInfoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_zoom_info, null);

        TextView textView;
        textView = (TextView) view.findViewById(R.id.textViewZoomMax);
        textView.setText(Integer.toString(mZoomMax));
        textView = (TextView) view.findViewById(R.id.textViewZoomCurrent);
        textView.setText(Integer.toString(mZoomCurrent));
        textView = (TextView) view.findViewById(R.id.textViewZoomStatus);
        textView.setText(mZoomStatus);

        final RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radioGroupZoomType);
        radioGroup.check(mZoomType);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                mZoomType = i;
                switch (mZoomType) {
                    case R.id.radioButtonZoomFast:
                        mZoomString = ZOOM_FAST;
                        break;
                    case R.id.radioButtonZoomJump:
                        mZoomString = ZOOM_JUMP;
                        break;
                    case R.id.radioButtonZoomSlow:
                        mZoomString = ZOOM_SLOW;
                        break;
                    default:
                        mZoomString = ZOOM_NORMAL;
                }
            }
        });

        builder.setTitle("Camera Zoom")
               .setView(view)
               .setPositiveButton("OK", null)
               .show();
    }

    private void updatePlayerViews() {
        mVideoView.setBackgroundColor(VIDEOVIEW_COLOR_ON);
        if (mPlayingState == STATE_ON) {
            mViewPlayer.setVisibility(View.VISIBLE);
            mViewPlayer.setImageResource(R.drawable.ic_shutter_stop);
        } else if (mPlayingState == STATE_OFF) {
            mViewPlayer.setVisibility(View.VISIBLE);
            mViewPlayer.setImageResource(R.drawable.ic_shutter_start);
       } else {
            mViewPlayer.setVisibility(View.INVISIBLE);
        }
    }
    
    private void showTimer() {
        if (!mEnableClock)
            return;
        mViewTime.setVisibility(View.VISIBLE);
        mScheduledTask = worker.scheduleAtFixedRate(new Runnable() {
            public void run() {
                mListener.onFragmentAction(IFragmentListener.ACTION_RECORD_TIME, null);                  
            }
        }, 0, 1, TimeUnit.SECONDS);        
    }
    
    private void hideTimer() {
        if (mViewTime != null)
            mViewTime.setVisibility(View.INVISIBLE);

        if (mScheduledTask != null)
            mScheduledTask.cancel(false);
    }
    
    public void startRecord() {
        if (mRecordingState == STATE_STARTING) {
            mRecordingState = STATE_ON;
            mViewTime.setText("00:00");
            showTimer();
        }
    }

    public void stopRecord() {
        if (mRecordingState == STATE_STOPPING) {
            mRecordingState = STATE_OFF;
            hideTimer();
        }
    }
    
    public void upDateRecordTime(String time) {
        int seconds = Integer.parseInt(time);
        int minutes = seconds / 60;
        seconds -= minutes * 60;
        final String timeText = String.format("%02d:%02d", minutes, seconds);
        mViewTime.setText(timeText);
    }
}
