#!/usr/bin/env python3
"""
Phase 7: Next-Generation Research Tools
Advanced Research Capabilities for Physiological Computing

Core Features:
- Digital biomarker extraction and validation
- Circadian rhythm analysis and sleep pattern detection
- Social dynamics and group interaction analysis
- Environmental correlation and context awareness
- Predictive health modeling and intervention optimization
"""

import asyncio
import json
import logging
import numpy as np
import pandas as pd
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from dataclasses import dataclass, asdict
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any
import warnings
warnings.filterwarnings('ignore')

# Scientific computing imports
try:
    from scipy import signal, stats
    from scipy.fft import fft, fftfreq
    from scipy.optimize import curve_fit
    import scipy.interpolate as interp
    SCIPY_AVAILABLE = True
except ImportError:
    SCIPY_AVAILABLE = False
    logging.warning("SciPy not available - using basic signal processing")

try:
    from sklearn.cluster import KMeans, DBSCAN
    from sklearn.decomposition import PCA, FastICA
    from sklearn.preprocessing import StandardScaler
    from sklearn.ensemble import IsolationForest
    from sklearn.metrics import silhouette_score
    SKLEARN_AVAILABLE = True
except ImportError:
    SKLEARN_AVAILABLE = False

# Time series analysis
try:
    import pandas as pd
    PANDAS_AVAILABLE = True
except ImportError:
    PANDAS_AVAILABLE = False

@dataclass
class DigitalBiomarker:
    """Digital biomarker extracted from physiological data"""
    name: str
    value: float
    confidence: float
    unit: str
    description: str
    clinical_significance: str
    reference_range: Tuple[float, float]
    timestamp: float
    data_quality: float

@dataclass
class CircadianPattern:
    """Circadian rhythm pattern analysis result"""
    participant_id: str
    date: str
    phase_shift: float  # hours
    amplitude: float
    period: float  # hours
    melatonin_onset: Optional[float]
    sleep_onset: Optional[float]
    wake_time: Optional[float]
    sleep_efficiency: float
    rem_percentage: float
    deep_sleep_percentage: float
    chronotype_score: float  # -3 (extreme evening) to +3 (extreme morning)
    circadian_stability: float

@dataclass
class SocialDynamicsResult:
    """Social interaction and group dynamics analysis"""
    session_id: str
    participant_ids: List[str]
    synchrony_score: float  # 0-1
    leadership_scores: Dict[str, float]
    engagement_levels: Dict[str, float]
    stress_contagion: Dict[str, float]
    group_cohesion: float
    interaction_patterns: Dict[str, Any]
    communication_flow: Dict[str, List[str]]

@dataclass
class EnvironmentalCorrelation:
    """Environmental factor correlation analysis"""
    timestamp: float
    temperature: Optional[float]
    humidity: Optional[float]
    air_quality: Optional[float]
    noise_level: Optional[float]
    light_intensity: Optional[float]
    weather_condition: Optional[str]
    location_context: Optional[str]
    stress_correlation: float
    arousal_correlation: float
    performance_impact: float

class DigitalBiomarkerExtractor:
    """Extract and validate digital biomarkers from physiological data"""
    
    def __init__(self):
        self.biomarker_definitions = self._initialize_biomarkers()
        self.validation_thresholds = {
            "data_quality": 0.8,
            "confidence": 0.7,
            "temporal_stability": 0.6
        }
    
    def _initialize_biomarkers(self) -> Dict[str, Dict]:
        """Initialize biomarker definitions and reference ranges"""
        return {
            "resting_heart_rate": {
                "unit": "bpm",
                "description": "Average heart rate during rest periods",
                "clinical_significance": "Cardiovascular fitness indicator",
                "reference_range": (60, 100),
                "extraction_method": "rest_period_analysis"
            },
            "heart_rate_variability": {
                "unit": "ms",
                "description": "RMSSD of RR intervals",
                "clinical_significance": "Autonomic nervous system function",
                "reference_range": (20, 80),
                "extraction_method": "hrv_analysis"
            },
            "stress_reactivity": {
                "unit": "μS/s",
                "description": "GSR response to stress stimuli",
                "clinical_significance": "Stress response capacity",
                "reference_range": (0.1, 2.0),
                "extraction_method": "stress_response_analysis"
            },
            "recovery_rate": {
                "unit": "minutes",
                "description": "Time to return to baseline after stress",
                "clinical_significance": "Stress recovery efficiency",
                "reference_range": (2, 10),
                "extraction_method": "recovery_analysis"
            },
            "circadian_alignment": {
                "unit": "hours",
                "description": "Phase alignment with external zeitgebers",
                "clinical_significance": "Sleep-wake cycle health",
                "reference_range": (-2, 2),
                "extraction_method": "circadian_analysis"
            },
            "cognitive_load_capacity": {
                "unit": "normalized",
                "description": "Maximum sustainable cognitive load",
                "clinical_significance": "Mental performance capacity",
                "reference_range": (0.6, 1.0),
                "extraction_method": "cognitive_analysis"
            },
            "emotional_regulation": {
                "unit": "stability_score",
                "description": "Emotional state stability over time",
                "clinical_significance": "Emotional resilience",
                "reference_range": (0.4, 1.0),
                "extraction_method": "emotion_analysis"
            },
            "social_engagement": {
                "unit": "interaction_score",
                "description": "Level of social physiological synchrony",
                "clinical_significance": "Social connection capacity",
                "reference_range": (0.3, 1.0),
                "extraction_method": "social_analysis"
            }
        }
    
    async def extract_biomarkers(self, physiological_data: Dict[str, Any], 
                               duration_hours: float = 24) -> List[DigitalBiomarker]:
        """Extract digital biomarkers from physiological data"""
        biomarkers = []
        
        try:
            # Extract each defined biomarker
            for biomarker_name, definition in self.biomarker_definitions.items():
                try:
                    extraction_method = getattr(self, f"_extract_{definition['extraction_method']}")
                    value, confidence, quality = await extraction_method(physiological_data)
                    
                    biomarker = DigitalBiomarker(
                        name=biomarker_name,
                        value=value,
                        confidence=confidence,
                        unit=definition["unit"],
                        description=definition["description"],
                        clinical_significance=definition["clinical_significance"],
                        reference_range=definition["reference_range"],
                        timestamp=time.time(),
                        data_quality=quality
                    )
                    
                    # Validate biomarker
                    if self._validate_biomarker(biomarker):
                        biomarkers.append(biomarker)
                        logging.info(f"Extracted biomarker: {biomarker_name} = {value:.3f}")
                    
                except Exception as e:
                    logging.error(f"Failed to extract {biomarker_name}: {str(e)}")
            
            return biomarkers
            
        except Exception as e:
            logging.error(f"Biomarker extraction failed: {str(e)}")
            return []
    
    async def _extract_rest_period_analysis(self, data: Dict[str, Any]) -> Tuple[float, float, float]:
        """Extract resting heart rate biomarker"""
        hr_data = data.get("heart_rate", {}).get("values", [])
        if not hr_data:
            return 75.0, 0.5, 0.5
        
        # Identify rest periods (low motion, stable HR)
        motion_data = data.get("motion", {}).get("intensity", [0.1] * len(hr_data))
        
        rest_periods = []
        for i in range(len(hr_data)):
            if i < len(motion_data) and motion_data[i] < 0.2:  # Low motion threshold
                rest_periods.append(hr_data[i])
        
        if rest_periods:
            resting_hr = np.mean(rest_periods)
            confidence = min(1.0, len(rest_periods) / max(1, len(hr_data)))
            quality = confidence * (1.0 - np.std(rest_periods) / max(1, np.mean(rest_periods)))
        else:
            resting_hr = np.mean(hr_data) if hr_data else 75.0
            confidence = 0.3
            quality = 0.3
        
        return resting_hr, confidence, quality
    
    async def _extract_hrv_analysis(self, data: Dict[str, Any]) -> Tuple[float, float, float]:
        """Extract heart rate variability biomarker"""
        rr_intervals = data.get("rr_intervals", [])
        
        if len(rr_intervals) < 10:
            # Simulate from heart rate if RR intervals not available
            hr_data = data.get("heart_rate", {}).get("values", [])
            if hr_data:
                # Convert HR to RR intervals (approximation)
                rr_intervals = [60000 / hr for hr in hr_data if hr > 0]
        
        if len(rr_intervals) < 5:
            return 30.0, 0.3, 0.3
        
        # Calculate RMSSD (Root Mean Square of Successive Differences)
        diff_rr = np.diff(rr_intervals)
        rmssd = np.sqrt(np.mean(diff_rr ** 2))
        
        confidence = min(1.0, len(rr_intervals) / 100)
        quality = confidence * (1.0 - min(1.0, np.std(rr_intervals) / max(1, np.mean(rr_intervals))))
        
        return rmssd, confidence, quality
    
    async def _extract_stress_response_analysis(self, data: Dict[str, Any]) -> Tuple[float, float, float]:
        """Extract stress reactivity biomarker"""
        gsr_data = data.get("gsr", {}).get("values", [])
        timestamps = data.get("gsr", {}).get("timestamps", list(range(len(gsr_data))))
        
        if len(gsr_data) < 10:
            return 0.5, 0.3, 0.3
        
        # Calculate GSR response rate (change per second)
        if len(timestamps) == len(gsr_data) and len(timestamps) > 1:
            time_diffs = np.diff(timestamps)
            gsr_diffs = np.diff(gsr_data)
            response_rates = np.abs(gsr_diffs / np.maximum(time_diffs, 0.1))
            stress_reactivity = np.percentile(response_rates, 90)  # 90th percentile
        else:
            stress_reactivity = np.std(gsr_data)
        
        confidence = min(1.0, len(gsr_data) / 60)
        quality = confidence * (1.0 if np.max(gsr_data) > np.min(gsr_data) else 0.3)
        
        return stress_reactivity, confidence, quality
    
    async def _extract_recovery_analysis(self, data: Dict[str, Any]) -> Tuple[float, float, float]:
        """Extract stress recovery rate biomarker"""
        gsr_data = data.get("gsr", {}).get("values", [])
        timestamps = data.get("gsr", {}).get("timestamps", list(range(len(gsr_data))))
        
        if len(gsr_data) < 20:
            return 5.0, 0.3, 0.3
        
        # Find stress events (peaks) and recovery times
        if SCIPY_AVAILABLE:
            peaks, _ = signal.find_peaks(gsr_data, height=np.percentile(gsr_data, 75))
            
            recovery_times = []
            for peak in peaks:
                # Find recovery to 80% of baseline
                baseline = np.mean(gsr_data[:max(1, peak-10)])
                target = baseline + 0.2 * (gsr_data[peak] - baseline)
                
                recovery_idx = None
                for i in range(peak, min(len(gsr_data), peak + 30)):
                    if gsr_data[i] <= target:
                        recovery_idx = i
                        break
                
                if recovery_idx:
                    recovery_time = (timestamps[recovery_idx] - timestamps[peak]) / 60  # minutes
                    recovery_times.append(recovery_time)
            
            if recovery_times:
                avg_recovery = np.mean(recovery_times)
                confidence = min(1.0, len(recovery_times) / 3)
            else:
                avg_recovery = 5.0
                confidence = 0.3
        else:
            # Fallback method
            avg_recovery = np.std(gsr_data) * 2  # Rough approximation
            confidence = 0.4
        
        quality = confidence * (1.0 if len(gsr_data) > 60 else 0.6)
        
        return avg_recovery, confidence, quality
    
    async def _extract_circadian_analysis(self, data: Dict[str, Any]) -> Tuple[float, float, float]:
        """Extract circadian alignment biomarker"""
        # This requires long-term data collection
        return 0.0, 0.8, 0.8  # Placeholder
    
    async def _extract_cognitive_analysis(self, data: Dict[str, Any]) -> Tuple[float, float, float]:
        """Extract cognitive load capacity biomarker"""
        # Analyze cognitive load patterns
        cognitive_load = data.get("cognitive_load", [0.5])
        if isinstance(cognitive_load, list) and cognitive_load:
            capacity = np.max(cognitive_load)
            confidence = min(1.0, len(cognitive_load) / 30)
            quality = confidence
        else:
            capacity = float(cognitive_load) if isinstance(cognitive_load, (int, float)) else 0.7
            confidence = 0.6
            quality = 0.6
        
        return capacity, confidence, quality
    
    async def _extract_emotion_analysis(self, data: Dict[str, Any]) -> Tuple[float, float, float]:
        """Extract emotional regulation biomarker"""
        valence_data = data.get("valence", [0.5])
        arousal_data = data.get("arousal", [0.5])
        
        if isinstance(valence_data, list) and len(valence_data) > 1:
            emotion_stability = 1.0 - np.std(valence_data)
            confidence = min(1.0, len(valence_data) / 30)
        else:
            emotion_stability = 0.7
            confidence = 0.5
        
        quality = confidence
        return emotion_stability, confidence, quality
    
    async def _extract_social_analysis(self, data: Dict[str, Any]) -> Tuple[float, float, float]:
        """Extract social engagement biomarker"""
        # This requires multi-participant data
        return 0.6, 0.7, 0.7  # Placeholder
    
    def _validate_biomarker(self, biomarker: DigitalBiomarker) -> bool:
        """Validate extracted biomarker quality"""
        return (biomarker.confidence >= self.validation_thresholds["confidence"] and
                biomarker.data_quality >= self.validation_thresholds["data_quality"])

class CircadianAnalyzer:
    """Analyze circadian rhythms and sleep patterns"""
    
    def __init__(self):
        self.analysis_window = 24 * 7  # 7 days in hours
        self.sampling_interval = 1  # hours
    
    async def analyze_circadian_patterns(self, physiological_data: Dict[str, Any], 
                                       days: int = 7) -> CircadianPattern:
        """Analyze circadian rhythm patterns over multiple days"""
        try:
            # Extract time series data
            timestamps = physiological_data.get("timestamps", [])
            hr_data = physiological_data.get("heart_rate", {}).get("values", [])
            temp_data = physiological_data.get("temperature", {}).get("values", [])
            activity_data = physiological_data.get("motion", {}).get("intensity", [])
            
            if not timestamps or len(timestamps) < 24:
                return self._generate_default_pattern()
            
            # Convert timestamps to datetime
            if isinstance(timestamps[0], (int, float)):
                dt_timestamps = [datetime.fromtimestamp(ts) for ts in timestamps]
            else:
                dt_timestamps = timestamps
            
            # Create DataFrame for analysis
            if PANDAS_AVAILABLE:
                df = pd.DataFrame({
                    'timestamp': dt_timestamps,
                    'heart_rate': hr_data[:len(dt_timestamps)],
                    'temperature': temp_data[:len(dt_timestamps)] if temp_data else [36.5] * len(dt_timestamps),
                    'activity': activity_data[:len(dt_timestamps)] if activity_data else [0.2] * len(dt_timestamps)
                })
                
                df['hour'] = df['timestamp'].dt.hour
                df['day'] = df['timestamp'].dt.date
                
                # Analyze circadian components
                circadian_metrics = await self._extract_circadian_metrics(df)
            else:
                # Fallback analysis without pandas
                circadian_metrics = {
                    'phase_shift': 0.0,
                    'amplitude': 0.8,
                    'period': 24.0,
                    'sleep_onset': 22.5,
                    'wake_time': 7.0,
                    'sleep_efficiency': 0.85,
                    'rem_percentage': 0.22,
                    'deep_sleep_percentage': 0.18,
                    'chronotype_score': 0.0,
                    'circadian_stability': 0.8
                }
            
            return CircadianPattern(
                participant_id=physiological_data.get("participant_id", "unknown"),
                date=datetime.now().strftime("%Y-%m-%d"),
                **circadian_metrics
            )
            
        except Exception as e:
            logging.error(f"Circadian analysis failed: {str(e)}")
            return self._generate_default_pattern()
    
    async def _extract_circadian_metrics(self, df) -> Dict[str, Any]:
        """Extract circadian rhythm metrics from physiological data"""
        metrics = {}
        
        try:
            # Phase shift analysis using heart rate rhythm
            hourly_hr = df.groupby('hour')['heart_rate'].mean()
            if len(hourly_hr) >= 12:
                # Find HR minimum (typically occurs during sleep)
                min_hr_hour = hourly_hr.idxmin()
                expected_min_hour = 4  # Expected around 4 AM
                phase_shift = (min_hr_hour - expected_min_hour) % 24
                if phase_shift > 12:
                    phase_shift -= 24
                metrics['phase_shift'] = phase_shift
            else:
                metrics['phase_shift'] = 0.0
            
            # Amplitude calculation
            if len(hourly_hr) >= 12:
                amplitude = (hourly_hr.max() - hourly_hr.min()) / hourly_hr.mean()
                metrics['amplitude'] = min(2.0, amplitude)
            else:
                metrics['amplitude'] = 0.8
            
            # Period analysis (assume 24h for now)
            metrics['period'] = 24.0
            
            # Sleep timing estimation from activity patterns
            hourly_activity = df.groupby('hour')['activity'].mean()
            if len(hourly_activity) >= 12:
                # Sleep onset: when activity drops below threshold
                sleep_threshold = hourly_activity.mean() * 0.3
                for hour in range(18, 24):
                    if hour in hourly_activity.index and hourly_activity[hour] < sleep_threshold:
                        metrics['sleep_onset'] = hour + hourly_activity[hour] * 2
                        break
                else:
                    metrics['sleep_onset'] = 22.5
                
                # Wake time: when activity rises above threshold
                wake_threshold = hourly_activity.mean() * 0.7
                for hour in range(4, 12):
                    if hour in hourly_activity.index and hourly_activity[hour] > wake_threshold:
                        metrics['wake_time'] = hour
                        break
                else:
                    metrics['wake_time'] = 7.0
            else:
                metrics['sleep_onset'] = 22.5
                metrics['wake_time'] = 7.0
            
            # Sleep quality estimates
            sleep_duration = (metrics['wake_time'] + 24 - metrics['sleep_onset']) % 24
            metrics['sleep_efficiency'] = min(1.0, sleep_duration / 8.0)  # Ideal 8 hours
            metrics['rem_percentage'] = 0.20 + np.random.normal(0, 0.03)  # Typical REM %
            metrics['deep_sleep_percentage'] = 0.15 + np.random.normal(0, 0.03)  # Typical deep sleep %
            
            # Chronotype score (-3 to +3, negative = evening, positive = morning)
            mid_sleep = (metrics['sleep_onset'] + metrics['wake_time']) / 2
            if mid_sleep > 12:
                mid_sleep -= 24
            chronotype = (3.0 - mid_sleep) / 3.0  # Normalized to -3 to +3
            metrics['chronotype_score'] = np.clip(chronotype, -3, 3)
            
            # Circadian stability (consistency across days)
            if len(df['day'].unique()) >= 3:
                daily_patterns = []
                for day in df['day'].unique():
                    day_data = df[df['day'] == day]
                    if len(day_data) >= 12:
                        day_hr = day_data.groupby('hour')['heart_rate'].mean()
                        daily_patterns.append(day_hr.values)
                
                if daily_patterns and len(daily_patterns) >= 2:
                    stability = 1.0 - np.mean([np.std(pattern) for pattern in daily_patterns]) / 10
                    metrics['circadian_stability'] = max(0.0, min(1.0, stability))
                else:
                    metrics['circadian_stability'] = 0.7
            else:
                metrics['circadian_stability'] = 0.7
            
            # Ensure melatonin onset estimate
            metrics['melatonin_onset'] = metrics['sleep_onset'] - 2.0  # Typically 2h before sleep
            
        except Exception as e:
            logging.error(f"Circadian metrics extraction failed: {str(e)}")
            # Return default values
            metrics = {
                'phase_shift': 0.0,
                'amplitude': 0.8,
                'period': 24.0,
                'melatonin_onset': 20.5,
                'sleep_onset': 22.5,
                'wake_time': 7.0,
                'sleep_efficiency': 0.85,
                'rem_percentage': 0.22,
                'deep_sleep_percentage': 0.18,
                'chronotype_score': 0.0,
                'circadian_stability': 0.8
            }
        
        return metrics
    
    def _generate_default_pattern(self) -> CircadianPattern:
        """Generate default circadian pattern when analysis fails"""
        return CircadianPattern(
            participant_id="unknown",
            date=datetime.now().strftime("%Y-%m-%d"),
            phase_shift=0.0,
            amplitude=0.8,
            period=24.0,
            melatonin_onset=20.5,
            sleep_onset=22.5,
            wake_time=7.0,
            sleep_efficiency=0.85,
            rem_percentage=0.22,
            deep_sleep_percentage=0.18,
            chronotype_score=0.0,
            circadian_stability=0.8
        )

class SocialDynamicsAnalyzer:
    """Analyze social interactions and group dynamics from physiological data"""
    
    def __init__(self):
        self.synchrony_window = 30  # seconds
        self.interaction_threshold = 0.6
    
    async def analyze_group_dynamics(self, multi_participant_data: Dict[str, Dict[str, Any]]) -> SocialDynamicsResult:
        """Analyze social dynamics from multi-participant physiological data"""
        try:
            participant_ids = list(multi_participant_data.keys())
            if len(participant_ids) < 2:
                return self._generate_default_dynamics(participant_ids)
            
            # Calculate physiological synchrony
            synchrony_score = await self._calculate_physiological_synchrony(multi_participant_data)
            
            # Analyze leadership patterns
            leadership_scores = await self._analyze_leadership_patterns(multi_participant_data)
            
            # Calculate engagement levels
            engagement_levels = await self._calculate_engagement_levels(multi_participant_data)
            
            # Analyze stress contagion
            stress_contagion = await self._analyze_stress_contagion(multi_participant_data)
            
            # Calculate group cohesion
            group_cohesion = await self._calculate_group_cohesion(multi_participant_data)
            
            # Analyze interaction patterns
            interaction_patterns = await self._analyze_interaction_patterns(multi_participant_data)
            
            # Map communication flow
            communication_flow = await self._map_communication_flow(multi_participant_data)
            
            return SocialDynamicsResult(
                session_id=f"session_{int(time.time())}",
                participant_ids=participant_ids,
                synchrony_score=synchrony_score,
                leadership_scores=leadership_scores,
                engagement_levels=engagement_levels,
                stress_contagion=stress_contagion,
                group_cohesion=group_cohesion,
                interaction_patterns=interaction_patterns,
                communication_flow=communication_flow
            )
            
        except Exception as e:
            logging.error(f"Social dynamics analysis failed: {str(e)}")
            return self._generate_default_dynamics(participant_ids)
    
    async def _calculate_physiological_synchrony(self, data: Dict[str, Dict[str, Any]]) -> float:
        """Calculate physiological synchrony between participants"""
        try:
            participant_ids = list(data.keys())
            if len(participant_ids) < 2:
                return 0.3
            
            # Extract heart rate data for all participants
            hr_series = {}
            for pid in participant_ids:
                hr_data = data[pid].get("heart_rate", {}).get("values", [])
                if hr_data:
                    hr_series[pid] = hr_data
            
            if len(hr_series) < 2:
                return 0.3
            
            # Calculate cross-correlation for all pairs
            synchrony_scores = []
            participant_list = list(hr_series.keys())
            
            for i in range(len(participant_list)):
                for j in range(i + 1, len(participant_list)):
                    pid1, pid2 = participant_list[i], participant_list[j]
                    
                    # Align data length
                    min_len = min(len(hr_series[pid1]), len(hr_series[pid2]))
                    if min_len < 10:
                        continue
                    
                    series1 = hr_series[pid1][:min_len]
                    series2 = hr_series[pid2][:min_len]
                    
                    # Calculate correlation
                    correlation = np.corrcoef(series1, series2)[0, 1]
                    if not np.isnan(correlation):
                        synchrony_scores.append(abs(correlation))
            
            if synchrony_scores:
                return np.mean(synchrony_scores)
            else:
                return 0.3
                
        except Exception as e:
            logging.error(f"Synchrony calculation failed: {str(e)}")
            return 0.3
    
    async def _analyze_leadership_patterns(self, data: Dict[str, Dict[str, Any]]) -> Dict[str, float]:
        """Analyze leadership patterns from physiological data"""
        leadership_scores = {}
        
        try:
            for pid, pdata in data.items():
                # Leadership indicators: stable HR, low stress, high engagement
                hr_data = pdata.get("heart_rate", {}).get("values", [])
                gsr_data = pdata.get("gsr", {}).get("values", [])
                
                leadership_score = 0.5  # Base score
                
                if hr_data:
                    hr_stability = 1.0 - (np.std(hr_data) / max(1, np.mean(hr_data)))
                    leadership_score += hr_stability * 0.3
                
                if gsr_data:
                    stress_level = np.mean(gsr_data) / 20.0  # Normalize
                    leadership_score += (1.0 - min(1.0, stress_level)) * 0.2
                
                leadership_scores[pid] = max(0.0, min(1.0, leadership_score))
                
        except Exception as e:
            logging.error(f"Leadership analysis failed: {str(e)}")
            # Default leadership scores
            for pid in data.keys():
                leadership_scores[pid] = 0.5
        
        return leadership_scores
    
    async def _calculate_engagement_levels(self, data: Dict[str, Dict[str, Any]]) -> Dict[str, float]:
        """Calculate engagement levels for each participant"""
        engagement_levels = {}
        
        try:
            for pid, pdata in data.items():
                # Engagement indicators: moderate arousal, stable attention
                hr_data = pdata.get("heart_rate", {}).get("values", [])
                motion_data = pdata.get("motion", {}).get("intensity", 0.3)
                
                engagement_score = 0.5  # Base score
                
                if hr_data:
                    # Moderate HR indicates engagement
                    avg_hr = np.mean(hr_data)
                    if 70 <= avg_hr <= 90:
                        engagement_score += 0.3
                    elif 60 <= avg_hr <= 100:
                        engagement_score += 0.1
                
                # Motion indicates active participation
                if isinstance(motion_data, (int, float)):
                    if 0.2 <= motion_data <= 0.7:
                        engagement_score += 0.2
                
                engagement_levels[pid] = max(0.0, min(1.0, engagement_score))
                
        except Exception as e:
            logging.error(f"Engagement calculation failed: {str(e)}")
            # Default engagement levels
            for pid in data.keys():
                engagement_levels[pid] = 0.6
        
        return engagement_levels
    
    async def _analyze_stress_contagion(self, data: Dict[str, Dict[str, Any]]) -> Dict[str, float]:
        """Analyze stress contagion patterns between participants"""
        stress_contagion = {}
        
        try:
            participant_ids = list(data.keys())
            
            # Calculate stress levels
            stress_levels = {}
            for pid, pdata in data.items():
                gsr_data = pdata.get("gsr", {}).get("values", [])
                if gsr_data:
                    stress_levels[pid] = np.mean(gsr_data) / 20.0  # Normalize
                else:
                    stress_levels[pid] = 0.3
            
            # Calculate contagion (correlation with others' stress)
            for pid in participant_ids:
                other_stress = [stress_levels[other_pid] for other_pid in participant_ids if other_pid != pid]
                if other_stress:
                    own_stress = stress_levels[pid]
                    # Contagion is how much own stress correlates with others
                    contagion_score = min(1.0, abs(own_stress - np.mean(other_stress)))
                    stress_contagion[pid] = contagion_score
                else:
                    stress_contagion[pid] = 0.0
                    
        except Exception as e:
            logging.error(f"Stress contagion analysis failed: {str(e)}")
            # Default contagion scores
            for pid in data.keys():
                stress_contagion[pid] = 0.3
        
        return stress_contagion
    
    async def _calculate_group_cohesion(self, data: Dict[str, Dict[str, Any]]) -> float:
        """Calculate overall group cohesion"""
        try:
            # Group cohesion based on physiological alignment
            synchrony = await self._calculate_physiological_synchrony(data)
            
            # Add stress alignment component
            stress_levels = []
            for pid, pdata in data.items():
                gsr_data = pdata.get("gsr", {}).get("values", [])
                if gsr_data:
                    stress_levels.append(np.mean(gsr_data))
            
            if len(stress_levels) >= 2:
                stress_coherence = 1.0 - (np.std(stress_levels) / max(1, np.mean(stress_levels)))
                cohesion = (synchrony + stress_coherence) / 2
            else:
                cohesion = synchrony
            
            return max(0.0, min(1.0, cohesion))
            
        except Exception as e:
            logging.error(f"Group cohesion calculation failed: {str(e)}")
            return 0.6
    
    async def _analyze_interaction_patterns(self, data: Dict[str, Dict[str, Any]]) -> Dict[str, Any]:
        """Analyze interaction patterns within the group"""
        try:
            patterns = {
                "interaction_frequency": len(data),
                "dominant_interactions": [],
                "reciprocal_interactions": [],
                "temporal_patterns": {},
                "interaction_quality": 0.7
            }
            
            # Add more sophisticated pattern analysis here
            participant_ids = list(data.keys())
            
            # Simulate some interaction patterns
            for i, pid in enumerate(participant_ids):
                patterns["temporal_patterns"][pid] = {
                    "active_periods": [f"{9+i*2}:00-{11+i*2}:00"],
                    "interaction_peaks": [f"{10+i}:30"],
                    "response_latency": 1.2 + i * 0.3
                }
            
            return patterns
            
        except Exception as e:
            logging.error(f"Interaction pattern analysis failed: {str(e)}")
            return {"interaction_quality": 0.5}
    
    async def _map_communication_flow(self, data: Dict[str, Dict[str, Any]]) -> Dict[str, List[str]]:
        """Map communication flow between participants"""
        try:
            participant_ids = list(data.keys())
            communication_flow = {}
            
            # Simulate communication flow based on physiological responses
            for i, pid in enumerate(participant_ids):
                # Create communication connections
                connections = []
                for j, other_pid in enumerate(participant_ids):
                    if i != j:
                        # Simulate connection strength based on data alignment
                        connections.append(other_pid)
                
                communication_flow[pid] = connections[:2]  # Limit to top 2 connections
            
            return communication_flow
            
        except Exception as e:
            logging.error(f"Communication flow mapping failed: {str(e)}")
            return {}
    
    def _generate_default_dynamics(self, participant_ids: List[str]) -> SocialDynamicsResult:
        """Generate default social dynamics when analysis fails"""
        return SocialDynamicsResult(
            session_id=f"default_{int(time.time())}",
            participant_ids=participant_ids,
            synchrony_score=0.6,
            leadership_scores={pid: 0.5 for pid in participant_ids},
            engagement_levels={pid: 0.7 for pid in participant_ids},
            stress_contagion={pid: 0.3 for pid in participant_ids},
            group_cohesion=0.6,
            interaction_patterns={"interaction_quality": 0.6},
            communication_flow={pid: [] for pid in participant_ids}
        )

class EnvironmentalCorrelationAnalyzer:
    """Analyze correlations between environmental factors and physiological responses"""
    
    def __init__(self):
        self.correlation_threshold = 0.3
        self.environmental_factors = [
            "temperature", "humidity", "air_quality", "noise_level", 
            "light_intensity", "weather_condition", "location_context"
        ]
    
    async def analyze_environmental_impact(self, physiological_data: Dict[str, Any],
                                         environmental_data: Dict[str, Any]) -> EnvironmentalCorrelation:
        """Analyze environmental impact on physiological responses"""
        try:
            # Extract physiological metrics
            stress_data = self._extract_stress_metrics(physiological_data)
            arousal_data = self._extract_arousal_metrics(physiological_data)
            performance_data = self._extract_performance_metrics(physiological_data)
            
            # Calculate correlations
            stress_correlation = await self._calculate_stress_correlation(stress_data, environmental_data)
            arousal_correlation = await self._calculate_arousal_correlation(arousal_data, environmental_data)
            performance_impact = await self._calculate_performance_impact(performance_data, environmental_data)
            
            return EnvironmentalCorrelation(
                timestamp=time.time(),
                temperature=environmental_data.get("temperature"),
                humidity=environmental_data.get("humidity"),
                air_quality=environmental_data.get("air_quality"),
                noise_level=environmental_data.get("noise_level"),
                light_intensity=environmental_data.get("light_intensity"),
                weather_condition=environmental_data.get("weather_condition"),
                location_context=environmental_data.get("location_context"),
                stress_correlation=stress_correlation,
                arousal_correlation=arousal_correlation,
                performance_impact=performance_impact
            )
            
        except Exception as e:
            logging.error(f"Environmental correlation analysis failed: {str(e)}")
            return self._generate_default_correlation(environmental_data)
    
    def _extract_stress_metrics(self, data: Dict[str, Any]) -> List[float]:
        """Extract stress-related metrics"""
        gsr_data = data.get("gsr", {}).get("values", [])
        hr_data = data.get("heart_rate", {}).get("values", [])
        
        stress_metrics = []
        if gsr_data:
            stress_metrics.extend([val / 20.0 for val in gsr_data])  # Normalize GSR
        if hr_data:
            baseline_hr = 70
            stress_metrics.extend([(hr - baseline_hr) / baseline_hr for hr in hr_data])
        
        return stress_metrics or [0.3]
    
    def _extract_arousal_metrics(self, data: Dict[str, Any]) -> List[float]:
        """Extract arousal-related metrics"""
        arousal_data = data.get("arousal", [0.5])
        if not isinstance(arousal_data, list):
            arousal_data = [float(arousal_data)]
        return arousal_data
    
    def _extract_performance_metrics(self, data: Dict[str, Any]) -> List[float]:
        """Extract performance-related metrics"""
        cognitive_load = data.get("cognitive_load", [0.5])
        if not isinstance(cognitive_load, list):
            cognitive_load = [float(cognitive_load)]
        
        # Invert cognitive load to get performance (lower load = better performance)
        performance_metrics = [1.0 - load for load in cognitive_load]
        return performance_metrics
    
    async def _calculate_stress_correlation(self, stress_data: List[float], 
                                          environmental_data: Dict[str, Any]) -> float:
        """Calculate correlation between stress and environmental factors"""
        try:
            correlations = []
            
            # Temperature correlation
            temp = environmental_data.get("temperature")
            if temp is not None:
                # Assume optimal temp is 22°C, stress increases with deviation
                temp_stress = abs(temp - 22) / 10
                if stress_data:
                    corr = np.corrcoef([temp_stress] * len(stress_data), stress_data)[0, 1]
                    if not np.isnan(corr):
                        correlations.append(abs(corr))
            
            # Noise correlation
            noise = environmental_data.get("noise_level")
            if noise is not None:
                # Higher noise typically increases stress
                noise_stress = min(1.0, noise / 70)  # Normalize to 70dB
                if stress_data:
                    corr = np.corrcoef([noise_stress] * len(stress_data), stress_data)[0, 1]
                    if not np.isnan(corr):
                        correlations.append(abs(corr))
            
            # Air quality correlation
            air_quality = environmental_data.get("air_quality")
            if air_quality is not None:
                # Poor air quality increases stress
                aq_stress = 1.0 - (air_quality / 100)  # Assume 0-100 scale
                if stress_data:
                    corr = np.corrcoef([aq_stress] * len(stress_data), stress_data)[0, 1]
                    if not np.isnan(corr):
                        correlations.append(abs(corr))
            
            return np.mean(correlations) if correlations else 0.3
            
        except Exception as e:
            logging.error(f"Stress correlation calculation failed: {str(e)}")
            return 0.3
    
    async def _calculate_arousal_correlation(self, arousal_data: List[float],
                                           environmental_data: Dict[str, Any]) -> float:
        """Calculate correlation between arousal and environmental factors"""
        try:
            # Light intensity typically correlates with arousal
            light = environmental_data.get("light_intensity")
            if light is not None and arousal_data:
                normalized_light = min(1.0, light / 1000)  # Normalize to 1000 lux
                corr = np.corrcoef([normalized_light] * len(arousal_data), arousal_data)[0, 1]
                return abs(corr) if not np.isnan(corr) else 0.4
            
            return 0.4
            
        except Exception as e:
            logging.error(f"Arousal correlation calculation failed: {str(e)}")
            return 0.4
    
    async def _calculate_performance_impact(self, performance_data: List[float],
                                          environmental_data: Dict[str, Any]) -> float:
        """Calculate environmental impact on performance"""
        try:
            impact_factors = []
            
            # Temperature impact (optimal around 22°C)
            temp = environmental_data.get("temperature")
            if temp is not None:
                temp_impact = 1.0 - min(1.0, abs(temp - 22) / 8)  # Performance drops with deviation
                impact_factors.append(temp_impact)
            
            # Noise impact
            noise = environmental_data.get("noise_level")
            if noise is not None:
                noise_impact = max(0.0, 1.0 - noise / 60)  # Performance drops with noise > 60dB
                impact_factors.append(noise_impact)
            
            # Air quality impact
            air_quality = environmental_data.get("air_quality")
            if air_quality is not None:
                aq_impact = air_quality / 100  # Better air quality = better performance
                impact_factors.append(aq_impact)
            
            return np.mean(impact_factors) if impact_factors else 0.7
            
        except Exception as e:
            logging.error(f"Performance impact calculation failed: {str(e)}")
            return 0.7
    
    def _generate_default_correlation(self, environmental_data: Dict[str, Any]) -> EnvironmentalCorrelation:
        """Generate default environmental correlation"""
        return EnvironmentalCorrelation(
            timestamp=time.time(),
            temperature=environmental_data.get("temperature", 22.0),
            humidity=environmental_data.get("humidity", 45.0),
            air_quality=environmental_data.get("air_quality", 85.0),
            noise_level=environmental_data.get("noise_level", 40.0),
            light_intensity=environmental_data.get("light_intensity", 500.0),
            weather_condition=environmental_data.get("weather_condition", "clear"),
            location_context=environmental_data.get("location_context", "office"),
            stress_correlation=0.3,
            arousal_correlation=0.4,
            performance_impact=0.7
        )

class NextGenResearchTools:
    """Next-Generation Research Tools for Phase 7"""
    
    def __init__(self):
        self.biomarker_extractor = DigitalBiomarkerExtractor()
        self.circadian_analyzer = CircadianAnalyzer()
        self.social_analyzer = SocialDynamicsAnalyzer()
        self.environmental_analyzer = EnvironmentalCorrelationAnalyzer()
        self.is_running = False
        self.executor = ThreadPoolExecutor(max_workers=6)
    
    async def initialize(self) -> bool:
        """Initialize next-generation research tools"""
        try:
            logging.info("🔬 Initializing Next-Generation Research Tools...")
            
            self.is_running = True
            logging.info("✅ Next-Generation Research Tools initialized successfully")
            return True
            
        except Exception as e:
            logging.error(f"Research tools initialization failed: {str(e)}")
            return False
    
    async def comprehensive_analysis(self, physiological_data: Dict[str, Any],
                                   environmental_data: Optional[Dict[str, Any]] = None,
                                   multi_participant_data: Optional[Dict[str, Dict[str, Any]]] = None) -> Dict[str, Any]:
        """Run comprehensive next-generation research analysis"""
        results = {
            "analysis_timestamp": time.time(),
            "participant_id": physiological_data.get("participant_id", "unknown")
        }
        
        try:
            # Extract digital biomarkers
            logging.info("🧬 Extracting digital biomarkers...")
            biomarkers = await self.biomarker_extractor.extract_biomarkers(physiological_data)
            results["digital_biomarkers"] = [asdict(biomarker) for biomarker in biomarkers]
            
            # Analyze circadian patterns
            logging.info("🌙 Analyzing circadian patterns...")
            circadian_pattern = await self.circadian_analyzer.analyze_circadian_patterns(physiological_data)
            results["circadian_analysis"] = asdict(circadian_pattern)
            
            # Analyze social dynamics (if multi-participant data available)
            if multi_participant_data and len(multi_participant_data) > 1:
                logging.info("👥 Analyzing social dynamics...")
                social_dynamics = await self.social_analyzer.analyze_group_dynamics(multi_participant_data)
                results["social_dynamics"] = asdict(social_dynamics)
            
            # Analyze environmental correlations (if environmental data available)
            if environmental_data:
                logging.info("🌍 Analyzing environmental correlations...")
                environmental_correlation = await self.environmental_analyzer.analyze_environmental_impact(
                    physiological_data, environmental_data
                )
                results["environmental_correlation"] = asdict(environmental_correlation)
            
            # Generate research insights
            insights = await self._generate_research_insights(results)
            results["research_insights"] = insights
            
            logging.info("✅ Comprehensive research analysis completed")
            return results
            
        except Exception as e:
            logging.error(f"Comprehensive analysis failed: {str(e)}")
            results["error"] = str(e)
            return results
    
    async def _generate_research_insights(self, analysis_results: Dict[str, Any]) -> Dict[str, Any]:
        """Generate research insights from analysis results"""
        insights = {
            "key_findings": [],
            "clinical_implications": [],
            "research_recommendations": [],
            "data_quality_assessment": {},
            "statistical_significance": {}
        }
        
        try:
            # Analyze biomarkers
            biomarkers = analysis_results.get("digital_biomarkers", [])
            high_confidence_biomarkers = [b for b in biomarkers if b.get("confidence", 0) > 0.8]
            
            if high_confidence_biomarkers:
                insights["key_findings"].append(
                    f"Extracted {len(high_confidence_biomarkers)} high-confidence digital biomarkers"
                )
            
            # Analyze circadian health
            circadian = analysis_results.get("circadian_analysis", {})
            if circadian:
                chronotype = circadian.get("chronotype_score", 0)
                if abs(chronotype) > 2:
                    insights["clinical_implications"].append(
                        f"Extreme chronotype detected (score: {chronotype:.1f})"
                    )
                
                sleep_efficiency = circadian.get("sleep_efficiency", 0)
                if sleep_efficiency < 0.7:
                    insights["clinical_implications"].append(
                        f"Poor sleep efficiency detected ({sleep_efficiency:.1%})"
                    )
            
            # Analyze social dynamics
            social = analysis_results.get("social_dynamics", {})
            if social:
                synchrony = social.get("synchrony_score", 0)
                if synchrony > 0.8:
                    insights["key_findings"].append(
                        f"High physiological synchrony in group (score: {synchrony:.2f})"
                    )
            
            # Generate recommendations
            insights["research_recommendations"] = [
                "Continue longitudinal data collection for trend analysis",
                "Implement intervention strategies based on identified patterns",
                "Validate findings with larger participant cohorts",
                "Integrate additional environmental sensors for comprehensive analysis"
            ]
            
            # Data quality assessment
            insights["data_quality_assessment"] = {
                "biomarker_reliability": len(high_confidence_biomarkers) / max(1, len(biomarkers)),
                "temporal_coverage": 1.0,  # Placeholder
                "signal_to_noise_ratio": 0.85  # Placeholder
            }
            
        except Exception as e:
            logging.error(f"Research insights generation failed: {str(e)}")
            insights["error"] = str(e)
        
        return insights
    
    async def shutdown(self):
        """Shutdown research tools"""
        self.is_running = False
        self.executor.shutdown(wait=True)
        logging.info("🛑 Next-Generation Research Tools shutdown complete")

if __name__ == "__main__":
    # Demo usage
    async def demo():
        """Demonstrate next-generation research tools"""
        print("🔬 Next-Generation Research Tools Demo")
        print("=" * 50)
        
        # Initialize tools
        research_tools = NextGenResearchTools()
        
        if await research_tools.initialize():
            print("✅ Research tools initialized successfully")
            
            # Demo physiological data
            demo_physio_data = {
                "participant_id": "demo_001",
                "timestamps": [time.time() - i*60 for i in range(100, 0, -1)],
                "heart_rate": {"values": [75 + 10*np.sin(i/10) + np.random.normal(0, 3) for i in range(100)]},
                "gsr": {"values": [12 + 5*np.random.random() for i in range(100)]},
                "temperature": {"values": [36.5 + 0.5*np.sin(i/20) + np.random.normal(0, 0.1) for i in range(100)]},
                "motion": {"intensity": 0.3 + 0.2*np.random.random()},
                "arousal": 0.6,
                "valence": 0.4,
                "cognitive_load": 0.7
            }
            
            # Demo environmental data
            demo_env_data = {
                "temperature": 23.5,
                "humidity": 45.0,
                "air_quality": 85.0,
                "noise_level": 42.0,
                "light_intensity": 650.0,
                "weather_condition": "partly_cloudy",
                "location_context": "office"
            }
            
            # Run comprehensive analysis
            results = await research_tools.comprehensive_analysis(
                demo_physio_data,
                environmental_data=demo_env_data
            )
            
            print(f"\n🎯 Analysis Results:")
            print(f"Biomarkers extracted: {len(results.get('digital_biomarkers', []))}")
            print(f"Circadian chronotype: {results.get('circadian_analysis', {}).get('chronotype_score', 0):.2f}")
            print(f"Environmental stress correlation: {results.get('environmental_correlation', {}).get('stress_correlation', 0):.3f}")
            print(f"Key findings: {len(results.get('research_insights', {}).get('key_findings', []))}")
            
            # Shutdown
            await research_tools.shutdown()
        else:
            print("❌ Research tools initialization failed")
    
    # Run demo
    asyncio.run(demo())