#!/usr/bin/env python3
"""
Enterprise Quality Enforcer - Advanced Quality Management System
Provides intelligent quality analysis, trend tracking, and automated enforcement
"""

import argparse
import json
import os
import sqlite3
import subprocess
import sys
import time
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Optional, Tuple

import yaml


class QualityDatabase:
    """Manages historical quality metrics and trends"""

    def __init__(self, db_path: str = "quality_metrics.db"):
        self.db_path = db_path
        self._init_database()

    def _init_database(self):
        """Initialize the quality metrics database"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        cursor.execute(
            """
            CREATE TABLE IF NOT EXISTS quality_metrics (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp TEXT NOT NULL,
                overall_score INTEGER NOT NULL,
                security_score INTEGER NOT NULL,
                complexity_score INTEGER NOT NULL,
                duplication_score INTEGER NOT NULL,
                coverage_score INTEGER NOT NULL,
                style_score INTEGER NOT NULL,
                commit_hash TEXT,
                branch TEXT,
                analysis_duration INTEGER
            )
        """
        )

        cursor.execute(
            """
            CREATE TABLE IF NOT EXISTS quality_trends (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                avg_score REAL NOT NULL,
                score_change REAL,
                trend_direction TEXT
            )
        """
        )

        conn.commit()
        conn.close()

    def store_metrics(self, metrics: Dict) -> None:
        """Store quality metrics in the database"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        cursor.execute(
            """
            INSERT INTO quality_metrics 
            (timestamp, overall_score, security_score, complexity_score, 
             duplication_score, coverage_score, style_score, commit_hash, branch, analysis_duration)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
            (
                metrics.get("timestamp", datetime.now().isoformat()),
                metrics.get("overall_score", 0),
                metrics.get("metrics", {}).get("security", {}).get("score", 0),
                metrics.get("metrics", {}).get("complexity", {}).get("score", 0),
                metrics.get("metrics", {}).get("duplication", {}).get("score", 0),
                metrics.get("metrics", {}).get("coverage", {}).get("score", 0),
                metrics.get("metrics", {}).get("style", {}).get("score", 0),
                metrics.get("commit_hash", ""),
                metrics.get("branch", ""),
                metrics.get("analysis_duration", 0),
            ),
        )

        conn.commit()
        conn.close()

    def get_trend_analysis(self, days: int = 30) -> Dict:
        """Get quality trend analysis for the specified period"""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()

        cutoff_date = (datetime.now() - timedelta(days=days)).isoformat()

        cursor.execute(
            """
            SELECT overall_score, timestamp 
            FROM quality_metrics 
            WHERE timestamp > ? 
            ORDER BY timestamp
        """,
            (cutoff_date,),
        )

        data = cursor.fetchall()
        conn.close()

        if len(data) < 2:
            return {"trend": "insufficient_data", "scores": [], "improvement": 0}

        scores = [row[0] for row in data]
        timestamps = [row[1] for row in data]

        # Calculate trend
        first_score = scores[0]
        last_score = scores[-1]
        improvement = last_score - first_score

        trend = (
            "improving"
            if improvement > 2
            else "declining" if improvement < -2 else "stable"
        )

        return {
            "trend": trend,
            "improvement": improvement,
            "scores": scores,
            "timestamps": timestamps,
            "average": sum(scores) / len(scores),
            "min": min(scores),
            "max": max(scores),
        }


class EnterpriseQualityEnforcer:
    """Main quality enforcement engine"""

    def __init__(self, config_path: str = "quality_standards.yml"):
        self.config_path = config_path
        self.config = self._load_config()
        self.db = QualityDatabase()
        self.reports_dir = Path("quality_reports")
        self.reports_dir.mkdir(exist_ok=True)

    def _load_config(self) -> Dict:
        """Load quality standards configuration"""
        try:
            with open(self.config_path, "r") as f:
                return yaml.safe_load(f)
        except FileNotFoundError:
            print(f"⚠️  Configuration file {self.config_path} not found, using defaults")
            return self._default_config()

    def _default_config(self) -> Dict:
        """Default configuration if file is missing"""
        return {
            "quality_gates": {
                "overall_minimum": 85,
                "security_minimum": 90,
                "complexity_maximum": 15,
                "coverage_minimum": 70,
                "style_minimum": 95,
            }
        }

    def run_comprehensive_analysis(self) -> Dict:
        """Run comprehensive quality analysis"""
        print("🚀 Starting Enterprise Quality Analysis...")
        start_time = time.time()

        # Run the advanced quality system
        try:
            result = subprocess.run(
                ["bash", "advanced_quality_system.sh", "analyze"],
                capture_output=True,
                text=True,
                timeout=300,
            )

            if result.returncode != 0:
                print(f"⚠️  Quality analysis completed with warnings: {result.stderr}")
        except subprocess.TimeoutExpired:
            print("⚠️  Quality analysis timed out, using partial results")
        except Exception as e:
            print(f"❌ Quality analysis failed: {e}")
            return {}

        # Load results
        dashboard_path = self.reports_dir / "quality_dashboard.json"
        if dashboard_path.exists():
            with open(dashboard_path, "r") as f:
                metrics = json.load(f)

            # Add metadata
            metrics["analysis_duration"] = int(time.time() - start_time)
            metrics["commit_hash"] = self._get_current_commit()
            metrics["branch"] = self._get_current_branch()

            # Store in database
            self.db.store_metrics(metrics)

            return metrics
        else:
            print("❌ Quality dashboard not generated")
            return {}

    def _get_current_commit(self) -> str:
        """Get current Git commit hash"""
        try:
            result = subprocess.run(
                ["git", "rev-parse", "HEAD"], capture_output=True, text=True
            )
            return result.stdout.strip()[:8] if result.returncode == 0 else ""
        except:
            return ""

    def _get_current_branch(self) -> str:
        """Get current Git branch"""
        try:
            result = subprocess.run(
                ["git", "branch", "--show-current"], capture_output=True, text=True
            )
            return result.stdout.strip() if result.returncode == 0 else ""
        except:
            return ""

    def enforce_quality_gates(self, metrics: Dict) -> bool:
        """Enforce quality gates based on configuration"""
        print("\n🛡️  Enforcing Quality Gates...")

        gates = self.config.get("quality_gates", {})
        overall_score = metrics.get("overall_score", 0)
        minimum_score = gates.get("overall_minimum", 85)

        print(f"📊 Overall Quality Score: {overall_score}/100")
        print(f"🎯 Required Minimum: {minimum_score}/100")

        if overall_score >= minimum_score:
            print("✅ Quality Gates: PASSED")
            return True
        else:
            print("❌ Quality Gates: FAILED")
            self._print_improvement_recommendations(metrics, gates)
            return False

    def _print_improvement_recommendations(self, metrics: Dict, gates: Dict) -> None:
        """Print specific recommendations for improvement"""
        print("\n🔧 Improvement Recommendations:")

        quality_metrics = metrics.get("metrics", {})

        # Security recommendations
        security_score = quality_metrics.get("security", {}).get("score", 0)
        if security_score < gates.get("security_minimum", 90):
            print(
                f"  🔒 Security: {security_score}/100 - Address security vulnerabilities"
            )

        # Complexity recommendations
        complexity_score = quality_metrics.get("complexity", {}).get("score", 0)
        if complexity_score < 80:
            print(f"  🧠 Complexity: {complexity_score}/100 - Reduce code complexity")

        # Coverage recommendations
        coverage_score = quality_metrics.get("coverage", {}).get("score", 0)
        if coverage_score < gates.get("coverage_minimum", 70):
            print(f"  🧪 Coverage: {coverage_score}/100 - Increase test coverage")

        # Style recommendations
        style_score = quality_metrics.get("style", {}).get("score", 0)
        if style_score < gates.get("style_minimum", 95):
            print(f"  💅 Style: {style_score}/100 - Fix code style violations")

    def generate_advanced_report(self, metrics: Dict) -> None:
        """Generate advanced quality report with trends"""
        print("\n📈 Generating Advanced Quality Report...")

        # Get trend analysis
        trends = self.db.get_trend_analysis(30)

        # Generate enhanced HTML report
        report_path = self.reports_dir / "enterprise_quality_report.html"

        html_content = f"""
<!DOCTYPE html>
<html>
<head>
    <title>Enterprise Quality Report</title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>
        body {{ 
            font-family: 'Segoe UI', system-ui, sans-serif; 
            margin: 0; 
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); 
            min-height: 100vh;
        }}
        .container {{ 
            max-width: 1400px; 
            margin: 0 auto; 
            padding: 20px; 
        }}
        .header {{ 
            background: white; 
            padding: 40px; 
            border-radius: 20px; 
            text-align: center; 
            margin-bottom: 30px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.2);
        }}
        .score {{ 
            font-size: 72px; 
            font-weight: 900; 
            background: linear-gradient(45deg, #667eea, #764ba2);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            margin: 20px 0;
        }}
        .quality-gate {{ 
            font-size: 24px; 
            font-weight: bold; 
            padding: 15px 30px;
            border-radius: 50px;
            display: inline-block;
            margin: 20px 0;
        }}
        .passed {{ 
            background: linear-gradient(45deg, #4CAF50, #45a049);
            color: white;
        }}
        .failed {{ 
            background: linear-gradient(45deg, #f44336, #da190b);
            color: white;
        }}
        .metrics-grid {{ 
            display: grid; 
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); 
            gap: 20px; 
            margin: 30px 0; 
        }}
        .metric-card {{ 
            background: white; 
            padding: 30px; 
            border-radius: 15px; 
            text-align: center;
            box-shadow: 0 8px 25px rgba(0,0,0,0.15);
            transition: transform 0.3s ease;
        }}
        .metric-card:hover {{ 
            transform: translateY(-5px);
        }}
        .metric-title {{ 
            font-size: 18px;
            font-weight: 600; 
            color: #555; 
            margin-bottom: 15px; 
        }}
        .metric-score {{ 
            font-size: 48px; 
            font-weight: 900; 
            margin: 15px 0;
        }}
        .score-excellent {{ color: #4CAF50; }}
        .score-good {{ color: #2196F3; }}
        .score-warning {{ color: #FF9800; }}
        .score-poor {{ color: #f44336; }}
        .trends {{ 
            background: white; 
            padding: 30px; 
            border-radius: 15px;
            margin: 30px 0;
            box-shadow: 0 8px 25px rgba(0,0,0,0.15);
        }}
        .trend-indicator {{
            display: inline-block;
            padding: 8px 16px;
            border-radius: 20px;
            font-weight: bold;
            margin: 10px 0;
        }}
        .trend-improving {{ background: #E8F5E8; color: #2E7D32; }}
        .trend-declining {{ background: #FFEBEE; color: #C62828; }}
        .trend-stable {{ background: #E3F2FD; color: #1565C0; }}
        .recommendations {{ 
            background: white; 
            padding: 30px; 
            border-radius: 15px;
            margin: 30px 0;
            box-shadow: 0 8px 25px rgba(0,0,0,0.15);
        }}
        .rec-item {{ 
            background: #f8f9fa; 
            padding: 15px; 
            margin: 10px 0; 
            border-radius: 8px;
            border-left: 4px solid #667eea;
        }}
        .footer {{ 
            text-align: center; 
            color: white; 
            margin: 40px 0; 
            opacity: 0.8;
        }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🏆 Enterprise Quality Report</h1>
            <div class="score">{metrics.get('overall_score', 0)}</div>
            <div class="quality-gate {'passed' if metrics.get('quality_gate') == 'PASSED' else 'failed'}">
                {metrics.get('quality_gate', 'UNKNOWN')}
            </div>
            <p>Generated on {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>
        </div>
        
        <div class="metrics-grid">
"""

        # Add metric cards
        for key, metric in metrics.get("metrics", {}).items():
            score = metric.get("score", 0)
            score_class = (
                "score-excellent"
                if score >= 90
                else (
                    "score-good"
                    if score >= 80
                    else "score-warning" if score >= 70 else "score-poor"
                )
            )

            html_content += f"""
            <div class="metric-card">
                <div class="metric-title">{metric.get('description', key.title())}</div>
                <div class="metric-score {score_class}">{score}</div>
                <div>Weight: {metric.get('weight', 0)}%</div>
            </div>
            """

        # Add trends section
        trend_class = f"trend-{trends.get('trend', 'stable')}"
        html_content += f"""
        </div>
        
        <div class="trends">
            <h2>📈 Quality Trends (30 days)</h2>
            <div class="trend-indicator {trend_class}">
                {trends.get('trend', 'stable').title()} 
                ({trends.get('improvement', 0):+.1f} points)
            </div>
            <p>Average Score: {trends.get('average', 0):.1f}/100</p>
            <p>Range: {trends.get('min', 0)} - {trends.get('max', 0)}</p>
        </div>
        
        <div class="recommendations">
            <h2>🔧 Quality Recommendations</h2>
        """

        # Add recommendations
        recommendations = metrics.get("recommendations", [])
        if recommendations:
            for rec in recommendations:
                if rec:  # Skip empty recommendations
                    html_content += f'<div class="rec-item">{rec}</div>'
        else:
            html_content += (
                '<div class="rec-item">✅ All quality standards are being met!</div>'
            )

        html_content += f"""
        </div>
        
        <div class="footer">
            <p>Analysis Duration: {metrics.get('analysis_duration', 0)}s | 
               Commit: {metrics.get('commit_hash', 'unknown')} | 
               Branch: {metrics.get('branch', 'unknown')}</p>
        </div>
    </div>
</body>
</html>
"""

        with open(report_path, "w") as f:
            f.write(html_content)

        print(f"✅ Advanced report generated: {report_path}")

    def auto_fix_issues(self, metrics: Dict) -> bool:
        """Attempt to automatically fix common quality issues"""
        print("\n🔧 Attempting Automatic Fixes...")

        fixed_issues = 0

        # Auto-fix style issues
        style_score = metrics.get("metrics", {}).get("style", {}).get("score", 100)
        if style_score < 95:
            print("  🎨 Auto-fixing style issues...")
            try:
                # Run ktlint with auto-format
                subprocess.run(["bash", "quality_format.sh", "format"], check=False)
                fixed_issues += 1
                print("    ✅ Style issues auto-fixed")
            except Exception as e:
                print(f"    ⚠️  Could not auto-fix style issues: {e}")

        # Auto-fix Python formatting
        if Path("pyproject.toml").exists():
            try:
                subprocess.run(
                    ["python", "-m", "black", "."], check=False, capture_output=True
                )
                subprocess.run(
                    ["python", "-m", "isort", "."], check=False, capture_output=True
                )
                fixed_issues += 1
                print("    ✅ Python formatting auto-fixed")
            except Exception as e:
                print(f"    ⚠️  Could not auto-fix Python formatting: {e}")

        return fixed_issues > 0


def main():
    parser = argparse.ArgumentParser(description="Enterprise Quality Enforcer")
    parser.add_argument(
        "--analyze", action="store_true", help="Run comprehensive analysis"
    )
    parser.add_argument("--enforce", action="store_true", help="Enforce quality gates")
    parser.add_argument("--fix", action="store_true", help="Attempt automatic fixes")
    parser.add_argument(
        "--report", action="store_true", help="Generate advanced report"
    )
    parser.add_argument(
        "--all", action="store_true", help="Run complete quality pipeline"
    )

    args = parser.parse_args()

    enforcer = EnterpriseQualityEnforcer()

    if args.all or not any([args.analyze, args.enforce, args.fix, args.report]):
        # Run complete pipeline
        metrics = enforcer.run_comprehensive_analysis()
        if metrics:
            enforcer.generate_advanced_report(metrics)

            if args.fix or args.all:
                enforcer.auto_fix_issues(metrics)

            gates_passed = enforcer.enforce_quality_gates(metrics)
            sys.exit(0 if gates_passed else 1)
        else:
            print("❌ Analysis failed")
            sys.exit(1)

    if args.analyze:
        metrics = enforcer.run_comprehensive_analysis()
        if not metrics:
            sys.exit(1)

    if args.report:
        # Load existing metrics if available
        dashboard_path = Path("quality_reports/quality_dashboard.json")
        if dashboard_path.exists():
            with open(dashboard_path) as f:
                metrics = json.load(f)
            enforcer.generate_advanced_report(metrics)

    if args.enforce:
        dashboard_path = Path("quality_reports/quality_dashboard.json")
        if dashboard_path.exists():
            with open(dashboard_path) as f:
                metrics = json.load(f)
            gates_passed = enforcer.enforce_quality_gates(metrics)
            sys.exit(0 if gates_passed else 1)


if __name__ == "__main__":
    main()
