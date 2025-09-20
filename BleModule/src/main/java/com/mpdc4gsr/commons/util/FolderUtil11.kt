package com.mpdc4gsr.commons.util

import android.text.TextUtils
import android.util.Log
import com.mpdc4gsr.lib.core.lms.LMS
import java.io.File

object FolderUtil {
    var mPath: String = "/data/user/0/com.topdon.diag.artidiag/files"
    var mUserId: String? = null
    var fileName: String? = null
    var tdartsSn: String? = null

    fun getFileName(): String {
        return fileName!!
    }

    fun setFileName(mfileName: String) {
        fileName = mfileName
    }

    fun setUserId(userId: String?) {
        mUserId = userId
    }


    fun init() {
        mUserId = PreUtil.Companion.getInstance(MPDC4GSR.getApp()).get("VCI_" + LMS.getInstance().getLoginName())
        setUserId(mUserId)
        Log.e("bcf", "FolderUtil mUserId: " + mUserId)
        mPath = MPDC4GSR.getApp().getExternalFilesDir("")!!.getAbsolutePath()
        Log.e("bcf", "FolderUtil init: " + mPath)
        initPath()
    }

    fun initTDarts(tdSn: String?) {
        tdartsSn = tdSn
        val mPath = MPDC4GSR.getApp().getExternalFilesDir("")!!.getAbsolutePath()
        Log.e("bcf", fileName + "---FolderUtil initTDarts: " + mPath)
        if (!TextUtils.isEmpty(tdSn)) {
            val rfidFile = File(mPath + fileName + tdSn + "/RFID/")
            if (!rfidFile.exists()) {
                Log.e("bcf", fileName + "---FolderUtil initTDarts: create")
                rfidFile.mkdirs()
            }
        }
    }

    fun initFilePath() {
        val basePath = MPDC4GSR.getApp().getExternalFilesDir("")!!.getAbsolutePath() + fileName
        val downPath = basePath + "Download/"
        Log.e("bcf", fileName + "--Download[CHINESE_TEXT]--" + downPath)
        val file = File(downPath)
        if (!file.exists()) {
            Log.e("bcf", fileName + "---Download[CHINESE_TEXT]Create ")
            file.mkdirs()
        }
    }

    private fun initPath() {
        if (!TextUtils.isEmpty(mUserId)) {
            val asiaLibsFile = File(mPath + fileName + mUserId + "/Diagnosis/Asia/")
            if (!asiaLibsFile.exists()) {
                asiaLibsFile.mkdirs()
            }
            val europeLibsFile = File(mPath + fileName + mUserId + "/Diagnosis/Europe/")
            if (!europeLibsFile.exists()) {
                europeLibsFile.mkdirs()
            }
            val americaLibsFile = File(mPath + fileName + mUserId + "/Diagnosis/America/")
            if (!americaLibsFile.exists()) {
                americaLibsFile.mkdirs()
            }
            val chinaLibsFile = File(mPath + fileName + mUserId + "/Diagnosis/China/")
            if (!chinaLibsFile.exists()) {
                chinaLibsFile.mkdirs()
            }
            val publicLibsFile = File(mPath + fileName + mUserId + "/Diagnosis/Public/")
            if (!publicLibsFile.exists()) {
                publicLibsFile.mkdirs()
            }

            val immoAsiaLibsFile = File(mPath + fileName + mUserId + "/Immo/Asia/")
            if (!immoAsiaLibsFile.exists()) {
                immoAsiaLibsFile.mkdirs()
            }

            val immoEuropeLibsFile = File(mPath + fileName + mUserId + "/Immo/Europe/")
            if (!immoEuropeLibsFile.exists()) {
                immoEuropeLibsFile.mkdirs()
            }

            val immoAmericaLibsFile = File(mPath + fileName + mUserId + "/Immo/America/")
            if (!immoAmericaLibsFile.exists()) {
                immoAmericaLibsFile.mkdirs()
            }

            val immoChinaLibsFile = File(mPath + fileName + mUserId + "/Immo/China/")
            if (!immoChinaLibsFile.exists()) {
                immoChinaLibsFile.mkdirs()
            }

            val immoAustraliaLibsFile = File(mPath + fileName + mUserId + "/Immo/Australia/")
            if (!immoAustraliaLibsFile.exists()) {
                immoAustraliaLibsFile.mkdirs()
            }

            val rfidLibsFile = File(mPath + fileName + mUserId + "/RFID/")
            if (!rfidLibsFile.exists()) {
                rfidLibsFile.mkdirs()
            }

            val energy = File(mPath + fileName + mUserId + "/NewEnergy/")
            if (!energy.exists()) {
                energy.mkdirs()
            }

            val shotFile = File(mPath + fileName + mUserId + "/Shot/")
            if (!shotFile.exists()) {
                shotFile.mkdirs()
            }

            val pdfFile = File(mPath + fileName + mUserId + "/Pdf/")
            if (!pdfFile.exists()) {
                pdfFile.mkdirs()
            }

            val dataStreamFile = File(mPath + fileName + mUserId + "/Datastream/")
            if (!dataStreamFile.exists()) {
                dataStreamFile.mkdirs()
            }

            val appFile = File(mPath + fileName + "App/")
            if (!appFile.exists()) {
                appFile.mkdirs()
            }

            val firmwareFile = File(mPath + fileName + "Firmware/")
            if (!firmwareFile.exists()) {
                firmwareFile.mkdirs()
            }


            val tdartsFile = File(mPath + fileName + "T-darts/")
            if (!tdartsFile.exists()) {
                tdartsFile.mkdirs()
            }

            val userDiagnose = File(mPath + fileName + "UserData/Diagnose/")
            if (!userDiagnose.exists()) {
                userDiagnose.mkdirs()
            }

            val userImmo = File(mPath + fileName + "UserData/Immo/")
            if (!userImmo.exists()) {
                userImmo.mkdirs()
            }

            val userNewEnergy = File(mPath + fileName + "UserData/NewEnergy/")
            if (!userNewEnergy.exists()) {
                userNewEnergy.mkdirs()
            }

            val userRFID = File(mPath + fileName + "UserData/RFID/")
            if (!userRFID.exists()) {
                userRFID.mkdirs()
            }

            val downFile = File(mPath + fileName + mUserId + "Download/")
            if (!downFile.exists()) {
                downFile.mkdirs()
            }

            val diagHistoryFile = File(mPath + fileName + mUserId + "/History/Diagnose/")
            if (!diagHistoryFile.exists()) {
                diagHistoryFile.mkdirs()
            }

            val seriveHistoryFile = File(mPath + fileName + mUserId + "/History/Service/")
            if (!seriveHistoryFile.exists()) {
                seriveHistoryFile.mkdirs()
            }

            val galleryFile = File(mPath + fileName + mUserId + "/Gallery/")
            if (!galleryFile.exists()) {
                galleryFile.mkdirs()
            }
            val dataLogFile = File(mPath + fileName + mUserId + "/DataLog/")
            if (!dataLogFile.exists()) {
                dataLogFile.mkdirs()
            }

            val log6File = File(mPath + fileName + "666666/")
            if (!log6File.exists()) {
            }
            val log7File = File(mPath + fileName + "777777/")
            if (!log7File.exists()) {
            }
            val log8File = File(mPath + fileName + "888888/")
            if (!log8File.exists()) {
            }
            val log9File = File(mPath + fileName + "999999/")
            if (!log9File.exists()) {
            }

            val feedbackLog = File(mPath + fileName + mUserId + "/FeedbackLog/")
            if (!feedbackLog.exists()) {
                feedbackLog.mkdirs()
            }

            val autovinLog = File(mPath + fileName + mUserId + "/autovinLog/")
            if (!autovinLog.exists()) {
                autovinLog.mkdirs()
            }
        }
    }

    val otaPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!.getAbsolutePath() + "/s/"


    val dataBasePath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!.getAbsolutePath() + fileName

    val tDartsRootPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + tdartsSn + "/"

    val rootPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + mUserId + "/"

    val vehiclesPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + mUserId + "/Diagnosis/"

    val immoPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + mUserId + "/Immo/"

    val rfidTopScanPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + tdartsSn + "/RFID/"

    val rfidPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + mUserId + "/RFID/"

    val asiaPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + mUserId + "/Diagnosis/Asia/"

    val americaPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + mUserId + "/Diagnosis/America/"

    val europePath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + mUserId + "/Diagnosis/Europe/"

    val vehiclePublicPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + mUserId + "/Diagnosis/Public/"

    val vehicleTopScanPublicPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!.getAbsolutePath() + fileName


    val shotPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + mUserId + "/Shot/"

    val dataStreamPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + mUserId + "/Datastream/"

    val pdfPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + mUserId + "/Pdf/"

    val appPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!.getAbsolutePath() + fileName + "App/"

    val firmwarePath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!.getAbsolutePath() + fileName + "Firmware/"

    val tdartsUpgradePath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!.getAbsolutePath() + fileName + "T-darts/"

    val downloadPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + mUserId + "/Download/"

    val diagHistoryPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + mUserId + "/History/Diagnose/"

    val serviceHistoryPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + mUserId + "/History/Service/"

    val logPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!.getAbsolutePath() + fileName + "Log/"

    val soLogPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!.getAbsolutePath() + fileName + "Log/SoLog/"

    val galleryPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + mUserId + "/Gallery/"

    val dataLogPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + mUserId + "/DataLog/"

    val diagDataLogPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + mUserId + "/DataLog/DIAG/"


    val immoDataLogPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + mUserId + "/DataLog/IMMO/"

    val feedbackLogPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + mUserId + "/FeedbackLog/"

    val userDataDiag: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!.getAbsolutePath() + fileName + "UserData/Diagnose/"

    val userDataImmo: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!.getAbsolutePath() + fileName + "UserData/Immo/"

    val userDataNewEnergy: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + "UserData/NewEnergy/"

    val userDataRFID: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!.getAbsolutePath() + fileName + "UserData/RFID/"

    val softDownPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!.getAbsolutePath() + fileName + "Download/"

    val autoVinLogPath: String
        get() = MPDC4GSR.getApp().getExternalFilesDir("")!!
            .getAbsolutePath() + fileName + mUserId + "/autovinLog/"
}
