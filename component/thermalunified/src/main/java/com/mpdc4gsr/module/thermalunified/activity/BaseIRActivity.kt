package com.mpdc4gsr.module.thermalunified.activity

import com.mpdc4gsr.libunified.app.comm.bean.SaveSettingBean
import com.mpdc4gsr.libunified.app.ktbase.BaseActivity


abstract class BaseIRActivity : BaseActivity() {

    protected val saveSetBean = SaveSettingBean()
}
