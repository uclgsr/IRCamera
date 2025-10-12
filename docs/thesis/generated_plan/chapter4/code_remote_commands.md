# Chapter 4: Remote Command Handling (TCP Server)

        ## Code Snippet 4.4: Command Dispatch and Session Handling

        ```kotlin
            private suspend fun handleCommand(envelope: CommandEnvelope) {
    val name = envelope.command?.lowercase() ?: run {
        Log.w(TAG, "Command envelope missing command field: $envelope")
        return
    }
    val commandId = envelope.commandId
    val requiresAck = envelope.requiresAck
    try {
        when (name) {
            "start_recording" -> handleStartRecording(envelope.payload)
            "stop_recording" -> handleStopRecording(envelope.payload)
            "stimulus_marker" -> handleStimulus("stimulus_marker", envelope.payload)
            "flash_sync" -> handleStimulus("flash_sync", envelope.payload)
            "audio_beep" -> handleStimulus("audio_beep", envelope.payload)
            "set_simulation_mode" -> handleSimulationCommand(envelope.payload)
            else -> {
                Log.w(TAG, "Unknown command type received: $name")
                if (requiresAck && commandId != null) {
                    sendAck(commandId, "error", "unknown_command")
                }
                return
            }
        }
        if (requiresAck && commandId != null) {
            sendAck(commandId, "ok")
        }
    } catch (error: Exception) {
        Log.e(TAG, "Failed to handle command '$name'", error)
        if (requiresAck && commandId != null) {
            sendAck(commandId, "error", error.message ?: "error")
        }
    }

}

private suspend fun handleStartRecording(payload: JsonObject) {
val sessionId =
payload["session_id"]?.jsonPrimitive?.content
?: payload["sessionId"]?.jsonPrimitive?.content
?: "unknown"
val modalities =
payload["modalities"]?.jsonArray?.mapNotNull {
it.jsonPrimitive.contentOrNull?.let { name ->
runCatching { RecorderKind.valueOf(name.uppercase()) }.getOrNull()
}
}?.toSet()
?: RecorderKind.values().toSet()
val scheduled = payload["scheduledTimeMillis"]?.jsonPrimitive?.longOrNull ?: 0L
val parameters = payload.toKotlinMap()
val command =
SessionCommand.StartRecording(
sessionId = sessionId,
modalities = modalities,
scheduledTimeMillis = scheduled,
parameters = parameters,
)
_events.emit(command)
}
```
