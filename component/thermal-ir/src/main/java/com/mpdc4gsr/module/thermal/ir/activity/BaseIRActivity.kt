package com.mpdc4gsr.module.thermal.ir.activity

import com.mpdc4gsr.lib.core.comm.bean.SaveSettingBean
import com.mpdc4gsr.lib.core.ktbase.BaseActivity


abstract class BaseIRActivity : BaseActivity() {

    protected val saveSetBean = SaveSettingBean()
}
