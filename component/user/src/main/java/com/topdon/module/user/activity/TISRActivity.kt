package com.topdon.module.user.activity

import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.topdon.lib.core.bean.event.SocketMsgEvent
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.lib.core.repository.TS004Repository
import com.topdon.lib.core.socket.SocketCmdUtil
import com.topdon.lib.core.utils.WsCmdConstants
import com.topdon.lib.core.view.TitleView
import com.topdon.lms.sdk.weiget.TToast
import com.topdon.module.user.R
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import com.topdon.lib.core.R as RCore

class TISRActivity : BaseActivity() {

    private lateinit var titleView: TitleView
    private lateinit var settingItemTisrSelect: SwitchCompat

    override fun initContentView() = R.layout.activity_tisr

    override fun initView() {

        titleView = findViewById(R.id.title_view)
        settingItemTisrSelect = findViewById(R.id.setting_item_tisr_select)

        titleView.setTitleText("TISR")
        settingItemTisrSelect.isChecked = SharedManager.is04TISR
        settingItemTisrSelect.setOnCheckedChangeListener { _, isChecked ->
            updateTISR(if (isChecked) 1 else 0)
            SharedManager.is04TISR = isChecked
        }
    }

    override fun initData() {
        lifecycleScope.launch {
            val tisrBean = TS004Repository.getTISR()
            if (tisrBean?.isSuccess()!!) {
                val isTISR = tisrBean.data?.enable!! == 1
                settingItemTisrSelect.isChecked = isTISR
                SharedManager.is04TISR = isTISR
            } else {
                TToast.shortToast(this@TISRActivity, RCore.string.operation_failed_tips)
            }
        }
    }

    private fun updateTISR(state: Int) {
        lifecycleScope.launch {
            val isSuccess = TS004Repository.setTISR(state)
            if (isSuccess) {
            } else {
                TToast.shortToast(this@TISRActivity, RCore.string.operation_failed_tips)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSocketMsgEvent(event: SocketMsgEvent) {
        when (SocketCmdUtil.getCmdResponse(event.text)) {
            WsCmdConstants.AR_COMMAND_TISR_GET -> { 
                try {
                    val webSocketIp = SocketCmdUtil.getIpResponse(event.text)
                    if (webSocketIp == WsCmdConstants.AR_COMMAND_IP) {
                        val data: JSONObject = JSONObject(event.text).getJSONObject("data")
                        val state: Int = data.getInt("state")
                        val isTISR = state == 1
                        settingItemTisrSelect.isChecked = isTISR
                        SharedManager.is04TISR = isTISR
                    }
                } catch (_: Exception) {
                }
            }
        }
    }
}
