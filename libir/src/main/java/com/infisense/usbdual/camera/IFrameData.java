package com.infisense.usbdual.camera;

import com.infisense.usbdual.Const;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class IFrameData {
    /**
     * fusionimagedata长度，ARGB，故值为：
     * fusionimage输出宽度 x fusionimage输出高度 x 4.
     */
    public static int FUSION_LEN = Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 4;
    /**
     * 原始infrareddata长度、原始temperaturedata长度，YUV-Y16，故值为：
     * 原始infrared宽度 x 原始infrared高度 x 2.
     */
    public static int ORIGINAL_LEN = Const.IR_WIDTH * Const.IR_HEIGHT * 2;
    /**
     * Scaletemperaturedata长度，YUV-422，故值为：
     * fusionimage输出宽度 x fusionimage输出高度 x 2.
     */
    public static int REMAP_TEMP_LEN = Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 2;
    /**
     * 原始visible lightdata长度，RGB24，故值为：
     * 原始visible light宽度 x 原始visible light高度 x 3.
     */
    public static int LIGHT_LEN = Const.VL_WIDTH * Const.VL_HEIGHT * 3;
    /**
     * Scalevisible lightdata长度，ARGB，故值为：
     * 原始visible light宽度 x 原始visible light高度 x 4.
     */
    public static int P_IN_P_LEN = Const.DUAL_WIDTH * Const.DUAL_HEIGHT * 4;
    /**
     * 一帧除picture-in-pictureScaleScalevisible lightdata之外的所有data长度，
     * 包含 fusionimage、原始infrared、原始temperature、Scaletemperature、原始visible light、picture-in-pictureScalevisible light data.
     * 值为上述data长度之和.
     */
    public static int FRAME_LEN = FUSION_LEN + ORIGINAL_LEN + ORIGINAL_LEN + REMAP_TEMP_LEN + LIGHT_LEN + P_IN_P_LEN;

    /**
     * 将指定帧data中 ARGB <b>fusionimagedata</b> copy到指定array中.
     */
    public static byte[] readFusionData(@NonNull byte[] frame, @Nullable byte[] fusionData) {
        if (fusionData == null) {
            fusionData = new byte[FUSION_LEN];
        }
        System.arraycopy(frame, 0, fusionData, 0, fusionData.length);   //fusionimagedata，ARGB
        return fusionData;
    }

    /**
     * 将指定帧data中 YUV-16 <b>原始infrareddata</b> copy到指定array中.
     */
    public static byte[] readNorIRData(@NonNull byte[] frame, @Nullable byte[] irData) {
        if (irData == null) {
            irData = new byte[ORIGINAL_LEN];
        }
        System.arraycopy(frame, FUSION_LEN, irData, 0, irData.length); //原始infrareddata，YUV-Y16
        return irData;
    }

    /**
     * 将指定帧data中 YUV-16 <b>原始temperaturedata</b> copy到指定array中.
     */
    public static byte[] readNorTempData(@NonNull byte[] frame, @Nullable byte[] norTempData) {
        if (norTempData == null) {
            norTempData = new byte[ORIGINAL_LEN];
        }
        System.arraycopy(frame, FUSION_LEN + ORIGINAL_LEN, norTempData, 0, norTempData.length); //原始temperaturedata，YUV-Y16
        return norTempData;
    }

    /**
     * 将指定帧data中 YUV-422 <b>Scaletemperaturedata</b> copy到指定array中.
     */
    public static byte[] readRemapTempData(@NonNull byte[] frame, @Nullable byte[] remapTempData) {
        if (remapTempData == null) {
            remapTempData = new byte[REMAP_TEMP_LEN];
        }
        System.arraycopy(frame, FUSION_LEN + ORIGINAL_LEN + ORIGINAL_LEN, remapTempData, 0, remapTempData.length); //Scaletemperaturedata，YUV-422
        return remapTempData;
    }
}
