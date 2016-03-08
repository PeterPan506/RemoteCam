package com.ambarella.remotecam.connectivity;

/**
 * Created by jli on 9/19/14.
 */
public interface IChannelListener {
    final static int MSG_MASK = 0x7FFFFF00;
    
    final static int CMD_CHANNEL_MSG = 0x000;
    final static int CMD_CHANNEL_EVENT_INIT = 0x01;
    final static int CMD_CHANNEL_EVENT_SHUTDOWN = 0x02;
    final static int CMD_CHANNEL_EVENT_LOG = 0x03;
    final static int CMD_CHANNEL_EVENT_SHOW_ALERT = 0x04;
    final static int CMD_CHANNEL_EVENT_LS = 0x05;
    final static int CMD_CHANNEL_EVENT_DEL = 0x06;
    final static int CMD_CHANNEL_EVENT_GET_FILE = 0x07;
    final static int CMD_CHANNEL_EVENT_GET_INFO = 0x08;
    final static int CMD_CHANNEL_EVENT_RESETVF = 0x09;
    final static int CMD_CHANNEL_EVENT_GET_ALL_SETTINGS = 0x0A;
    final static int CMD_CHANNEL_EVENT_GET_OPTIONS = 0x0B;
    final static int CMD_CHANNEL_EVENT_SET_SETTING = 0x0C;
    final static int CMD_CHANNEL_EVENT_CONNECTED = 0x0D;
    final static int CMD_CHANNEL_EVENT_GET_SPACE = 0x0F;
    final static int CMD_CHANNEL_EVENT_GET_NUM_FILES = 0x10;
    final static int CMD_CHANNEL_EVENT_GET_DEVINFO = 0x11;
    final static int CMD_CHANNEL_EVENT_FORMAT_SD = 0x12;
    final static int CMD_CHANNEL_EVENT_PUT_FILE = 0x13;
    final static int CMD_CHANNEL_EVENT_BATTERY_LEVEL = 0x14;
    final static int CMD_CHANNEL_EVENT_RECORD_TIME = 0x15;
    final static int CMD_CHANNEL_EVENT_STOP_VF = 0x16;
    final static int CMD_CHANNEL_EVENT_START_SESSION = 0x17;
    final static int CMD_CHANNEL_EVENT_START_CONNECT = 0x20;
    final static int CMD_CHANNEL_EVENT_START_LS = 0x21;    
    final static int CMD_CHANNEL_EVENT_WAKEUP_START = 0x22;
    final static int CMD_CHANNEL_EVENT_WAKEUP_OK = 0x23;
    final static int CMD_CHANNEL_EVENT_SET_ATTRIBUTE = 0x24;
    final static int CMD_CHANNEL_EVENT_GET_THUMB = 0x25;
    final static int CMD_CHANNEL_EVENT_SET_ZOOM = 0x26;
    final static int CMD_CHANNEL_EVENT_GET_ZOOM_INFO = 0x27;

    final static int CMD_CHANNEL_ERROR_TIMEOUT = 0x80;
    final static int CMD_CHANNEL_ERROR_INVALID_TOKEN = 0x81;
    final static int CMD_CHANNEL_ERROR_BLE_INVALID_ADDR = 0x82;
    final static int CMD_CHANNEL_ERROR_BLE_DISABLED = 0x83;
    final static int CMD_CHANNEL_ERROR_BROKEN_CHANNEL = 0x84;
    final static int CMD_CHANNEL_ERROR_WAKEUP = 0x85;
    final static int CMD_CHANNEL_ERROR_CONNECT = 0x86;
    final static int CMD_CHANNEL_SESSION_START_FAIL = 0x87;//潘俊威加的session建立失败
    
    final static int DATA_CHANNEL_MSG = 0x200;
    final static int DATA_CHANNEL_EVENT_GET_START = 0x200;
    final static int DATA_CHANNEL_EVENT_GET_PROGRESS = 0x201;
    final static int DATA_CHANNEL_EVENT_GET_FINISH = 0x202;
    final static int DATA_CHANNEL_EVENT_PUT_START = 0x203;
    final static int DATA_CHANNEL_EVENT_PUT_PROGRESS = 0x204;
    final static int DATA_CHANNEL_EVENT_PUT_FINISH = 0x205;
    final static int DATA_CHANNEL_EVENT_PUT_MD5 = 0x206;
    final static int DATA_CHANNEL_EVENT_START_UPGRADE = 0x207;
    
    final static int STREAM_CHANNEL_MSG = 0x400;
    final static int STREAM_CHANNEL_EVENT_BUFFERING = 0x400;
    final static int STREAM_CHANNEL_EVENT_PLAYING = 0x401;
    final static int STREAM_CHANNEL_ERROR_PLAYING = 0x402;

    public void onChannelEvent(int type, Object param, String...array);
}
