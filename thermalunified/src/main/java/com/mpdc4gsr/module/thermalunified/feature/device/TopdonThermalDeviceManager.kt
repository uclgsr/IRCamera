package com.mpdc4gsr.module.thermalunified.feature.device

import android.content.Context
import com.energy.iruvc.ircmd.IRCMD
import com.energy.iruvc.utils.CommonParams
import com.energy.iruvc.utils.SynchronizedBitmap
import com.energy.iruvc.uvc.ConnectCallback
import com.energy.iruvc.uvc.UVCCamera
import com.mpdc4gsr.libunified.app.tools.DeviceTools
import com.mpdc4gsr.libunified.ir.camera.IRUVCTC
import com.mpdc4gsr.libunified.ir.extension.ColorPalette
import com.mpdc4gsr.libunified.ir.extension.performNUC
import com.mpdc4gsr.libunified.ir.extension.setAutoShutter
import com.mpdc4gsr.libunified.ir.extension.setColorPalette
import com.mpdc4gsr.libunified.ir.extension.setContrast
import com.mpdc4gsr.libunified.ir.extension.setPropDdeLevel
import com.mpdc4gsr.libunified.ir.utils.USBMonitorCallback
import com.topdon.commons.util.Topdon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TopdonThermalDeviceManager(
    context: Context,
    private val externalScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val hardwareProbe: HardwareProbe = DeviceToolsHardwareProbe,
) : ThermalDeviceManager {

    private val applicationContext = context.applicationContext
    private val _status = MutableStateFlow(
        ThermalDeviceStatus(
            deviceLabel = "Topdon TC001",
            capabilities = DEFAULT_CAPABILITIES,
        )
    )
    override val status = _status.asStateFlow()

    private val syncBitmap = SynchronizedBitmap()
    private var irCmd: IRCMD? = null
    private var irCamera: IRUVCTC? = null
    private var monitorJob: Job? = null
    private var currentConfig: ThermalDeviceConfig = ThermalDeviceConfig()

    init {
        runCatching { Topdon.init(applicationContext) }
        startDeviceMonitoring()
    }

    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            ensureCamera()
            irCamera?.registerUSB()
            _status.update {
                it.copy(
                    lastError = null,
                    deviceLabel = "Topdon TC001 - Connecting",
                )
            }
        }.onFailure { error ->
            _status.update { it.copy(lastError = error.message) }
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            runCatching {
                irCamera?.stopPreview()
                irCamera?.unregisterUSB()
            }
            irCamera = null
            irCmd = null
            _status.update {
                it.copy(
                    isConnected = false,
                    isStreaming = false,
                    deviceLabel = "Topdon TC001 - Disconnected",
                )
            }
        }
    }

    override suspend fun startStream(config: ThermalDeviceConfig): Result<Unit> = withContext(Dispatchers.IO) {
        currentConfig = config
        runCatching {
            ensureCamera()
            applyConfigToSdk(config)
            _status.update {
                it.copy(
                    isStreaming = true,
                    deviceLabel = "Topdon TC001 - Streaming",
                    lastError = null,
                )
            }
        }.onFailure { error ->
            _status.update { it.copy(isStreaming = false, lastError = error.message) }
        }
    }

    override suspend fun stopStream() {
        withContext(Dispatchers.IO) {
            runCatching {
                irCamera?.stopPreview()
            }
            _status.update {
                it.copy(
                    isStreaming = false,
                    deviceLabel = "Topdon TC001 - Ready",
                )
            }
        }
    }

    override suspend fun triggerManualCalibration(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            irCmd?.performNUC() ?: error("Topdon IRCMD not available")
        }.onFailure { error ->
            _status.update { it.copy(lastError = error.message) }
        }
    }

    private fun ensureCamera() {
        if (irCamera != null) return
        val connectCallback = object : ConnectCallback {
            override fun onCameraOpened(camera: UVCCamera?) {
                _status.update {
                    it.copy(
                        isConnected = true,
                        isStreaming = true,
                        deviceLabel = "Topdon TC001 - Preview Active",
                        lastError = null,
                    )
                }
            }

            override fun onIRCMDCreate(cmd: IRCMD?) {
                irCmd = cmd
                applyConfigToSdk(currentConfig)
                cmd?.setAutoShutter(true)
                cmd?.setPropDdeLevel(128)
                cmd?.setContrast(128)
            }
        }
        val usbMonitorCallback = object : USBMonitorCallback {
            override fun onAttach() {
                _status.update { it.copy(deviceLabel = "Topdon TC001 - Attached") }
            }

            override fun onGranted() {
                _status.update { it.copy(deviceLabel = "Topdon TC001 - Permission Granted") }
            }

            override fun onDettach() {
                _status.update {
                    it.copy(
                        deviceLabel = "Topdon TC001 - Detached",
                        isConnected = false,
                        isStreaming = false
                    )
                }
            }

            override fun onCancel() {
                _status.update { it.copy(deviceLabel = "Topdon TC001 - Permission Cancelled") }
            }

            override fun onConnect() {
                _status.update { it.copy(deviceLabel = "Topdon TC001 - Negotiating") }
            }

            override fun onDisconnect() {
                _status.update {
                    it.copy(
                        deviceLabel = "Topdon TC001 - Disconnected",
                        isConnected = false,
                        isStreaming = false
                    )
                }
            }
        }
        irCamera = IRUVCTC(
            256,
            192,
            applicationContext,
            syncBitmap,
            CommonParams.DataFlowMode.IMAGE_AND_TEMP_OUTPUT,
            connectCallback,
            usbMonitorCallback
        )
    }

    private fun applyConfigToSdk(config: ThermalDeviceConfig) {
        val palette = when (config.colorPalette) {
            ThermalColorPalette.Ironbow -> ColorPalette.IRONBOW
            ThermalColorPalette.Rainbow -> ColorPalette.RAINBOW
            ThermalColorPalette.WhiteHot -> ColorPalette.WHITEHOT
            ThermalColorPalette.BlackHot -> ColorPalette.BLACKHOT
        }
        irCmd?.setColorPalette(palette)
        irCmd?.setAutoShutter(config.enableNoiseReduction)
    }

    private fun startDeviceMonitoring() {
        monitorJob?.cancel()
        monitorJob = externalScope.launch {
            while (isActive) {
                val usbAttached = hardwareProbe.isUsbAttached()
                val topdonConnected = hardwareProbe.isTopdonConnected()
                val label = when {
                    topdonConnected -> "Topdon TC001 - Connected"
                    usbAttached -> "Topdon TC001 - USB Detected"
                    else -> "Topdon TC001 - Not Detected"
                }
                _status.update {
                    it.copy(
                        deviceLabel = label,
                        isConnected = topdonConnected,
                        lastError = it.lastError?.takeIf { topdonConnected.not() },
                        capabilities = DEFAULT_CAPABILITIES,
                    )
                }
                delay(750)
            }
        }
    }

    interface HardwareProbe {
        fun isTopdonConnected(): Boolean
        fun isUsbAttached(): Boolean
    }

    private object DeviceToolsHardwareProbe : HardwareProbe {
        override fun isTopdonConnected(): Boolean =
            DeviceTools.isTC001PlusConnect() || DeviceTools.isTC001LiteConnect()

        override fun isUsbAttached(): Boolean = DeviceTools.findUsbDevice() != null
    }

    companion object {
        private val DEFAULT_CAPABILITIES = setOf(
            ThermalDeviceCapability.Radiometric,
            ThermalDeviceCapability.ManualCalibration,
            ThermalDeviceCapability.VideoRecording,
        )
    }

    internal fun cancelMonitoringForTests() {
        monitorJob?.cancel()
    }
}

