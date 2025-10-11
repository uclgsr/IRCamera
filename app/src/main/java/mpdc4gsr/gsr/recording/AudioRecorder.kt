package mpdc4gsr.gsr.recording

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mpdc4gsr.gsr.model.RecorderKind
import mpdc4gsr.gsr.model.RecorderState
import mpdc4gsr.gsr.session.TimelineClock

class AudioRecorder(
    private val context: android.content.Context,
    private val dispatcher: CoroutineDispatcher,
    private val clock: TimelineClock,
) : Recorder {

    override val kind: RecorderKind = RecorderKind.AUDIO
    private val _state = MutableStateFlow(RecorderState.IDLE)
    override val state: StateFlow<RecorderState> = _state

    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private lateinit var audioDirectory: File
    private var audioRecord: AudioRecord? = null
    private var recordingFile: RandomAccessFile? = null
    private var recordingContext: RecordingContext? = null
    private var bytesWritten: Int = 0

    override suspend fun prepare(context: RecordingContext) {
        recordingContext = context
        _state.value = RecorderState.PREPARING
        audioDirectory = File(context.sessionDirectory, "audio").apply { mkdirs() }
    }

    override suspend fun start(context: RecordingContext) {
        recordingContext = context
        if (
            ContextCompat.checkSelfPermission(
                this.context,
                Manifest.permission.RECORD_AUDIO,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _state.value = RecorderState.FAILED
            return
        }
        _state.value = RecorderState.RECORDING
        val minBuffer =
            AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
            )
        val bufferSize = maxOf(minBuffer, SAMPLE_RATE / 2)
        audioRecord =
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.UNPROCESSED)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build(),
                )
                .setBufferSizeInBytes(bufferSize)
                .build()
        val outputFile = createOutputFile(context.sessionId)
        recordingFile = RandomAccessFile(outputFile, "rw").apply {
            setLength(0)
            writeWavHeader(0)
        }
        val buffer = ByteArray(bufferSize)
        audioRecord?.startRecording()
        scope.launch {
            while (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    recordingFile?.write(buffer, 0, read)
                    bytesWritten += read
                }
            }
        }
    }

    override suspend fun stop() {
        _state.value = RecorderState.STOPPING
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        withContext(dispatcher) {
            recordingFile?.apply {
                seek(0)
                writeWavHeader(bytesWritten)
                close()
            }
            recordingFile = null
            bytesWritten = 0
        }
        _state.value = RecorderState.IDLE
    }

    override fun close() {
        scope.cancel()
        audioRecord?.release()
    }

    private fun createOutputFile(sessionId: String): File {
        val timestamp = clock.nowInstant().toString().replace(":", "-")
        return File(audioDirectory, "${sessionId}_audio_$timestamp.wav")
    }

    private fun RandomAccessFile.writeWavHeader(dataSize: Int) {
        val totalDataLen = dataSize + 36
        val byteRate = SAMPLE_RATE * NUM_CHANNELS * BITS_PER_SAMPLE / 8
        val buffer = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray())
        buffer.putInt(totalDataLen)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(NUM_CHANNELS.toShort())
        buffer.putInt(SAMPLE_RATE)
        buffer.putInt(byteRate)
        buffer.putShort((NUM_CHANNELS * BITS_PER_SAMPLE / 8).toShort())
        buffer.putShort(BITS_PER_SAMPLE.toShort())
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)
        seek(0)
        write(buffer.array())
    }

    companion object {
        private const val SAMPLE_RATE = 44_100
        private const val NUM_CHANNELS = 1
        private const val BITS_PER_SAMPLE = 16
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }
}
