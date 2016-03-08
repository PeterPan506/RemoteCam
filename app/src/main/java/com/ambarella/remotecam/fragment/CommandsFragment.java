package com.ambarella.remotecam.fragment;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.ambarella.remotecam.R;


/**
 * A simple {@link Fragment} subclass.
 *
 */
public class CommandsFragment extends Fragment
    implements ViewTreeObserver.OnGlobalLayoutListener {
    private final static String TAG = "CommandsFrag";
    private final static String TEMPLATE_FILENAME = "AmbaJsonCmds.txt";
    private SharedPreferences mSavedPosi;
    private SharedPreferences.Editor mEditor;
    
    EditText mJsonCmd;
    int mSessionId;
    int mPosition;
    IFragmentListener mListener;
    Context mContext;

    static TextView mLogView;
    static CircularString mJsonLog = new CircularString(256);
    static ArrayList<CharSequence> mCmdTemplates;
    
    public void reset() {       
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_commands, container, false);
        mSavedPosi = mContext.getSharedPreferences("position_data", 0);
        mEditor = mSavedPosi.edit();

        mLogView = (TextView) view.findViewById(R.id.textViewJsonLog);
        mLogView.setMovementMethod(new ScrollingMovementMethod());
        mLogView.getViewTreeObserver().addOnGlobalLayoutListener(this);

        mJsonCmd = (EditText) view.findViewById(R.id.editTextCommand);
        mJsonCmd.setText("json_message");

        ImageView cmdsView = (ImageView) view.findViewById(R.id.imageViewCmds);
        cmdsView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showTemplate();
            }
        });

        ImageView sendView = (ImageView) view.findViewById(R.id.buttonJsonSend);
        sendView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //showTemplate();
                if (mListener != null) {
                    mListener.onFragmentAction(IFragmentListener.ACTION_BC_SEND_COMMAND,
                            mJsonCmd.getText().toString());
                }
            }
        });
        
        /*Button button = (Button) view.findViewById(R.id.buttonJsonSend);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onFragmentAction(IFragmentListener.ACTION_BC_SEND_COMMAND,
                            mJsonCmd.getText().toString());
                }
            }
        });*/
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        mContext = activity;
        super.onAttach(activity);
        try {
            mListener = (IFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        mLogView = null;
        mListener = null;
        super.onDetach();
    }

    @Override
    public void onGlobalLayout() {
        showText();
        mLogView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    public void setSessionId(int id) {
        mSessionId = id;
    }
    
    public static boolean addLog(String log) {
        mJsonLog.add(log);
        showText();
        return true;
    }

    public void showTemplate() {
        if (!getTemplates()) {
            showTemplateWarning();
            return;
        }
        mPosition = mSavedPosi.getInt("mPosition", 0);

        final CharSequence[] items = Arrays.copyOf(
                mCmdTemplates.toArray(), mCmdTemplates.size(), CharSequence[].class);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Pick a command");
        builder.setPositiveButton("OK", null);
        builder.setSingleChoiceItems(items, mPosition, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String cmd = items[which].toString();
                cmd = cmd.replace("*", Integer.toString(mSessionId));
                mJsonCmd.setText(cmd);
                mEditor.putInt("mPosition",which);
                mEditor.commit();
            }
        });
        builder.show();
    }

    public static void showText() {
        //Log.e(TAG, "showText enter");
        if (mLogView == null || mLogView.getVisibility() != View.VISIBLE)
            return;

        mLogView.setText(Html.fromHtml(mJsonLog.toString()));
        final int scrollAmount = mLogView.getLayout().getLineTop(mLogView.getLineCount())
                - mLogView.getHeight();
        if (scrollAmount > 0)
            mLogView.scrollTo(0, scrollAmount);
    }

    private boolean getTemplates() {
        if (mCmdTemplates == null) {
            String path = Environment.getExternalStorageDirectory()
                + "/" + TEMPLATE_FILENAME;

            FileInputStream fstream;
            try {
                fstream = new FileInputStream(path);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Cannot open " + path);
                return false;
            }

            mCmdTemplates = new ArrayList<CharSequence>();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(fstream));
            String line;
            try {
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && line.charAt(0) != '#')
                        mCmdTemplates.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private void showTemplateWarning() {
        new AlertDialog.Builder(mContext)
            .setTitle("Warning")
            .setMessage("Template file not found!! Please put file \""
                    + TEMPLATE_FILENAME + "\" in SD-card root direcory. "
                    + "The file syntax is as follows:\n\n"
                    + "\t# Anyline starts with hash sign is ignored\n"
                    + "\t# Empty line is also ignored\n"
                    + "\t# One line for each JSON command\n"
                    + "\t# Use \'*\' as wildcard for JSON session ID\n\n"
                    + "\t# start a session\n"
                    + "\t{\"token\":0,\"msg_id\":257}\n\n"
                    + "\t# stop the session\n"
                    + "\t{\"token\":*,\"msg_id\":258}\n")
            .setPositiveButton("OK", null)
            .show();
    }

    static class CircularString {
        private String[] mArray;
        int     mIndex;
        int     mSize;
        boolean mFull;
        int     mLineCount;

        int     mDisplayIndex;
        String  mDisplayString = "";

        public CircularString(int size) {
            mIndex = 0;
            mLineCount = 0;
            mFull = false;
            mSize = size;
            mArray = new String[mSize];
        }

        public synchronized void add(String log) {
            mArray[mIndex] = String.format("%d:\t%s", ++mLineCount, log);
            if (++mIndex == mSize) {
                mIndex = 0;
                mFull = true;
            }
        }

        public synchronized String toString() {
            if (mDisplayIndex == mIndex) {
                return mDisplayString;
            }

            StringBuilder sb = new StringBuilder(64*1024);
            int i;

            if (mFull) {
                for (i = mIndex; i < mSize; i++)
                    sb.append(mArray[i]);
            }

            for (i = 0; i < mIndex; i++)
                sb.append(mArray[i]);

            mDisplayString = sb.toString();
            mDisplayIndex = mIndex;
            return mDisplayString;
        }
    }
}
