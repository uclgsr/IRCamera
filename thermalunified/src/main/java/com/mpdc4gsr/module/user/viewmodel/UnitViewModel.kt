package com.mpdc4gsr.module.user.viewmodel

import com.mpdc4gsr.libunified.app.common.SharedManager
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UnitViewModel : BaseViewModel() {
    companion object {
        const val CELSIUS = 1
        const val FAHRENHEIT = 0
    }

    private val _selectedUnit = MutableStateFlow(CELSIUS)
    val selectedUnit: StateFlow<Int> = _selectedUnit.asStateFlow()

    init {
        loadTemperatureUnit()
    }

    private fun loadTemperatureUnit() {
        _selectedUnit.value = SharedManager.getTemperature()
    }

    fun selectUnit(unit: Int) {
        launchWithErrorHandling {
            _selectedUnit.value = unit
            // Don't save immediately, wait for user to confirm with save button
        }
    }

    fun saveTemperatureUnit() {
        launchWithErrorHandling {
            SharedManager.setTemperature(_selectedUnit.value)
        }
    }

    fun isCelsiusSelected(): Boolean = _selectedUnit.value == CELSIUS

    fun isFahrenheitSelected(): Boolean = _selectedUnit.value == FAHRENHEIT
}
