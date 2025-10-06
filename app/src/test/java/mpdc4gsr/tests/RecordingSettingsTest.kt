package mpdc4gsr.tests
import android.content.Context
import android.content.SharedPreferences
import mpdc4gsr.feature.settings.data.RecordingSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
@Ignore("All tests disabled")
@RunWith(MockitoJUnitRunner::class)
class RecordingSettingsTest {
    @Mock
    private lateinit var mockContext: Context
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var repository: RecordingSettingsRepository
    @Before
    fun setup() {
        `when`(mockContext.applicationContext).thenReturn(mockContext)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        `when`(mockSharedPreferences.getBoolean(eq("recording_auto_recording"), anyBoolean())).thenReturn(false)
        `when`(mockSharedPreferences.getString(eq("recording_quality"), anyString())).thenReturn("High")
        `when`(mockSharedPreferences.getInt(eq("recording_video_frame_rate"), anyInt())).thenReturn(30)
        `when`(mockSharedPreferences.getBoolean(eq("recording_audio_enabled"), anyBoolean())).thenReturn(true)
        `when`(mockSharedPreferences.getBoolean(eq("recording_simultaneous"), anyBoolean())).thenReturn(true)
        `when`(mockSharedPreferences.getBoolean(eq("recording_timestamp_sync"), anyBoolean())).thenReturn(true)
    }
    @Test
    fun `test default settings`() {
        val settings = RecordingSettingsRepository.RecordingSettings()
        assertEquals(false, settings.autoRecording)
        assertEquals("High", settings.recordingQuality)
        assertEquals(30, settings.videoFrameRate)
        assertEquals(true, settings.audioEnabled)
        assertEquals(true, settings.simultaneousRecording)
        assertEquals(true, settings.timestampSync)
    }
    @Test
    fun `test quality config for Ultra quality`() {
        repository = RecordingSettingsRepository(mockContext)
        val qualityConfig = repository.getQualityConfig("Ultra")
        assertEquals(50_000_000, qualityConfig.videoBitrate)
        assertEquals(3840, qualityConfig.videoWidth)
        assertEquals(2160, qualityConfig.videoHeight)
        assertEquals(60, qualityConfig.preferredFps)
    }
    @Test
    fun `test quality config for High quality`() {
        repository = RecordingSettingsRepository(mockContext)
        val qualityConfig = repository.getQualityConfig("High")
        assertEquals(20_000_000, qualityConfig.videoBitrate)
        assertEquals(1920, qualityConfig.videoWidth)
        assertEquals(1080, qualityConfig.videoHeight)
        assertEquals(30, qualityConfig.preferredFps)
    }
    @Test
    fun `test quality config for Medium quality`() {
        repository = RecordingSettingsRepository(mockContext)
        val qualityConfig = repository.getQualityConfig("Medium")
        assertEquals(10_000_000, qualityConfig.videoBitrate)
        assertEquals(1280, qualityConfig.videoWidth)
        assertEquals(720, qualityConfig.videoHeight)
        assertEquals(30, qualityConfig.preferredFps)
    }
    @Test
    fun `test quality config for Low quality`() {
        repository = RecordingSettingsRepository(mockContext)
        val qualityConfig = repository.getQualityConfig("Low")
        assertEquals(5_000_000, qualityConfig.videoBitrate)
        assertEquals(854, qualityConfig.videoWidth)
        assertEquals(480, qualityConfig.videoHeight)
        assertEquals(24, qualityConfig.preferredFps)
    }
}
