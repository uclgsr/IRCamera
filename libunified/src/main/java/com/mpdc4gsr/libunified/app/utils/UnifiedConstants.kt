package com.mpdc4gsr.libunified.app.utils

object UnifiedConstants {
    // Product Configuration Constants
    object Product {
        const val TYPE_NAME = "product_type"
        const val TS001_NAME = "TS001"
        const val TS004_NAME = "TS004"
    }

    // Settings Constants
    object Settings {
        const val TYPE = "setting_type"
        const val BOOK = 0
        const val FAQ = 1
        const val CONNECTION_TYPE = "connection_type"
        const val CONNECTION = 0
        const val DISCONNECTION = 1
        const val IS_REPORT_FIRST = "IS_REPORT_FIRST"
    }

    // IR Mode Constants
    object IRMode {
        const val TEMPERATURE_MODE = 1
        const val OBSERVE_MODE = 2
        const val EDIT_MODE = 4
        const val TCPLUS_MODE = 5
        const val TC007_MODE = 6
        const val TEMPERATURE_LITE = 7
    }

    // Network and Connection Constants
    object Network {
        const val DEFAULT_TIMEOUT = 5000L
        const val HEARTBEAT_INTERVAL = 5000L
        const val DISCOVERY_PORT = 8081
        const val CONTROLLER_PORT = 8080
    }

    // File System Constants
    object FileSystem {
        const val TEMP_DIR = "temp"
        const val CACHE_DIR = "cache"
        const val LOGS_DIR = "logs"
        const val RECORDINGS_DIR = "recordings"
        const val THERMAL_DIR = "thermal"
        const val RGB_DIR = "rgb"
        const val GSR_DIR = "gsr"
    }

    // Sensor Constants
    object Sensors {
        const val THERMAL_SENSOR = "thermal"
        const val RGB_SENSOR = "rgb"
        const val GSR_SENSOR = "gsr"
        const val POLLING_INTERVAL_MS = 100L
        const val CONNECTION_TIMEOUT_MS = 10000L
    }

    // Recording Constants
    object Recording {
        const val DEFAULT_QUALITY = 80
        const val MAX_DURATION_MS = 600000L // 10 minutes
        const val MIN_DURATION_MS = 1000L // 1 second
    }
}
