#!/usr/bin/env python3
"""
Phase 2 Implementation Validation Script
========================================

This script validates the Phase 2 implementation of the Multi-Modal Physiological 
Sensing Platform, focusing on hardware integration and Samsung S22 testing capabilities.

The script performs comprehensive validation of:
- Hardware validation controller implementation
- Phase 2 validation activity integration
- Permission system integration
- Test coverage and build system compatibility
- Code quality and documentation standards

Usage:
    python validate_phase2_implementation.py

Requirements:
- Python 3.8+
- Android project structure
- Gradle build system

Author: IRCamera Phase 2 Implementation Team
"""

import os
import sys
import json
import subprocess
import re
from pathlib import Path
from typing import Dict, List, Tuple, Optional
from dataclasses import dataclass
from datetime import datetime

@dataclass
class ValidationResult:
    """Result of a validation check"""
    category: str
    test_name: str
    passed: bool
    message: str
    details: Dict[str, any] = None

class Phase2ValidationRunner:
    """Comprehensive Phase 2 implementation validator"""
    
    def __init__(self, project_root: str):
        self.project_root = Path(project_root)
        self.app_root = self.project_root / "app"
        self.src_root = self.app_root / "src" / "main" / "java" / "com" / "topdon" / "tc001"
        self.test_root = self.app_root / "src" / "test" / "java" / "com" / "topdon" / "tc001"
        self.results: List[ValidationResult] = []
        
    def run_validation(self) -> Dict[str, any]:
        """Run complete Phase 2 validation suite"""
        print("🚀 Starting Phase 2 Implementation Validation")
        print("=" * 60)
        
        # Core implementation validation
        self.validate_hardware_validation_controller()
        self.validate_phase2_validation_activity()
        self.validate_permission_integration()
        self.validate_test_coverage()
        self.validate_build_system()
        self.validate_code_quality()
        self.validate_documentation()
        
        # Generate comprehensive report
        return self.generate_validation_report()
    
    def validate_hardware_validation_controller(self):
        """Validate HardwareValidationController implementation"""
        print("📱 Validating Hardware Validation Controller...")
        
        controller_path = self.src_root / "controller" / "HardwareValidationController.kt"
        
        # Check controller file exists
        if not controller_path.exists():
            self.results.append(ValidationResult(
                "Hardware Controller", "File Existence", False,
                f"HardwareValidationController.kt not found at {controller_path}"
            ))
            return
        
        # Read and analyze controller content
        content = controller_path.read_text()
        
        # Validate class structure
        if "class HardwareValidationController" not in content:
            self.results.append(ValidationResult(
                "Hardware Controller", "Class Definition", False,
                "HardwareValidationController class not found"
            ))
            return
        
        # Validate required methods
        required_methods = [
            "validateAllSensors",
            "validatePermissionSystem", 
            "validateRGBCamera",
            "validateThermalCamera",
            "validateGSRSensor",
            "validateMultiSensorRecording"
        ]
        
        missing_methods = []
        for method in required_methods:
            if method not in content:
                missing_methods.append(method)
        
        if missing_methods:
            self.results.append(ValidationResult(
                "Hardware Controller", "Required Methods", False,
                f"Missing methods: {', '.join(missing_methods)}"
            ))
        else:
            self.results.append(ValidationResult(
                "Hardware Controller", "Required Methods", True,
                "All required validation methods implemented"
            ))
        
        # Validate data classes
        required_data_classes = [
            "ValidationReport",
            "ValidationResult", 
            "SensorCapability",
            "ValidationSummary",
            "DeviceInfo"
        ]
        
        missing_classes = []
        for data_class in required_data_classes:
            if f"data class {data_class}" not in content:
                missing_classes.append(data_class)
        
        if missing_classes:
            self.results.append(ValidationResult(
                "Hardware Controller", "Data Classes", False,
                f"Missing data classes: {', '.join(missing_classes)}"
            ))
        else:
            self.results.append(ValidationResult(
                "Hardware Controller", "Data Classes", True,
                "All validation data classes defined"
            ))
        
        # Validate dependencies
        required_imports = [
            "androidx.lifecycle.LifecycleOwner",
            "kotlinx.coroutines",
            "com.topdon.tc001.permissions.PermissionController"
        ]
        
        missing_imports = []
        for import_item in required_imports:
            if import_item not in content:
                missing_imports.append(import_item)
        
        if missing_imports:
            self.results.append(ValidationResult(
                "Hardware Controller", "Dependencies", False,
                f"Missing imports: {', '.join(missing_imports)}"
            ))
        else:
            self.results.append(ValidationResult(
                "Hardware Controller", "Dependencies", True,
                "All required dependencies imported"
            ))
        
        # Validate Samsung S22 specific features
        s22_features = [
            "Samsung S22",
            "Android 12+",
            "SYNC_ACCURACY_THRESHOLD_MS",
            "MIN_RECORDING_DURATION_MS"
        ]
        
        s22_support = all(feature in content for feature in s22_features)
        self.results.append(ValidationResult(
            "Hardware Controller", "Samsung S22 Support", s22_support,
            "Samsung S22 specific validation features" if s22_support else "Missing S22 specific features"
        ))
        
        print("  ✅ Hardware Validation Controller analysis complete")
    
    def validate_phase2_validation_activity(self):
        """Validate Phase2ValidationActivity implementation"""
        print("🎛️ Validating Phase 2 Validation Activity...")
        
        activity_path = self.src_root / "Phase2ValidationActivity.kt"
        layout_path = self.app_root / "src" / "main" / "res" / "layout" / "activity_phase2_validation.xml"
        
        # Check activity file exists
        if not activity_path.exists():
            self.results.append(ValidationResult(
                "Phase 2 Activity", "Activity File", False,
                f"Phase2ValidationActivity.kt not found at {activity_path}"
            ))
            return
        
        # Check layout file exists  
        if not layout_path.exists():
            self.results.append(ValidationResult(
                "Phase 2 Activity", "Layout File", False,
                f"activity_phase2_validation.xml not found at {layout_path}"
            ))
        else:
            self.results.append(ValidationResult(
                "Phase 2 Activity", "Layout File", True,
                "Phase 2 validation layout file exists"
            ))
        
        # Read and analyze activity content
        content = activity_path.read_text()
        
        # Validate activity structure
        if "class Phase2ValidationActivity : AppCompatActivity" not in content:
            self.results.append(ValidationResult(
                "Phase 2 Activity", "Class Definition", False,
                "Phase2ValidationActivity class not found or incorrect inheritance"
            ))
            return
        
        # Validate UI components
        required_ui_components = [
            "startValidationButton",
            "validationProgressBar",
            "validationStatusText", 
            "validationResultsText",
            "exportReportButton"
        ]
        
        missing_components = []
        for component in required_ui_components:
            if component not in content:
                missing_components.append(component)
        
        if missing_components:
            self.results.append(ValidationResult(
                "Phase 2 Activity", "UI Components", False,
                f"Missing UI components: {', '.join(missing_components)}"
            ))
        else:
            self.results.append(ValidationResult(
                "Phase 2 Activity", "UI Components", True,
                "All required UI components defined"
            ))
        
        # Validate controller integration
        controller_integration = all(controller in content for controller in [
            "HardwareValidationController",
            "PermissionController", 
            "RecordingController"
        ])
        
        self.results.append(ValidationResult(
            "Phase 2 Activity", "Controller Integration", controller_integration,
            "All controllers integrated" if controller_integration else "Missing controller integrations"
        ))
        
        # Validate permission handling
        permission_features = [
            "requestMissingPermissions",
            "updatePermissionStatus",
            "showPermissionExplanationDialog"
        ]
        
        permission_support = all(feature in content for feature in permission_features)
        self.results.append(ValidationResult(
            "Phase 2 Activity", "Permission Handling", permission_support,
            "Comprehensive permission handling" if permission_support else "Incomplete permission handling"
        ))
        
        # Validate report generation
        report_features = [
            "exportValidationReport",
            "convertReportToJson",
            "generateValidationResultsText"
        ]
        
        report_support = all(feature in content for feature in report_features)
        self.results.append(ValidationResult(
            "Phase 2 Activity", "Report Generation", report_support,
            "Complete report generation system" if report_support else "Incomplete report generation"
        ))
        
        print("  ✅ Phase 2 Validation Activity analysis complete")
    
    def validate_permission_integration(self):
        """Validate permission system integration"""
        print("🔐 Validating Permission System Integration...")
        
        permission_controller_path = self.src_root / "permissions" / "PermissionController.kt"
        
        if not permission_controller_path.exists():
            self.results.append(ValidationResult(
                "Permissions", "Controller Exists", False,
                "PermissionController.kt not found"
            ))
            return
        
        content = permission_controller_path.read_text()
        
        # Validate Phase 2 integration methods
        phase2_methods = [
            "hasAllRequiredPermissions",
            "requestAllPermissions",
            "hasCameraPermission",
            "hasBluetoothPermissions",
            "hasStoragePermissions"
        ]
        
        missing_methods = []
        for method in phase2_methods:
            if method not in content:
                missing_methods.append(method)
        
        if missing_methods:
            self.results.append(ValidationResult(
                "Permissions", "Phase 2 Methods", False,
                f"Missing Phase 2 permission methods: {', '.join(missing_methods)}"
            ))
        else:
            self.results.append(ValidationResult(
                "Permissions", "Phase 2 Methods", True,
                "All Phase 2 permission methods available"
            ))
        
        # Validate Android version awareness
        android_version_support = all(version in content for version in [
            "Build.VERSION.SDK_INT",
            "Build.VERSION_CODES.S",
            "BLUETOOTH_SCAN",
            "BLUETOOTH_CONNECT"
        ])
        
        self.results.append(ValidationResult(
            "Permissions", "Android Version Support", android_version_support,
            "Android version-aware permissions" if android_version_support else "Missing version awareness"
        ))
        
        print("  ✅ Permission system integration analysis complete")
    
    def validate_test_coverage(self):
        """Validate test coverage for Phase 2 implementation"""
        print("🧪 Validating Test Coverage...")
        
        controller_test_path = self.test_root / "controller" / "HardwareValidationControllerTest.kt"
        
        if not controller_test_path.exists():
            self.results.append(ValidationResult(
                "Test Coverage", "Controller Tests", False,
                "HardwareValidationControllerTest.kt not found"
            ))
            return
        
        content = controller_test_path.read_text()
        
        # Validate test methods
        required_tests = [
            "testValidationControllerInitialization",
            "testValidateAllSensorsWithAllPermissionsGranted",
            "testValidateAllSensorsWithMissingPermissions",
            "testDeviceInfoCapture",
            "testSensorCapabilityDetection",
            "testPerformanceMetricsCollection"
        ]
        
        missing_tests = []
        for test in required_tests:
            if test not in content:
                missing_tests.append(test)
        
        if missing_tests:
            self.results.append(ValidationResult(
                "Test Coverage", "Controller Tests", False,
                f"Missing test methods: {', '.join(missing_tests)}"
            ))
        else:
            self.results.append(ValidationResult(
                "Test Coverage", "Controller Tests", True,
                "Comprehensive controller test coverage"
            ))
        
        # Validate test framework usage
        test_frameworks = ["io.mockk", "kotlinx.coroutines.test", "org.junit"]
        framework_usage = all(framework in content for framework in test_frameworks)
        
        self.results.append(ValidationResult(
            "Test Coverage", "Test Frameworks", framework_usage,
            "Proper test frameworks integrated" if framework_usage else "Missing test framework dependencies"
        ))
        
        print("  ✅ Test coverage analysis complete")
    
    def validate_build_system(self):
        """Validate build system compatibility"""
        print("🏗️ Validating Build System...")
        
        # Check if Gradle build succeeds (mock validation)
        try:
            # In a real implementation, this would run: ./gradlew build
            # For now, just validate build files exist
            gradle_files = [
                self.project_root / "build.gradle.kts",
                self.app_root / "build.gradle.kts",
                self.project_root / "gradle.properties"
            ]
            
            missing_files = [f for f in gradle_files if not f.exists()]
            
            if missing_files:
                self.results.append(ValidationResult(
                    "Build System", "Gradle Files", False,
                    f"Missing Gradle files: {[str(f) for f in missing_files]}"
                ))
            else:
                self.results.append(ValidationResult(
                    "Build System", "Gradle Files", True,
                    "All required Gradle files present"
                ))
            
            # Validate AndroidManifest.xml updates
            manifest_path = self.app_root / "src" / "main" / "AndroidManifest.xml"
            if manifest_path.exists():
                manifest_content = manifest_path.read_text()
                
                # Check for Phase 2 activity registration
                if "Phase2ValidationActivity" in manifest_content:
                    self.results.append(ValidationResult(
                        "Build System", "Manifest Registration", True,
                        "Phase2ValidationActivity registered in manifest"
                    ))
                else:
                    self.results.append(ValidationResult(
                        "Build System", "Manifest Registration", False,
                        "Phase2ValidationActivity not registered in AndroidManifest.xml"
                    ))
            else:
                self.results.append(ValidationResult(
                    "Build System", "Manifest File", False,
                    "AndroidManifest.xml not found"
                ))
                
        except Exception as e:
            self.results.append(ValidationResult(
                "Build System", "Validation Error", False,
                f"Build system validation failed: {str(e)}"
            ))
        
        print("  ✅ Build system analysis complete")
    
    def validate_code_quality(self):
        """Validate code quality standards"""
        print("📝 Validating Code Quality...")
        
        # Check KDoc documentation
        controller_path = self.src_root / "controller" / "HardwareValidationController.kt"
        if controller_path.exists():
            content = controller_path.read_text()
            
            # Count KDoc comments
            kdoc_count = content.count("/**")
            class_count = content.count("class ") + content.count("data class ")
            
            if kdoc_count >= class_count:
                self.results.append(ValidationResult(
                    "Code Quality", "KDoc Documentation", True,
                    f"Adequate KDoc coverage ({kdoc_count} docs for {class_count} classes)"
                ))
            else:
                self.results.append(ValidationResult(
                    "Code Quality", "KDoc Documentation", False,
                    f"Insufficient KDoc coverage ({kdoc_count} docs for {class_count} classes)"
                ))
        
        # Validate Kotlin style compliance
        style_issues = []
        
        for kt_file in self.src_root.rglob("*.kt"):
            if "Phase2" in kt_file.name or "HardwareValidation" in kt_file.name:
                content = kt_file.read_text()
                
                # Check for common style issues
                if "\t" in content:
                    style_issues.append(f"{kt_file.name}: Uses tabs instead of spaces")
                if re.search(r"class\s+\w+\s*\{", content):
                    style_issues.append(f"{kt_file.name}: Missing space before opening brace")
        
        if style_issues:
            self.results.append(ValidationResult(
                "Code Quality", "Kotlin Style", False,
                f"Style issues found: {len(style_issues)} issues"
            ))
        else:
            self.results.append(ValidationResult(
                "Code Quality", "Kotlin Style", True,
                "Kotlin style guidelines followed"
            ))
        
        print("  ✅ Code quality analysis complete")
    
    def validate_documentation(self):
        """Validate documentation completeness"""
        print("📚 Validating Documentation...")
        
        # Check for Phase 2 documentation
        phase2_docs = [
            self.project_root / "PHASE_2_IMPLEMENTATION_PLAN.md",
            self.src_root / "permissions" / "README.md"
        ]
        
        missing_docs = []
        for doc in phase2_docs:
            if not doc.exists():
                missing_docs.append(doc.name)
        
        if missing_docs:
            self.results.append(ValidationResult(
                "Documentation", "Required Documents", False,
                f"Missing documentation: {', '.join(missing_docs)}"
            ))
        else:
            self.results.append(ValidationResult(
                "Documentation", "Required Documents", True,
                "All required Phase 2 documentation present"
            ))
        
        # Validate documentation content quality
        plan_path = self.project_root / "PHASE_2_IMPLEMENTATION_PLAN.md"
        if plan_path.exists():
            content = plan_path.read_text()
            
            required_sections = [
                "Phase 2: Hardware Integration & Testing",
                "Samsung S22 Device Validation",
                "Sensor Hardware Validation",
                "Network Testing"
            ]
            
            missing_sections = []
            for section in required_sections:
                if section not in content:
                    missing_sections.append(section)
            
            if missing_sections:
                self.results.append(ValidationResult(
                    "Documentation", "Plan Completeness", False,
                    f"Missing sections in implementation plan: {', '.join(missing_sections)}"
                ))
            else:
                self.results.append(ValidationResult(
                    "Documentation", "Plan Completeness", True,
                    "Implementation plan covers all required sections"
                ))
        
        print("  ✅ Documentation analysis complete")
    
    def generate_validation_report(self) -> Dict[str, any]:
        """Generate comprehensive validation report"""
        print("\n" + "=" * 60)
        print("📊 Generating Validation Report...")
        
        # Calculate summary statistics
        total_tests = len(self.results)
        passed_tests = sum(1 for r in self.results if r.passed)
        failed_tests = total_tests - passed_tests
        success_rate = (passed_tests / total_tests * 100) if total_tests > 0 else 0
        
        # Group results by category
        categories = {}
        for result in self.results:
            if result.category not in categories:
                categories[result.category] = {"passed": 0, "failed": 0, "tests": []}
            
            categories[result.category]["tests"].append(result)
            if result.passed:
                categories[result.category]["passed"] += 1
            else:
                categories[result.category]["failed"] += 1
        
        # Generate report
        report = {
            "timestamp": datetime.now().isoformat(),
            "phase": "Phase 2 - Hardware Integration & Testing",
            "summary": {
                "total_tests": total_tests,
                "passed_tests": passed_tests,
                "failed_tests": failed_tests,
                "success_rate": round(success_rate, 2),
                "overall_status": "PASSED" if failed_tests == 0 else "FAILED"
            },
            "categories": {
                cat: {
                    "passed": stats["passed"],
                    "failed": stats["failed"],
                    "tests": [
                        {
                            "test_name": t.test_name,
                            "passed": t.passed,
                            "message": t.message
                        }
                        for t in stats["tests"]
                    ]
                }
                for cat, stats in categories.items()
            },
            "detailed_results": [
                {
                    "category": r.category,
                    "test_name": r.test_name,
                    "passed": r.passed,
                    "message": r.message,
                    "details": r.details or {}
                }
                for r in self.results
            ]
        }
        
        # Print summary
        print(f"\n🎯 PHASE 2 VALIDATION SUMMARY")
        print(f"Total Tests: {total_tests}")
        print(f"Passed: {passed_tests}")
        print(f"Failed: {failed_tests}")
        print(f"Success Rate: {success_rate:.1f}%")
        print(f"Overall Status: {report['summary']['overall_status']}")
        
        # Print category breakdown
        print(f"\n📋 CATEGORY BREAKDOWN:")
        for category, stats in categories.items():
            status = "✅" if stats["failed"] == 0 else "❌"
            print(f"{status} {category}: {stats['passed']}/{stats['passed'] + stats['failed']} passed")
        
        # Print failed tests details
        failed_results = [r for r in self.results if not r.passed]
        if failed_results:
            print(f"\n❌ FAILED TESTS:")
            for result in failed_results:
                print(f"  • {result.category} - {result.test_name}: {result.message}")
        else:
            print(f"\n✅ ALL TESTS PASSED!")
        
        # Save report to file
        report_path = self.project_root / "phase2_validation_report.json"
        with open(report_path, 'w') as f:
            json.dump(report, f, indent=2)
        
        print(f"\n📄 Detailed report saved to: {report_path}")
        print("=" * 60)
        
        return report

def main():
    """Main validation entry point"""
    if len(sys.argv) > 1:
        project_root = sys.argv[1]
    else:
        project_root = os.getcwd()
    
    print(f"🏠 Project root: {project_root}")
    
    validator = Phase2ValidationRunner(project_root)
    report = validator.run_validation()
    
    # Return appropriate exit code
    sys.exit(0 if report["summary"]["overall_status"] == "PASSED" else 1)

if __name__ == "__main__":
    main()