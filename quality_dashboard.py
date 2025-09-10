#!/usr/bin/env python3
"""
Quality Dashboard - Real-time Quality Monitoring and Visualization
Provides live quality metrics, trends, and interactive dashboards
"""

import argparse
import json
import sqlite3
import threading
import time
import urllib.parse as urlparse
import webbrowser
from datetime import datetime, timedelta
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path


class QualityDashboardServer(BaseHTTPRequestHandler):
    """HTTP server for the quality dashboard"""

    def do_GET(self):
        """Handle GET requests"""
        parsed_path = urlparse.urlparse(self.path)

        if parsed_path.path == "/":
            self.serve_dashboard()
        elif parsed_path.path == "/api/metrics":
            self.serve_metrics_api()
        elif parsed_path.path == "/api/trends":
            self.serve_trends_api()
        elif parsed_path.path.startswith("/api/"):
            self.serve_api_endpoint(parsed_path.path)
        else:
            self.send_error(404)

    def serve_dashboard(self):
        """Serve the main dashboard HTML"""
        html_content = self.generate_dashboard_html()

        self.send_response(200)
        self.send_header("Content-type", "text/html")
        self.end_headers()
        self.wfile.write(html_content.encode())

    def serve_metrics_api(self):
        """Serve current metrics as JSON API"""
        try:
            # Load latest metrics
            dashboard_path = Path("quality_reports/quality_dashboard.json")
            if dashboard_path.exists():
                with open(dashboard_path) as f:
                    metrics = json.load(f)
            else:
                metrics = {"error": "No metrics available"}

            self.send_response(200)
            self.send_header("Content-type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(json.dumps(metrics, indent=2).encode())
        except Exception as e:
            self.send_error(500, str(e))

    def serve_trends_api(self):
        """Serve trend data as JSON API"""
        try:
            # Connect to database and get trends
            db_path = "quality_metrics.db"
            if Path(db_path).exists():
                conn = sqlite3.connect(db_path)
                cursor = conn.cursor()

                # Get last 30 days of data
                cutoff_date = (datetime.now() - timedelta(days=30)).isoformat()
                cursor.execute(
                    """
                    SELECT timestamp, overall_score, security_score, 
                           complexity_score, coverage_score, style_score
                    FROM quality_metrics 
                    WHERE timestamp > ? 
                    ORDER BY timestamp
                """,
                    (cutoff_date,),
                )

                rows = cursor.fetchall()
                conn.close()

                trends = {
                    "labels": [row[0] for row in rows],
                    "overall": [row[1] for row in rows],
                    "security": [row[2] for row in rows],
                    "complexity": [row[3] for row in rows],
                    "coverage": [row[4] for row in rows],
                    "style": [row[5] for row in rows],
                }
            else:
                trends = {"error": "No trend data available"}

            self.send_response(200)
            self.send_header("Content-type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(json.dumps(trends, indent=2).encode())
        except Exception as e:
            self.send_error(500, str(e))

    def serve_api_endpoint(self, path):
        """Serve additional API endpoints"""
        if path == "/api/health":
            health_data = {
                "status": "healthy",
                "timestamp": datetime.now().isoformat(),
                "version": "1.0.0",
            }

            self.send_response(200)
            self.send_header("Content-type", "application/json")
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(json.dumps(health_data).encode())
        else:
            self.send_error(404)

    def generate_dashboard_html(self):
        """Generate the interactive dashboard HTML"""
        return """
<!DOCTYPE html>
<html>
<head>
    <title>Enterprise Quality Dashboard</title>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        
        body {
            font-family: 'Segoe UI', system-ui, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            color: #333;
        }
        
        .header {
            background: rgba(255,255,255,0.95);
            padding: 20px;
            text-align: center;
            backdrop-filter: blur(10px);
            box-shadow: 0 2px 20px rgba(0,0,0,0.1);
        }
        
        .title {
            font-size: 2.5em;
            font-weight: 900;
            background: linear-gradient(45deg, #667eea, #764ba2);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            margin-bottom: 10px;
        }
        
        .subtitle {
            color: #666;
            font-size: 1.1em;
        }
        
        .container {
            max-width: 1400px;
            margin: 0 auto;
            padding: 20px;
        }
        
        .dashboard-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 20px;
            margin: 20px 0;
        }
        
        .metrics-overview {
            grid-column: 1 / -1;
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
            margin-bottom: 30px;
        }
        
        .metric-card {
            background: white;
            padding: 25px;
            border-radius: 15px;
            text-align: center;
            box-shadow: 0 8px 25px rgba(0,0,0,0.1);
            transition: transform 0.3s ease, box-shadow 0.3s ease;
        }
        
        .metric-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 12px 35px rgba(0,0,0,0.15);
        }
        
        .metric-title {
            font-size: 0.9em;
            color: #666;
            margin-bottom: 10px;
            text-transform: uppercase;
            letter-spacing: 1px;
        }
        
        .metric-value {
            font-size: 2.5em;
            font-weight: 900;
            margin: 10px 0;
        }
        
        .metric-change {
            font-size: 0.85em;
            font-weight: 600;
        }
        
        .score-excellent { color: #4CAF50; }
        .score-good { color: #2196F3; }
        .score-warning { color: #FF9800; }
        .score-poor { color: #f44336; }
        
        .change-positive { color: #4CAF50; }
        .change-negative { color: #f44336; }
        .change-neutral { color: #666; }
        
        .chart-container {
            background: white;
            padding: 25px;
            border-radius: 15px;
            box-shadow: 0 8px 25px rgba(0,0,0,0.1);
            position: relative;
            height: 400px;
        }
        
        .chart-title {
            font-size: 1.2em;
            font-weight: 600;
            margin-bottom: 20px;
            color: #333;
        }
        
        .status-indicator {
            display: inline-block;
            padding: 8px 20px;
            border-radius: 25px;
            font-weight: 600;
            font-size: 0.9em;
            text-transform: uppercase;
            letter-spacing: 1px;
        }
        
        .status-passed {
            background: linear-gradient(45deg, #4CAF50, #45a049);
            color: white;
        }
        
        .status-failed {
            background: linear-gradient(45deg, #f44336, #da190b);
            color: white;
        }
        
        .loading {
            display: flex;
            align-items: center;
            justify-content: center;
            height: 200px;
            color: #666;
            font-size: 1.1em;
        }
        
        .refresh-info {
            text-align: center;
            color: rgba(255,255,255,0.8);
            margin: 20px 0;
            font-size: 0.9em;
        }
        
        @media (max-width: 768px) {
            .dashboard-grid {
                grid-template-columns: 1fr;
            }
            
            .metrics-overview {
                grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
            }
        }
    </style>
</head>
<body>
    <div class="header">
        <div class="title">🏆 Enterprise Quality Dashboard</div>
        <div class="subtitle">Real-time Code Quality Monitoring</div>
        <div id="status-indicator" class="status-indicator">Loading...</div>
    </div>
    
    <div class="container">
        <div class="metrics-overview" id="metrics-overview">
            <div class="loading">Loading metrics...</div>
        </div>
        
        <div class="dashboard-grid">
            <div class="chart-container">
                <div class="chart-title">📈 Quality Trends (30 days)</div>
                <canvas id="trendsChart"></canvas>
            </div>
            
            <div class="chart-container">
                <div class="chart-title">📊 Current Quality Breakdown</div>
                <canvas id="metricsChart"></canvas>
            </div>
        </div>
        
        <div class="refresh-info">
            🔄 Dashboard auto-refreshes every 30 seconds | Last updated: <span id="last-updated">Never</span>
        </div>
    </div>

    <script>
        let trendsChart, metricsChart;
        
        async function loadDashboard() {
            try {
                // Load current metrics
                const metricsResponse = await fetch('/api/metrics');
                const metrics = await metricsResponse.json();
                
                // Load trends
                const trendsResponse = await fetch('/api/trends');
                const trends = await trendsResponse.json();
                
                updateMetricsOverview(metrics);
                updateTrendsChart(trends);
                updateMetricsChart(metrics);
                updateStatusIndicator(metrics);
                
                document.getElementById('last-updated').textContent = new Date().toLocaleTimeString();
                
            } catch (error) {
                console.error('Error loading dashboard:', error);
                document.getElementById('metrics-overview').innerHTML = 
                    '<div style="text-align: center; color: #f44336;">❌ Error loading metrics</div>';
            }
        }
        
        function updateMetricsOverview(metrics) {
            const container = document.getElementById('metrics-overview');
            
            if (metrics.error) {
                container.innerHTML = '<div style="text-align: center; color: #f44336;">❌ ' + metrics.error + '</div>';
                return;
            }
            
            const metricsData = [
                { title: 'Overall Score', value: metrics.overall_score || 0, suffix: '/100' },
                { title: 'Security', value: metrics.metrics?.security?.score || 0, suffix: '/100' },
                { title: 'Complexity', value: metrics.metrics?.complexity?.score || 0, suffix: '/100' },
                { title: 'Coverage', value: metrics.metrics?.coverage?.score || 0, suffix: '/100' },
                { title: 'Style', value: metrics.metrics?.style?.score || 0, suffix: '/100' },
                { title: 'Duplication', value: metrics.metrics?.duplication?.score || 0, suffix: '/100' }
            ];
            
            container.innerHTML = metricsData.map(metric => {
                const scoreClass = getScoreClass(metric.value);
                return `
                    <div class="metric-card">
                        <div class="metric-title">${metric.title}</div>
                        <div class="metric-value ${scoreClass}">${metric.value}${metric.suffix}</div>
                        <div class="metric-change change-neutral">●●●</div>
                    </div>
                `;
            }).join('');
        }
        
        function updateStatusIndicator(metrics) {
            const indicator = document.getElementById('status-indicator');
            const qualityGate = metrics.quality_gate || 'UNKNOWN';
            
            indicator.textContent = qualityGate;
            indicator.className = 'status-indicator ' + 
                (qualityGate === 'PASSED' ? 'status-passed' : 'status-failed');
        }
        
        function updateTrendsChart(trends) {
            const ctx = document.getElementById('trendsChart').getContext('2d');
            
            if (trendsChart) {
                trendsChart.destroy();
            }
            
            if (trends.error) {
                return;
            }
            
            trendsChart = new Chart(ctx, {
                type: 'line',
                data: {
                    labels: trends.labels?.map(label => new Date(label).toLocaleDateString()) || [],
                    datasets: [
                        {
                            label: 'Overall',
                            data: trends.overall || [],
                            borderColor: '#667eea',
                            backgroundColor: 'rgba(102, 126, 234, 0.1)',
                            fill: true,
                            tension: 0.4
                        },
                        {
                            label: 'Security',
                            data: trends.security || [],
                            borderColor: '#4CAF50',
                            backgroundColor: 'rgba(76, 175, 80, 0.1)',
                            fill: false,
                            tension: 0.4
                        },
                        {
                            label: 'Coverage',
                            data: trends.coverage || [],
                            borderColor: '#2196F3',
                            backgroundColor: 'rgba(33, 150, 243, 0.1)',
                            fill: false,
                            tension: 0.4
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    scales: {
                        y: {
                            beginAtZero: true,
                            max: 100
                        }
                    },
                    plugins: {
                        legend: {
                            position: 'top'
                        }
                    }
                }
            });
        }
        
        function updateMetricsChart(metrics) {
            const ctx = document.getElementById('metricsChart').getContext('2d');
            
            if (metricsChart) {
                metricsChart.destroy();
            }
            
            if (metrics.error) {
                return;
            }
            
            const metricsData = metrics.metrics || {};
            const labels = Object.keys(metricsData).map(key => key.charAt(0).toUpperCase() + key.slice(1));
            const scores = Object.values(metricsData).map(metric => metric.score || 0);
            
            metricsChart = new Chart(ctx, {
                type: 'doughnut',
                data: {
                    labels: labels,
                    datasets: [{
                        data: scores,
                        backgroundColor: [
                            '#667eea',
                            '#4CAF50',
                            '#2196F3',
                            '#FF9800',
                            '#9C27B0',
                            '#607D8B'
                        ],
                        borderWidth: 2,
                        borderColor: '#fff'
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            position: 'bottom'
                        }
                    }
                }
            });
        }
        
        function getScoreClass(score) {
            if (score >= 90) return 'score-excellent';
            if (score >= 80) return 'score-good';
            if (score >= 70) return 'score-warning';
            return 'score-poor';
        }
        
        // Initialize dashboard
        loadDashboard();
        
        // Auto-refresh every 30 seconds
        setInterval(loadDashboard, 30000);
    </script>
</body>
</html>
        """


def start_dashboard_server(port=8080):
    """Start the quality dashboard server"""
    server_address = ("", port)
    httpd = HTTPServer(server_address, QualityDashboardServer)

    print(f"🚀 Quality Dashboard starting on http://localhost:{port}")
    print(f"📊 Access your real-time quality metrics at the URL above")
    print(f"🔄 Dashboard will auto-refresh every 30 seconds")
    print(f"⚡ Press Ctrl+C to stop the server")

    try:
        # Open browser automatically
        webbrowser.open(f"http://localhost:{port}")
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n🛑 Shutting down Quality Dashboard...")
        httpd.shutdown()


def main():
    parser = argparse.ArgumentParser(description="Quality Dashboard Server")
    parser.add_argument(
        "--port", type=int, default=8080, help="Port to run the dashboard on"
    )
    parser.add_argument(
        "--no-browser", action="store_true", help="Don't automatically open browser"
    )

    args = parser.parse_args()

    if not args.no_browser:
        # Start server in a separate thread so we can open browser
        server_thread = threading.Thread(
            target=start_dashboard_server, args=(args.port,)
        )
        server_thread.daemon = True
        server_thread.start()

        # Give server time to start
        time.sleep(1)

        try:
            server_thread.join()
        except KeyboardInterrupt:
            print("\n🛑 Shutting down Quality Dashboard...")
    else:
        start_dashboard_server(args.port)


if __name__ == "__main__":
    main()
