package mpdc4gsr

import android.content.Context
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.TimeUtils
import com.blankj.utilcode.util.Utils
import com.csl.irCamera.BuildConfig
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.LogLevel
import com.elvishew.xlog.XLog
import com.elvishew.xlog.flattener.PatternFlattener
import com.elvishew.xlog.printer.AndroidPrinter
import com.elvishew.xlog.printer.file.FilePrinter
import com.elvishew.xlog.printer.file.backup.FileSizeBackupStrategy2
import com.elvishew.xlog.printer.file.clean.FileLastModifiedCleanStrategy
import com.elvishew.xlog.printer.file.naming.ChangelessFileNameGenerator
import com.mpdc4gsr.libunified.app.BaseApplication
import com.mpdc4gsr.libunified.app.broadcast.DeviceBroadcastReceiver
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.config.HttpConfig
import com.mpdc4gsr.libunified.app.lms.LMS
import com.mpdc4gsr.libunified.app.lms.utils.ConstantUtil
import com.mpdc4gsr.libunified.app.lms.utils.LanguageUtil
import java.util.Date

public var InitUtil.productType: String
    get() = LMS.getInstance().productType
    set(value) {
        LMS.getInstance().productType = value
    }

object InitUtil {
    fun initLog() {
        val fileName = "logs_${TimeUtils.date2String(Date(), "yyyy-MM-dd")}.log"
        val fileDir = BaseApplication.instance.getExternalFilesDir("log")!!.absolutePath
        val tag = "MPDC4GSR_LOG"
        val pattern = "{d}, {L}, {t}, {m}"
        val backupStrategy = FileSizeBackupStrategy2(5 * 1024 * 1024L, 10)
        val cleanStrategy = FileLastModifiedCleanStrategy(30 * 24 * 60 * 60)

        val config =
            LogConfiguration.Builder()
                .logLevel(LogLevel.ALL)
                .tag(tag)
                .build()
        val androidPrinter = AndroidPrinter(true)
        val filePrinter =
            FilePrinter.Builder(fileDir)
                .fileNameGenerator(ChangelessFileNameGenerator(fileName))
                .backupStrategy(backupStrategy)
                .cleanStrategy(cleanStrategy)
                .flattener(PatternFlattener(pattern))
                .build()
        if (BuildConfig.DEBUG) {
            XLog.init(config, androidPrinter, filePrinter)
        } else {
            XLog.init(config, filePrinter)
        }
    }

    fun initLms() {
        val privacyPolicyUrl =
            "https://plat.topdon.com/topdon-plat/out-user/baseinfo/template/getHtmlContentById?" +
                    "softCode=${BaseApplication.instance.getSoftWareCode()}&" +
                    "language=${LanguageUtil.getLanguageId(Utils.getApp())}&type=22"

        val servicesAgreementUrl =
            "https://plat.topdon.com/topdon-plat/out-user/baseinfo/template/getHtmlContentById?" +
                    "softCode=${BaseApplication.instance.getSoftWareCode()}&" +
                    "language=${LanguageUtil.getLanguageId(Utils.getApp())}&type=21"

        LMS.getInstance().init(BaseApplication.instance)
            .apply {
                productType = "TC001"
                setLoginType(ConstantUtil.LOGIN_TS001_TYPE)
                softwareCode = BaseApplication.instance.getSoftWareCode()
                setEnabledLog(false)
                setPrivacyPolicy(privacyPolicyUrl)
                setServicesAgreement(servicesAgreementUrl)
                if (!BaseApplication.instance.isDomestic()) {
                    initXutils()
                } else {
                    setWxAppId("wx588cb319449b72dd")
                    setBuglyAppId("0b375add84")
                }
                setAppKey(BuildConfig.APP_KEY)
                setAppSecret(BuildConfig.APP_SECRET)
                setAuthSecret(HttpConfig.AUTH_SECRET)
            }
    }

    fun initUM() {
        try {
            // Initialize UMeng analytics and common SDK
            val context = BaseApplication.instance
            
            // Initialize UMeng Common SDK first
            com.umeng.commonsdk.UMConfigure.setLogEnabled(true)
            com.umeng.commonsdk.UMConfigure.init(
                context,
                BuildConfig.APP_KEY,
                "Umeng",
                com.umeng.commonsdk.UMConfigure.DEVICE_TYPE_PHONE,
                null
            )
            
            // Initialize analytics
            com.umeng.analytics.MobclickAgent.setPageCollectionMode(
                com.umeng.analytics.MobclickAgent.PageMode.AUTO
            )
            
            XLog.i("UMeng SDK initialized successfully")
        } catch (e: Exception) {
            XLog.e("Failed to initialize UMeng SDK: ${e.message}")
        }
    }

    fun initJPush() {
        try {
            val context = BaseApplication.instance
            
            // Initialize JPush
            cn.jpush.android.api.JPushInterface.setDebugMode(BuildConfig.DEBUG)
            cn.jpush.android.api.JPushInterface.init(context)
            
            // Get registration ID
            val registrationID = cn.jpush.android.api.JPushInterface.getRegistrationID(context)
            
            if (SharedManager.getHasShowClause()) {
                XLog.w("JPush registrationID= $registrationID")
            }
            
            XLog.i("JPush SDK initialized successfully")
        } catch (e: Exception) {
            XLog.e("Failed to initialize JPush SDK: ${e.message}")
            
            // Fallback logging for debugging
            if (SharedManager.getHasShowClause()) {
                XLog.w("registrationID= unavailable")
            }
        }
    }

    fun initReceiver() {
        try {
            BaseApplication.instance.unregisterReceiver(BaseApplication.usbObserver)
        } catch (e: Exception) {

        }

        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
        filter.addAction(DeviceBroadcastReceiver.ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT < 33) {
            ContextCompat.registerReceiver(
                BaseApplication.instance,
                BaseApplication.usbObserver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } else {
            BaseApplication.instance.registerReceiver(
                BaseApplication.usbObserver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        }
    }
}
