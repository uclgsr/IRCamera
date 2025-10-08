import asyncio
import numpy as np
import pandas as pd
import warnings
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass
from datetime import datetime
from enum import Enum
from scipy import signal, stats
from scipy.ndimage import uniform_filter1d
from typing import Any, Dict, List, Optional, Tuple

warnings.filterwarnings("ignore", category=RuntimeWarning)


class StressLevel(Enum):
    VERY_LOW = "very_low"
    LOW = "low"
    MODERATE = "moderate"
    HIGH = "high"
    VERY_HIGH = "very_high"


@dataclass
class GSRFeatures:
    timestamp: float
    device_id: str
    session_id: str

    mean_gsr: float
    std_gsr: float
    min_gsr: float
    max_gsr: float
    range_gsr: float

    slope: float
    slope_significance: float

    peak_count: int
    peak_amplitude_mean: float
    peak_amplitude_std: float
    peak_frequency: float

    rising_time: float
    rapid_changes: int

    power_low_freq: float
    power_mid_freq: float
    power_high_freq: float
    spectral_entropy: float

    stress_score: float
    stress_level: StressLevel
    confidence: float


@dataclass
class GSRAnalysisReport:
    session_id: str
    device_id: str
    start_time: datetime
    end_time: datetime
    duration_minutes: float

    total_samples: int
    sampling_rate: float
    data_quality: float

    features: List[GSRFeatures]

    average_stress_score: float
    peak_stress_score: float
    stress_distribution: Dict[str, float]

    stress_trend: str
    trend_confidence: float

    recommendations: List[str]


class GSRAnalytics:

    def __init__(
            self,
            window_size_seconds: int = 60,
            overlap_seconds: int = 30,
            sampling_rate: float = 128.0,
    ):

        self.window_size_seconds = window_size_seconds
        self.overlap_seconds = overlap_seconds
        self.sampling_rate = sampling_rate
        self.window_size_samples = int(window_size_seconds * sampling_rate)
        self.overlap_samples = int(overlap_seconds * sampling_rate)

        self.device_buffers: Dict[str, np.ndarray] = {}
        self.device_timestamps: Dict[str, np.ndarray] = {}
        self.last_analysis: Dict[str, float] = {}

        self.feature_history: Dict[str, List[GSRFeatures]] = {}

        self.executor = ThreadPoolExecutor(max_workers=4)

    def add_gsr_samples(
            self,
            device_id: None = str,
            session_id: None = str,
            gsr_values: None = List[float],
            timestamps: None = List[float],
    ) -> None:

        if not gsr_values or not timestamps:
            return

        device_key = f"{device_id}_{session_id}"

        if device_key not in self.device_buffers:
            self.device_buffers[device_key] = np.array([])
            self.device_timestamps[device_key] = np.array([])
            self.last_analysis[device_key] = 0
            self.feature_history[device_key] = []

        self.device_buffers[device_key] = np.append(
            self.device_buffers[device_key], gsr_values
        )
        self.device_timestamps[device_key] = np.append(
            self.device_timestamps[device_key], timestamps
        )

        max_samples = int(10 * 60 * self.sampling_rate)
        if len(self.device_buffers[device_key]) > max_samples:
            excess = len(self.device_buffers[device_key]) - max_samples
            self.device_buffers[device_key] = self.device_buffers[device_key][excess:]
            self.device_timestamps[device_key] = self.device_timestamps[device_key][
                excess:
            ]

        current_time = timestamps[-1] if timestamps else 0
        time_since_last = current_time - self.last_analysis[device_key]

        if (
                time_since_last >= (self.window_size_seconds - self.overlap_seconds)
                and len(self.device_buffers[device_key]) >= self.window_size_samples
        ):

            try:
                asyncio.create_task(
                    self._analyze_window_async(device_id, session_id, current_time)
                )
            except RuntimeError:
                self._analyze_window_sync(device_id, session_id, current_time)

    async def _analyze_window_async(
            self, device_id: str, session_id: str, current_time: float
    ):

        loop = asyncio.get_event_loop()
        await loop.run_in_executor(
            self.executor,
            self._analyze_window_sync,
            device_id,
            session_id,
            current_time,
        )

    def _analyze_window_sync(
            self, device_id: str, session_id: str, current_time: float
    ):

        device_key = f"{device_id}_{session_id}"

        buffer = self.device_buffers[device_key]
        timestamps = self.device_timestamps[device_key]

        if len(buffer) < self.window_size_samples:
            return

        window_data = buffer[-self.window_size_samples:]
        window_timestamps = timestamps[-self.window_size_samples:]

        features = self._extract_features(
            device_id, session_id, window_data, window_timestamps
        )

        if features:
            self.feature_history[device_key].append(features)

            if len(self.feature_history[device_key]) > 100:
                self.feature_history[device_key] = self.feature_history[device_key][
                    -100:
                ]

            self.last_analysis[device_key] = current_time

    def _extract_features(
            self,
            device_id: str,
            session_id: str,
            gsr_data: np.ndarray,
            timestamps: np.ndarray,
    ) -> Optional[GSRFeatures]:

        if len(gsr_data) < 10:
            return None

        gsr_clean = self._clean_gsr_signal(gsr_data)

        mean_gsr = float(np.mean(gsr_clean))
        std_gsr = float(np.std(gsr_clean))
        min_gsr = float(np.min(gsr_clean))
        max_gsr = float(np.max(gsr_clean))
        range_gsr = max_gsr - min_gsr

        slope, slope_p = self._calculate_trend(gsr_clean)

        peaks = self._detect_peaks(gsr_clean)
        peak_count = len(peaks)
        peak_amplitudes = gsr_clean[peaks] if len(peaks) > 0 else np.array([0])
        peak_amplitude_mean = float(np.mean(peak_amplitudes))
        peak_amplitude_std = float(np.std(peak_amplitudes))
        peak_frequency = (
                (peak_count / len(gsr_clean)) * self.sampling_rate * 60
        )

        rising_time = self._calculate_rising_time(gsr_clean)
        rapid_changes = self._count_rapid_changes(gsr_clean)

        power_bands = self._analyze_frequency_domain(gsr_clean)

        stress_score, stress_level, confidence = self._assess_stress(
            gsr_clean,
            mean_gsr,
            std_gsr,
            range_gsr,
            peak_frequency,
            rising_time,
            rapid_changes,
        )

        return GSRFeatures(
            timestamp=float(timestamps[-1]),
            device_id=device_id,
            session_id=session_id,
            mean_gsr=mean_gsr,
            std_gsr=std_gsr,
            min_gsr=min_gsr,
            max_gsr=max_gsr,
            range_gsr=range_gsr,
            slope=float(slope),
            slope_significance=float(1.0 - slope_p) if slope_p > 0 else 1.0,
            peak_count=peak_count,
            peak_amplitude_mean=peak_amplitude_mean,
            peak_amplitude_std=peak_amplitude_std,
            peak_frequency=peak_frequency,
            rising_time=rising_time,
            rapid_changes=rapid_changes,
            power_low_freq=power_bands.get("low", 0.0),
            power_mid_freq=power_bands.get("mid", 0.0),
            power_high_freq=power_bands.get("high", 0.0),
            spectral_entropy=power_bands.get("entropy", 0.0),
            stress_score=stress_score,
            stress_level=stress_level,
            confidence=confidence,
        )

    def _clean_gsr_signal(self, gsr_data: np.ndarray) -> np.ndarray:

        mean_val = np.mean(gsr_data)
        std_val = np.std(gsr_data)
        outlier_threshold = 3.0

        clean_data = np.copy(gsr_data)
        outlier_mask = np.abs(clean_data - mean_val) > (outlier_threshold * std_val)

        if np.any(outlier_mask):
            outlier_indices = np.where(outlier_mask)[0]
            for idx in outlier_indices:

                if idx > 0 and idx < len(clean_data) - 1:
                    clean_data[idx] = (clean_data[idx - 1] + clean_data[idx + 1]) / 2
                elif idx == 0:
                    clean_data[idx] = clean_data[1]
                else:
                    clean_data[idx] = clean_data[-2]

        if len(clean_data) > 10:
            window_size = min(5, len(clean_data) // 10)
            clean_data = uniform_filter1d(clean_data, size=window_size)

        return clean_data

    def _calculate_trend(self, gsr_data: np.ndarray) -> Tuple[float, float]:

        x = np.arange(len(gsr_data))
        slope, intercept, r_value, p_value, std_err = stats.linregress(x, gsr_data)
        return slope, p_value

    def _detect_peaks(self, gsr_data: np.ndarray) -> np.ndarray:

        prominence_threshold = np.std(gsr_data) * 0.5
        min_distance = int(self.sampling_rate * 2)

        peaks, properties = signal.find_peaks(
            gsr_data, prominence=prominence_threshold, distance=min_distance
        )

        return peaks

    def _calculate_rising_time(self, gsr_data: np.ndarray) -> float:

        diff = np.diff(gsr_data)
        rising_samples = np.sum(diff > 0)
        total_samples = len(diff)
        return (
            (rising_samples / total_samples) * 100.0 if total_samples > 0 else 0.0
        )

    def _count_rapid_changes(self, gsr_data: np.ndarray) -> int:

        diff = np.diff(gsr_data)
        std_diff = np.std(diff)

        rapid_threshold = 2.0 * std_diff
        rapid_changes = np.sum(np.abs(diff) > rapid_threshold)

        return int(rapid_changes)

    def _analyze_frequency_domain(self, gsr_data: np.ndarray) -> Dict[str, float]:

        freqs, psd = signal.welch(
            gsr_data, fs=self.sampling_rate, nperseg=min(256, len(gsr_data) // 4)
        )

        low_band = (freqs >= 0.01) & (freqs < 0.08)
        mid_band = (freqs >= 0.08) & (freqs < 0.25)
        high_band = (freqs >= 0.25) & (freqs < 2.0)

        power_low = (
            float(np.trapz(psd[low_band], freqs[low_band]))
            if np.any(low_band)
            else 0.0
        )
        power_mid = (
            float(np.trapz(psd[mid_band], freqs[mid_band]))
            if np.any(mid_band)
            else 0.0
        )
        power_high = (
            float(np.trapz(psd[high_band], freqs[high_band]))
            if np.any(high_band)
            else 0.0
        )

        psd_norm = psd / np.sum(psd)
        psd_norm = psd_norm[psd_norm > 0]
        spectral_entropy = (
            float(-np.sum(psd_norm * np.log2(psd_norm)))
            if len(psd_norm) > 0
            else 0.0
        )

        return {
            "low": power_low,
            "mid": power_mid,
            "high": power_high,
            "entropy": spectral_entropy,
        }

    def _assess_stress(
            self,
            gsr_data: np.ndarray,
            mean_gsr: float,
            std_gsr: float,
            range_gsr: float,
            peak_frequency: float,
            rising_time: float,
            rapid_changes: int,
    ) -> Tuple[float, StressLevel, float]:

        indicators = []

        if mean_gsr > 5.0:
            indicators.append(min(mean_gsr / 10.0, 1.0) * 25)

        if std_gsr > 1.0:
            indicators.append(min(std_gsr / 5.0, 1.0) * 20)

        if range_gsr > 2.0:
            indicators.append(min(range_gsr / 10.0, 1.0) * 15)

        if peak_frequency > 5.0:
            indicators.append(min(peak_frequency / 20.0, 1.0) * 20)

        if rising_time > 60.0:
            indicators.append(min((rising_time - 50) / 30.0, 1.0) * 10)

        if rapid_changes > 10:
            indicators.append(min(rapid_changes / 50.0, 1.0) * 10)

        stress_score = float(np.sum(indicators))
        stress_score = max(0.0, min(100.0, stress_score))

        if stress_score < 20:
            stress_level = StressLevel.VERY_LOW
        elif stress_score < 40:
            stress_level = StressLevel.LOW
        elif stress_score < 60:
            stress_level = StressLevel.MODERATE
        elif stress_score < 80:
            stress_level = StressLevel.HIGH
        else:
            stress_level = StressLevel.VERY_HIGH

        confidence = min(1.0, len(gsr_data) / self.window_size_samples) * 100.0

        return stress_score, stress_level, confidence

    def get_real_time_features(
            self, device_id: str, session_id: str
    ) -> Optional[GSRFeatures]:

        device_key = f"{device_id}_{session_id}"

        if device_key in self.feature_history and self.feature_history[device_key]:
            return self.feature_history[device_key][-1]

        return None

    def generate_session_report(
            self, device_id: str, session_id: str
    ) -> Optional[GSRAnalysisReport]:

        device_key = f"{device_id}_{session_id}"

        if (
                device_key not in self.feature_history
                or not self.feature_history[device_key]
        ):
            return None

        features = self.feature_history[device_key]

        start_time = datetime.fromtimestamp(features[0].timestamp)
        end_time = datetime.fromtimestamp(features[-1].timestamp)
        duration_minutes = (end_time - start_time).total_seconds() / 60.0

        stress_scores = [f.stress_score for f in features]
        average_stress = float(np.mean(stress_scores))
        peak_stress = float(np.max(stress_scores))

        stress_levels = [f.stress_level.value for f in features]
        stress_distribution = {}
        for level in StressLevel:
            count = stress_levels.count(level.value)
            stress_distribution[level.value] = (count / len(stress_levels)) * 100.0

        if len(stress_scores) >= 3:
            x = np.arange(len(stress_scores))
            slope, _, _, p_value, _ = stats.linregress(x, stress_scores)

            if p_value < 0.05:
                if slope > 0.1:
                    stress_trend = "increasing"
                elif slope < -0.1:
                    stress_trend = "decreasing"
                else:
                    stress_trend = "stable"
                trend_confidence = (1.0 - p_value) * 100.0
            else:
                stress_trend = "stable"
                trend_confidence = 50.0
        else:
            stress_trend = "insufficient_data"
            trend_confidence = 0.0

        recommendations = self._generate_recommendations(
            average_stress, peak_stress, stress_trend, stress_distribution
        )

        return GSRAnalysisReport(
            session_id=session_id,
            device_id=device_id,
            start_time=start_time,
            end_time=end_time,
            duration_minutes=duration_minutes,
            total_samples=len(features) * self.window_size_samples,
            sampling_rate=self.sampling_rate,
            data_quality=float(np.mean([f.confidence for f in features])),
            features=features,
            average_stress_score=average_stress,
            peak_stress_score=peak_stress,
            stress_distribution=stress_distribution,
            stress_trend=stress_trend,
            trend_confidence=trend_confidence,
            recommendations=recommendations,
        )

    def _generate_recommendations(
            self,
            average_stress: float,
            peak_stress: float,
            stress_trend: str,
            stress_distribution: Dict[str, float],
    ) -> List[str]:

        recommendations = []

        if average_stress > 70:
            recommendations.append(
                "Consider stress reduction techniques such as deep breathing or meditation"
            )
            recommendations.append("Take regular breaks during stressful activities")

        if peak_stress > 85:
            recommendations.append("Monitor for stress peaks and identify triggers")
            recommendations.append(
                "Consider professional stress management consultation"
            )

        if stress_trend == "increasing":
            recommendations.append(
                "Stress levels are increasing - consider intervention strategies"
            )
            recommendations.append("Evaluate current stressors and coping mechanisms")
        elif stress_trend == "decreasing":
            recommendations.append(
                "Stress levels are decreasing - maintain current strategies"
            )

        high_stress_time = stress_distribution.get("high", 0) + stress_distribution.get(
            "very_high", 0
        )
        if high_stress_time > 30:
            recommendations.append(
                f"High stress detected {high_stress_time:.1f}% of the time"
            )
            recommendations.append(
                "Consider lifestyle modifications to reduce chronic stress"
            )

        if len(recommendations) == 0:
            recommendations.append("Continue monitoring stress levels for patterns")
            recommendations.append("Maintain healthy stress management practices")

        return recommendations

    def export_features_csv(
            self, device_id: str, session_id: str, filename: str
    ) -> bool:

        device_key = f"{device_id}_{session_id}"

        if (
                device_key not in self.feature_history
                or not self.feature_history[device_key]
        ):
            return False

        features = self.feature_history[device_key]

        data = []
        for f in features:
            data.append(
                {
                    "timestamp": f.timestamp,
                    "device_id": f.device_id,
                    "session_id": f.session_id,
                    "mean_gsr": f.mean_gsr,
                    "std_gsr": f.std_gsr,
                    "min_gsr": f.min_gsr,
                    "max_gsr": f.max_gsr,
                    "range_gsr": f.range_gsr,
                    "slope": f.slope,
                    "slope_significance": f.slope_significance,
                    "peak_count": f.peak_count,
                    "peak_amplitude_mean": f.peak_amplitude_mean,
                    "peak_amplitude_std": f.peak_amplitude_std,
                    "peak_frequency": f.peak_frequency,
                    "rising_time": f.rising_time,
                    "rapid_changes": f.rapid_changes,
                    "power_low_freq": f.power_low_freq,
                    "power_mid_freq": f.power_mid_freq,
                    "power_high_freq": f.power_high_freq,
                    "spectral_entropy": f.spectral_entropy,
                    "stress_score": f.stress_score,
                    "stress_level": f.stress_level.value,
                    "confidence": f.confidence,
                }
            )

        df = pd.DataFrame(data)
        df.to_csv(filename, index=False)
        return True

    def cleanup_device_session(
            self, device_id: Any = str, session_id: Any = str
    ) -> Any:

        device_key = f"{device_id}_{session_id}"

        if device_key in self.device_buffers:
            del self.device_buffers[device_key]
        if device_key in self.device_timestamps:
            del self.device_timestamps[device_key]
        if device_key in self.last_analysis:
            del self.last_analysis[device_key]

    def get_stress_summary(self) -> Dict[str, Any]:

        summary = {"active_sessions": len(self.feature_history), "sessions": {}}

        for device_key, features in self.feature_history.items():
            if features:
                latest = features[-1]
                summary["sessions"][device_key] = {
                    "device_id": latest.device_id,
                    "session_id": latest.session_id,
                    "latest_stress_score": latest.stress_score,
                    "latest_stress_level": latest.stress_level.value,
                    "confidence": latest.confidence,
                    "feature_count": len(features),
                }

        return summary
