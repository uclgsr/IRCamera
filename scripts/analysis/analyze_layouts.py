#!/usr/bin/env python3
"""
Analyze XML layout files and their Compose equivalents
"""
import os
import re
from pathlib import Path

def find_xml_layouts(base_path):
    """Find all XML layout files in the repository"""
    layouts = []
    for root, dirs, files in os.walk(base_path):
        if '/layout' in root or '/layout-' in root:
            for file in files:
                if file.endswith('.xml'):
                    full_path = os.path.join(root, file)
                    rel_path = full_path.replace(base_path + '/', '')
                    layouts.append({
                        'file': file,
                        'path': rel_path,
                        'full_path': full_path,
                        'module': extract_module(rel_path)
                    })
    return sorted(layouts, key=lambda x: (x['module'], x['file']))

def extract_module(path):
    """Extract module name from path"""
    parts = path.split('/')
    if 'app/' in path:
        return 'app'
    elif 'component/thermalunified' in path:
        return 'component/thermalunified'
    elif 'component/user' in path:
        return 'component/user'
    elif 'libunified' in path:
        return 'libunified'
    elif 'BleModule' in path:
        return 'BleModule'
    return 'other'

def find_compose_files(base_path):
    """Find all Compose files in the repository"""
    compose_files = []
    for root, dirs, files in os.walk(base_path):
        if '/compose' in root or 'Compose' in root:
            for file in files:
                if file.endswith('.kt') and ('Compose' in file or '/compose/' in root):
                    full_path = os.path.join(root, file)
                    rel_path = full_path.replace(base_path + '/', '')
                    compose_files.append({
                        'file': file,
                        'path': rel_path,
                        'full_path': full_path
                    })
    return compose_files

def read_file_content(file_path):
    """Read file content safely"""
    try:
        with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
            return f.read()
    except:
        return ""

def check_compose_equivalent(layout_name, compose_files):
    """Check if a layout has a Compose equivalent"""
    # Remove .xml extension
    base_name = layout_name.replace('.xml', '')
    
    # Common patterns to check
    patterns = [
        base_name + 'Compose',
        base_name.replace('activity_', '') + 'Compose',
        base_name.replace('fragment_', '') + 'Compose',
        base_name.replace('dialog_', '') + 'DialogCompose',
        base_name.replace('layout_', '') + 'Compose',
        base_name.replace('item_', '') + 'ItemCompose',
        base_name.replace('ui_', '') + 'Compose',
        base_name.replace('view_', '') + 'ViewCompose',
        # Convert snake_case to PascalCase
        ''.join(word.capitalize() for word in base_name.split('_')) + 'Compose'
    ]
    
    matches = []
    for compose_file in compose_files:
        compose_name = compose_file['file'].replace('.kt', '')
        # Check if any pattern matches
        for pattern in patterns:
            if pattern.lower() in compose_name.lower():
                matches.append(compose_file)
                break
    
    return matches

def search_compose_references(layout_name, compose_files):
    """Search for references to layout name in compose files"""
    base_name = layout_name.replace('.xml', '').replace('_', '')
    references = []
    
    for compose_file in compose_files:
        content = read_file_content(compose_file['full_path'])
        # Look for references to the layout name or similar patterns
        if base_name.lower() in content.lower():
            references.append(compose_file)
    
    return references

def categorize_layout(layout_name, path):
    """Categorize layout by type"""
    if layout_name.startswith('activity_'):
        return 'Activity Layout'
    elif layout_name.startswith('fragment_'):
        return 'Fragment Layout'
    elif layout_name.startswith('dialog_'):
        return 'Dialog Layout'
    elif layout_name.startswith('item_'):
        return 'List Item Layout'
    elif layout_name.startswith('layout_'):
        return 'Generic Layout'
    elif layout_name.startswith('ui_'):
        return 'UI Component'
    elif layout_name.startswith('view_'):
        return 'Custom View'
    elif 'wheel_picker' in layout_name:
        return 'Wheel Picker'
    else:
        return 'Other'

def main():
    script_dir = Path(__file__).resolve().parent
    base_path = str(script_dir.parent.parent)
    
    print("Analyzing XML layouts and Compose equivalents...")
    print("=" * 80)
    
    # Find all layouts and compose files
    layouts = find_xml_layouts(base_path)
    compose_files = find_compose_files(base_path)
    
    print(f"\nTotal XML layouts found: {len(layouts)}")
    print(f"Total Compose files found: {len(compose_files)}")
    print("=" * 80)
    
    # Categorize layouts
    by_category = {}
    by_module = {}
    
    for layout in layouts:
        category = categorize_layout(layout['file'], layout['path'])
        module = layout['module']
        
        if category not in by_category:
            by_category[category] = []
        by_category[category].append(layout)
        
        if module not in by_module:
            by_module[module] = []
        by_module[module].append(layout)
    
    # Report by category
    print("\n## Layouts by Category:")
    print("-" * 80)
    for category in sorted(by_category.keys()):
        layouts_in_cat = by_category[category]
        print(f"\n### {category} ({len(layouts_in_cat)} files)")
        for layout in layouts_in_cat[:5]:  # Show first 5
            print(f"  - {layout['file']} ({layout['module']})")
        if len(layouts_in_cat) > 5:
            print(f"  ... and {len(layouts_in_cat) - 5} more")
    
    # Report by module
    print("\n\n## Layouts by Module:")
    print("-" * 80)
    for module in sorted(by_module.keys()):
        layouts_in_mod = by_module[module]
        print(f"\n### {module} ({len(layouts_in_mod)} files)")
    
    # Check for Compose equivalents
    print("\n\n## Layout Migration Status:")
    print("-" * 80)
    
    has_equivalent = 0
    no_equivalent = 0
    
    for module in sorted(by_module.keys()):
        print(f"\n### Module: {module}")
        layouts_in_mod = by_module[module]
        
        for layout in layouts_in_mod[:10]:  # Show first 10 per module
            matches = check_compose_equivalent(layout['file'], compose_files)
            references = search_compose_references(layout['file'], compose_files)
            
            if matches or references:
                has_equivalent += 1
                status = "HAS COMPOSE EQUIVALENT"
                if matches:
                    compose_info = f" -> {matches[0]['file']}"
                else:
                    compose_info = f" (referenced in {len(references)} files)"
            else:
                no_equivalent += 1
                status = "NO COMPOSE EQUIVALENT"
                compose_info = ""
            
            print(f"  {layout['file']:<50} [{status}]{compose_info}")
        
        if len(layouts_in_mod) > 10:
            print(f"  ... and {len(layouts_in_mod) - 10} more")
    
    print("\n\n" + "=" * 80)
    print(f"Summary:")
    print(f"  Total layouts: {len(layouts)}")
    print(f"  With Compose equivalent: {has_equivalent}")
    print(f"  Without Compose equivalent: {no_equivalent}")
    print(f"  Migration coverage: {has_equivalent / len(layouts) * 100:.1f}%")
    print("=" * 80)

if __name__ == '__main__':
    main()
