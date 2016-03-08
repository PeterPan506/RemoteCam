package com.ambarella.remotecam.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.ambarella.remotecam.R;
import com.ambarella.remotecam.RemoteCam;


public class SetupFragment extends Fragment {
    private IFragmentListener mListener;
    private RadioGroup mRadioConnectivity;
    private int mConnectivityType;
    private String mBTDeviceName;
    private String mWifiSsidName;
    
    private TextView mTextViewBT;
    private TextView mTextViewWifi;

    public SetupFragment() {
    }

    public SetupFragment setConnectivityType(int type) {
        mConnectivityType = type;
        return this;
    }

    public SetupFragment setBTDevice(String name) {
        mBTDeviceName = name;
        if (mTextViewBT != null)
            mTextViewBT.setText(name);
        return this;
    }

    public SetupFragment setWifiDevice(String name) {
        mWifiSsidName = name;
        if (mTextViewWifi != null)
            mTextViewWifi.setText(name);
        return this;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setup, container, false);

        /**
         * set selected Bluetooth device name
         */
        mTextViewBT = (TextView) view.findViewById(R.id.textViewBTSelectedDevice);
        mTextViewBT.setText(mBTDeviceName);

        mTextViewWifi = (TextView) view.findViewById(R.id.textViewWifiSelectedDevice);
        mTextViewWifi.setText(mWifiSsidName);

        ImageButton listButton = (ImageButton) view.findViewById(R.id.imageButtonWifiList);
        listButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mListener.onFragmentAction(IFragmentListener.ACTION_WIFI_LIST, null);            }
        });

        listButton = (ImageButton) view.findViewById(R.id.imageButtonBTList);
        listButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // show Bluetooth list
                if (mListener != null) {
                    BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
                    if (!bta.isEnabled()) {
                        mListener.onFragmentAction(
                                IFragmentListener.ACTION_BT_ENABLE, null);
                        return;
                    }

                    if (mConnectivityType == RemoteCam.CAM_CONNECTIVITY_BLE_WIFI)
                        mListener.onFragmentAction(IFragmentListener.ACTION_BLE_LIST, null);
                    else
                        mListener.onFragmentAction(IFragmentListener.ACTION_BT_LIST, null);
                }
            }
        });

        Button button = (Button) view.findViewById(R.id.buttonWakeup);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onFragmentAction(IFragmentListener.ACTION_BC_WAKEUP, null);
                }
            }
        });

        button = (Button) view.findViewById(R.id.buttonStandby);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onFragmentAction(IFragmentListener.ACTION_BC_STANDBY, null);
                }
            }
        });

        button = (Button) view.findViewById(R.id.buttonStartSession);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onFragmentAction(IFragmentListener.ACTION_BC_START_SESSION, null);
                }
            }
        });

        button = (Button) view.findViewById(R.id.buttonStopSession);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onFragmentAction(IFragmentListener.ACTION_BC_STOP_SESSION, null);
                }
            }
        });

        mRadioConnectivity = (RadioGroup) view.findViewById(R.id.radioConnectivity);
        switch (mConnectivityType) {
        case RemoteCam.CAM_CONNECTIVITY_WIFI_WIFI:
            mRadioConnectivity.check(R.id.radioButtonWiWi);
            break;
        case RemoteCam.CAM_CONNECTIVITY_BT_BT:
            mRadioConnectivity.check(R.id.radioButtonBtBt);
            break;
        case RemoteCam.CAM_CONNECTIVITY_BLE_WIFI:
            mRadioConnectivity.check(R.id.radioButtonBleWi);
            break;
        case RemoteCam.CAM_CONNECTIVITY_BT_WIFI:
            mRadioConnectivity.check(R.id.radioButtonBtWi);
            break;
        }
        mRadioConnectivity.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkID) {
                mConnectivityType = checkID;
                if (mListener != null) {
                    switch (checkID) {
                    case R.id.radioButtonBleWi:
                        mConnectivityType = RemoteCam.CAM_CONNECTIVITY_BLE_WIFI;
                        break;
                    case R.id.radioButtonWiWi:
                        mConnectivityType = RemoteCam.CAM_CONNECTIVITY_WIFI_WIFI;
                        break;
                    case R.id.radioButtonBtBt:
                        mConnectivityType= RemoteCam.CAM_CONNECTIVITY_BT_BT;
                        break;
                    case R.id.radioButtonBtWi:
                        mConnectivityType = RemoteCam.CAM_CONNECTIVITY_BT_WIFI;
                        break;
                    default:
                        mConnectivityType = RemoteCam.CAM_CONNECTIVITY_INVALID;
                    }
                    mListener.onFragmentAction(IFragmentListener.ACTION_CONNECTIVITY_SELECTED,
                            mConnectivityType);
                }
            }
        });

        RadioButton radioButton = (RadioButton) view.findViewById(R.id.radioButtonBtBt);
        radioButton.setVisibility(View.GONE);

        radioButton = (RadioButton) view.findViewById(R.id.radioButtonBtWi);
        radioButton.setVisibility(View.GONE);

        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            radioButton = (RadioButton) view.findViewById(R.id.radioButtonBleWi);
            radioButton.setVisibility(View.GONE);
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
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        mTextViewBT = null;
        mTextViewWifi = null;
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
