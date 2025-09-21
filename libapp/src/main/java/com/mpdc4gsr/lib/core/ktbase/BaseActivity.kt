package com.mpdc4gsr.lib.core.ktbase

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
import com.mpdc4gsr.lib.core.BaseApplication
import com.mpdc4gsr.lib.core.R
import com.mpdc4gsr.lib.core.bean.event.SocketStateEvent
import com.mpdc4gsr.lib.core.bean.event.device.DeviceConnectEvent
import com.mpdc4gsr.lib.core.bean.response.ResponseUserInfo
import com.mpdc4gsr.lib.core.common.SharedManager
import com.mpdc4gsr.lib.core.common.UserInfoManager
import com.mpdc4gsr.lib.core.dialog.LoadingDialog
import com.mpdc4gsr.lib.core.dialog.TipCameraProgressDialog
import com.mpdc4gsr.lib.core.lms.LMS
import com.mpdc4gsr.lib.core.lms.bean.CommonBean
import com.mpdc4gsr.lib.core.tools.AppLanguageUtils
import com.mpdc4gsr.lib.core.tools.ConstantLanguages
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

abstract class BaseActivity : AppCompatActivity() {
    val TAG = this.javaClass.simpleName

    protected abstract fun initContentView(): Int

    protected abstract fun initView()

    protected abstract fun initData()

    protected var savedInstanceState: Bundle? = null

    protected open fun isLockPortrait(): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BaseApplication.instance.activitys.add(this)
        this.savedInstanceState = savedInstanceState
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }

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
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        cameraDialog?.dismiss()
        super.onDestroy()
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        BaseApplication.instance.activitys.remove(this)
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
        Log.d("onSocketConnectState", "${event.isConnect}")
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

    private var loadingDialog: LoadingDialog? = null

    fun showLoadingDialog(
        @StringRes resId: Int = R.string.tip_loading,
    ) {
        showLoadingDialog(getString(resId))
    }

    fun showLoadingDialog(text: CharSequence?) {
        if (loadingDialog == null) {
            loadingDialog = LoadingDialog(this)
        }
        loadingDialog?.setTips(text)
        loadingDialog?.show()
    }

    fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
    }

    private var cameraDialog: TipCameraProgressDialog? = null

    fun showCameraLoading() {
        if (cameraDialog != null && cameraDialog!!.isShowing) {
            return
        }
        if (cameraDialog == null) {
            cameraDialog =
                TipCameraProgressDialog.Builder(this)
                    .setCanceleable(false)
                    .create()
        }
        try {
            if (!(isFinishing && isDestroyed)) {
                cameraDialog?.show()
            }
        } catch (e: Exception) {

            Log.e("[ph][ph][ph][ph][ph][ph]", e.message.toString())
        }
    }

    fun dismissCameraLoading() {
        if (cameraDialog != null && cameraDialog!!.isShowing) {
            cameraDialog?.dismiss()
        }
    }

    private fun synLogin() {
        if (this::class.java.simpleName == "MainActivity") {
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
