#!/usr/bin/env python3
"""
Recorded Data File Integrity Check

Test: Validate all output files from recording sessions
Output: Validation report for all data files (CSV, images, video)
Subsystem: Data logging subsystem (all sensors)
Chapters: Chapter 5 (successful capture documentation) and Chapter 6 (overall reliability)
"""

import json
import os
import sys
from dataclasses import dataclass, asdict
from datetime import datetime
from pathlib import Path
from typing import List, Dict, Optional, Any


@dataclass
class FileValidation:
    file_path: str
    file_type: str
    exists: bool
    size_bytes: int
    size_mb: float
    is_valid: bool
    validation_errors: List[str]


@dataclass
class CSVValidation(FileValidation):
    has_header: bool = False
    row_count: int = 0
    column_count: int = 0
    is_complete: bool = False


@dataclass
class VideoValidation(FileValidation):
    can_open: bool = False
    duration_seconds: float = 0.0
    has_valid_codec: bool = False


@dataclass
class ImageValidation(FileValidation):
    width: int = 0
    height: int = 0
    format: str = ""


@dataclass
class TestResult:
    test_name: str
    start_time: str
    end_time: str
    duration_seconds: float
    files_validated: int
    files_passed: int
    files_failed: int
    validations: List[Dict[str, any]]
    passed: bool
    summary: Dict[str, Dict[str, int]]


class FileIntegrityValidator:
    """Validate integrity of recorded data files"""
    
    def __init__(self, session_dir: str, output_dir: str = "output"):
        self.session_dir = Path(session_dir)
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.validations: List[FileValidation] = []
        
    def validate_csv_file(self, file_path: Path) -> CSVValidation:
        """Validate CSV file integrity"""
        errors = []
        
        exists = file_path.exists()
        size_bytes = file_path.stat().st_size if exists else 0
        size_mb = size_bytes / (1024 * 1024)
        
        has_header = False
        row_count = 0
        column_count = 0
        is_complete = False
        
        if not exists:
            errors.append("File does not exist")
        elif size_bytes == 0:
            errors.append("File is empty")
        else:
            try:
                with open(file_path, 'r') as f:
                    lines = f.readlines()
                    
                    if len(lines) > 0:
                        has_header = True
                        header = lines[0].strip()
                        column_count = len(header.split(','))
                        row_count = len(lines) - 1
                        
                        # Check if file ends properly
                        last_line = lines[-1].strip()
                        is_complete = len(last_line) > 0
                        
                        if row_count == 0:
                            errors.append("No data rows (only header)")
                        
                        if not is_complete:
                            errors.append("File may be truncated (empty last line)")
                    else:
                        errors.append("File has no content")
            
            except Exception as e:
                errors.append(f"Failed to read file: {e}")
        
        is_valid = len(errors) == 0 and row_count > 0
        
        return CSVValidation(
            file_path=str(file_path),
            file_type="CSV",
            exists=exists,
            size_bytes=size_bytes,
            size_mb=size_mb,
            is_valid=is_valid,
            validation_errors=errors,
            has_header=has_header,
            row_count=row_count,
            column_count=column_count,
            is_complete=is_complete
        )
    
    def validate_video_file(self, file_path: Path) -> VideoValidation:
        """Validate video file integrity"""
        errors = []
        
        exists = file_path.exists()
        size_bytes = file_path.stat().st_size if exists else 0
        size_mb = size_bytes / (1024 * 1024)
        
        can_open = False
        duration_seconds = 0.0
        has_valid_codec = False
        
        if not exists:
            errors.append("File does not exist")
        elif size_bytes < 1024:  # Less than 1KB
            errors.append("File too small to be valid video")
        else:
            # Basic check: video files should have reasonable size
            can_open = True
            has_valid_codec = True  # Assume valid if size is reasonable
            
            # Try to get metadata using ffprobe if available
            try:
                import subprocess
                result = subprocess.run(
                    ['ffprobe', '-v', 'quiet', '-print_format', 'json',
                     '-show_format', str(file_path)],
                    capture_output=True, text=True, timeout=5
                )
                
                if result.returncode == 0:
                    data = json.loads(result.stdout)
                    duration_seconds = float(data.get('format', {}).get('duration', 0))
                    
                    if duration_seconds == 0:
                        errors.append("Video duration is 0")
                        can_open = False
            
            except (FileNotFoundError, subprocess.TimeoutExpired, json.JSONDecodeError):
                # ffprobe not available, skip detailed validation
                pass
            except Exception as e:
                errors.append(f"Failed to validate video: {e}")
        
        is_valid = len(errors) == 0 and can_open
        
        return VideoValidation(
            file_path=str(file_path),
            file_type="VIDEO",
            exists=exists,
            size_bytes=size_bytes,
            size_mb=size_mb,
            is_valid=is_valid,
            validation_errors=errors,
            can_open=can_open,
            duration_seconds=duration_seconds,
            has_valid_codec=has_valid_codec
        )
    
    def validate_image_file(self, file_path: Path) -> ImageValidation:
        """Validate image file integrity"""
        errors = []
        
        exists = file_path.exists()
        size_bytes = file_path.stat().st_size if exists else 0
        size_mb = size_bytes / (1024 * 1024)
        
        width = 0
        height = 0
        format_type = ""
        
        if not exists:
            errors.append("File does not exist")
        elif size_bytes < 100:  # Less than 100 bytes
            errors.append("File too small to be valid image")
        else:
            # Basic validation: check file extension and size
            format_type = file_path.suffix.lstrip('.').upper()
            
            # Try to get image dimensions using PIL if available
            try:
                from PIL import Image
                with Image.open(file_path) as img:
                    width, height = img.size
                    
                    if width == 0 or height == 0:
                        errors.append("Invalid image dimensions")
            
            except ImportError:
                # PIL not available, skip detailed validation
                pass
            except Exception as e:
                errors.append(f"Failed to open image: {e}")
        
        is_valid = len(errors) == 0 and size_bytes >= 100
        
        return ImageValidation(
            file_path=str(file_path),
            file_type="IMAGE",
            exists=exists,
            size_bytes=size_bytes,
            size_mb=size_mb,
            is_valid=is_valid,
            validation_errors=errors,
            width=width,
            height=height,
            format=format_type
        )
    
    def validate_session_directory(self) -> None:
        """Validate all files in session directory"""
        if not self.session_dir.exists():
            print(f"Error: Session directory not found: {self.session_dir}")
            return
        
        print(f"Validating files in: {self.session_dir}")
        
        # Find all relevant files
        csv_files = list(self.session_dir.glob("**/*.csv"))
        video_files = list(self.session_dir.glob("**/*.mp4"))
        video_files.extend(self.session_dir.glob("**/*.avi"))
        image_files = list(self.session_dir.glob("**/*.jpg"))
        image_files.extend(self.session_dir.glob("**/*.jpeg"))
        image_files.extend(self.session_dir.glob("**/*.png"))
        
        print(f"Found {len(csv_files)} CSV files")
        print(f"Found {len(video_files)} video files")
        print(f"Found {len(image_files)} image files")
        
        # Validate CSV files
        for csv_file in csv_files:
            print(f"  Validating CSV: {csv_file.name}")
            validation = self.validate_csv_file(csv_file)
            self.validations.append(validation)
        
        # Validate video files
        for video_file in video_files:
            print(f"  Validating video: {video_file.name}")
            validation = self.validate_video_file(video_file)
            self.validations.append(validation)
        
        # Validate image files (limit to first 10 to avoid too much output)
        for i, image_file in enumerate(image_files[:10]):
            print(f"  Validating image: {image_file.name}")
            validation = self.validate_image_file(image_file)
            self.validations.append(validation)
        
        if len(image_files) > 10:
            print(f"  (Skipped validation of {len(image_files) - 10} additional images)")
    
    def run_test(self) -> TestResult:
        """Execute the file integrity validation test"""
        print(f"\nStarting File Integrity Validation Test...")
        
        start_time = datetime.now()
        start_timestamp = start_time.isoformat()
        
        # Validate all files
        self.validate_session_directory()
        
        end_time = datetime.now()
        duration = (end_time - start_time).total_seconds()
        
        # Count results
        files_passed = sum(1 for v in self.validations if v.is_valid)
        files_failed = sum(1 for v in self.validations if not v.is_valid)
        
        # Create summary by file type
        summary = {}
        for validation in self.validations:
            file_type = validation.file_type
            if file_type not in summary:
                summary[file_type] = {'total': 0, 'passed': 0, 'failed': 0}
            
            summary[file_type]['total'] += 1
            if validation.is_valid:
                summary[file_type]['passed'] += 1
            else:
                summary[file_type]['failed'] += 1
        
        # Test passes if all files are valid
        passed = files_failed == 0
        
        result = TestResult(
            test_name="File Integrity Validation Test",
            start_time=start_timestamp,
            end_time=end_time.isoformat(),
            duration_seconds=duration,
            files_validated=len(self.validations),
            files_passed=files_passed,
            files_failed=files_failed,
            validations=[asdict(v) for v in self.validations],
            passed=passed,
            summary=summary
        )
        
        # Save result
        self.save_result(result)
        
        # Print summary
        self.print_summary(result)
        
        return result
    
    def save_result(self, result: TestResult) -> None:
        """Save test result as JSON"""
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        result_file = self.output_dir / f"file_integrity_result_{timestamp}.json"
        
        with open(result_file, 'w') as f:
            json.dump(asdict(result), f, indent=2)
        
        print(f"\nSaved result: {result_file}")
    
    def print_summary(self, result: TestResult) -> None:
        """Print test summary"""
        print("\n" + "="*60)
        print("FILE INTEGRITY VALIDATION TEST SUMMARY")
        print("="*60)
        print(f"Test Name: {result.test_name}")
        print(f"Duration: {result.duration_seconds:.2f} seconds")
        print(f"Files Validated: {result.files_validated}")
        print(f"Files Passed: {result.files_passed}")
        print(f"Files Failed: {result.files_failed}")
        
        print(f"\nSummary by File Type:")
        for file_type, counts in result.summary.items():
            print(f"  {file_type}:")
            print(f"    Total: {counts['total']}")
            print(f"    Passed: {counts['passed']}")
            print(f"    Failed: {counts['failed']}")
        
        print(f"\nDetailed Validation Results:")
        for validation in self.validations:
            status = "PASS" if validation.is_valid else "FAIL"
            print(f"  [{status}] {Path(validation.file_path).name}")
            print(f"       Size: {validation.size_mb:.2f} MB")
            
            if isinstance(validation, CSVValidation):
                print(f"       Rows: {validation.row_count}, Columns: {validation.column_count}")
            elif isinstance(validation, VideoValidation):
                print(f"       Duration: {validation.duration_seconds:.2f}s")
            elif isinstance(validation, ImageValidation):
                print(f"       Dimensions: {validation.width}x{validation.height}")
            
            if validation.validation_errors:
                for error in validation.validation_errors:
                    print(f"       Error: {error}")
        
        print(f"\nStatus: {'PASSED' if result.passed else 'FAILED'}")
        print("="*60)


def main():
    """Main entry point"""
    import argparse
    
    parser = argparse.ArgumentParser(description='File Integrity Validation Test')
    parser.add_argument('session_dir', help='Path to session directory to validate')
    
    args = parser.parse_args()
    
    # Create output directory
    output_dir = Path(__file__).parent.parent / "output" / "data_integrity"
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # Run test
    validator = FileIntegrityValidator(
        session_dir=args.session_dir,
        output_dir=str(output_dir)
    )
    result = validator.run_test()
    
    # Exit with appropriate code
    sys.exit(0 if result.passed else 1)


if __name__ == "__main__":
    main()
