package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.component.shared.app.ktbase.BaseViewModel
import com.mpdc4gsr.component.shared.app.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ElectronicManualViewModel : BaseViewModel() {
    data class ManualOption(
        val name: String,
        val isTS001: Boolean,
    )

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()
    private val _options = MutableStateFlow<List<ManualOption>>(emptyList())
    val options: StateFlow<List<ManualOption>> = _options.asStateFlow()
    private val _productType = MutableStateFlow(0)
    val productType: StateFlow<Int> = _productType.asStateFlow()

    fun loadManualOptions(productType: Int) {
        launchWithErrorHandling {
            _productType.value = productType
            val isFAQ = productType != Constants.SETTING_BOOK
            val optionsList = mutableListOf<ManualOption>()
            if (isFAQ) {
                optionsList.add(ManualOption("TS001", true))
            }
            optionsList.add(ManualOption("TS004", false))
            _options.value = optionsList
            _title.value =
                if (productType == Constants.SETTING_BOOK) {
                    "Electronic Manual"
                } else {
                    "Questions"
                }
        }
    }

    fun isBookMode(): Boolean = _productType.value == Constants.SETTING_BOOK
}


