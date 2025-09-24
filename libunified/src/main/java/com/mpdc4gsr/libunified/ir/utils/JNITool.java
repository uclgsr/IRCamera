package com.mpdc4gsr.libunified.ir.utils;

import android.util.Log;
import org.opencv.core.Mat;
import org.opencv.core.Core;
import java.io.IOException;

/**
 * JNITool implementation for thermal image processing using OpenCV
 * Provides actual functionality for thermal analysis and frame differencing
 */
public class JNITool {
    public static final JNITool INSTANCE = new JNITool();
    private static final String TAG = "JNITool";
    private static final int DEFAULT_IMAGE_WIDTH = 192;
    private static final int DEFAULT_IMAGE_HEIGHT = 256;
    private static final int BGR_CHANNELS = 3;

    public byte[] maxTempL(byte[] image, byte[] temperature, int width, int height, int flag) {
        if (image == null || temperature == null || width <= 0 || height <= 0) {
            Log.w(TAG, "Invalid input parameters for maxTempL");
            return new byte[0];
        }
        
        try {
            // Use OpencvTools.highTemTrack for maximum temperature tracking
            Mat result = OpencvTools.highTemTrack(image, temperature);
            if (result != null && !result.empty()) {
                return OpencvTools.matToByteArray(result);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error in maxTempL processing", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in maxTempL", e);
        }
        
        // Fallback to empty array on error
        return new byte[width * height * BGR_CHANNELS];
    }

    public byte[] lowTemTrack(byte[] image, byte[] temperature, int width, int height, int flag) {
        if (image == null || temperature == null || width <= 0 || height <= 0) {
            Log.w(TAG, "Invalid input parameters for lowTemTrack");
            return new byte[0];
        }
        
        try {
            // Use OpencvTools.lowTemTrack for minimum temperature tracking
            Mat result = OpencvTools.lowTemTrack(image, temperature);
            if (result != null && !result.empty()) {
                return OpencvTools.matToByteArray(result);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error in lowTemTrack processing", e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in lowTemTrack", e);
        }
        
        // Fallback to empty array on error
        return new byte[width * height * BGR_CHANNELS];
    }

    public byte[] diff2firstFrameByTempWH(int width, int height, byte[] firstTemp, byte[] temperature, byte[] image) {
        if (firstTemp == null || temperature == null || image == null || width <= 0 || height <= 0) {
            Log.w(TAG, "Invalid input parameters for diff2firstFrameByTempWH");
            return new byte[0];
        }
        
        try {
            // Create temperature-based frame difference using OpenCV
            Mat firstTempMat = OpencvTools.getTempData(firstTemp);
            Mat currentTempMat = OpencvTools.getTempData(temperature);
            
            if (firstTempMat != null && currentTempMat != null && 
                !firstTempMat.empty() && !currentTempMat.empty()) {
                
                Mat diffMat = new Mat();
                Core.absdiff(firstTempMat, currentTempMat, diffMat);
                
                // Convert back to byte array
                return OpencvTools.matToByteArray(diffMat);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in diff2firstFrameByTempWH processing", e);
        }
        
        // Fallback to empty array on error
        return new byte[width * height * BGR_CHANNELS];
    }

    public static byte[] diff2firstFrameU1(byte[] buffer, byte[] bufferB) {
        if (buffer == null || bufferB == null) {
            Log.w(TAG, "Invalid input parameters for diff2firstFrameU1");
            return new byte[0];
        }
        
        try {
            // Create frame difference for U1 format using OpenCV
            Mat mat1 = OpencvTools.getImageData(buffer);
            Mat mat2 = OpencvTools.getImageData(bufferB);
            
            if (mat1 != null && mat2 != null && !mat1.empty() && !mat2.empty()) {
                Mat diffMat = new Mat();
                Core.absdiff(mat1, mat2, diffMat);
                
                return OpencvTools.matToByteArray(diffMat);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in diff2firstFrameU1 processing", e);
        }
        
        // Fallback to default size on error
        return new byte[DEFAULT_IMAGE_WIDTH * DEFAULT_IMAGE_HEIGHT * BGR_CHANNELS];
    }

    public static byte[] diff2firstFrameU4(byte[] baseImage, byte[] nextImage) {
        if (baseImage == null || nextImage == null) {
            Log.w(TAG, "Invalid input parameters for diff2firstFrameU4");
            return new byte[0];
        }
        
        try {
            // Create frame difference for U4 format using OpenCV
            Mat baseMat = OpencvTools.getImageData(baseImage);
            Mat nextMat = OpencvTools.getImageData(nextImage);
            
            if (baseMat != null && nextMat != null && !baseMat.empty() && !nextMat.empty()) {
                Mat diffMat = new Mat();
                Core.absdiff(baseMat, nextMat, diffMat);
                
                return OpencvTools.matToByteArray(diffMat);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in diff2firstFrameU4 processing", e);
        }
        
        // Fallback to default size on error
        return new byte[DEFAULT_IMAGE_WIDTH * DEFAULT_IMAGE_HEIGHT * BGR_CHANNELS];
    }
}