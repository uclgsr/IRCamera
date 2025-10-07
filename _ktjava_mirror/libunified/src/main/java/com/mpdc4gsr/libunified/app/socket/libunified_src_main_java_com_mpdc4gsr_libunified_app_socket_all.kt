// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\socket' subtree
// Files: 4; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\socket\SocketCmdUtils.kt =====

package com.mpdc4gsr.libunified.app.socket

import android.text.TextUtils
import com.google.gson.Gson
import org.json.JSONException
import org.json.JSONObject

object SocketCmdUtils {
    fun getSocketCmd(cmd: Int): String? {
        var cmdJson: String? = null
        try {
            val gson = Gson()
            val paramMap: HashMap<String, Int> = HashMap()
            paramMap["cmd"] = cmd
            cmdJson = gson.toJson(paramMap)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            return cmdJson
        }
    }

    fun getCmdResponse(response: String?): Int? {
        var cmd: Int? = null
        if (TextUtils.isEmpty(response)) return null
        try {
            val jsonObject = JSONObject(response!!)
            cmd = jsonObject.getInt("cmd")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return cmd
    }

    fun getIpResponse(response: String?): String? {
        var ip: String? = null
        if (TextUtils.isEmpty(response)) return null
        try {
            val jsonObject = JSONObject(response!!)
            ip = jsonObject.getString("ip")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return ip
    }

    fun getDataResponse(response: String?): String? {
        var data: String? = null
        if (TextUtils.isEmpty(response)) return null
        try {
            val jsonObject = JSONObject(response!!)
            data = jsonObject.getString("data")
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return data
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\socket\SocketFrameBean.kt =====

package com.mpdc4gsr.libunified.app.socket

data class SocketFrameBean(
    val isMaxShow: Boolean,
    val isMinShow: Boolean,
    val isCenterShow: Boolean,
    val maxX: Int,
    val maxY: Int,
    val maxValue: Int,
    val minX: Int,
    val minY: Int,
    val minValue: Int,
    val centerX: Int,
    val centerY: Int,
    val centerValue: Int,
    val isMaxWarn: Boolean,
    val isMinWarn: Boolean,
    val isCenterWarn: Boolean,
    val isP1Show: Boolean,
    val p1X: Int,
    val p1Y: Int,
    val p1Value: Int,
    val isP1MaxWarn: Boolean,
    val isP1MinWarn: Boolean,
    val isP1CenterWarn: Boolean,
    val isP2Show: Boolean,
    val p2X: Int,
    val p2Y: Int,
    val p2Value: Int,
    val isP2MaxWarn: Boolean,
    val isP2MinWarn: Boolean,
    val isP2CenterWarn: Boolean,
    val isP3Show: Boolean,
    val p3X: Int,
    val p3Y: Int,
    val p3Value: Int,
    val isP3MaxWarn: Boolean,
    val isP3MinWarn: Boolean,
    val isP3CenterWarn: Boolean,
    val isL1Show: Boolean,
    val l1StartX: Int,
    val l1StartY: Int,
    val l1EndX: Int,
    val l1EndY: Int,
    val l1MaxX: Int,
    val l1MaxY: Int,
    val l1MaxValue: Int,
    val l1MinX: Int,
    val l1MinY: Int,
    val l1MinValue: Int,
    val l1AveValue: Int,
    val isL1MaxWarn: Boolean,
    val isL1MinWarn: Boolean,
    val isL1CenterWarn: Boolean,
    val isL2Show: Boolean,
    val l2StartX: Int,
    val l2StartY: Int,
    val l2EndX: Int,
    val l2EndY: Int,
    val l2MaxX: Int,
    val l2MaxY: Int,
    val l2MaxValue: Int,
    val l2MinX: Int,
    val l2MinY: Int,
    val l2MinValue: Int,
    val l2AveValue: Int,
    val isL2MaxWarn: Boolean,
    val isL2MinWarn: Boolean,
    val isL2CenterWarn: Boolean,
    val isL3Show: Boolean,
    val l3StartX: Int,
    val l3StartY: Int,
    val l3EndX: Int,
    val l3EndY: Int,
    val l3MaxX: Int,
    val l3MaxY: Int,
    val l3MaxValue: Int,
    val l3MinX: Int,
    val l3MinY: Int,
    val l3MinValue: Int,
    val l3AveValue: Int,
    val isL3MaxWarn: Boolean,
    val isL3MinWarn: Boolean,
    val isL3CenterWarn: Boolean,
    val isR1Show: Boolean,
    val r1StartX: Int,
    val r1StartY: Int,
    val r1EndX: Int,
    val r1EndY: Int,
    val r1MaxX: Int,
    val r1MaxY: Int,
    val r1MaxValue: Int,
    val r1MinX: Int,
    val r1MinY: Int,
    val r1MinValue: Int,
    val r1AveValue: Int,
    val isR1MaxWarn: Boolean,
    val isR1MinWarn: Boolean,
    val isR1CenterWarn: Boolean,
    val isR2Show: Boolean,
    val r2StartX: Int,
    val r2StartY: Int,
    val r2EndX: Int,
    val r2EndY: Int,
    val r2MaxX: Int,
    val r2MaxY: Int,
    val r2MaxValue: Int,
    val r2MinX: Int,
    val r2MinY: Int,
    val r2MinValue: Int,
    val r2AveValue: Int,
    val isR2MaxWarn: Boolean,
    val isR2MinWarn: Boolean,
    val isR2CenterWarn: Boolean,
    val isR3Show: Boolean,
    val r3StartX: Int,
    val r3StartY: Int,
    val r3EndX: Int,
    val r3EndY: Int,
    val r3MaxX: Int,
    val r3MaxY: Int,
    val r3MaxValue: Int,
    val r3MinX: Int,
    val r3MinY: Int,
    val r3MinValue: Int,
    val r3AveValue: Int,
    val isR3MaxWarn: Boolean,
    val isR3MinWarn: Boolean,
    val isR3CenterWarn: Boolean,
) {
    constructor(byteArray: ByteArray) : this(
        isMaxShow = byteArray[0].toInt() and 0xff == 1,
        isMinShow = byteArray[1].toInt() and 0xff == 1,
        isCenterShow = byteArray[2].toInt() and 0xff == 1,
        maxX = (byteArray[4].toInt() and 0xff) or (byteArray[5].toInt() and 0xff shl 8),
        maxY = (byteArray[6].toInt() and 0xff) or (byteArray[7].toInt() and 0xff shl 8),
        maxValue = ((byteArray[8].toInt() and 0xff) or (byteArray[9].toInt() and 0xff shl 8)) - 2732,
        minX = (byteArray[10].toInt() and 0xff) or (byteArray[11].toInt() and 0xff shl 8),
        minY = (byteArray[12].toInt() and 0xff) or (byteArray[13].toInt() and 0xff shl 8),
        minValue = ((byteArray[14].toInt() and 0xff) or (byteArray[15].toInt() and 0xff shl 8)) - 2732,
        centerX = (byteArray[16].toInt() and 0xff) or (byteArray[17].toInt() and 0xff shl 8),
        centerY = (byteArray[18].toInt() and 0xff) or (byteArray[19].toInt() and 0xff shl 8),
        centerValue = ((byteArray[20].toInt() and 0xff) or (byteArray[21].toInt() and 0xff shl 8)) - 2732,
        isMaxWarn = byteArray[22].toInt() and 0xff == 1,
        isMinWarn = byteArray[23].toInt() and 0xff == 1,
        isCenterWarn = byteArray[24].toInt() and 0xff == 1,
        isP1Show = byteArray[26].toInt() and 0xff == 1,
        p1X = (byteArray[28].toInt() and 0xff) or (byteArray[29].toInt() and 0xff shl 8),
        p1Y = (byteArray[30].toInt() and 0xff) or (byteArray[31].toInt() and 0xff shl 8),
        p1Value = ((byteArray[32].toInt() and 0xff) or (byteArray[33].toInt() and 0xff shl 8)) - 2732,
        isP1MaxWarn = byteArray[34].toInt() and 0xff == 1,
        isP1MinWarn = byteArray[35].toInt() and 0xff == 1,
        isP1CenterWarn = byteArray[36].toInt() and 0xff == 1,
        isP2Show = byteArray[38].toInt() and 0xff == 1,
        p2X = (byteArray[40].toInt() and 0xff) or (byteArray[41].toInt() and 0xff shl 8),
        p2Y = (byteArray[42].toInt() and 0xff) or (byteArray[43].toInt() and 0xff shl 8),
        p2Value = ((byteArray[44].toInt() and 0xff) or (byteArray[45].toInt() and 0xff shl 8)) - 2732,
        isP2MaxWarn = byteArray[46].toInt() and 0xff == 1,
        isP2MinWarn = byteArray[47].toInt() and 0xff == 1,
        isP2CenterWarn = byteArray[48].toInt() and 0xff == 1,
        isP3Show = byteArray[50].toInt() and 0xff == 1,
        p3X = (byteArray[52].toInt() and 0xff) or (byteArray[53].toInt() and 0xff shl 8),
        p3Y = (byteArray[54].toInt() and 0xff) or (byteArray[55].toInt() and 0xff shl 8),
        p3Value = ((byteArray[56].toInt() and 0xff) or (byteArray[57].toInt() and 0xff shl 8)) - 2732,
        isP3MaxWarn = byteArray[58].toInt() and 0xff == 1,
        isP3MinWarn = byteArray[59].toInt() and 0xff == 1,
        isP3CenterWarn = byteArray[60].toInt() and 0xff == 1,
        isL1Show = byteArray[62].toInt() and 0xff == 1,
        l1StartX = (byteArray[64].toInt() and 0xff) or (byteArray[65].toInt() and 0xff shl 8),
        l1StartY = (byteArray[66].toInt() and 0xff) or (byteArray[67].toInt() and 0xff shl 8),
        l1EndX = (byteArray[68].toInt() and 0xff) or (byteArray[69].toInt() and 0xff shl 8),
        l1EndY = (byteArray[70].toInt() and 0xff) or (byteArray[71].toInt() and 0xff shl 8),
        l1MaxX = (byteArray[72].toInt() and 0xff) or (byteArray[73].toInt() and 0xff shl 8),
        l1MaxY = (byteArray[74].toInt() and 0xff) or (byteArray[75].toInt() and 0xff shl 8),
        l1MaxValue = ((byteArray[76].toInt() and 0xff) or (byteArray[77].toInt() and 0xff shl 8)) - 2732,
        l1MinX = (byteArray[78].toInt() and 0xff) or (byteArray[79].toInt() and 0xff shl 8),
        l1MinY = (byteArray[80].toInt() and 0xff) or (byteArray[81].toInt() and 0xff shl 8),
        l1MinValue = ((byteArray[82].toInt() and 0xff) or (byteArray[83].toInt() and 0xff shl 8)) - 2732,
        l1AveValue = ((byteArray[88].toInt() and 0xff) or (byteArray[89].toInt() and 0xff shl 8)) - 2732,
        isL1MaxWarn = byteArray[90].toInt() and 0xff == 1,
        isL1MinWarn = byteArray[91].toInt() and 0xff == 1,
        isL1CenterWarn = byteArray[92].toInt() and 0xff == 1,
        isL2Show = byteArray[94].toInt() and 0xff == 1,
        l2StartX = (byteArray[96].toInt() and 0xff) or (byteArray[97].toInt() and 0xff shl 8),
        l2StartY = (byteArray[98].toInt() and 0xff) or (byteArray[99].toInt() and 0xff shl 8),
        l2EndX = (byteArray[100].toInt() and 0xff) or (byteArray[101].toInt() and 0xff shl 8),
        l2EndY = (byteArray[102].toInt() and 0xff) or (byteArray[103].toInt() and 0xff shl 8),
        l2MaxX = (byteArray[104].toInt() and 0xff) or (byteArray[105].toInt() and 0xff shl 8),
        l2MaxY = (byteArray[106].toInt() and 0xff) or (byteArray[107].toInt() and 0xff shl 8),
        l2MaxValue = ((byteArray[108].toInt() and 0xff) or (byteArray[109].toInt() and 0xff shl 8)) - 2732,
        l2MinX = (byteArray[110].toInt() and 0xff) or (byteArray[111].toInt() and 0xff shl 8),
        l2MinY = (byteArray[112].toInt() and 0xff) or (byteArray[113].toInt() and 0xff shl 8),
        l2MinValue = ((byteArray[114].toInt() and 0xff) or (byteArray[115].toInt() and 0xff shl 8)) - 2732,
        l2AveValue = ((byteArray[120].toInt() and 0xff) or (byteArray[121].toInt() and 0xff shl 8)) - 2732,
        isL2MaxWarn = byteArray[122].toInt() and 0xff == 1,
        isL2MinWarn = byteArray[123].toInt() and 0xff == 1,
        isL2CenterWarn = byteArray[124].toInt() and 0xff == 1,
        isL3Show = byteArray[126].toInt() and 0xff == 1,
        l3StartX = (byteArray[128].toInt() and 0xff) or (byteArray[129].toInt() and 0xff shl 8),
        l3StartY = (byteArray[130].toInt() and 0xff) or (byteArray[131].toInt() and 0xff shl 8),
        l3EndX = (byteArray[132].toInt() and 0xff) or (byteArray[133].toInt() and 0xff shl 8),
        l3EndY = (byteArray[134].toInt() and 0xff) or (byteArray[135].toInt() and 0xff shl 8),
        l3MaxX = (byteArray[136].toInt() and 0xff) or (byteArray[137].toInt() and 0xff shl 8),
        l3MaxY = (byteArray[138].toInt() and 0xff) or (byteArray[139].toInt() and 0xff shl 8),
        l3MaxValue = ((byteArray[140].toInt() and 0xff) or (byteArray[141].toInt() and 0xff shl 8)) - 2732,
        l3MinX = (byteArray[142].toInt() and 0xff) or (byteArray[143].toInt() and 0xff shl 8),
        l3MinY = (byteArray[144].toInt() and 0xff) or (byteArray[145].toInt() and 0xff shl 8),
        l3MinValue = ((byteArray[146].toInt() and 0xff) or (byteArray[147].toInt() and 0xff shl 8)) - 2732,
        l3AveValue = ((byteArray[152].toInt() and 0xff) or (byteArray[153].toInt() and 0xff shl 8)) - 2732,
        isL3MaxWarn = byteArray[154].toInt() and 0xff == 1,
        isL3MinWarn = byteArray[155].toInt() and 0xff == 1,
        isL3CenterWarn = byteArray[156].toInt() and 0xff == 1,
        isR1Show = byteArray[158].toInt() and 0xff == 1,
        r1StartX = (byteArray[160].toInt() and 0xff) or (byteArray[161].toInt() and 0xff shl 8),
        r1StartY = (byteArray[162].toInt() and 0xff) or (byteArray[163].toInt() and 0xff shl 8),
        r1EndX = (byteArray[164].toInt() and 0xff) or (byteArray[165].toInt() and 0xff shl 8),
        r1EndY = (byteArray[166].toInt() and 0xff) or (byteArray[167].toInt() and 0xff shl 8),
        r1MaxX = (byteArray[168].toInt() and 0xff) or (byteArray[169].toInt() and 0xff shl 8),
        r1MaxY = (byteArray[170].toInt() and 0xff) or (byteArray[171].toInt() and 0xff shl 8),
        r1MaxValue = ((byteArray[172].toInt() and 0xff) or (byteArray[173].toInt() and 0xff shl 8)) - 2732,
        r1MinX = (byteArray[174].toInt() and 0xff) or (byteArray[175].toInt() and 0xff shl 8),
        r1MinY = (byteArray[176].toInt() and 0xff) or (byteArray[177].toInt() and 0xff shl 8),
        r1MinValue = ((byteArray[178].toInt() and 0xff) or (byteArray[179].toInt() and 0xff shl 8)) - 2732,
        r1AveValue = ((byteArray[184].toInt() and 0xff) or (byteArray[185].toInt() and 0xff shl 8)) - 2732,
        isR1MaxWarn = byteArray[186].toInt() and 0xff == 1,
        isR1MinWarn = byteArray[187].toInt() and 0xff == 1,
        isR1CenterWarn = byteArray[188].toInt() and 0xff == 1,
        isR2Show = byteArray[190].toInt() and 0xff == 1,
        r2StartX = (byteArray[192].toInt() and 0xff) or (byteArray[193].toInt() and 0xff shl 8),
        r2StartY = (byteArray[194].toInt() and 0xff) or (byteArray[195].toInt() and 0xff shl 8),
        r2EndX = (byteArray[196].toInt() and 0xff) or (byteArray[197].toInt() and 0xff shl 8),
        r2EndY = (byteArray[198].toInt() and 0xff) or (byteArray[199].toInt() and 0xff shl 8),
        r2MaxX = (byteArray[200].toInt() and 0xff) or (byteArray[201].toInt() and 0xff shl 8),
        r2MaxY = (byteArray[202].toInt() and 0xff) or (byteArray[203].toInt() and 0xff shl 8),
        r2MaxValue = ((byteArray[204].toInt() and 0xff) or (byteArray[205].toInt() and 0xff shl 8)) - 2732,
        r2MinX = (byteArray[206].toInt() and 0xff) or (byteArray[207].toInt() and 0xff shl 8),
        r2MinY = (byteArray[208].toInt() and 0xff) or (byteArray[209].toInt() and 0xff shl 8),
        r2MinValue = ((byteArray[210].toInt() and 0xff) or (byteArray[211].toInt() and 0xff shl 8)) - 2732,
        r2AveValue = ((byteArray[216].toInt() and 0xff) or (byteArray[217].toInt() and 0xff shl 8)) - 2732,
        isR2MaxWarn = byteArray[218].toInt() and 0xff == 1,
        isR2MinWarn = byteArray[219].toInt() and 0xff == 1,
        isR2CenterWarn = byteArray[220].toInt() and 0xff == 1,
        isR3Show = byteArray[222].toInt() and 0xff == 1,
        r3StartX = (byteArray[224].toInt() and 0xff) or (byteArray[225].toInt() and 0xff shl 8),
        r3StartY = (byteArray[226].toInt() and 0xff) or (byteArray[227].toInt() and 0xff shl 8),
        r3EndX = (byteArray[228].toInt() and 0xff) or (byteArray[229].toInt() and 0xff shl 8),
        r3EndY = (byteArray[230].toInt() and 0xff) or (byteArray[231].toInt() and 0xff shl 8),
        r3MaxX = (byteArray[232].toInt() and 0xff) or (byteArray[233].toInt() and 0xff shl 8),
        r3MaxY = (byteArray[234].toInt() and 0xff) or (byteArray[235].toInt() and 0xff shl 8),
        r3MaxValue = ((byteArray[236].toInt() and 0xff) or (byteArray[237].toInt() and 0xff shl 8)) - 2732,
        r3MinX = (byteArray[238].toInt() and 0xff) or (byteArray[239].toInt() and 0xff shl 8),
        r3MinY = (byteArray[240].toInt() and 0xff) or (byteArray[241].toInt() and 0xff shl 8),
        r3MinValue = ((byteArray[242].toInt() and 0xff) or (byteArray[243].toInt() and 0xff shl 8)) - 2732,
        r3AveValue = ((byteArray[248].toInt() and 0xff) or (byteArray[249].toInt() and 0xff shl 8)) - 2732,
        isR3MaxWarn = byteArray[250].toInt() and 0xff == 1,
        isR3MinWarn = byteArray[251].toInt() and 0xff == 1,
        isR3CenterWarn = byteArray[252].toInt() and 0xff == 1,
    )

    companion object {
        private fun Boolean.openText(): String = if (this) "[ph][ph]" else "[ph][ph]"
        private fun Int.toCStr(): String =
            "${this / 10}${if (this % 10 == 0) "" else ".${this % 10}"}Â°C"
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        if (isMaxShow) {
            stringBuilder.append("[ph][ph][ph] ($maxX, $maxY) [ph][ph]${maxValue.toCStr()} [ph][ph]${isMaxWarn.openText()}\n")
        }
        if (isMinShow) {
            stringBuilder.append("[ph][ph][ph] ($minX, $minY) [ph][ph]${minValue.toCStr()} [ph][ph]${isMinWarn.openText()}\n")
        }
        if (isCenterShow) {
            stringBuilder.append("[ph][ph][ph] ($centerX, $centerY) [ph][ph]${centerValue.toCStr()} [ph][ph]${isCenterWarn.openText()}\n")
        }
        if (isP1Show) {
            stringBuilder.append("[ph]1 ($p1X, $p1Y) [ph][ph]${p1Value.toCStr()}\n")
        }
        if (isP2Show) {
            stringBuilder.append("[ph]2 ($p2X, $p2Y) [ph][ph]${p2Value.toCStr()}\n")
        }
        if (isP3Show) {
            stringBuilder.append("[ph]3 ($p3X, $p3Y) [ph][ph]${p3Value.toCStr()}\n")
        }
        if (isL1Show) {
            stringBuilder.append("[ph]1 ($l1StartX, $l1StartY)-($l1EndX, $l1EndY) ")
            stringBuilder.append("[ph][ph][ph]${l1MinValue.toCStr()}($l1MinX, $l1MinY) [ph][ph][ph]${l1MaxValue.toCStr()}($l1MaxX, $l1MaxY) ")
            stringBuilder.append("[ph][ph][ph]${l1AveValue.toCStr()}\n")
        }
        if (isL2Show) {
            stringBuilder.append("[ph]2 ($l2StartX, $l2StartY)-($l2EndX, $l2EndY) ")
            stringBuilder.append("[ph][ph][ph]${l2MinValue.toCStr()}($l2MinX, $l2MinY) [ph][ph][ph]${l2MaxValue.toCStr()}($l2MaxX, $l2MaxY) ")
            stringBuilder.append("[ph][ph][ph]${l2AveValue.toCStr()}\n")
        }
        if (isL3Show) {
            stringBuilder.append("[ph]3 ($l3StartX, $l3StartY)-($l3EndX, $l3EndY) ")
            stringBuilder.append("[ph][ph][ph]${l3MinValue.toCStr()}($l3MinX, $l3MinY) [ph][ph][ph]${l3MaxValue.toCStr()}($l3MaxX, $l3MaxY) ")
            stringBuilder.append("[ph][ph][ph]${l3AveValue.toCStr()}\n")
        }
        if (isR1Show) {
            stringBuilder.append("[ph]1 ($r1StartX, $r1StartY)-($r1EndX, $r1EndY) ")
            stringBuilder.append("[ph][ph][ph]${r1MinValue.toCStr()}($r1MinX, $r1MinY) [ph][ph][ph]${r1MaxValue.toCStr()}($r1MaxX, $r1MaxY) ")
            stringBuilder.append("[ph][ph][ph]${r1AveValue.toCStr()}\n")
        }
        if (isR2Show) {
            stringBuilder.append("[ph]2 ($r2StartX, $r2StartY)-($r2EndX, $r2EndY) ")
            stringBuilder.append("[ph][ph][ph]${r2MinValue.toCStr()}($r2MinX, $r2MinY) [ph][ph][ph]${r2MaxValue.toCStr()}($r2MaxX, $r2MaxY) ")
            stringBuilder.append("[ph][ph][ph]${l2AveValue.toCStr()}\n")
        }
        if (isR3Show) {
            stringBuilder.append("[ph]3 ($r3StartX, $r3StartY)-($r3EndX, $r3EndY) ")
            stringBuilder.append("[ph][ph][ph]${r3MinValue.toCStr()}($r3MinX, $r3MinY) [ph][ph][ph]${r3MaxValue.toCStr()}($r3MaxX, $r3MaxY) ")
            stringBuilder.append("[ph][ph][ph]${r3AveValue.toCStr()}\n")
        }
        return stringBuilder.toString()
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\socket\WebSocketProxy.kt =====

package com.mpdc4gsr.libunified.app.socket

import android.Manifest
import android.content.pm.PackageManager
import android.net.Network
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.core.os.postDelayed
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mpdc4gsr.libunified.compat.ContextProvider
import com.elvishew.xlog.XLog
import com.mpdc4gsr.libunified.app.event.DeviceEventManager
import com.mpdc4gsr.libunified.app.security.CertificateManager
import com.mpdc4gsr.libunified.app.utils.WifiUtils
import com.mpdc4gsr.libunified.app.utils.WsCmdConstants
import okhttp3.*
import okio.ByteString

class WebSocketProxy {
    companion object {
        private const val TS004_URL = "wss://192.168.40.1:888"
        private const val TC007_URL = "wss://192.168.40.1:63206/v1/thermal/temp/template/data"
        private const val TS004_URL_FALLBACK = "ws://192.168.40.1:888"
        private const val TC007_URL_FALLBACK =
            "ws://192.168.40.1:63206/v1/thermal/temp/template/data"

        @JvmStatic
        private var mWebSocketProxy: WebSocketProxy? = null
        fun getInstance(): WebSocketProxy {
            if (mWebSocketProxy == null) {
                synchronized(WebSocketProxy::class) {
                    if (mWebSocketProxy == null) {
                        mWebSocketProxy = WebSocketProxy()
                    }
                }
            }
            return mWebSocketProxy!!
        }
    }

    private var currentSSID: String? = null
    private var mWsManager: WsManager? = null
    private var webSocketListener: MyWebSocketListener? = null
    private var reconnectHandler = ReconnectHandler()
    private var network: Network? = null
    private var certificateManager: CertificateManager? = null
    private var useSecureConnection = true
    fun initializeSecurity(context: android.content.Context) {
        certificateManager = CertificateManager(context)
        val initialized = certificateManager?.initialize() ?: false
        if (!initialized) {
            XLog.tag("WebSocket")
                .w("Failed to initialize certificate manager, falling back to insecure connections")
            useSecureConnection = false
        } else {
            XLog.tag("WebSocket").i("Certificate manager initialized successfully")
        }
    }

    private fun getOKHttpClient(): OkHttpClient {
        val builder =
            OkHttpClient.Builder()
                .addInterceptor(
                    Interceptor { chain ->
                        val originalRequest = chain.request()
                        val requestBuilder: Request.Builder = originalRequest.newBuilder()
                        certificateManager?.let { certManager ->
                            val authToken = certManager.generateAuthToken()
                            requestBuilder.addHeader("Authorization", "Bearer $authToken")
                        }
                        val compressedRequest: Request = requestBuilder.build()
                        XLog.tag("WebSocket").d("request:$compressedRequest")
                        chain.proceed(compressedRequest)
                    },
                )
                .retryOnConnectionFailure(true)
        if (useSecureConnection && certificateManager != null) {
            try {
                val sslSocketFactory = certificateManager?.createSSLSocketFactory()
                val trustManager = certificateManager?.getTrustManager()
                val hostnameVerifier = certificateManager?.createHostnameVerifier()
                if (sslSocketFactory != null && trustManager != null && hostnameVerifier != null) {
                    builder.sslSocketFactory(sslSocketFactory, trustManager)
                    builder.hostnameVerifier(hostnameVerifier)
                    XLog.tag("WebSocket").d("Configured secure WebSocket connection")
                } else {
                    XLog.tag("WebSocket")
                        .w("SSL configuration incomplete, falling back to insecure connection")
                    useSecureConnection = false
                }
            } catch (e: Exception) {
                XLog.tag("WebSocket")
                    .e("Failed to configure SSL, falling back to insecure connection", e)
                useSecureConnection = false
            }
        }
        network?.socketFactory?.let {
            if (!useSecureConnection) {
                builder.socketFactory(it)
            }
        }
        return builder.build()
    }

    private var onFrameListener: ((frame: SocketFrameBean) -> Unit)? = null
    fun setOnFrameListener(
        activity: ComponentActivity,
        listener: (frame: SocketFrameBean) -> Unit,
    ) {
        activity.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onCreate(owner: LifecycleOwner) {
                    onFrameListener = listener
                }

                override fun onDestroy(owner: LifecycleOwner) {
                    onFrameListener = null
                }
            },
        )
    }

    var onMessageListener: ((text: String) -> Unit)? = null
    fun startWebSocket(
        ssid: String,
        network: Network? = null,
    ) {
        if (ssid == currentSSID) {
            if (mWsManager != null) {
                XLog.tag("WebSocket").w("$ssid startWebSocket() [ph][ph][ph][ph]")
                return
            }
            this.network = network
        } else {
            XLog.tag("WebSocket")
                .d("[ph][ph][ph] $currentSSID [ph][ph][ph] $ssidï¼Œ[ph][ph][ph][ph][ph]")
            if (reconnectHandler.isReconnecting) {
                DeviceEventManager.emitSocketConnectionSync(false, false)
            }
            this.network = network
            currentSSID = ssid
            reconnectHandler.currentSSID = ssid
            stopWebSocket()
        }
        XLog.tag("WebSocket").d("$ssid startWebSocket()")
        if (mWsManager == null) {
            webSocketListener =
                MyWebSocketListener(ssid, reconnectHandler, onMessageListener) {
                    onFrameListener?.invoke(it)
                }
            mWsManager =
                WsManager.Builder()
                    .client(getOKHttpClient())
                    .wsUrl(getWebSocketUrl(ssid))
                    .setWsStatusListener(webSocketListener)
                    .build()
        }
        mWsManager?.startConnect()
    }

    fun stopWebSocket() {
        XLog.tag("WebSocket").d("stopWebSocket()")
        webSocketListener?.isNeedReconnect = false
        webSocketListener = null
        mWsManager?.stopConnect()
        mWsManager = null
    }

    fun isConnected(): Boolean = isTS004Connect() || isTC007Connect()
    fun isTS004Connect(): Boolean {
        // TS004 functionality removed
        return false
    }

    fun isTC007Connect(): Boolean {
        // TC007 functionality removed
        return false
    }

    fun sendMessage(cmd: String?) {
        mWsManager?.sendMessage(cmd)
    }

    private fun getWebSocketUrl(ssid: String): String {
        // TS004/TC007 functionality removed
        throw UnsupportedOperationException("TS004/TC007 device support removed")
    }

    private class MyWebSocketListener(
        val ssid: String,
        val handler: ReconnectHandler,
        val onMessageListener: ((text: String) -> Unit)?,
        val onFrameListener: (frame: SocketFrameBean) -> Unit,
    ) : WsManager.IWebSocketListener() {
        var isNeedReconnect = true
        override fun onOpen(
            webSocket: WebSocket,
            response: Response,
        ) {
            XLog.tag("WebSocket").d("$ssid Socket [ph][ph][ph][ph]")
            isNeedReconnect = true
            handler.reset()
            DeviceEventManager.emitSocketConnectionSync(true, false)
        }

        override fun onMessage(
            webSocket: WebSocket,
            text: String,
        ) {
            if (SocketCmdUtils.getCmdResponse(text) == WsCmdConstants.APP_EVENT_HEART_BEATS) {
                Log.v(
                    "WebSocket",
                    "<-- [ph][ph][ph][ph][ph][ph] ${text.replace("\n", "").replace(" ", "")}"
                )
            } else {
                XLog.tag("WebSocket").d("$ssid [ph][ph]TEXT[ph][ph]:$text")
            }
            onMessageListener?.invoke(text)
        }

        private var needPrint = false
        override fun onMessage(
            webSocket: WebSocket,
            bytes: ByteString,
        ) {
            XLog.tag("WebSocket")
                .w("[ph][ph][ph][ph][ph][ph][ph]ï¼Œ[ph][ph][ph] bytes [ph][ph]ï¼Œ[ph][ph] ${bytes.size}")
        }

        override fun onClosing(
            webSocket: WebSocket,
            code: Int,
            reason: String,
        ) {
            XLog.tag("WebSocket").d("$ssid [ph][ph][ph][ph][ph]ï¼Œ[ph][ph]ï¼š$reason")
        }

        override fun onClosed(
            webSocket: WebSocket,
            code: Int,
            reason: String,
        ) {
            if (handler.isReconnecting) {
                XLog.tag("WebSocket")
                    .d("$ssid [ph][ph][ph][ph][ph]ï¼Œ[ph][ph][ph][ph][ph][ph]ï¼Œ[ph][ph]ï¼š$reason")
            } else {
                XLog.tag("WebSocket").d("$ssid [ph][ph][ph][ph][ph]ï¼Œ[ph][ph]ï¼š$reason")
                handler.reset()
                DeviceEventManager.emitSocketConnectionSync(false, false)
            }
            mWebSocketProxy?.currentSSID = ""
        }

        override fun onFailure(
            webSocket: WebSocket,
            t: Throwable,
            response: Response?,
        ) {
            XLog.tag("WebSocket")
                .d("$ssid [ph][ph][ph][ph][ph][ph][ph]ï¼Œresponse: ${response?.message}")
            XLog.tag("WebSocket")
                .d("$ssid [ph][ph][ph][ph][ph][ph][ph]ï¼Œ[ph][ph][ph][ph]: ${t.message}")
            if (checkNeedReconnect()) {
                handler.handleFail(ssid)
                if (!handler.isReconnecting) {
                    DeviceEventManager.emitSocketConnectionSync(false, false)
                }
            } else {
                XLog.tag("WebSocket").w("[ph][ph][ph][ph][ph][ph]")
                handler.reset()
                getInstance().stopWebSocket()
                DeviceEventManager.emitSocketConnectionSync(false, false)
            }
            mWebSocketProxy?.currentSSID = ""
        }

        override fun onHeartBeat(): String? =
            SocketCmdUtils.getSocketCmd(WsCmdConstants.APP_EVENT_HEART_BEATS)

        override fun onHeartBeatTimeout() {
            XLog.tag("WebSocket").w("[ph][ph][ph][ph]")
            handler.handleFail(ssid)
        }

        private fun checkNeedReconnect(): Boolean {
            if (!isNeedReconnect) {
                return false
            }
            if (ContextCompat.checkSelfPermission(
                    ContextProvider.getContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return true
            }
            val wifiName: String = WifiUtils.getCurrentWifiSSID(ContextProvider.getContext()) ?: return true
            XLog.tag("WebSocket").i("[ph][ph][ph][ph][ph]ï¼Œ[ph][ph][ph][ph] WIFIï¼š$wifiName")
            return wifiName == ssid
        }
    }

    private class ReconnectHandler : Handler(Looper.getMainLooper()) {
        companion object {
            private const val MAX_RECONNECT_COUNT = 3
            private const val RECONNECT_MILLIS = 3000L
        }

        var currentSSID: String = ""
            set(value) {
                if (value != field) {
                    field = value
                    reset()
                }
            }
        var reconnectCount: Int = 0
        var isReconnecting: Boolean = false
        fun reset() {
            reconnectCount = 0
            isReconnecting = false
            removeCallbacksAndMessages(null)
        }

        fun handleFail(currentSSID: String) {
            if (this.currentSSID != currentSSID) {
                XLog.tag("WebSocket")
                    .w("[ph][ph][ph][ph][ph] ${this.currentSSID} [ph]ï¼Œ[ph][ph] $currentSSID fail [ph][ph]")
                return
            }
            if (isReconnecting) {
                reconnectCount++
                if (reconnectCount < MAX_RECONNECT_COUNT) {
                    XLog.tag("WebSocket").w("[ph] $reconnectCount [ph][ph][ph][ph][ph]")
                    getInstance().stopWebSocket()
                    removeCallbacksAndMessages(null)
                    postDelayed(RECONNECT_MILLIS) {
                        getInstance().startWebSocket(currentSSID)
                    }
                } else {
                    XLog.tag("WebSocket")
                        .w("[ph][ph][ph][ph][ph][ph][ph][ph]ï¼Œ[ph][ph] [ph][ph][ph][ph][ph] [ph][ph]")
                    reconnectCount = 0
                    isReconnecting = false
                    removeCallbacksAndMessages(null)
                    getInstance().stopWebSocket()
                }
            } else {
                XLog.tag("WebSocket")
                    .d("[ph][ph][ph][ph][ph][ph][ph][ph][ph][ph]ï¼Œ[ph][ph][ph][ph][ph][ph][ph][ph]")
                reconnectCount = 0
                isReconnecting = true
                getInstance().stopWebSocket()
                removeCallbacksAndMessages(null)
                postDelayed(RECONNECT_MILLIS) {
                    getInstance().startWebSocket(currentSSID)
                }
            }
        }
    }
}


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\socket\WsManager.kt =====

package com.mpdc4gsr.libunified.app.socket

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import okio.ByteString
import java.util.*
import java.util.concurrent.locks.ReentrantLock

class WsManager(
    private val wsUrl: String,
    private val okHttpClient: OkHttpClient,
    private val statusListener: IWebSocketListener
) {
    companion object {
        private const val NORMAL_CLOSE_CODE = 1000
        private const val ABNORMAL_CLOSE_CODE = 1001
        private const val NORMAL_CLOSE_TIPS = "APP call close() and return true"
        private const val ABNORMAL_CLOSE_TIPS = "APP call close() and return false"
    }

    private var mWebSocket: WebSocket? = null
    private var status: State = State.DISCONNECTED
    private var heartBeatTimer: HeartBeatTimer? = null
    private val mWebSocketListener: WebSocketListener =
        object : WebSocketListener() {
            @Override
            override fun onOpen(
                webSocket: WebSocket,
                response: Response,
            ) {
                mWebSocket = webSocket
                status = State.CONNECTED
                heartBeatTimer?.cancel()
                heartBeatTimer = HeartBeatTimer(this@WsManager)
                heartBeatTimer?.timeoutListener = {
                    statusListener.runMain {
                        it.onHeartBeatTimeout()
                    }
                }
                heartBeatTimer?.start()
                statusListener.runMain {
                    it.onOpen(webSocket, response)
                }
            }

            @Override
            override fun onMessage(
                webSocket: WebSocket,
                bytes: ByteString,
            ) {
                heartBeatTimer?.lastHeartBeatTime = System.currentTimeMillis()
                statusListener.runMain {
                    it.onMessage(webSocket, bytes)
                }
            }

            @Override
            override fun onMessage(
                webSocket: WebSocket,
                text: String,
            ) {
                heartBeatTimer?.lastHeartBeatTime = System.currentTimeMillis()
                statusListener.runMain {
                    it.onMessage(webSocket, text)
                }
            }

            @Override
            override fun onClosing(
                webSocket: WebSocket,
                code: Int,
                reason: String,
            ) {
                status = State.DISCONNECTED
                statusListener.runMain {
                    it.onClosing(webSocket, code, reason)
                }
            }

            @Override
            override fun onClosed(
                webSocket: WebSocket,
                code: Int,
                reason: String,
            ) {
                status = State.DISCONNECTED
                heartBeatTimer?.cancel()
                heartBeatTimer = null
                statusListener.runMain {
                    it.onClosed(webSocket, code, reason)
                }
            }

            @Override
            override fun onFailure(
                webSocket: WebSocket,
                t: Throwable,
                response: Response?,
            ) {
                status = State.DISCONNECTED
                statusListener.runMain {
                    it.onFailure(webSocket, t, response)
                }
            }
        }

    fun isConnect(): Boolean = status == State.CONNECTING || status == State.CONNECTED
    private var mLock = ReentrantLock()

    @Synchronized
    fun startConnect() {
        if (status == State.CONNECTING || status == State.CONNECTED) {
            Log.w(
                "WebSocket",
                "${if (status == State.CONNECTING) "[ph][ph][ph]" else "[ph][ph][ph]"} startConnect() [ph][ph][ph][ph]"
            )
            return
        }
        status = State.CONNECTING
        okHttpClient.dispatcher.cancelAll()
        val mRequest: Request =
            Request.Builder()
                .url(wsUrl)
                .build()
        try {
            mLock.lockInterruptibly()
            try {
                okHttpClient.newWebSocket(mRequest, mWebSocketListener)
            } finally {
                mLock.unlock()
            }
        } catch (_: InterruptedException) {
        }
    }

    fun stopConnect() {
        heartBeatTimer?.cancel()
        heartBeatTimer = null
        if (status == State.DISCONNECTED) {
            return
        }
        status = State.DISCONNECTED
        okHttpClient.dispatcher.cancelAll()
        if (mWebSocket != null) {
            val isClosed = mWebSocket!!.close(NORMAL_CLOSE_CODE, NORMAL_CLOSE_TIPS)
            if (isClosed) {
                statusListener.runMain {
                    it.onClosed(mWebSocket!!, NORMAL_CLOSE_CODE, NORMAL_CLOSE_TIPS)
                }
            } else {
                statusListener.runMain {
                    it.onClosed(mWebSocket!!, ABNORMAL_CLOSE_CODE, ABNORMAL_CLOSE_TIPS)
                }
            }
        }
    }

    fun sendMessage(msg: String?): Boolean {
        return send(msg)
    }

    fun sendMessage(byteString: ByteString?): Boolean {
        return send(byteString)
    }

    private fun send(msg: Any?): Boolean {
        var isSend = false
        if (mWebSocket != null && status == State.CONNECTED) {
            if (msg is String) {
                isSend = mWebSocket!!.send(msg)
            } else if (msg is ByteString) {
                isSend = mWebSocket!!.send(msg)
            }
        }
        return isSend
    }

    private val wsMainHandler = Handler(Looper.getMainLooper())
    private fun IWebSocketListener?.runMain(block: (IWebSocketListener) -> Unit) {
        if (this != null) {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                wsMainHandler.post {
                    block(this)
                }
            } else {
                block(this)
            }
        }
    }

    private class HeartBeatTimer(val wsManager: WsManager) : Timer() {
        var timeoutListener: (() -> Unit)? = null

        @Volatile
        var lastHeartBeatTime: Long = 0
        fun start() {
            schedule(
                object : TimerTask() {
                    override fun run() {
                        val currentTime = System.currentTimeMillis()
                        if (lastHeartBeatTime == 0L) {
                            lastHeartBeatTime = currentTime
                        }
                        if (currentTime - lastHeartBeatTime > 15 * 1000) {
                            Log.d(
                                "WebSocket",
                                "[ph][ph]5[ph][ph][ph][ph][ph][ph][ph]ï¼Œ[ph][ph][ph][ph][ph][ph]"
                            )
                            timeoutListener?.invoke()
                            lastHeartBeatTime = currentTime
                        } else {
                            val heartBeatMsg: String? = wsManager.statusListener.onHeartBeat()
                            if (heartBeatMsg == null) {
                                lastHeartBeatTime = currentTime
                            } else {
                                val isSuccess = wsManager.sendMessage(heartBeatMsg)
                                Log.v(
                                    "WebSocket",
                                    "--> [ph][ph][ph][ph][ph][ph] ${if (isSuccess) "[ph][ph]" else "[ph][ph]"}"
                                )
                            }
                        }
                    }
                },
                3000,
                3000,
            )
        }
    }

    abstract class IWebSocketListener : WebSocketListener() {
        abstract fun onHeartBeat(): String?
        abstract fun onHeartBeatTimeout()
    }

    enum class State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
    }

    class Builder {
        private var wsUrl: String? = null
        private var okHttpClient: OkHttpClient? = null
        private var statusListener: IWebSocketListener? = null
        fun wsUrl(url: String?): Builder {
            wsUrl = url
            return this
        }

        fun client(client: OkHttpClient?): Builder {
            okHttpClient = client
            return this
        }

        fun setWsStatusListener(wsStatusListener: IWebSocketListener?): Builder {
            statusListener = wsStatusListener
            return this
        }

        fun build(): WsManager = WsManager(wsUrl!!, okHttpClient!!, statusListener!!)
    }
}


