#!/usr/bin/env python3
"""
Thesis Content Generation Integration Tests
Tests the complete thesis generation pipeline from source data to final documentation.
"""

import os
import json
import subprocess
import tempfile
from pathlib import Path
from typing import Dict, List, Any, Optional
import re
from dataclasses import dataclass
import time

@dataclass
class ContentValidationResult:
    component: str
    test_type: str
    status: str  # PASS, FAIL, WARNING
    details: Dict[str, Any]
    timestamp: str

class ThesisContentIntegrationTest:
    """Integration tests for thesis content generation"""
    
    def __init__(self, repo_root: str = "."):
        self.repo_root = Path(repo_root)
        self.results: List[ContentValidationResult] = []
        self.output_dir = Path("testing-suite/results/integration")
        self.output_dir.mkdir(parents=True, exist_ok=True)
    
    def run_integration_tests(self) -> Dict[str, Any]:
        """Run all integration tests"""
        print("🔗 Running Thesis Content Generation Integration Tests")
        print("=" * 60)
        
        # Test 1: Source Data Integrity
        self.test_source_data_integrity()
        
        # Test 2: Document Generation Pipeline
        self.test_document_generation()
        
        # Test 3: Cross-Reference Validation
        self.test_cross_references()
        
        # Test 4: Content Consistency
        self.test_content_consistency()
        
        # Test 5: LaTeX Compilation
        self.test_latex_compilation()
        
        # Test 6: Diagram Integration
        self.test_diagram_integration()
        
        # Test 7: Table Generation
        self.test_table_generation()
        
        # Generate final report
        return self.generate_integration_report()
    
    def test_source_data_integrity(self) -> None:
        """Test integrity of source data files"""
        print(" Testing Source Data Integrity...")
        
        # Check critical source files
        critical_files = [
            "../docs/latex/main.tex",
            "../docs/latex/4.tex",
            "../docs/latex/5.tex", 
            "../docs/latex/6.tex",
            "../docs/latex/appendix_Z.tex"
        ]
        
        for file_path in critical_files:
            full_path = self.repo_root / file_path
            
            if not full_path.exists():
                self.add_result(
                    f"File Existence: {file_path}",
                    "Source Integrity",
                    "FAIL",
                    {"error": "File not found", "path": str(full_path)}
                )
                continue
            
            try:
                # Check file readability and encoding
                with open(full_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Basic content validation
                if len(content) < 100:  # Minimum reasonable content
                    status = "WARNING"
                    details = {"warning": "File seems too short", "length": len(content)}
                elif "\\chapter" in content or "\\section" in content or "\\input" in content:
                    status = "PASS"
                    details = {"length": len(content), "valid_latex": True}
                else:
                    status = "WARNING"
                    details = {"warning": "No LaTeX structure found", "length": len(content)}
                
                self.add_result(
                    f"Content Validation: {Path(file_path).name}",
                    "Source Integrity",
                    status,
                    details
                )
                
            except UnicodeDecodeError:
                self.add_result(
                    f"Encoding Test: {file_path}",
                    "Source Integrity",
                    "FAIL",
                    {"error": "UTF-8 encoding issue"}
                )
            except Exception as e:
                self.add_result(
                    f"File Access: {file_path}",
                    "Source Integrity",
                    "FAIL", 
                    {"error": str(e)}
                )
    
    def test_document_generation(self) -> None:
        """Test document generation process"""
        print("📝 Testing Document Generation Pipeline...")
        
        # Test Markdown to content conversion
        diagram_files = list((self.repo_root / "../docs/thesis-diagrams").glob("*.md"))
        
        for diagram_file in diagram_files:
            try:
                with open(diagram_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Check for Mermaid diagrams
                mermaid_count = content.count('```mermaid')
                
                # Check for structured content
                has_headers = bool(re.search(r'^#+ ', content, re.MULTILINE))
                has_tables = '|' in content and '---' in content
                has_descriptions = len(content) > 2000  # Substantial content
                
                details = {
                    "mermaid_diagrams": mermaid_count,
                    "has_headers": has_headers,
                    "has_tables": has_tables,
                    "content_substantial": has_descriptions,
                    "file_size": len(content)
                }
                
                # Scoring
                score = sum([
                    mermaid_count > 0,
                    has_headers,
                    has_tables,
                    has_descriptions
                ])
                
                if score >= 3:
                    status = "PASS"
                elif score >= 2:
                    status = "WARNING"
                else:
                    status = "FAIL"
                
                self.add_result(
                    f"Content Generation: {diagram_file.name}",
                    "Document Generation",
                    status,
                    details
                )
                
            except Exception as e:
                self.add_result(
                    f"Generation Error: {diagram_file.name}",
                    "Document Generation",
                    "FAIL",
                    {"error": str(e)}
                )
    
    def test_cross_references(self) -> None:
        """Test cross-references between documents"""
        print("🔗 Testing Cross-Reference Validation...")
        
        # Build reference map
        references = self.build_reference_map()
        
        # Test LaTeX files for proper referencing
        latex_files = [
            "../docs/latex/4.tex",
            "../docs/latex/5.tex",
            "../docs/latex/appendix_Z.tex"
        ]
        
        for latex_file in latex_files:
            full_path = self.repo_root / latex_file
            
            if not full_path.exists():
                continue
                
            try:
                with open(full_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Check for reference patterns
                figure_refs = len(re.findall(r'Figure[~\s]*\d+\.\d+', content))
                table_refs = len(re.findall(r'Table[~\s]*\d+\.\d+', content))
                appendix_refs = len(re.findall(r'Appendix[~\s]*Z', content))
                section_refs = len(re.findall(r'Section[~\s]*\d+\.\d+', content))
                
                total_refs = figure_refs + table_refs + appendix_refs + section_refs
                
                details = {
                    "figure_references": figure_refs,
                    "table_references": table_refs,
                    "appendix_references": appendix_refs,
                    "section_references": section_refs,
                    "total_references": total_refs
                }
                
                # Validate reference quality
                if total_refs >= 5:  # Good cross-referencing
                    status = "PASS"
                elif total_refs >= 2:
                    status = "WARNING"
                else:
                    status = "FAIL"
                
                self.add_result(
                    f"Cross-References: {Path(latex_file).name}",
                    "Cross-Reference Validation",
                    status,
                    details
                )
                
            except Exception as e:
                self.add_result(
                    f"Reference Check Error: {latex_file}",
                    "Cross-Reference Validation",
                    "FAIL",
                    {"error": str(e)}
                )
    
    def build_reference_map(self) -> Dict[str, List[str]]:
        """Build a map of available references"""
        references = {
            "figures": [],
            "tables": [],
            "sections": []
        }
        
        # Scan appendix_Z for figure and table definitions
        appendix_path = self.repo_root / "../docs/latex/appendix_Z.tex"
        if appendix_path.exists():
            try:
                with open(appendix_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Extract figure references
                figure_matches = re.findall(r'Figure\s+(\d+\.\d+)', content)
                references["figures"].extend(figure_matches)
                
                # Extract table references  
                table_matches = re.findall(r'Table\s+(\d+\.\d+)', content)
                references["tables"].extend(table_matches)
                
            except Exception as e:
                print(f"Warning: Could not scan appendix for references: {e}")
        
        return references
    
    def test_content_consistency(self) -> None:
        """Test consistency of content across documents"""
        print("🔄 Testing Content Consistency...")
        
        # Key terms that should appear consistently
        key_terms = [
            ("TC001", "thermal camera"),
            ("Shimmer3", "GSR sensor"),
            ("2.1ms", "synchronization"),
            ("5ms", "target accuracy"),
            ("1.21 MB/s", "throughput")
        ]
        
        content_files = [
            "../docs/latex/4.tex",
            "../docs/latex/5.tex",
            "../docs/thesis-diagrams/performance-test-tables.md",
            "../docs/thesis-diagrams/system-configuration-tables.md"
        ]
        
        term_consistency = {}
        
        for term, context in key_terms:
            term_consistency[term] = {"files": [], "contexts": []}
            
            for file_path in content_files:
                full_path = self.repo_root / file_path
                
                if not full_path.exists():
                    continue
                
                try:
                    with open(full_path, 'r', encoding='utf-8') as f:
                        content = f.read()
                    
                    if term.lower() in content.lower():
                        term_consistency[term]["files"].append(file_path)
                        
                        # Extract context around the term
                        pattern = rf'.{{0,50}}{re.escape(term)}.{{0,50}}'
                        matches = re.findall(pattern, content, re.IGNORECASE)
                        term_consistency[term]["contexts"].extend(matches[:2])  # First 2 matches
                
                except Exception as e:
                    print(f"Warning: Could not check consistency in {file_path}: {e}")
        
        # Evaluate consistency
        for term, data in term_consistency.items():
            file_count = len(data["files"])
            context_count = len(data["contexts"])
            
            if file_count >= 3:  # Term appears in multiple files
                status = "PASS"
            elif file_count >= 2:
                status = "WARNING"
            else:
                status = "FAIL"
            
            self.add_result(
                f"Term Consistency: {term}",
                "Content Consistency",
                status,
                {
                    "files_containing_term": file_count,
                    "context_examples": data["contexts"][:3],  # First 3 examples
                    "files": data["files"]
                }
            )
    
    def test_latex_compilation(self) -> None:
        """Test LaTeX compilation readiness"""
        print("📄 Testing LaTeX Compilation Readiness...")
        
        latex_files = ["../docs/latex/main.tex"]
        
        for latex_file in latex_files:
            full_path = self.repo_root / latex_file
            
            if not full_path.exists():
                self.add_result(
                    f"LaTeX File Missing: {latex_file}",
                    "LaTeX Compilation",
                    "FAIL",
                    {"error": "Main LaTeX file not found"}
                )
                continue
            
            try:
                with open(full_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Check for essential LaTeX structure
                has_documentclass = "\\documentclass" in content
                has_begin_document = "\\begin{document}" in content
                has_end_document = "\\end{document}" in content
                has_inputs = "\\input{" in content
                has_bibliography = "\\bibliography" in content or "\\bibname" in content
                
                structure_score = sum([
                    has_documentclass,
                    has_begin_document,
                    has_end_document, 
                    has_inputs,
                    has_bibliography
                ])
                
                details = {
                    "has_documentclass": has_documentclass,
                    "has_document_environment": has_begin_document and has_end_document,
                    "has_input_files": has_inputs,
                    "has_bibliography": has_bibliography,
                    "structure_score": structure_score,
                    "content_length": len(content)
                }
                
                if structure_score >= 4:
                    status = "PASS"
                elif structure_score >= 3:
                    status = "WARNING"
                else:
                    status = "FAIL"
                
                self.add_result(
                    f"LaTeX Structure: {Path(latex_file).name}",
                    "LaTeX Compilation",
                    status,
                    details
                )
                
                # Test for common LaTeX issues
                self.test_latex_syntax_issues(content, latex_file)
                
            except Exception as e:
                self.add_result(
                    f"LaTeX Read Error: {latex_file}",
                    "LaTeX Compilation",
                    "FAIL",
                    {"error": str(e)}
                )
    
    def test_latex_syntax_issues(self, content: str, filename: str) -> None:
        """Test for common LaTeX syntax issues"""
        issues = []
        
        # Check for unmatched braces (simplified)
        open_braces = content.count('{')
        close_braces = content.count('}')
        if open_braces != close_braces:
            issues.append(f"Unmatched braces: {open_braces} open, {close_braces} close")
        
        # Check for unmatched math delimiters (simplified)
        dollar_count = content.count('$')
        if dollar_count % 2 != 0:
            issues.append("Unmatched $ math delimiters")
        
        # Check for common problematic characters
        problematic_chars = ['_', '&', '%', '#']  # Outside of proper LaTeX contexts
        for char in problematic_chars:
            if char in content and not f"\\{char}" in content:
                # This is a simplified check - in reality, these chars are often valid
                pass  # Skip for now to avoid false positives
        
        status = "PASS" if len(issues) == 0 else "WARNING"
        
        self.add_result(
            f"LaTeX Syntax Check: {Path(filename).name}",
            "LaTeX Compilation",
            status,
            {"issues": issues, "issue_count": len(issues)}
        )
    
    def test_diagram_integration(self) -> None:
        """Test diagram integration into thesis"""
        print(" Testing Diagram Integration...")
        
        # Check if diagrams are properly integrated
        diagram_dir = self.repo_root / "../docs/thesis-diagrams"
        if not diagram_dir.exists():
            self.add_result(
                "Diagram Directory Missing",
                "Diagram Integration",
                "FAIL",
                {"error": "docs/thesis-diagrams directory not found"}
            )
            return
        
        diagram_files = list(diagram_dir.glob("*.md"))
        
        if len(diagram_files) == 0:
            self.add_result(
                "No Diagram Files",
                "Diagram Integration", 
                "FAIL",
                {"error": "No diagram files found"}
            )
            return
        
        # Validate each diagram file
        for diagram_file in diagram_files:
            try:
                with open(diagram_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Check diagram quality
                has_mermaid = "```mermaid" in content
                has_title = content.startswith('#')
                has_description = len(content) > 1000  # Substantial description
                mermaid_count = content.count('```mermaid')
                
                details = {
                    "has_mermaid_diagrams": has_mermaid,
                    "has_title": has_title,
                    "substantial_content": has_description,
                    "mermaid_diagram_count": mermaid_count,
                    "file_size": len(content)
                }
                
                quality_score = sum([has_mermaid, has_title, has_description])
                
                if quality_score >= 2 and mermaid_count > 0:
                    status = "PASS"
                elif quality_score >= 1:
                    status = "WARNING"
                else:
                    status = "FAIL"
                
                self.add_result(
                    f"Diagram Quality: {diagram_file.name}",
                    "Diagram Integration",
                    status,
                    details
                )
                
            except Exception as e:
                self.add_result(
                    f"Diagram Read Error: {diagram_file.name}",
                    "Diagram Integration",
                    "FAIL",
                    {"error": str(e)}
                )
        
        # Summary integration test
        total_diagrams = len(diagram_files)
        self.add_result(
            "Diagram Collection Completeness",
            "Diagram Integration",
            "PASS" if total_diagrams >= 5 else "WARNING",
            {
                "total_diagram_files": total_diagrams,
                "expected_minimum": 5,
                "diagram_files": [f.name for f in diagram_files]
            }
        )
    
    def test_table_generation(self) -> None:
        """Test table generation and integration"""
        print(" Testing Table Generation...")
        
        table_files = [
            "../docs/thesis-diagrams/system-configuration-tables.md",
            "../docs/thesis-diagrams/performance-test-tables.md"
        ]
        
        for table_file in table_files:
            full_path = self.repo_root / table_file
            
            if not full_path.exists():
                self.add_result(
                    f"Table File Missing: {Path(table_file).name}",
                    "Table Generation",
                    "FAIL",
                    {"error": "Table file not found"}
                )
                continue
            
            try:
                with open(full_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Analyze table structure
                table_headers = content.count('| Component |') + content.count('| Metric |') + \
                              content.count('| Test Category |') + content.count('| Test Case |')
                table_separators = content.count('|---|') + content.count('|:---:|')
                table_rows = len([line for line in content.split('\n') if '|' in line and not line.strip().startswith('#')])
                
                # Check for data content
                has_numeric_data = bool(re.search(r'\d+(\.\d+)?\s*(ms|MB|GB|Hz|%)', content))
                has_specifications = 'TC001' in content or 'Shimmer3' in content
                has_performance_data = any(term in content for term in ['2.1ms', '1.21 MB/s', '95%', '128 Hz'])
                
                details = {
                    "table_headers": table_headers,
                    "table_separators": table_separators,
                    "table_rows": table_rows,
                    "has_numeric_data": has_numeric_data,
                    "has_specifications": has_specifications,
                    "has_performance_data": has_performance_data,
                    "content_length": len(content)
                }
                
                # Quality assessment
                quality_indicators = [
                    table_headers > 0,
                    table_rows >= 10,  # Substantial number of rows
                    has_numeric_data,
                    has_specifications or has_performance_data
                ]
                
                quality_score = sum(quality_indicators)
                
                if quality_score >= 3:
                    status = "PASS"
                elif quality_score >= 2:
                    status = "WARNING"
                else:
                    status = "FAIL"
                
                self.add_result(
                    f"Table Quality: {Path(table_file).name}",
                    "Table Generation",
                    status,
                    details
                )
                
            except Exception as e:
                self.add_result(
                    f"Table Read Error: {Path(table_file).name}",
                    "Table Generation",
                    "FAIL",
                    {"error": str(e)}
                )
    
    def add_result(self, component: str, test_type: str, status: str, details: Dict[str, Any]) -> None:
        """Add a test result"""
        result = ContentValidationResult(
            component=component,
            test_type=test_type,
            status=status,
            details=details,
            timestamp=time.strftime("%Y-%m-%d %H:%M:%S")
        )
        
        self.results.append(result)
        
        # Print result
        status_symbol = "" if status == "PASS" else "" if status == "WARNING" else ""
        print(f"  {status_symbol} {component}: {status}")
    
    def generate_integration_report(self) -> Dict[str, Any]:
        """Generate comprehensive integration report"""
        print("\n Generating Integration Test Report...")
        
        # Calculate statistics by test type
        test_types = {}
        for result in self.results:
            if result.test_type not in test_types:
                test_types[result.test_type] = {"PASS": 0, "WARNING": 0, "FAIL": 0, "total": 0}
            
            test_types[result.test_type][result.status] += 1
            test_types[result.test_type]["total"] += 1
        
        # Overall statistics
        total_tests = len(self.results)
        passed_tests = sum(1 for r in self.results if r.status == "PASS")
        warning_tests = sum(1 for r in self.results if r.status == "WARNING")
        failed_tests = sum(1 for r in self.results if r.status == "FAIL")
        
        summary = {
            "total_tests": total_tests,
            "passed": passed_tests,
            "warnings": warning_tests,
            "failed": failed_tests,
            "pass_rate": passed_tests / total_tests if total_tests > 0 else 0,
            "test_types": test_types,
            "overall_status": "PASS" if failed_tests == 0 else "PARTIAL" if passed_tests > failed_tests else "FAIL"
        }
        
        # Save results
        self.save_integration_results(summary)
        
        # Print summary
        print(f"\n🔗 Integration Test Summary:")
        print(f"   Total Tests: {total_tests}")
        print(f"   Passed: {passed_tests} ({summary['pass_rate']:.1%})")
        print(f"   Warnings: {warning_tests}")
        print(f"   Failed: {failed_tests}")
        print(f"   Overall Status: {summary['overall_status']}")
        
        # Print by test type
        for test_type, stats in test_types.items():
            pass_rate = stats["PASS"] / stats["total"] if stats["total"] > 0 else 0
            print(f"   {test_type}: {stats['PASS']}/{stats['total']} ({pass_rate:.1%}) passed")
        
        return summary
    
    def save_integration_results(self, summary: Dict[str, Any]) -> None:
        """Save integration test results"""
        # JSON results
        results_data = {
            "summary": summary,
            "results": [
                {
                    "component": r.component,
                    "test_type": r.test_type,
                    "status": r.status,
                    "details": r.details,
                    "timestamp": r.timestamp
                }
                for r in self.results
            ]
        }
        
        json_file = self.output_dir / "integration_test_results.json"
        with open(json_file, 'w') as f:
            json.dump(results_data, f, indent=2)
        
        # Generate thesis integration summary
        thesis_file = self.output_dir / "thesis_integration_summary.md"
        with open(thesis_file, 'w') as f:
            f.write("# Thesis Content Generation Integration Test Results\n\n")
            f.write(f"**Overall Status**: {summary['overall_status']}\n")
            f.write(f"**Pass Rate**: {summary['pass_rate']:.1%}\n\n")
            
            f.write("## Test Results by Category\n\n")
            for test_type, stats in summary['test_types'].items():
                pass_rate = stats["PASS"] / stats["total"] if stats["total"] > 0 else 0
                f.write(f"### {test_type}\n")
                f.write(f"- Total Tests: {stats['total']}\n")
                f.write(f"- Passed: {stats['PASS']} ({pass_rate:.1%})\n")
                f.write(f"- Warnings: {stats['WARNING']}\n")
                f.write(f"- Failed: {stats['FAIL']}\n\n")
            
            f.write("## Detailed Results\n\n")
            for result in self.results:
                status_emoji = "" if result.status == "PASS" else "" if result.status == "WARNING" else ""
                f.write(f"{status_emoji} **{result.component}** ({result.test_type}): {result.status}\n")


def main():
    """Main execution function"""
    integration_test = ThesisContentIntegrationTest()
    results = integration_test.run_integration_tests()
    
    print(f"\n🔗 Integration testing complete!")
    print(f" Results saved to: {integration_test.output_dir}")
    
    return results

if __name__ == "__main__":
    main()