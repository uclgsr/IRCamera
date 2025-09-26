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
        if (BuildConfig.DEBUG) {        } else {        }
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
            // Initialize UMeng analytics and common SDK using app/libs libraries
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

            // Initialize analytics with enhanced configuration
            com.umeng.analytics.MobclickAgent.setPageCollectionMode(
                com.umeng.analytics.MobclickAgent.PageMode.AUTO
            )

            // Enable enhanced analytics features available in UMeng libs
            try {
                com.umeng.analytics.MobclickAgent.setCatchUncaughtExceptions(true)
                com.umeng.analytics.MobclickAgent.setScenarioType(
                    context,
                    com.umeng.analytics.MobclickAgent.EScenarioType.E_UM_NORMAL
                )

                // Initialize auth-number service if available (from auth-number-2.13.2.1.aar)
                initializeAuthNumberService(context)

                X            } catch (e: Exception) {
                X                X            }
        } catch (e: Exception) {
            X        }
    }

    private fun initializeAuthNumberService(context: Context) {
        try {
            // Use reflection to initialize auth-number service from app/libs if available
            val authClass = Class.forName("com.netease.nis.quicklogin.QuickLogin")
            val initMethod = authClass.getMethod("getInstance", Context::class.java, String::class.java)
            val authInstance = initMethod.invoke(null, context, BuildConfig.APP_KEY)

            X        } catch (e: ClassNotFoundException) {
            X")
        } catch (e: Exception) {
            X        }
    }

    fun initJPush() {
        try {
            val context = BaseApplication.instance

            // Check if JPush SDK is available at runtime
            val jpushClass = try {
                Class.forName("cn.jpush.android.api.JPushInterface")
            } catch (e: ClassNotFoundException) {
                X                if (SharedManager.getHasShowClause()) {
                    X")
                }
                return
            }

            // Use reflection to call JPush methods safely
            try {
                val setDebugModeMethod = jpushClass.getMethod("setDebugMode", Boolean::class.java)
                val initMethod = jpushClass.getMethod("init", android.content.Context::class.java)
                val getRegistrationIDMethod =
                    jpushClass.getMethod("getRegistrationID", android.content.Context::class.java)

                setDebugModeMethod.invoke(null, BuildConfig.DEBUG)
                initMethod.invoke(null, context)

                val registrationID = getRegistrationIDMethod.invoke(null, context) as? String ?: "unknown"

                if (SharedManager.getHasShowClause()) {
                    X                }

                X            } catch (e: Exception) {
                X                if (SharedManager.getHasShowClause()) {
                    X")
                }
            }
        } catch (e: Exception) {
            X            // Fallback logging for debugging
            if (SharedManager.getHasShowClause()) {
                X            }
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
