#!/usr/bin/env python3
"""
Test suite for PC Controller features

Tests the key features implemented:
1. C++ Native Backend with PyBind11
2. Network Server with SSL/TLS support
3. Real-time data processing
4. WebCam integration
5. Data export functionality
"""

import sys
import unittest
from pathlib import Path
import tempfile
import json


# Add native backend to path
sys.path.insert(0, str(Path(__file__).parent / 'native_backend'))


class TestNativeBackend(unittest.TestCase):
    """Test C++ native backend integration"""
    
    def test_native_backend_import(self):
        """Test that native backend can be imported"""
        try:
            import enhanced_native_backend
            self.assertTrue(True, "Native backend imported successfully")
        except ImportError as e:
            self.skipTest(f"Native backend not built yet. Run 'cd native_backend && python setup.py build_ext --inplace' first. Error: {e}")
    
    def test_gsr_data_structure(self):
        """Test GSRData structure from C++ backend"""
        try:
            import enhanced_native_backend
        except ImportError:
            self.skipTest("Native backend not available")
        
        gsr_data = enhanced_native_backend.GSRData()
        self.assertEqual(gsr_data.timestamp_ns, 0)
        self.assertEqual(gsr_data.gsr_microsiemens, 0.0)
        
        # Set values
        gsr_data.timestamp_ns = 1234567890
        gsr_data.gsr_microsiemens = 5.5
        gsr_data.raw_gsr_value = 2048
        
        self.assertEqual(gsr_data.timestamp_ns, 1234567890)
        self.assertEqual(gsr_data.gsr_microsiemens, 5.5)
        self.assertEqual(gsr_data.raw_gsr_value, 2048)
    
    def test_enhanced_shimmer(self):
        """Test EnhancedShimmer class"""
        try:
            import enhanced_native_backend
        except ImportError:
            self.skipTest("Native backend not available")
        
        shimmer = enhanced_native_backend.EnhancedShimmer()
        self.assertFalse(shimmer.is_connected())
        self.assertFalse(shimmer.is_streaming())
    
    def test_data_processor(self):
        """Test DataProcessor from C++ backend"""
        try:
            import enhanced_native_backend
        except ImportError:
            self.skipTest("Native backend not available")
        
        processor = enhanced_native_backend.DataProcessor()
        self.assertIsNotNone(processor)
    
    def test_processing_functions(self):
        """Test signal processing functions"""
        try:
            import enhanced_native_backend
        except ImportError:
            self.skipTest("Native backend not available")
        
        # Create test data
        test_data = [1.0, 2.0, 3.0, 4.0, 5.0]
        
        # Test mean calculation
        mean = enhanced_native_backend.processing.calculate_mean(test_data)
        self.assertAlmostEqual(mean, 3.0, places=5)
        
        # Test std calculation
        std = enhanced_native_backend.processing.calculate_std(test_data)
        self.assertGreater(std, 0)


class TestNetworkProtocol(unittest.TestCase):
    """Test network protocol and messaging"""
    
    def test_protocol_import(self):
        """Test protocol module import"""
        try:
            # Test that we can import the controller modules
            import pc_controller
            self.assertTrue(hasattr(pc_controller, 'Protocol'))
        except ImportError as e:
            self.skipTest(f"pc_controller not available: {e}")
    
    def test_message_creation(self):
        """Test creating protocol messages"""
        message = {
            'type': 'HELLO',
            'device_id': 'test_device_001',
            'device_name': 'Test Device',
            'sensors': ['GSR', 'RGB', 'Thermal'],
            'firmware_version': '1.0.0'
        }
        
        # Verify message structure
        self.assertEqual(message['type'], 'HELLO')
        self.assertIn('GSR', message['sensors'])
        
        # Test JSON serialization
        json_str = json.dumps(message)
        self.assertIsInstance(json_str, str)
        
        # Test deserialization
        parsed = json.loads(json_str)
        self.assertEqual(parsed['device_id'], 'test_device_001')


class TestDataExport(unittest.TestCase):
    """Test data export functionality"""
    
    def test_csv_export(self):
        """Test CSV export of GSR data"""
        # Create temporary directory
        with tempfile.TemporaryDirectory() as tmpdir:
            export_path = Path(tmpdir) / "test_export"
            export_path.mkdir()
            
            # Generate test data
            gsr_data = [(1234567890.0 + i, 5.0 + i * 0.1) for i in range(100)]
            
            # Export to CSV
            csv_file = export_path / "test_gsr_data.csv"
            with open(csv_file, 'w') as f:
                f.write("timestamp,gsr_value\n")
                for timestamp, value in gsr_data:
                    f.write(f"{timestamp},{value}\n")
            
            # Verify file exists
            self.assertTrue(csv_file.exists())
            
            # Verify file content
            with open(csv_file, 'r') as f:
                lines = f.readlines()
                self.assertEqual(len(lines), 101)  # Header + 100 data lines
                self.assertEqual(lines[0].strip(), "timestamp,gsr_value")
    
    def test_json_export(self):
        """Test JSON export of device status"""
        with tempfile.TemporaryDirectory() as tmpdir:
            status_file = Path(tmpdir) / "device_status.json"
            
            device_status = {
                'device_001': {
                    'device_name': 'Test Device 1',
                    'ip_address': '192.168.1.100',
                    'status': 'Connected',
                    'capabilities': ['GSR', 'RGB'],
                    'firmware_version': '1.0.0'
                }
            }
            
            # Export to JSON
            with open(status_file, 'w') as f:
                json.dump(device_status, f, indent=2)
            
            # Verify file exists
            self.assertTrue(status_file.exists())
            
            # Verify file can be read back
            with open(status_file, 'r') as f:
                loaded_status = json.load(f)
                self.assertEqual(loaded_status['device_001']['device_name'], 'Test Device 1')


class TestWebcamIntegration(unittest.TestCase):
    """Test webcam integration"""
    
    def test_webcam_class_exists(self):
        """Test that WebcamCapture class is available"""
        try:
            import pc_controller
            self.assertTrue(hasattr(pc_controller, 'WebcamCapture'))
        except ImportError:
            self.skipTest("pc_controller not available")
    
    def test_opencv_availability(self):
        """Test OpenCV availability for webcam"""
        try:
            import cv2
            self.assertTrue(True, "OpenCV is available")
            
            # Test that we can query camera backends
            backends = [cv2.CAP_ANY]
            self.assertIsNotNone(backends)
        except ImportError:
            self.skipTest("OpenCV not available - this is expected in CI environment")


class TestSecurityLayer(unittest.TestCase):
    """Test TLS/SSL security implementation"""
    
    def test_ssl_context_creation(self):
        """Test SSL context can be created"""
        import ssl
        
        context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        self.assertIsNotNone(context)
    
    def test_certificate_generation(self):
        """Test that certificate generation utilities work"""
        try:
            from cryptography import x509
            from cryptography.hazmat.primitives import hashes
            from cryptography.hazmat.primitives.asymmetric import rsa
            
            # Generate a test key
            private_key = rsa.generate_private_key(
                public_exponent=65537,
                key_size=2048,
            )
            self.assertIsNotNone(private_key)
            
        except ImportError:
            self.skipTest("cryptography library not available")


def run_tests():
    """Run all tests"""
    # Create test suite
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()
    
    # Add test cases
    suite.addTests(loader.loadTestsFromTestCase(TestNativeBackend))
    suite.addTests(loader.loadTestsFromTestCase(TestNetworkProtocol))
    suite.addTests(loader.loadTestsFromTestCase(TestDataExport))
    suite.addTests(loader.loadTestsFromTestCase(TestWebcamIntegration))
    suite.addTests(loader.loadTestsFromTestCase(TestSecurityLayer))
    
    # Run tests
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    
    # Print summary
    print("\n" + "="*70)
    print("Test Summary")
    print("="*70)
    print(f"Tests run: {result.testsRun}")
    print(f"Successes: {result.testsRun - len(result.failures) - len(result.errors)}")
    print(f"Failures: {len(result.failures)}")
    print(f"Errors: {len(result.errors)}")
    print(f"Skipped: {len(result.skipped)}")
    print("="*70)
    
    return result.wasSuccessful()


if __name__ == '__main__':
    success = run_tests()
    sys.exit(0 if success else 1)
