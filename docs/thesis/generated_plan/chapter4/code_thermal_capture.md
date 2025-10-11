# Chapter 4: Thermal Camera Frame Capture (USB)

        ## Code Snippet 4.2: Thermal Streaming and Recording Control

        ```kotlin
            override suspend fun startStreaming(): Flow<ThermalFrameData> =
    flow {
        if (!isConnected.get()) {
            throw IllegalStateException("Simulation not connected")
        }
        if (!isStreaming.compareAndSet(false, true)) {
            throw IllegalStateException("Simulation stream already active")
        }
        try {
            while (isStreaming.get()) {
                val frame = generateFrame()
                latestFrame = frame
                emit(frame)
                if (isRecording.get()) {
                    writeFrameMetadata(frame)
                }
                delay(frameIntervalMs.toLong())
            }
        } finally {
            isStreaming.set(false)
        }
    }

override suspend fun startRecording(): Result<Unit> {
    if (!isConnected.get()) {
        return Result.failure(IllegalStateException("Simulation not connected"))
    }
    if (isRecording.get()) {
        return Result.success(Unit)
    }
    val directory = File(context.filesDir, "thermal_simulation")
    if (!directory.exists()) {
        directory.mkdirs()
    }
    val fileName = "thermal_sim_${timeManager.getCurrentTimestampMs()}.csv"
    val file = File(directory, fileName)
    recordingWriter = FileWriter(file).apply {
        write("$CSV_HEADER\n")
        flush()
    }
    recordingFile = file
    isRecording.set(true)
    return Result.success(Unit)
}
        ```
