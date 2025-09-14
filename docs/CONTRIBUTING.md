# Contributing to MPDC4GSR

We welcome contributions to the Multi-Modal Physiological Sensing Platform! This guide will help you
get started.

## 🎯 How to Contribute

### Types of Contributions

We welcome several types of contributions:

- **🐛 Bug Reports**: Help us identify and fix issues
- **✨ Feature Requests**: Suggest new capabilities
- **📝 Documentation**: Improve guides and API docs
- **🧪 Testing**: Add tests and improve coverage
- **🔧 Code**: Fix bugs and implement features
- **🎨 UI/UX**: Improve user interfaces
- **📊 Research**: Validate platform capabilities

## 🚀 Getting Started

### Development Environment

#### Prerequisites

```bash
# Required tools
- Git
- Android Studio Hedgehog+
- Python 3.11+
- Node.js 18+ (for documentation)

# Android development
- Android SDK 34+
- NDK r25+
- Java 17

# PC Controller development
- PyQt6
- CMake 3.20+
- C++17 compiler
```

#### Repository Setup

```bash
# Fork and clone
git clone https://github.com/YOUR_USERNAME/IRCamera.git
cd IRCamera

# Set up remote for upstream
git remote add upstream https://github.com/buccancs/IRCamera.git

# Install pre-commit hooks
pip install pre-commit
pre-commit install

# Verify environment
./scripts/verify_environment.sh
```

### First-Time Setup

#### Android Development

```bash
# Build all modules
./gradlew clean build

# Run tests
./gradlew test

# Check code style
./gradlew ktlintCheck
```

#### PC Controller Development

```bash
cd pc-controller

# Create virtual environment
python -m venv venv
source venv/bin/activate  # Linux/macOS
venv\Scripts\activate     # Windows

# Install dependencies
pip install -r requirements-dev.txt

# Run tests
pytest

# Check code quality
black --check src/
flake8 src/
mypy src/
```

## 📋 Development Workflow

### Issue-Based Development

1. **Check existing issues** before creating new ones
2. **Comment on issues** you'd like to work on
3. **Wait for assignment** to avoid duplicate work
4. **Create feature branch** from main
5. **Follow coding standards** (see below)
6. **Submit pull request** with tests and documentation

### Branch Naming Convention

```bash
# Feature branches
git checkout -b feature/shimmer-battery-monitoring
git checkout -b feature/thermal-camera-integration

# Bug fix branches
git checkout -b fix/gsr-connection-timeout
git checkout -b fix/video-sync-drift

# Documentation branches
git checkout -b docs/api-reference-update
git checkout -b docs/troubleshooting-guide

# Research branches
git checkout -b research/timing-accuracy-validation
git checkout -b research/power-consumption-analysis
```

### Commit Message Format

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```bash
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

#### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

#### Examples

```bash
feat(gsr): add battery level monitoring for Shimmer3 devices

- Implement battery level polling every 30 seconds
- Add low battery warnings in UI
- Store battery data in session metadata

Closes #123

fix(camera): resolve RAW image capture race condition

The CaptureRequest and Image were not properly paired during
high-frequency capture, causing frame drops.

- Implement proper request-result mapping
- Add timeout handling for orphaned requests
- Increase buffer pool size for burst capture

Fixes #456

docs(api): update GSR recording interface documentation

- Add examples for async GSR recording
- Document error handling patterns
- Include performance considerations

test(network): add integration tests for device discovery

- Test mDNS discovery with multiple devices
- Validate TLS handshake error cases
- Add network timeout scenarios
```

## 🏗️ Code Standards

### Kotlin (Android)

#### Style Guide

```kotlin
// Follow official Kotlin style guide
class GSRRecorder @Inject constructor(
    private val shimmerManager: ShimmerManager,
    private val fileManager: FileManager,
    private val timeManager: TimeManager
) : SensorRecorder {

    companion object {
        private const val TAG = "GSRRecorder"
        private const val DEFAULT_SAMPLE_RATE = 128
    }

    private val _recordingState = MutableLiveData<RecordingState>()
    val recordingState: LiveData<RecordingState> = _recordingState

    
    override suspend fun startRecording(
        session: Session,
        syncTimeOffset: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting GSR recording for session ${session.id}")

            val shimmerDevice = shimmerManager.getConnectedDevice()
                ?: return@withContext Result.failure(
                    GSRException("No Shimmer3 device connected")
                )

            // Configure recording parameters
            shimmerDevice.configure {
                sampleRate = session.config.gsrSampleRate
                filters = session.config.gsrFilters
                calibration = session.config.gsrCalibration
            }

            // Start data collection
            shimmerDevice.startStreaming { sample ->
                processSample(sample, syncTimeOffset)
            }

            _recordingState.postValue(RecordingState.RECORDING)
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start GSR recording", e)
            Result.failure(e)
        }
    }

    private fun processSample(sample: RawGSRSample, syncOffset: Long) {
        val timestamp = System.nanoTime() + syncOffset
        val gsrSample = GSRSample(
            timestamp = timestamp,
            conductanceMicrosiemens = sample.conductance,
            resistanceKilohms = sample.resistance,
            sampleIndex = sample.index,
            qualityScore = assessSignalQuality(sample)
        )

        // Write to file asynchronously
        fileManager.writeSample(gsrSample)

        // Emit for real-time processing
        sampleFlow.tryEmit(gsrSample)
    }
}
```

#### Testing Standards

```kotlin
@RunWith(AndroidJUnit4::class)
class GSRRecorderTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    lateinit var mockShimmerManager: ShimmerManager

    @Mock
    lateinit var mockFileManager: FileManager

    private lateinit var gsrRecorder: GSRRecorder

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        gsrRecorder = GSRRecorder(mockShimmerManager, mockFileManager, TimeManager())
    }

    @Test
    fun `startRecording should succeed with valid configuration`() = runTest {
        // Given
        val session = createTestSession()
        val mockDevice = mock<ShimmerDevice>()
        whenever(mockShimmerManager.getConnectedDevice()).thenReturn(mockDevice)

        // When
        val result = gsrRecorder.startRecording(session, 0L)

        // Then
        assertTrue(result.isSuccess)
        verify(mockDevice).startStreaming(any())
        assertEquals(RecordingState.RECORDING, gsrRecorder.recordingState.value)
    }

    @Test
    fun `startRecording should fail when no device connected`() = runTest {
        // Given
        val session = createTestSession()
        whenever(mockShimmerManager.getConnectedDevice()).thenReturn(null)

        // When
        val result = gsrRecorder.startRecording(session, 0L)

        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is GSRException)
    }

    private fun createTestSession() = Session(
        id = "test_session",
        participantId = "P001",
        config = SessionConfig(
            gsrSampleRate = 128,
            gsrFilters = listOf("lowpass_5hz")
        )
    )
}
```

### Python (PC Controller)

#### Style Guide

```python
"""GSR data processing module for real-time analysis."""

import asyncio
import logging
from dataclasses import dataclass
from typing import Dict, List, Optional, Callable
from pathlib import Path

import numpy as np
import pandas as pd
from PyQt6.QtCore import QThread, pyqtSignal

logger = logging.getLogger(__name__)


@dataclass
class GSRSample:
    """Represents a single GSR measurement sample."""

    timestamp: int  # Nanoseconds since epoch
    conductance_us: float  # Microsiemens
    resistance_kohms: float  # Kilohms
    sample_index: int
    quality_score: float


class GSRProcessor(QThread):
    """Real-time GSR data processor with signal analysis capabilities."""

    sample_processed = pyqtSignal(GSRSample)
    scr_detected = pyqtSignal(dict)  # Skin conductance response

    def __init__(self, sample_rate: int = 128) -> None:
        """Initialize GSR processor.

        Args:
            sample_rate: Expected sample rate in Hz
        """
        super().__init__()
        self.sample_rate = sample_rate
        self.samples_buffer: List[GSRSample] = []
        self.processing_enabled = False

        # Signal processing parameters
        self.scr_threshold = 0.1  # Microsiemens
        self.scr_min_duration = 1.0  # Seconds
        self.baseline_window = 30.0  # Seconds

    async def add_sample(self, sample: GSRSample) -> None:
        """Add new sample for processing.

        Args:
            sample: GSR sample to process
        """
        self.samples_buffer.append(sample)

        # Maintain rolling window
        max_samples = int(self.sample_rate * self.baseline_window)
        if len(self.samples_buffer) > max_samples:
            self.samples_buffer.pop(0)

        # Process sample
        await self._process_sample(sample)

        # Emit signal for UI updates
        self.sample_processed.emit(sample)

    async def _process_sample(self, sample: GSRSample) -> None:
        """Process individual sample for SCR detection.

        Args:
            sample: Sample to analyze
        """
        if len(self.samples_buffer) < self.sample_rate:
            return  # Need at least 1 second of data

        try:
            # Extract recent data for analysis
            recent_samples = self.samples_buffer[-self.sample_rate:]
            conductance_values = [s.conductance_us for s in recent_samples]

            # Calculate baseline
            baseline = np.mean(conductance_values[:-10])  # Exclude last 10 samples
            current_value = sample.conductance_us

            # Detect SCR (skin conductance response)
            if current_value - baseline > self.scr_threshold:
                scr_event = {
                    'timestamp': sample.timestamp,
                    'amplitude': current_value - baseline,
                    'baseline': baseline,
                    'peak_value': current_value
                }

                logger.info(f"SCR detected: amplitude={scr_event['amplitude']:.3f}µS")
                self.scr_detected.emit(scr_event)

        except Exception as e:
            logger.error(f"Error processing GSR sample: {e}")

    def get_signal_quality(self) -> float:
        """Calculate signal quality score.

        Returns:
            Quality score between 0.0 and 1.0
        """
        if len(self.samples_buffer) < self.sample_rate:
            return 0.0

        # Calculate metrics
        recent_samples = self.samples_buffer[-self.sample_rate:]
        conductance_values = [s.conductance_us for s in recent_samples]

        # Check for signal stability
        std_dev = np.std(conductance_values)
        mean_value = np.mean(conductance_values)

        # Quality based on coefficient of variation
        if mean_value > 0:
            cv = std_dev / mean_value
            quality = max(0.0, 1.0 - cv)
        else:
            quality = 0.0

        return min(1.0, quality)


class SessionDataExporter:
    """Export session data in various formats."""

    def __init__(self, session_path: Path) -> None:
        """Initialize exporter with session path.

        Args:
            session_path: Path to session data directory
        """
        self.session_path = session_path

    async def export_to_hdf5(self, output_path: Path) -> None:
        """Export session data to HDF5 format.

        Args:
            output_path: Output file path

        Raises:
            ExportError: If export fails
        """
        try:
            import h5py

            with h5py.File(output_path, 'w') as f:
                # Load and write GSR data
                gsr_data = await self._load_gsr_data()
                if gsr_data is not None:
                    gsr_group = f.create_group('gsr')
                    gsr_group.create_dataset('timestamps', data=gsr_data['timestamps'])
                    gsr_group.create_dataset('conductance', data=gsr_data['conductance'])
                    gsr_group.create_dataset('resistance', data=gsr_data['resistance'])
                    gsr_group.attrs['sample_rate'] = 128.0
                    gsr_group.attrs['units'] = 'microsiemens'

                # Load and write sync events
                sync_data = await self._load_sync_events()
                if sync_data is not None:
                    sync_group = f.create_group('sync_events')
                    sync_group.create_dataset('timestamps', data=sync_data['timestamps'])
                    sync_group.create_dataset('event_types', data=sync_data['types'])

                logger.info(f"Successfully exported session to {output_path}")

        except Exception as e:
            logger.error(f"Failed to export session data: {e}")
            raise ExportError(f"HDF5 export failed: {e}") from e

    async def _load_gsr_data(self) -> Optional[Dict[str, np.ndarray]]:
        """Load GSR data from CSV file.

        Returns:
            Dictionary with GSR data arrays or None if not found
        """
        gsr_file = self.session_path / 'gsr_data.csv'
        if not gsr_file.exists():
            logger.warning(f"GSR data file not found: {gsr_file}")
            return None

        try:
            df = pd.read_csv(gsr_file)
            return {
                'timestamps': df['timestamp_ms'].values,
                'conductance': df['conductance_us'].values,
                'resistance': df['resistance_kohms'].values
            }
        except Exception as e:
            logger.error(f"Failed to load GSR data: {e}")
            return None


class ExportError(Exception):
    """Exception raised during data export operations."""
    pass
```

#### Testing Standards

```python
"""Tests for GSR processing module."""

import asyncio
import pytest
import numpy as np
from pathlib import Path
from unittest.mock import Mock, patch, AsyncMock

from src.gsr_processor import GSRProcessor, GSRSample, SessionDataExporter


class TestGSRProcessor:
    """Test suite for GSRProcessor class."""

    @pytest.fixture
    def processor(self) -> GSRProcessor:
        """Create GSR processor for testing."""
        return GSRProcessor(sample_rate=128)

    @pytest.fixture
    def sample_data(self) -> List[GSRSample]:
        """Create test sample data."""
        samples = []
        for i in range(256):  # 2 seconds at 128Hz
            timestamp = i * 7_812_500  # ~7.8ms intervals
            conductance = 10.0 + np.sin(i * 0.1) * 2.0  # Simulated GSR with variation
            resistance = 1000.0 / conductance if conductance > 0 else 100.0

            samples.append(GSRSample(
                timestamp=timestamp,
                conductance_us=conductance,
                resistance_kohms=resistance,
                sample_index=i,
                quality_score=0.9
            ))
        return samples

    @pytest.mark.asyncio
    async def test_add_sample_updates_buffer(self, processor: GSRProcessor, sample_data: List[GSRSample]):
        """Test that adding samples updates internal buffer."""
        # Given
        sample = sample_data[0]

        # When
        await processor.add_sample(sample)

        # Then
        assert len(processor.samples_buffer) == 1
        assert processor.samples_buffer[0] == sample

    @pytest.mark.asyncio
    async def test_scr_detection(self, processor: GSRProcessor):
        """Test skin conductance response detection."""
        # Given - baseline samples
        baseline_samples = [
            GSRSample(i * 1000000, 10.0, 100.0, i, 0.9)
            for i in range(128)
        ]

        for sample in baseline_samples:
            await processor.add_sample(sample)

        # Mock the signal emission
        scr_mock = Mock()
        processor.scr_detected.connect(scr_mock)

        # When - add sample with elevated conductance (SCR)
        scr_sample = GSRSample(
            timestamp=128_000_000,
            conductance_us=10.5,  # 0.5µS above baseline
            resistance_kohms=95.2,
            sample_index=128,
            quality_score=0.9
        )

        await processor.add_sample(scr_sample)

        # Then
        scr_mock.emit.assert_called_once()
        scr_event = scr_mock.emit.call_args[0][0]
        assert scr_event['amplitude'] > processor.scr_threshold

    def test_signal_quality_calculation(self, processor: GSRProcessor):
        """Test signal quality score calculation."""
        # Given - stable signal samples
        stable_samples = [
            GSRSample(i * 1000000, 10.0, 100.0, i, 0.9)
            for i in range(128)
        ]
        processor.samples_buffer = stable_samples

        # When
        quality = processor.get_signal_quality()

        # Then
        assert 0.8 <= quality <= 1.0  # High quality for stable signal

    def test_signal_quality_with_noise(self, processor: GSRProcessor):
        """Test signal quality with noisy data."""
        # Given - noisy signal samples
        noisy_samples = [
            GSRSample(i * 1000000, 10.0 + np.random.normal(0, 2.0), 100.0, i, 0.9)
            for i in range(128)
        ]
        processor.samples_buffer = noisy_samples

        # When
        quality = processor.get_signal_quality()

        # Then
        assert quality < 0.8  # Lower quality for noisy signal


class TestSessionDataExporter:
    """Test suite for SessionDataExporter class."""

    @pytest.fixture
    def temp_session_dir(self, tmp_path: Path) -> Path:
        """Create temporary session directory."""
        session_dir = tmp_path / "test_session"
        session_dir.mkdir()

        # Create test GSR data file
        gsr_file = session_dir / "gsr_data.csv"
        gsr_data = [
            "timestamp_ms,conductance_us,resistance_kohms,sample_index",
            "1000000,10.5,95.2,1",
            "1008000,10.3,97.1,2",
            "1016000,10.7,93.5,3"
        ]
        gsr_file.write_text('\n'.join(gsr_data))

        return session_dir

    @pytest.fixture
    def exporter(self, temp_session_dir: Path) -> SessionDataExporter:
        """Create session data exporter."""
        return SessionDataExporter(temp_session_dir)

    @pytest.mark.asyncio
    async def test_export_to_hdf5(self, exporter: SessionDataExporter, tmp_path: Path):
        """Test HDF5 export functionality."""
        pytest.importorskip("h5py")  # Skip if h5py not available

        # Given
        output_path = tmp_path / "exported_session.h5"

        # When
        await exporter.export_to_hdf5(output_path)

        # Then
        assert output_path.exists()

        # Verify HDF5 contents
        import h5py
        with h5py.File(output_path, 'r') as f:
            assert 'gsr' in f
            assert 'timestamps' in f['gsr']
            assert 'conductance' in f['gsr']
            assert 'resistance' in f['gsr']
            assert f['gsr'].attrs['sample_rate'] == 128.0

    @pytest.mark.asyncio
    async def test_load_gsr_data_success(self, exporter: SessionDataExporter):
        """Test successful GSR data loading."""
        # When
        gsr_data = await exporter._load_gsr_data()

        # Then
        assert gsr_data is not None
        assert 'timestamps' in gsr_data
        assert 'conductance' in gsr_data
        assert 'resistance' in gsr_data
        assert len(gsr_data['timestamps']) == 3

    @pytest.mark.asyncio
    async def test_load_gsr_data_file_not_found(self, tmp_path: Path):
        """Test GSR data loading when file doesn't exist."""
        # Given
        empty_session_dir = tmp_path / "empty_session"
        empty_session_dir.mkdir()
        exporter = SessionDataExporter(empty_session_dir)

        # When
        gsr_data = await exporter._load_gsr_data()

        # Then
        assert gsr_data is None
```

### Documentation Standards

#### Code Documentation
```kotlin

class GSRRecorder @Inject constructor(
    private val shimmerManager: ShimmerManager,
    private val fileManager: FileManager,
    private val timeManager: TimeManager
) : SensorRecorder {
    // Implementation...
}
````

#### API Documentation

Use clear, comprehensive documentation:

```python
async def start_session(self, config: SessionConfig) -> Session:
    """Start a new synchronized recording session across all connected devices.

    This method coordinates session initialization across multiple Android devices,
    performs time synchronization, and begins data collection. The session will
    continue until explicitly stopped or the configured duration expires.

    Args:
        config: Session configuration including participant info, duration,
               and sensor settings. See SessionConfig for detailed options.

    Returns:
        Session object containing session metadata and device assignments.
        The session will be in STARTING state initially, transitioning to
        RECORDING once all devices confirm readiness.

    Raises:
        NoDevicesAvailableError: If no Android devices are connected
        SessionStartError: If session initialization fails on any device
        TimeoutError: If device responses exceed configured timeout
        ConfigurationError: If session config contains invalid parameters

    Example:
        >>> config = SessionConfig(
        ...     participant_id="P001",
        ...     duration_ms=300000,  # 5 minutes
        ...     gsr_enabled=True,
        ...     video_quality="4K"
        ... )
        >>> session = await session_manager.start_session(config)
        >>> print(f"Session {session.id} started with {len(session.devices)} devices")

    Note:
        This is an async operation that may take 3-5 seconds to complete as it
        involves network communication and device initialization. Progress can
        be monitored via the session_status_changed signal.

    See Also:
        stop_session(): End current recording session
        get_session_status(): Check current session state
        SessionConfig: Complete configuration options
    """
```

## 🧪 Testing Guidelines

### Test Categories

#### Unit Tests

- **Coverage Target**: >80% line coverage
- **Focus**: Individual functions and classes
- **Mock Dependencies**: External services and hardware
- **Fast Execution**: <10ms per test

#### Integration Tests

- **Coverage**: Component interactions
- **Real Dependencies**: Limited external services
- **Data Validation**: End-to-end data flow
- **Moderate Duration**: <1s per test

#### System Tests

- **Coverage**: Complete workflows
- **Real Hardware**: When available
- **Performance**: Timing and throughput validation
- **Extended Duration**: <30s per test

### Test Data Management

#### Mock Data Generation

```kotlin
object TestDataFactory {
    fun createGSRSamples(
        count: Int = 128,
        sampleRate: Int = 128,
        baselineConductance: Double = 10.0,
        noiseLevel: Double = 0.1
    ): List<GSRSample> {
        return (0 until count).map { i ->
            val timestamp = i * (1_000_000_000L / sampleRate)
            val conductance = baselineConductance +
                             sin(i * 0.1) * 2.0 +  // Slow variation
                             Random.nextGaussian() * noiseLevel  // Noise

            GSRSample(
                timestamp = timestamp,
                conductanceMicrosiemens = conductance,
                resistanceKilohms = 1000.0 / conductance,
                sampleIndex = i.toLong(),
                qualityScore = 0.9f
            )
        }
    }

    fun createTestSession(
        sessionId: String = "test_session_${System.currentTimeMillis()}",
        participantId: String = "P_TEST",
        durationMs: Long = 60000L
    ): Session {
        return Session(
            id = sessionId,
            participantId = participantId,
            studyProtocol = "test_protocol",
            startTime = Instant.now(),
            configuration = SessionConfig(
                durationMs = durationMs,
                gsrSampleRate = 128,
                videoResolution = VideoResolution.HD_1080P
            )
        )
    }
}
```

### Performance Testing

#### Benchmarking

```python
import time
import statistics
from typing import List

class PerformanceBenchmark:
    """Performance testing utilities for MPDC4GSR components."""

    @staticmethod
    def benchmark_gsr_processing(processor: GSRProcessor, sample_count: int = 1000) -> dict:
        """Benchmark GSR sample processing performance.

        Args:
            processor: GSR processor to test
            sample_count: Number of samples to process

        Returns:
            Performance metrics including latency and throughput
        """
        samples = TestDataFactory.create_gsr_samples(sample_count)
        processing_times = []

        for sample in samples:
            start_time = time.perf_counter()
            asyncio.run(processor.add_sample(sample))
            end_time = time.perf_counter()

            processing_times.append((end_time - start_time) * 1000)  # Convert to ms

        return {
            'mean_latency_ms': statistics.mean(processing_times),
            'median_latency_ms': statistics.median(processing_times),
            'max_latency_ms': max(processing_times),
            'throughput_samples_per_sec': sample_count / sum(processing_times) * 1000,
            'samples_processed': sample_count
        }

    @staticmethod
    def validate_timing_accuracy(target_rate: int = 128, duration_sec: int = 10) -> dict:
        """Validate GSR sampling timing accuracy.

        Args:
            target_rate: Expected sample rate in Hz
            duration_sec: Test duration in seconds

        Returns:
            Timing accuracy metrics
        """
        expected_interval_ms = 1000.0 / target_rate
        timestamps = []

        # Simulate sample collection
        start_time = time.time()
        while time.time() - start_time < duration_sec:
            timestamps.append(time.time() * 1000)  # Convert to ms
            time.sleep(expected_interval_ms / 1000.0)

        # Calculate intervals
        intervals = [timestamps[i+1] - timestamps[i] for i in range(len(timestamps)-1)]

        return {
            'expected_interval_ms': expected_interval_ms,
            'mean_interval_ms': statistics.mean(intervals),
            'std_deviation_ms': statistics.stdev(intervals),
            'max_jitter_ms': max(intervals) - min(intervals),
            'accuracy_percent': (1.0 - abs(statistics.mean(intervals) - expected_interval_ms) / expected_interval_ms) * 100
        }
```

## 📋 Pull Request Process

### Before Submitting

#### Checklist

- [ ] **Tests pass**: All existing tests continue to pass
- [ ] **New tests added**: For new functionality or bug fixes
- [ ] **Code style**: Follows project coding standards
- [ ] **Documentation**: Updated for API changes
- [ ] **Performance**: No significant performance regression
- [ ] **Backwards compatibility**: Maintains API compatibility
- [ ] **Security**: No new security vulnerabilities

#### Code Quality Checks

```bash
# Android checks
./gradlew ktlintCheck
./gradlew test
./gradlew connectedAndroidTest

# PC Controller checks
black --check src/
flake8 src/
mypy src/
pytest --cov=src --cov-report=html

# Documentation checks
markdownlint docs/
vale docs/
```

### Pull Request Template

```markdown
## Description

Brief description of changes and motivation.

## Type of Change

- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as
      expected)
- [ ] Documentation update

## Testing

- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing completed
- [ ] Performance testing completed (if applicable)

## Screenshots (if applicable)

Include screenshots for UI changes.

## Checklist

- [ ] My code follows the style guidelines of this project
- [ ] I have performed a self-review of my own code
- [ ] I have commented my code, particularly in hard-to-understand areas
- [ ] I have made corresponding changes to the documentation
- [ ] My changes generate no new warnings
- [ ] I have added tests that prove my fix is effective or that my feature works
- [ ] New and existing unit tests pass locally with my changes

## Related Issues

Closes #123 Related to #456
```

### Review Process

1. **Automated Checks**: CI/CD pipeline runs automatically
2. **Code Review**: At least one maintainer review required
3. **Testing**: Automated and manual testing validation
4. **Documentation**: Review of documentation updates
5. **Approval**: Maintainer approval required for merge

## 🏷️ Release Process

### Version Numbering

We follow [Semantic Versioning](https://semver.org/):

- **MAJOR**: Breaking changes
- **MINOR**: New features, backwards compatible
- **PATCH**: Bug fixes, backwards compatible

Examples:

- `1.0.0` → `1.0.1` (bug fix)
- `1.0.1` → `1.1.0` (new feature)
- `1.1.0` → `2.0.0` (breaking change)

### Release Checklist

#### Pre-Release

- [ ] All tests passing
- [ ] Documentation updated
- [ ] CHANGELOG.md updated
- [ ] Version numbers bumped
- [ ] Performance benchmarks validated

#### Release

- [ ] Create release tag
- [ ] Build and test APK
- [ ] Generate release notes
- [ ] Publish GitHub release
- [ ] Update documentation website

#### Post-Release

- [ ] Monitor for issues
- [ ] Update project boards
- [ ] Plan next release cycle

## 🎨 UI/UX Contributions

### Design Guidelines

#### Android Material Design

- Follow Material Design 3 principles
- Use consistent color schemes and typography
- Implement proper accessibility features
- Test on various screen sizes

#### PC Controller Design

- Follow platform conventions (Windows/macOS/Linux)
- Maintain consistent PyQt6 styling
- Ensure keyboard navigation support
- Implement proper window management

### Accessibility Requirements

- **Color Contrast**: WCAG AA compliance minimum
- **Text Size**: Support system font scaling
- **Keyboard Navigation**: Full keyboard access
- **Screen Readers**: Proper labeling and descriptions
- **Touch Targets**: Minimum 44dp touch targets

## 🤝 Community

### Communication Channels

- **GitHub Issues**: Bug reports and feature requests
- **GitHub Discussions**: Questions and general discussion
- **Pull Requests**: Code contributions and reviews

### Code of Conduct

We follow the [Contributor Covenant](https://www.contributor-covenant.org/) code of conduct. Please
be respectful and inclusive in all interactions.

### Recognition

Contributors are recognized in:

- CONTRIBUTORS.md file
- Release notes for significant contributions
- GitHub contributor statistics

---

**Thank you for contributing to MPDC4GSR!** Your contributions help make physiological sensing
research more accessible and reliable. 🚀

_For questions about contributing, feel free to open a GitHub Discussion or comment on relevant
issues._
