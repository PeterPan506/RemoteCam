package com.ambarella.remotecam;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.VideoView;

public class StreamPlayerActivity extends Activity
    implements MediaPlayer.OnPreparedListener {
    VideoView mVideoView;
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream_player);

        Intent intent = getIntent();
        String url = intent.getStringExtra(CommonUtility.PLAYER_EXTRA);
        Log.e(CommonUtility.LOG_TAG, "Player path " + url);
        mVideoView = (VideoView) findViewById(R.id.videoViewFile);
        mVideoView.setVideoURI(Uri.parse(url));
        mVideoView.requestFocus();
        mVideoView.setKeepScreenOn(true);
        mVideoView.setOnPreparedListener(this);
        mVideoView.start();

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Buffering, please wait ...");
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                Log.e(CommonUtility.LOG_TAG, "onInfo " + what);
                if (what == 3) {
                    mProgressDialog.dismiss();
                }
                return true;
            }
        });
    }
}
