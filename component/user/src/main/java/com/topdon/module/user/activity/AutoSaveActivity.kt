package com.topdon.module.user.activity

import androidx.appcompat.widget.SwitchCompat
import com.topdon.lib.core.common.SharedManager
import com.topdon.lib.core.ktbase.BaseActivity
import com.topdon.module.user.R

/**
自动save到手机
 */
// Legacy ARouter route annotation - now using NavigationManager
class AutoSaveActivity : BaseActivity() {

    // View references - migrated from synthetic views
    private lateinit var settingItemSaveSelect: SwitchCompat

    override fun initContentView() = R.layout.activity_auto_save

    override fun initView() {
        // Initialize views - migrated from synthetic views
        settingItemSaveSelect = findViewById(R.id.setting_item_save_select)

        settingItemSaveSelect.isChecked = SharedManager.is04AutoSync
        settingItemSaveSelect.setOnCheckedChangeListener { _, isChecked ->
            SharedManager.is04AutoSync = isChecked
        }
    }

    override fun initData() {
    }
}
