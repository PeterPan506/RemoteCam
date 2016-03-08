package com.ambarella.remotecam.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.ambarella.remotecam.R;
import com.ambarella.remotecam.RemoteCam;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MediaFragment extends Fragment implements
        ListView.OnItemClickListener, ListView.OnCreateContextMenuListener {
    private final static String TAG = "MediaFrag";
    private final static String UPDATE_FILENAME = "QHSysFW.zip";

    private String mPWD;
    private String mSwitch = "record";
    private String mHome;
    private int mSwflag = 1;
    private DentryAdapter mAdapter;
    private IFragmentListener mListener;
    private ListView mListView;
    private TextView mTextViewPath;
    private RemoteCam mRemoteCam;

    public MediaFragment() {
        reset();
    }

    public void reset() {
        mPWD = null;
        mHome = null;
        mAdapter = null;
        mSwitch ="record";
        mSwflag = 1;
    }

    public void setRemoteCam(RemoteCam cam) {mRemoteCam = cam;}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_media, container, false);

        mTextViewPath = (TextView) view.findViewById(R.id.textViewDentryPath);

        // Setup the list view
        mListView = (ListView) view.findViewById(R.id.listViewDentryName);
        mListView.setOnItemClickListener(this);
        mListView.setOnCreateContextMenuListener(this);
        registerForContextMenu(mListView);

        if (mAdapter == null) {
            mPWD = mHome = mRemoteCam.sdCardDirectory();
            listDirContents(mSwitch);
        } else {
            showDirContents();
        }

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
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
        super.onDetach();
        mListener = null;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (null != mListener) {
            Model item = (Model) parent.getItemAtPosition(position);
            if (item.isDirectory()) {
                mPWD += item.getName() + "/";
                listDirContents(mSwitch);
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        Model model = mAdapter.getItem(info.position);
        if (model.isDirectory())
            return;

        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.menu_dentry, menu);
        String name = model.getName();
        int len = name.length();
        String surfix = name.substring(len-3, len).toLowerCase();
        if (!surfix.equals("jpg") && !surfix.equals("mp4")) {
            menu.removeItem(R.id.item_dentry_info);
            menu.removeItem(R.id.item_dentry_view);
            menu.removeItem(R.id.item_dentry_thumb);
        }
        if (!surfix.equals("bin")) {
            menu.removeItem(R.id.item_dentry_burning_fw);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Model model = mAdapter.getItem(info.position);
        String path = mPWD + model.getName();
        String file_id = model.getUid()+"";
        String file_name = model.getName();
        switch (item.getItemId()) {
            case R.id.item_dentry_delete:
                Log.e(TAG, model.getUid()+"");
                mListener.onFragmentAction(IFragmentListener.ACTION_FS_DELETE, model.getUid()+"");
                return true;
            case R.id.item_dentry_download:
                mListener.onFragmentAction(IFragmentListener.ACTION_FS_DOWNLOAD, file_id);
                return true;
            case R.id.item_dentry_info:
                new AlertDialog.Builder(getActivity())
                        .setTitle("info")
                        .setMessage("时长："+model.getDuration()
                                +"s\n分辨率："+model.getReso()
                                +"\n幀率："+model.getFps()+"fps")
                        .setPositiveButton("OK", null)
                        .show();
                //mListener.onFragmentAction(IFragmentListener.ACTION_FS_INFO, path);
                return true;
            case R.id.item_dentry_view:
                mListener.onFragmentAction(IFragmentListener.ACTION_FS_VIEW, file_name);
                return true;
            case R.id.item_dentry_set_RO:
                mListener.onFragmentAction(IFragmentListener.ACTION_FS_SET_RO, file_id);
                return true;
            case R.id.item_dentry_set_WR:
                mListener.onFragmentAction(IFragmentListener.ACTION_FS_SET_WR, file_id);
                return true;
            case R.id.item_dentry_burning_fw:
                mListener.onFragmentAction(IFragmentListener.ACTION_FS_BURN_FW, path);
                break;
            case R.id.item_dentry_thumb:
                mListener.onFragmentAction(IFragmentListener.ACTION_FS_GET_THUMB, file_id);
                break;
        }
        return super.onContextItemSelected(item);
    }

    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyText instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    public void refreshDirContents() {
        listDirContents(mSwitch);
    }

    public void goParentDir() {
        /*int index = mPWD.lastIndexOf('/');
        if (index > 0) {
            index = mPWD.lastIndexOf('/', index - 1);
            mPWD = mPWD.substring(0, index + 1);
            listDirContents(mPWD);
        }*/
        mSwflag++;
        switch (mSwflag){
            case 1:
                mSwitch = "record";
                break;
            case 2:
                mSwitch = "event";
                break;
            case 3:
                mSwitch = "photo";
                mSwflag = 0;
                break;
            default:
                mSwflag = 0;
                break;
        }
        listDirContents(mSwitch);
    }
    
    public void formatSD() {
        mListener.onFragmentAction(IFragmentListener.ACTION_FS_FORMAT_SD,
                mHome.equals("/tmp/fuse_d/") ? "D:" : "C:");
    }
    
    public void showSD() {
        mPWD = mHome;
        refreshDirContents();
    }
    
    public String getPWD() {
        return mPWD;
    }
    
    private void listDirContents(String type) {
        if (type != null)
            mListener.onFragmentAction(IFragmentListener.ACTION_FS_LS, type);
    }

    private void showDirContents() {
        mTextViewPath.setText("Directory: " + mSwitch);
        mListView.setAdapter(mAdapter);
    }

    public void updateDirContents(JSONObject parser) {
        ArrayList<Model> models = new ArrayList<Model>();

        try {
            JSONArray contents = parser.getJSONArray("list");

            for (int i = 0; i < contents.length(); i++) {
                String mJsonString = contents.getJSONObject(i).toString();
                Log.e(TAG, mJsonString);
                models.add(new Model(mJsonString));
                //models.add(new Model(contents.getJSONObject(i).toString()));
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }

        mAdapter = new DentryAdapter(models);
        showDirContents();
    }

    private class Model {
        private boolean isDirectory;
        private int size;
        private int duration;
        private int fps;
        private int uid;
        private String name;
        private String time;
        private String reso;

        public Model(String descriptor) {
            try {
                JSONObject parser = new JSONObject(descriptor);
                name = parser.getString("name");
                size = parser.getInt("size");
                time = parser.getString("ts");
                uid = parser.getInt("uid");
                if(mSwflag == 1 || mSwflag == 2){
                    duration = parser.getInt("time");
                    fps = parser.getInt("fps");
                    reso = parser.getString("reso");
                }

            }catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            if(mSwflag == 1){
                name = name.substring(6);
            }
            size = size/1024;
            isDirectory = false;

        }

        /*
        public Model(String descriptor) {
            descriptor = descriptor.replaceAll("[{}\"]", "");
            Log.e(TAG, descriptor);

            // parse the name
            //int index = descriptor.indexOf(':');
            int index = descriptor.indexOf("36001");
            Log.e(TAG, index+"");
            //name = descriptor.substring(0, index).trim();
            name = descriptor.substring(index+7, index+28).trim();

            // figure out if this is file or directory
            isDirectory = name.endsWith("/");
            if (isDirectory)
                name = name.substring(0, name.length()-2);

            if (descriptor.contains("|")) {
                // get the size
                descriptor = descriptor.substring(index+1).trim();
                index = descriptor.indexOf(" ");
                size = Integer.parseInt(descriptor.substring(0, index));
                // get the time
                time = descriptor.substring(descriptor.indexOf('|')+1).trim();
            } else if (descriptor.contains("bytes")) {
                index = descriptor.indexOf(" ");
                size = Integer.parseInt(descriptor.substring(0, index));
                time = null;
            } else {
                size = -1;
                time = descriptor.substring(index+1).trim();
            }
        }*/

        public String getName() {
            return name;
        }
        public int getSize() {
            return size;
        }
        public String getTime() {
            return time;
        }
        public boolean isDirectory() {
            return isDirectory;
        }
        public String getReso() {
            return reso;
        }
        public int  getDuration(){
            return duration;
        }
        public int  getFps(){
            return fps;
        }
        public int  getUid(){
            return uid;
        }


    }

    private class DentryAdapter extends ArrayAdapter<Model> {
        final private ArrayList<Model> mArrayList;

        public DentryAdapter(ArrayList<Model> arrayList) {
            super(getActivity(), R.layout.listview_dentry, arrayList);
            mArrayList = arrayList;
        }//布局文件

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getActivity()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.listview_dentry, parent, false);
            Model model = mArrayList.get(position);

            TextView nameView = (TextView) view.findViewById(R.id.textViewDentryName);
            nameView.setText(model.getName());

            TextView timeView = (TextView) view.findViewById(R.id.textViewDentryTime);
            timeView.setText(model.getTime());

            ImageView imageView = (ImageView) view.findViewById(R.id.imageViewDentryType);
            TextView sizeView = (TextView) view.findViewById(R.id.textViewDentrySize);
            if (model.isDirectory()) {
                imageView.setImageResource(R.drawable.ic_folder);
                sizeView.setVisibility(View.INVISIBLE);
            } else {
                imageView.setImageResource(R.drawable.ic_file);
                int size = model.getSize();
                if (size > 0)
                    sizeView.setText(Integer.toString(model.getSize()) + " bytes");
                else 
                    sizeView.setVisibility(View.INVISIBLE);
            }

            return view;
        }
    }
}
