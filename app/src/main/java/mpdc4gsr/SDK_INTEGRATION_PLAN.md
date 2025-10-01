# SDK Integration Plan - Shimmer & Topdon

## Overview

This document outlines the integration of Shimmer3 GSR+ SDK and Topdon TC001/TC007 thermal camera SDK into the Clean Architecture with proper abstraction layers.

## Current SDK Status

### Shimmer SDK (GSR Sensor)
**Location**: `app/libs/`
- `shimmerandroidinstrumentdriver-3.2.4_beta.aar`
- `shimmerdriver-0.11.5_beta.jar`
- `shimmerdriverpc-0.11.5_beta.jar`
- `shimmerbluetoothmanager-0.11.5_beta.jar`

**Current Integration**:
- ✅ Direct SDK usage in `ShimmerDeviceManager.kt`
- ✅ `ShimmerConfigViewModel` for UI state management
- ✅ BLE connectivity through `ShimmerBluetoothManagerAndroid`
- ⚠️ Missing: Repository abstraction layer
- ⚠️ Missing: Data source abstraction
- ⚠️ Missing: Domain models separate from SDK models

### Topdon SDK (Thermal Camera)
**Location**: `app/libs/topdon.aar`, `libunified/libs/topdon.aar`

**Current Integration**:
- ✅ Module: `component/thermalunified`
- ✅ `ThermalCameraViewModel` created
- ⚠️ Missing: Repository abstraction layer
- ⚠️ Missing: Data source abstraction
- ⚠️ Missing: USB connection management abstraction

## Architecture Pattern for SDK Integration

```
┌─────────────────────────────────────────────────────────────┐
│                      UI Layer (Compose)                      │
│  ┌────────────────────────────────────────────────────┐     │
│  │  ShimmerConfigActivity  │  ThermalCameraActivity   │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                  Presentation Layer (ViewModels)             │
│  ┌────────────────────────────────────────────────────┐     │
│  │ ShimmerConfigViewModel │ ThermalCameraViewModel    │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                     Domain Layer (Use Cases)                 │
│  ┌────────────────────────────────────────────────────┐     │
│  │ ConnectShimmerUseCase  │  StartThermalRecordingUseCase│  │
│  │ GetGSRDataUseCase      │  CaptureThermalImageUseCase  │  │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                  Data Layer (Repositories)                   │
│  ┌────────────────────────────────────────────────────┐     │
│  │ ShimmerRepository      │  ThermalCameraRepository  │     │
│  │ (Interface in domain)  │  (Interface in domain)    │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                   Data Sources (SDK Wrappers)                │
│  ┌────────────────────────────────────────────────────┐     │
│  │ ShimmerDataSource      │  TopdonDataSource         │     │
│  │ (Wraps Shimmer SDK)    │  (Wraps Topdon SDK)       │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                        Native SDKs                            │
│  ┌────────────────────────────────────────────────────┐     │
│  │ Shimmer SDK (AAR/JAR)  │  Topdon SDK (AAR)         │     │
│  └────────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

## Implementation Plan

### Phase 1: Create Data Source Abstractions ✅

#### 1.1 Shimmer Data Source
```kotlin
// feature/gsr/data/source/ShimmerDataSource.kt
interface ShimmerDataSource {
    suspend fun scanForDevices(): Flow<List<ShimmerDevice>>
    suspend fun connect(deviceId: String): Result<Unit>
    suspend fun disconnect(deviceId: String)
    suspend fun startStreaming(deviceId: String): Flow<GSRSample>
    suspend fun stopStreaming(deviceId: String)
    suspend fun configureDevice(deviceId: String, config: ShimmerConfig): Result<Unit>
}

// feature/gsr/data/source/ShimmerDataSourceImpl.kt
class ShimmerDataSourceImpl(
    private val shimmerManager: ShimmerDeviceManager
) : ShimmerDataSource {
    // Wraps ShimmerBluetoothManagerAndroid
}
```

#### 1.2 Topdon Data Source
```kotlin
// feature/thermal/data/source/TopdonDataSource.kt
interface TopdonDataSource {
    suspend fun connectDevice(): Result<Unit>
    suspend fun disconnectDevice()
    suspend fun startStreaming(): Flow<ThermalFrame>
    suspend fun stopStreaming()
    suspend fun captureImage(): Result<ThermalImage>
    suspend fun startRecording(): Result<Unit>
    suspend fun stopRecording(): Result<File>
}

// feature/thermal/data/source/TopdonDataSourceImpl.kt
class TopdonDataSourceImpl(
    private val context: Context
) : TopdonDataSource {
    // Wraps Topdon SDK
}
```

### Phase 2: Create Repository Layer ✅

#### 2.1 Shimmer Repository
```kotlin
// feature/gsr/domain/repository/ShimmerRepository.kt (interface)
interface ShimmerRepository {
    suspend fun scanDevices(): Flow<List<ShimmerDevice>>
    suspend fun connectDevice(deviceId: String): Result<Unit>
    suspend fun getGSRData(deviceId: String): Flow<GSRData>
    suspend fun saveSession(session: GSRSession): Result<Unit>
}

// feature/gsr/data/repository/ShimmerRepositoryImpl.kt
class ShimmerRepositoryImpl(
    private val shimmerDataSource: ShimmerDataSource,
    private val localDataSource: LocalGSRDataSource
) : ShimmerRepository {
    // Coordinates between remote (Shimmer) and local data sources
}
```

#### 2.2 Thermal Repository
```kotlin
// feature/thermal/domain/repository/ThermalRepository.kt (interface)
interface ThermalRepository {
    suspend fun connectCamera(): Result<Unit>
    suspend fun getThermalStream(): Flow<ThermalFrame>
    suspend fun captureSnapshot(): Result<ThermalImage>
    suspend fun saveRecording(recording: ThermalRecording): Result<Unit>
}

// feature/thermal/data/repository/ThermalRepositoryImpl.kt
class ThermalRepositoryImpl(
    private val topdonDataSource: TopdonDataSource,
    private val localDataSource: LocalThermalDataSource
) : ThermalRepository {
    // Coordinates between remote (Topdon) and local data sources
}
```

### Phase 3: Create Domain Models ✅

Transform SDK-specific models to domain models:

```kotlin
// feature/gsr/domain/model/ShimmerDevice.kt
data class ShimmerDevice(
    val id: String,
    val name: String,
    val macAddress: String,
    val rssi: Int,
    val isConnected: Boolean,
    val batteryLevel: Int?
)

// feature/gsr/domain/model/GSRData.kt
data class GSRData(
    val timestamp: Long,
    val resistance: Double,
    val conductance: Double,
    val quality: SignalQuality
)

// feature/thermal/domain/model/ThermalFrame.kt
data class ThermalFrame(
    val timestamp: Long,
    val width: Int,
    val height: Int,
    val temperatureData: FloatArray,
    val minTemp: Float,
    val maxTemp: Float
)
```

### Phase 4: Create Use Cases ✅

Extract business logic from ViewModels:

```kotlin
// feature/gsr/domain/usecase/ConnectShimmerDeviceUseCase.kt
class ConnectShimmerDeviceUseCase(
    private val repository: ShimmerRepository
) {
    suspend operator fun invoke(deviceId: String): Result<Unit> {
        return repository.connectDevice(deviceId)
    }
}

// feature/gsr/domain/usecase/StartGSRRecordingUseCase.kt
class StartGSRRecordingUseCase(
    private val repository: ShimmerRepository
) {
    suspend operator fun invoke(deviceId: String): Flow<GSRData> {
        return repository.getGSRData(deviceId)
    }
}

// feature/thermal/domain/usecase/ConnectThermalCameraUseCase.kt
class ConnectThermalCameraUseCase(
    private val repository: ThermalRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.connectCamera()
    }
}
```

### Phase 5: Update ViewModels to Use Use Cases ✅

```kotlin
// feature/gsr/presentation/ShimmerConfigViewModel.kt (updated)
class ShimmerConfigViewModel(
    private val connectDeviceUseCase: ConnectShimmerDeviceUseCase,
    private val scanDevicesUseCase: ScanShimmerDevicesUseCase
) : BaseViewModel() {
    
    fun connectToDevice(deviceId: String) {
        viewModelScope.launch {
            connectDeviceUseCase(deviceId)
                .onSuccess { /* Update UI state */ }
                .onFailure { /* Handle error */ }
        }
    }
}
```

## Benefits of This Architecture

### 1. SDK Independence
- Easy to switch or upgrade SDKs
- SDKs isolated in data sources
- Domain layer is SDK-agnostic

### 2. Testability
- Mock data sources for testing
- Test use cases without SDK
- Test ViewModels with fake repositories

### 3. Maintainability
- Clear separation of concerns
- Business logic in use cases
- UI logic in ViewModels

### 4. Flexibility
- Add caching easily
- Support multiple data sources
- Implement offline mode

## Current Integration Points

### Shimmer SDK Entry Points
1. **BLE Scanning**: `ShimmerBluetoothManagerAndroid.startBluetoothScan()`
2. **Device Connection**: `ShimmerBluetoothManagerAndroid.connectShimmerThroughBT()`
3. **Data Streaming**: `Shimmer.enableSensors()` + callback listeners
4. **Configuration**: `Shimmer.writeEnabledSensors()`, `Shimmer.writeSamplingRate()`

### Topdon SDK Entry Points
1. **USB Connection**: Topdon SDK initialization
2. **Image Streaming**: Frame callback listeners
3. **Image Capture**: Snapshot APIs
4. **Temperature Data**: Temperature matrix access

## Migration Path

1. ✅ Create data source interfaces and implementations
2. ✅ Create repository interfaces in domain layer
3. ✅ Create repository implementations in data layer
4. ✅ Create domain models
5. ✅ Create use cases
6. ✅ Update existing ViewModels to use use cases
7. ✅ Add dependency injection for all layers
8. ✅ Write tests for each layer

## Testing Strategy

### Unit Tests
- Use case logic (pure business logic)
- Domain model transformations
- ViewModel behavior with mock repositories

### Integration Tests
- Repository with mock data sources
- Data source with mock SDK

### End-to-End Tests
- Full flow with real SDK (on device)
- Performance testing
- Connection stability testing

## Performance Considerations

### Shimmer SDK
- BLE connection can be slow (2-5 seconds)
- Implement connection pooling for multiple devices
- Cache device information
- Optimize data streaming (buffer management)

### Topdon SDK
- USB connection is faster than BLE
- Thermal frames can be large (optimize bitmap handling)
- Implement frame skipping for UI smoothness
- Use coroutines for non-blocking operations

## Error Handling

### Common Errors
1. **Bluetooth not enabled**: Prompt user
2. **Permissions denied**: Request permissions
3. **Device not found**: Retry scan
4. **Connection timeout**: Implement retry logic
5. **Data corruption**: Validate and filter

### Retry Strategy
- Exponential backoff for connections
- Max retry attempts configuration
- User notification on persistent failures

## Next Steps

1. Implement Phase 1: Data Source abstractions
2. Implement Phase 2: Repository layer
3. Implement Phase 3: Domain models
4. Implement Phase 4: Use cases
5. Implement Phase 5: Update ViewModels
6. Add comprehensive tests
7. Document SDK-specific configurations
8. Create SDK integration examples

## References

- Shimmer SDK Documentation: `ShimmerBluetoothManagerAndroid` JavaDoc
- Topdon SDK: Proprietary documentation
- Ground truth implementations:
  - Shimmer: https://github.com/CoderCaiSL/IRCamera (BleModule)
  - Shimmer Java API: https://github.com/ShimmerEngineering/Shimmer-Java-Android-API
