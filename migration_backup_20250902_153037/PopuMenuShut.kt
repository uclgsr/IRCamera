// package com.infisense.usbir.view
//
// import android.content.Context
// import android.graphics.drawable.ColorDrawable
// import android.view.Gravity
// import android.view.LayoutInflater
// import android.view.View
// import android.view.ViewGroup
// import android.widget.*
// import androidx.recyclerview.widget.LinearLayoutManager
// import com.infisense.iruvc.sdkisp.Libircmd
// import com.infisense.usbir.R
// import com.infisense.usbir.camera.IRUVC
// import kotlinx.android.synthetic.main.layout_shut.view.*
//
// //长按相机弹出
// class PopuMenuShut(context: Context?) {
//
//    private val popupWindow: PopupWindow
//
//    var usbcamera: IRUVC? = null
//    var layView: View
//
//    fun showheight(linearLayout: LinearLayout?, popupheight: Int) {
//        popupWindow.showAtLocation(linearLayout, Gravity.NO_GRAVITY, 0, popupheight)
//        if (usbcamera != null && usbcamera!!.uvcCamera != null) shutParam
//    }
//
//    private val shutParam: Unit
//        private get() {
//            val mode = CharArray(1)
//            Libircmd.get_prop_auto_shutter_params(
//                Libircmd.SHUTTER_PROP_SWITCH,
//                mode,
//                usbcamera!!.uvcCamera.nativePtr
//            )
//            layView.automode!!.isChecked = mode[0] == 1.toChar()
//            Libircmd.get_prop_auto_shutter_params(
//                Libircmd.SHUTTER_PROP_MIN_INTERVAL,
//                mode,
//                usbcamera!!.uvcCamera.nativePtr
//            )
//            layView.min!!.setText(mode[0] + 0 + "")
//            Libircmd.get_prop_auto_shutter_params(
//                Libircmd.SHUTTER_PROP_MAX_INTERVAL,
//                mode,
//                usbcamera!!.uvcCamera.nativePtr
//            )
//            layView.max!!.setText(mode[0] + 0 + "")
//            Libircmd.get_prop_auto_shutter_params(
//                Libircmd.SHUTTER_PROP_TEMP_THRESHOLD_OOC,
//                mode,
//                usbcamera!!.uvcCamera.nativePtr
//            )
//            layView.ooc!!.setText(mode[0] + 0 + "")
//            Libircmd.get_prop_auto_shutter_params(
//                Libircmd.SHUTTER_PROP_TEMP_THRESHOLD_B,
//                mode,
//                usbcamera!!.uvcCamera.nativePtr
//            )
//            layView.b!!.setText(mode[0] + 0 + "")
//            layView.automode!!.setOnCheckedChangeListener { compoundButton, status ->
//                if (layView.min!!.text.toString().length != 0) Libircmd.set_prop_auto_shutter_params(
//                    Libircmd.SHUTTER_PROP_MIN_INTERVAL,
//                    layView.min!!.text.toString().toInt().toChar(), usbcamera!!.uvcCamera.nativePtr
//                )
//                if (layView.max!!.text.toString().length != 0) Libircmd.set_prop_auto_shutter_params(
//                    Libircmd.SHUTTER_PROP_MAX_INTERVAL,
//                    layView.max!!.text.toString().toInt().toChar(), usbcamera!!.uvcCamera.nativePtr
//                )
//                if (layView.ooc!!.text.toString().length != 0) Libircmd.set_prop_auto_shutter_params(
//                    Libircmd.SHUTTER_PROP_TEMP_THRESHOLD_OOC,
//                    layView.ooc!!.text.toString().toInt().toChar(), usbcamera!!.uvcCamera.nativePtr
//                )
//                if (layView.b!!.text.toString().length != 0) Libircmd.set_prop_auto_shutter_params(
//                    Libircmd.SHUTTER_PROP_TEMP_THRESHOLD_B,
//                    layView.b!!.text.toString().toInt().toChar(), usbcamera!!.uvcCamera.nativePtr
//                )
//                Libircmd.set_prop_auto_shutter_params(
//                    Libircmd.SHUTTER_PROP_SWITCH,
//                    (if (status) 1 else 0).toChar(), usbcamera!!.uvcCamera.nativePtr
//                )
//            }
//        }
//
//    companion object {
//        private const val TAG = "PopuMenuISP"
//    }
//
//    init {
//        layView = LayoutInflater.from(context).inflate(R.layout.layout_shut, null)
// //        ButterKnife.bind(this, view)
//        popupWindow = PopupWindow(layView)
//        popupWindow.width = ViewGroup.LayoutParams.MATCH_PARENT
//        popupWindow.height = ViewGroup.LayoutParams.WRAP_CONTENT
//        popupWindow.isFocusable = true
//        popupWindow.isOutsideTouchable = false
//        popupWindow.setBackgroundDrawable(ColorDrawable(0x00000000)) // 解决 7.0 手机，点击外部不消失
//        popupWindow.animationStyle = R.style.contextMenuAnim
//        layView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
//        //创建布局管理
//        val layoutManager = LinearLayoutManager(context)
//        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
//    }
// }
