#!/usr/bin/env python3
"""
Intelligent Code Optimizer with Machine Learning Insights
Advanced code analysis and optimization with performance metrics and trend analysis
"""

import os
import sys
import json
import time
import sqlite3
import hashlib
import subprocess
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Tuple, Optional, Set
from dataclasses import dataclass, asdict
import concurrent.futures
import threading

@dataclass
class FileMetrics:
    """Comprehensive file metrics with historical tracking"""
    file_path: str
    file_type: str
    size_bytes: int
    lines_of_code: int
    complexity_score: float
    violations_count: int
    last_modified: datetime
    processing_time: float
    hash_signature: str
    
    def to_dict(self) -> dict:
        return asdict(self)

@dataclass
class QualityTrend:
    """Quality trend analysis over time"""
    timestamp: datetime
    total_violations: int
    average_complexity: float
    files_processed: int
    processing_speed: float
    quality_score: float

class IntelligentCodeOptimizer:
    """Advanced code optimizer with ML insights and historical analysis"""
    
    def __init__(self, project_root: str = "."):
        self.project_root = Path(project_root)
        self.db_path = self.project_root / ".quality_history.db"
        self.config_path = self.project_root / ".optimizer_config.json"
        self.report_dir = self.project_root / "quality_reports"
        
        # Performance tracking
        self.start_time = time.time()
        self.processed_files = 0
        self.total_violations_fixed = 0
        
        # Threading for parallel processing
        self.max_workers = min(os.cpu_count() or 1, 8)
        self.lock = threading.Lock()
        
        # Initialize components
        self._init_database()
        self._load_config()
        self._ensure_directories()
        
    def _init_database(self):
        """Initialize SQLite database for historical tracking"""
        with sqlite3.connect(self.db_path) as conn:
            conn.execute("""
                CREATE TABLE IF NOT EXISTS file_metrics (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    file_path TEXT NOT NULL,
                    file_type TEXT NOT NULL,
                    size_bytes INTEGER,
                    lines_of_code INTEGER,
                    complexity_score REAL,
                    violations_count INTEGER,
                    last_modified TEXT,
                    processing_time REAL,
                    hash_signature TEXT,
                    analysis_timestamp TEXT,
                    UNIQUE(file_path, analysis_timestamp)
                )
            """)
            
            conn.execute("""
                CREATE TABLE IF NOT EXISTS quality_trends (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp TEXT NOT NULL,
                    total_violations INTEGER,
                    average_complexity REAL,
                    files_processed INTEGER,
                    processing_speed REAL,
                    quality_score REAL
                )
            """)
            
            conn.execute("CREATE INDEX IF NOT EXISTS idx_file_path ON file_metrics(file_path)")
            conn.execute("CREATE INDEX IF NOT EXISTS idx_timestamp ON quality_trends(timestamp)")
    
    def _load_config(self):
        """Load optimizer configuration"""
        default_config = {
            "file_types": {
                "kotlin": {"extensions": [".kt"], "max_complexity": 15, "max_violations": 5},
                "java": {"extensions": [".java"], "max_complexity": 20, "max_violations": 10},
                "python": {"extensions": [".py"], "max_complexity": 10, "max_violations": 3},
                "javascript": {"extensions": [".js", ".ts"], "max_complexity": 12, "max_violations": 5}
            },
            "optimization": {
                "enable_parallel_processing": True,
                "enable_incremental_analysis": True,
                "enable_ml_insights": True,
                "quality_threshold": 75
            },
            "reporting": {
                "generate_html": True,
                "generate_trends": True,
                "retention_days": 30
            }
        }
        
        if self.config_path.exists():
            with open(self.config_path) as f:
                self.config = {**default_config, **json.load(f)}
        else:
            self.config = default_config
            self._save_config()
    
    def _save_config(self):
        """Save configuration to file"""
        with open(self.config_path, 'w') as f:
            json.dump(self.config, f, indent=2)
    
    def _ensure_directories(self):
        """Ensure required directories exist"""
        self.report_dir.mkdir(exist_ok=True)
    
    def discover_files(self) -> Dict[str, List[Path]]:
        """Discover files by type with intelligent filtering"""
        print("🔍 Discovering files with intelligent filtering...")
        
        files_by_type = {}
        exclude_patterns = {'.git', '.gradle', 'build', 'node_modules', '__pycache__', '.venv'}
        
        for file_type, config in self.config["file_types"].items():
            files_by_type[file_type] = []
            
            for ext in config["extensions"]:
                for file_path in self.project_root.rglob(f"*{ext}"):
                    # Check if file should be excluded
                    if any(pattern in str(file_path) for pattern in exclude_patterns):
                        continue
                    
                    # Check file size (skip very large files > 1MB)
                    if file_path.stat().st_size > 1024 * 1024:
                        continue
                        
                    files_by_type[file_type].append(file_path)
        
        # Print discovery summary
        total_files = sum(len(files) for files in files_by_type.values())
        print(f"📊 Discovery Results: {total_files} files found")
        for file_type, files in files_by_type.items():
            print(f"  {file_type.capitalize()}: {len(files)} files")
            
        return files_by_type
    
    def analyze_file(self, file_path: Path, file_type: str) -> Optional[FileMetrics]:
        """Analyze individual file with comprehensive metrics"""
        try:
            # Basic file information
            stat = file_path.stat()
            size_bytes = stat.st_size
            last_modified = datetime.fromtimestamp(stat.st_mtime)
            
            # Read file content
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
            
            # Calculate hash for change detection
            hash_signature = hashlib.sha256(content.encode()).hexdigest()
            
            # Count lines of code (excluding empty lines and comments)
            lines = content.split('\n')
            lines_of_code = len([line for line in lines if line.strip() and not line.strip().startswith(('#', '//', '/*', '*', '<!--'))])
            
            # Calculate complexity score (simplified)
            complexity_score = self._calculate_complexity(content, file_type)
            
            # Count violations using appropriate linter
            violations_count = self._count_violations(file_path, file_type)
            
            # Measure processing time
            processing_start = time.time()
            processing_time = time.time() - processing_start
            
            return FileMetrics(
                file_path=str(file_path.relative_to(self.project_root)),
                file_type=file_type,
                size_bytes=size_bytes,
                lines_of_code=lines_of_code,
                complexity_score=complexity_score,
                violations_count=violations_count,
                last_modified=last_modified,
                processing_time=processing_time,
                hash_signature=hash_signature
            )
            
        except Exception as e:
            print(f"⚠️ Error analyzing {file_path}: {e}")
            return None
    
    def _calculate_complexity(self, content: str, file_type: str) -> float:
        """Calculate code complexity score"""
        if file_type == "python":
            # Count control structures
            complexity_keywords = ['if', 'elif', 'else', 'for', 'while', 'try', 'except', 'finally', 'with']
        elif file_type in ["kotlin", "java"]:
            complexity_keywords = ['if', 'else', 'for', 'while', 'switch', 'case', 'try', 'catch', 'finally']
        elif file_type == "javascript":
            complexity_keywords = ['if', 'else', 'for', 'while', 'switch', 'case', 'try', 'catch', 'finally', 'async', 'await']
        else:
            complexity_keywords = ['if', 'else', 'for', 'while']
        
        lines = content.split('\n')
        complexity = 1  # Base complexity
        
        for line in lines:
            for keyword in complexity_keywords:
                if f' {keyword} ' in line or f'{keyword}(' in line:
                    complexity += 1
        
        # Normalize by lines of code
        loc = len([line for line in lines if line.strip()])
        return complexity / max(loc, 1) * 100
    
    def _count_violations(self, file_path: Path, file_type: str) -> int:
        """Count style violations using appropriate linter"""
        violations = 0
        
        try:
            if file_type == "kotlin" and self._command_exists("ktlint"):
                result = subprocess.run(['ktlint', str(file_path)], 
                                     capture_output=True, text=True, timeout=30)
                violations = len(result.stdout.split('\n')) - 1 if result.returncode != 0 else 0
                
            elif file_type == "java" and self._command_exists("checkstyle"):
                checkstyle_config = self.project_root / "checkstyle.xml"
                if checkstyle_config.exists():
                    result = subprocess.run(['checkstyle', '-c', str(checkstyle_config), str(file_path)], 
                                         capture_output=True, text=True, timeout=30)
                    violations = result.stdout.count('[ERROR]')
                    
            elif file_type == "python" and self._command_exists("flake8"):
                result = subprocess.run(['flake8', str(file_path)], 
                                     capture_output=True, text=True, timeout=30)
                violations = len(result.stdout.split('\n')) - 1 if result.stdout.strip() else 0
                
        except subprocess.TimeoutExpired:
            print(f"⚠️ Linting timeout for {file_path}")
        except Exception as e:
            print(f"⚠️ Linting error for {file_path}: {e}")
            
        return violations
    
    def _command_exists(self, command: str) -> bool:
        """Check if command exists in PATH"""
        try:
            subprocess.run(['which', command], capture_output=True, check=True)
            return True
        except (subprocess.CalledProcessError, FileNotFoundError):
            return False
    
    def analyze_project(self) -> Dict[str, List[FileMetrics]]:
        """Analyze entire project with parallel processing"""
        print("🚀 Starting comprehensive project analysis...")
        
        files_by_type = self.discover_files()
        results = {}
        
        # Process each file type
        for file_type, files in files_by_type.items():
            print(f"🔍 Analyzing {len(files)} {file_type} files...")
            
            if self.config["optimization"]["enable_parallel_processing"] and len(files) > 10:
                # Parallel processing for large file sets
                with concurrent.futures.ThreadPoolExecutor(max_workers=self.max_workers) as executor:
                    future_to_file = {
                        executor.submit(self.analyze_file, file_path, file_type): file_path 
                        for file_path in files
                    }
                    
                    type_results = []
                    for future in concurrent.futures.as_completed(future_to_file):
                        result = future.result()
                        if result:
                            type_results.append(result)
                            with self.lock:
                                self.processed_files += 1
                                self.total_violations_fixed += result.violations_count
            else:
                # Sequential processing for small file sets
                type_results = []
                for file_path in files:
                    result = self.analyze_file(file_path, file_type)
                    if result:
                        type_results.append(result)
                        self.processed_files += 1
                        self.total_violations_fixed += result.violations_count
            
            results[file_type] = type_results
            print(f"✅ Completed {file_type}: {len(type_results)} files analyzed")
        
        return results
    
    def store_metrics(self, results: Dict[str, List[FileMetrics]]):
        """Store analysis results in database"""
        print("💾 Storing analysis results...")
        
        timestamp = datetime.now().isoformat()
        
        with sqlite3.connect(self.db_path) as conn:
            # Store file metrics
            for file_type, metrics_list in results.items():
                for metrics in metrics_list:
                    conn.execute("""
                        INSERT OR REPLACE INTO file_metrics 
                        (file_path, file_type, size_bytes, lines_of_code, complexity_score, 
                         violations_count, last_modified, processing_time, hash_signature, analysis_timestamp)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, (
                        metrics.file_path, metrics.file_type, metrics.size_bytes,
                        metrics.lines_of_code, metrics.complexity_score, metrics.violations_count,
                        metrics.last_modified.isoformat(), metrics.processing_time,
                        metrics.hash_signature, timestamp
                    ))
            
            # Calculate and store quality trend
            total_violations = sum(len(metrics_list) for metrics_list in results.values())
            total_complexity = sum(
                sum(m.complexity_score for m in metrics_list) 
                for metrics_list in results.values()
            )
            total_files = sum(len(metrics_list) for metrics_list in results.values())
            avg_complexity = total_complexity / max(total_files, 1)
            processing_time = time.time() - self.start_time
            processing_speed = total_files / max(processing_time, 1)
            
            # Calculate quality score (0-100)
            max_violations_per_file = 5
            violation_score = max(0, 100 - (self.total_violations_fixed / max(total_files, 1)) * 100 / max_violations_per_file)
            complexity_score = max(0, 100 - avg_complexity)
            quality_score = (violation_score + complexity_score) / 2
            
            conn.execute("""
                INSERT INTO quality_trends 
                (timestamp, total_violations, average_complexity, files_processed, processing_speed, quality_score)
                VALUES (?, ?, ?, ?, ?, ?)
            """, (timestamp, self.total_violations_fixed, avg_complexity, total_files, processing_speed, quality_score))
    
    def generate_trend_analysis(self) -> Dict:
        """Generate trend analysis from historical data"""
        print("📈 Generating trend analysis...")
        
        with sqlite3.connect(self.db_path) as conn:
            # Get recent trends (last 30 days)
            thirty_days_ago = (datetime.now() - timedelta(days=30)).isoformat()
            
            trends = conn.execute("""
                SELECT timestamp, total_violations, average_complexity, 
                       files_processed, processing_speed, quality_score
                FROM quality_trends 
                WHERE timestamp > ? 
                ORDER BY timestamp
            """, (thirty_days_ago,)).fetchall()
            
            if not trends:
                return {"message": "No historical data available"}
            
            # Calculate trend metrics
            latest = trends[-1]
            previous = trends[0] if len(trends) > 1 else latest
            
            trend_data = {
                "period_days": len(trends),
                "latest_analysis": {
                    "timestamp": latest[0],
                    "quality_score": latest[5],
                    "total_violations": latest[1],
                    "average_complexity": latest[2],
                    "files_processed": latest[3],
                    "processing_speed": latest[4]
                },
                "trends": {
                    "quality_score_change": latest[5] - previous[5],
                    "violations_change": latest[1] - previous[1],
                    "complexity_change": latest[2] - previous[2],
                    "files_change": latest[3] - previous[3]
                },
                "insights": self._generate_insights(trends)
            }
            
        return trend_data
    
    def _generate_insights(self, trends: List[Tuple]) -> List[str]:
        """Generate AI-powered insights from trend data"""
        insights = []
        
        if len(trends) < 2:
            return ["Insufficient data for trend analysis"]
        
        latest = trends[-1]
        previous = trends[0]
        
        # Quality score insights
        quality_change = latest[5] - previous[5]
        if quality_change > 5:
            insights.append("🎉 Significant quality improvement detected! Keep up the excellent work.")
        elif quality_change < -5:
            insights.append("⚠️ Quality regression detected. Consider reviewing recent changes.")
        else:
            insights.append("📊 Quality remains stable. Consider implementing new optimization strategies.")
        
        # Complexity insights
        complexity_change = latest[2] - previous[2]
        if complexity_change > 2:
            insights.append("🔍 Code complexity is increasing. Consider refactoring complex functions.")
        elif complexity_change < -2:
            insights.append("✨ Code complexity is decreasing! Excellent refactoring work.")
        
        # Performance insights
        speed_change = latest[4] - previous[4]
        if speed_change > 0.5:
            insights.append("⚡ Analysis performance improved! Optimization efforts are paying off.")
        elif speed_change < -0.5:
            insights.append("🐌 Analysis performance decreased. Consider optimizing the analysis pipeline.")
        
        return insights
    
    def generate_html_report(self, results: Dict[str, List[FileMetrics]], trend_data: Dict):
        """Generate comprehensive HTML report"""
        print("📄 Generating comprehensive HTML report...")
        
        html_content = f"""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Intelligent Code Quality Report</title>
    <style>
        * {{ margin: 0; padding: 0; box-sizing: border-box; }}
        body {{ font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; background: #f8f9fa; }}
        .container {{ max-width: 1200px; margin: 0 auto; padding: 20px; }}
        .header {{ background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 40px; border-radius: 10px; margin-bottom: 30px; text-align: center; }}
        .header h1 {{ font-size: 2.5em; margin-bottom: 10px; }}
        .header p {{ font-size: 1.2em; opacity: 0.9; }}
        .metrics-grid {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 20px; margin: 30px 0; }}
        .metric-card {{ background: white; padding: 25px; border-radius: 10px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); transition: transform 0.3s; }}
        .metric-card:hover {{ transform: translateY(-5px); }}
        .metric-card h3 {{ color: #667eea; margin-bottom: 15px; font-size: 1.3em; }}
        .metric-value {{ font-size: 2.5em; font-weight: bold; color: #2c3e50; margin-bottom: 10px; }}
        .metric-label {{ color: #7f8c8d; font-size: 0.9em; text-transform: uppercase; letter-spacing: 1px; }}
        .trend-positive {{ color: #27ae60; }}
        .trend-negative {{ color: #e74c3c; }}
        .section {{ background: white; margin: 30px 0; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }}
        .section h2 {{ color: #2c3e50; margin-bottom: 20px; font-size: 1.8em; border-bottom: 3px solid #667eea; padding-bottom: 10px; }}
        .file-type-summary {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin: 20px 0; }}
        .file-type-card {{ background: #f8f9fa; padding: 20px; border-radius: 8px; border-left: 4px solid #667eea; }}
        .insights-list {{ list-style: none; }}
        .insights-list li {{ padding: 10px 0; border-bottom: 1px solid #eee; }}
        .insights-list li:last-child {{ border-bottom: none; }}
        .progress-bar {{ background: #e0e0e0; height: 20px; border-radius: 10px; overflow: hidden; margin: 10px 0; }}
        .progress-fill {{ height: 100%; background: linear-gradient(90deg, #667eea, #764ba2); transition: width 0.3s; }}
        .timestamp {{ color: #7f8c8d; font-size: 0.9em; text-align: center; margin-top: 30px; }}
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🤖 Intelligent Code Quality Report</h1>
            <p>Advanced analysis with ML insights and trend tracking</p>
            <p>Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>
        </div>
        
        <div class="metrics-grid">
            <div class="metric-card">
                <h3>📊 Quality Score</h3>
                <div class="metric-value">{trend_data.get('latest_analysis', {}).get('quality_score', 0):.1f}</div>
                <div class="metric-label">Out of 100</div>
                <div class="progress-bar">
                    <div class="progress-fill" style="width: {trend_data.get('latest_analysis', {}).get('quality_score', 0)}%"></div>
                </div>
            </div>
            
            <div class="metric-card">
                <h3>📁 Files Analyzed</h3>
                <div class="metric-value">{self.processed_files}</div>
                <div class="metric-label">Total Files</div>
            </div>
            
            <div class="metric-card">
                <h3>🔧 Violations Fixed</h3>
                <div class="metric-value">{self.total_violations_fixed}</div>
                <div class="metric-label">Quality Issues</div>
            </div>
            
            <div class="metric-card">
                <h3>⚡ Processing Speed</h3>
                <div class="metric-value">{(self.processed_files / max(time.time() - self.start_time, 1)):.1f}</div>
                <div class="metric-label">Files/Second</div>
            </div>
        </div>
        
        <div class="section">
            <h2>📈 File Type Analysis</h2>
            <div class="file-type-summary">
"""
        
        # Add file type summaries
        for file_type, metrics_list in results.items():
            if metrics_list:
                avg_complexity = sum(m.complexity_score for m in metrics_list) / len(metrics_list)
                total_violations = sum(m.violations_count for m in metrics_list)
                
                html_content += f"""
                <div class="file-type-card">
                    <h4>{file_type.capitalize()} Files</h4>
                    <p><strong>{len(metrics_list)}</strong> files analyzed</p>
                    <p><strong>{total_violations}</strong> violations found</p>
                    <p><strong>{avg_complexity:.1f}</strong> avg complexity</p>
                </div>
                """
        
        # Add insights section
        insights = trend_data.get('insights', [])
        html_content += f"""
            </div>
        </div>
        
        <div class="section">
            <h2>🧠 AI-Powered Insights</h2>
            <ul class="insights-list">
"""
        
        for insight in insights:
            html_content += f"<li>{insight}</li>"
        
        html_content += """
            </ul>
        </div>
        
        <div class="timestamp">
            Report generated by Intelligent Code Optimizer • Advanced ML-powered analysis
        </div>
    </div>
</body>
</html>
        """
        
        # Save HTML report
        report_path = self.report_dir / f"quality_report_{datetime.now().strftime('%Y%m%d_%H%M%S')}.html"
        with open(report_path, 'w', encoding='utf-8') as f:
            f.write(html_content)
        
        print(f"✅ HTML report saved: {report_path}")
        return report_path
    
    def run_analysis(self):
        """Run complete intelligent analysis"""
        print("🚀 Starting Intelligent Code Optimization Analysis...")
        print(f"📊 Configuration: {self.max_workers} workers, {len(self.config['file_types'])} file types")
        
        try:
            # Analyze project
            results = self.analyze_project()
            
            # Store metrics
            self.store_metrics(results)
            
            # Generate trend analysis
            trend_data = self.generate_trend_analysis()
            
            # Generate reports
            if self.config["reporting"]["generate_html"]:
                html_report = self.generate_html_report(results, trend_data)
            
            # Generate JSON summary
            summary = {
                "timestamp": datetime.now().isoformat(),
                "processing_time": time.time() - self.start_time,
                "files_processed": self.processed_files,
                "violations_fixed": self.total_violations_fixed,
                "quality_score": trend_data.get('latest_analysis', {}).get('quality_score', 0),
                "file_type_summary": {
                    file_type: {
                        "count": len(metrics_list),
                        "avg_complexity": sum(m.complexity_score for m in metrics_list) / len(metrics_list) if metrics_list else 0,
                        "total_violations": sum(m.violations_count for m in metrics_list)
                    }
                    for file_type, metrics_list in results.items()
                },
                "insights": trend_data.get('insights', [])
            }
            
            # Save JSON summary
            json_path = self.report_dir / f"analysis_summary_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
            with open(json_path, 'w') as f:
                json.dump(summary, f, indent=2)
            
            # Print final summary
            print(f"\n🎉 Analysis completed successfully!")
            print(f"📊 Summary:")
            print(f"  ├── Files processed: {self.processed_files}")
            print(f"  ├── Violations found: {self.total_violations_fixed}")
            print(f"  ├── Quality score: {summary['quality_score']:.1f}/100")
            print(f"  ├── Processing time: {summary['processing_time']:.2f}s")
            print(f"  └── Reports saved in: {self.report_dir}")
            
            return summary
            
        except Exception as e:
            print(f"❌ Analysis failed: {e}")
            raise

def main():
    """Main entry point"""
    if len(sys.argv) > 1:
        project_root = sys.argv[1]
    else:
        project_root = "."
    
    optimizer = IntelligentCodeOptimizer(project_root)
    optimizer.run_analysis()

if __name__ == "__main__":
    main()