package com.mpdc4gsr.module.user.activity

import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.lib.core.bean.event.SocketMsgEvent
import com.mpdc4gsr.lib.core.common.SharedManager
import com.mpdc4gsr.lib.core.ktbase.BaseActivity
// TS004Repository functionality removed
// import com.mpdc4gsr.lib.core.repository.TS004Repository
import com.mpdc4gsr.lib.core.socket.SocketCmdUtil
import com.mpdc4gsr.lib.core.utils.WsCmdConstants
import com.mpdc4gsr.lib.core.view.TitleView
import com.mpdc4gsr.lib.core.lms.weiget.TToast
import com.mpdc4gsr.module.user.R
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import com.mpdc4gsr.lib.core.R as RCore

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
            // TS004Repository functionality removed - set default TISR state
            settingItemTisrSelect.isChecked = false
            SharedManager.is04TISR = false
            TToast.shortToast(this@TISRActivity, RCore.string.operation_failed_tips)
        }
    }

    private fun updateTISR(state: Int) {
        lifecycleScope.launch {
            val isSuccess = false
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
