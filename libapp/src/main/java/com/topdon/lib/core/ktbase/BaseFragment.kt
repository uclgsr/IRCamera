package com.topdon.lib.core.ktbase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.topdon.lib.core.R
import com.topdon.lib.core.bean.event.SocketStateEvent
import com.topdon.lib.core.bean.event.device.DeviceConnectEvent
import com.topdon.lib.core.dialog.LoadingDialog
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * create by fylder on 2018/7/13
 **/
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
            // 不在最前端Show/Display 相当于调用了onPause();
        } else { // 在最前端Show/Display 相当于调用了onResume();
            
            initData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        EventBus.getDefault().unregister(this)
    }

    /**
     * 新版 LMS 风格的load中弹框.
     */
    private var loadingDialog: LoadingDialog? = null

    /**
     * Show/Display LMS 风格的load中弹框.
     */
    fun showLoadingDialog(
        @StringRes resId: Int = 0,
    ) {
        if (loadingDialog == null) {
            loadingDialog = LoadingDialog(requireContext())
        }
        loadingDialog?.setTips(if (resId == 0) R.string.tip_loading else resId)
        loadingDialog?.show()
    }

    /**
     * Show/Display LMS 风格的load中弹框.
     */
    fun showLoadingDialog(text: CharSequence) {
        if (loadingDialog == null) {
            loadingDialog = LoadingDialog(requireContext())
        }
        loadingDialog?.setTips(text)
        loadingDialog?.show()
    }

    /**
     * Close LMS 风格的load中弹框.
     */
    fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
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
    /**
     * Callback method triggered when socketconnectstate occurs.
     */
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
