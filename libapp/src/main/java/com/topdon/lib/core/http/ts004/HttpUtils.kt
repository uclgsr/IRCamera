package com.topdon.lib.core.http.ts004
import com.topdon.lms.sdk.xutils.common.Callback
import com.topdon.lms.sdk.xutils.http.RequestParams
import com.topdon.lms.sdk.xutils.x

object HttpUtils {
    /**
     * settingspseudo color样式
     * @param mode              pseudo color样式
     * @param iResponseCallback Callbackfunction
     * @ void
     */
    fun setPseudoColor(
        mode: Int,
        iResponseCallback: Callback.CommonCallback<String>?,
    ) {
        val params = RequestParams()
        params.addBodyParameter("enable", false)
        params.addBodyParameter("mode", mode)
        params.uri = TS004URL.SET_PSEUDO_COLOR
        params.isAsJsonContent = true
        x.http().post(params, iResponseCallback!!)
    }

    /**
     * Get/Retrievepseudo color样式
     */
    fun getPseudoColor(iResponseCallback: Callback.CommonCallback<String>?) {
        val params = RequestParams()
        params.uri = TS004URL.GET_PSEUDO_COLOR
        params.isAsJsonContent = true
        x.http().post(params, iResponseCallback!!)
    }

    /**
     * settings屏幕brightness
     * @param mode              屏幕brightness值:range0-100
     * @param iResponseCallback Callbackfunction
     * @ void
     */
    fun setBrightness(
        brightness: Int,
        iResponseCallback: Callback.CommonCallback<String>?,
    ) {
        val params = RequestParams()
        params.addBodyParameter("brightness", brightness)
        params.uri = TS004URL.SET_PANEL_PARAM
        params.isAsJsonContent = true
        x.http().post(params, iResponseCallback!!)
    }

    /**
     * Get/Retrieve屏幕brightness
     */
    fun getBrightness(iResponseCallback: Callback.CommonCallback<String>?) {
        val params = RequestParams()
        params.uri = TS004URL.GET_PANEL_PARAM
        params.isAsJsonContent = true
        x.http().post(params, iResponseCallback!!)
    }

    /**
     * settingspicture-in-picture
     * @param iResponseCallback Callbackfunction
     * @ void
     */
    fun setPip(
        enable: Boolean,
        iResponseCallback: Callback.CommonCallback<String>?,
    ) {
        val params = RequestParams()
        params.addBodyParameter("enable", enable)
        params.uri = TS004URL.SET_PIP
        params.isAsJsonContent = true
        x.http().post(params, iResponseCallback!!)
    }

    /**
     * Get/Retrievepicture-in-picture
     */
    fun getPip(iResponseCallback: Callback.CommonCallback<String>?) {
        val params = RequestParams()
        params.uri = TS004URL.GET_PIP
        params.isAsJsonContent = true
        x.http().post(params, iResponseCallback!!)
    }

    /**
     * settings放大倍数
     * @param factor            放大倍数:1,2,4,8
     * @param iResponseCallback Callbackfunction
     * @ void
     */
    fun setZoom(
        factor: Int,
        iResponseCallback: Callback.CommonCallback<String>?,
    ) {
        val params = RequestParams()
        params.addBodyParameter("enable", true)
        params.addBodyParameter("factor", factor)
        params.uri = TS004URL.SET_ZOOM
        params.isAsJsonContent = true
        x.http().post(params, iResponseCallback!!)
    }

    /**
     * Get/Retrieve放大倍数
     */
    fun getZoom(iResponseCallback: Callback.CommonCallback<String>?) {
        val params = RequestParams()
        params.uri = TS004URL.GET_ZOOM
        params.isAsJsonContent = true
        x.http().post(params, iResponseCallback!!)
    }

    /**
     * settingscapture
     * @param iResponseCallback Callbackfunction
     * @void
     */
    fun setCamera(iResponseCallback: Callback.CommonCallback<String>?) {
        val params = RequestParams()
        params.uri = TS004URL.SET_SNAPSHOT
        params.isAsJsonContent = true
        x.http().post(params, iResponseCallback!!)
    }

    /**
     * settingsrecording
     * @param enable recording开关
     * @param iResponseCallback Callbackfunction
     * @void
     */
    fun setVideo(
        enable: Boolean,
        iResponseCallback: Callback.CommonCallback<String>?,
    ) {
        val params = RequestParams()
        params.addBodyParameter("enable", enable)
        params.uri = TS004URL.GET_VRECORD
        params.isAsJsonContent = true
        x.http().post(params, iResponseCallback!!)
    }

    /**
     * Get/Retrieverecordingstate
     * @param iResponseCallback Callbackfunction
     * @void
     */
    fun getVideoStatus(iResponseCallback: Callback.CommonCallback<String>?) {
        val params = RequestParams()
        params.uri = TS004URL.GET_RECORD_STATUS
        params.isAsJsonContent = true
        x.http().post(params, iResponseCallback!!)
    }

    /**
     * Get/Retrieveversioninfo
     */
    fun getVersion(iResponseCallback: Callback.CommonCallback<String>?) {
        val params = RequestParams()
        params.uri = TS004URL.GET_VERSION
        params.isAsJsonContent = true
        x.http().post(params, iResponseCallback!!)
    }

    /**
     * Get/Retrievedeviceinfo
     */
    fun getDeviceDetails(iResponseCallback: Callback.CommonCallback<String>?) {
        val params = RequestParams()
        params.uri = TS004URL.GET_DEVICE_DETAILS
        params.isAsJsonContent = true
        x.http().post(params, iResponseCallback!!)
    }

    /**
     * Get/Retrievestorage分区info
     */
    fun getFreeSpace(iResponseCallback: Callback.CommonCallback<String>?) {
        val params = RequestParams()
        params.uri = TS004URL.GET_FREE_SPACE
        params.isAsJsonContent = true
        x.http().post(params, iResponseCallback!!)
    }

    /**
     * restore出厂settings
     */
    fun getResetAll(iResponseCallback: Callback.CommonCallback<String>?) {
        val params = RequestParams()
        params.uri = TS004URL.GET_RESET_ALL
        params.isAsJsonContent = true
        x.http().post(params, iResponseCallback!!)
    }
}
