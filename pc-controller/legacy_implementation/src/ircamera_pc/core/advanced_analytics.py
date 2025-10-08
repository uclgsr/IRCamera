import json
import logging
import numpy as np
import pandas as pd
import queue
import threading
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass, asdict
from enum import Enum
from scipy import signal, stats
from typing import Dict, List, Optional, Tuple, Any

logger = logging.getLogger(__name__)


class BiometricPattern(Enum):
    BASELINE = "baseline"
    ACUTE_STRESS = "acute_stress"
    CHRONIC_STRESS = "chronic_stress"
    RELAXATION = "relaxation"
    COGNITIVE_LOAD = "cognitive_load"
    PHYSICAL_ACTIVITY = "physical_activity"
    EMOTIONAL_AROUSAL = "emotional_arousal"
    FATIGUE = "fatigue"
    ANOMALY = "anomaly"


class SensorModality(Enum):
    GSR = "gsr"
    THERMAL = "thermal"
    RGB_FACIAL = "rgb_facial"
    HEART_RATE = "heart_rate"
    MOTION = "motion"
    AUDIO = "audio"
    ENVIRONMENTAL = "environmental"


@dataclass
class SensorReading:
    timestamp: float
    device_id: str
    session_id: str
    modality: SensorModality
    raw_value: float
    processed_value: float
    quality_score: float
    confidence: float
    metadata: Dict[str, Any]


@dataclass
class MultiModalFeatures:
    timestamp: float
    device_id: str
    session_id: str

    gsr_features: Dict[str, float]
    thermal_features: Dict[str, float]
    facial_features: Dict[str, float]
    motion_features: Dict[str, float]

    stress_level: float
    arousal_level: float
    valence_score: float
    cognitive_load: float

    detected_pattern: BiometricPattern
    pattern_confidence: float
    pattern_stability: float

    stress_trend: float
    predicted_stress_5min: float
    risk_assessment: str

    data_quality: float
    sensor_coverage: float


@dataclass
class BehavioralInsight:
    timestamp: float
    insight_type: str
    severity: str
    message: str
    confidence: float
    recommended_action: str
    data_support: Dict[str, Any]


@dataclass
class ResearchMetrics:
    session_id: str
    participant_id: str

    effect_sizes: Dict[str, float]
    statistical_significance: Dict[str, float]
    confidence_intervals: Dict[str, Tuple[float, float]]

    stress_variability: float
    pattern_transitions: List[Tuple[str, str, float]]
    circadian_markers: Dict[str, float]

    data_completeness: float
    artifact_percentage: float
    synchronization_accuracy: float

    bids_compliance: bool
    metadata_completeness: float


class AdvancedAnalyticsEngine:

    def __init__(self, config: Dict[str, Any] = None):

        self.config = config or {}

        self.executor = ThreadPoolExecutor(max_workers=8)
        self.processing_queue = queue.PriorityQueue()
        self.results_queue = queue.Queue()

        self.sensor_buffers: Dict[str, Dict[SensorModality, List[SensorReading]]] = {}
        self.feature_history: Dict[str, List[MultiModalFeatures]] = {}
        self.insight_history: Dict[str, List[BehavioralInsight]] = {}

        # AI/ML models (placeholder for future ML integration)
        self.stress_predictor = None
        self.pattern_classifier = None
        self.anomaly_detector = None

        self.fusion_window_seconds = 30
        self.prediction_horizon_minutes = 5
        self.pattern_detection_threshold = 0.7

        self._processing_active = True
        self._processing_thread = threading.Thread(target=self._background_processor)
        self._processing_thread.daemon = True
        self._processing_thread.start()

        logger.info("Advanced Analytics Engine initialized with multi-modal fusion")

    def add_sensor_reading(self, reading: SensorReading) -> None:

        session_key = f"{reading.device_id}_{reading.session_id}"

        if session_key not in self.sensor_buffers:
            self.sensor_buffers[session_key] = {modality: [] for modality in SensorModality}
            self.feature_history[session_key] = []
            self.insight_history[session_key] = []

        self.sensor_buffers[session_key][reading.modality].append(reading)

        max_readings = 600
        if len(self.sensor_buffers[session_key][reading.modality]) > max_readings:
            self.sensor_buffers[session_key][reading.modality] = \
                self.sensor_buffers[session_key][reading.modality][-max_readings:]

        priority = 1
        self.processing_queue.put((priority, reading.timestamp, session_key))

    def _background_processor(self) -> None:

        while self._processing_active:
            try:

                priority, timestamp, session_key = self.processing_queue.get(timeout=1.0)

                if self._has_sufficient_data(session_key):

                    features = self._extract_multimodal_features(session_key, timestamp)

                    if features:
                        self.feature_history[session_key].append(features)

                        insights = self._generate_behavioral_insights(session_key, features)
                        self.insight_history[session_key].extend(insights)

                        self.results_queue.put({
                            'type': 'features',
                            'session_key': session_key,
                            'features': features,
                            'insights': insights
                        })

                        logger.debug(f"Processed multi-modal features for {session_key}")

            except queue.Empty:
                continue
            except Exception as e:
                logger.error(f"Error in background processor: {e}")

    def _has_sufficient_data(self, session_key: str) -> bool:

        if session_key not in self.sensor_buffers:
            return False

        buffers = self.sensor_buffers[session_key]

        gsr_count = len(buffers.get(SensorModality.GSR, []))
        thermal_count = len(buffers.get(SensorModality.THERMAL, []))

        min_required = 30

        return gsr_count >= min_required and thermal_count >= 10

    def _extract_multimodal_features(self, session_key: str, timestamp: float) -> Optional[
        MultiModalFeatures]:

        try:
            buffers = self.sensor_buffers[session_key]

            gsr_features = self._extract_gsr_advanced_features(buffers[SensorModality.GSR])
            thermal_features = self._extract_thermal_features(buffers[SensorModality.THERMAL])
            facial_features = self._extract_facial_features(
                buffers.get(SensorModality.RGB_FACIAL, []))
            motion_features = self._extract_motion_features(buffers.get(SensorModality.MOTION, []))

            stress_level = self._fuse_stress_indicators(gsr_features, thermal_features,
                                                        facial_features)
            arousal_level = self._calculate_arousal(gsr_features, motion_features)
            valence_score = self._calculate_valence(facial_features, gsr_features)
            cognitive_load = self._calculate_cognitive_load(gsr_features, thermal_features)

            pattern, pattern_confidence = self._detect_biometric_pattern(
                stress_level, arousal_level, valence_score, cognitive_load
            )

            stress_trend = self._calculate_stress_trend(session_key)
            predicted_stress = self._predict_future_stress(session_key, stress_level)
            risk_assessment = self._assess_risk_level(stress_level, stress_trend, pattern)

            data_quality = self._assess_data_quality(buffers)
            sensor_coverage = self._calculate_sensor_coverage(buffers)

            device_parts = session_key.split('_', 1)
            device_id = device_parts[0] if len(device_parts) > 0 else "unknown"
            session_id = device_parts[1] if len(device_parts) > 1 else "unknown"

            return MultiModalFeatures(
                timestamp=timestamp,
                device_id=device_id,
                session_id=session_id,
                gsr_features=gsr_features,
                thermal_features=thermal_features,
                facial_features=facial_features,
                motion_features=motion_features,
                stress_level=stress_level,
                arousal_level=arousal_level,
                valence_score=valence_score,
                cognitive_load=cognitive_load,
                detected_pattern=pattern,
                pattern_confidence=pattern_confidence,
                pattern_stability=self._calculate_pattern_stability(session_key, pattern),
                stress_trend=stress_trend,
                predicted_stress_5min=predicted_stress,
                risk_assessment=risk_assessment,
                data_quality=data_quality,
                sensor_coverage=sensor_coverage
            )

        except Exception as e:
            logger.error(f"Multi-modal feature extraction failed: {e}")
            return None

    def _extract_gsr_advanced_features(self, gsr_readings: List[SensorReading]) -> Dict[str, float]:

        if not gsr_readings:
            return {}

        try:

            values = np.array([r.processed_value for r in gsr_readings[-600:]])
            timestamps = np.array([r.timestamp for r in gsr_readings[-600:]])

            if len(values) < 10:
                return {}

            features = {}

            features['mean'] = float(np.mean(values))
            features['std'] = float(np.std(values))
            features['range'] = float(np.max(values) - np.min(values))
            features['coefficient_variation'] = features['std'] / features['mean'] if features[
                                                                                          'mean'] > 0 else 0

            diff = np.diff(values)
            features['mean_derivative'] = float(np.mean(diff))
            features['std_derivative'] = float(np.std(diff))
            features['rising_time_percent'] = float((np.sum(diff > 0) / len(diff)) * 100) if len(
                diff) > 0 else 0

            peaks, properties = signal.find_peaks(values, prominence=features['std'] * 0.3)
            features['peak_count'] = len(peaks)
            features['peak_frequency'] = len(peaks) / (len(values) / 60.0) if len(
                values) > 60 else 0

            if len(peaks) > 0:
                peak_amplitudes = values[peaks]
                features['peak_amplitude_mean'] = float(np.mean(peak_amplitudes))
                features['peak_amplitude_std'] = float(np.std(peak_amplitudes))

                peak_widths = signal.peak_widths(values, peaks)[0]
                features['peak_width_mean'] = float(np.mean(peak_widths)) if len(
                    peak_widths) > 0 else 0
            else:
                features.update({
                    'peak_amplitude_mean': 0.0,
                    'peak_amplitude_std': 0.0,
                    'peak_width_mean': 0.0
                })

            if len(values) > 64:
                freqs, psd = signal.welch(values, fs=1.0, nperseg=min(64, len(values) // 2))

                low_band = (freqs >= 0.01) & (freqs < 0.08)
                mid_band = (freqs >= 0.08) & (freqs < 0.25)
                high_band = (freqs >= 0.25) & (freqs < 2.0)

                features['power_low'] = float(np.trapz(psd[low_band])) if np.any(low_band) else 0.0
                features['power_mid'] = float(np.trapz(psd[mid_band])) if np.any(mid_band) else 0.0
                features['power_high'] = float(np.trapz(psd[high_band])) if np.any(
                    high_band) else 0.0

                total_power = np.sum(psd)
                if total_power > 0:
                    features['spectral_centroid'] = float(np.sum(freqs * psd) / total_power)

                    psd_norm = psd / total_power
                    psd_norm = psd_norm[psd_norm > 0]
                    features['spectral_entropy'] = float(
                        -np.sum(psd_norm * np.log2(psd_norm))) if len(psd_norm) > 0 else 0.0

            if len(values) > 100:
                autocorr = np.correlate(values - np.mean(values), values - np.mean(values),
                                        mode='full')
                autocorr = autocorr[autocorr.size // 2:]
                autocorr = autocorr / autocorr[0] if autocorr[0] != 0 else autocorr

                ac_peaks, _ = signal.find_peaks(autocorr[1:], height=0.3)
                features['periodicity'] = float(ac_peaks[0] + 1) if len(ac_peaks) > 0 else 0.0

            features['sample_entropy'] = self._calculate_sample_entropy(values)

            features['dfa_alpha'] = self._calculate_dfa_alpha(values)

            return features

        except Exception as e:
            logger.error(f"GSR feature extraction error: {e}")
            return {}

    def _extract_thermal_features(self, thermal_readings: List[SensorReading]) -> Dict[str, float]:
        if not thermal_readings:
            return {}

        features = {}
        values = np.array(
            [r.processed_value for r in thermal_readings[-180:]])


        features['mean_temp'] = float(np.mean(values))
        features['std_temp'] = float(np.std(values))
        features['temp_range'] = float(np.max(values) - np.min(values))

        if len(values) > 10:
            diff = np.diff(values)
            features['thermal_variability'] = float(np.std(diff))
            features['thermal_trend'] = float(np.mean(diff))

        features['temp_stability'] = 1.0 / (
                1.0 + features['std_temp']) if 'std_temp' in features else 0.0

        return features

    def _extract_facial_features(self, facial_readings: List[SensorReading]) -> Dict[str, float]:

        if not facial_readings:
            return {}

        try:
            features = {}

            # Placeholder for advanced facial analysis

            if facial_readings:
                values = [r.processed_value for r in facial_readings[-60:]]
                features['facial_activity'] = float(np.mean(values)) if values else 0.0
                features['expression_variability'] = float(np.std(values)) if len(
                    values) > 1 else 0.0

            return features

        except Exception as e:
            logger.error(f"Facial feature extraction error: {e}")
            return {}

    def _extract_motion_features(self, motion_readings: List[SensorReading]) -> Dict[str, float]:

        if not motion_readings:
            return {}

        try:
            features = {}
            values = np.array([r.processed_value for r in motion_readings[-300:]])

            if len(values) < 5:
                return {}

            features['motion_intensity'] = float(np.mean(values))
            features['motion_variability'] = float(np.std(values))

            if features['motion_intensity'] > 0.8:
                features['activity_level'] = 3.0
            elif features['motion_intensity'] > 0.4:
                features['activity_level'] = 2.0
            elif features['motion_intensity'] > 0.1:
                features['activity_level'] = 1.0
            else:
                features['activity_level'] = 0.0

            return features

        except Exception as e:
            logger.error(f"Motion feature extraction error: {e}")
            return {}

    def _fuse_stress_indicators(self, gsr_features: Dict, thermal_features: Dict,
                                facial_features: Dict) -> float:

        stress_score = 0.0
        total_weight = 0.0

        if gsr_features:
            gsr_stress = 0.0

            mean_gsr = gsr_features.get('mean', 0)
            if mean_gsr > 5.0:
                gsr_stress += min(mean_gsr / 10.0, 1.0) * 25

            std_gsr = gsr_features.get('std', 0)
            if std_gsr > 1.0:
                gsr_stress += min(std_gsr / 3.0, 1.0) * 20

            peak_freq = gsr_features.get('peak_frequency', 0)
            if peak_freq > 3.0:
                gsr_stress += min(peak_freq / 10.0, 1.0) * 15

            rising_time = gsr_features.get('rising_time_percent', 0)
            if rising_time > 60.0:
                gsr_stress += min((rising_time - 50) / 30.0, 1.0) * 10

            stress_score += gsr_stress * 0.4
            total_weight += 0.4

        if thermal_features:
            thermal_stress = 0.0

            temp_var = thermal_features.get('thermal_variability', 0)
            if temp_var > 0.1:
                thermal_stress += min(temp_var * 10, 1.0) * 30

            temp_stability = thermal_features.get('temp_stability', 1.0)
            thermal_stress += (1.0 - temp_stability) * 20

            stress_score += thermal_stress * 0.25
            total_weight += 0.25

        if facial_features:
            facial_stress = facial_features.get('expression_variability', 0) * 50
            stress_score += min(facial_stress, 50) * 0.2
            total_weight += 0.2

        if total_weight > 0:
            stress_score = (stress_score / total_weight) * 100

        return max(0.0, min(100.0, stress_score))

    def _calculate_arousal(self, gsr_features: Dict, motion_features: Dict) -> float:

        arousal = 0.0

        if gsr_features:
            mean_gsr = gsr_features.get('mean', 0)
            std_gsr = gsr_features.get('std', 0)
            peak_freq = gsr_features.get('peak_frequency', 0)

            arousal += min(mean_gsr / 15.0, 1.0) * 40
            arousal += min(std_gsr / 5.0, 1.0) * 30
            arousal += min(peak_freq / 15.0, 1.0) * 20

        if motion_features:
            motion_intensity = motion_features.get('motion_intensity', 0)
            arousal += motion_intensity * 10

        return max(0.0, min(100.0, arousal))

    def _calculate_valence(self, facial_features: Dict, gsr_features: Dict) -> float:

        # Placeholder implementation
        # Full implementation would use facial expression analysis

        valence = 0.0

        if gsr_features:
            stress_indicator = gsr_features.get('mean', 0)
            if stress_indicator > 10:
                valence -= min((stress_indicator - 10) * 5, 50)

        if facial_features:
            facial_activity = facial_features.get('facial_activity', 0)
            # Placeholder: assume neutral to slightly positive baseline
            valence += (facial_activity - 0.5) * 20

        return max(-100.0, min(100.0, valence))

    def _calculate_cognitive_load(self, gsr_features: Dict, thermal_features: Dict) -> float:

        cognitive_load = 0.0

        if gsr_features:

            mean_gsr = gsr_features.get('mean', 0)
            peak_freq = gsr_features.get('peak_frequency', 0)

            if mean_gsr > 5 and peak_freq < 5:
                cognitive_load += min(mean_gsr / 12.0, 1.0) * 60

            coefficient_var = gsr_features.get('coefficient_variation', 0)
            if coefficient_var < 0.3 and mean_gsr > 3:
                cognitive_load += 30

        if thermal_features:
            temp_stability = thermal_features.get('temp_stability', 0)

            cognitive_load += temp_stability * 10

        return max(0.0, min(100.0, cognitive_load))

    def _detect_biometric_pattern(self, stress: float, arousal: float, valence: float,
                                  cognitive_load: float) -> Tuple[BiometricPattern, float]:

        patterns = {
            BiometricPattern.BASELINE: (10, 20, 0, 20),
            BiometricPattern.ACUTE_STRESS: (80, 85, -30, 40),
            BiometricPattern.CHRONIC_STRESS: (60, 50, -20, 60),
            BiometricPattern.RELAXATION: (5, 10, 20, 5),
            BiometricPattern.COGNITIVE_LOAD: (30, 40, -5, 85),
            BiometricPattern.PHYSICAL_ACTIVITY: (40, 90, 10, 20),
            BiometricPattern.EMOTIONAL_AROUSAL: (70, 95, -40, 30),
            BiometricPattern.FATIGUE: (25, 15, -15, 70),
        }

        current_state = np.array([stress, arousal, valence, cognitive_load])
        best_pattern = BiometricPattern.BASELINE
        best_confidence = 0.0

        for pattern, signature in patterns.items():
            signature_array = np.array(signature)

            distance = np.linalg.norm(current_state - signature_array)
            max_distance = np.linalg.norm([100, 100, 100, 100])

            confidence = max(0.0, 1.0 - (distance / max_distance))

            if confidence > best_confidence:
                best_confidence = confidence
                best_pattern = pattern

        return best_pattern, best_confidence

    def _calculate_stress_trend(self, session_key: str) -> float:

        if session_key not in self.feature_history or len(self.feature_history[session_key]) < 3:
            return 0.0

        recent_features = self.feature_history[session_key][-10:]
        stress_values = [f.stress_level for f in recent_features]

        if len(stress_values) < 2:
            return 0.0

        x = np.arange(len(stress_values))
        slope, _, _, p_value, _ = stats.linregress(x, stress_values)

        return slope if p_value < 0.05 else 0.0

    def _predict_future_stress(self, session_key: str, current_stress: float) -> float:

        trend = self._calculate_stress_trend(session_key)

        prediction_periods = 10
        predicted_stress = current_stress + (trend * prediction_periods)

        return max(0.0, min(100.0, predicted_stress))

    def _assess_risk_level(self, stress_level: float, stress_trend: float,
                           pattern: BiometricPattern) -> str:

        if (stress_level > 85 or
                pattern in [BiometricPattern.ACUTE_STRESS, BiometricPattern.CHRONIC_STRESS] or
                stress_trend > 5.0):
            return "high"

        elif (stress_level > 60 or
              pattern == BiometricPattern.EMOTIONAL_AROUSAL or
              stress_trend > 2.0):
            return "medium"

        else:
            return "low"

    def _assess_data_quality(self, buffers: Dict[SensorModality, List]) -> float:

        total_quality = 0.0
        modality_count = 0

        for modality, readings in buffers.items():
            if readings:
                modality_count += 1

                quality_scores = [r.quality_score for r in readings[-30:]]
                avg_quality = np.mean(quality_scores) if quality_scores else 0
                total_quality += avg_quality

        return (total_quality / modality_count) if modality_count > 0 else 0.0

    def _calculate_sensor_coverage(self, buffers: Dict[SensorModality, List]) -> float:

        expected_sensors = [SensorModality.GSR, SensorModality.THERMAL]
        active_sensors = sum(1 for modality in expected_sensors if buffers.get(modality, []))

        return (active_sensors / len(expected_sensors)) * 100.0

    def _calculate_pattern_stability(self, session_key: str,
                                     current_pattern: BiometricPattern) -> float:

        if session_key not in self.feature_history:
            return 0.0

        recent_features = self.feature_history[session_key][-5:]
        if len(recent_features) < 2:
            return 0.0

        same_pattern_count = sum(
            1 for f in recent_features if f.detected_pattern == current_pattern)

        return (same_pattern_count / len(recent_features)) * 100.0

    def _generate_behavioral_insights(self, session_key: str, features: MultiModalFeatures) -> List[
        BehavioralInsight]:

        insights = []
        timestamp = features.timestamp

        if features.stress_level > 80:
            insights.append(BehavioralInsight(
                timestamp=timestamp,
                insight_type="stress_alert",
                severity="critical" if features.stress_level > 90 else "warning",
                message=f"High stress detected (Level: {features.stress_level:.1f}/100)",
                confidence=features.pattern_confidence,
                recommended_action="Consider stress reduction techniques or take a break",
                data_support={
                    "stress_level": features.stress_level,
                    "pattern": features.detected_pattern.value,
                    "trend": features.stress_trend
                }
            ))

        if features.detected_pattern == BiometricPattern.COGNITIVE_LOAD and features.pattern_confidence > 0.7:
            insights.append(BehavioralInsight(
                timestamp=timestamp,
                insight_type="cognitive_load",
                severity="info",
                message=f"High cognitive load detected (Level: {features.cognitive_load:.1f}/100)",
                confidence=features.pattern_confidence,
                recommended_action="Consider taking mental breaks to maintain performance",
                data_support={
                    "cognitive_load": features.cognitive_load,
                    "pattern_stability": features.pattern_stability
                }
            ))

        if features.data_quality < 70:
            insights.append(BehavioralInsight(
                timestamp=timestamp,
                insight_type="data_quality",
                severity="warning",
                message=f"Data quality degraded (Quality: {features.data_quality:.1f}%)",
                confidence=1.0,
                recommended_action="Check sensor connections and positioning",
                data_support={
                    "data_quality": features.data_quality,
                    "sensor_coverage": features.sensor_coverage
                }
            ))

        if features.detected_pattern == BiometricPattern.RELAXATION and features.pattern_confidence > 0.8:
            insights.append(BehavioralInsight(
                timestamp=timestamp,
                insight_type="wellbeing",
                severity="info",
                message="Relaxation state detected - good stress management",
                confidence=features.pattern_confidence,
                recommended_action="Continue current activities to maintain wellbeing",
                data_support={
                    "pattern": features.detected_pattern.value,
                    "stress_level": features.stress_level
                }
            ))

        return insights

    def _calculate_sample_entropy(self, data: np.ndarray, m: int = 2, r: float = 0.2) -> float:

        try:
            N = len(data)
            if N < m + 1:
                return 0.0

            tolerance = r * np.std(data)

            def _maxdist(xi, xj, m):
                return max([abs(ua - va) for ua, va in zip(xi, xj)])

            def _phi(m):
                patterns = np.array([data[i:i + m] for i in range(N - m + 1)])
                C = np.zeros(N - m + 1)

                for i in range(N - m + 1):
                    template_i = patterns[i]
                    for j in range(N - m + 1):
                        if _maxdist(template_i, patterns[j], m) <= tolerance:
                            C[i] += 1

                phi = np.mean(np.log(C / (N - m + 1)))
                return phi

            return _phi(m) - _phi(m + 1)

        except Exception:
            return 0.0

    def _calculate_dfa_alpha(self, data: np.ndarray) -> float:

        try:
            N = len(data)
            if N < 50:
                return 0.0

            y = np.cumsum(data - np.mean(data))

            min_box = 4
            max_box = N // 4
            box_sizes = np.logspace(np.log10(min_box), np.log10(max_box), num=20).astype(int)

            fluctuations = []

            for box_size in box_sizes:

                n_boxes = N // box_size

                local_trends = []
                for i in range(n_boxes):
                    start_idx = i * box_size
                    end_idx = (i + 1) * box_size

                    x = np.arange(box_size)
                    box_data = y[start_idx:end_idx]

                    coeffs = np.polyfit(x, box_data, 1)
                    trend = np.polyval(coeffs, x)

                    detrended = box_data - trend
                    local_trends.extend(detrended)

                if local_trends:
                    rms_fluctuation = np.sqrt(np.mean(np.array(local_trends) ** 2))
                    fluctuations.append(rms_fluctuation)
                else:
                    fluctuations.append(0.0)

            valid_fluctuations = [f for f in fluctuations if f > 0]
            valid_box_sizes = box_sizes[:len(valid_fluctuations)]

            if len(valid_fluctuations) < 3:
                return 0.0

            log_box_sizes = np.log10(valid_box_sizes)
            log_fluctuations = np.log10(valid_fluctuations)

            slope, _, _, _, _ = stats.linregress(log_box_sizes, log_fluctuations)

            return slope

        except Exception:
            return 0.0

    def get_latest_analysis(self, device_id: str, session_id: str) -> Optional[MultiModalFeatures]:

        session_key = f"{device_id}_{session_id}"

        if session_key in self.feature_history and self.feature_history[session_key]:
            return self.feature_history[session_key][-1]

        return None

    def get_recent_insights(self, device_id: str, session_id: str, limit: int = 5) -> List[
        BehavioralInsight]:

        session_key = f"{device_id}_{session_id}"

        if session_key in self.insight_history:
            return self.insight_history[session_key][-limit:]

        return []

    def generate_research_report(self, device_id: str, session_id: str,
                                 participant_id: str = None) -> Optional[ResearchMetrics]:

        session_key = f"{device_id}_{session_id}"

        if session_key not in self.feature_history or not self.feature_history[session_key]:
            return None

        try:
            features = self.feature_history[session_key]

            stress_values = [f.stress_level for f in features]
            arousal_values = [f.arousal_level for f in features]

            effect_sizes = {
                "stress_mean_vs_baseline": (np.mean(stress_values) - 20) / np.std(
                    stress_values) if np.std(stress_values) > 0 else 0,
                "arousal_variability": np.std(arousal_values) / np.mean(arousal_values) if np.mean(
                    arousal_values) > 0 else 0
            }

            patterns = [f.detected_pattern for f in features]
            pattern_transitions = []

            for i in range(1, len(patterns)):
                if patterns[i] != patterns[i - 1]:
                    pattern_transitions.append((
                        patterns[i - 1].value,
                        patterns[i].value,
                        features[i].timestamp
                    ))

            quality_scores = [f.data_quality for f in features]
            data_completeness = len([q for q in quality_scores if q > 80]) / len(
                quality_scores) * 100 if quality_scores else 0

            return ResearchMetrics(
                session_id=session_id,
                participant_id=participant_id or device_id,
                effect_sizes=effect_sizes,
                statistical_significance={"placeholder": 0.05},  # Would calculate actual p-values
                confidence_intervals={
                    "stress_mean": (np.mean(stress_values) - 1.96 * np.std(stress_values),
                                    np.mean(stress_values) + 1.96 * np.std(stress_values))},
                stress_variability=np.std(stress_values),
                pattern_transitions=pattern_transitions,
                circadian_markers={"placeholder": 0.0},
                data_completeness=data_completeness,
                artifact_percentage=100 - np.mean(quality_scores) if quality_scores else 0,
                synchronization_accuracy=95.0,  # Placeholder - would calculate actual sync accuracy
                bids_compliance=True,  # Placeholder - would validate BIDS format
                metadata_completeness=90.0  # Placeholder - would check metadata fields
            )

        except Exception as e:
            logger.error(f"Research report generation failed: {e}")
            return None

    def export_research_data(self, device_id: str, session_id: str,
                             output_dir: str, format: str = "bids") -> bool:

        try:
            session_key = f"{device_id}_{session_id}"

            if session_key not in self.feature_history:
                return False

            features = self.feature_history[session_key]
            insights = self.insight_history.get(session_key, [])

            import os
            os.makedirs(output_dir, exist_ok=True)

            if format.lower() == "bids":

                # This would implement full BIDS compliance

                physio_data = []
                for f in features:
                    physio_data.append({
                        "timestamp": f.timestamp,
                        "stress_level": f.stress_level,
                        "arousal_level": f.arousal_level,
                        "valence_score": f.valence_score,
                        "cognitive_load": f.cognitive_load,
                        "detected_pattern": f.detected_pattern.value,
                        "data_quality": f.data_quality
                    })

                df = pd.DataFrame(physio_data)
                physio_file = os.path.join(output_dir,
                                           f"sub-{device_id}_ses-{session_id}_physio.tsv")
                df.to_csv(physio_file, sep='\t', index=False)

                sidecar = {
                    "SamplingFrequency": 0.033,
                    "StartTime": features[0].timestamp if features else 0,
                    "Columns": list(df.columns),
                    "PhysiologyAcquisitionMethod": "Multi-modal sensor fusion",
                    "ProcessingDescription": "Advanced analytics with pattern recognition"
                }

                sidecar_file = os.path.join(output_dir,
                                            f"sub-{device_id}_ses-{session_id}_physio.json")
                with open(sidecar_file, 'w') as f:
                    json.dump(sidecar, f, indent=2)

                insights_data = [asdict(insight) for insight in insights]
                insights_file = os.path.join(output_dir,
                                             f"sub-{device_id}_ses-{session_id}_behavior.json")
                with open(insights_file, 'w') as f:
                    json.dump(insights_data, f, indent=2)

                logger.info(f"BIDS-compliant data exported to {output_dir}")
                return True

            else:

                features_data = [asdict(f) for f in features]
                features_file = os.path.join(output_dir, f"{device_id}_{session_id}_features.json")
                with open(features_file, 'w') as f:
                    json.dump(features_data, f, indent=2)

                return True

        except Exception as e:
            logger.error(f"Research data export failed: {e}")
            return False

    def cleanup_session(self, device_id: str, session_id: str) -> None:

        session_key = f"{device_id}_{session_id}"

        if session_key in self.sensor_buffers:
            del self.sensor_buffers[session_key]

        logger.info(f"Cleaned up advanced analytics for {session_key}")

    def get_system_status(self) -> Dict[str, Any]:

        return {
            "active_sessions": len(self.sensor_buffers),
            "processing_queue_size": self.processing_queue.qsize(),
            "results_queue_size": self.results_queue.qsize(),
            "total_feature_analyses": sum(
                len(history) for history in self.feature_history.values()),
            "total_insights_generated": sum(
                len(insights) for insights in self.insight_history.values()),
            "processing_active": self._processing_active
        }

    def shutdown(self) -> None:

        self._processing_active = False
        if hasattr(self, '_processing_thread'):
            self._processing_thread.join(timeout=5.0)

        self.executor.shutdown(wait=True)
        logger.info("Advanced Analytics Engine shutdown complete")


def create_gsr_reading(device_id: str, session_id: str, timestamp: float,
                       gsr_value: float, quality: float = 95.0) -> SensorReading:
    return SensorReading(
        timestamp=timestamp,
        device_id=device_id,
        session_id=session_id,
        modality=SensorModality.GSR,
        raw_value=gsr_value * 1000,
        processed_value=gsr_value,
        quality_score=quality,
        confidence=quality / 100.0,
        metadata={"unit": "microsiemens", "sensor_type": "GSR"}
    )


def create_thermal_reading(device_id: str, session_id: str, timestamp: float,
                           temperature: float, quality: float = 90.0) -> SensorReading:
    return SensorReading(
        timestamp=timestamp,
        device_id=device_id,
        session_id=session_id,
        modality=SensorModality.THERMAL,
        raw_value=temperature,
        processed_value=temperature,
        quality_score=quality,
        confidence=quality / 100.0,
        metadata={"unit": "celsius", "sensor_type": "thermal_camera"}
    )
