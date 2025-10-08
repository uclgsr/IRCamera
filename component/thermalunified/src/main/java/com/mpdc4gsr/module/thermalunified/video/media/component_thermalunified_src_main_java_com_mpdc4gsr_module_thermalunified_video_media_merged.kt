// Merged ALL .kt and .java files from the 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\video\media' directory and its subdirectories.
// Total files: 4 | Generated on: 2025-10-08 01:42:36


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\video\media\Encoder.java =====

package com.mpdc4gsr.module.thermalunified.video.media;

import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Encoder {
    protected static final int STATE_IDLE = 0;
    protected static final int STATE_RECORDING = 1;
    protected static final int STATE_RECORDING_UNTIL_LAST_FRAME = 2;
    private static final String TAG = Encoder.
    class.getSimpleName();
    protected String outputFilePath = null;
    private List<Bitmap> bitmapQueue;
    private EncodeFinishListener encodeFinishListener;
    private EncodingOptions encodingOptions;
    private Thread encodingThread;
    private int frameDelay = 50;
    private int height;
    private int state = STATE_IDLE;
    private int width;

    private Runnable mRunnableEncoder = new Runnable()
    {
        public void run() {
            while (true) {
                if (state != STATE_RECORDING && bitmapQueue.size() <= 0) {
                    break;
                } else if (bitmapQueue.size() > 0) {
                    Bitmap bitmap = null;
                    try {
                        bitmap = bitmapQueue.remove(0);
                    } catch (IndexOutOfBoundsException e) {
                        Log.e(TAG, e.getMessage());
                    }
                    if (bitmap != null) {
                        try {
                            onAddFrame(bitmap);
                        } catch (ArrayIndexOutOfBoundsException e) {
                            Log.e(TAG, e.getMessage());
                        }
                        bitmap.recycle();
                    }
                    if (state == STATE_RECORDING_UNTIL_LAST_FRAME && bitmapQueue.size() == 0) {
                        Log.d(TAG, "Last frame added");
                        break;
                    }
                }
            }
            Log.d(TAG, "add Frame finished");
            onStop();
            notifyEncodeFinish();
        }
    };

    public Encoder()
    {
        setDefaultEncodingOptions();
        init();
    }

    public Encoder(EncodingOptions options)
    {
        encodingOptions = options;
        init();
    }

    private void init()
    {
        onInit();
        initBitmapQueue();
    }

    private void setDefaultEncodingOptions()
    {
        encodingOptions = new EncodingOptions ();
        encodingOptions.compressLevel = 0;
    }

    private void initBitmapQueue()
    {
        bitmapQueue = Collections.synchronizedList(new ArrayList < Bitmap >());
    }

    public void setOutputFilePath(String outputFilePath)
    {
        this.outputFilePath = outputFilePath;
    }

    public void setOutputSize(int width, int height)
    {
        this.width = width;
        this.height = height;
    }

    public void startEncode()
    {
        bitmapQueue.clear();
        onStart();
        setState(STATE_RECORDING);
        encodingThread = new Thread (this.mRunnableEncoder);
        encodingThread.setName("EncodeThread");
        encodingThread.start();
    }

    private void notifyEncodeFinish()
    {
        if (encodeFinishListener != null) {
            encodeFinishListener.onEncodeFinished();
        }
    }

    public void stopEncode()
    {
        if (encodingThread != null && encodingThread.isAlive()) {
            encodingThread.interrupt();
        }
        setState(STATE_IDLE);
    }

    public void addFrame(Bitmap bitmap)
    {
        if (state != STATE_RECORDING) {

        } else {
            bitmapQueue.add(bitmap);
        }
    }

    public void setEncodeFinishListener(EncodeFinishListener listener)
    {
        encodeFinishListener = listener;
    }

    public void notifyLastFrameAdded()
    {
        setState(STATE_RECORDING_UNTIL_LAST_FRAME);
    }

    private void setState(int state)
    {
        this.state = state;
    }

    protected abstract void onAddFrame(Bitmap bitmap);

    protected abstract void onInit();

    protected abstract void onStart();

    protected abstract void onStop();

    protected int getFrameDelay()
    {
        return frameDelay;
    }

    public void setFrameDelay(int delay)
    {
        frameDelay = delay;
    }

    protected int getHeight()
    {
        return height;
    }

    protected int getWidth()
    {
        return width;
    }

    protected EncodingOptions getEncodingOptions()
    {
        return encodingOptions;
    }

    public interface EncodeFinishListener {
        void onEncodeFinished();
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\video\media\EncodeYuvTools.kt =====

@file:Suppress("DEPRECATION")

package com.mpdc4gsr.module.thermalunified.video.media

import android.graphics.Bitmap
import android.media.MediaCodecInfo.CodecCapabilities.*

object EncodeYuvTools {
    fun getNV12(
        inputWidth: Int,
        inputHeight: Int,
        scaled: Bitmap?,
        colorFormat: Int = @Suppress("DEPRECATION") COLOR_FormatYUV420SemiPlanar,
    ): ByteArray {
        val argb = IntArray(inputWidth * inputHeight)
        scaled!!.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        val yuv = ByteArray(inputWidth * inputHeight * 3 / 2)
        when (colorFormat) {
            @Suppress("DEPRECATION") COLOR_FormatYUV420SemiPlanar ->
                encodeYUV420SP(
                    yuv,
                    argb,
                    inputWidth,
                    inputHeight,
                )

            @Suppress("DEPRECATION") COLOR_FormatYUV420Planar ->
                encodeYUV420P(
                    yuv,
                    argb,
                    inputWidth,
                    inputHeight,
                )

            @Suppress("DEPRECATION") COLOR_FormatYUV420PackedSemiPlanar ->
                encodeYUV420PSP(
                    yuv,
                    argb,
                    inputWidth,
                    inputHeight,
                )

            @Suppress("DEPRECATION") COLOR_FormatYUV420PackedPlanar ->
                encodeYUV420PP(
                    yuv,
                    argb,
                    inputWidth,
                    inputHeight,
                )

            else ->
                encodeYUV420SP(
                    yuv,
                    argb,
                    inputWidth,
                    inputHeight,
                )
        }
        return yuv
    }

    private fun encodeYUV420SP(
        yuv420sp: ByteArray,
        argb: IntArray,
        width: Int,
        height: Int,
    ) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize
        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                val r = argb[index] and 0xff0000 shr 16
                val g = argb[index] and 0xff00 shr 8
                val b = argb[index] and 0xff shr 0
                val y = (66 * r + 129 * g + 25 * b + 128 shr 8) + 16
                val u = (112 * r - 94 * g - 18 * b + 128 shr 8) + 128
                val v = (-38 * r - 74 * g + 112 * b + 128 shr 8) + 128
                yuv420sp[yIndex++] =
                    (
                            if (y < 0) {
                                0
                            } else if (y > 255) {
                                255
                            } else {
                                y
                            }
                            ).toByte()
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] =
                        (
                                if (v < 0) {
                                    0
                                } else if (v > 255) {
                                    255
                                } else {
                                    v
                                }
                                ).toByte()
                    yuv420sp[uvIndex++] =
                        (
                                if (u < 0) {
                                    0
                                } else if (u > 255) {
                                    255
                                } else {
                                    u
                                }
                                ).toByte()
                }
                index++
            }
        }
    }

    private fun encodeYUV420P(
        yuv420sp: ByteArray,
        argb: IntArray,
        width: Int,
        height: Int,
    ) {
        val frameSize = width * height
        var yIndex = 0
        var uIndex = frameSize
        var vIndex = frameSize + width * height / 4
        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                val r = argb[index] and 0xff0000 shr 16
                val g = argb[index] and 0xff00 shr 8
                val b = argb[index] and 0xff shr 0
                val y = (66 * r + 129 * g + 25 * b + 128 shr 8) + 16
                val u = (112 * r - 94 * g - 18 * b + 128 shr 8) + 128
                val v = (-38 * r - 74 * g + 112 * b + 128 shr 8) + 128
                yuv420sp[yIndex++] =
                    (
                            if (y < 0) {
                                0
                            } else if (y > 255) {
                                255
                            } else {
                                y
                            }
                            ).toByte()
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[vIndex++] =
                        (
                                if (u < 0) {
                                    0
                                } else if (u > 255) {
                                    255
                                } else {
                                    u
                                }
                                ).toByte()
                    yuv420sp[uIndex++] =
                        (
                                if (v < 0) {
                                    0
                                } else if (v > 255) {
                                    255
                                } else {
                                    v
                                }
                                ).toByte()
                }
                index++
            }
        }
    }

    private fun encodeYUV420PSP(
        yuv420sp: ByteArray,
        argb: IntArray,
        width: Int,
        height: Int,
    ) {
        var yIndex = 0
        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                val r = argb[index] and 0xff0000 shr 16
                val g = argb[index] and 0xff00 shr 8
                val b = argb[index] and 0xff shr 0
                val y = (66 * r + 129 * g + 25 * b + 128 shr 8) + 16
                val u = (112 * r - 94 * g - 18 * b + 128 shr 8) + 128
                val v = (-38 * r - 74 * g + 112 * b + 128 shr 8) + 128
                yuv420sp[yIndex++] =
                    (
                            if (y < 0) {
                                0
                            } else if (y > 255) {
                                255
                            } else {
                                y
                            }
                            ).toByte()
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[yIndex + 1] =
                        (
                                if (v < 0) {
                                    0
                                } else if (v > 255) {
                                    255
                                } else {
                                    v
                                }
                                ).toByte()
                    yuv420sp[yIndex + 3] =
                        (
                                if (u < 0) {
                                    0
                                } else if (u > 255) {
                                    255
                                } else {
                                    u
                                }
                                ).toByte()
                }
                if (index % 2 == 0) {
                    yIndex++
                }
                index++
            }
        }
    }

    private fun encodeYUV420PP(
        yuv420sp: ByteArray,
        argb: IntArray,
        width: Int,
        height: Int,
    ) {
        var yIndex = 0
        var vIndex = yuv420sp.size / 2
        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                val r = argb[index] and 0xff0000 shr 16
                val g = argb[index] and 0xff00 shr 8
                val b = argb[index] and 0xff shr 0
                val y = (66 * r + 129 * g + 25 * b + 128 shr 8) + 16
                val u = (112 * r - 94 * g - 18 * b + 128 shr 8) + 128
                val v = (-38 * r - 74 * g + 112 * b + 128 shr 8) + 128
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[yIndex++] =
                        (
                                if (y < 0) {
                                    0
                                } else if (y > 255) {
                                    255
                                } else {
                                    y
                                }
                                ).toByte()
                    yuv420sp[yIndex + 1] =
                        (
                                if (v < 0) {
                                    0
                                } else if (v > 255) {
                                    255
                                } else {
                                    v
                                }
                                ).toByte()
                    yuv420sp[vIndex + 1] =
                        (
                                if (u < 0) {
                                    0
                                } else if (u > 255) {
                                    255
                                } else {
                                    u
                                }
                                ).toByte()
                    yIndex++
                } else if (j % 2 == 0 && index % 2 == 1) {
                    yuv420sp[yIndex++] =
                        (
                                if (y < 0) {
                                    0
                                } else if (y > 255) {
                                    255
                                } else {
                                    y
                                }
                                ).toByte()
                } else if (j % 2 == 1 && index % 2 == 0) {
                    yuv420sp[vIndex++] =
                        (
                                if (y < 0) {
                                    0
                                } else if (y > 255) {
                                    255
                                } else {
                                    y
                                }
                                ).toByte()
                    vIndex++
                } else if (j % 2 == 1 && index % 2 == 1) {
                    yuv420sp[vIndex++] =
                        (
                                if (y < 0) {
                                    0
                                } else if (y > 255) {
                                    255
                                } else {
                                    y
                                }
                                ).toByte()
                }
                index++
            }
        }
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\video\media\EncodingOptions.java =====

package com.mpdc4gsr.module.thermalunified.video.media;

import org.jetbrains.annotations.NotNull;

public class EncodingOptions {
    public static final int COMPRESS_HIGH = 2;
    public static final int COMPRESS_LOW = 0;
    public static final int COMPRESS_MID = 1;
    public int compressLevel;

    @NotNull
    @Override
    public String toString()
    {
        return "EncodingOptions : compLevel = " + this.compressLevel;
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\video\media\MP4Encoder.java =====

package com.mpdc4gsr.module.thermalunified.video.media;

import static

android.media.MediaCodec.*;
import static

android.media.MediaCodecInfo.CodecProfileLevel.MPEG2ProfileHigh;
import static

android.media.MediaFormat.*;
import static

android.media.MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MP4Encoder extends Encoder {

    private static final int BIT_RATE = 600000;
    private static final int FRAME_RATE = 20;
    private static final int I_FRAME_INTERVAL = 5;
    private static final long ONE_SEC = 1000000;
    private static final String TAG = MP4Encoder.class. getSimpleName ();
    private static final int TIMEOUT_US = 10000;
    private int addedFrameCount;

    private MediaCodec audioCodec;
    private int audioTrackIndex;
    private BufferInfo bufferInfo;
    private int encodedFrameCount;
    private boolean isMuxerStarted = false;
    private boolean isStarted = false;
    private MediaMuxer mediaMuxer;
    private int trackCount = 0;
    private MediaCodec videoCodec;
    private int videoTrackIndex;

    private static long getPresentationTimeUsec (int frameIndex) {
        return (((long) frameIndex) * ONE_SEC) / 20;
    }

    @Override
    protected void onInit() {
    }

    @Override
    protected void onStart() {
        isStarted = true;
        addedFrameCount = 0;
        encodedFrameCount = 0;
        int width = getWidth ();
        int height = getHeight ();
        try {
            bufferInfo = new BufferInfo ();
            videoCodec = MediaCodec.createEncoderByType(MIMETYPE_VIDEO_AVC);
            MediaFormat videoFormat = MediaFormat . createVideoFormat (MIMETYPE_VIDEO_AVC, width, height);
            videoFormat.setInteger(KEY_BIT_RATE, BIT_RATE);
            videoFormat.setInteger(KEY_FRAME_RATE, FRAME_RATE);
            videoFormat.setInteger(KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
            videoFormat.setInteger(KEY_COLOR_FORMAT, getColorFormat());
            videoCodec.configure(videoFormat, null, null, CONFIGURE_FLAG_ENCODE);
            videoCodec.start();
            audioCodec = MediaCodec.createEncoderByType(MIMETYPE_AUDIO_AAC);
            MediaFormat audioFormat = MediaFormat . createAudioFormat (MIMETYPE_AUDIO_AAC, 44100, 1);
            int profile;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                profile = MPEG2ProfileHigh;
            } else {
                profile = 5;
            }
            audioFormat.setInteger(KEY_AAC_PROFILE, profile);
            audioFormat.setInteger(KEY_BIT_RATE, 65536);
            audioCodec.configure(audioFormat, null, null, CONFIGURE_FLAG_ENCODE);
            audioCodec.start();
            mediaMuxer = new MediaMuxer (outputFilePath, MUXER_OUTPUT_MPEG_4);
        } catch (IOException ioe) {
            throw new RuntimeException ("MediaMuxer creation failed", ioe);
        }
    }

    @Override
    protected void onStop() {
        if (isStarted) {
            encode();
            if (this.addedFrameCount > 0) {
                Log.i(TAG, String.format("Total frame count = %s", this.addedFrameCount));
                if (videoCodec != null) {
                    videoCodec.stop();
                    videoCodec.release();
                    videoCodec = null;
                    Log.i(TAG, "RELEASE VIDEO CODEC");
                }
                if (audioCodec != null) {
                    audioCodec.stop();
                    audioCodec.release();
                    audioCodec = null;
                    Log.i(TAG, "RELEASE AUDIO CODEC");
                }
                if (mediaMuxer != null) {
                    mediaMuxer.stop();
                    mediaMuxer.release();
                    mediaMuxer = null;
                    Log.i(TAG, "RELEASE MUXER");
                }
            } else {
                Log.e(TAG, "not added any frame");
            }
            isStarted = false;
        }
    }

    @Override
    protected void onAddFrame(Bitmap bitmap) {
        if (!isStarted) {
            Log.d(TAG, "already finished. can't add Frame ");
        } else if (bitmap == null) {
            Log.e(TAG, "Bitmap is null");
        } else {
            int inputBufIndex = videoCodec . dequeueInputBuffer (TIMEOUT_US);
            if (inputBufIndex >= 0) {

                byte[] input = EncodeYuvTools . INSTANCE . getNV12 (bitmap.getWidth(), bitmap.getHeight(), bitmap, getColorFormat());
                ByteBuffer inputBuffer = videoCodec . getInputBuffer (inputBufIndex);
                inputBuffer.clear();
                inputBuffer.put(input);
                videoCodec.queueInputBuffer(
                    inputBufIndex, 0, input.length,
                    getPresentationTimeUsec(addedFrameCount), 0
                );
            }
            int audioInputBufferIndex = audioCodec . dequeueInputBuffer (TIMEOUT_US);
            if (audioInputBufferIndex >= -1) {

            }
            addedFrameCount++;
            while (addedFrameCount > encodedFrameCount) {
                encode();
            }
        }
    }

    private void encode() {
        encodeVideo();
        encodeAudio();

    }

    private void encodeAudio() {
        int audioStatus = audioCodec . dequeueOutputBuffer (bufferInfo, TIMEOUT_US);
        Log.i(
            TAG, "Audio encoderStatus = " + audioStatus + ", presentationTimeUs = "
                    + bufferInfo.presentationTimeUs
        );
        if (audioStatus == INFO_OUTPUT_FORMAT_CHANGED) {
            MediaFormat audioFormat = audioCodec . getOutputFormat ();
            Log.i(TAG, String.format("output format changed. audio format: %s", audioFormat.toString()));
            audioTrackIndex = mediaMuxer.addTrack(audioFormat);
            trackCount++;
            if (trackCount == 2) {
                Log.i(TAG, "started media muxer.");
                mediaMuxer.start();
                isMuxerStarted = true;
            }
        } else if (audioStatus == INFO_TRY_AGAIN_LATER) {
            Log.d(TAG, "no output from audio encoder available");
        } else {
            ByteBuffer audioData = audioCodec . getOutputBuffer (audioStatus);
            if (audioData != null) {
                audioData.position(bufferInfo.offset);
                audioData.limit(bufferInfo.offset + bufferInfo.size);
                if (isMuxerStarted) {
                    mediaMuxer.writeSampleData(audioTrackIndex, audioData, bufferInfo);
                }
                audioCodec.releaseOutputBuffer(audioStatus, false);
            }
        }
    }

    private void encodeVideo() {
        int encoderStatus = videoCodec . dequeueOutputBuffer (bufferInfo, TIMEOUT_US);
        Log.i(
            TAG, "Video encoderStatus = " + encoderStatus + ", presentationTimeUs = "
                    + bufferInfo.presentationTimeUs
        );
        if (encoderStatus == INFO_OUTPUT_FORMAT_CHANGED) {
            MediaFormat videoFormat = videoCodec . getOutputFormat ();
            Log.i(TAG, String.format("output format changed. video format: %s", videoFormat.toString()));
            videoTrackIndex = mediaMuxer.addTrack(videoFormat);
            trackCount++;
            if (trackCount == 2) {
                Log.i(TAG, "started media muxer.");
                mediaMuxer.start();
                isMuxerStarted = true;
            }
        } else if (encoderStatus == INFO_TRY_AGAIN_LATER) {
            Log.d(TAG, "no output from video encoder available");
        } else {
            ByteBuffer encodedData = videoCodec . getOutputBuffer (encoderStatus);
            if (encodedData != null) {
                encodedData.position(bufferInfo.offset);
                encodedData.limit(bufferInfo.offset + bufferInfo.size);
                if (isMuxerStarted) {
                    mediaMuxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo);
                }
                videoCodec.releaseOutputBuffer(encoderStatus, false);
                encodedFrameCount++;
            }
            Log.i(TAG, "encoderOutputBuffer " + encoderStatus + " was null");
        }
    }

    private int getColorFormat() {
        if ("GOOGLE".equalsIgnoreCase(Build.BRAND) && "PIXEL 4".equalsIgnoreCase(Build.MODEL)) {
            return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
        } else {
            return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
        }
    }

}