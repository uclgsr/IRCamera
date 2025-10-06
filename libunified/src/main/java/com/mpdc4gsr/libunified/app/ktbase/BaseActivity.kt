package com.mpdc4gsr.libunified.app.ktbase

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.elvishew.xlog.XLog
import com.google.gson.Gson
import com.mpdc4gsr.libunified.R
import com.mpdc4gsr.libunified.app.BaseApplication
import com.mpdc4gsr.libunified.app.bean.response.ResponseUserInfo
import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.common.UserInfoManager
import com.mpdc4gsr.libunified.app.compose.dialogs.LoadingDialogState
import com.mpdc4gsr.libunified.app.compose.dialogs.ProgressDialogState
import com.mpdc4gsr.libunified.app.event.DeviceEventManager
import com.mpdc4gsr.libunified.app.lms.LMS
import com.mpdc4gsr.libunified.app.lms.bean.CommonBean
import com.mpdc4gsr.libunified.app.tools.AppLanguageUtils
import com.mpdc4gsr.libunified.app.tools.ConstantLanguages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

abstract class BaseActivity : AppCompatActivity() {
    val TAG = this.javaClass.simpleName
    protected abstract fun initContentView(): Int
    protected abstract fun initView()
    protected abstract fun initData()
    protected var savedInstanceState: Bundle? = null
    protected open fun isLockPortrait(): Boolean = true
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BaseApplication.instance.activitys.add(this)
        this.savedInstanceState = savedInstanceState
        observeDeviceEvents()
        if (isLockPortrait()) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        @Suppress("DEPRECATION")
        window.navigationBarColor = ContextCompat.getColor(this, R.color.toolbar_16131E)
        setContentView(initContentView())
        initView()
        initData()
        synLogin()
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(
            AppLanguageUtils.attachBaseContext(
                newBase,
                ConstantLanguages.ENGLISH
            )
        )
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        cameraDialogState.dismiss()
        super.onDestroy()
        activityScope.cancel()
        BaseApplication.instance.activitys.remove(this)
    }

    private fun observeDeviceEvents() {
        activityScope.launch {
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
        activityScope.launch {
            DeviceEventManager.socketConnectionState.collectLatest { state ->
                state?.let {
                    Log.d("onSocketConnectState", "${it.isConnected}")
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

    private val loadingDialogState by lazy { LoadingDialogState(this) }
    fun showLoadingDialog(
        @StringRes resId: Int = R.string.tip_loading,
    ) {
        showLoadingDialog(getString(resId))
    }

    fun showLoadingDialog(text: CharSequence?) {
        loadingDialogState.show(text?.toString() ?: "")
    }

    fun dismissLoadingDialog() {
        loadingDialogState.dismiss()
    }

    private val cameraDialogState by lazy { ProgressDialogState(this) }
    fun showCameraLoading() {
        try {
            if (!(isFinishing && isDestroyed)) {
                cameraDialogState.show(
                    message = getString(R.string.tip_loading),
                    progress = -1f,
                    cancelable = false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing camera loading: ${e.message}")
        }
    }

    fun dismissCameraLoading() {
        cameraDialogState.dismiss()
    }

    private fun synLogin() {
        if (this.javaClass.simpleName == "MainComposeActivity") {
            LMS.getInstance().syncUserInfo()
        }
        if (SharedManager.getHasShowClause() && LMS.getInstance().isLogin) {
            LMS.getInstance().getUserInfo { userinfo: CommonBean ->
                try {
                    val infoData = Gson().fromJson(userinfo.data, ResponseUserInfo::class.java)
                    UserInfoManager.getInstance().login(
                        token = LMS.getInstance().token,
                        userId = infoData.topdonId,
                        phone = infoData.phone,
                        email = infoData.email,
                        nickname = infoData.userName,
                        headUrl = infoData.avatar,
                    )
                } catch (e: Exception) {
                    XLog.e("login error:${e.message}")
                }
            }
        } else {
            if (UserInfoManager.getInstance().isLogin()) {
                UserInfoManager.getInstance().logout()
            }
        }
    }

    protected class TakePhotoResult : ActivityResultContract<File, File?>() {
        private lateinit var file: File
        override fun createIntent(
            context: Context,
            input: File,
        ): Intent {
            file = input
            val uri =
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            return Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, uri)
        }

        override fun parseResult(
            resultCode: Int,
            intent: Intent?,
        ): File? = if (resultCode == RESULT_OK) file else null
    }
}
