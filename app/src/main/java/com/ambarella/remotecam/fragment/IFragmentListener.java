package com.ambarella.remotecam.fragment;

/**
 * Created by jli on 8/29/14.
 */
public interface IFragmentListener {

    public void onFragmentAction(int type, Object param, Integer... array);

    /**
     * Action: Connectivity setup
     */
    final static int ACTION_CONNECTIVITY_SELECTED = 0x01;
    final static int ACTION_BT_LIST = 0x02;
    final static int ACTION_BT_CANCEL = 0x03;
    final static int ACTION_BT_SELECTED = 0x04;
    final static int ACTION_WIFI_LIST = 0x05;
    final static int ACTION_BLE_LIST = 0x06;
    final static int ACTION_BT_ENABLE = 0x07;

    /**
     * Action: Various Button Clicks
     */
    final static int ACTION_BC_WAKEUP = 0x10;
    final static int ACTION_BC_STANDBY = 0x11;
    final static int ACTION_BC_START_SESSION = 0x12;
    final static int ACTION_BC_STOP_SESSION = 0x13;
    final static int ACTION_BC_SEND_COMMAND = 0x14;
    final static int ACTION_BC_GET_ALL_SETTINGS = 0x15;
    final static int ACTION_BC_GET_SETTING_OPTIONS = 0x16;
    final static int ACTION_BC_SET_SETTING = 0x17;
    final static int ACTION_BC_GET_ALL_SETTINGS_DONE = 0x18;
    final static int ACTION_BC_SET_BITRATE = 0x19;
    
    /**
     * file-system related
     */
    final static int ACTION_FS_CD = 0x20;
    final static int ACTION_FS_LS = 0x21;
    final static int ACTION_FS_DELETE = 0x22;
    final static int ACTION_FS_DOWNLOAD = 0x23;
    final static int ACTION_FS_VIEW = 0x24;
    final static int ACTION_FS_INFO = 0x25;
    final static int ACTION_FS_FORMAT_SD = 0x26;
    final static int ACTION_FS_GET_FILE_INFO = 0x27;
    final static int ACTION_FS_BURN_FW = 0x28;
    final static int ACTION_FS_SET_RO = 0x29;
    final static int ACTION_FS_SET_WR = 0x2A;
    final static int ACTION_FS_GET_THUMB = 0x2B;
    
    /**
     * Viewfinder related
     */
    final static int ACTION_VF_START = 0x30;
    final static int ACTION_VF_STOP = 0x31;
    final static int ACTION_PHOTO_START = 0x32;
    final static int ACTION_RECORD_START = 0x33;
    final static int ACTION_RECORD_STOP = 0x34;
    final static int ACTION_RECORD_TIME = 0x35;
    final static int ACTION_PLAYER_START = 0x36;
    final static int ACTION_PLAYER_STOP = 0x37;
    final static int ACTION_PHOTO_STOP = 0x38;
    final static int ACTION_FORCE_SPLIT = 0x39;
    final static int ACTION_SET_ZOOM = 0x3A;
    final static int ACTION_GET_ZOOM_INFO = 0x3B;
    final static int ACTION_MIC_ON = 0x3C;
    final static int ACTION_MIC_OFF = 0x3D;
    final static int ACTION_SYNC_TIME = 0x3E;
}
