package mpdc4gsr.feature.camera.data

import mpdc4gsr.core.data.ErrorType

object CameraErrorMessageProvider {

    fun getUserFriendlyErrorMessage(errorType: ErrorType, originalMessage: String): String {
        return when (errorType) {
            ErrorType.PERMISSION_DENIED -> {
                "Camera permission required. Please:\n" +
                        "• Go to Settings > Apps > IRCamera > Permissions\n" +
                        "• Enable Camera permission\n" +
                        "• Restart the app and try again"
            }

            ErrorType.HARDWARE_UNAVAILABLE -> {
                "Camera not available. Please:\n" +
                        "• Close other camera apps\n" +
                        "• Restart your device if the issue persists\n" +
                        "• Check if camera hardware is functioning properly"
            }

            ErrorType.INITIALIZATION_FAILED -> {
                when {
                    originalMessage.contains("another application", ignoreCase = true) -> {
                        "Camera in use by another app. Please:\n" +
                                "• Close all camera and video apps\n" +
                                "• Wait a few seconds and try again\n" +
                                "• Restart the device if the problem continues"
                    }

                    originalMessage.contains("service unavailable", ignoreCase = true) -> {
                        "Camera service unavailable. Please:\n" +
                                "• Restart the camera app\n" +
                                "• If problem persists, restart your device\n" +
                                "• Check for system updates"
                    }

                    else -> {
                        "Camera initialization failed. Please:\n" +
                                "• Try switching between front/back camera\n" +
                                "• Restart the app\n" +
                                "• Check device storage space (need 1GB+ free)"
                    }
                }
            }

            ErrorType.RECORDING_FAILED -> {
                when {
                    originalMessage.contains("storage", ignoreCase = true) -> {
                        "Recording failed due to storage issues. Please:\n" +
                                "• Free up at least 2GB of storage space\n" +
                                "• Check if SD card is properly inserted\n" +
                                "• Try recording to internal storage instead"
                    }

                    originalMessage.contains("encoder", ignoreCase = true) -> {
                        "Video encoder error. Please:\n" +
                                "• Try recording at lower resolution (1080p instead of 4K)\n" +
                                "• Close other apps using camera/video\n" +
                                "• Restart the device if problem persists"
                    }

                    else -> {
                        "Recording failed. Please:\n" +
                                "• Check available storage space\n" +
                                "• Try recording at lower quality settings\n" +
                                "• Restart the app and try again"
                    }
                }
            }

            ErrorType.FEATURE_NOT_SUPPORTED -> {
                when {
                    originalMessage.contains("4K", ignoreCase = true) -> {
                        "4K recording not supported on this device. \n" +
                                "Alternative options:\n" +
                                "• Use 1080p recording (still high quality)\n" +
                                "• Enable 60fps if available\n" +
                                "• Check device specifications for camera capabilities"
                    }

                    originalMessage.contains("RAW", ignoreCase = true) -> {
                        "RAW capture not supported on this device.\n" +
                                "Alternative options:\n" +
                                "• Use highest quality JPEG settings\n" +
                                "• Enable HDR if available\n" +
                                "• Consider using manual exposure controls"
                    }

                    originalMessage.contains("60fps", ignoreCase = true) -> {
                        "60fps recording not supported at current resolution.\n" +
                                "Try these options:\n" +
                                "• Lower resolution to 1080p for 60fps\n" +
                                "• Use 30fps at current resolution\n" +
                                "• Check if device supports high-speed recording"
                    }

                    originalMessage.contains("focus", ignoreCase = true) -> {
                        "Manual focus control not fully supported.\n" +
                                "Available alternatives:\n" +
                                "• Use tap-to-focus on preview\n" +
                                "• Enable continuous autofocus\n" +
                                "• Lock focus after tapping to focus"
                    }

                    originalMessage.contains("exposure", ignoreCase = true) -> {
                        "Advanced exposure control not supported.\n" +
                                "Available alternatives:\n" +
                                "• Use exposure compensation slider\n" +
                                "• Enable auto-exposure lock\n" +
                                "• Adjust scene mode settings"
                    }

                    else -> {
                        "Feature not supported on this device.\n" +
                                "• Check device specifications\n" +
                                "• Try alternative settings\n" +
                                "• Update to latest app version"
                    }
                }
            }

            ErrorType.OPERATION_FAILED -> {
                when {
                    originalMessage.contains("focus", ignoreCase = true) -> {
                        "Focus operation failed. Please:\n" +
                                "• Clean camera lens\n" +
                                "• Ensure adequate lighting\n" +
                                "• Try tapping different areas to focus"
                    }

                    originalMessage.contains("exposure", ignoreCase = true) -> {
                        "Exposure adjustment failed. Please:\n" +
                                "• Reset to auto-exposure mode\n" +
                                "• Adjust lighting conditions\n" +
                                "• Try smaller exposure compensation values"
                    }

                    else -> {
                        "Camera operation failed. Please:\n" +
                                "• Try the operation again\n" +
                                "• Reset camera settings to default\n" +
                                "• Restart the app if problem continues"
                    }
                }
            }

            ErrorType.DEVICE_NOT_SUPPORTED -> {
                "Device compatibility issue detected.\n" +
                        "Possible solutions:\n" +
                        "• Update Android to latest version\n" +
                        "• Enable Camera2 API in developer options\n" +
                        "• Use basic camera features only"
            }

            ErrorType.SYNC_FAILED -> {
                "Synchronization failed. This may affect:\n" +
                        "• Multi-sensor data alignment\n" +
                        "• Timestamp accuracy\n" +
                        "Recording can continue but check results carefully."
            }

            else -> {
                "Camera error occurred. Please:\n" +
                        "• Try restarting the app\n" +
                        "• Check device camera functionality\n" +
                        "• Contact support if problem persists\n\n" +
                        "Error details: $originalMessage"
            }
        }
    }

    fun getShortErrorMessage(errorType: ErrorType): String {
        return when (errorType) {
            ErrorType.PERMISSION_DENIED -> "Camera permission required - check Settings"
            ErrorType.HARDWARE_UNAVAILABLE -> "Camera unavailable - close other camera apps"
            ErrorType.INITIALIZATION_FAILED -> "Camera initialization failed - restart app"
            ErrorType.RECORDING_FAILED -> "Recording failed - check storage space"
            ErrorType.FEATURE_NOT_SUPPORTED -> "Feature not supported on this device"
            ErrorType.OPERATION_FAILED -> "Camera operation failed - try again"
            ErrorType.DEVICE_NOT_SUPPORTED -> "Device not supported - update Android"
            ErrorType.SYNC_FAILED -> "Synchronization failed - recording may continue"
            else -> "Camera error - restart app"
        }
    }

    fun getPerformanceSuggestions(
        deviceSupports4K: Boolean,
        supportsRAW: Boolean,
        supports60fps: Boolean
    ): List<String> {
        val suggestions = mutableListOf<String>()
        if (!deviceSupports4K) {
            suggestions.add("• Device doesn't support 4K - use 1080p for best quality")
        }
        if (!supports60fps) {
            suggestions.add("• 60fps not available - use 30fps for stability")
        }
        if (!supportsRAW) {
            suggestions.add("• RAW capture not supported - use maximum JPEG quality")
        }
        suggestions.addAll(
            listOf(
                "• Close unnecessary apps before recording",
                "• Ensure device has 20%+ battery remaining",
                "• Use well-lit environments for better focus",
                "• Keep device cool to prevent thermal throttling",
                "• Free up storage space (recommended: 5GB+)"
            )
        )
        return suggestions
    }
}