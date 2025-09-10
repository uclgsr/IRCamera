#!/usr/bin/env python3
"""
System Health Monitor - Advanced Build and Quality Monitoring
Real-time monitoring of build health, quality metrics, and system performance
"""

import argparse
import json
import os
import signal
import sqlite3
import subprocess
import sys
import threading
import time
from dataclasses import asdict, dataclass
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Optional, Tuple

import psutil


@dataclass
class SystemMetrics:
    """System health metrics"""

    timestamp: datetime
    cpu_percent: float
    memory_percent: float
    disk_usage_percent: float
    gradle_processes: int
    build_cache_size_mb: float
    compilation_errors: int
    quality_score: float
    last_build_duration: float
    active_validators: int


class SystemHealthMonitor:
    """Advanced system health and build monitoring"""

    def __init__(self, project_root: str = "."):
        self.project_root = Path(project_root)
        self.db_path = self.project_root / ".health_monitor.db"
        self.running = False
        self.monitor_thread = None

        # Monitoring configuration
        self.monitor_interval = 30  # seconds
        self.alert_thresholds = {
            "cpu_percent": 80,
            "memory_percent": 85,
            "disk_usage_percent": 90,
            "build_duration": 300,  # 5 minutes
            "quality_score": 70,
        }

        # Initialize database
        self._init_database()

    def _init_database(self):
        """Initialize monitoring database"""
        with sqlite3.connect(self.db_path) as conn:
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS system_metrics (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp TEXT NOT NULL,
                    cpu_percent REAL,
                    memory_percent REAL,
                    disk_usage_percent REAL,
                    gradle_processes INTEGER,
                    build_cache_size_mb REAL,
                    compilation_errors INTEGER,
                    quality_score REAL,
                    last_build_duration REAL,
                    active_validators INTEGER
                )
            """
            )

            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS build_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp TEXT NOT NULL,
                    event_type TEXT NOT NULL,
                    module TEXT,
                    duration REAL,
                    status TEXT,
                    error_message TEXT,
                    memory_usage REAL
                )
            """
            )

            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS quality_alerts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp TEXT NOT NULL,
                    alert_type TEXT NOT NULL,
                    severity TEXT,
                    message TEXT,
                    resolved BOOLEAN DEFAULT FALSE,
                    resolution_time TEXT
                )
            """
            )

    def collect_system_metrics(self) -> SystemMetrics:
        """Collect comprehensive system metrics"""
        # System resources
        cpu_percent = psutil.cpu_percent(interval=1)
        memory = psutil.virtual_memory()
        disk = psutil.disk_usage(str(self.project_root))

        # Gradle processes
        gradle_processes = len(
            [
                p
                for p in psutil.process_iter(["name"])
                if "gradle" in p.info["name"].lower()
            ]
        )

        # Build cache size
        gradle_cache = Path.home() / ".gradle" / "caches"
        build_cache_size = 0
        if gradle_cache.exists():
            try:
                result = subprocess.run(
                    ["du", "-sm", str(gradle_cache)],
                    capture_output=True,
                    text=True,
                    timeout=10,
                )
                if result.returncode == 0:
                    build_cache_size = float(result.stdout.split()[0])
            except:
                build_cache_size = 0

        # Compilation errors (from recent logs)
        compilation_errors = self._count_recent_errors()

        # Quality score (from latest analysis)
        quality_score = self._get_latest_quality_score()

        # Last build duration
        last_build_duration = self._get_last_build_duration()

        # Active validators
        active_validators = len(
            [
                p
                for p in psutil.process_iter(["name"])
                if any(
                    validator in p.info["name"].lower()
                    for validator in ["validate", "gradle", "ktlint"]
                )
            ]
        )

        return SystemMetrics(
            timestamp=datetime.now(),
            cpu_percent=cpu_percent,
            memory_percent=memory.percent,
            disk_usage_percent=(disk.used / disk.total) * 100,
            gradle_processes=gradle_processes,
            build_cache_size_mb=build_cache_size,
            compilation_errors=compilation_errors,
            quality_score=quality_score,
            last_build_duration=last_build_duration,
            active_validators=active_validators,
        )

    def _count_recent_errors(self) -> int:
        """Count compilation errors from recent build logs"""
        try:
            # Check gradle build logs
            gradle_log = self.project_root / "build.log"
            if (
                gradle_log.exists() and time.time() - gradle_log.stat().st_mtime < 3600
            ):  # 1 hour
                with open(gradle_log) as f:
                    content = f.read()
                    return content.count("FAILED") + content.count("ERROR")
        except:
            pass
        return 0

    def _get_latest_quality_score(self) -> float:
        """Get latest quality score from analysis"""
        try:
            reports_dir = self.project_root / "quality_reports"
            if reports_dir.exists():
                json_files = list(reports_dir.glob("analysis_summary_*.json"))
                if json_files:
                    latest_file = max(json_files, key=lambda x: x.stat().st_mtime)
                    if time.time() - latest_file.stat().st_mtime < 86400:  # 24 hours
                        with open(latest_file) as f:
                            data = json.load(f)
                            return data.get("quality_score", 0)
        except:
            pass
        return 0

    def _get_last_build_duration(self) -> float:
        """Get duration of last build"""
        try:
            # Check for build timing files
            timing_files = list(self.project_root.glob("build_timing_*.txt"))
            if timing_files:
                latest_file = max(timing_files, key=lambda x: x.stat().st_mtime)
                with open(latest_file) as f:
                    return float(f.read().strip())
        except:
            pass
        return 0

    def check_alerts(self, metrics: SystemMetrics) -> List[Dict]:
        """Check for alert conditions"""
        alerts = []

        # CPU usage alert
        if metrics.cpu_percent > self.alert_thresholds["cpu_percent"]:
            alerts.append(
                {
                    "type": "system_performance",
                    "severity": "warning",
                    "message": f"High CPU usage: {metrics.cpu_percent:.1f}%",
                }
            )

        # Memory usage alert
        if metrics.memory_percent > self.alert_thresholds["memory_percent"]:
            alerts.append(
                {
                    "type": "system_performance",
                    "severity": "warning",
                    "message": f"High memory usage: {metrics.memory_percent:.1f}%",
                }
            )

        # Disk usage alert
        if metrics.disk_usage_percent > self.alert_thresholds["disk_usage_percent"]:
            alerts.append(
                {
                    "type": "system_storage",
                    "severity": "critical",
                    "message": f"High disk usage: {metrics.disk_usage_percent:.1f}%",
                }
            )

        # Build performance alert
        if metrics.last_build_duration > self.alert_thresholds["build_duration"]:
            alerts.append(
                {
                    "type": "build_performance",
                    "severity": "warning",
                    "message": f"Slow build detected: {metrics.last_build_duration:.1f}s",
                }
            )

        # Quality score alert
        if (
            metrics.quality_score > 0
            and metrics.quality_score < self.alert_thresholds["quality_score"]
        ):
            alerts.append(
                {
                    "type": "code_quality",
                    "severity": "warning",
                    "message": f"Low quality score: {metrics.quality_score:.1f}/100",
                }
            )

        # Large cache alert
        if metrics.build_cache_size_mb > 5000:  # 5GB
            alerts.append(
                {
                    "type": "build_optimization",
                    "severity": "info",
                    "message": f"Large build cache: {metrics.build_cache_size_mb:.0f}MB - consider cleaning",
                }
            )

        return alerts

    def store_metrics(self, metrics: SystemMetrics):
        """Store metrics in database"""
        with sqlite3.connect(self.db_path) as conn:
            conn.execute(
                """
                INSERT INTO system_metrics 
                (timestamp, cpu_percent, memory_percent, disk_usage_percent,
                 gradle_processes, build_cache_size_mb, compilation_errors,
                 quality_score, last_build_duration, active_validators)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
                (
                    metrics.timestamp.isoformat(),
                    metrics.cpu_percent,
                    metrics.memory_percent,
                    metrics.disk_usage_percent,
                    metrics.gradle_processes,
                    metrics.build_cache_size_mb,
                    metrics.compilation_errors,
                    metrics.quality_score,
                    metrics.last_build_duration,
                    metrics.active_validators,
                ),
            )

    def store_alerts(self, alerts: List[Dict]):
        """Store alerts in database"""
        if not alerts:
            return

        with sqlite3.connect(self.db_path) as conn:
            for alert in alerts:
                conn.execute(
                    """
                    INSERT INTO quality_alerts (timestamp, alert_type, severity, message)
                    VALUES (?, ?, ?, ?)
                """,
                    (
                        datetime.now().isoformat(),
                        alert["type"],
                        alert["severity"],
                        alert["message"],
                    ),
                )

    def print_dashboard(self, metrics: SystemMetrics, alerts: List[Dict]):
        """Print real-time dashboard"""
        os.system("clear" if os.name == "posix" else "cls")

        print("🔍 IRCamera System Health Monitor")
        print("=" * 60)
        print(f"📅 Last Update: {metrics.timestamp.strftime('%Y-%m-%d %H:%M:%S')}")
        print()

        # System Resources
        print("🖥️  System Resources:")
        print(
            f"   CPU Usage:     {metrics.cpu_percent:5.1f}% {'🔥' if metrics.cpu_percent > 80 else '✅'}"
        )
        print(
            f"   Memory Usage:  {metrics.memory_percent:5.1f}% {'🔥' if metrics.memory_percent > 80 else '✅'}"
        )
        print(
            f"   Disk Usage:    {metrics.disk_usage_percent:5.1f}% {'🔥' if metrics.disk_usage_percent > 90 else '✅'}"
        )
        print()

        # Build System
        print("🏗️  Build System:")
        print(f"   Gradle Processes:  {metrics.gradle_processes}")
        print(f"   Active Validators: {metrics.active_validators}")
        print(f"   Build Cache:       {metrics.build_cache_size_mb:,.0f} MB")
        print(
            f"   Last Build Time:   {metrics.last_build_duration:.1f}s {'🐌' if metrics.last_build_duration > 300 else '⚡'}"
        )
        print()

        # Code Quality
        print("📊 Code Quality:")
        print(
            f"   Quality Score:     {metrics.quality_score:.1f}/100 {'🌟' if metrics.quality_score > 80 else '⚠️' if metrics.quality_score > 60 else '🔥'}"
        )
        print(
            f"   Compilation Errors: {metrics.compilation_errors} {'❌' if metrics.compilation_errors > 0 else '✅'}"
        )
        print()

        # Alerts
        if alerts:
            print("🚨 Active Alerts:")
            for alert in alerts:
                severity_icon = {"critical": "🔥", "warning": "⚠️", "info": "💡"}.get(
                    alert["severity"], "📋"
                )
                print(f"   {severity_icon} {alert['message']}")
            print()
        else:
            print("✅ No Active Alerts")
            print()

        # Performance Recommendations
        recommendations = self._generate_recommendations(metrics)
        if recommendations:
            print("💡 Performance Recommendations:")
            for rec in recommendations:
                print(f"   • {rec}")
            print()

        print("🔄 Monitoring... (Press Ctrl+C to stop)")

    def _generate_recommendations(self, metrics: SystemMetrics) -> List[str]:
        """Generate performance recommendations"""
        recommendations = []

        if metrics.cpu_percent > 70:
            recommendations.append(
                "Consider closing unnecessary applications to reduce CPU usage"
            )

        if metrics.memory_percent > 75:
            recommendations.append("Close memory-intensive applications or restart IDE")

        if metrics.build_cache_size_mb > 3000:
            recommendations.append("Run 'gradle clean' to clear build cache")

        if metrics.last_build_duration > 180:
            recommendations.append("Enable gradle daemon and parallel builds")

        if metrics.gradle_processes > 3:
            recommendations.append(
                "Multiple gradle processes detected - consider using single daemon"
            )

        if metrics.quality_score < 70 and metrics.quality_score > 0:
            recommendations.append("Run quality analysis and fix reported issues")

        return recommendations

    def monitor_loop(self):
        """Main monitoring loop"""
        print("🚀 Starting system health monitoring...")

        while self.running:
            try:
                # Collect metrics
                metrics = self.collect_system_metrics()

                # Check for alerts
                alerts = self.check_alerts(metrics)

                # Store data
                self.store_metrics(metrics)
                self.store_alerts(alerts)

                # Update dashboard
                self.print_dashboard(metrics, alerts)

                # Wait for next iteration
                time.sleep(self.monitor_interval)

            except KeyboardInterrupt:
                print("\n🛑 Monitoring stopped by user")
                break
            except Exception as e:
                print(f"\n❌ Monitoring error: {e}")
                time.sleep(5)

        self.running = False

    def start_monitoring(self, interval: int = 30):
        """Start monitoring in background thread"""
        self.monitor_interval = interval
        self.running = True
        self.monitor_thread = threading.Thread(target=self.monitor_loop)
        self.monitor_thread.daemon = True
        self.monitor_thread.start()

    def stop_monitoring(self):
        """Stop monitoring"""
        self.running = False
        if self.monitor_thread:
            self.monitor_thread.join(timeout=5)

    def generate_health_report(self) -> Dict:
        """Generate comprehensive health report"""
        print("📊 Generating system health report...")

        with sqlite3.connect(self.db_path) as conn:
            # Get recent metrics (last 24 hours)
            yesterday = (datetime.now() - timedelta(days=1)).isoformat()

            metrics = conn.execute(
                """
                SELECT * FROM system_metrics 
                WHERE timestamp > ? 
                ORDER BY timestamp DESC
                LIMIT 100
            """,
                (yesterday,),
            ).fetchall()

            alerts = conn.execute(
                """
                SELECT * FROM quality_alerts 
                WHERE timestamp > ? 
                ORDER BY timestamp DESC
            """,
                (yesterday,),
            ).fetchall()

            if not metrics:
                return {"message": "No recent monitoring data available"}

            # Calculate statistics
            latest = metrics[0]
            avg_cpu = sum(m[2] for m in metrics) / len(metrics)
            avg_memory = sum(m[3] for m in metrics) / len(metrics)
            max_build_time = max(m[9] for m in metrics)

            report = {
                "timestamp": datetime.now().isoformat(),
                "monitoring_period_hours": 24,
                "samples_collected": len(metrics),
                "current_status": {
                    "cpu_percent": latest[2],
                    "memory_percent": latest[3],
                    "quality_score": latest[8],
                    "last_build_duration": latest[9],
                },
                "averages_24h": {
                    "cpu_percent": avg_cpu,
                    "memory_percent": avg_memory,
                    "build_cache_size_mb": sum(m[6] for m in metrics) / len(metrics),
                },
                "performance_analysis": {
                    "max_build_time": max_build_time,
                    "build_performance_rating": (
                        "Excellent"
                        if max_build_time < 120
                        else "Good" if max_build_time < 300 else "Needs Improvement"
                    ),
                    "system_load_rating": (
                        "Low" if avg_cpu < 50 else "Medium" if avg_cpu < 75 else "High"
                    ),
                },
                "alert_summary": {
                    "total_alerts": len(alerts),
                    "critical_alerts": len([a for a in alerts if a[3] == "critical"]),
                    "warning_alerts": len([a for a in alerts if a[3] == "warning"]),
                },
                "recommendations": self._generate_health_recommendations(
                    metrics, alerts
                ),
            }

        return report

    def _generate_health_recommendations(
        self, metrics: List, alerts: List
    ) -> List[str]:
        """Generate health-based recommendations"""
        recommendations = []

        if len(metrics) == 0:
            return ["No monitoring data available"]

        avg_cpu = sum(m[2] for m in metrics) / len(metrics)
        avg_memory = sum(m[3] for m in metrics) / len(metrics)
        max_cache = max(m[6] for m in metrics)

        if avg_cpu > 60:
            recommendations.append(
                "System running hot - consider upgrading hardware or optimizing builds"
            )

        if avg_memory > 70:
            recommendations.append(
                "High memory usage detected - increase RAM or optimize IDE settings"
            )

        if max_cache > 4000:
            recommendations.append(
                "Large build cache detected - implement automated cache cleanup"
            )

        if len([a for a in alerts if a[3] == "critical"]) > 0:
            recommendations.append(
                "Critical alerts detected - immediate attention required"
            )

        if len(alerts) > 20:
            recommendations.append(
                "Frequent alerts - review system configuration and thresholds"
            )

        return recommendations or [
            "System running optimally - no recommendations needed"
        ]


def main():
    """Main entry point"""
    parser = argparse.ArgumentParser(description="IRCamera System Health Monitor")
    parser.add_argument(
        "--interval", type=int, default=30, help="Monitoring interval in seconds"
    )
    parser.add_argument(
        "--report", action="store_true", help="Generate health report and exit"
    )
    parser.add_argument(
        "--dashboard", action="store_true", help="Show real-time dashboard"
    )
    parser.add_argument("--project-root", default=".", help="Project root directory")

    args = parser.parse_args()

    monitor = SystemHealthMonitor(args.project_root)

    if args.report:
        # Generate and print health report
        report = monitor.generate_health_report()
        print(json.dumps(report, indent=2))

        # Save report to file
        report_file = (
            Path(args.project_root)
            / f"health_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        )
        with open(report_file, "w") as f:
            json.dump(report, f, indent=2)
        print(f"\n📊 Health report saved: {report_file}")

    elif args.dashboard:
        # Show real-time dashboard
        try:
            monitor.monitor_loop()
        except KeyboardInterrupt:
            print("\n👋 Monitoring stopped")
    else:
        print("IRCamera System Health Monitor")
        print(
            "Use --dashboard for real-time monitoring or --report for health analysis"
        )
        print("Run with --help for all options")


if __name__ == "__main__":
    main()
