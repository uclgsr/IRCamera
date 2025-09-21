package com.mpdc4gsr.commons.base.entity

import java.io.Serializable

class UnitDBBean : Serializable {
    var dbid: Long? = null
    var loginName: String? = null
    var unitType: Int = 0
    var conversionRelation: String? = null
    var preUnit: String? = null
    var preName: String? = null
    var afterUnit: String? = null
    var afterName: String? = null
    var conversionFormula: String? = null
    var calcFactor: String? = null


    companion object {
        private val serialVersionUID = -1L
    }
}
