package mpdc4gsr

import android.content.Context
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
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
        // TODO: Implement UM initialization
    }

    fun initJPush() {
        val registrationID = ""
        // TODO: Implement JPush initialization

        if (SharedManager.getHasShowClause()) {
            XLog.w("registrationID= $registrationID")
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
            BaseApplication.instance.registerReceiver(BaseApplication.usbObserver, filter)
        } else {
            BaseApplication.instance.registerReceiver(
                BaseApplication.usbObserver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        }
    }
}
