// package com.infisense.usbir.view
//
// import android.content.Context
// import android.graphics.drawable.ColorDrawable
// import android.util.Log
// import android.view.Gravity
// import android.view.LayoutInflater
// import android.view.View
// import android.view.ViewGroup
// import android.widget.*
// import androidx.recyclerview.widget.LinearLayoutManager
// import com.infisense.iruvc.sdkisp.Libircmd
// import com.infisense.usbir.R
// import com.infisense.usbir.camera.IRUVC
// import kotlinx.android.synthetic.main.layout_tpd.view.*
//
//
// //长按[温度测量]弹出
// class PopuMenuTPD(context: Context?) {
//
//    private val popupWindow: PopupWindow
//
//    var usbcamera: IRUVC? = null
//    var param = CharArray(6)
//    private var layView: View
//
//    fun showheight(linearLayout: LinearLayout?, popupheight: Int) {
//        popupWindow.showAtLocation(linearLayout, Gravity.NO_GRAVITY, 0, popupheight)
//        if (usbcamera != null && usbcamera!!.uvcCamera != null) getParam()
//    }
//
//    private fun getParam() {
//        var i = 0
//        val value = CharArray(1)
//        while (i < 6) {
//            Libircmd.get_prop_tpd_params(i, value, usbcamera!!.uvcCamera.nativePtr)
//            param[i] = value[0]
//            i++
//        }
//    }
//
//    companion object {
//        private const val TAG = "PopuMenuTPD"
//        private val tpdtype = arrayOf("DISTANCE", "TU", "TA", "EMS", "TAU", "GAIN_SEL")
//    }
//
//    init {
//        layView = LayoutInflater.from(context).inflate(R.layout.layout_tpd, null)
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
//        val adapter =
//            ArrayAdapter(context!!, R.layout.support_simple_spinner_dropdown_item, tpdtype)
//        layView.Param_Sel!!.adapter = adapter
//        val spinner: AdapterView.OnItemSelectedListener =
//            object : AdapterView.OnItemSelectedListener {
//                override fun onItemSelected(
//                    adapterView: AdapterView<*>?,
//                    view: View,
//                    i: Int,
//                    l: Long
//                ) {
//                    layView.data!!.setText(param[i] + 0 + "")
//                }
//
//                override fun onNothingSelected(adapterView: AdapterView<*>?) {}
//            }
//        layView.Param_Sel!!.onItemSelectedListener = spinner
//        layView.submit!!.setOnClickListener {
//            Log.d(TAG, layView.Param_Sel!!.selectedItemPosition.toString())
//            if (layView.data!!.text.toString().isNotEmpty()) {
//                param[layView.Param_Sel!!.selectedItemPosition] =
//                    layView.data!!.text.toString().toInt().toChar()
//                Libircmd.set_prop_tpd_params(
//                    layView.Param_Sel!!.selectedItemPosition,
//                    layView.data!!.text.toString().toInt().toChar(), usbcamera!!.uvcCamera.nativePtr
//                )
//            }
//        }
//    }
// }
