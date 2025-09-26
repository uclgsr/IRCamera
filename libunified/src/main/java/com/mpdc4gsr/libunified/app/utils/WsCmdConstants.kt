package com.mpdc4gsr.libunified.app.utils

object WsCmdConstants {
    const val AR_COMMAND_IP: String = "127.0.0.1"
    const val AR_COMMAND_LOGIN: Int = 1
    const val AR_COMMAND_LOGOUT: String = "AR_COMMAND_LOGOUT"
    const val AR_COMMAND_VERSION_GET = 3
    const val AR_COMMAND_DEV_INFO_GET: String = "AR_COMMAND_DEV_INFO_GET"
    const val AR_COMMAND_CONFIG_RESET: String = "AR_COMMAND_CONFIG_RESET"
    const val AR_COMMAND_ALL_RESET: String = "AR_COMMAND_ALL_RESET"
    const val AR_COMMAND_POWER_CTL: String = "AR_COMMAND_POWER_CTL"
    const val AR_COMMAND_BATTERY_GET: String = "AR_COMMAND_BATTERY_GET"
    const val AR_COMMAND_USBPC_CONN_STATE_GET: String = "AR_COMMAND_USBPC_CONN_STATE_GET"
    const val AR_COMMAND_LANGUAGE_SET: Int = 11
    const val AR_COMMAND_LANGUAGE_GET: String = "AR_COMMAND_LANGUAGE_GET"
    const val AR_COMMAND_TIME_ZONE_SET: String = "AR_COMMAND_TIME_ZONE_SET"
    const val AR_COMMAND_TIME_ZONE_GET: String = "AR_COMMAND_TIME_ZONE_GET"
    const val AR_COMMAND_TIME_SET: String = "AR_COMMAND_TIME_SET"
    const val AR_COMMAND_TIME_GET: String = "AR_COMMAND_TIME_GET"
    const val AR_COMMAND_SNAPSHOT: Int = 20
    const val AR_COMMAND_RECORD_START: String = "AR_COMMAND_RECORD_START"
    const val AR_COMMAND_RECORD_STOP: String = "AR_COMMAND_RECORD_STOP"
    const val AR_COMMAND_RECORD_DELETE: String = "AR_COMMAND_RECORD_DELETE"
    const val AR_COMMAND_VRECORD: Int = 21
    const val AR_COMMAND_SCENE_COMP: Int = 120
    const val AR_COMMAND_IMG_SCENE_SET: Int = 201
    const val AR_COMMAND_PANEL_SHIFT_SET: String = "AR_COMMAND_PANEL_SHIFT_SET"
    const val AR_COMMAND_PANEL_SHIFT_GET: String = "AR_COMMAND_PANEL_SHIFT_GET"
    const val AR_COMMAND_PRODUCT_CFG_GET: Int = 401
    const val APP_EVENT_HEART_BEATS: Int = 1001
    const val APP_EVENT_DISTANCE_DATA: Int = 500
    const val APP_EVENT_TEMP_DATA: Int = 500
}