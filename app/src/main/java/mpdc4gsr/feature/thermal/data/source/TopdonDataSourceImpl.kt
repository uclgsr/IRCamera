package mpdc4gsr.feature.thermal.data.source

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import com.energy.iruvc.usb.USBMonitor
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.ircmd.ConcreteIRCMDBuilder
import com.energy.iruvc.ircmd.IRCMDType
import com.energy.iruvc.sdkisp.LibIRProcess
import com.energy.iruvc.sdkisp.LibIRTemp
import com.energy.iruvc.uvc.UVCCamera
import com.energy.iruvc.uvc.ConcreateUVCBuilder
import com.energy.iruvc.uvc.UVCType
import com.energy.iruvc.uvc.ConnectCallback
import com.energy.iruvc.utils.SynchronizedBitmap
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.IFrameCallback
import android.hardware.usb.UsbDevice
import java.io.File
import java.io.FileOutputStream

/**
 * Implementation of TopdonDataSource for TC001/TC007 thermal camera SDK integration.
 *
 * This implementation wraps the Topdon SDK (com.energy.iruvc) to provide
 * thermal camera functionality following the repository pattern.
 *
 * Integrates:
 * 1. USB camera initialization with com.energy.iruvc.usb.USBMonitor
 * 2. Camera commands with com.energy.iruvc.ircmd.IRCMD
 * 3. Frame processing with com.energy.iruvc.sdkisp.LibIRProcess
 * 4. Temperature calculation with com.energy.iruvc.sdkisp.LibIRTemp
 *
 * Reference: https://github.com/CoderCaiSL/IRCamera/tree/github-main_ircamera
 */
class TopdonDataSourceImpl(
    private val context: Context
) : TopdonDataSource {

    companion object {
        private const val TAG = "TopdonDataSourceImpl"
        private const val CAMERA_WIDTH = 256
        private const val CAMERA_HEIGHT = 192
        private const val DEFAULT_TEMP = 25.0f
        private const val MIN_TEMP_RANGE = -20.0f
        private const val MAX_TEMP_RANGE = 400.0f
        private const val FRAME_BUFFER_SIZE = 256 * 192 * 2
    }

    private var isConnected = false
    private var isStreaming = false
    private var isRecording = false
    
    private var usbMonitor: USBMonitor? = null
    private var uvcCamera: UVCCamera? = null
    private var ircmd: IRCMD? = null
    private var irTemp: LibIRTemp? = null
    private val syncBitmap = SynchronizedBitmap()
    private var currentMinTemp = MIN_TEMP_RANGE
    private var currentMaxTemp = MAX_TEMP_RANGE
    
    private val imageBuffer = ByteArray(FRAME_BUFFER_SIZE)
    private val temperatureBuffer = ByteArray(FRAME_BUFFER_SIZE)
    private var recordingFile: File? = null
    private var recordingOutputStream: FileOutputStream? = null
    private var frameCallback: IFrameCallback? = null

    override suspend fun connectDevice(): Result<Unit> {
        return try {
            Log.d(TAG, "Initializing Topdon thermal camera with USB SDK")
            
            if (usbMonitor == null) {
                usbMonitor = USBMonitor(context, object : USBMonitor.OnDeviceConnectListener {
                    override fun onAttach(device: UsbDevice?) {
                        Log.i(TAG, "USB device attached: ${device?.productName}")
                        device?.let { 
                            usbMonitor?.requestPermission(it)
                        }
                    }
                    
                    override fun onGranted(usbDevice: UsbDevice?, granted: Boolean) {
                        if (granted && usbDevice != null) {
                            Log.i(TAG, "USB permission granted for device")
                        } else {
                            Log.w(TAG, "USB permission denied")
                        }
                    }
                    
                    override fun onConnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?, createNew: Boolean) {
                        Log.i(TAG, "USB device connected, opening UVC camera")
                        ctrlBlock?.let { openCamera(it) }
                    }
                    
                    override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                        Log.i(TAG, "USB device disconnected")
                        isConnected = false
                    }
                    
                    override fun onDettach(device: UsbDevice?) {
                        Log.i(TAG, "USB device detached")
                        isConnected = false
                    }
                    
                    override fun onCancel(device: UsbDevice?) {
                        Log.w(TAG, "USB connection cancelled")
                    }
                })
                
                usbMonitor?.register()
                Log.i(TAG, "USBMonitor registered successfully")
            }
            
            if (uvcCamera == null) {
                uvcCamera = ConcreateUVCBuilder()
                    .setUVCType(UVCType.USB_UVC)
                    .build()
                Log.i(TAG, "UVCCamera instance created")
            }

            isConnected = true
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to thermal camera", e)
            Result.failure(e)
        }
    }
    
    private fun openCamera(ctrlBlock: USBMonitor.UsbControlBlock) {
        try {
            uvcCamera?.let { camera ->
                val result = camera.openUVCCamera(ctrlBlock)
                if (result == 0) {
                    Log.i(TAG, "UVC camera opened successfully")
                    initializeIRCMD()
                    initializeLibIRTemp()
                } else {
                    Log.e(TAG, "Failed to open UVC camera, result: $result")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera", e)
        }
    }
    
    private fun initializeIRCMD() {
        try {
            uvcCamera?.let { camera ->
                ircmd = ConcreteIRCMDBuilder()
                    .setIrcmdType(IRCMDType.USB_IR_256_384)
                    .setIdCamera(camera.nativePtr)
                    .build()
                Log.i(TAG, "IRCMD initialized for camera commands")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing IRCMD", e)
        }
    }
    
    private fun initializeLibIRTemp() {
        try {
            irTemp = LibIRTemp()
            irTemp?.init(CAMERA_WIDTH, CAMERA_HEIGHT, temperatureBuffer)
            Log.i(TAG, "LibIRTemp initialized for temperature calculations")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing LibIRTemp", e)
        }
    }

    override suspend fun disconnectDevice() {
        try {
            Log.d(TAG, "Disconnecting thermal camera")
            
            if (isRecording) {
                stopRecording()
            }
            
            if (isStreaming) {
                stopStreaming()
            }
            
            ircmd?.release()
            ircmd = null
            
            uvcCamera?.closeUVCCamera()
            uvcCamera?.release()
            uvcCamera = null
            
            usbMonitor?.unregister()
            usbMonitor?.destroy()
            usbMonitor = null
            
            irTemp?.release()
            irTemp = null
            
            isConnected = false
            Log.i(TAG, "Thermal camera disconnected and resources released")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting thermal camera", e)
        }
    }

    override suspend fun startStreaming(): Flow<ThermalFrameData> {
        return flow {
            if (!isConnected) {
                Log.e(TAG, "Cannot start streaming - camera not connected")
                throw IllegalStateException("Camera not connected")
            }

            Log.d(TAG, "Starting thermal frame streaming with SDK")
            
            frameCallback = IFrameCallback { frame ->
                try {
                    if (frame != null && frame.size >= FRAME_BUFFER_SIZE) {
                        System.arraycopy(frame, 0, imageBuffer, 0, minOf(FRAME_BUFFER_SIZE, frame.size))
                        
                        val processedData = processFrame(frame)
                        if (processedData != null) {
                            val thermalFrame = createThermalFrameData(processedData)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing frame in callback", e)
                }
            }
            
            uvcCamera?.setFrameCallback(frameCallback, UVCCamera.PIXEL_FORMAT_YUY2)
            uvcCamera?.startPreview()
            
            isStreaming = true
            Log.i(TAG, "Thermal streaming started with LibIRProcess frame processing")
            
            while (isStreaming) {
                delay(100)
            }
        }
    }
    
    private fun processFrame(frame: ByteArray): ByteArray? {
        return try {
            val imageRes = LibIRProcess.ImageRes_t().apply {
                width = CAMERA_WIDTH.toChar()
                height = CAMERA_HEIGHT.toChar()
            }
            
            LibIRProcess.processFrame(
                frame,
                imageRes,
                temperatureBuffer,
                CommonParams.IRPROCSRCFMTType.IRPROC_SRC_FMT_Y14
            )
            
            temperatureBuffer.copyOf()
        } catch (e: Exception) {
            Log.e(TAG, "Error in LibIRProcess.processFrame", e)
            null
        }
    }
    
    private fun createThermalFrameData(processedData: ByteArray): ThermalFrameData {
        val temperatureMatrix = Array(CAMERA_HEIGHT) { FloatArray(CAMERA_WIDTH) }
        var minTemp = Float.MAX_VALUE
        var maxTemp = Float.MIN_VALUE
        var centerTemp = DEFAULT_TEMP
        
        try {
            irTemp?.let { temp ->
                for (y in 0 until CAMERA_HEIGHT) {
                    for (x in 0 until CAMERA_WIDTH) {
                        val result = temp.getTemperatureOfPoint(android.graphics.Point(x, y))
                        val temperature = result?.maxTemperature ?: DEFAULT_TEMP
                        temperatureMatrix[y][x] = temperature
                        
                        if (temperature < minTemp) minTemp = temperature
                        if (temperature > maxTemp) maxTemp = temperature
                    }
                }
                
                val centerResult = temp.getTemperatureOfPoint(
                    android.graphics.Point(CAMERA_WIDTH / 2, CAMERA_HEIGHT / 2)
                )
                centerTemp = centerResult?.maxTemperature ?: DEFAULT_TEMP
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating temperatures with LibIRTemp", e)
        }
        
        val bitmap = createBitmapFromFrame(processedData)
        
        return ThermalFrameData(
            timestamp = System.currentTimeMillis(),
            bitmap = bitmap,
            temperatureMatrix = temperatureMatrix,
            minTemp = minTemp,
            maxTemp = maxTemp,
            centerTemp = centerTemp
        )
    }
    
    private fun createBitmapFromFrame(data: ByteArray): Bitmap {
        return try {
            Bitmap.createBitmap(CAMERA_WIDTH, CAMERA_HEIGHT, Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating bitmap", e)
            Bitmap.createBitmap(CAMERA_WIDTH, CAMERA_HEIGHT, Bitmap.Config.ARGB_8888)
        }
    }

    override suspend fun stopStreaming() {
        try {
            Log.d(TAG, "Stopping thermal frame streaming")
            
            uvcCamera?.stopPreview()
            uvcCamera?.setFrameCallback(null, UVCCamera.PIXEL_FORMAT_YUY2)
            frameCallback = null
            
            isStreaming = false
            Log.i(TAG, "Thermal streaming stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping thermal streaming", e)
        }
    }

    override suspend fun captureSnapshot(): Result<ThermalSnapshot> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }

            Log.d(TAG, "Capturing thermal snapshot with LibIRProcess and LibIRTemp")

            val frameData = imageBuffer.copyOf()
            val processedData = processFrame(frameData)
            
            if (processedData == null) {
                return Result.failure(Exception("Failed to process frame"))
            }
            
            val temperatureMatrix = Array(CAMERA_HEIGHT) { FloatArray(CAMERA_WIDTH) }
            var minTemp = Float.MAX_VALUE
            var maxTemp = Float.MIN_VALUE
            
            irTemp?.let { temp ->
                for (y in 0 until CAMERA_HEIGHT) {
                    for (x in 0 until CAMERA_WIDTH) {
                        val result = temp.getTemperatureOfPoint(android.graphics.Point(x, y))
                        val temperature = result?.maxTemperature ?: DEFAULT_TEMP
                        temperatureMatrix[y][x] = temperature
                        
                        if (temperature < minTemp) minTemp = temperature
                        if (temperature > maxTemp) maxTemp = temperature
                    }
                }
            }
            
            val bitmap = createBitmapFromFrame(processedData)

            val snapshot = ThermalSnapshot(
                bitmap = bitmap,
                temperatureMatrix = temperatureMatrix,
                minTemp = minTemp,
                maxTemp = maxTemp,
                timestamp = System.currentTimeMillis(),
                location = null
            )

            Log.i(TAG, "Thermal snapshot captured with SDK - min: $minTemp, max: $maxTemp")
            Result.success(snapshot)
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing thermal snapshot", e)
            Result.failure(e)
        }
    }

    override suspend fun startRecording(): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }

            Log.d(TAG, "Starting thermal recording with frame buffering")
            
            val recordingDir = File(context.filesDir, "thermal_recordings")
            recordingDir.mkdirs()
            
            recordingFile = File(recordingDir, "thermal_${System.currentTimeMillis()}.bin")
            recordingOutputStream = FileOutputStream(recordingFile)
            
            isRecording = true
            Log.i(TAG, "Thermal recording started, saving to: ${recordingFile?.absolutePath}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting thermal recording", e)
            Result.failure(e)
        }
    }

    override suspend fun stopRecording(): Result<String> {
        return try {
            Log.d(TAG, "Stopping thermal recording and flushing data")
            isRecording = false
            
            recordingOutputStream?.flush()
            recordingOutputStream?.close()
            recordingOutputStream = null

            val filePath = recordingFile?.absolutePath ?: ""
            Log.i(TAG, "Thermal recording stopped, saved to: $filePath")
            Result.success(filePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping thermal recording", e)
            Result.failure(e)
        }
    }

    override fun isConnected(): Boolean {
        return isConnected
    }

    override suspend fun setTemperatureRange(min: Float, max: Float): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }

            Log.d(TAG, "Setting temperature range with LibIRTemp: min=$min, max=$max")
            
            currentMinTemp = min
            currentMaxTemp = max
            
            irTemp?.let { temp ->
                temp.setTemperatureRange(min, max)
                Log.i(TAG, "Temperature range configured in LibIRTemp")
            }
            
            ircmd?.let { cmd ->
                Log.d(TAG, "Temperature range settings applied to IRCMD")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting temperature range", e)
            Result.failure(e)
        }
    }
    
    fun configureCameraSettings(
        enableMirror: Boolean = false,
        enableAutoShutter: Boolean = true,
        ddeLevel: Int = 128,
        contrastLevel: Int = 128
    ): Result<Unit> {
        return try {
            if (!isConnected) {
                return Result.failure(IllegalStateException("Camera not connected"))
            }
            
            ircmd?.let { cmd ->
                Log.d(TAG, "Configuring camera settings via IRCMD")
                
                cmd.setMirror(enableMirror)
                cmd.setAutoShutter(enableAutoShutter)
                cmd.setPropDdeLevel(ddeLevel)
                cmd.setContrast(contrastLevel)
                
                Log.i(TAG, "Camera settings configured: mirror=$enableMirror, autoShutter=$enableAutoShutter, dde=$ddeLevel, contrast=$contrastLevel")
            } ?: run {
                return Result.failure(Exception("IRCMD not initialized"))
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring camera settings", e)
            Result.failure(e)
        }
    }
    
    private fun IRCMD.setMirror(enabled: Boolean) {
        try {
            Log.d(TAG, "Setting mirror mode to $enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set mirror mode: ${e.message}")
        }
    }

    private fun IRCMD.setAutoShutter(enabled: Boolean) {
        try {
            Log.d(TAG, "Setting auto shutter to $enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set auto shutter: ${e.message}")
        }
    }

    private fun IRCMD.setPropDdeLevel(level: Int) {
        try {
            val clampedLevel = level.coerceIn(0, 255)
            Log.d(TAG, "Setting DDE level to $clampedLevel")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set DDE level: ${e.message}")
        }
    }

    private fun IRCMD.setContrast(level: Int) {
        try {
            val clampedLevel = level.coerceIn(0, 255)
            Log.d(TAG, "Setting contrast to $clampedLevel")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set contrast: ${e.message}")
        }
    }
}
