package com.mpdc4gsr.module.user.fragment


import android.content.Intent
import android.graphics.BitmapFactory
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.CleanUtils
import com.blankj.utilcode.util.SizeUtils
import com.bumptech.glide.request.RequestOptions
import com.elvishew.xlog.XLog
import com.google.gson.Gson
import com.mpdc4gsr.lib.core.BaseApplication
import com.mpdc4gsr.lib.core.bean.event.PDFEvent
import com.mpdc4gsr.lib.core.bean.event.WinterClickEvent
import com.mpdc4gsr.lib.core.bean.response.ResponseUserInfo
import com.mpdc4gsr.lib.core.common.SharedManager
import com.mpdc4gsr.lib.core.common.UserInfoManager
import com.mpdc4gsr.lib.core.config.ExtraKeyConfig
import com.mpdc4gsr.lib.core.config.RouterConfig
import com.mpdc4gsr.lib.core.db.AppDatabase
import com.mpdc4gsr.lib.core.dialog.TipDialog
import com.mpdc4gsr.lib.core.ktbase.BaseFragment
import com.mpdc4gsr.lib.core.navigation.NavigationManager
import com.mpdc4gsr.lib.core.socket.WebSocketProxy
import com.mpdc4gsr.lib.core.tools.GlideLoader
import com.mpdc4gsr.lib.core.utils.Constants
import com.mpdc4gsr.lib.core.utils.NetWorkUtils
import com.mpdc4gsr.lms.sdk.LMS
import com.mpdc4gsr.lms.sdk.UrlConstant
import com.mpdc4gsr.lms.sdk.bean.CommonBean
import com.mpdc4gsr.lms.sdk.bean.FeedBackBean
import com.mpdc4gsr.lms.sdk.feedback.activity.FeedbackActivity
import com.mpdc4gsr.commons.util.FolderUtil
import com.mpdc4gsr.module.user.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import com.mpdc4gsr.lib.core.R as LibAppR
import com.mpdc4gsr.lib.core.R as RCore


class MineFragment : BaseFragment(), View.OnClickListener {

    private var isNeedRefreshLogin = false

    private lateinit var ivWinter: ImageView
    private lateinit var settingItemVersion: View
    private lateinit var settingItemClear: View
    private lateinit var settingUserLay: View
    private lateinit var settingUserImgNight: ImageView
    private lateinit var settingUserText: TextView
    private lateinit var settingElectronicManual: View
    private lateinit var settingFaq: View
    private lateinit var settingFeedback: View
    private lateinit var settingItemUnit: View
    private lateinit var dragCustomerView: View
    private lateinit var viewWinterPoint: View

    override fun initContentView(): Int = R.layout.fragment_mine

    override fun initView() {

        ivWinter = requireView().findViewById(R.id.iv_winter)
        settingItemVersion = requireView().findViewById(R.id.setting_item_version)
        settingItemClear = requireView().findViewById(R.id.setting_item_clear)
        settingUserLay = requireView().findViewById(R.id.setting_user_lay)
        settingUserImgNight = requireView().findViewById(R.id.setting_user_img_night)
        settingUserText = requireView().findViewById(R.id.setting_user_text)
        settingElectronicManual = requireView().findViewById(R.id.setting_electronic_manual)
        settingFaq = requireView().findViewById(R.id.setting_faq)
        settingFeedback = requireView().findViewById(R.id.setting_feedback)
        settingItemUnit = requireView().findViewById(R.id.setting_item_unit)
        dragCustomerView = requireView().findViewById(R.id.drag_customer_view)
        viewWinterPoint = requireView().findViewById(R.id.view_winter_point)

        ivWinter.setOnClickListener(this)
        settingItemVersion.setOnClickListener(this)
        settingItemClear.setOnClickListener(this)
        settingUserLay.setOnClickListener(this)
        settingUserImgNight.setOnClickListener(this)
        settingUserText.setOnClickListener(this)
        settingElectronicManual.setOnClickListener(this)
        settingFaq.setOnClickListener(this)
        settingFeedback.setOnClickListener(this)
        settingItemUnit.setOnClickListener(this)
        dragCustomerView.setOnClickListener(this)

        viewWinterPoint.isVisible = !SharedManager.hasClickWinter

        if (BaseApplication.instance.isDomestic()) {

        }

        viewLifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {

                    if (WebSocketProxy.getInstance().isConnected()) {
                        NetWorkUtils.switchNetwork(false)
                    }
                }
            },
        )
    }

    override fun initData() {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun updatePDF(event: PDFEvent) {
        isNeedRefreshLogin = true
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onWinterClick(event: WinterClickEvent) {
        viewWinterPoint.isVisible = false
    }

    override fun onResume() {
        super.onResume()
        changeLoginStyle()
        if (isNeedRefreshLogin) {
            isNeedRefreshLogin = false
            checkLoginResult()
        }
    }


    override fun onClick(v: View?) {
        when (v) {
            ivWinter -> {
                viewWinterPoint.isVisible = false
                SharedManager.hasClickWinter = true
                EventBus.getDefault().post(WinterClickEvent())

                val url =
                    if (UrlConstant.BASE_URL == "https://api.topdon.com/") {
                        "https://app.topdon.com/h5/share/#/detectionGuidanceIndex?showHeader=1&" +
                                "languageId=1"
                    } else {
                        "http://172.16.66.77:8081/#/detectionGuidanceIndex?languageId=1&showHeader=1"
                    }

                NavigationManager.getInstance().build(RouterConfig.WEB_VIEW)
                    .withString(ExtraKeyConfig.URL, url)
                    .navigation(requireContext())
            }

            settingUserLay, settingUserImgNight -> {
                if (UserInfoManager.getInstance().isLogin()) {
                    isNeedRefreshLogin = true
                    LMS.getInstance().activityUserInfo(requireContext())
                } else {
                    loginAction()
                }
            }

            settingUserText -> {
                if (!LMS.getInstance().isLogin) {
                    loginAction()
                }
            }

            settingElectronicManual -> {
                NavigationManager.getInstance().build(
                    RouterConfig.ELECTRONIC_MANUAL,
                ).withInt(Constants.SETTING_TYPE, Constants.SETTING_BOOK)
                    .navigation(requireContext())
            }

            settingFaq -> {
                NavigationManager.getInstance().build(
                    RouterConfig.ELECTRONIC_MANUAL,
                ).withInt(Constants.SETTING_TYPE, Constants.SETTING_FAQ)
                    .navigation(requireContext())
            }

            settingFeedback -> {
                if (LMS.getInstance().isLogin) {
                    val devSn = SharedManager.getDeviceSn()
                    FeedBackBean().apply {
                        logPath = ""
                        sn = devSn
                        lastConnectSn = devSn
                        XLog.e("bcf", "sn $sn  logPath $logPath")
                    }.let { feedBackBean ->
                        val intent = Intent(requireContext(), FeedbackActivity::class.java)
                        intent.putExtra(FeedbackActivity.FEEDBACKBEAN, feedBackBean)
                        startActivity(intent)
                    }
                } else {
                    loginAction()
                }
            }

            settingItemUnit -> {
                NavigationManager.getInstance().build(RouterConfig.UNIT)
                    .navigation(requireContext())
            }

            settingItemVersion -> {
                NavigationManager.getInstance().build(RouterConfig.VERSION)
                    .navigation(requireContext())
            }

            settingItemClear -> {
                clearCache()
            }

            dragCustomerView -> {

                val sn = SharedManager.getDeviceSn()

                if (!TextUtils.isEmpty(sn)) {

                }


            }
        }
    }

    private fun loginAction() {
        isNeedRefreshLogin = true

        val bgBitmap = BitmapFactory.decodeResource(
            resources,
            LibAppR.mipmap.ic_default_user_head
        )
        LMS.getInstance().activityLogin(null, null, false, null, bgBitmap)
    }

    private fun checkLoginResult() {
        if (LMS.getInstance().isLogin) {

            LMS.getInstance().getUserInfo { userinfo: CommonBean ->
                try {
                    val json = userinfo.data
                    val infoData = Gson().fromJson(json, ResponseUserInfo::class.java)
                    UserInfoManager.getInstance().login(
                        token = LMS.getInstance().token,
                        userId = infoData.topdonId,
                        phone = infoData.phone,
                        email = infoData.email,
                        nickname = infoData.userName,
                        headUrl = infoData.avatar,
                    )

                    changeLoginStyle()
                } catch (e: Exception) {
                    XLog.e(" 登录异常: ${e.message}")
                }
            }
        } else {

            XLog.e(" 登录失败")
            changeLoginStyle()
            settingUserImgNight.setImageResource(LibAppR.mipmap.ic_default_user_head)
        }
    }

    private fun changeLoginStyle() {
        if (LMS.getInstance().isLogin) {
            val layoutParams =
                ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            layoutParams.startToEnd = R.id.setting_user_img_night
            layoutParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            layoutParams.marginStart = SizeUtils.dp2px(16f)
            layoutParams.marginEnd = SizeUtils.dp2px(16f)
            settingUserText.setPadding(0, 0, 0, 0)
            settingUserText.gravity = Gravity.LEFT
            settingUserText.layoutParams = layoutParams
            val drawable = ContextCompat.getDrawable(requireContext(), android.R.color.transparent)
            drawable!!.setBounds(0, 0, drawable.minimumWidth, drawable.minimumHeight)
            settingUserText.setCompoundDrawables(null, null, drawable, null)
            settingUserText.text = SharedManager.getNickname()
            val tvEmail = requireView().findViewById<TextView>(R.id.tv_email)
            tvEmail.text = SharedManager.getUsername()
            settingUserLay.visibility = View.VISIBLE

            if (settingUserImgNight != null) {
                GlideLoader.loadCircle(
                    settingUserImgNight,
                    SharedManager.getHeadIcon(),
                    LibAppR.mipmap.ic_default_user_head,
                    RequestOptions().optionalCircleCrop(),
                )
            }
        } else {
            val layoutParams =
                ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                )
            layoutParams.startToEnd = R.id.setting_user_img_night
            layoutParams.topToTop = R.id.setting_user_img_night
            layoutParams.bottomToBottom = R.id.setting_user_img_night
            settingUserText.setPadding(
                SizeUtils.dp2px(16f),
                SizeUtils.dp2px(16f),
                SizeUtils.dp2px(16f),
                SizeUtils.dp2px(16f)
            )
            settingUserText.gravity = Gravity.CENTER
            settingUserText.layoutParams = layoutParams
            settingUserText.setText(


                context?.getString(RCore.string.app_sign_in) ?: "Sign In",
            )
            val drawable = ContextCompat.getDrawable(requireContext(), R.mipmap.ic_arrow_login)
            drawable!!.setBounds(0, 0, drawable.minimumWidth, drawable.minimumHeight)
            settingUserText.setCompoundDrawables(null, null, drawable, null)
            settingUserLay.visibility = View.GONE
            val tvEmail = requireView().findViewById<TextView>(R.id.tv_email)
            tvEmail.text = ""
            settingUserImgNight.setImageResource(LibAppR.mipmap.ic_default_user_head)
        }
    }

    private fun clearCache() {
        lifecycleScope.launch {
            showLoadingDialog()
            withContext(Dispatchers.IO) {
                try {
                    AppDatabase.getInstance().thermalDao().deleteByUserId(SharedManager.getUserId())
                    CleanUtils.cleanExternalCache()
                } catch (e: Exception) {
                    XLog.w("清除缓存异常: ${e.message}")
                }
                delay(1000)
            }
            dismissLoadingDialog()
            delay(50)
            TipDialog.Builder(requireContext())
                .setMessage(RCore.string.clear_finish)
                .setCanceled(true)
                .create().show()
        }
    }
}
