package com.mpdc4gsr.module.thermal.ir.activity

import android.annotation.SuppressLint
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ToastUtils
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.mpdc4gsr.lib.core.BaseApplication
import com.mpdc4gsr.lib.core.bean.ContinuousBean
import com.mpdc4gsr.lib.core.bean.WatermarkBean
import com.mpdc4gsr.lib.core.common.SharedManager
import com.mpdc4gsr.lib.core.dialog.TipDialog
import com.mpdc4gsr.lib.core.ktbase.BaseActivity
import com.mpdc4gsr.lib.core.tools.TimeTool
import com.mpdc4gsr.lib.core.utils.CommUtils
import com.topdon.lib.ui.listener.SingleClickListener
import com.topdon.lib.ui.widget.BarPickView
import com.topdon.module.thermal.ir.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import com.mpdc4gsr.lib.core.R as LibR




class IRCameraSettingActivity : BaseActivity() {
    companion object {
        const val KEY_PRODUCT_TYPE = "key_product_type"
    }

    private var locationManager: LocationManager? = null
    private var locationProvider: String? = null

    private lateinit var tvAddress: TextView
    private lateinit var edAddress: EditText

    private var watermarkBean: WatermarkBean = SharedManager.watermarkBean
    private var continuousBean: ContinuousBean = SharedManager.continuousBean
    private var productName = ""

    private val permissionList =
        listOf(
            Permission.ACCESS_FINE_LOCATION,
            Permission.ACCESS_COARSE_LOCATION,
        )

    override fun initContentView(): Int = R.layout.activity_ir_camera_setting

    override fun initView() {
        productName = intent.getStringExtra(KEY_PRODUCT_TYPE) ?: ""
        if (isTC007()) {
            watermarkBean = SharedManager.wifiWatermarkBean 
            continuousBean = SharedManager.continuousBean
        } else {
            watermarkBean = SharedManager.watermarkBean
            continuousBean = SharedManager.continuousBean
        }

        val barPickViewTime = findViewById<BarPickView>(R.id.bar_pick_view_time)
        val barPickViewCount = findViewById<BarPickView>(R.id.bar_pick_view_count)
        val switchTime = findViewById<Switch>(R.id.switch_time)
        val switchWatermark = findViewById<Switch>(R.id.switch_watermark)
        val switchDelay = findViewById<Switch>(R.id.switch_delay)
        val clDelayMore =
            findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.cl_delay_more)
        val clWatermarkMore =
            findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.cl_watermark_more)
        val clShowEp =
            findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.cl_show_ep)
        val tvTimeShow = findViewById<TextView>(R.id.tv_time_show)
        tvAddress = findViewById(R.id.tv_address)
        val edTitle = findViewById<EditText>(R.id.ed_title)
        edAddress = findViewById(R.id.ed_address)
        val tvTitleShow = findViewById<android.widget.TextView>(R.id.tv_title_show)
        val imgLocation = findViewById<android.widget.ImageView>(R.id.img_location)
        val lyAuto = findViewById<android.widget.LinearLayout>(R.id.ly_auto)

        barPickViewTime.setProgressAndRefresh((continuousBean.continuaTime / 100).toInt())
        barPickViewTime.onStopTrackingTouch = { progress, _ ->
            continuousBean.continuaTime = progress.toLong() * 100
            SharedManager.continuousBean = continuousBean
        }
        barPickViewTime.valueFormatListener = {
            (it / 10).toString() + if (it % 10 == 0) "" else ("." + (it % 10).toString())
        }

        barPickViewCount.setProgressAndRefresh(continuousBean.count)
        barPickViewCount.onStopTrackingTouch = { progress, _ ->
            continuousBean.count = progress
            SharedManager.continuousBean = continuousBean
        }

        switchTime.isChecked = watermarkBean.isAddTime
        switchWatermark.isChecked = watermarkBean.isOpen
        switchDelay.isChecked = continuousBean.isOpen

        clDelayMore.isVisible = continuousBean.isOpen
        clWatermarkMore.isVisible = watermarkBean.isOpen
        clShowEp.isVisible = watermarkBean.isOpen

        tvTimeShow.text = TimeTool.getNowTime()
        tvTimeShow.isVisible = watermarkBean.isAddTime

        tvAddress.inputType = InputType.TYPE_NULL
        if (TextUtils.isEmpty(watermarkBean.address)) {
            tvAddress.visibility = View.GONE
        } else {
            tvAddress.visibility = View.VISIBLE
            tvAddress.text = watermarkBean.address
        }
        edTitle.setText(watermarkBean.title)
        edAddress.setText(watermarkBean.address)
        tvTitleShow.text = watermarkBean.title
        switchDelay.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                clDelayMore.visibility = View.VISIBLE
            } else {
                clDelayMore.visibility = View.GONE
            }
            continuousBean.isOpen = isChecked
            SharedManager.continuousBean = continuousBean
        }
        switchWatermark.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                clWatermarkMore.visibility = View.VISIBLE
                clShowEp.visibility = View.VISIBLE
            } else {
                clWatermarkMore.visibility = View.GONE
                clShowEp.visibility = View.GONE
            }
            watermarkBean.isOpen = isChecked
        }
        switchTime.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                tvTimeShow.text = TimeTool.getNowTime()
                tvTimeShow.visibility = View.VISIBLE
            } else {
                tvTimeShow.visibility = View.GONE
            }
            watermarkBean.isAddTime = isChecked
        }
        edTitle.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
                ) {
                }

                override fun afterTextChanged(s: Editable?) {
                    watermarkBean.title = edTitle.text.toString()
                    tvTitleShow.text = watermarkBean.title
                }
            },
        )
        edAddress.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
                ) {
                }

                override fun afterTextChanged(s: Editable?) {
                    watermarkBean.address = edAddress.text.toString()
                    tvAddress.text = watermarkBean.address
                    if (!watermarkBean.address.isNullOrEmpty()) {
                        tvAddress.visibility = View.VISIBLE
                    } else {
                        tvAddress.visibility = View.GONE
                    }
                }
            },
        )
        imgLocation.setOnClickListener(
            object : SingleClickListener() {
                override fun onSingleClick() {
                    checkStoragePermission()
                }
            },
        )

        lyAuto.visibility = if (isTC007()) View.GONE else View.VISIBLE
    }

    fun isTC007(): Boolean {
        return productName.contains("TC007")
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(): String? {

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        val providers = locationManager?.getProviders(true)
        locationProvider =
            if (providers!!.contains(LocationManager.GPS_PROVIDER)) {

                LocationManager.GPS_PROVIDER
            } else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {

                LocationManager.NETWORK_PROVIDER
            } else {
                return null
            }
        var location = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (location == null) {
            location = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }
        return if (location == null) {
            null
        } else {
            getAddress(location)
        }
    }

    var locationListener: LocationListener =
        object : LocationListener {

            override fun onStatusChanged(
                provider: String,
                status: Int,
                extras: Bundle,
            ) {
                Toast.makeText(
                    this@IRCameraSettingActivity,
                    provider,
                    Toast.LENGTH_SHORT,
                ).show()
            }

            override fun onProviderEnabled(provider: String) {
                Toast.makeText(
                    this@IRCameraSettingActivity,
                    "GPS打开",
                    Toast.LENGTH_SHORT,
                ).show()
                getLocation()
            }

            override fun onProviderDisabled(provider: String) {
                Toast.makeText(
                    this@IRCameraSettingActivity,
                    "GPS关闭",
                    Toast.LENGTH_SHORT,
                ).show()
            }

            override fun onLocationChanged(location: Location) {
                if (location != null) {

                    Toast.makeText(
                        this@IRCameraSettingActivity,
                        location.longitude.toString() + " " +
                                location.latitude + "",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(): Location? {
        locationManager = applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager
        val providers: List<String> = locationManager!!.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            val l: Location = locationManager!!.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || l.accuracy < bestLocation.accuracy) {

                bestLocation = l
            }
        }
        return bestLocation
    }

    private fun getAddress(location: Location?): String {
        var result: List<Address?>? = null
        try {
            if (location != null) {
                val gc = Geocoder(this, Locale.getDefault())
                result =
                    gc.getFromLocation(
                        location.latitude,
                        location.longitude, 1,
                    )
                Log.v("TAG", "获取地址信息：$result")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        var str = ""
        if (result != null && result.isNotEmpty()) {
            result?.get(0)?.let {
                str += getNullString(it.adminArea)
                if (TextUtils.isEmpty(it.subLocality) && !str.contains(getNullString(it.subAdminArea))) {
                    str += getNullString(it.subAdminArea)
                }
                if (!str.contains(getNullString(it.locality))) {
                    str += getNullString(it.locality)
                }
                if (!str.contains(getNullString(it.subLocality))) {
                    str += getNullString(it.subLocality)
                }
            }
        }
        return str
    }

    private fun getNullString(str: String?): String {
        return if (str.isNullOrEmpty()) {
            ""
        } else {
            str
        }
    }

    override fun onPause() {
        super.onPause()
        if (isTC007()) {
            SharedManager.wifiWatermarkBean = watermarkBean
        } else {
            SharedManager.watermarkBean = watermarkBean
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun initData() {
    }

    private fun initLocationPermission() {
        XXPermissions.with(this@IRCameraSettingActivity)
            .permission(
                permissionList,
            ).request(
                object : OnPermissionCallback {
                    override fun onGranted(
                        permissions: MutableList<String>,
                        all: Boolean,
                    ) {
                        if (all) {
                            showLoadingDialog(LibR.string.get_current_address)
                            lifecycleScope.launch {
                                var addressText: String? = ""
                                withContext(Dispatchers.IO) {
                                    addressText = getLocation()
                                }
                                dismissLoadingDialog()
                                if (addressText == null) {
                                    ToastUtils.showShort(LibR.string.get_Location_failed)
                                } else {
                                    watermarkBean.address = addressText as String
                                    edAddress.setText(addressText)
                                    tvAddress.visibility = View.VISIBLE
                                    tvAddress.setText(addressText)
                                }
                            }
                        } else {
                            ToastUtils.showShort(LibR.string.scan_ble_tip_authorize)
                        }
                    }

                    override fun onDenied(
                        permissions: MutableList<String>,
                        never: Boolean,
                    ) {
                        if (never) {

                            if (BaseApplication.instance.isDomestic()) {
                                ToastUtils.showShort(getString(LibR.string.app_location_content))
                            } else {
                                TipDialog.Builder(this@IRCameraSettingActivity)
                                    .setTitleMessage(getString(LibR.string.app_tip))
                                    .setMessage(getString(LibR.string.app_location_content))
                                    .setPositiveListener(LibR.string.app_open) {
                                        XXPermissions.startPermissionActivity(
                                            this@IRCameraSettingActivity,
                                            permissions
                                        )
                                    }
                                    .setCancelListener(LibR.string.app_cancel) {
                                    }
                                    .setCanceled(true)
                                    .create().show()
                            }
                        } else {
                            ToastUtils.showShort(LibR.string.scan_ble_tip_authorize)
                        }
                    }
                },
            )
    }

    private fun checkStoragePermission() {
        if (!XXPermissions.isGranted(this, permissionList)) {
            if (BaseApplication.instance.isDomestic()) {
                TipDialog.Builder(this)
                    .setMessage(
                        getString(
                            LibR.string.permission_request_location_app,
                            CommUtils.getAppName()
                        )
                    )
                    .setCancelListener(LibR.string.app_cancel)
                    .setPositiveListener(LibR.string.app_confirm) {
                        initLocationPermission()
                    }
                    .create().show()
            } else {
                initLocationPermission()
            }
        } else {
            initLocationPermission()
        }
    }
}
