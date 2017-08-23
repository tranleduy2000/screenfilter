package com.duy.screenfilter;

public class Constants {

    /**
     * Service
     */
    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_COLOR_PROFILE = "color";
    public static final String EXTRA_BRIGHTNESS = "brightness";
    public static final String EXTRA_CHECK_FROM_TOGGLE = "check_from_toggle";
    public static final String EXTRA_DO_NOT_SEND_CHECK = "dont_send_check";

    public static final String ALARM_ACTION_START = "info.papdt.blackbulb.ALARM_ACTION_START";
    public static final String ALARM_ACTION_STOP = "info.papdt.blackbulb.ALARM_ACTION_STOP";

    public static final String ACTION_START = "start";
    public static final String ACTION_UPDATE = "update";
    public static final String ACTION_UPDATE_COLOR = "update_color";
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
    public static final int EVENT_CANNOT_START = 1;
    public static final int EVENT_DESTORY_SERVICE = 2;
    public static final int EVENT_CHECK = 3;

    public static final int MODE_NORMAL = 0;

}
