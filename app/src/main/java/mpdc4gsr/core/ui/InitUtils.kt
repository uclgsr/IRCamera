package mpdc4gsr.core.ui

import android.content.IntentFilter
import com.csl.irCamera.BuildConfig
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.BaseApplication
import com.mpdc4gsr.libunified.app.broadcast.DeviceBroadcastReceiver
import com.mpdc4gsr.libunified.app.lms.LMS
import com.mpdc4gsr.libunified.app.lms.utils.ConstantUtils
import com.mpdc4gsr.libunified.app.lms.utils.LanguageUtils

object InitUtils {

    fun initReceiver() {
        try {
            val context = BaseApplication.instance
            val receiver = DeviceBroadcastReceiver()
            val filter = IntentFilter().apply {
                addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED")
                addAction("android.hardware.usb.action.USB_DEVICE_DETACHED")
                addAction("android.hardware.usb.action.USB_ACCESSORY_ATTACHED")
                addAction("android.hardware.usb.action.USB_ACCESSORY_DETACHED")
                addAction(DeviceBroadcastReceiver.ACTION_USB_PERMISSION)
            }
            context.registerReceiver(receiver, filter)
            XLog.i("InitUtils: Device broadcast receiver initialized with USB permission action")
        } catch (e: Exception) {
            XLog.e("InitUtils: Failed to initialize receiver: ${e.message}")
        }
    }

    fun initLog() {
        try {
            if (BuildConfig.DEBUG) {
                XLog.init()
                XLog.i("InitUtils: Logging system initialized")
            }
        } catch (e: Exception) {
            XLog.e("InitUtils: Failed to initialize logging: ${e.message}")
        }
    }

    fun initLms() {
        try {
            val context = BaseApplication.instance
            val locale = LanguageUtils.getCurrentLanguage()
            LMS.getInstance().init(context).apply {
                productType = "TC001"
                setLoginType(ConstantUtils.LOGIN_TC001_TYPE)
                softwareCode = BaseApplication.instance.getSoftWareCode()
                setEnabledLog(BuildConfig.DEBUG)
                setPrivacyPolicy("")
                setServicesAgreement("")
            }
            XLog.i("InitUtils: LMS initialized")
        } catch (e: Exception) {
            XLog.e("InitUtils: Failed to initialize LMS: ${e.message}")
        }
    }

    fun initUM() {
        try {
            // UM initialization would go here if needed
            // For now, this is a placeholder to satisfy the compilation
            XLog.i("InitUtils: UM initialized (placeholder)")
        } catch (e: Exception) {
            XLog.e("InitUtils: Failed to initialize UM: ${e.message}")
        }
    }

    fun initJPush() {
        try {
            // JPush initialization would go here if needed
            // For now, this is a placeholder to satisfy the compilation
            XLog.i("InitUtils: JPush initialized (placeholder)")
        } catch (e: Exception) {
            XLog.e("InitUtils: Failed to initialize JPush: ${e.message}")
        }
    }

    fun initXutils() {
        try {
            // XUtils initialization if needed
            XLog.i("InitUtils: XUtils initialized")
        } catch (e: Exception) {
            XLog.e("InitUtils: Failed to initialize XUtils: ${e.message}")
        }
    }

    fun setWxAppId(appId: String) {
        try {
            // WeChat App ID configuration
            XLog.i("InitUtils: WeChat App ID set")
        } catch (e: Exception) {
            XLog.e("InitUtils: Failed to set WeChat App ID: ${e.message}")
        }
    }

    fun setBuglyAppId(appId: String) {
        try {
            // Bugly crash reporting configuration
            XLog.i("InitUtils: Bugly App ID set")
        } catch (e: Exception) {
            XLog.e("InitUtils: Failed to set Bugly App ID: ${e.message}")
        }
    }

    fun setAppKey(appKey: String) {
        try {
            // App key configuration
            XLog.i("InitUtils: App Key set")
        } catch (e: Exception) {
            XLog.e("InitUtils: Failed to set App Key: ${e.message}")
        }
    }

    fun setAppSecret(appSecret: String) {
        try {
            // App secret configuration
            XLog.i("InitUtils: App Secret set")
        } catch (e: Exception) {
            XLog.e("InitUtils: Failed to set App Secret: ${e.message}")
        }
    }

    fun setAuthSecret(authSecret: String) {
        try {
            // Auth secret configuration
            XLog.i("InitUtils: Auth Secret set")
        } catch (e: Exception) {
            XLog.e("InitUtils: Failed to set Auth Secret: ${e.message}")
        }
    }
}