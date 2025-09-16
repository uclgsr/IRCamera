#!/usr/bin/env python3
"""
Multi-Modal Data Stream Synchronization Script

This script demonstrates how to align timestamps across different sensor modalities
using the session metadata and timing information recorded by the IRCamera system.

Usage:
    python sync_data_streams.py <session_directory>

The script expects to find the following files in the session directory:
- session_metadata.json: Session timing and metadata
- thermal_stats_*.csv: Thermal camera data with synchronized timestamps
- gsr_data_*.csv: GSR sensor data with synchronized timestamps
- rgb_video_*.mp4: RGB video file (timing derived from metadata)

Author: IRCamera Synchronization System
"""

import json
import pandas as pd
import numpy as np
import argparse
import os
from pathlib import Path
from typing import Dict, List, Optional, Tuple
from datetime import datetime
import glob


class MultiModalSynchronizer:
    """
    Synchronizes data streams from multiple sensor modalities using session metadata.
    """
    
    def __init__(self, session_directory: str):
        self.session_dir = Path(session_directory)
        self.session_metadata = None
        self.thermal_data = None
        self.gsr_data = None
        self.sync_events = None
        
        # Load session metadata
        self._load_session_metadata()
    
    def _load_session_metadata(self):
        """Load session metadata from JSON file."""
        metadata_file = self.session_dir / "session_metadata.json"
        
        if not metadata_file.exists():
            raise FileNotFoundError(f"Session metadata not found: {metadata_file}")
        
        with open(metadata_file, 'r') as f:
            self.session_metadata = json.load(f)
        
        print(f"Loaded session: {self.session_metadata['sessionId']}")
        print(f"Session start: {self.session_metadata['sessionStartIso']}")
        print(f"Recording duration: {self.session_metadata.get('recordingDurationMs', 'N/A')}ms")
    
    def load_thermal_data(self) -> pd.DataFrame:
        """Load and parse thermal camera data."""
        thermal_files = glob.glob(str(self.session_dir / "thermal_stats_*.csv"))
        
        if not thermal_files:
            print("Warning: No thermal data files found")
            return pd.DataFrame()
        
        thermal_file = thermal_files[0]  # Use first file found
        print(f"Loading thermal data from: {thermal_file}")
        
        # Read CSV, skipping comment lines
        df = pd.read_csv(thermal_file, comment='#')
        
        # Ensure we have the expected columns
        expected_cols = ['timestamp_wall_ms', 'timestamp_relative_ms', 'timestamp_monotonic_ns', 
                        'frame_sequence', 'min_temp_c', 'avg_temp_c', 'max_temp_c', 'pixel_count']
        
        if all(col in df.columns for col in expected_cols):
            print(f"Loaded {len(df)} thermal frames with synchronized timing")
        else:
            print("Warning: Thermal data may be in legacy format without full synchronization")
        
        # Convert timestamps to datetime for easier handling
        if 'timestamp_wall_ms' in df.columns:
            df['datetime'] = pd.to_datetime(df['timestamp_wall_ms'], unit='ms')
        
        self.thermal_data = df
        return df
    
    def load_gsr_data(self) -> pd.DataFrame:
        """Load and parse GSR sensor data."""
        gsr_files = glob.glob(str(self.session_dir / "gsr_data_*.csv"))
        
        if not gsr_files:
            print("Warning: No GSR data files found")
            return pd.DataFrame()
        
        gsr_file = gsr_files[0]  # Use first file found
        print(f"Loading GSR data from: {gsr_file}")
        
        # Read CSV, skipping comment lines
        df = pd.read_csv(gsr_file, comment='#')
        
        # Ensure we have the expected columns
        expected_cols = ['timestamp_wall_ms', 'timestamp_relative_ms', 'timestamp_monotonic_ns',
                        'gsr_microsiemens', 'gsr_raw_12bit', 'ppg_raw', 'quality_score', 'connection_rssi']
        
        if all(col in df.columns for col in expected_cols):
            print(f"Loaded {len(df)} GSR samples with synchronized timing")
        else:
            print("Warning: GSR data may be in legacy format without full synchronization")
        
        # Convert timestamps to datetime
        if 'timestamp_wall_ms' in df.columns:
            df['datetime'] = pd.to_datetime(df['timestamp_wall_ms'], unit='ms')
        
        self.gsr_data = df
        return df
    
    def extract_sync_events(self) -> pd.DataFrame:
        """Extract synchronization events from session metadata."""
        if not self.session_metadata or 'syncEvents' not in self.session_metadata:
            print("Warning: No sync events found in session metadata")
            return pd.DataFrame()
        
        sync_events = []
        for event in self.session_metadata['syncEvents']:
            sync_events.append({
                'event_type': event['eventType'],
                'timestamp_ms': event['timestampMs'],
                'relative_offset_ms': event['monotonicOffsetNs'] / 1_000_000,
                'datetime': pd.to_datetime(event['timestampMs'], unit='ms'),
                'metadata': event.get('metadata', {})
            })
        
        df = pd.DataFrame(sync_events)
        print(f"Found {len(df)} synchronization events")
        
        self.sync_events = df
        return df
    
    def align_data_streams(self, window_ms: int = 100) -> Dict[str, pd.DataFrame]:
        """
        Align data streams using relative timestamps from session start.
        
        Args:
            window_ms: Time window in milliseconds for alignment tolerance
            
        Returns:
            Dictionary containing aligned data frames
        """
        aligned_data = {}
        
        # Use relative timestamps as the common time base
        if self.thermal_data is not None and not self.thermal_data.empty:
            if 'timestamp_relative_ms' in self.thermal_data.columns:
                thermal_aligned = self.thermal_data.copy()
                thermal_aligned['common_time_ms'] = thermal_aligned['timestamp_relative_ms']
                aligned_data['thermal'] = thermal_aligned
            else:
                print("Warning: Thermal data lacks relative timestamps")
        
        if self.gsr_data is not None and not self.gsr_data.empty:
            if 'timestamp_relative_ms' in self.gsr_data.columns:
                gsr_aligned = self.gsr_data.copy()
                gsr_aligned['common_time_ms'] = gsr_aligned['timestamp_relative_ms']
                aligned_data['gsr'] = gsr_aligned
            else:
                print("Warning: GSR data lacks relative timestamps")
        
        return aligned_data
    
    def find_simultaneous_events(self, window_ms: int = 50) -> List[Dict]:
        """
        Find data points that occur simultaneously across modalities.
        
        Args:
            window_ms: Time window for considering events simultaneous
            
        Returns:
            List of simultaneous event dictionaries
        """
        aligned_data = self.align_data_streams()
        simultaneous_events = []
        
        if 'thermal' not in aligned_data or 'gsr' not in aligned_data:
            print("Warning: Need both thermal and GSR data for simultaneous event detection")
            return simultaneous_events
        
        thermal_df = aligned_data['thermal']
        gsr_df = aligned_data['gsr']
        
        # Find overlapping time ranges
        thermal_times = thermal_df['common_time_ms'].values
        gsr_times = gsr_df['common_time_ms'].values
        
        for thermal_time in thermal_times[::10]:  # Sample every 10th thermal frame to reduce computation
            # Find GSR samples within window
            gsr_matches = gsr_df[
                (gsr_df['common_time_ms'] >= thermal_time - window_ms) &
                (gsr_df['common_time_ms'] <= thermal_time + window_ms)
            ]
            
            if not gsr_matches.empty:
                thermal_match = thermal_df[thermal_df['common_time_ms'] == thermal_time].iloc[0]
                gsr_match = gsr_matches.iloc[0]  # Take closest match
                
                simultaneous_events.append({
                    'common_time_ms': thermal_time,
                    'thermal_frame': thermal_match['frame_sequence'],
                    'thermal_avg_temp': thermal_match['avg_temp_c'],
                    'gsr_value': gsr_match['gsr_microsiemens'],
                    'gsr_raw': gsr_match['gsr_raw_12bit'],
                    'time_diff_ms': abs(gsr_match['common_time_ms'] - thermal_time)
                })
        
        print(f"Found {len(simultaneous_events)} simultaneous events within {window_ms}ms window")
        return simultaneous_events
    
    def generate_sync_report(self) -> str:
        """Generate a synchronization quality report."""
        report = []
        report.append("=== Multi-Modal Data Synchronization Report ===")
        report.append(f"Session ID: {self.session_metadata['sessionId']}")
        report.append(f"Session Start: {self.session_metadata['sessionStartIso']}")
        report.append(f"Recording Duration: {self.session_metadata.get('recordingDurationMs', 'N/A')}ms")
        report.append("")
        
        # Data stream summary
        if self.thermal_data is not None and not self.thermal_data.empty:
            thermal_duration = self.thermal_data['timestamp_relative_ms'].max() - self.thermal_data['timestamp_relative_ms'].min()
            report.append(f"Thermal Data: {len(self.thermal_data)} frames over {thermal_duration:.1f}ms")
        
        if self.gsr_data is not None and not self.gsr_data.empty:
            gsr_duration = self.gsr_data['timestamp_relative_ms'].max() - self.gsr_data['timestamp_relative_ms'].min()
            report.append(f"GSR Data: {len(self.gsr_data)} samples over {gsr_duration:.1f}ms")
        
        # Sync events
        if self.sync_events is not None and not self.sync_events.empty:
            report.append(f"Sync Events: {len(self.sync_events)} recorded")
            for _, event in self.sync_events.iterrows():
                report.append(f"  - {event['event_type']} at {event['relative_offset_ms']:.1f}ms")
        
        # Simultaneous events analysis
        simultaneous = self.find_simultaneous_events()
        if simultaneous:
            report.append(f"\nSimultaneous Events: {len(simultaneous)} found")
            time_diffs = [event['time_diff_ms'] for event in simultaneous]
            report.append(f"Average time difference: {np.mean(time_diffs):.2f}ms")
            report.append(f"Max time difference: {np.max(time_diffs):.2f}ms")
            report.append(f"Sync quality: {'EXCELLENT' if np.max(time_diffs) < 5 else 'GOOD' if np.max(time_diffs) < 50 else 'ACCEPTABLE' if np.max(time_diffs) < 100 else 'POOR'}")
        
        return "\n".join(report)
    
    def export_aligned_data(self, output_file: str = "aligned_data.csv"):
        """Export aligned data to CSV for further analysis."""
        aligned_data = self.align_data_streams()
        simultaneous_events = self.find_simultaneous_events()
        
        if simultaneous_events:
            df = pd.DataFrame(simultaneous_events)
            output_path = self.session_dir / output_file
            df.to_csv(output_path, index=False)
            print(f"Exported aligned data to: {output_path}")


def main():
    parser = argparse.ArgumentParser(description="Synchronize multi-modal sensor data streams")
    parser.add_argument("session_directory", help="Path to session directory containing data files")
    parser.add_argument("--window", type=int, default=50, 
                       help="Time window in milliseconds for simultaneous event detection (default: 50)")
    parser.add_argument("--export", action="store_true", 
                       help="Export aligned data to CSV")
    
    args = parser.parse_args()
    
    try:
        # Initialize synchronizer
        synchronizer = MultiModalSynchronizer(args.session_directory)
        
        # Load data streams
        synchronizer.load_thermal_data()
        synchronizer.load_gsr_data()
        synchronizer.extract_sync_events()
        
        # Generate and display sync report
        report = synchronizer.generate_sync_report()
        print(report)
        
        # Export aligned data if requested
        if args.export:
            synchronizer.export_aligned_data()
        
        print("\nSynchronization analysis complete!")
        print("\nTo align data streams in your analysis:")
        print("1. Use 'timestamp_relative_ms' as the common time base")
        print("2. All timestamps are relative to the session start time")
        print("3. Session metadata provides the absolute start time for reference")
        
    except Exception as e:
        print(f"Error: {e}")
        return 1
    
    return 0


if __name__ == "__main__":
    exit(main())