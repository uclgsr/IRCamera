package com.mpdc4gsr.commons.helper

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class PermissionsRequester {
    private val allPermissions: MutableList<String> = ArrayList<String>()
    private val refusedPermissions: MutableList<String?> = ArrayList<String?>()
    private var callback: Callback? = null
    private var activity: Activity? = null
    private var fragment: Fragment? = null
    private var checking = false

    constructor(activity: Activity) {
        this.activity = activity
    }

    constructor(fragment: Fragment) {
        this.fragment = fragment
    }

    fun setCallback(callback: Callback?) {
        this.callback = callback
    }

    fun checkAndRequest(permissions: MutableList<String?>) {
        if (checking) {
            return
        }
        refusedPermissions.clear()
        allPermissions.clear()
        allPermissions.addAll(permissions)
        checkPermissions(allPermissions, false)
    }

    fun hasPermissions(permissions: MutableList<String>): Boolean {
        return checkPermissions(permissions, true)
    }

    private fun checkPermissions(permissions: MutableList<String>, onlyCheck: Boolean): Boolean {
        val context = if (activity != null) activity else fragment!!.getContext()
        if (context == null) return false
        if (permissions.remove(Manifest.permission.WRITE_SETTINGS) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                if (!onlyCheck) {
                    val intent =
                        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + context.getPackageName()))
                    if (activity != null) {
                        activity!!.startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS)
                    } else {
                        fragment!!.startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS)
                    }
                    checking = true
                }
                return false
            }
        }
        if (permissions.remove(Manifest.permission.REQUEST_INSTALL_PACKAGES) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.getPackageManager().canRequestPackageInstalls()) {
                if (!onlyCheck) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + context.getPackageName())
                    )
                    if (activity != null) {
                        activity!!.startActivityForResult(intent, REQUEST_CODE_UNKNOWN_APP_SOURCES)
                    } else {
                        fragment!!.startActivityForResult(intent, REQUEST_CODE_UNKNOWN_APP_SOURCES)
                    }
                    checking = true
                }
                return false
            }
        }
        val needRequestPermissonList = findDeniedPermissions(permissions)
        if (onlyCheck) {
            return needRequestPermissonList.isEmpty()
        } else if (!needRequestPermissonList.isEmpty()) {
            if (activity != null) {
                ActivityCompat.requestPermissions(
                    activity!!,
                    needRequestPermissonList.toTypedArray<String?>(),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                fragment!!.requestPermissions(needRequestPermissonList.toTypedArray<String?>(), PERMISSION_REQUEST_CODE)
            }
            checking = true
            return false
        } else {
            if (callback != null) {
                callback!!.onRequestResult(refusedPermissions)
            }
            checking = false
            return true
        }
    }

    private fun findDeniedPermissions(permissions: MutableList<String>): MutableList<String?> {
        val needRequestPermissionList: MutableList<String?> = ArrayList<String?>()
        val activity = if (this.activity != null) this.activity else fragment!!.getActivity()
        if (activity != null) {
            for (perm in permissions) {
                if (ContextCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
                ) {
                    needRequestPermissionList.add(perm)
                }
            }
        }
        return needRequestPermissionList
    }

    fun onActivityResult(requestCode: Int) {
        val context = if (activity != null) activity else fragment!!.getContext()
        if (context == null) return
        if (requestCode == REQUEST_CODE_WRITE_SETTINGS && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                refusedPermissions.add(Manifest.permission.WRITE_SETTINGS)
            }
            checkPermissions(allPermissions, false)
        }
        if (requestCode == REQUEST_CODE_UNKNOWN_APP_SOURCES && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.getPackageManager().canRequestPackageInstalls()) {
                refusedPermissions.add(Manifest.permission.REQUEST_INSTALL_PACKAGES)
            }
            checkPermissions(allPermissions, false)
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (i in permissions.indices) {
                val permission = permissions[i]
                if (allPermissions.remove(permission) && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    refusedPermissions.add(permission)
                }
            }
            if (callback != null) {
                callback!!.onRequestResult(refusedPermissions)
            }
            checking = false
        }
    }

    interface Callback {
        fun onRequestResult(refusedPermissions: MutableList<String?>?)
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 10
        private const val REQUEST_CODE_WRITE_SETTINGS = 11
        private const val REQUEST_CODE_UNKNOWN_APP_SOURCES = 12
    }
}
