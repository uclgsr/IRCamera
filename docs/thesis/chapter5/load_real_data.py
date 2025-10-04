#!/usr/bin/env python3
"""
Real Data Loader for Chapter 5 Experimental Evaluation

This utility script loads actual experimental data from recording sessions
and updates the Chapter 5 content with real measurements instead of synthetic data.

Usage:
    python3 load_real_data.py --session /path/to/session_directory
    python3 load_real_data.py --aggregate /path/to/sessions_parent/
"""

import argparse
import json
import csv
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Any, Optional
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class RealDataLoader:
    """Load and process real experimental data"""

    def __init__(self, output_dir: str = "./docs/chapter5"):
        self.output_dir = Path(output_dir)
        self.sessions = []
        self.aggregated_stats = {}

    def load_session(self, session_path: str) -> bool:
        """Load data from a single recording session"""
        session_dir = Path(session_path)
        
        if not session_dir.exists():
            logger.error(f"Session directory not found: {session_dir}")
            return False

        logger.info(f"Loading session from: {session_dir}")
        
        session_data = {
            'path': session_dir,
            'session_id': session_dir.name,
            'timesync_data': None,
            'gsr_data': None,
            'thermal_data': None,
            'metadata': None
        }

        # Load time sync log
        timesync_file = session_dir / "timesync_log.csv"
        if timesync_file.exists():
            session_data['timesync_data'] = self._load_timesync_log(timesync_file)
            logger.info(f"Loaded {len(session_data['timesync_data'])} sync records")

        # Load GSR data
        gsr_files = list(session_dir.glob("*gsr*.csv"))
        if gsr_files:
            session_data['gsr_data'] = self._load_sensor_csv(gsr_files[0])
            logger.info(f"Loaded {len(session_data['gsr_data'])} GSR samples")

        # Load thermal data
        thermal_files = list(session_dir.glob("*thermal*.csv"))
        if thermal_files:
            session_data['thermal_data'] = self._load_sensor_csv(thermal_files[0])
            logger.info(f"Loaded {len(session_data['thermal_data'])} thermal frames")

        # Load metadata if available
        metadata_file = session_dir / "session_metadata.json"
        if metadata_file.exists():
            with open(metadata_file) as f:
                session_data['metadata'] = json.load(f)

        self.sessions.append(session_data)
        return True

    def _load_timesync_log(self, csv_path: Path) -> List[Dict[str, Any]]:
        """Load time synchronization log"""
        data = []
        try:
            with open(csv_path, 'r') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    # Convert numeric fields
                    try:
                        row['offset_ms'] = float(row.get('offset_ms', 0))
                        row['rtt_ms'] = float(row.get('rtt_ms', 0))
                        data.append(row)
                    except ValueError:
                        continue
        except Exception as e:
            logger.error(f"Error loading timesync log: {e}")
        return data

    def _load_sensor_csv(self, csv_path: Path) -> List[Dict[str, Any]]:
        """Load sensor data CSV"""
        data = []
        try:
            with open(csv_path, 'r') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    data.append(row)
        except Exception as e:
            logger.error(f"Error loading sensor CSV: {e}")
        return data

    def aggregate_sessions(self, sessions_dir: str) -> bool:
        """Load and aggregate multiple sessions"""
        sessions_path = Path(sessions_dir)
        
        if not sessions_path.exists():
            logger.error(f"Sessions directory not found: {sessions_path}")
            return False

        session_dirs = [d for d in sessions_path.iterdir() if d.is_dir() and d.name.startswith('session_')]
        
        logger.info(f"Found {len(session_dirs)} session directories")
        
        for session_dir in session_dirs:
            self.load_session(str(session_dir))

        if self.sessions:
            self._calculate_aggregate_stats()
            return True
        
        return False

    def _calculate_aggregate_stats(self):
        """Calculate statistics across all loaded sessions"""
        logger.info("Calculating aggregate statistics")

        # Time sync statistics
        all_offsets = []
        all_rtts = []
        
        for session in self.sessions:
            if session['timesync_data']:
                for record in session['timesync_data']:
                    all_offsets.append(record['offset_ms'])
                    all_rtts.append(record['rtt_ms'])

        if all_offsets:
            self.aggregated_stats['sync_offset'] = {
                'mean': sum(all_offsets) / len(all_offsets),
                'min': min(all_offsets),
                'max': max(all_offsets),
                'count': len(all_offsets)
            }
            logger.info(f"Sync offset stats: mean={self.aggregated_stats['sync_offset']['mean']:.2f}ms")

        if all_rtts:
            self.aggregated_stats['rtt'] = {
                'mean': sum(all_rtts) / len(all_rtts),
                'min': min(all_rtts),
                'max': max(all_rtts),
                'count': len(all_rtts)
            }
            logger.info(f"RTT stats: mean={self.aggregated_stats['rtt']['mean']:.2f}ms")

        # TODO: Add more statistics (sensor rates, latency, etc.)

    def export_real_data_summary(self):
        """Export summary of real data loaded"""
        summary_file = self.output_dir / "real_data_summary.json"
        
        summary = {
            'sessions_loaded': len(self.sessions),
            'aggregate_stats': self.aggregated_stats,
            'sessions': [
                {
                    'session_id': s['session_id'],
                    'timesync_records': len(s['timesync_data']) if s['timesync_data'] else 0,
                    'gsr_samples': len(s['gsr_data']) if s['gsr_data'] else 0,
                    'thermal_frames': len(s['thermal_data']) if s['thermal_data'] else 0
                }
                for s in self.sessions
            ]
        }

        with open(summary_file, 'w') as f:
            json.dump(summary, f, indent=2)

        logger.info(f"Real data summary exported to {summary_file}")

    def generate_enhanced_content(self):
        """Generate Chapter 5 content enhanced with real data"""
        if not self.sessions:
            logger.warning("No sessions loaded - cannot generate enhanced content")
            return

        logger.info("Generating enhanced Chapter 5 content with real data")
        
        # Use the aggregated stats to update the experimental_evaluation output
        # This would modify the ExperimentalEvaluationFramework to use real values
        
        # For now, just export the summary
        self.export_real_data_summary()

        logger.info(f"Enhanced content would use {len(self.sessions)} real sessions")
        logger.info("To integrate real data, update experimental_evaluation.py to use loaded values")


def main():
    parser = argparse.ArgumentParser(
        description='Load real experimental data for Chapter 5 content'
    )
    parser.add_argument(
        '--session',
        type=str,
        help='Path to a single session directory'
    )
    parser.add_argument(
        '--aggregate',
        type=str,
        help='Path to parent directory containing multiple sessions'
    )
    parser.add_argument(
        '--output',
        type=str,
        default='./docs/chapter5',
        help='Output directory for generated content'
    )

    args = parser.parse_args()

    loader = RealDataLoader(output_dir=args.output)

    if args.session:
        if loader.load_session(args.session):
            loader.export_real_data_summary()
            loader.generate_enhanced_content()
        else:
            logger.error("Failed to load session")
            return 1

    elif args.aggregate:
        if loader.aggregate_sessions(args.aggregate):
            loader.export_real_data_summary()
            loader.generate_enhanced_content()
        else:
            logger.error("Failed to aggregate sessions")
            return 1

    else:
        parser.print_help()
        return 1

    logger.info("Real data loading complete!")
    return 0


if __name__ == "__main__":
    exit(main())
