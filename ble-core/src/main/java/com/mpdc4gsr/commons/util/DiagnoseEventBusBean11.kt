package com.mpdc4gsr.commons.util


class DiagnoseEventBusBean {
    var what: Int = 0
    var language: String? = null
    var isSnConnection: Boolean = false
    var isDiagnose: Boolean = false
    private var mDiagEntryType: Long = 0
    var diagMenuMask: Long = 0
    var snPath: String? = null

    fun getmDiagEntryType(): Long {
        return mDiagEntryType
    }

    fun setmDiagEntryType(mDiagEntryType: Long) {
        this.mDiagEntryType = mDiagEntryType
    }
}
