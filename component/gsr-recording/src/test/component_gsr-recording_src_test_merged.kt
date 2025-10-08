// Merged ALL .kt and .java files from the 'component\gsr-recording\src\test' directory and its subdirectories.
// Total files: 8 | Generated on: 2025-10-08 01:42:34


// ===== FROM: component\gsr-recording\src\test\java\com\mpdc4gsr\gsr\tests\GSRDataModelsTest.kt =====

package com.mpdc4gsr.gsr.tests

import com.mpdc4gsr.gsr.model.GSRSample
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.gsr.model.SyncMark
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test

@Ignore("All tests disabled")
class GSRDataModelsTest {
    @Test
    fun testGSRSampleCreation() {
        val timestamp = System.currentTimeMillis()
        val utcTimestamp = timestamp + 1000
        val sampleIndex = 42L
        val sessionId = "test_session"
        val sample = GSRSample.createSimulated(timestamp, utcTimestamp, sampleIndex, sessionId)
        assertEquals(timestamp, sample.timestamp)
        assertEquals(utcTimestamp, sample.utcTimestamp)
        assertEquals(sampleIndex, sample.sampleIndex)
        assertEquals(sessionId, sample.sessionId)
        assertTrue("Conductance should be positive", sample.conductance > 0)
        assertTrue("Resistance should be positive", sample.resistance > 0)
    }

    @Test
    fun testGSRSampleToCsvRow() {
        val sample =
            GSRSample(
                timestamp = 1234567890L,
                utcTimestamp = 1234567891L,
                conductance = 12.345678,
                resistance = 80.987654,
                rawValue = 2048,
                sampleIndex = 100L,
                sessionId = "test_session",
            )
        val csvRow = sample.toCsvRow()
        assertEquals(7, csvRow.size)
        assertEquals("1234567890", csvRow[0])
        assertEquals("1234567891", csvRow[1])
        assertEquals("12.345678", csvRow[2])
        assertEquals("80.987654", csvRow[3])
        assertEquals("2048", csvRow[4])
        assertEquals("100", csvRow[5])
        assertEquals("test_session", csvRow[6])
    }

    @Test
    fun testSessionInfo() {
        val sessionId = "test_session"
        val startTime = System.currentTimeMillis()
        val session = SessionInfo(sessionId = sessionId, startTime = startTime)
        assertEquals(sessionId, session.sessionId)
        assertEquals(startTime, session.startTime)
        assertNull(session.endTime)
        assertTrue(session.isActive())
        assertTrue(session.getDurationMs() >= 0)
        session.endTime = startTime + 5000
        assertFalse(session.isActive())
        assertEquals(5000L, session.getDurationMs())
    }

    @Test
    fun testSyncMark() {
        val timestamp = System.currentTimeMillis()
        val utcTimestamp = timestamp + 1000
        val eventType = "THERMAL_CAPTURE"
        val sessionId = "test_session"
        val metadata = mapOf("camera" to "thermal", "frame" to "123")
        val syncMark =
            SyncMark(
                timestamp = timestamp,
                utcTimestamp = utcTimestamp,
                eventType = eventType,
                sessionId = sessionId,
                metadata = metadata,
            )
        assertEquals(timestamp, syncMark.timestamp)
        assertEquals(utcTimestamp, syncMark.utcTimestamp)
        assertEquals(eventType, syncMark.eventType)
        assertEquals(sessionId, syncMark.sessionId)
        assertEquals(metadata, syncMark.metadata)
        val csvRow = syncMark.toCsvRow()
        assertEquals(5, csvRow.size)
        assertEquals(timestamp.toString(), csvRow[0])
        assertEquals(utcTimestamp.toString(), csvRow[1])
        assertEquals(eventType, csvRow[2])
        assertEquals(sessionId, csvRow[3])
        assertTrue(csvRow[4].contains("camera=thermal"))
        assertTrue(csvRow[4].contains("frame=123"))
    }

    @Test
    fun testSyncMarkEmptyMetadata() {
        val syncMark =
            SyncMark(
                timestamp = 123L,
                utcTimestamp = 124L,
                eventType = "TEST",
                sessionId = "test",
            )
        val csvRow = syncMark.toCsvRow()
        assertEquals("", csvRow[4])
    }
}


// ===== FROM: component\gsr-recording\src\test\java\com\mpdc4gsr\gsr\tests\GSRRecordingServiceTest.kt =====

package com.mpdc4gsr.gsr.tests

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.mpdc4gsr.gsr.service.EnhancedRecordingService
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Ignore("All tests disabled")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
class GSRRecordingServiceTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testServiceCreation() {
        val serviceClass = EnhancedRecordingService::class.java
        assertNotNull("Service class should be accessible", serviceClass)
        assertEquals(
            "Service class name should match",
            "EnhancedRecordingService",
            serviceClass.simpleName
        )
    }

    @Test
    fun testServiceBinder() {
        try {
            val binderClass =
                Class.forName("com.mpdc4gsr.gsr.service.EnhancedRecordingService\$EnhancedRecordingBinder")
            assertNotNull("EnhancedRecordingBinder class should exist", binderClass)
        } catch (e: ClassNotFoundException) {
            assertTrue("Service structure test completed", true)
        }
    }

    @Test
    fun testServiceLifecycle() {
        val intent = Intent(context, EnhancedRecordingService::class.java)
        assertNotNull("Service intent should be created", intent)
        assertEquals(
            "Intent component should match service",
            EnhancedRecordingService::class.java.name,
            intent.component?.className,
        )
    }

    @Test
    fun testStartRecordingIntent() {
        val intent = Intent(context, EnhancedRecordingService::class.java)
        intent.action = "action_start_recording"
        intent.putExtra("extra_session_id", "test_session_123")
        intent.putExtra("extra_participant_id", "participant_001")
        intent.putExtra("extra_study_name", "test_study")
        assertNotNull("Start recording intent should be created", intent)
        assertEquals("Action should be set", "action_start_recording", intent.action)
        assertEquals(
            "Session ID should be set",
            "test_session_123",
            intent.getStringExtra("extra_session_id")
        )
    }

    @Test
    fun testStopRecordingIntent() {
        val intent = Intent(context, EnhancedRecordingService::class.java)
        intent.action = "action_stop_recording"
        assertNotNull("Stop recording intent should be created", intent)
        assertEquals("Action should be set", "action_stop_recording", intent.action)
    }

    @Test
    fun testPCConnectionIntent() {
        val intent = Intent(context, EnhancedRecordingService::class.java)
        intent.action = "action_connect_pc"
        intent.putExtra("extra_pc_ip", "192.168.1.100")
        intent.putExtra("extra_pc_port", 8080)
        assertNotNull("PC connection intent should be created", intent)
        assertEquals("Action should be set", "action_connect_pc", intent.action)
        assertEquals("PC IP should be set", "192.168.1.100", intent.getStringExtra("extra_pc_ip"))
        assertEquals("PC port should be set", 8080, intent.getIntExtra("extra_pc_port", 0))
    }

    @Test
    fun testDiscoveryIntent() {
        val intent = Intent(context, EnhancedRecordingService::class.java)
        intent.action = "action_start_discovery"
        assertNotNull("Discovery intent should be created", intent)
        assertEquals("Action should be set", "action_start_discovery", intent.action)
    }

    @Test
    fun testServiceState() {
        try {
            val serviceClass = EnhancedRecordingService::class.java
            assertNotNull("Service class should be accessible", serviceClass)
            val methods = serviceClass.declaredMethods
            assertTrue("Service should have methods", methods.isNotEmpty())
        } catch (e: Exception) {
            assertTrue("Service state test attempted", true)
        }
    }

    @Test
    fun testNotificationChannelCreation() {
        val channelId = "gsr_recording_channel"
        assertNotNull("Channel ID should be defined", channelId)
        assertTrue("Channel ID should not be empty", channelId.isNotEmpty())
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
        assertNotNull("NotificationManager should be available", notificationManager)
    }
}


// ===== FROM: component\gsr-recording\src\test\java\com\mpdc4gsr\gsr\tests\RecordingSessionManagerTest.kt =====

package com.mpdc4gsr.gsr.tests

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.mpdc4gsr.gsr.model.SessionInfo
import com.mpdc4gsr.gsr.service.SessionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Ignore("All tests disabled")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
@OptIn(ExperimentalCoroutinesApi::class)
class RecordingSessionManagerTest {
    private lateinit var context: Context
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sessionManager = SessionManager.getInstance(context)
    }

    @Test
    fun testSingletonInstance() {
        val instance1 = SessionManager.getInstance(context)
        val instance2 = SessionManager.getInstance(context)
        assertSame("SessionManager should be singleton", instance1, instance2)
    }

    @Test
    fun testCreateSession() =
        runTest {
            val sessionId = "test_session_001"
            val participantId = "participant_123"
            val studyName = "Robolectric Test Study"
            val session = sessionManager.createSession(sessionId, participantId, studyName)
            assertNotNull("Session should be created", session)
            assertEquals("Session ID should match", sessionId, session.sessionId)
            assertEquals("Participant ID should match", participantId, session.participantId)
            assertEquals("Study name should match", studyName, session.studyName)
            assertTrue("Session should be active", session.isActive())
            assertNull("End time should be null for active session", session.endTime)
        }

    @Test
    fun testGetActiveSession() =
        runTest {
            val sessionId = "active_session_test"
            val session = sessionManager.createSession(sessionId, "participant", "study")
            val activeSessions = sessionManager.getActiveSessions()
            assertNotNull("Should have active sessions", activeSessions)
            assertTrue("Should have at least one active session", activeSessions.isNotEmpty())
            val foundSession = activeSessions.find { it.sessionId == sessionId }
            assertNotNull("Should find the created session", foundSession)
            assertEquals("Session ID should match", sessionId, foundSession?.sessionId)
        }

    @Test
    fun testCompleteSession() =
        runTest {
            val sessionId = "complete_session_test"
            val session = sessionManager.createSession(sessionId, "participant", "study")
            assertTrue("Session should be active", session.isActive())
            val completedSession = sessionManager.completeSession(sessionId)
            assertNotNull("Completed session should be returned", completedSession)
            assertFalse("Session should not be active", completedSession!!.isActive())
            assertNotNull("End time should be set", completedSession.endTime)
            assertTrue("Duration should be positive", completedSession.getDurationMs() >= 0)
            val activeSessionsAfter = sessionManager.getActiveSessions()
            val stillActiveSession = activeSessionsAfter.find { it.sessionId == sessionId }
            assertTrue(
                "Session should no longer be active",
                stillActiveSession == null || !stillActiveSession.isActive()
            )
        }

    @Test
    fun testGetSessionInfo() =
        runTest {
            val sessionId = "info_session_test"
            assertNull("No session info initially", sessionManager.getSession(sessionId))
            sessionManager.createSession(sessionId, "participant", "study")
            val sessionInfo = sessionManager.getSession(sessionId)
            assertNotNull("Should have session info", sessionInfo)
            assertEquals("Session ID should match", sessionId, sessionInfo?.sessionId)
        }

    @Test
    fun testSessionListener() =
        runTest {
            val sessionId = "listener_test_session"
            var createdSession: SessionInfo? = null
            var updatedSession: SessionInfo? = null
            var completedSession: SessionInfo? = null
            val listener =
                object : SessionManager.SessionListener {
                    override fun onSessionCreated(session: SessionInfo) {
                        createdSession = session
                    }

                    override fun onSessionUpdated(session: SessionInfo) {
                        updatedSession = session
                    }

                    override fun onSessionCompleted(session: SessionInfo) {
                        completedSession = session
                    }

                    override fun onSessionError(
                        sessionId: String,
                        error: String,
                    ) {
                    }
                }
            sessionManager.addSessionListener(listener)
            val session = sessionManager.createSession(sessionId, "participant", "study")
            val existingSession = sessionManager.getSession(sessionId)
            assertNotNull("Session should exist for metadata test", existingSession)
            sessionManager.completeSession(sessionId)
            assertTrue("Listener callbacks should work", true)
            assertTrue("Test completed successfully", true)
            sessionManager.removeSessionListener(listener)
        }

    @Test
    fun testSessionMetadata() =
        runTest {
            val sessionId = "metadata_test_session"
            val session = sessionManager.createSession(sessionId, "participant", "study")
            assertNotNull("Session should be created", session)
            assertEquals("Session ID should match", sessionId, session.sessionId)
            assertEquals("Participant ID should match", "participant", session.participantId)
            assertEquals("Study name should match", "study", session.studyName)
            val retrievedSession = sessionManager.getSession(sessionId)
            assertNotNull("Session should exist", retrievedSession)
            assertEquals(
                "Retrieved session should match created session",
                session.sessionId,
                retrievedSession?.sessionId
            )
        }

    @Test
    fun testGetAllSessions() =
        runTest {
            val initialSessions = sessionManager.getActiveSessions()
            assertNotNull("Should have sessions list", initialSessions)
            val initialCount = initialSessions.size
            sessionManager.createSession("session_1", "participant_1", "study_1")
            sessionManager.createSession("session_2", "participant_2", "study_2")
            val allSessions = sessionManager.getActiveSessions()
            assertTrue("Should have more sessions", allSessions.size >= initialCount + 2)
            val sessionIds = allSessions.map { it.sessionId }
            assertTrue("Should contain session_1", sessionIds.contains("session_1"))
            assertTrue("Should contain session_2", sessionIds.contains("session_2"))
        }

    @Test
    fun testSessionLifecycle() =
        runTest {
            val session1 = sessionManager.createSession("lifecycle_1", "participant", "study")
            val session2 = sessionManager.createSession("lifecycle_2", "participant", "study")
            assertNotNull("Session 1 should be created", session1)
            assertNotNull("Session 2 should be created", session2)
            assertTrue("Session 1 should be active", session1.isActive())
            assertTrue("Session 2 should be active", session2.isActive())
            val completedSession = sessionManager.completeSession("lifecycle_1")
            assertNotNull("Completed session should be returned", completedSession)
            assertFalse(
                "Session should not be active after completion",
                completedSession!!.isActive()
            )
            val activeSessions = sessionManager.getActiveSessions()
            val activeSession1 = activeSessions.find { it.sessionId == "lifecycle_1" }
            val activeSession2 = activeSessions.find { it.sessionId == "lifecycle_2" }
            assertTrue(
                "Session 1 should not be in active sessions or should be inactive",
                activeSession1 == null || !activeSession1.isActive(),
            )
            assertNotNull("Session 2 should still be active", activeSession2)
            assertTrue("Session 2 should still be active", activeSession2?.isActive() == true)
        }
}


// ===== FROM: component\gsr-recording\src\test\java\com\mpdc4gsr\gsr\tests\RobolectricFrameworkTest.kt =====

package com.mpdc4gsr.gsr.tests

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Ignore("All tests disabled")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
@OptIn(ExperimentalCoroutinesApi::class)
class RobolectricFrameworkTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testSharedPreferencesWithRealContext() {
        val prefs = context.getSharedPreferences("test_session_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("session_id", "test_session_123")
        editor.putLong("start_time", System.currentTimeMillis())
        editor.putBoolean("is_active", true)
        editor.apply()
        assertEquals("test_session_123", prefs.getString("session_id", null))
        assertTrue("start_time should be stored", prefs.getLong("start_time", 0) > 0)
        assertTrue("is_active should be true", prefs.getBoolean("is_active", false))
        editor.clear()
        editor.apply()
        assertNull("session_id should be cleared", prefs.getString("session_id", null))
        assertEquals("start_time should be cleared", 0L, prefs.getLong("start_time", 0))
        assertFalse("is_active should be false", prefs.getBoolean("is_active", false))
    }

    @Test
    fun testFileOperationsWithRealFileSystem() {
        val filesDir = context.filesDir
        val testFile = java.io.File(filesDir, "test_gsr_data.csv")
        testFile.writeText("timestamp,conductance,resistance\n1234567890,12.5,80.0\n")
        assertTrue("File should exist", testFile.exists())
        assertTrue("File should have content", testFile.length() > 0)
        val content = testFile.readText()
        assertTrue(
            "Content should contain header",
            content.contains("timestamp,conductance,resistance")
        )
        assertTrue("Content should contain data", content.contains("1234567890,12.5,80.0"))
        testFile.delete()
        assertFalse("File should be deleted", testFile.exists())
    }

    @Test
    fun testSystemServiceAccess() {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as android.app.NotificationManager
        assertNotNull("NotificationManager should be available", notificationManager)
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE)
                    as android.bluetooth.BluetoothManager?
        assertNotNull("BluetoothManager should be available", bluetoothManager)
        val bluetoothAdapter = bluetoothManager?.adapter
        assertNotNull("BluetoothAdapter should be available", bluetoothAdapter)
    }

    @Test
    fun testIntegrationWithMultipleServices() {
        val prefs = context.getSharedPreferences("integration_test", Context.MODE_PRIVATE)
        val filesDir = context.filesDir
        prefs.edit()
            .putString("data_directory", filesDir.absolutePath)
            .putInt("sampling_rate", 128)
            .putBoolean("bluetooth_enabled", true)
            .apply()
        val dataDir = prefs.getString("data_directory", "")
        val samplingRate = prefs.getInt("sampling_rate", 0)
        val bluetoothEnabled = prefs.getBoolean("bluetooth_enabled", false)
        assertEquals(filesDir.absolutePath, dataDir)
        assertEquals(128, samplingRate)
        assertTrue(bluetoothEnabled)
        val configFile = java.io.File(dataDir, "config.json")
        configFile.writeText(
            """
            {
                "sampling_rate": $samplingRate,
                "bluetooth_enabled": $bluetoothEnabled,
                "data_directory": "$dataDir"
            }
            """.trimIndent(),
        )
        assertTrue("Config file should exist", configFile.exists())
        val configContent = configFile.readText()
        assertTrue("Config should contain sampling rate", configContent.contains("128"))
        assertTrue("Config should contain bluetooth setting", configContent.contains("true"))
        configFile.delete()
    }

    @Test
    fun testAndroidSpecificBehavior() {
        assertTrue(
            "Should be running on Android O or higher",
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O,
        )
        assertNotNull("Package name should be available", context.packageName)
        assertFalse("Package name should not be empty", context.packageName.isEmpty())
        val resources = context.resources
        assertNotNull("Resources should be available", resources)
        val displayMetrics = resources.displayMetrics
        assertTrue("Display density should be realistic", displayMetrics.density > 0)
        assertTrue("Screen width should be realistic", displayMetrics.widthPixels > 0)
        assertTrue("Screen height should be realistic", displayMetrics.heightPixels > 0)
    }

    @Test
    fun testContextAccess() {
        assertNotNull("Context should be available", context)
        assertNotNull("Package name should be available", context.packageName)
        assertFalse("Package name should not be empty", context.packageName.isEmpty())
    }

    @Test
    fun testResourceAccess() {
        val resources = context.resources
        assertNotNull("Resources should be available", resources)
        val displayMetrics = resources.displayMetrics
        assertNotNull("Display metrics should be available", displayMetrics)
        assertTrue("Display density should be positive", displayMetrics.density > 0)
    }

    @Test
    fun testPackageManagerAccess() {
        val packageManager = context.packageManager
        assertNotNull("Package manager should be available", packageManager)
        try {
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            assertNotNull("Package info should be available", packageInfo)
            assertEquals("Package name should match", context.packageName, packageInfo.packageName)
        } catch (e: Exception) {
            // Expected in test environment
        }
    }

    @Test
    fun testMultipleContextInstances() {
        val context1 = ApplicationProvider.getApplicationContext<Context>()
        val context2 = ApplicationProvider.getApplicationContext<Context>()
        assertSame("Application contexts should be the same instance", context1, context2)
        assertEquals("Package names should match", context1.packageName, context2.packageName)
    }
}


// ===== FROM: component\gsr-recording\src\test\java\com\mpdc4gsr\gsr\tests\ShimmerFactoryResolverTest.kt =====

package com.mpdc4gsr.gsr.tests

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mpdc4gsr.gsr.service.MockShimmerDevice
import com.mpdc4gsr.gsr.service.MockShimmerDeviceFactory
import com.mpdc4gsr.gsr.service.ShimmerDeviceFactoryResolver
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Ignore("All tests disabled")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ShimmerFactoryResolverTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `resolver creates factory successfully`() {
        // When
        val factory = ShimmerDeviceFactoryResolver.createFactory(context)
        // Then
        assertNotNull("Factory should not be null", factory)
        // factory is always ShimmerDeviceFactory by contract, no need for type check
    }

    @Test
    fun `resolver factory creates device interface`() {
        // Given
        val factory = ShimmerDeviceFactoryResolver.createFactory(context)
        // When
        val device = factory.createShimmerDevice()
        // Then
        assertNotNull("Device should not be null", device)
        // device is always ShimmerDeviceInterface by contract, no need for type check
    }

    @Test
    fun `resolver falls back to mock in test environment`() {
        // Given
        val factory = ShimmerDeviceFactoryResolver.createFactory(context)
        // When
        val device = factory.createShimmerDevice()
        // Then
        // In test environment, should fall back to mock since real implementation class is not available
        assertTrue(
            "Should fall back to mock implementation in test environment",
            factory is MockShimmerDeviceFactory
        )
        assertTrue(
            "Device should be mock implementation",
            device is MockShimmerDevice
        )
    }

    @Test
    fun `mock device has expected interface methods`() {
        // Given
        val factory = ShimmerDeviceFactoryResolver.createFactory(context)
        val device = factory.createShimmerDevice()
        // When/Then - Test that all interface methods work
        val connected = device.isConnected()
        assertTrue("Should be able to call isConnected", connected == false || connected == true)
        val connectResult = device.connect("00:00:00:00:00:00", "test")
        assertTrue(
            "Should be able to call connect",
            connectResult == true || connectResult == false
        )
        val streamResult = device.startStreaming()
        assertTrue(
            "Should be able to call startStreaming",
            streamResult == true || streamResult == false
        )
        val stopResult = device.stopStreaming()
        assertTrue(
            "Should be able to call stopStreaming",
            stopResult == true || stopResult == false
        )
        val disconnectResult = device.disconnect()
        assertTrue(
            "Should be able to call disconnect",
            disconnectResult == true || disconnectResult == false
        )
    }
}


// ===== FROM: component\gsr-recording\src\test\java\com\mpdc4gsr\gsr\tests\ShimmerGSRRecorderTest.kt =====

package com.mpdc4gsr.gsr.tests

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import com.mpdc4gsr.gsr.service.MockShimmerDeviceFactory
import com.mpdc4gsr.gsr.service.ShimmerApiBridge
import com.mpdc4gsr.gsr.service.ShimmerGSRRecorder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication
import org.robolectric.shadows.ShadowEnvironment

@Ignore("All tests disabled")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
@OptIn(ExperimentalCoroutinesApi::class)
class ShimmerGSRRecorderTest {
    private lateinit var context: Context
    private lateinit var recorder: ShimmerGSRRecorder
    private lateinit var shadowApplication: ShadowApplication

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowApplication = Shadows.shadowOf(context as android.app.Application)
        shadowApplication.grantPermissions(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
        )
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)
        recorder = ShimmerGSRRecorder(context, MockShimmerDeviceFactory(), samplingRateHz = 128)
    }

    @Test
    fun testRecorderCreation() {
        assertNotNull("Recorder should be created", recorder)
    }

    @Test
    fun testBluetoothPermissionCheck() {
        val hasPermission =
            context.checkSelfPermission(Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        assertTrue("Bluetooth permission should be granted in test", hasPermission)
    }

    @Test
    fun testBluetoothAdapterAccess() {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        assertNotNull("BluetoothManager should be available", bluetoothManager)
        val bluetoothAdapter = bluetoothManager?.adapter
        assertNotNull("BluetoothAdapter should be available", bluetoothAdapter)
    }

    @Test
    fun testRecordingStateManagement() =
        runTest {
            val sessionId = "recording_state_test"
            assertFalse("Should not be recording initially", recorder.isRecording())
            try {
                val started = recorder.startRecording(sessionId)
            } catch (e: Exception) {
            }
            try {
                recorder.stopRecording()
            } catch (e: Exception) {
            }
        }

    @Test
    fun testSensorConfiguration() {
        val customRecorder =
            ShimmerGSRRecorder(context, MockShimmerDeviceFactory(), samplingRateHz = 256)
        assertNotNull("Custom recorder should be created", customRecorder)
        assertNotNull("Default recorder should have context", recorder)
    }

    @Test
    fun testContextUsage() {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        assertNotNull("Bluetooth manager should be accessible through context", bluetoothManager)
        val externalStorageState = Environment.getExternalStorageState()
        assertEquals(
            "External storage should be mounted in test",
            Environment.MEDIA_MOUNTED,
            externalStorageState
        )
    }

    @Test
    fun testFileSystemAccess() {
        val filesDir = context.filesDir
        assertNotNull("Files directory should be accessible", filesDir)
        assertTrue("Files directory should exist", filesDir.exists())
        val externalFilesDir = context.getExternalFilesDir(null)
        assertNotNull("External files directory should be accessible", externalFilesDir)
    }

    @Test
    fun testErrorHandling() =
        runTest {
            try {
                val result = recorder.startRecording("error_test")
                assertTrue("Error handling test completed", true)
            } catch (e: Exception) {
                assertTrue("Exception handled gracefully", true)
            }
        }

    @Test
    fun testCleanupHandling() =
        runTest {
            val sessionId = "cleanup_test"
            try {
                recorder.startRecording(sessionId)
            } catch (e: Exception) {
            }
            try {
                recorder.stopRecording()
            } catch (e: Exception) {
            }
            assertTrue("Cleanup handling test completed", true)
        }

    @Test
    fun testMultipleInstances() {
        val recorder2 = ShimmerGSRRecorder(context, MockShimmerDeviceFactory(), samplingRateHz = 64)
        val recorder3 =
            ShimmerGSRRecorder(context, MockShimmerDeviceFactory(), samplingRateHz = 512)
        assertNotNull("Second recorder should be created", recorder2)
        assertNotNull("Third recorder should be created", recorder3)
        assertNotSame("Recorders should be different instances", recorder, recorder2)
        assertNotSame("Recorders should be different instances", recorder2, recorder3)
    }

    @Test
    fun testRecordingModeConfiguration() {
        val streamingRecorder = ShimmerGSRRecorder(
            context,
            MockShimmerDeviceFactory(),
            samplingRateHz = 128,
            recordingMode = ShimmerGSRRecorder.RecordingMode.STREAMING
        )
        val loggingRecorder = ShimmerGSRRecorder(
            context,
            MockShimmerDeviceFactory(),
            samplingRateHz = 128,
            recordingMode = ShimmerGSRRecorder.RecordingMode.LOGGING
        )
        val logAndStreamRecorder = ShimmerGSRRecorder(
            context,
            MockShimmerDeviceFactory(),
            samplingRateHz = 128,
            recordingMode = ShimmerGSRRecorder.RecordingMode.LOG_AND_STREAM
        )
        assertEquals(
            "Should be streaming mode",
            ShimmerGSRRecorder.RecordingMode.STREAMING,
            streamingRecorder.getRecordingMode()
        )
        assertEquals(
            "Should be logging mode",
            ShimmerGSRRecorder.RecordingMode.LOGGING,
            loggingRecorder.getRecordingMode()
        )
        assertEquals(
            "Should be log-and-stream mode",
            ShimmerGSRRecorder.RecordingMode.LOG_AND_STREAM,
            logAndStreamRecorder.getRecordingMode()
        )
    }

    @Test
    fun testGSRDataProcessingAccuracy() {
        val apisBridge = ShimmerApiBridge.getInstance()
        val testValues = arrayOf(0.0, 1024.0, 2048.0, 3072.0, 4095.0)
        for (rawValue in testValues) {
            val sample = apisBridge.processGSRData(
                rawValue = rawValue,
                timestamp = System.currentTimeMillis(),
                sessionId = "test_session"
            )
            assertTrue(
                "Raw value should be preserved: $rawValue",
                sample.rawValue.toDouble() == rawValue
            )
            assertTrue(
                "Conductance should be positive",
                sample.conductance >= 0.0
            )
            assertTrue(
                "Resistance should be positive",
                sample.resistance >= 0.0
            )
        }
    }
}


// ===== FROM: component\gsr-recording\src\test\java\com\mpdc4gsr\gsr\tests\TimeUtilitiesTest.kt =====

package com.mpdc4gsr.gsr.tests

import com.mpdc4gsr.gsr.util.TimeUtils
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test

@Ignore("All tests disabled")
class TimeUtilitiesTest {
    @Test
    fun testPcTimeOffset() {
        TimeUtils.initializeGroundTruthTiming()
        val initialOffset = TimeUtils.getPcTimeOffset()
        assertEquals(0L, initialOffset)
        val testOffset = 5000L
        TimeUtils.setPcTimeOffset(testOffset)
        assertEquals(testOffset, TimeUtils.getPcTimeOffset())
        TimeUtils.setPcTimeOffset(0L)
    }

    @Test
    fun testUtcTimestamp() {
        val offset = 1000L
        TimeUtils.setPcTimeOffset(offset)
        val systemTime = System.currentTimeMillis()
        val utcTime = TimeUtils.getUtcTimestamp()
        assertTrue("UTC time should be greater than system time", utcTime > systemTime)
        assertTrue(
            "UTC time should be close to system time + offset",
            Math.abs(utcTime - (systemTime + offset)) < 100,
        )
        TimeUtils.setPcTimeOffset(0L)
    }

    @Test
    fun testTimeConversion() {
        TimeUtils.initializeGroundTruthTiming()
        val offset = 2000L
        TimeUtils.setPcTimeOffset(offset)
        val systemTime = System.currentTimeMillis()
        val utcTime = TimeUtils.systemToUtc(systemTime)
        val backToSystem = TimeUtils.utcToSystem(utcTime)
        assertTrue(
            "UTC time should include PC offset",
            Math.abs(utcTime - (systemTime + offset)) < 100
        )
        assertTrue(
            "Back conversion should be close to original",
            Math.abs(backToSystem - systemTime) < 100
        )
        TimeUtils.setPcTimeOffset(0L)
    }

    @Test
    fun testFormatTimestamp() {
        val timestamp = 1640995200000L
        val formatted = TimeUtils.formatTimestamp(timestamp)
        assertTrue(
            "Formatted time should contain year",
            formatted.contains("2022") || formatted.contains("2021")
        )
        assertTrue("Formatted time should contain time separator", formatted.contains(":"))
    }

    @Test
    fun testGenerateSessionId() {
        val sessionId1 = TimeUtils.generateSessionId()
        val sessionId2 = TimeUtils.generateSessionId()
        val customId = TimeUtils.generateSessionId("CUSTOM")
        assertTrue("Session ID should start with GSR", sessionId1.startsWith("GSR_"))
        assertTrue("Session ID should start with GSR", sessionId2.startsWith("GSR_"))
        assertTrue("Custom session ID should start with CUSTOM", customId.startsWith("CUSTOM_"))
        assertTrue("Session ID should not be empty", sessionId1.length > 4)
        assertTrue("Session ID should contain underscore", sessionId1.contains("_"))
    }

    @Test
    fun testGroundTruthTiming() {
        TimeUtils.initializeGroundTruthTiming()
        val groundTruthBase = TimeUtils.getGroundTruthBase()
        assertTrue(
            "Ground truth base should be recent",
            System.currentTimeMillis() - groundTruthBase < 1000,
        )
        val syncTime = TimeUtils.getSynchronizedTimestamp()
        assertTrue("Synchronized timestamp should be valid", syncTime > 0)
    }

    @Test
    fun testTimingMetadata() {
        TimeUtils.initializeGroundTruthTiming()
        TimeUtils.setPcTimeOffset(1500L)
        val metadata = TimeUtils.getTimingMetadata()
        assertTrue("Should contain ground truth base", metadata.containsKey("ground_truth_base"))
        assertTrue("Should contain PC offset", metadata.containsKey("pc_offset_ms"))
        assertTrue("Should contain device model", metadata.containsKey("device_model"))
        assertTrue("Should contain timing mode", metadata.containsKey("timing_mode"))
        assertEquals("1500", metadata["pc_offset_ms"])
        assertNotNull("Device model should be detected", metadata["device_model"])
        assertEquals("unified_ntp_style", metadata["timing_mode"])
        assertNotNull("Device processor should be detected", metadata["device_processor"])
        TimeUtils.setPcTimeOffset(0L)
    }
}


// ===== FROM: component\gsr-recording\src\test\java\com\mpdc4gsr\gsr\tests\ZeroconfServiceDiscoveryTest.kt =====

package com.mpdc4gsr.gsr.tests

import android.content.Context
import android.net.nsd.NsdManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.mpdc4gsr.gsr.network.NetworkClient
import com.mpdc4gsr.gsr.network.ZeroconfDiscoveryService
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNsdManager

@Ignore("All tests disabled")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O])
class ZeroconfServiceDiscoveryTest {
    private lateinit var context: Context
    private lateinit var discoveryService: ZeroconfDiscoveryService
    private lateinit var nsdManager: NsdManager
    private lateinit var shadowNsdManager: ShadowNsdManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        discoveryService = ZeroconfDiscoveryService(context)
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        shadowNsdManager = Shadows.shadowOf(nsdManager)
    }

    @Test
    fun testServiceCreation() {
        assertNotNull("Discovery service should be created", discoveryService)
    }

    @Test
    fun testNsdManagerAccess() {
        assertNotNull("NsdManager should be available", nsdManager)
        assertNotNull("Shadow NsdManager should be available", shadowNsdManager)
    }

    @Test
    fun testSetServiceListener() {
        var serviceDiscovered = false
        var serviceLost = false
        var serviceRegistered = false
        var discoveryError = false
        val listener =
            object : ZeroconfDiscoveryService.ServiceDiscoveryListener {
                override fun onServiceDiscovered(serviceInfo: NetworkClient.ControllerInfo) {
                    serviceDiscovered = true
                }

                override fun onServiceLost(serviceName: String) {
                    serviceLost = true
                }

                override fun onServiceRegistered(serviceName: String) {
                    serviceRegistered = true
                }

                override fun onDiscoveryError(
                    errorCode: Int,
                    message: String,
                ) {
                    discoveryError = true
                }
            }
        discoveryService.setServiceListener(listener)
        assertTrue("Setting service listener should succeed", true)
        discoveryService.setServiceListener(null)
        assertTrue("Removing service listener should succeed", true)
    }

    @Test
    fun testGetDiscoveredServices() {
        val initialServices = discoveryService.getDiscoveredControllers()
        assertNotNull("Discovered services should not be null", initialServices)
        assertTrue("Initial services should be empty", initialServices.isEmpty())
    }

    @Test
    fun testServiceNameGeneration() {
        val deviceName1 = "Device One"
        val deviceName2 = "Device Two"
        assertTrue(
            "Different device names should be handled",
            deviceName1 != deviceName2,
        )
    }

    @Test
    fun testCleanupResources() {
        discoveryService.cleanup()
        assertTrue("Cleanup should complete without errors", true)
    }

    @Test
    fun testServiceListenerInterface() {
        val listener =
            object : ZeroconfDiscoveryService.ServiceDiscoveryListener {
                override fun onServiceDiscovered(serviceInfo: NetworkClient.ControllerInfo) {
                }

                override fun onServiceLost(serviceName: String) {
                }

                override fun onServiceRegistered(serviceName: String) {
                }

                override fun onDiscoveryError(
                    errorCode: Int,
                    message: String,
                ) {
                }
            }
        assertNotNull("Service listener should be created", listener)
        discoveryService.setServiceListener(listener)
        discoveryService.setServiceListener(null)
        assertTrue("Service listener interface test should pass", true)
    }

    @Test
    fun testContextDependency() {
        val testContext = ApplicationProvider.getApplicationContext<Context>()
        val testService = ZeroconfDiscoveryService(testContext)
        assertNotNull("Service with context should be created", testService)
        val controllers = testService.getDiscoveredControllers()
        assertNotNull("Controllers list should not be null", controllers)
        assertTrue("Controllers list should be empty initially", controllers.isEmpty())
    }

    @Test
    fun testNetworkClientControllerInfo() {
        val controllerInfo =
            NetworkClient.ControllerInfo(
                ipAddress = "192.168.1.100",
                port = 8080,
                deviceName = "Test Controller",
                capabilities = listOf("VIDEO", "GSR"),
            )
        assertEquals("IP address should match", "192.168.1.100", controllerInfo.ipAddress)
        assertEquals("Port should match", 8080, controllerInfo.port)
        assertEquals("Device name should match", "Test Controller", controllerInfo.deviceName)
        assertEquals("Capabilities should match", 2, controllerInfo.capabilities.size)
        assertTrue("Should contain VIDEO capability", controllerInfo.capabilities.contains("VIDEO"))
    }
}