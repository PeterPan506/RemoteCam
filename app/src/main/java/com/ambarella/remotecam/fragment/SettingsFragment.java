package com.ambarella.remotecam.fragment;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.ambarella.remotecam.R;

public class SettingsFragment extends Fragment {
    private final static String TAG = "SETTINGS";
    
    private EditText mEditTextBitRate;
    private Button mButtonBitRate;

    private IFragmentListener mListener;
    private List<Group> mListGroup = new ArrayList<Group>();
    private SettingListAdapter mListAdapter = new SettingListAdapter();
    private Group mSelectedGroup;
    private boolean mInited = false;
    private int mIndex;
    ExpandableListView mListView;

    public void reset() {
        mInited = false;
        mListGroup.clear();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        mEditTextBitRate = (EditText) view.findViewById(R.id.editTextBitRate);
        mButtonBitRate = (Button) view.findViewById(R.id.buttonBitRate);
        mButtonBitRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String bitRate = mEditTextBitRate.getText().toString();
                if (!bitRate.equals(""))
                    mListener.onFragmentAction(
                            IFragmentListener.ACTION_BC_SET_BITRATE,
                            Integer.parseInt(bitRate));
            }
        });

        mListView = (ExpandableListView) view.findViewById(R.id.expandableListViewSettings);
        mListView.setAdapter(mListAdapter);
        
        if (!mInited) {
            refreshSettings();
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
                    + " must implement OnFragmentInteractionListener");
        }
        mListAdapter.setInflater((LayoutInflater)
                activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
    
    /**
     * start process to refresh the entire settings list
     */
    public void refreshSettings() {
        Log.e(TAG, "refresh settings");
        mListGroup.clear();
        mListener.onFragmentAction(IFragmentListener.ACTION_BC_GET_ALL_SETTINGS, null);        
    }
    
    /**
     * We got the master JSON about settings in @parser,
     * we can start to fetch each setting one by one
     */
    public void updateAllSettings(JSONObject parser) {
        mInited = true;
        mListAdapter.updateAllSettings(parser);
        
        // now get the setting options one by one
        mIndex = 0;
        mListener.onFragmentAction(
                IFragmentListener.ACTION_BC_GET_SETTING_OPTIONS, 
                mListGroup.get(mIndex).getHeader());
        
    }
    
    /**
     * get next setting options until we are done with all the settings
     */
    public void updateSettingOptions(JSONObject parser) {
        mListGroup.get(mIndex).updateSettingOptions(parser);
        
        if (++mIndex < mListGroup.size()) {
            // get next setting options
            mListener.onFragmentAction(
                    IFragmentListener.ACTION_BC_GET_SETTING_OPTIONS, 
                    mListGroup.get(mIndex).getHeader());
        } else {
            mListener.onFragmentAction(
                    IFragmentListener.ACTION_BC_GET_ALL_SETTINGS_DONE, null);
            mListAdapter.notifyDataSetChanged();
        }
    }
    
    public void updateSettingNotification(JSONObject parser) {
        mSelectedGroup.updateSettingNotification(parser);
    }
    
    private class SettingListAdapter extends BaseExpandableListAdapter {
        
        public LayoutInflater mInflater;
        
        public void setInflater(LayoutInflater inflater) {
            mInflater = inflater;
        }
        
        public void updateAllSettings(JSONObject parser) {
            try {
                JSONArray contents = parser.getJSONArray("param");
                Log.e(TAG, contents.length()+"");
                JSONObject settinglist = new JSONObject(contents.getJSONObject(0).toString());//潘俊威加的，未实现，会crash
                /*for (int i = 0; i < contents.length(); i++) {
                    mListGroup.add(new Group(contents.getJSONObject(i).toString()));
                }*/
            } catch (JSONException e) { 
                Log.e("UpdateAllSettings", e.getMessage());
            }
        }
     
        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return mListGroup.get(groupPosition).get(childPosition);
        }
     
        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }
     
        @Override
        public View getChildView(int groupPosition, final int childPosition,
                boolean isLastChild, View convertView, ViewGroup parent) {
            final Group group = mListGroup.get(groupPosition);
     
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.listview_setting_body, null);
            }
     
            //Log.e("EV", "get group (" + groupPosition + ")" + group.getHeader() + " " + convertView);
            group.getView(childPosition, (ViewGroup)convertView);
            return convertView;
        }
     
        @Override
        public int getChildrenCount(int groupPosition) {
            return mListGroup.get(groupPosition).size();
        }
     
        @Override
        public Object getGroup(int groupPosition) {
            return mListGroup.get(groupPosition).getHeader();
        }
     
        @Override
        public int getGroupCount() {
            return mListGroup.size();
        }
     
        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }
     
        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                View convertView, ViewGroup parent) {
            String headerTitle = (String) getGroup(groupPosition);
            //Log.e("GroupView", headerTitle + " " + groupPosition);
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.listview_setting_header, null);
            }
     
            TextView header = (TextView) convertView
                    .findViewById(R.id.textViewSettingHeader);
            header.setText(headerTitle);
     
            return convertView;
        }
     
        @Override
        public boolean hasStableIds() {
            return false;
        }
     
        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }        
    }
    
    @SuppressWarnings("serial")
    private class Group extends ArrayList<String> implements View.OnClickListener {
        public String mHeader;
        public ArrayList<RadioButton> mListView;
        public String mSelectedItem;
        public boolean mSettable;
        
        public Group(String descriptor) {
            super();
            descriptor = descriptor.replaceAll("[{}\"]", "");

            // parse the name
            int index = descriptor.indexOf(':');
            mHeader = descriptor.substring(0, index).trim();
            mSelectedItem = descriptor.substring(index+1, descriptor.length()).replace("\\/", "/");
            mListView = new ArrayList<RadioButton>();
        }
        
        public String getHeader() {
            return mHeader;
        }
        
        public void updateSettingOptions(JSONObject parser) {
            try {
                mSettable = parser.getString("permission").equals("settable");
                JSONArray options = parser.getJSONArray("options");
                for (int i = 0; i < options.length(); i++) {
                    String item = options.getString(i);
                    add(item);
                }
            } catch (JSONException e) {
                Log.e("Adapter", e.getMessage());
            }
        }
        
       public boolean add(String item) {
            return super.add(item);
        }
        
        public void generateViews() {
            for (String item : this) {
                RadioButton radioButton = new RadioButton(getActivity());
                radioButton.setText(item);
                radioButton.setOnClickListener(this);
                radioButton.setId(View.generateViewId());
                radioButton.setEnabled(mSettable);
                if (item.equals(mSelectedItem))
                    radioButton.toggle();
                mListView.add(radioButton);               
            }
        }

        public void getView(int position, ViewGroup parent) {
            if (mListView.isEmpty()) {
                generateViews();
            }
            
            View view = mListView.get(position);
            ViewGroup oldParent = (ViewGroup) view.getParent();
            if (oldParent != null)
                oldParent.removeAllViews();
            
            parent.removeAllViews();
            parent.addView(mListView.get(position));
        }

        public void updateSettingNotification(JSONObject parser) {
            try {
                if (parser.getInt("rval") == 0) {
                    if (parser.has("param"))
                            mSelectedItem = parser.getString("param");
                    for (RadioButton button : mListView) {
                        String item = button.getText().toString();
                        button.setChecked(item.equals(mSelectedItem));
                    }
                }
            } catch (JSONException e) {
                Log.e("Adapter", e.getMessage());
            }
        }
        
        @Override
        public void onClick(View view) {
            RadioButton button = (RadioButton) view;
            String target = button.getText().toString();

            if (!target.equals(mSelectedItem)) {

                mSelectedItem = target;
                mSelectedGroup = this;

                String cmd = "\"type\":\"" + mHeader + 
                        "\",\"param\":\"" + mSelectedItem + "\"";
                mListener.onFragmentAction(
                        IFragmentListener.ACTION_BC_SET_SETTING, cmd);
            }
        }
    }
}
