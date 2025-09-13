package com.topdon.lib.core.viewmodel

import com.elvishew.xlog.XLog
import com.topdon.lib.core.bean.event.VersionUpData
import com.topdon.lib.core.bean.json.CheckVersionJson
import com.topdon.lib.core.bean.json.SoftConfigOtherTypeVO
import com.topdon.lib.core.ktbase.BaseViewModel
import com.topdon.lib.core.utils.SingleLiveEvent

/**
 * VersionViewModel implements custom user interface component functionality.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
class VersionViewModel : BaseViewModel() {
    val updateLiveData = SingleLiveEvent<VersionUpData>()

    /**
     * forcedUpgradeFlag: 1 强制update    0 非强制update
     * descType: 包含3时,Show/Display给User(descTypeGet/RetrieveUpgrade描述info)
     */
    fun checkVersion() {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                if (TimeUtils.isToday(SharedManager.getVersionCheckDate())) {
//                    Log.w("123", "今天已有versionupdatetip")
//                    return@launch
//                }
//                val result: CheckVersionJson = LmsRepository.getVersionInfo() ?: return@launch
//                /*if (result.googleVerCode > AppUtils.getAppVersionCode()) {
//                    // google play需要Upgrade
//                    updateTip(result)
//                    return@launch
//                }*/
//                if (VersionTool.checkVersion(remoteStr = result.versionNo ?: "1.0", localStr = AppUtils.getAppVersionName())) {
//                    // google play检测不出时,官方Upgrade,根据app情况跳转对应的Upgrade渠道
//                    updateTip(result)
//                    return@launch
//                }
//            } catch (e: Exception) {
//                XLog.e("检测exception: ${e.message}")
//            }
//        }
    }

    /**
     * Updates the tip with new data.
     */
    private fun updateTip(result: CheckVersionJson) {
        val isForcedUpgrade = (result.forcedUpgradeFlag?.toInt() ?: 0) == 1 // 1: 强制Upgrade
        val description = getDescription(result.softConfigOtherTypeVOList)
        val downPageUrl = result.downloadPageUrl
        val sizeStr = "${result.notUnZipSize}MB"

        XLog.i("有versionUpgrade,Upgradeinfo: $description, 是否强制Upgrade: $isForcedUpgrade")

        val versionUpData =
            VersionUpData(
                versionNo = result.versionNo ?: "",
                isForcedUpgrade = isForcedUpgrade,
                description = description,
                downPageUrl = downPageUrl,
                sizeStr = sizeStr,
            )
        updateLiveData.postValue(versionUpData)
    }

    /**
     * Get/RetrieveUpgradeinfo
     */
    private fun getDescription(list: List<SoftConfigOtherTypeVO>?): String {
        list?.forEach {
            if (it.descType == 3) {
                return it.textDescription
            }
        }
        return ""
    }
}
