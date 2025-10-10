package com.mpdc4gsr.libunified.app.ktbase

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.compose.dialogs.LoadingDialogState
import com.mpdc4gsr.libunified.app.event.DeviceEventManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class BaseFragment : Fragment() {
    val TAG = BaseFragment::class.java.simpleName

    abstract fun initContentView(): Int

    abstract fun initView()

    abstract fun initData()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? = inflater.inflate(initContentView(), container, false)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        observeDeviceEvents()
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

    private fun observeDeviceEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            DeviceEventManager.deviceConnectionState.collectLatest { state ->
                state?.let {
                    if (it.isConnected) {
                        connected()
                    } else {
                        disConnected()
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            DeviceEventManager.socketConnectionState.collectLatest { state ->
                state?.let {
                    if (it.isConnected) {
                        onSocketConnected(it.isTS004)
                    } else {
                        onSocketDisConnected(it.isTS004)
                    }
                }
            }
        }
    }

    protected open fun connected() {
    }

    protected open fun disConnected() {
    }

    protected open fun onSocketConnected(isTS004: Boolean) {
    }

    protected open fun onSocketDisConnected(isTS004: Boolean) {
    }
}
