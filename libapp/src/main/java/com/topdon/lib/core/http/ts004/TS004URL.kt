package com.topdon.lib.core.http.ts004

object TS004URL {
    const val RTSP_URL = "rtsp://192.168.40.1/ch0/stream0"
    private const val BASE_URL = "http://192.168.40.1:8080"
    const val SET_PSEUDO_COLOR = "$BASE_URL/api/v1/system/setPseudoColor" // settingspseudo color样式
    const val GET_PSEUDO_COLOR = "$BASE_URL/api/v1/system/getPseudoColor" // Get/Retrievepseudo color样式
    const val SET_PANEL_PARAM = "$BASE_URL/api/v1/system/setPanelParam" // settings屏幕brightness
    const val GET_PANEL_PARAM = "$BASE_URL/api/v1/system/setPanelParam" // Get/Retrieve屏幕brightness
    const val SET_PIP = "$BASE_URL/api/v1/system/setPip" // settingspicture-in-picture
    const val GET_PIP = "$BASE_URL/api/v1/system/getPip" // Get/Retrievepicture-in-picturestate
    const val SET_ZOOM = "$BASE_URL/api/v1/system/setZoom" // settings放大倍数
    const val GET_ZOOM = "$BASE_URL/api/v1/system/getZoom" // Get/Retrieve放大倍数
    const val SET_SNAPSHOT = "$BASE_URL/api/v1/system/snapshot" // capture
    const val GET_VRECORD = "$BASE_URL/api/v1/system/vrecord" // recording
    const val GET_RECORD_STATUS = "$BASE_URL/api/v1/system/getRecordStatus" // Get/Retrieverecordingstate
    const val GET_VERSION = "$BASE_URL/api/v1/system/getVersion" // Get/Retrieveversioninfo
    const val GET_DEVICE_DETAILS = "$BASE_URL/api/v1/system/getDeviceInfo" // Get/Retrievedeviceinfo
    const val GET_FREE_SPACE = "$BASE_URL/api/v1/system/getFreeSpace" // Get/Retrievestorage分区info
    const val GET_FORMAT_STORAGE = "$BASE_URL/api/v1/system/formatStorage" // format化storage分区
    const val GET_RESET_ALL = "$BASE_URL/api/v1/system/resetAll" // restore出厂settings
    const val GET_DELETE_FILE = "$BASE_URL/api/v1/system/deleteFile" // deletephotovideofile
    const val GET_UPGRADE_STATUS = "$BASE_URL/api/v1/system/getUpgradeStatus" // Get/RetrievefirmwareUpgradestate
    const val SET_TEMPERATURE_STATE = "$BASE_URL/api/v1/system/setTemperatureState" // settingstemperature measurement开关
    const val GET_FILE_LIST = "$BASE_URL/api/v1/system/getFileList" // Get/Retrievefilelist
    const val SET_DATE_TIME = "$BASE_URL/api/v1/system/setDateTime" // settings时钟
    const val GET_SEND_UPGRADE_FILE_START = "$BASE_URL/api/v1/system/sendUpgradeFileStart" // firmwareUpgradedata传输start
    const val GET_SEND_UPGRADE_FILE_DATA = "$BASE_URL/api/v1/system/sendUpgradeFileData" // firmwareUpgradedata传输
    const val GET_SEND_UPGRADE_FILE_END = "$BASE_URL/api/v1/system/sendUpgradeFileEnd" // firmwareUpgradedata传输end
    const val GET_REMOTE_UPGRADE = "$BASE_URL/api/v1/system/remoteUpgrade" // firmwareUpgrade
    const val SET_WIFI_AP_ON_OFF = "$BASE_URL/api/v1/system/setWifiAPOnOff" // settingswifi on/off
    const val GET_WIFI_AP_CONFIG = "$BASE_URL/api/v1/system/getWifiAPConfig" // Get/Retrievewificonfigurationinfo
    const val GET_FILE_COUNT = "$BASE_URL/api/v1/system/getFileCount" // Get/Retrievefilequantity
    const val SET_POWER_ACTION = "$BASE_URL/api/v1/system/setPowerAction" // settings电源state
    const val SET_DO_NUC = "$BASE_URL/api/v1/system/doNuc" // nuc
    const val SET_FREEZE = "$BASE_URL/api/v1/system/setFreeze" // image冻结
    const val GET_BATTERY_INFO = "$BASE_URL/api/v1/system/getBatteryInfo" // Get/Retrieve电池info
    const val SET_TISP = "$BASE_URL/api/v1/system/setTISR" // settings超分
    const val GET_TISR = "$BASE_URL/api/v1/system/getTISR" // Get/Retrieve超分state
    const val GET_DATE_TIME = "$BASE_URL/api/v1/system/getDateTime" // Get/Retrieve时钟
}
