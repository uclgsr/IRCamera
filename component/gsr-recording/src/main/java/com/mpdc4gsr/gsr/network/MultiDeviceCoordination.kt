package com.mpdc4gsr.gsr.network

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class MultiDeviceCoordination(
    private val context: Context,
    private val networkClient: NetworkClient,
) {
    companion object {
        private const val TAG = "MultiDeviceCoordination"
        private const val SYNC_INTERVAL_MS = 1000L
        private const val LEADER_ELECTION_TIMEOUT = 5000L
        private const val HEARTBEAT_TIMEOUT = 3000L
        private const val MAX_SYNC_DRIFT_MS = 50L
    }

    private val coordinationJob = SupervisorJob()
    private val coordinationScope = CoroutineScope(Dispatchers.IO + coordinationJob)

    private val connectedDevices = ConcurrentHashMap<String, DeviceInfo>()
    private val syncEvents = ConcurrentHashMap<String, SyncEvent>()
    private val isCoordinating = AtomicBoolean(false)

    private var deviceId: String =
        android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID,
        )
    private var isLeader = AtomicBoolean(false)
    private var currentSessionId: String? = null
    private var syncJob: Job? = null

    data class DeviceInfo(
        val deviceId: String,
        val deviceName: String,
        val capabilities: List<String>,
        val lastHeartbeat: Long,
        val clockOffset: Long = 0L,
        val batteryLevel: Int = 100,
        val isRecording: Boolean = false,
    )

    data class SyncEvent(
        val eventId: String,
        val eventType: String,
        val scheduledTime: Long,
        val deviceResponses: MutableMap<String, Boolean> = mutableMapOf(),
        val isCompleted: Boolean = false,
    )

    enum class CoordinationEvent(val eventType: String) {
        SESSION_START("session_start"),
        SESSION_STOP("session_stop"),
        RECORDING_START("recording_start"),
        RECORDING_STOP("recording_stop"),
        SYNC_FLASH("sync_flash"),
        CALIBRATION("calibration"),
        TIME_SYNC("time_sync"),
    }

    suspend fun initializeCoordination(sessionId: String) =
        withContext(Dispatchers.IO) {
            currentSessionId = sessionId
            isCoordinating.set(true)

            startDeviceDiscovery()

            initiateLeaderElection()

            startSynchronizationLoop()        }

    private suspend fun startDeviceDiscovery() {
        val discoveryMessage =
            JSONObject().apply {
                put("type", "device_discovery")
                put("device_id", deviceId)
                put("device_name", android.os.Build.MODEL)
                put("capabilities", JSONArray(listOf("gsr", "thermal", "rgb_camera")))
                put("session_id", currentSessionId)
                put("timestamp", System.currentTimeMillis())
            }

        networkClient.broadcastMessage(discoveryMessage)

        networkClient.setMessageHandler("device_discovery_response") { message ->
            handleDeviceDiscoveryResponse(message)
        }

        networkClient.setMessageHandler("device_discovery") { message ->
            handleDeviceDiscoveryRequest(message)
        }
    }

    private fun handleDeviceDiscoveryResponse(message: JSONObject) {
        val remoteDeviceId = message.optString("device_id")
        if (remoteDeviceId.isEmpty() || remoteDeviceId == deviceId) return

        val deviceInfo =
            DeviceInfo(
                deviceId = remoteDeviceId,
                deviceName = message.optString("device_name", "Unknown"),
                capabilities = jsonArrayToList(message.optJSONArray("capabilities")),
                lastHeartbeat = System.currentTimeMillis(),
                clockOffset = message.optLong("clock_offset", 0L),
                batteryLevel = message.optInt("battery_level", 100),
            )

        connectedDevices[remoteDeviceId] = deviceInfo")
    }

    private fun handleDeviceDiscoveryRequest(message: JSONObject) {
        val response =
            JSONObject().apply {
                put("type", "device_discovery_response")
                put("device_id", deviceId)
                put("device_name", android.os.Build.MODEL)
                put("capabilities", JSONArray(listOf("gsr", "thermal", "rgb_camera")))
                put("session_id", currentSessionId)
                put("clock_offset", networkClient.getClockOffset())
                put("battery_level", getBatteryLevel())
                put("timestamp", System.currentTimeMillis())
            }

        coordinationScope.launch {
            networkClient.sendMessage(response)
        }
    }

    private suspend fun initiateLeaderElection() {
        val electionMessage =
            JSONObject().apply {
                put("type", "leader_election")
                put("device_id", deviceId)
                put("priority", calculateLeadershipPriority())
                put("timestamp", System.currentTimeMillis())
            }

        networkClient.broadcastMessage(electionMessage)

        delay(LEADER_ELECTION_TIMEOUT)
        determineLeader()

        networkClient.setMessageHandler("leader_election") { message ->
            handleLeaderElection(message)
        }
    }

    private fun calculateLeadershipPriority(): Int {
        var priority = 100

        priority += getBatteryLevel()

        priority += 50

        if (networkClient.isConnected()) priority += 25

        return priority
    }

    private fun handleLeaderElection(message: JSONObject) {
        val remoteDeviceId = message.optString("device_id")
        val remotePriority = message.optInt("priority", 0)
        val myPriority = calculateLeadershipPriority()

        if (remotePriority > myPriority ||
            (remotePriority == myPriority && remoteDeviceId < deviceId)
        ) {

            isLeader.set(false)
        }
    }

    private fun determineLeader() {
        if (isLeader.get()) {            startLeadershipDuties()
        } else {            startFollowerMode()
        }
    }

    private fun startLeadershipDuties() {
        coordinationScope.launch {
            while (isCoordinating.get() && isLeader.get()) {

                broadcastSyncSignal()

                checkDeviceHealth()


                processScheduledEvents()

                delay(SYNC_INTERVAL_MS)
            }
        }
    }

    private fun startFollowerMode() {
        networkClient.setMessageHandler("sync_signal") { message ->
            handleSyncSignal(message)
        }

        networkClient.setMessageHandler("coordination_event") { message ->
            handleCoordinationEvent(message)
        }
    }

    private suspend fun broadcastSyncSignal() {
        val syncMessage =
            JSONObject().apply {
                put("type", "sync_signal")
                put("leader_id", deviceId)
                put("session_id", currentSessionId)
                put("sync_timestamp", System.currentTimeMillis())
                put("device_count", connectedDevices.size + 1)
            }

        networkClient.broadcastMessage(syncMessage)
    }

    private fun handleSyncSignal(message: JSONObject) {
        val leaderTimestamp = message.optLong("sync_timestamp")
        val currentTime = System.currentTimeMillis()
        val drift = Math.abs(currentTime - leaderTimestamp)

        if (drift > MAX_SYNC_DRIFT_MS) {            coordinationScope.launch {
                requestTimeResync()
            }
        }


        sendHeartbeat()
    }

    private fun sendHeartbeat() {
        val heartbeatMessage =
            JSONObject().apply {
                put("type", "device_heartbeat")
                put("device_id", deviceId)
                put("timestamp", System.currentTimeMillis())
                put("battery_level", getBatteryLevel())
                put("is_recording", isDeviceRecording())
            }

        coordinationScope.launch {
            networkClient.sendMessage(heartbeatMessage)
        }
    }

    suspend fun scheduleCoordinatedEvent(
        eventType: CoordinationEvent,
        delayMs: Long = 1000L,
    ): String =
        withContext(Dispatchers.IO) {
            val eventId = generateEventId(eventType.eventType)
            val scheduledTime = System.currentTimeMillis() + delayMs

            val syncEvent =
                SyncEvent(
                    eventId = eventId,
                    eventType = eventType.eventType,
                    scheduledTime = scheduledTime,
                )

            syncEvents[eventId] = syncEvent


            val eventMessage =
                JSONObject().apply {
                    put("type", "coordination_event")
                    put("event_id", eventId)
                    put("event_type", eventType.eventType)
                    put("scheduled_time", scheduledTime)
                    put("session_id", currentSessionId)
                }

            networkClient.broadcastMessage(eventMessage)            eventId
        }

    private fun handleCoordinationEvent(message: JSONObject) {
        val eventId = message.optString("event_id")
        val eventType = message.optString("event_type")
        val scheduledTime = message.optLong("scheduled_time")


        coordinationScope.launch {
            val delay = scheduledTime - System.currentTimeMillis()
            if (delay > 0) {
                delay(delay)
            }

            executeCoordinatedEvent(eventType, eventId)
            sendEventConfirmation(eventId)
        }
    }

    private suspend fun executeCoordinatedEvent(
        eventType: String,
        eventId: String,
    ) {        when (eventType) {
            "session_start" -> handleSessionStart()
            "session_stop" -> handleSessionStop()
            "recording_start" -> handleRecordingStart()
            "recording_stop" -> handleRecordingStop()
            "sync_flash" -> handleSyncFlash()
            "calibration" -> handleCalibration()
            "time_sync" -> handleTimeSync()
        }
    }

    private suspend fun sendEventConfirmation(eventId: String) {
        val confirmationMessage =
            JSONObject().apply {
                put("type", "event_confirmation")
                put("event_id", eventId)
                put("device_id", deviceId)
                put("execution_timestamp", System.currentTimeMillis())
            }

        networkClient.sendMessage(confirmationMessage)
    }

    suspend fun triggerSyncFlash() {
        if (isLeader.get()) {
            scheduleCoordinatedEvent(CoordinationEvent.SYNC_FLASH, 500L)
        }
    }

    private suspend fun handleSyncFlash() {}")


        val flashIntent = android.content.Intent("com.mpdc4gsr.gsr.SYNC_FLASH")
        flashIntent.putExtra("timestamp", System.currentTimeMillis())
        context.sendBroadcast(flashIntent)
    }

    fun getCoordinationStatus(): CoordinationStatus {
        return CoordinationStatus(
            isCoordinating = isCoordinating.get(),
            isLeader = isLeader.get(),
            connectedDevicesCount = connectedDevices.size,
            connectedDevices = connectedDevices.values.toList(),
            activeEvents = syncEvents.size,
            currentSessionId = currentSessionId,
        )
    }

    data class CoordinationStatus(
        val isCoordinating: Boolean,
        val isLeader: Boolean,
        val connectedDevicesCount: Int,
        val connectedDevices: List<DeviceInfo>,
        val activeEvents: Int,
        val currentSessionId: String?,
    )


    private fun jsonArrayToList(jsonArray: JSONArray?): List<String> {
        val list = mutableListOf<String>()
        jsonArray?.let {
            for (i in 0 until it.length()) {
                list.add(it.optString(i))
            }
        }
        return list
    }

    private fun generateEventId(eventType: String): String {
        return "${eventType}_${deviceId}_${System.currentTimeMillis()}"
    }

    private fun getBatteryLevel(): Int {
        val batteryManager =
            context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        return batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun isDeviceRecording(): Boolean {

        return false // Placeholder
    }

    private suspend fun requestTimeResync() {    }

    private suspend fun handleSessionStart() {    }

    private suspend fun handleSessionStop() {    }

    private suspend fun handleRecordingStart() {    }

    private suspend fun handleRecordingStop() {    }

    private suspend fun handleCalibration() {    }

    private suspend fun handleTimeSync() {    }

    private fun checkDeviceHealth() {
        val currentTime = System.currentTimeMillis()
        connectedDevices.entries.removeAll { (_, device) ->
            val isStale = (currentTime - device.lastHeartbeat) > HEARTBEAT_TIMEOUT
            if (isStale) {            }
            isStale
        }
    }

    private fun processScheduledEvents() {

        syncEvents.entries.removeAll { (_, event) ->
            event.isCompleted
        }
    }

    private fun startSynchronizationLoop() {
        syncJob =
            coordinationScope.launch {
                while (isCoordinating.get()) {
                    if (isLeader.get()) {
                        broadcastSyncSignal()
                    }
                    delay(SYNC_INTERVAL_MS)
                }
            }
    }

    fun stopCoordination() {
        isCoordinating.set(false)
        isLeader.set(false)
        syncJob?.cancel()
        connectedDevices.clear()
        syncEvents.clear()
        coordinationJob.cancel()    }
}
