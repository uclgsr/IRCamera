#!/usr/bin/env python3


from pathlib import Path
from typing import Any, Dict, List


class IconRegistry:
    

    
    ICONS = {
        "settings": {
            "name": "Settings Gear",
            "android_resource": "ic_setting_default_svg.xml",
            "android_path": "libui/src/main/res/drawable/ic_setting_default_svg.xml",
            "description": "Octagonal gear icon for settings and configuration",
            "use_cases": ["GUI widgets", "Settings panels", "Configuration tools"],
        },
        "calibration": {
            "name": "Calibration Crosshair",
            "android_resource": "ic_menu_coordinate_svg.xml",
            "android_path": "libui/src/main/res/drawable/ic_menu_coordinate_svg.xml",
            "description": "Crosshair with concentric circles for calibration and targeting",
            "use_cases": [
                "Camera calibration",
                "Coordinate systems",
                "Targeting tools",
            ],
        },
        "camera": {
            "name": "Camera Menu",
            "android_resource": "ic_camera_more_svg.xml",
            "android_path": "libui/src/main/res/drawable/ic_camera_more_svg.xml",
            "description": "Camera icon for camera-related functionality",
            "use_cases": ["Camera controls", "Video capture", "Image processing"],
        },
        "network": {
            "name": "Connection",
            "android_resource": "ic_connection_tip1.xml",
            "android_path": "libui/src/main/res/drawable/ic_connection_tip1.xml",
            "description": "Network connection icon",
            "use_cases": ["Network status", "Connection management", "Communication"],
        },
    }

    @classmethod
    def get_icon_info(cls, icon_name: str) -> Dict[str, Any]:
        
        return cls.ICONS.get(icon_name, {})

    @classmethod
    def list_available_icons(cls) -> Dict[str, str]:
        
        return {name: str(info["description"]) for name, info in cls.ICONS.items()}

    @classmethod
    def get_android_resource_path(cls, icon_name: str) -> str:
        
        icon_info = cls.ICONS.get(icon_name, {})
        return str(icon_info.get("android_path", ""))

    @classmethod
    def get_icon_use_cases(cls, icon_name: str) -> List[str]:
        
        icon_info = cls.ICONS.get(icon_name, {})
        use_cases = icon_info.get("use_cases", [])
        return [str(case) for case in use_cases] if use_cases else []


def get_project_icon_path(icon_name: str) -> Path:
    
    android_path = IconRegistry.get_android_resource_path(icon_name)
    if android_path:
        
        project_root = Path(__file__).parent.parent.parent.parent
        return project_root / android_path
    return Path()



ICON_USAGE_GUIDE = """
Generic Icon Usage Guide for IRCamera PC Controller
================================================

Available Icons:
--------------

1. SETTINGS ICON (ic_setting_default_svg.xml)
   - Visual: Octagonal gear with center hole
   - Used for: Placeholder GUI widgets, settings panels, configuration
   - Location: libui/src/main/res/drawable/ic_setting_default_svg.xml

2. CALIBRATION ICON (ic_menu_coordinate_svg.xml) 
   - Visual: Crosshair with concentric circles and axis lines
   - Used for: Camera calibration utilities, coordinate systems, targeting
   - Location: libui/src/main/res/drawable/ic_menu_coordinate_svg.xml

3. CAMERA ICON (ic_camera_more_svg.xml)
   - Visual: Camera symbol
   - Used for: Camera controls, video capture, image processing
   - Location: libui/src/main/res/drawable/ic_camera_more_svg.xml

4. NETWORK ICON (ic_connection_tip1.xml)
   - Visual: Connection/network symbol  
   - Used for: Network status, connection management
   - Location: libui/src/main/res/drawable/ic_connection_tip1.xml

Implementation:
--------------
The placeholder GUI widgets in widgets.py now use the settings icon,
and a new CalibrationUtilityWidget uses the calibration crosshair icon.

The icons are rendered as simple PyQt6 QPainter graphics for cross-platform
compatibility, based on the visual design of the Android SVG resources.
"""

if __name__ == "__main__":
    
    print("IRCamera Icon Registry")
    print("====================")

    for name, description in IconRegistry.list_available_icons().items():
        print(f"{name}: {description}")
        use_cases = IconRegistry.get_icon_use_cases(name)
        print(f"  Use cases: {', '.join(use_cases)}")
        print(f"  Android path: {IconRegistry.get_android_resource_path(name)}")
        print()
