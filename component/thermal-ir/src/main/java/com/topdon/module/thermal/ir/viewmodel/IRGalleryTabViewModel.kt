package com.topdon.module.thermal.ir.viewmodel

import androidx.lifecycle.MutableLiveData
import com.topdon.lib.core.ktbase.BaseViewModel

/**
 * Custom I r gallery tab view model view for thermal imaging display.
 * Provides specialized rendering and interaction capabilities.
 */
class IRGalleryTabViewModel : BaseViewModel() {
    /**
是否处于编辑mode.
     */
    val isEditModeLD: MutableLiveData<Boolean> = MutableLiveData(false)

    /**
当前selected数量.
     */
    val selectSizeLD: MutableLiveData<Int> = MutableLiveData(0)

    /**
click全选的 Fragment index，如 0 表示photo全选，1表示video全选.
     */
    val selectAllIndex: MutableLiveData<Int> = MutableLiveData(0)
}
