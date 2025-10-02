package com.mpdc4gsr.libunified.app.ktbase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.bean.event.SocketStateEvent
import com.mpdc4gsr.libunified.app.bean.event.device.DeviceConnectEvent
import com.mpdc4gsr.libunified.app.compose.dialogs.LoadingDialogState
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

abstract class BaseFragment : Fragment() {
    val TAG = BaseFragment::class.java.simpleName

    abstract fun initContentView(): Int

    abstract fun initView()

    abstract fun initData()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(initContentView(), container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        EventBus.getDefault().register(this)
        initView()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {

        } else {

            initData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        EventBus.getDefault().unregister(this)
    }

    private val loadingDialogState by lazy { LoadingDialogState(requireContext()) }

    fun showLoadingDialog(
        @StringRes resId: Int = 0,
    ) {
        val message = if (resId == 0) getString(R.string.tip_loading) else getString(resId)
        loadingDialogState.show(message)
    }

    fun showLoadingDialog(text: CharSequence) {
        loadingDialogState.show(text.toString())
    }

    fun dismissLoadingDialog() {
        loadingDialogState.dismiss()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun getConnectState(event: DeviceConnectEvent) {
        if (event.isConnect) {
            connected()
        } else {
            disConnected()
        }
    }

    protected open fun connected() {
    }

    protected open fun disConnected() {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSocketConnectState(event: SocketStateEvent) {
        if (event.isConnect) {
            onSocketConnected(event.isTS004)
        } else {
            onSocketDisConnected(event.isTS004)
        }
    }

    protected open fun onSocketConnected(isTS004: Boolean) {
    }

    protected open fun onSocketDisConnected(isTS004: Boolean) {
    }
}
