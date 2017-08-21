package com.duy.screenfilter;

public class Constants {

    /**
     * Service
     */
    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_BRIGHTNESS = "brightness";
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_CHECK_FROM_TOGGLE = "check_from_toggle";
    public static final String EXTRA_DO_NOT_SEND_CHECK = "dont_send_check";

    public static final String ALARM_ACTION_START = "info.papdt.blackbulb.ALARM_ACTION_START";
    public static final String ALARM_ACTION_STOP = "info.papdt.blackbulb.ALARM_ACTION_STOP";

    public static final String ACTION_START = "start";
    public static final String ACTION_UPDATE = "update";
    public static final String ACTION_PAUSE = "pause";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_CHECK = "check";

    /**
     * Broadcast
     */
    public static final String EXTRA_EVENT_ID = "event_id";

    /**
     * Event
     */
    public static final int EVENT_CANNOT_START = 1, EVENT_DESTORY_SERVICE = 2, EVENT_CHECK = 3;

    /**
     * Mode
     */
    public static final int MODE_NO_PERMISSION = 0, MODE_NORMAL = 1, MODE_OVERLAY_ALL = 2, MODE_EYES_CARE = 3;
    // PS: MODE_NO_PERMISSION is deprecated in Android 7.X

}
