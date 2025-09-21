package com.mpdc4gsr.commons.util

import android.text.TextUtils
import com.google.gson.reflect.TypeToken
import com.mpdc4gsr.libunified.app.lms.utils.SPUtils
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.Locale

object UnitUtils {
    fun getUnitDBBeanList(unitType: Int): MutableList<UnitDBBean> {
        try {
            val jsonStr: String?
            if (unitType == 0) {
                jsonStr = PreUtil.Companion.getInstance(MPDC4GSR.getApp()).get(SPKeyUtils.UNIT_METRIC)
            } else {
                jsonStr = PreUtil.Companion.getInstance(MPDC4GSR.getApp()).get(SPKeyUtils.UNIT_BRITISH)
            }
            LLog.w("bcf--jsonStr", jsonStr)
            if (TextUtils.isEmpty(jsonStr)) {
                return ArrayList<UnitDBBean>()
            }
            val unitDBBeanList: MutableList<UnitDBBean> =
                GsonUtils.fromJson<MutableList<UnitDBBean>>(jsonStr, object : TypeToken<MutableList<UnitDBBean?>?>() {
                }.getType())
            return unitDBBeanList
        } catch (e: Exception) {
            e.printStackTrace()
            return ArrayList<UnitDBBean>()
        }
    }

    val unitDBBeanHashMap: HashMap<String?, UnitDBBean>
        get() {
            val unit = SPUtils.getInstance(MPDC4GSR.getApp()).get("unit", "0")
            val unitType = if ("0" == unit) 0 else 1
            return getUnitDBBeanHashMap(unitType)
        }

    fun getUnitDBBeanHashMap(unitType: Int): java.util.HashMap<String?, UnitDBBean?> {
        val hashMap: java.util.HashMap<String?, UnitDBBean?> = java.util.HashMap<String?, UnitDBBean?>()
        try {
            val unitDBBeanList: MutableList<UnitDBBean> = getUnitDBBeanList(unitType)
            for (unitDBBean in unitDBBeanList) {
                hashMap.put(unitDBBean.getPreUnit().lowercase(Locale.getDefault()), unitDBBean)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return hashMap
    }

    fun getCalcResult(
        hashMap: java.util.HashMap<String?, UnitDBBean?>,
        preUnit: String?,
        numericalValue: String
    ): Array<String?> {
        val unit = SPUtils.getInstance(MPDC4GSR.getApp()).get("unit", "0")
        val unitType = if ("0" == unit) 0 else 1
        return getCalcResult(unitType, hashMap, preUnit, numericalValue)
    }

    fun getCalcResult(
        unitType: Int,
        hashMap: java.util.HashMap<String?, UnitDBBean?>,
        preUnit: String?,
        numericalValue: String
    ): Array<String?> {
        var unitDBBean: UnitDBBean? = null
        try {
            if (TextUtils.isEmpty(preUnit)) {
                return arrayOf<String?>(numericalValue, preUnit)
            }
            unitDBBean = hashMap.get(preUnit!!.lowercase(Locale.getDefault()))
            if (unitDBBean == null) {
                return arrayOf<String?>(numericalValue, preUnit)
            }
            if (unitType == 0) {
                if (preUnit.equals(unitDBBean.getAfterUnit(), ignoreCase = true)) {
                    return arrayOf<String?>(numericalValue, unitDBBean.getAfterUnit())
                }
                if (preUnit.equals("K", ignoreCase = true)) {
                    try {
                        return arrayOf<String?>(
                            getResult(numericalValue.toDouble() - 273.15).toString(),
                            unitDBBean.getAfterUnit()
                        )
                    } catch (e: NumberFormatException) {
                        e.printStackTrace()
                        return arrayOf<String?>(numericalValue, unitDBBean.getAfterUnit())
                    }
                } else if (preUnit == "deg.F") {
                    try {
                        return arrayOf<String>(getResult((numericalValue.toDouble() - 32) / 1.8).toString(), "°C")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return arrayOf<String?>(numericalValue, unitDBBean.getAfterUnit())
                    }
                }
                return arrayOf<String?>(
                    getResult(
                        numericalValue.toDouble() * unitDBBean.getCalcFactor().toDouble()
                    ).toString(), unitDBBean.getAfterUnit()
                )
            } else {
                if (preUnit.equals(unitDBBean.getAfterUnit(), ignoreCase = true)) {
                    return arrayOf<String?>(numericalValue, unitDBBean.getAfterUnit())
                }
                if (preUnit.equals("K", ignoreCase = true)) {
                    try {
                        return arrayOf<String?>(
                            getResult(32 + (numericalValue.toDouble() - 273.15) * 1.8).toString(),
                            unitDBBean.getAfterUnit()
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return arrayOf<String?>(numericalValue, unitDBBean.getAfterUnit())
                    }
                } else if (preUnit.equals("deg.C", ignoreCase = true)) {
                    try {
                        return arrayOf<String>(getResult(32 + numericalValue.toDouble() * 1.8).toString(), "°F")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return arrayOf<String?>(numericalValue, unitDBBean.getAfterUnit())
                    }
                }
                return arrayOf<String?>(
                    getResult(
                        numericalValue.toDouble() * unitDBBean.getCalcFactor().toDouble()
                    ).toString(), unitDBBean.getAfterUnit()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (unitDBBean == null) {
            return arrayOf<String?>(numericalValue, preUnit)
        } else {
            return arrayOf<String?>(numericalValue, unitDBBean.getAfterUnit())
        }
    }

    fun getResult(dou: Double): Double {
        val bigDecimal = BigDecimal(dou).setScale(2, RoundingMode.HALF_UP)
        return bigDecimal.toDouble()
    }

    fun getDecimalFormatByDouble(score: Double): String {
        val decimalFormat = DecimalFormat("0.00#")
        return decimalFormat.format(score)
    }
}
