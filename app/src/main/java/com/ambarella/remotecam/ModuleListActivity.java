package com.ambarella.remotecam;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import android.provider.Settings;
import android.support.v4.widget.DrawerLayout;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.ambarella.remotecam.connectivity.CmdChannel;
import com.ambarella.remotecam.connectivity.IChannelListener;
import com.ambarella.remotecam.fragment.BLESelectFragment;
import com.ambarella.remotecam.fragment.BTSelectFragment;
import com.ambarella.remotecam.fragment.CameraFragment;
import com.ambarella.remotecam.fragment.CommandsFragment;
import com.ambarella.remotecam.fragment.IFragmentListener;
import com.ambarella.remotecam.fragment.MediaFragment;
import com.ambarella.remotecam.fragment.SettingsFragment;
import com.ambarella.remotecam.fragment.SetupFragment;
import com.ipaulpro.afilechooser.FileChooserActivity;


public class ModuleListActivity extends Activity
        implements ModuleListFragment.Callbacks, IFragmentListener, IChannelListener {
    private static final String TAG = "ModuleList";
    private static final int REQUEST_CODE_UPLOAD = 6384;
    private static final int REQUEST_CODE_WIFI_SETTINGS = 6385;

    final static String KEY_CONNECTIVITY_TYPE = "connectivity_type";
    final static String KEY_SELECTED_MODULE = "selected_module";
    final static String KEY_SELECTED_BT_DEVICE_NAME = "selected_bt_device_name";
    final static String KEY_SELECTED_BT_DEVICE_ADDR = "selected_bt_device_addr";

    private int mFragContainerId;
    private int mSessionId;

    private int mConnectivityType;
    private SharedPreferences mPref;
    private String mBTDeviceName;
    private String mBTDeviceAddr;
    private String mWifiSsidName;
    private String mGetFileName;
    private String mPutFileName;
    private RemoteCam mRemoteCam;
    private ListView mListViewDrawer;
    private DrawerLayout mDrawerLayout;
    private ProgressDialog mProgressDialog;
    private AlertDialog mAlertDialog;
    private boolean mIsPreview;

    private WifiStatusReceiver mWifiReceiver = new WifiStatusReceiver();

    static private CameraFragment mCameraFrag = new CameraFragment();
    static private SetupFragment mSetupFrag = new SetupFragment();
    static private MediaFragment mMediaFrag = new MediaFragment();
    static private SettingsFragment mSettingsFrag = new SettingsFragment();
    static private CommandsFragment mCommandsFrag = new CommandsFragment();

    static private int mSelectedModule = ModuleContent.MODULE_POS_SETUP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module);

        mPref = getPreferences(Context.MODE_PRIVATE);
        getPrefs(mPref);

        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mRemoteCam = new RemoteCam(this);
        mRemoteCam.setChannelListener(this)
                .setBtDeviceAddr(mBTDeviceAddr)
                .setConnectivity(mConnectivityType)
                .setWifiInfo(wifiManager.getConnectionInfo().getSSID().replace("\"", ""), getWifiIpAddr())
                        //.setWifiMac(wifiManager.getConnectionInfo().getMacAddress().replaceAll(":","").trim().toUpperCase())
                .setWifiMac(wifiManager.getConnectionInfo().getMacAddress().trim().toUpperCase());
        mMediaFrag.setRemoteCam(mRemoteCam);

        if (findViewById(R.id.module_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mFragContainerId = R.id.module_detail_container;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ModuleListFragment frag = ((ModuleListFragment) getFragmentManager()
                    .findFragmentById(R.id.module_list));

            frag.setActivateOnItemClick(true);
            frag.getListView().setItemChecked(mSelectedModule, true);
        } else {
            mFragContainerId = R.id.onepan_content;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            mDrawerLayout = (DrawerLayout) findViewById(R.id.onepane_layout);
            mListViewDrawer = (ListView) findViewById(R.id.onepane_drawer);
            mListViewDrawer.setAdapter(new ArrayAdapter<ModuleContent.ModuleItem>(
                    this,
                    android.R.layout.simple_list_item_activated_1,
                    android.R.id.text1,
                    ModuleContent.ITEMS));
            mListViewDrawer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    mDrawerLayout.closeDrawers();
                    onItemSelected((int) id);
                }
            });
        }
        onItemSelected(mSelectedModule);
    }

    @Override
    protected void onResume() {
        IntentFilter filter = new IntentFilter();
        //filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        //filter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mWifiReceiver, filter);
        super.onResume();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mWifiReceiver);
        putPrefs(mPref);
        super.onPause();
    }

    private void getPrefs(SharedPreferences preferences) {
        mConnectivityType = mPref.getInt(KEY_CONNECTIVITY_TYPE, RemoteCam.CAM_CONNECTIVITY_WIFI_WIFI);
        //mSelectedModule = mPref.getInt(KEY_SELECTED_MODULE, ModuleContent.MODULE_POS_SETUP);
        mBTDeviceName = mPref.getString(KEY_SELECTED_BT_DEVICE_NAME, "");
        mBTDeviceAddr = mPref.getString(KEY_SELECTED_BT_DEVICE_ADDR, "00:00:00:00:00:00");
    }

    private void putPrefs(SharedPreferences preferences) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KEY_CONNECTIVITY_TYPE, mConnectivityType);
        editor.putInt(KEY_SELECTED_MODULE, mSelectedModule);
        editor.putString(KEY_SELECTED_BT_DEVICE_NAME, mBTDeviceName);
        editor.putString(KEY_SELECTED_BT_DEVICE_ADDR, mBTDeviceAddr);
        editor.commit();
    }

    private class WifiStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                String ssid;
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info.getType() != ConnectivityManager.TYPE_WIFI)
                    return;
                Log.e(TAG, "wifi intent " + info.toString());
                if (info.getState() == State.CONNECTED) {
                    ssid = info.getExtraInfo().replaceAll("\"", "");
                } else {
                    ssid = "Invalid";
                }
                if (!ssid.equals(mWifiSsidName)) {
                    mWifiSsidName = ssid;
                    resetRemoteCamera();
                    mSetupFrag.setWifiDevice(mWifiSsidName);
                    mRemoteCam.setWifiInfo(mWifiSsidName, getWifiIpAddr());
                }
            }
        }
    }

    private String getWifiIpAddr() {
        WifiManager mgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        int ip = mgr.getConnectionInfo().getIpAddress();
        return String.format("%d.%d.%d.%d",
                (ip & 0xFF), (ip >> 8 & 0xFF), (ip >> 16 & 0xFF), ip >> 24);
    }

    @Override
    public void onBackPressed() {
        //mRemoteCam.stopSession();//退出时发送stop session
        Log.e(TAG, "kill the process to force fresh launch next time");
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /**
     * Callback method from {@link ModuleListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(int id) {
        mSelectedModule = id;
        switch (id) {
            case ModuleContent.MODULE_POS_SETUP:
                showSetupFragment();
                break;
            case ModuleContent.MODULE_POS_CAMERA:
                showCameraFragment();
                break;
            case ModuleContent.MODULE_POS_COMMANDS:
                showCommandsFragment();
                break;
            case ModuleContent.MODULE_POS_MEDIA:
                showMediaFragment();
                break;
            case ModuleContent.MODULE_POS_SETTINGS:
                showSettingsFragment();
                break;
        }
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int menuId;
        switch (mSelectedModule) {
            case ModuleContent.MODULE_POS_MEDIA:
                menuId = R.menu.menu_action_media;
                break;
            case ModuleContent.MODULE_POS_SETUP:
                menuId = R.menu.menu_action_setup;
                break;
            case ModuleContent.MODULE_POS_SETTINGS:
                menuId = R.menu.menu_action_settings;
                break;
            default:
                return false;
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(menuId, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.itemParentDir:
                mMediaFrag.goParentDir();
                return true;
            case R.id.itemMediaRefresh:
                mMediaFrag.refreshDirContents();
                return true;
            case R.id.itemFormatSD:
                mMediaFrag.formatSD();
                return true;
            case R.id.itemUpload:
//                Intent intent = new Intent(this, FileChooserActivity.class);
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try{
                    startActivityForResult(intent, REQUEST_CODE_UPLOAD);
                }catch (ActivityNotFoundException e){
                    e.printStackTrace();
                    new AlertDialog.Builder(this)
                            .setTitle("Warning")
                            .setMessage("You don't have any compatible file explorer installed in your device. " +
                                    "Please install one first.")
                            .setPositiveButton("OK", null)
                            .show();
                }

                return true;

            case R.id.itemDevInfo:
                mRemoteCam.getMediaInfo();
                return true;
            case R.id.itemBatteryLevel:
                mRemoteCam.getBatteryLevel();
                return true;

            case R.id.itemSettingsRefresh:
                mSettingsFrag.refreshSettings();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_UPLOAD:
                // If the file selection was successful
                if (resultCode == RESULT_OK) {
                    if (data != null) {
                        // Get the URI of the selected file
                        final Uri uri = data.getData();
                        final String srcPath = uri.getPath();
                        final String fileName = srcPath.substring(srcPath.lastIndexOf('/') + 1);
                        //mPutFileName = mMediaFrag.getPWD() + fileName;
                        mPutFileName = fileName;
                        mRemoteCam.putFile(srcPath, mPutFileName);
                    }
                }
                break;
            case REQUEST_CODE_WIFI_SETTINGS:
                dismissDialog();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * reset RemoteCam due to:
     * 1. Lost of connections.
     * 2. Use selected a different camera
     */
    private void resetRemoteCamera() {
        mRemoteCam.reset();
        mCameraFrag.reset();
        mMediaFrag.reset();
        mCommandsFrag.reset();
        mSettingsFrag.reset();
    }

    /**
     * IFragmentListener
     */
    public void onFragmentAction(int type, final Object param, Integer... array) {
        Intent intent;
        switch (type) {
            case IFragmentListener.ACTION_CONNECTIVITY_SELECTED:
                mConnectivityType = (Integer) param;
                resetRemoteCamera();
                mRemoteCam.setConnectivity(mConnectivityType);
                break;
            case IFragmentListener.ACTION_BT_LIST:
                getFragmentManager().beginTransaction()
                        .replace(mFragContainerId, new BTSelectFragment())
                        .commit();
                break;
            case IFragmentListener.ACTION_BLE_LIST:
                getFragmentManager().beginTransaction()
                        .replace(mFragContainerId, new BLESelectFragment())
                        .commit();
                break;
            case IFragmentListener.ACTION_WIFI_LIST:
                intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                startActivityForResult(intent, REQUEST_CODE_WIFI_SETTINGS);
                break;
            case IFragmentListener.ACTION_BT_SELECTED:
                String device[] = (String[]) param;
                if (!mBTDeviceAddr.equals(device[1])) {
                    mBTDeviceName = device[0];
                    mBTDeviceAddr = device[1];
                    resetRemoteCamera();
                    mRemoteCam.setBtDeviceAddr(mBTDeviceAddr);
                }
                //fall through
            case IFragmentListener.ACTION_BT_CANCEL:
                showSetupFragment();
                break;
            case IFragmentListener.ACTION_BT_ENABLE:
                startBluetoothSettings();
                break;
            case IFragmentListener.ACTION_BC_WAKEUP:
                mRemoteCam.wakeUp();
                break;
            case IFragmentListener.ACTION_BC_STANDBY:
                mRemoteCam.standBy();
                break;
            case IFragmentListener.ACTION_BC_START_SESSION:
                mRemoteCam.startSession();
                break;
            case IFragmentListener.ACTION_BC_STOP_SESSION:
                mRemoteCam.stopSession();
                break;
            case IFragmentListener.ACTION_BC_SEND_COMMAND:
                mRemoteCam.sendCommand((String) param);
                break;
            case IFragmentListener.ACTION_BC_GET_ALL_SETTINGS:
                showWaitDialog("Fetching Settings Info");
                mRemoteCam.getAllSettings();
                break;
            case IFragmentListener.ACTION_BC_GET_ALL_SETTINGS_DONE:
                dismissDialog();
                break;
            case IFragmentListener.ACTION_BC_GET_SETTING_OPTIONS:
                mRemoteCam.getSettingOptions((String) param);
                break;
            case IFragmentListener.ACTION_BC_SET_SETTING:
                mRemoteCam.setSetting((String) param);
                break;
            case IFragmentListener.ACTION_BC_SET_BITRATE:
                mRemoteCam.setBitRate((Integer) param);
                break;

            case IFragmentListener.ACTION_FS_BURN_FW:
                mRemoteCam.burnFW((String) param);
                break;
            case IFragmentListener.ACTION_FS_GET_FILE_INFO:
                mRemoteCam.getMediaInfo();
                break;
            case IFragmentListener.ACTION_FS_FORMAT_SD:
                new AlertDialog.Builder(this)
                        .setTitle("Warning")
                        .setMessage("Are you sure to format SD card?")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mRemoteCam.formatSD((String) param);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .show();
                break;
            case IFragmentListener.ACTION_FS_LS:
                mRemoteCam.listDir((String) param);
                break;
            case IFragmentListener.ACTION_FS_DELETE:
                mRemoteCam.deleteFile((String) param);
                break;
            case IFragmentListener.ACTION_FS_DOWNLOAD:
                mGetFileName = (String) param;
                mRemoteCam.getFile(mGetFileName);
                break;
            case IFragmentListener.ACTION_FS_INFO:
                /*new AlertDialog.Builder(this)
                        .setTitle("info")
                        .setMessage("File information")
                        .setPositiveButton("OK", null)
                        .show();*/
                //mRemoteCam.getInfo((String) param);
                break;
            case IFragmentListener.ACTION_FS_SET_RO:
                mRemoteCam.setMediaAttribute((String) param, 0);
                break;
            case IFragmentListener.ACTION_FS_SET_WR:
                mRemoteCam.setMediaAttribute((String) param, 1);
                break;
            case IFragmentListener.ACTION_FS_GET_THUMB:
                mRemoteCam.getThumb((String) param);
                break;
            case IFragmentListener.ACTION_FS_VIEW:
                String path = (String) param;
                if (path.endsWith(".jpg")) {
                    mIsPreview = true;
                    mRemoteCam.getFile(path);
                } else {
                    String uri = mRemoteCam.streamFile(path);
                    intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(uri), "video/mp4");
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                        new AlertDialog.Builder(this)
                                .setTitle("Warning")
                                .setMessage("You don't have any compatible video player installed in your device. " +
                                        "Please install one (such as RTSP player) first.")
                                .setPositiveButton("OK", null)
                                .show();
                    }
                    /*
                    String url = mRemoteCam.streamFile((String)param);
                    Intent intent = new Intent(this, StreamPlayerActivity.class);
                    intent.putExtra(CommonUtility.PLAYER_EXTRA, url);
                    startActivity(intent);
                    */
                }
                break;

            case IFragmentListener.ACTION_VF_START:
                mRemoteCam.startVF();
                break;
            case IFragmentListener.ACTION_VF_STOP:
                mRemoteCam.stopVF();
                break;
            case IFragmentListener.ACTION_PLAYER_START:
                mRemoteCam.startLiveStream();
                break;
            case IFragmentListener.ACTION_PLAYER_STOP:
                mRemoteCam.stopLiveStream();
                mCameraFrag.stopStreamView();
                break;
            case IFragmentListener.ACTION_RECORD_START:
                mRemoteCam.startRecord();
                mCameraFrag.startRecord();
                break;
            case IFragmentListener.ACTION_RECORD_STOP:
                mRemoteCam.stopRecord();
                mCameraFrag.stopRecord();
                break;
            case IFragmentListener.ACTION_RECORD_TIME:
                mRemoteCam.getRecordTime();
                break;
            case IFragmentListener.ACTION_PHOTO_START:
                mRemoteCam.takePhoto();
                break;
            //case IFragmentListener.ACTION_PHOTO_STOP:
            //    mRemoteCam.stopPhoto();
            //    break;
            case IFragmentListener.ACTION_SYNC_TIME:
                mRemoteCam.syncTime();
                break;
            case IFragmentListener.ACTION_MIC_ON:
                mRemoteCam.startMic();
                break;
            case IFragmentListener.ACTION_MIC_OFF:
                mRemoteCam.stopMic();
                break;
            case IFragmentListener.ACTION_FORCE_SPLIT:
                mRemoteCam.forceSplit();
                break;
            case IFragmentListener.ACTION_GET_ZOOM_INFO:
                mRemoteCam.getZoomInfo((String) param);
                break;
            case IFragmentListener.ACTION_SET_ZOOM:
                mRemoteCam.setZoom((String) param, array[0]);
                break;
        }
    }

    /**
     * IChannelListener
     */
    public void onChannelEvent(final int type, final Object param, final String... array) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (type & IChannelListener.MSG_MASK) {
                    case IChannelListener.CMD_CHANNEL_MSG:
                        handleCmdChannelEvent(type, param, array);
                        return;
                    case IChannelListener.DATA_CHANNEL_MSG:
                        handleDataChannelEvent(type, param);
                        return;
                    case IChannelListener.STREAM_CHANNEL_MSG:
                        handleStreamChannelEvent(type, param);
                        return;
                    default:
                        break;
                }
            }
        });
    }

    private void handleCmdChannelEvent(int type, Object param, String... array) {
        if (type >= 80) {
            handleCmdChannelError(type, param);
            return;
        }

        switch (type) {
            case IChannelListener.CMD_CHANNEL_EVENT_START_SESSION:
                Toast.makeText(ModuleListActivity.this, "Connect success", Toast.LENGTH_SHORT).show();
                mSessionId = (Integer) param;
                mCommandsFrag.setSessionId(mSessionId);
                break;
            case IChannelListener.DATA_CHANNEL_EVENT_START_UPGRADE:
                Toast.makeText(ModuleListActivity.this, "Upload complete, Starting upgrade", Toast.LENGTH_SHORT).show();
                //startUpgrade
                break;  //潘俊威加的升级固件指令
            case IChannelListener.CMD_CHANNEL_EVENT_SHOW_ALERT:
                showAlertDialog("Warning", (String) param);
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_LOG:
                CommandsFragment.addLog((String) param);
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_LS:
                dismissDialog();
                mMediaFrag.updateDirContents((JSONObject) param);
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_SET_ATTRIBUTE:
                showAlertDialog("Info",
                        ((int) param != 0) ? "Set_Attribute failed" : "Set_Attribute OK");
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_FORMAT_SD:
                mMediaFrag.showSD();
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_DEL:
                mMediaFrag.refreshDirContents();
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_BATTERY_LEVEL:
            case IChannelListener.CMD_CHANNEL_EVENT_GET_INFO:
            case IChannelListener.CMD_CHANNEL_EVENT_GET_DEVINFO:
                showAlertDialog("Info", (String) param);
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_RESETVF:
                mCameraFrag.onVFReset();
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_STOP_VF:
                mCameraFrag.onVFStopped();
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_RECORD_TIME:
                mCameraFrag.upDateRecordTime((String) param);
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_SET_ZOOM:
                mCameraFrag.setZoomDone((Integer) param);
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_GET_ZOOM_INFO:
                mCameraFrag.setZoomInfo((String) param, array[0]);
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_GET_ALL_SETTINGS:
                JSONObject parser = (JSONObject) param;
                try {
                    if (parser.getInt("rval") < 0)
                        showAlertDialog("Warning", "Setting is not support by remote camera !");
                    else
                        mSettingsFrag.updateAllSettings(parser);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_GET_OPTIONS:
                //mSettingsFrag.updateSettingOptions((JSONObject)param);
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_SET_SETTING:
                mSettingsFrag.updateSettingNotification((JSONObject) param);
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_START_CONNECT:
                showWaitDialog("Connecting to Remote Camera");
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_START_LS:
                showWaitDialog("Fetching Directory Info");
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_WAKEUP_START:
                showWaitDialog("Waking up the Remote Camera");
                break;
            case IChannelListener.CMD_CHANNEL_EVENT_CONNECTED:
            case IChannelListener.CMD_CHANNEL_EVENT_WAKEUP_OK:
                dismissDialog();
                break;
        }
    }

    private void handleCmdChannelError(int type, Object param) {
        switch (type) {
            case IChannelListener.CMD_CHANNEL_ERROR_INVALID_TOKEN:
                showAlertDialog("Error", "Invalid Session! Please start session first!");
                break;
            case IChannelListener.CMD_CHANNEL_SESSION_START_FAIL:
                showAlertDialog("Notice", "Session is held by others or you have already started it!");
                break;
            case IChannelListener.CMD_CHANNEL_ERROR_TIMEOUT:
                showAlertDialog("Error", "Timeout! No response from Remote Camera!");
                break;
            case IChannelListener.CMD_CHANNEL_ERROR_BLE_INVALID_ADDR:
                showAlertDialog("Error", "Invalid bluetooth device");
                break;
            case IChannelListener.CMD_CHANNEL_ERROR_BLE_DISABLED:
                startBluetoothSettings();
                break;
            case IChannelListener.CMD_CHANNEL_ERROR_BROKEN_CHANNEL:
                showAlertDialog("Error", "Lost connection with Remote Camera!");
                resetRemoteCamera();
                break;
            case IChannelListener.CMD_CHANNEL_ERROR_CONNECT:
                showAlertDialog("Error",
                        "Cannot connect to the Camera. \n" +
                                "Please make sure the selected camera is on. \n" +
                                "If problem persists, please reboot both camera and this device.");
                break;
            case IChannelListener.CMD_CHANNEL_ERROR_WAKEUP:
                showAlertDialog("Error", "Cannot wakeup the Remote Camera");
                break;
        }
    }

    private void handleDataChannelEvent(int type, Object param) {
        switch (type) {
            case IChannelListener.DATA_CHANNEL_EVENT_GET_START:
                String str = mIsPreview ? "Please wait ..." : "Downloading ,,,";
                showProgressDialog(str,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface Dialog, int which) {
                                mRemoteCam.cancelGetFile(mGetFileName);
                            }
                        });
                break;
            case IChannelListener.DATA_CHANNEL_EVENT_GET_PROGRESS:
                mProgressDialog.setProgress((Integer) param);
                break;
            case IChannelListener.DATA_CHANNEL_EVENT_GET_FINISH:
                String path = (String) param;
                if (!mIsPreview) {
                    showAlertDialog("Info", "Downloaded to " + path);
                    mGetFileName = null;
                } else {
                    dismissDialog();
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse("file://" + path), "image/*");
                    startActivity(intent);
                    mIsPreview = false;
                }
                break;

            case IChannelListener.DATA_CHANNEL_EVENT_PUT_MD5:
                showWaitDialog("Calculating MD5");
                break;
            case IChannelListener.DATA_CHANNEL_EVENT_PUT_START:
                showProgressDialog("Uploading...",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface Dialog, int which) {
                                mRemoteCam.cancelPutFile(mPutFileName);
                            }
                        });
                break;
            case IChannelListener.DATA_CHANNEL_EVENT_PUT_PROGRESS:
                mProgressDialog.setProgress((Integer) param);
                break;
            case IChannelListener.DATA_CHANNEL_EVENT_PUT_FINISH:
                showAlertDialog("Info", "Uploaded to " + mPutFileName);
                mPutFileName = null;
                //mMediaFrag.refreshDirContents();
                break;
        }
    }

    private void handleStreamChannelEvent(int type, Object param) {
        switch (type) {
            case IChannelListener.STREAM_CHANNEL_EVENT_BUFFERING:
                showWaitDialog("Buffering...");
                break;
            case IChannelListener.STREAM_CHANNEL_EVENT_PLAYING:
                dismissDialog();
                mCameraFrag.startStreamView();
                break;
            case IChannelListener.STREAM_CHANNEL_ERROR_PLAYING:
                mRemoteCam.stopLiveStream();
                mCameraFrag.resetStreamView();
                showAlertDialog("Error", "Cannot connect to LiveView!");
                break;
        }
    }

    private void startBluetoothSettings() {
        dismissDialog();
        mAlertDialog = new AlertDialog.Builder(this)
                .setTitle("Alert")
                .setMessage("Bluetooth is disabled currently. \nPlease turn it on first.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final Intent intent = new
                                Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                        startActivity(intent);
                    }
                })
                .show();
    }

    private void dismissDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
        }
    }

    private void showAlertDialog(String title, String msg) {
        dismissDialog();
        mAlertDialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showWaitDialog(String msg) {
        dismissDialog();
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("PLEASE WAIT ...");
        mProgressDialog.setMessage(msg);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    private void showProgressDialog(String title,
                                    DialogInterface.OnClickListener listener) {
        dismissDialog();
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(title);
        mProgressDialog.setMax(100);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                "Cancel", listener);
        mProgressDialog.show();
    }

    private void showSetupFragment() {
        Log.e(TAG, "show setup frag");
        mSetupFrag.setConnectivityType(mConnectivityType)
                .setWifiDevice(mWifiSsidName)
                .setBTDevice(mBTDeviceName);
        getFragmentManager().beginTransaction()
                .replace(mFragContainerId, mSetupFrag)
                .commit();
    }

    private void showCameraFragment() {
        getFragmentManager().beginTransaction()
                .replace(mFragContainerId, mCameraFrag)
                .commit();
    }

    private void showCommandsFragment() {
        getFragmentManager().beginTransaction()
                .replace(mFragContainerId, mCommandsFrag)
                .commit();
    }

    private void showMediaFragment() {
        getFragmentManager().beginTransaction()
                .replace(mFragContainerId, mMediaFrag)
                .commit();
    }

    private void showSettingsFragment() {
        getFragmentManager().beginTransaction()
                .replace(mFragContainerId, mSettingsFrag)
                .commit();
    }
}
