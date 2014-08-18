package com.github.dddpaul.netcat;

public class Constants
{
    public static final String SHARED_PREFS_NAME = "SimpleNetCat-Area";
    public static final String CONNECT_TO_SET_KEY = "connect_to_set";
    public static final String ACTIONS_VISIBILITY_KEY = "actions_visibility";
    public static final String NETCAT_STATE_KEY = "netcat_state";
    public static final String RECEIVED_TEXT_KEY = "received_text";
    public static final String NETCAT_FRAGMENT_TAG = "netcat_fragment_tag";

    // Ready to get full-size UDP datagram or TCP segment in one step
    public static int RECEIVE_BUFFER_LIMIT = 2<<16 - 1;

    // Amount of characters you can type from touchscreen before you lose your mind
    public static int SEND_BUFFER_LIMIT = 1024;
}
