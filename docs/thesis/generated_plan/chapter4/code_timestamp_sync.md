# Chapter 4: Timestamp Synchronization Logic

        ## Code Snippet 4.3: TimeSyncClient UDP Loop and Calibration Polling

        ```kotlin
            private suspend fun runUdpLoop() = withContext(dispatcher) {
    val address = InetSocketAddress(parseHost(endpoint), parsePort(endpoint))
    DatagramSocket().use { socket ->
        socket.soTimeout = UDP_TIMEOUT_MILLIS
        val buffer = ByteArray(32)
        while (isActive) {
            try {
                val t0 = System.nanoTime()
                val packet = ByteBuffer.allocate(16).order(ByteOrder.BIG_ENDIAN)
                packet.putLong(t0)
                val outgoing = DatagramPacket(packet.array(), packet.position(), address)
                socket.send(outgoing)

                val incomingPacket = DatagramPacket(buffer, buffer.size)
                socket.receive(incomingPacket)
                val t3 = System.nanoTime()

                val response = ByteBuffer.wrap(buffer).order(ByteOrder.BIG_ENDIAN)
                val t1 = response.long
                val t2 = response.long
                val offset = ((t1 - t0) + (t2 - t3)) / 2.0 / 1_000_000.0
                val roundTrip = (t3 - t0 - (t2 - t1)) / 1_000_000.0
                val drift = ((t2 - t1) - (t3 - t0)) / (t3 - t0).toDouble() * 1_000_000.0

                val estimate =
                    TimelineEstimate(
                        referenceEpochMillis = System.currentTimeMillis(),
                        offsetMillis = offset,
                        roundTripMillis = roundTrip,
                        driftPpm = drift,
                        accuracyMillis = abs(offset) + roundTrip / 2.0,
                        lastUpdated = Instant.now(),
                    )
                sessionController.updateTimelineEstimate(estimate)
            } catch (ex: Exception) {
                Log.w(TAG, "UDP sync failed", ex)
                delay(UDP_RETRY_DELAY_MILLIS)
            }
            delay(UDP_INTERVAL_MILLIS)
        }
    }

}

private suspend fun pollCalibration() {
while (scope.isActive) {
try {
val request =
Request.Builder()
.url("$endpoint/time/calibration")
.build()
val response = okHttpClient.newCall(request).execute()
response.use {
if (!it.isSuccessful) {
Log.w(TAG, "Calibration endpoint failure: ${it.code}")
} else {
val body = it.body?.string()
if (body != null) {
val calibration = json.decodeFromString(TimeCalibration.serializer(), body)
val estimate =
TimelineEstimate(
referenceEpochMillis = calibration.referenceEpochMillis,
offsetMillis = calibration.offsetMillis,
roundTripMillis = calibration.roundTripMillis,
driftPpm = calibration.driftPpm,
accuracyMillis = calibration.accuracyMillis,
lastUpdated = Instant.now(),
)
sessionController.updateTimelineEstimate(estimate)
}
}
}
} catch (ex: Exception) {
Log.e(TAG, "Failed to poll calibration", ex)
}
delay(CALIBRATION_INTERVAL_MILLIS)
}
}
```
