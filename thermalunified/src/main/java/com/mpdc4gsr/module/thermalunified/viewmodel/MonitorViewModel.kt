package com.mpdc4gsr.module.thermalunified.viewmodel

import androidx.lifecycle.MutableLiveData
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel

class MonitorViewModel : BaseViewModel() {
    companion object {
        const val STATS_START = 101
        const val STATS_MONITOR = 102
        const val STATS_FINISH = 103
    }

    private val _monitorState = MutableLiveData(STATS_START)
    val monitorState: MutableLiveData<Int> = _monitorState
    private val _selectedType = MutableLiveData(1)
    val selectedType: MutableLiveData<Int> = _selectedType
    private val _selectedIndex = MutableLiveData<ArrayList<Int>>(arrayListOf())
    val selectedIndex: MutableLiveData<ArrayList<Int>> = _selectedIndex
    private val _recordingTime = MutableLiveData(0L)
    val recordingTime: MutableLiveData<Long> = _recordingTime

    fun setMonitorState(state: Int) {
        _monitorState.value = state
    }

    fun selectMonitorType(
        type: Int,
        indices: ArrayList<Int>,
    ) {
        _selectedType.value = type
        _selectedIndex.value = indices
        _monitorState.value = STATS_FINISH
    }

    fun startRecording() {
        _recordingTime.value = 0L
    }

    fun updateRecordingTime(time: Long) {
        _recordingTime.value = time
    }

    fun resetState() {
        _monitorState.value = STATS_START
        _selectedType.value = 1
        _selectedIndex.value = arrayListOf()
        _recordingTime.value = 0L
    }
}
