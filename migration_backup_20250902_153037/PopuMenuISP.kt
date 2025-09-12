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
// import com.infisense.usbir.activity.IRDisplayActivity
// import com.infisense.usbir.camera.IRUVC
// import kotlinx.android.synthetic.main.isp.view.*
//
//
// //图像参数
// class PopuMenuISP(context: Context, mainActivity: IRDisplayActivity) {
//
//    private val popupWindow: PopupWindow
//
//    var usbcamera: IRUVC? = null
//    var mainActivity: IRDisplayActivity
//
//    private val adapterm4: ArrayAdapter<String>
//    private val adapterm7: ArrayAdapter<String>
//    var layView: View
//
//    private val imageParam: Unit
//        private get() {
//            val mode = CharArray(1)
//            Libircmd.get_prop_image_params(
//                Libircmd.IMAGE_PROP_LEVEL_TNR,
//                mode,
//                usbcamera!!.uvcCamera.nativePtr
//            )
//            if (mode[0] < 4.toChar()) layView.TNR!!.setSelection(mode[0].code, true)
//            Libircmd.get_prop_image_params(
//                Libircmd.IMAGE_PROP_LEVEL_SNR,
//                mode,
//                usbcamera!!.uvcCamera.nativePtr
//            )
//            if (mode[0] < 4.toChar()) layView.SNR!!.setSelection(mode[0].code, true)
//            Libircmd.get_prop_image_params(
//                Libircmd.IMAGE_PROP_LEVEL_DDE,
//                mode,
//                usbcamera!!.uvcCamera.nativePtr
//            )
//            if (mode[0] < 7.toChar()) layView.DDE!!.setSelection(mode[0].code, true)
//            Libircmd.get_prop_image_params(
//                Libircmd.IMAGE_PROP_MODE_AGC,
//                mode,
//                usbcamera!!.uvcCamera.nativePtr
//            )
//            if (mode[0] < 4.toChar()) layView.AGC!!.setSelection(mode[0].code, true)
//            Libircmd.get_prop_image_params(
//                Libircmd.IMAGE_PROP_LEVEL_MAX_GAIN,
//                mode,
//                usbcamera!!.uvcCamera.nativePtr
//            )
//            layView.MAXGAIN!!.setText(mode[0] + 0 + "")
//            Libircmd.get_prop_image_params(
//                Libircmd.IMAGE_PROP_LEVEL_BOS,
//                mode,
//                usbcamera!!.uvcCamera.nativePtr
//            )
//            layView.BOS!!.setText(mode[0] + 0 + "")
//            Libircmd.get_prop_image_params(
//                Libircmd.IMAGE_PROP_LEVEL_CONTRAST,
//                mode,
//                usbcamera!!.uvcCamera.nativePtr
//            )
//            layView.CONTRAST!!.setText(mode[0] + 0 + "")
//            Libircmd.get_prop_image_params(
//                Libircmd.IMAGE_PROP_LEVEL_BRIGHTNESS,
//                mode,
//                usbcamera!!.uvcCamera.nativePtr
//            )
//            layView.BRIGHTNESS!!.setText(mode[0] + 0 + "")
//            Libircmd.get_prop_image_params(
//                Libircmd.IMAGE_PROP_ONOFF_AGC,
//                mode,
//                usbcamera!!.uvcCamera.nativePtr
//            )
//            layView.ONOFF_AGC!!.isChecked = mode[0] == 1.toChar()
//            val spinner: AdapterView.OnItemSelectedListener =
//                object : AdapterView.OnItemSelectedListener {
//                    override fun onItemSelected(
//                        adapterView: AdapterView<*>,
//                        view: View,
//                        i: Int,
//                        l: Long
//                    ) {
//                        when (adapterView.id) {
//                            R.id.TNR -> Libircmd.set_prop_image_params(
//                                Libircmd.IMAGE_PROP_LEVEL_TNR,
//                                i.toChar(), usbcamera!!.uvcCamera.nativePtr
//                            )
//                            R.id.SNR -> Libircmd.set_prop_image_params(
//                                Libircmd.IMAGE_PROP_LEVEL_SNR,
//                                i.toChar(), usbcamera!!.uvcCamera.nativePtr
//                            )
//                            R.id.DDE -> Libircmd.set_prop_image_params(
//                                Libircmd.IMAGE_PROP_LEVEL_DDE,
//                                i.toChar(), usbcamera!!.uvcCamera.nativePtr
//                            )
//                            R.id.AGC -> if (i < 3) {
//                                layView.MAXGAIN!!.visibility = View.INVISIBLE
//                                layView.MAXGAINtext!!.visibility = View.INVISIBLE
//                                layView.BOS!!.visibility = View.INVISIBLE
//                                layView.BOStext!!.visibility = View.INVISIBLE
//                                layView.setagc!!.visibility = View.INVISIBLE
//                                Libircmd.set_prop_image_params(
//                                    Libircmd.IMAGE_PROP_MODE_AGC,
//                                    i.toChar(), usbcamera!!.uvcCamera.nativePtr
//                                )
//                            } else {
//                                layView.MAXGAIN!!.visibility = View.VISIBLE
//                                layView.MAXGAINtext!!.visibility = View.VISIBLE
//                                layView.BOS!!.visibility = View.VISIBLE
//                                layView.BOStext!!.visibility = View.VISIBLE
//                                layView.setagc!!.visibility = View.VISIBLE
//                            }
//                        }
//                    }
//
//                    override fun onNothingSelected(adapterView: AdapterView<*>?) {}
//                }
//            layView.TNR!!.onItemSelectedListener = spinner
//            layView.SNR!!.onItemSelectedListener = spinner
//            layView.DDE!!.onItemSelectedListener = spinner
//            layView.AGC!!.onItemSelectedListener = spinner
//        }
//
//    fun showheight(linearLayout: LinearLayout?, popupheight: Int) {
//        popupWindow.showAtLocation(linearLayout, Gravity.NO_GRAVITY, 0, popupheight)
//        if (usbcamera != null && usbcamera!!.uvcCamera != null) imageParam
//    }
//
//    private fun showShortMsg(msg: String) {
//        Toast.makeText(layView.context, msg, Toast.LENGTH_SHORT).show()
//    }
//
//    companion object {
//        private const val TAG = "PopuMenuISP"
//        private val m4 = arrayOf("0", "1", "2", "3")
//        private val m7 = arrayOf("0", "1", "2", "3", "4", "5", "6")
//    }
//
//    init {
// //        adapterm4 = ArrayAdapter(context, R.layout.simple_spinner_item, m4)
// //        adapterm7 = ArrayAdapter(context, R.layout.simple_spinner_item, m7)
//        adapterm4 = ArrayAdapter(context, R.layout.support_simple_spinner_dropdown_item, m4)
//        adapterm7 = ArrayAdapter(context, R.layout.support_simple_spinner_dropdown_item, m7)
//        this.mainActivity = mainActivity
//        layView = LayoutInflater.from(context).inflate(R.layout.isp, null)
//        layView.TNR!!.adapter = adapterm4
//        layView.SNR!!.adapter = adapterm4
//        layView.DDE!!.adapter = adapterm7
//        layView.AGC!!.adapter = adapterm4
//        layView.ONOFF_AGC!!.setOnCheckedChangeListener { _, b ->
//            Libircmd.set_prop_image_params(
//                Libircmd.IMAGE_PROP_ONOFF_AGC,
//                (if (b) 1 else 0).toChar(), usbcamera!!.uvcCamera.nativePtr
//            )
//        }
//        val handler = View.OnClickListener { view ->
//            Log.d(TAG, "onViewClicked: " + view.id)
//            when (view.id) {
//                R.id.rotate -> mainActivity.setRotate(true)
//                R.id.derotate -> mainActivity.setRotate(false)
//                R.id.mirror -> Libircmd.set_prop_image_params(
//                    Libircmd.IMAGE_PROP_SEL_MIRROR_FLIP,
//                    Libircmd.ONLY_MIRROR.toChar(), usbcamera!!.uvcCamera.nativePtr
//                ) //ok
//                R.id.flip -> Libircmd.set_prop_image_params(
//                    Libircmd.IMAGE_PROP_SEL_MIRROR_FLIP,
//                    Libircmd.ONLY_FLIP.toChar(), usbcamera!!.uvcCamera.nativePtr
//                ) //ok
//                R.id.flip_mirror -> Libircmd.set_prop_image_params(
//                    Libircmd.IMAGE_PROP_SEL_MIRROR_FLIP,
//                    Libircmd.MIRROR_FLIP.toChar(), usbcamera!!.uvcCamera.nativePtr
//                ) //ok
//                R.id.none -> Libircmd.set_prop_image_params(
//                    Libircmd.IMAGE_PROP_SEL_MIRROR_FLIP,
//                    Libircmd.NO_MIRROR_FLIP.toChar(), usbcamera!!.uvcCamera.nativePtr
//                ) //ok
//                R.id.restore -> {
//                    //Libircmd.restore_default_cfg(Libircmd.DEF_CFG_PROP_PAGE,usbcamera.uvcCamera.nativePtr);
//                    Libircmd.load_prop_default_params(
//                        Libircmd.PROP_SEL_IMAGE,
//                        usbcamera!!.uvcCamera.nativePtr
//                    )
//                    imageParam
//                }
// //                R.id.more -> context.startActivity(Intent(context, ToolActivity::class.java))
//                R.id.zoomdown -> Libircmd.zoom_center_down(0, 2, usbcamera!!.uvcCamera.nativePtr)
//                R.id.zoomup -> Libircmd.zoom_center_up(0, 2, usbcamera!!.uvcCamera.nativePtr)
//                R.id.setagc -> {
//                    if (layView.MAXGAIN!!.text.toString()
//                            .isNotEmpty()
//                    ) Libircmd.set_prop_image_params(
//                        Libircmd.IMAGE_PROP_LEVEL_MAX_GAIN,
//                        layView.MAXGAIN!!.text.toString().toInt().toChar(),
//                        usbcamera!!.uvcCamera.nativePtr
//                    ) //ok
//                    if (layView.BOS!!.text.toString().isNotEmpty()) Libircmd.set_prop_image_params(
//                        Libircmd.IMAGE_PROP_LEVEL_BOS,
//                        layView.BOS!!.text.toString().toInt().toChar(),
//                        usbcamera!!.uvcCamera.nativePtr
//                    ) //ok
//                    Libircmd.set_prop_image_params(
//                        Libircmd.IMAGE_PROP_MODE_AGC,
//                        3.toChar(), usbcamera!!.uvcCamera.nativePtr
//                    )
//                }
//                R.id.setIR -> {
//                    if (layView.CONTRAST!!.text.toString()
//                            .isNotEmpty()
//                    ) Libircmd.set_prop_image_params(
//                        Libircmd.IMAGE_PROP_LEVEL_CONTRAST,
//                        layView.CONTRAST!!.text.toString().toInt().toChar(),
//                        usbcamera!!.uvcCamera.nativePtr
//                    ) //ok
//                    if (layView.BRIGHTNESS!!.text.toString()
//                            .isNotEmpty()
//                    ) Libircmd.set_prop_image_params(
//                        Libircmd.IMAGE_PROP_LEVEL_BRIGHTNESS,
//                        layView.BRIGHTNESS!!.text.toString().toInt().toChar(),
//                        usbcamera!!.uvcCamera.nativePtr
//                    ) //ok
//                }
//            }
//        }
//        layView.rotate!!.setOnClickListener(handler)
//        layView.derotate!!.setOnClickListener(handler)
//        layView.mirror!!.setOnClickListener(handler)
//        layView.flip!!.setOnClickListener(handler)
//        layView.flip_mirror!!.setOnClickListener(handler)
//        layView.zoomdown!!.setOnClickListener(handler)
//        layView.zoomup!!.setOnClickListener(handler)
//        layView.none!!.setOnClickListener(handler)
//        layView.restore!!.setOnClickListener(handler)
//        layView.more!!.setOnClickListener(handler)
//        layView.setIR!!.setOnClickListener(handler)
//        layView.setagc!!.setOnClickListener(handler)
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
