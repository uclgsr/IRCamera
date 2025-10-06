package com.mpdc4gsr.libunified.app.ktbase
import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.preference.PreferenceManager
import com.mpdc4gsr.libunified.app.utils.NetWorkUtils
abstract class BaseWifiActivity : BaseActivity() {
    protected val permissionList by lazy {
        if (this.applicationInfo.targetSdkVersion >= 34) {
            listOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        } else if (this.applicationInfo.targetSdkVersion == 33) {
            mutableListOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        } else {
            mutableListOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= 29) {
            NetWorkUtils.switchNetwork(true)
        }
        super.onCreate(savedInstanceState)
        PreferenceManager.getDefaultSharedPreferences(this@BaseWifiActivity)
            .edit()
            .putBoolean("use-sw-codec", true)
            .apply()
        PreferenceManager.getDefaultSharedPreferences(this@BaseWifiActivity)
            .getBoolean("auto_audio", false)
    }
    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= 29) {
            NetWorkUtils.switchNetwork(true)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    override fun onPause() {
        super.onPause()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    override fun onStop() {
        super.onStop()
    }
}
