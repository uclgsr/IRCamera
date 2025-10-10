package mpdc4gsr.core.ui

import android.content.IntentFilter
import com.csl.irCamera.BuildConfig
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
            val filter =
                IntentFilter().apply {
                    addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED")
                    addAction("android.hardware.usb.action.USB_DEVICE_DETACHED")
                    addAction("android.hardware.usb.action.USB_ACCESSORY_ATTACHED")
                    addAction("android.hardware.usb.action.USB_ACCESSORY_DETACHED")
                    addAction(DeviceBroadcastReceiver.ACTION_USB_PERMISSION)
                }
            context.registerReceiver(receiver, filter)
        } catch (e: Exception) {
        }
    }

    fun initLog() {
        // Logging disabled; no initialization required.
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
        } catch (e: Exception) {
        }
    }

    fun initUM() {
        try {
            // UM initialization would go here if needed
            // For now, this is a placeholder to satisfy the compilation
        } catch (e: Exception) {
        }
    }

    fun initJPush() {
        try {
            // JPush initialization would go here if needed
            // For now, this is a placeholder to satisfy the compilation
        } catch (e: Exception) {
        }
    }

    fun initXutils() {
        try {
            // XUtils initialization if needed
        } catch (e: Exception) {
        }
    }

    fun setWxAppId(appId: String) {
        try {
            // WeChat App ID configuration
        } catch (e: Exception) {
        }
    }

    fun setBuglyAppId(appId: String) {
        try {
            // Bugly crash reporting configuration
        } catch (e: Exception) {
        }
    }

    fun setAppKey(appKey: String) {
        try {
            // App key configuration
        } catch (e: Exception) {
        }
    }

    fun setAppSecret(appSecret: String) {
        try {
            // App secret configuration
        } catch (e: Exception) {
        }
    }

    fun setAuthSecret(authSecret: String) {
        try {
            // Auth secret configuration
        } catch (e: Exception) {
        }
    }
}
