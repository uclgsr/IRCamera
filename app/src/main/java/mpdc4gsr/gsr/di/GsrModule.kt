package mpdc4gsr.gsr.di

import android.content.Context
import android.os.Build
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import mpdc4gsr.feature.capture.thermal.data.ThermalCaptureCoordinator
import com.csl.irCamera.BuildConfig
import mpdc4gsr.gsr.GsrOrchestrator
import mpdc4gsr.gsr.capture.CaptureCoordinator
import mpdc4gsr.gsr.capture.RecorderFactory
import mpdc4gsr.gsr.capture.SimulationSource
import mpdc4gsr.gsr.device.ShimmerDeviceController
import mpdc4gsr.gsr.model.SessionStateStore
import mpdc4gsr.gsr.network.CommandClient
import mpdc4gsr.gsr.network.PreviewPublisher
import mpdc4gsr.gsr.network.TimeSyncClient
import mpdc4gsr.gsr.network.TransferClient
import mpdc4gsr.gsr.session.SessionController
import mpdc4gsr.gsr.session.TimelineClock
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

private const val FALLBACK_COMMAND_ENDPOINT = "https://127.0.0.1:8443"

private fun commandEndpoint(): String =
    BuildConfig.COMMAND_ENDPOINT.takeUnless { it.isBlank() } ?: FALLBACK_COMMAND_ENDPOINT

@Module
@InstallIn(SingletonComponent::class)
object GsrModule {

    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                },
            )
            .build()

    @Provides
    @Singleton
    fun provideSessionStateStore(): SessionStateStore = SessionStateStore()

    @Provides
    @Singleton
    fun provideTimelineClock(): TimelineClock = TimelineClock(Dispatchers.Default)

    @Provides
    @Singleton
    fun provideSimulationSource(): SimulationSource = SimulationSource(Dispatchers.Default)

    @Provides
    @Singleton
    fun provideShimmerDeviceController(
        @ApplicationContext context: Context,
        clock: TimelineClock,
        simulationSource: SimulationSource,
    ): ShimmerDeviceController =
        ShimmerDeviceController(
            context = context,
            clock = clock,
            dispatcher = Dispatchers.IO,
            simulationSource = simulationSource,
        )

    @Provides
    @Singleton
    fun provideRecorderFactory(
        @ApplicationContext context: Context,
        shimmerDeviceController: ShimmerDeviceController,
        simulationSource: SimulationSource,
        timelineClock: TimelineClock,
        thermalCoordinator: ThermalCaptureCoordinator,
    ): RecorderFactory =
        RecorderFactory(
            appContext = context,
            ioDispatcher = Dispatchers.IO,
            shimmerController = shimmerDeviceController,
            simulationSource = simulationSource,
            timelineClock = timelineClock,
            thermalCoordinator = thermalCoordinator,
        )

    @Provides
    @Singleton
    fun provideSessionController(
        @ApplicationContext context: Context,
        stateStore: SessionStateStore,
        clock: TimelineClock,
    ): SessionController =
        SessionController(
            context = context,
            stateStore = stateStore,
            clock = clock,
            dispatcher = Dispatchers.IO,
        )

    @Provides
    @Singleton
    fun provideCaptureCoordinator(
        recorderFactory: RecorderFactory,
        sessionController: SessionController,
    ): CaptureCoordinator =
        CaptureCoordinator(
            recorderFactory = recorderFactory,
            sessionController = sessionController,
            dispatcher = Dispatchers.IO,
        )

    @Provides
    @Singleton
    fun provideCommandClient(
        okHttpClient: OkHttpClient,
        json: Json,
        stateStore: SessionStateStore,
    ): CommandClient =
        CommandClient(
            okHttpClient = okHttpClient,
            json = json,
            endpoint = commandEndpoint(),
            deviceId = Build.DEVICE,
            dispatcher = Dispatchers.IO,
            sessionStateStore = stateStore,
        )

    @Provides
    @Singleton
    fun provideTimeSyncClient(
        okHttpClient: OkHttpClient,
        json: Json,
        sessionController: SessionController,
        clock: TimelineClock,
    ): TimeSyncClient =
        TimeSyncClient(
            okHttpClient = okHttpClient,
            json = json,
            endpoint = commandEndpoint(),
            dispatcher = Dispatchers.IO,
            sessionController = sessionController,
            clock = clock,
        )

    @Provides
    @Singleton
    fun provideTransferClient(
        commandClient: CommandClient,
    ): TransferClient =
        TransferClient(
            commandClient = commandClient,
            dispatcher = Dispatchers.IO,
        )

    @Provides
    @Singleton
    fun providePreviewPublisher(
        commandClient: CommandClient,
        stateStore: SessionStateStore,
    ): PreviewPublisher =
        PreviewPublisher(
            commandClient = commandClient,
            telemetryState = stateStore.deviceTelemetry,
            dispatcher = Dispatchers.IO,
        )

    @Provides
    @Singleton
    fun provideSessionDirectoryProvider(
        @ApplicationContext context: Context,
    ): () -> File = {
        File(context.filesDir, "sessions").apply { mkdirs() }
    }

    @Provides
    @Singleton
    fun provideGsrOrchestrator(
        @ApplicationContext context: Context,
        shimmerDeviceController: ShimmerDeviceController,
        commandClient: CommandClient,
        timeSyncClient: TimeSyncClient,
        transferClient: TransferClient,
        recorderFactory: RecorderFactory,
        sessionController: SessionController,
        captureCoordinator: CaptureCoordinator,
        timelineClock: TimelineClock,
        sessionStateStore: SessionStateStore,
        previewPublisher: PreviewPublisher,
        thermalCoordinator: ThermalCaptureCoordinator,
        simulationSource: SimulationSource,
        sessionDirectoryProvider: () -> File,
    ): GsrOrchestrator =
        GsrOrchestrator(
            context = context,
            lifecycleOwnerProvider = { androidx.lifecycle.ProcessLifecycleOwner.get() },
            ioDispatcher = Dispatchers.IO,
            sessionDirectoryProvider = sessionDirectoryProvider,
            shimmerController = shimmerDeviceController,
            commandClient = commandClient,
            timeSyncClient = timeSyncClient,
            transferClient = transferClient,
            recorderFactory = recorderFactory,
            sessionController = sessionController,
            captureCoordinator = captureCoordinator,
            timelineClock = timelineClock,
            sessionStateStore = sessionStateStore,
            previewPublisher = previewPublisher,
            thermalCoordinator = thermalCoordinator,
            simulationSource = simulationSource,
        )
}
