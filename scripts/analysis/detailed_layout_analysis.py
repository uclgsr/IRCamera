#!/usr/bin/env python3
"""
Detailed analysis of XML layout files and their Compose Activity equivalents
"""
import os
import re
from pathlib import Path
from collections import defaultdict

def find_xml_layouts(base_path):
    """Find all XML layout files"""
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
    if 'app/' in path:
        return 'app'
    elif 'component/thermalunified' in path:
        return 'thermalunified'
    elif 'component/user' in path:
        return 'user'
    elif 'libunified' in path:
        return 'libunified'
    elif 'BleModule' in path:
        return 'BleModule'
    return 'other'

def find_compose_activities(base_path):
    """Find all Compose Activity files"""
    activities = []
    for root, dirs, files in os.walk(base_path):
        for file in files:
            if file.endswith('ComposeActivity.kt') or file.endswith('ActivityCompose.kt'):
                full_path = os.path.join(root, file)
                rel_path = full_path.replace(base_path + '/', '')
                activities.append({
                    'file': file,
                    'path': rel_path,
                    'full_path': full_path,
                    'module': extract_module(rel_path)
                })
    return activities

def find_compose_files(base_path):
    """Find all Compose files (activities, fragments, components)"""
    compose_files = []
    for root, dirs, files in os.walk(base_path):
        for file in files:
            if file.endswith('.kt') and ('Compose' in file or '/compose/' in root):
                full_path = os.path.join(root, file)
                rel_path = full_path.replace(base_path + '/', '')
                compose_files.append({
                    'file': file,
                    'path': rel_path,
                    'full_path': full_path,
                    'module': extract_module(rel_path)
                })
    return compose_files

def layout_to_activity_name(layout_file):
    """Convert layout filename to expected activity name"""
    # Remove .xml
    base = layout_file.replace('.xml', '')
    
    # Convert from snake_case to PascalCase
    parts = base.split('_')
    
    # Special handling for activity_ prefix
    if parts[0] == 'activity':
        parts = parts[1:]  # Remove 'activity' prefix
    
    # Capitalize each part
    activity_name = ''.join(word.capitalize() for word in parts)
    
    return activity_name

def find_compose_match(layout_file, compose_activities, compose_files):
    """Find if a layout has a compose equivalent"""
    expected_name = layout_to_activity_name(layout_file)
    
    # Check for direct activity matches
    for activity in compose_activities:
        activity_base = activity['file'].replace('ComposeActivity.kt', '').replace('ActivityCompose.kt', '')
        if expected_name.lower() == activity_base.lower():
            return activity, 'Activity'
    
    # Check for fragment matches
    for compose_file in compose_files:
        if 'Fragment' in compose_file['file']:
            fragment_base = compose_file['file'].replace('FragmentCompose.kt', '').replace('ComposeFragment.kt', '')
            if expected_name.lower() == fragment_base.lower():
                return compose_file, 'Fragment'
    
    # Check for component matches
    for compose_file in compose_files:
        if '/compose/' in compose_file['path']:
            compose_base = compose_file['file'].replace('Compose.kt', '').replace('.kt', '')
            if expected_name.lower() in compose_base.lower():
                return compose_file, 'Component'
    
    return None, None

def categorize_layout(layout_name):
    """Categorize layout by type"""
    if layout_name.startswith('activity_'):
        return 'Activity'
    elif layout_name.startswith('fragment_'):
        return 'Fragment'
    elif layout_name.startswith('dialog_'):
        return 'Dialog'
    elif layout_name.startswith('item_'):
        return 'ListItem'
    elif layout_name.startswith('layout_'):
        return 'Layout'
    elif layout_name.startswith('ui_'):
        return 'UIComponent'
    elif layout_name.startswith('view_'):
        return 'CustomView'
    else:
        return 'Other'

def main():
    base_path = '/home/runner/work/IRCamera/IRCamera'
    
    print("# Detailed Layout vs Compose Migration Analysis")
    print("=" * 100)
    
    # Find all files
    layouts = find_xml_layouts(base_path)
    compose_activities = find_compose_activities(base_path)
    compose_files = find_compose_files(base_path)
    
    print(f"\n## Statistics")
    print(f"- Total XML layouts: {len(layouts)}")
    print(f"- Compose Activities: {len(compose_activities)}")
    print(f"- Total Compose files: {len(compose_files)}")
    print()
    
    # Group by module and category
    by_module_category = defaultdict(lambda: defaultdict(list))
    migration_status = {'migrated': [], 'not_migrated': []}
    
    for layout in layouts:
        category = categorize_layout(layout['file'])
        module = layout['module']
        
        # Check for Compose equivalent
        compose_match, match_type = find_compose_match(layout['file'], compose_activities, compose_files)
        
        layout_info = {
            'layout': layout,
            'compose': compose_match,
            'match_type': match_type,
            'category': category
        }
        
        by_module_category[module][category].append(layout_info)
        
        if compose_match:
            migration_status['migrated'].append(layout_info)
        else:
            migration_status['not_migrated'].append(layout_info)
    
    # Print results by module and category
    print("## Migration Status by Module and Category")
    print("-" * 100)
    
    for module in sorted(by_module_category.keys()):
        print(f"\n### Module: {module}")
        categories = by_module_category[module]
        
        for category in sorted(categories.keys()):
            items = categories[category]
            migrated_count = sum(1 for item in items if item['compose'])
            print(f"\n#### {category} ({migrated_count}/{len(items)} migrated)")
            
            for item in items:
                layout_name = item['layout']['file']
                if item['compose']:
                    compose_name = item['compose']['file']
                    match_type = item['match_type']
                    status = f"MIGRATED ({match_type})"
                    print(f"  [x] {layout_name:<50} -> {compose_name}")
                else:
                    print(f"  [ ] {layout_name:<50} NO COMPOSE EQUIVALENT")
    
    # Summary
    print("\n\n" + "=" * 100)
    print("## Summary")
    print(f"- Layouts with Compose equivalent: {len(migration_status['migrated'])}")
    print(f"- Layouts without Compose equivalent: {len(migration_status['not_migrated'])}")
    print(f"- Migration coverage: {len(migration_status['migrated']) / len(layouts) * 100:.1f}%")
    
    # List critical missing migrations (Activities)
    print("\n## Critical Missing Migrations (Activity Layouts)")
    print("-" * 100)
    activity_layouts = [item for item in migration_status['not_migrated'] if item['category'] == 'Activity']
    
    if activity_layouts:
        for module in sorted(set(item['layout']['module'] for item in activity_layouts)):
            module_activities = [item for item in activity_layouts if item['layout']['module'] == module]
            if module_activities:
                print(f"\n### {module} ({len(module_activities)} activities)")
                for item in module_activities[:20]:
                    print(f"  - {item['layout']['file']}")
                if len(module_activities) > 20:
                    print(f"  ... and {len(module_activities) - 20} more")
    else:
        print("\nAll activity layouts have been migrated!")
    
    print("\n" + "=" * 100)

if __name__ == '__main__':
    main()
