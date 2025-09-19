"""
Administrator Privileges Manager for IRCamera PC Controller

Handles privilege elevation and system integration for full PC control
including network, Bluetooth, and system-level operations.
"""

import ctypes
import os
import platform
import subprocess
import sys
from dataclasses import dataclass
from enum import Enum
from typing import Optional, Any

try:
    from PyQt6.QtCore import pyqtSignal
    from PyQt6.QtWidgets import QApplication, QMessageBox

    from .base_manager import BaseManager

    PYQT_AVAILABLE = True

except ImportError:
    from .base_manager import BaseManager

    PYQT_AVAILABLE = False


    # Mock classes for when PyQt6 is not available
    class QMessageBox:
        StandardButton = type(
            "StandardButton",
            (),
            {"Yes": "Yes", "No": "No", "Cancel": "Cancel"},
        )()

        @staticmethod
        def question(*args, **kwargs) -> Any:
            return QMessageBox.StandardButton.Yes

        @staticmethod
        def warning(*args, **kwargs) -> Any:
            pass


    class QApplication:
        @staticmethod
        def quit() -> Any:
            pass

from loguru import logger

try:
    if platform.system() == "Windows":
        import win32api
        import win32con
        import win32security

        WIN32_AVAILABLE = True
    else:
        WIN32_AVAILABLE = False
except ImportError:
    WIN32_AVAILABLE = False

try:
    from elevate import elevate

    ELEVATE_AVAILABLE = True
except ImportError:
    ELEVATE_AVAILABLE = False


class PrivilegeLevel(Enum):
    """Current privilege levels."""

    USER = "user"
    ELEVATED = "elevated"
    ADMIN = "admin"
    SYSTEM = "system"
    UNKNOWN = "unknown"


class ElevationResult(Enum):
    """Results of privilege elevation attempts."""

    SUCCESS = "success"
    CANCELLED = "cancelled"
    FAILED = "failed"
    ALREADY_ELEVATED = "already_elevated"
    NOT_SUPPORTED = "not_supported"


@dataclass
class SystemPermissions:
    """System permission status."""

    network_config: bool = False
    bluetooth_control: bool = False
    service_management: bool = False
    registry_access: bool = False
    hardware_access: bool = False
    firewall_control: bool = False


class AdminPrivilegesManager(BaseManager):
    """
    Manages administrator privileges and system integration.

    Provides:
    - Privilege elevation and UAC handling
    - Permission verification and status checking
    - System service integration
    - Security context management
    - Platform-specific privilege handling
    """

    # Signals (only available with PyQt6)
    if PYQT_AVAILABLE:
        privilege_changed = pyqtSignal(PrivilegeLevel)
        elevation_requested = pyqtSignal(str)  # reason
        elevation_completed = pyqtSignal(ElevationResult, str)  # result, message
        permission_denied = pyqtSignal(str, str)  # operation, reason
        system_ready = pyqtSignal(SystemPermissions)

    def __init__(self):
        super().__init__("admin_privileges")
        self._current_privilege = PrivilegeLevel.UNKNOWN
        self._permissions = SystemPermissions()
        self._elevation_requested = False

        # Check initial privilege level
        self._check_current_privileges()
        self._check_system_permissions()

    def _emit_signal(self, signal_name: str, *args):
        """Emit a signal if PyQt6 is available."""
        if PYQT_AVAILABLE and hasattr(self, signal_name):
            signal = getattr(self, signal_name)
            signal.emit(*args)

    @property
    def current_privilege_level(self) -> PrivilegeLevel:
        """Get current privilege level."""
        return self._current_privilege

    @property
    def is_elevated(self) -> bool:
        """Check if running with elevated privileges."""
        return self._current_privilege in [
            PrivilegeLevel.ELEVATED,
            PrivilegeLevel.ADMIN,
            PrivilegeLevel.SYSTEM,
        ]

    @property
    def system_permissions(self) -> SystemPermissions:
        """Get current system permissions status."""
        return self._permissions

    def request_elevation(self, reason: str = "System Integration") -> ElevationResult:
        """
        Request privilege elevation for system integration.

        Args:
            reason: Reason for elevation request

        Returns:
            Result of elevation attempt
        """
        if self.is_elevated:
            return ElevationResult.ALREADY_ELEVATED

        if self._elevation_requested:
            logger.warning("Elevation already requested")
            return ElevationResult.FAILED

        logger.info(f"Requesting privilege elevation: {reason}")
        self._emit_signal("elevation_requested", reason)
        self._elevation_requested = True

        try:
            result = self._perform_elevation(reason)
            self._emit_signal(
                "elevation_completed", result, self._get_result_message(result)
            )

            if result == ElevationResult.SUCCESS:
                self._check_current_privileges()
                self._check_system_permissions()

            return result

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Elevation request failed: {e}")
            result = ElevationResult.FAILED
            self._emit_signal("elevation_completed", result, str(e))
            return result
        finally:
            self._elevation_requested = False

    def verify_operation_permissions(self, operation: str) -> bool:
        """
        Verify if current privileges allow specific operation.

        Args:
            operation: Operation to verify ('bluetooth',
                'wifi',
                'service',
                etc.)

        Returns:
            True if operation is permitted
        """
        operation_lower = operation.lower()

        permission_map = {
            "bluetooth": self._permissions.bluetooth_control,
            "wifi": self._permissions.network_config,
            "network": self._permissions.network_config,
            "service": self._permissions.service_management,
            "registry": self._permissions.registry_access,
            "hardware": self._permissions.hardware_access,
            "firewall": self._permissions.firewall_control,
        }

        if operation_lower in permission_map:
            has_permission = permission_map[operation_lower]
            if not has_permission:
                self._emit_signal(
                    "permission_denied", operation, "Insufficient privileges"
                )
            return has_permission

        logger.warning(f"Unknown operation permission check: {operation}")
        return False

    def run_as_admin(self, command: str, arguments: Optional[list] = None) -> bool:
        """
        Run a command with administrator privileges.

        Args:
            command: Command to run
            arguments: Command arguments

        Returns:
            True if command executed successfully
        """
        if not self.is_elevated:
            logger.error("Cannot run admin command without elevation")
            return False

        try:
            system = platform.system()

            if system == "Windows":
                return self._run_windows_admin_command(command, arguments or [])
            elif system in ["Linux", "Darwin"]:
                return self._run_unix_admin_command(command, arguments or [])
            else:
                logger.error(f"Unsupported platform: {system}")
                return False

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Failed to run admin command: {e}")
            return False

    def check_service_status(self, service_name: str) -> Optional[str]:
        """
        Check the status of a system service.

        Args:
            service_name: Name of the service

        Returns:
            Service status or None if not accessible
        """
        if not self.verify_operation_permissions("service"):
            return None

        try:
            system = platform.system()

            if system == "Windows":
                return self._check_windows_service(service_name)
            elif system == "Linux":
                return self._check_linux_service(service_name)
            elif system == "Darwin":
                return self._check_macos_service(service_name)
            else:
                return None

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Failed to check service status: {e}")
            return None

    def manage_firewall_rule(self, rule_name: str, action: str, **kwargs) -> bool:
        """
        Manage Windows Firewall rules for IRCamera communication.

        Args:
            rule_name: Firewall rule name
            action: 'add', 'remove', or 'modify'
            **kwargs: Rule parameters (port, protocol, direction, etc.)

        Returns:
            True if operation successful
        """
        if not self.verify_operation_permissions("firewall"):
            return False

        if platform.system() != "Windows":
            logger.warning("Firewall management only supported on Windows")
            return False

        try:
            return self._manage_windows_firewall_rule(rule_name, action, **kwargs)

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Firewall rule management failed: {e}")
            return False

    def _check_current_privileges(self) -> None:
        """Check and update current privilege level."""
        system = platform.system()

        try:
            if system == "Windows":
                self._current_privilege = self._check_windows_privileges()
            elif system in ["Linux", "Darwin"]:
                self._current_privilege = self._check_unix_privileges()
            else:
                self._current_privilege = PrivilegeLevel.UNKNOWN

            logger.info(f"Current privilege level: {self._current_privilege.value}")
            self._emit_signal("privilege_changed", self._current_privilege)

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Failed to check privileges: {e}")
            self._current_privilege = PrivilegeLevel.UNKNOWN

    def _check_windows_privileges(self) -> PrivilegeLevel:
        """Check privilege level on Windows."""
        try:
            # Check if running as administrator
            if ctypes.windll.shell32.IsUserAnAdmin():
                # Check if running as SYSTEM
                if WIN32_AVAILABLE:
                    try:
                        token = win32security.OpenProcessToken(
                            win32api.GetCurrentProcess(), win32con.TOKEN_QUERY
                        )
                        user_sid = win32security.GetTokenInformation(
                            token, win32security.TokenUser
                        )[0]

                        # SYSTEM SID: S-1-5-18
                        system_sid = win32security.ConvertStringSidToSid("S-1-5-18")

                        if win32security.EqualSid(user_sid, system_sid):
                            return PrivilegeLevel.SYSTEM
                        else:
                            return PrivilegeLevel.ADMIN

                    except (OSError, ValueError, RuntimeError):
                        return PrivilegeLevel.ELEVATED
                else:
                    return PrivilegeLevel.ELEVATED
            else:
                return PrivilegeLevel.USER

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Windows privilege check failed: {e}")
            return PrivilegeLevel.UNKNOWN

    def _check_unix_privileges(self) -> PrivilegeLevel:
        """Check privilege level on Unix-like systems."""
        try:
            uid = os.getuid()

            if uid == 0:
                return PrivilegeLevel.ADMIN
            else:
                # Check if user can sudo
                try:
                    # Security: Use explicit command list and timeout
                    result = subprocess.run(
                        ["/usr/bin/sudo", "-n", "true"],
                        capture_output=True,
                        timeout=5,
                        shell=False,
                        check=False,
                    )
                    if result.returncode == 0:
                        return PrivilegeLevel.ELEVATED
                    else:
                        return PrivilegeLevel.USER
                except (OSError, ValueError, RuntimeError):
                    return PrivilegeLevel.USER

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Unix privilege check failed: {e}")
            return PrivilegeLevel.UNKNOWN

    def _check_system_permissions(self) -> None:
        """Check specific system permissions."""
        try:
            system = platform.system()

            if system == "Windows":
                self._check_windows_permissions()
            elif system in ["Linux", "Darwin"]:
                self._check_unix_permissions()
            else:
                logger.warning(f"Permission checking not implemented for {system}")

            self._emit_signal("system_ready", self._permissions)

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Permission check failed: {e}")

    def _check_windows_permissions(self) -> None:
        """Check Windows-specific permissions."""
        # Network configuration
        self._permissions.network_config = self._test_network_config_access()

        # Bluetooth control
        self._permissions.bluetooth_control = self._test_bluetooth_access()

        # Service management
        self._permissions.service_management = self._test_service_management_access()

        # Registry access
        self._permissions.registry_access = self._test_registry_access()

        # Hardware access
        self._permissions.hardware_access = self._test_hardware_access()

        # Firewall control
        self._permissions.firewall_control = self._test_firewall_access()

    def _check_unix_permissions(self) -> None:
        """Check Unix-specific permissions."""
        # Basic checks for Unix systems - should only be called on Unix-like systems
        if platform.system() == "Windows":
            logger.error("_check_unix_permissions called on Windows - this is a bug")
            return

        try:
            is_root = os.getuid() == 0
            can_sudo = self._can_sudo()

            self._permissions.network_config = is_root or can_sudo
            self._permissions.bluetooth_control = is_root or can_sudo
            self._permissions.service_management = is_root or can_sudo
            self._permissions.hardware_access = is_root
        except AttributeError:
            # os.getuid() not available (shouldn't happen on Unix systems)
            logger.error("os.getuid() not available - platform detection failed")
            # Fallback to checking sudo only
            can_sudo = self._can_sudo()
            self._permissions.network_config = can_sudo
            self._permissions.bluetooth_control = can_sudo
            self._permissions.service_management = can_sudo
            self._permissions.hardware_access = False

    def _perform_elevation(self, reason: str) -> ElevationResult:
        """Perform the actual privilege elevation."""
        system = platform.system()

        if system == "Windows":
            return self._elevate_windows(reason)
        elif system in ["Linux", "Darwin"]:
            return self._elevate_unix(reason)
        else:
            return ElevationResult.NOT_SUPPORTED

    def _elevate_windows(self, reason: str) -> ElevationResult:
        """Elevate privileges on Windows using UAC."""
        try:
            if ELEVATE_AVAILABLE:
                # Show user dialog before elevation
                reply = QMessageBox.question(
                    None,
                    "Administrator Privileges Required",
                    "IRCamera PC Controller needs administrator"
                    "privileges for:\n\n"
                    f"{reason}\n\n"
                    "This will allow full system integration including:\n"
                    "• WiFi network management\n"
                    "• Bluetooth device control\n"
                    "• Firewall configuration\n"
                    "• Service management\n\n"
                    "Would you like to continue?",
                    QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No,
                    QMessageBox.StandardButton.Yes,
                )

                if reply != QMessageBox.StandardButton.Yes:
                    return ElevationResult.CANCELLED

                # Attempt elevation
                elevate(show_console=False)
                return ElevationResult.SUCCESS
            else:
                # Fallback to manual UAC prompt
                return self._manual_uac_elevation()

        except PermissionError:
            return ElevationResult.CANCELLED
        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Windows elevation failed: {e}")
            return ElevationResult.FAILED

    def _elevate_unix(self, reason: str) -> ElevationResult:
        """Elevate privileges on Unix systems."""
        try:
            # Show user dialog
            reply = QMessageBox.question(
                None,
                "Administrator Privileges Required",
                "IRCamera PC Controller needs administrator"
                "privileges for:\n\n"
                f"{reason}\n\n"
                "Please run the application with sudo or as root.\n"
                "Would you like to restart with sudo?",
                QMessageBox.StandardButton.Yes | QMessageBox.StandardButton.No,
                QMessageBox.StandardButton.Yes,
            )

            if reply != QMessageBox.StandardButton.Yes:
                return ElevationResult.CANCELLED

            # Restart with sudo
            python_path = sys.executable
            script_path = sys.argv[0]

            # Security: Validate paths before subprocess call
            if not os.path.exists(python_path) or not os.path.exists(script_path):
                raise FileNotFoundError("Required executable paths not found")

            subprocess.Popen(
                ["/usr/bin/sudo", python_path, script_path] + sys.argv[1:],
                shell=False,
            )

            # Exit current process
            QApplication.quit()

            return ElevationResult.SUCCESS

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Unix elevation failed: {e}")
            return ElevationResult.FAILED

    def _manual_uac_elevation(self) -> ElevationResult:
        """Manual UAC elevation for Windows."""
        try:
            if WIN32_AVAILABLE:
                # Use ShellExecute with "runas" to trigger UAC
                python_path = sys.executable
                script_path = " ".join(sys.argv)

                result = ctypes.windll.shell32.ShellExecuteW(
                    None, "runas", python_path, script_path, None, 1
                )

                if result > 32:  # Success
                    # Exit current process as new elevated one will start
                    QApplication.quit()
                    return ElevationResult.SUCCESS
                else:
                    return ElevationResult.CANCELLED
            else:
                return ElevationResult.NOT_SUPPORTED

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Manual UAC elevation failed: {e}")
            return ElevationResult.FAILED

    def _get_result_message(self, result: ElevationResult) -> str:
        """Get user-friendly message for elevation result."""
        messages = {
            ElevationResult.SUCCESS: "Administrator privileges" "granted successfully",
            ElevationResult.CANCELLED: "Privilege elevation was" "cancelled by user",
            ElevationResult.FAILED: "Failed to obtain administrator" "privileges",
            ElevationResult.ALREADY_ELEVATED: (
                "Application already running with administrator privileges"
            ),
            ElevationResult.NOT_SUPPORTED: (
                "Privilege elevation not supported on this platform"
            ),
        }
        return messages.get(result, "Unknown elevation result")

    # Permission testing methods
    def _test_network_config_access(self) -> bool:
        """Test network configuration access."""
        try:
            if platform.system() == "Windows":
                # Security: Use full path and explicit security settings
                result = subprocess.run(
                    [
                        "C:\\Windows\\System32\\netsh.exe",
                        "interface",
                        "show",
                        "interface",
                    ],
                    capture_output=True,
                    timeout=10,
                    shell=False,
                    check=False,
                    text=True,
                )
                return result.returncode == 0
            return False
        except (OSError, ValueError, RuntimeError):
            return False

    def _test_bluetooth_access(self) -> bool:
        """Test Bluetooth control access."""
        return self.is_elevated  # Simplified check

    def _test_service_management_access(self) -> bool:
        """Test service management access."""
        try:
            if platform.system() == "Windows":
                # Security: Use full path and proper command structure
                result = subprocess.run(
                    ["C:\\Windows\\System32\\sc.exe", "query", "type=service"],
                    capture_output=True,
                    timeout=10,
                    shell=False,
                    check=False,
                    text=True,
                )
                return result.returncode == 0
            return False
        except (OSError, ValueError, RuntimeError):
            return False

    def _test_registry_access(self) -> bool:
        """Test Windows registry access."""
        if platform.system() != "Windows":
            return False

        try:
            import winreg

            key = winreg.OpenKey(
                winreg.HKEY_LOCAL_MACHINE, "SOFTWARE", 0, winreg.KEY_READ
            )
            winreg.CloseKey(key)
            return True
        except (OSError, ValueError, RuntimeError):
            return False

    def _test_hardware_access(self) -> bool:
        """Test hardware access permissions."""
        return self.is_elevated

    def _test_firewall_access(self) -> bool:
        """Test Windows Firewall access."""
        if platform.system() != "Windows":
            return False

        try:
            result = subprocess.run(
                ["netsh", "advfirewall", "show", "allprofiles"],
                capture_output=True,
                timeout=5,
            )
            return result.returncode == 0
        except (OSError, ValueError, RuntimeError):
            return False

    def _can_sudo(self) -> bool:
        """Check if user can use sudo."""
        # sudo is not available on Windows
        if platform.system() == "Windows":
            return False

        try:
            result = subprocess.run(
                ["sudo", "-n", "true"], capture_output=True, timeout=5
            )
            return result.returncode == 0
        except (OSError, ValueError, RuntimeError):
            return False

    # Command execution methods
    def _run_windows_admin_command(self, command: str, arguments: list) -> bool:
        """Run Windows admin command."""
        try:
            result = subprocess.run(
                [command] + arguments, capture_output=True, timeout=30
            )
            return result.returncode == 0
        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Windows admin command failed: {e}")
            return False

    def _run_unix_admin_command(self, command: str, arguments: list) -> bool:
        """Run Unix admin command."""
        try:
            if os.getuid() == 0:
                result = subprocess.run(
                    [command] + arguments, capture_output=True, timeout=30
                )
            else:
                result = subprocess.run(
                    ["sudo"] + [command] + arguments,
                    capture_output=True,
                    timeout=30,
                )
            return result.returncode == 0
        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Unix admin command failed: {e}")
            return False

    # Service management methods
    def _check_windows_service(self, service_name: str) -> Optional[str]:
        """Check Windows service status."""
        try:
            result = subprocess.run(
                ["sc", "query", service_name],
                capture_output=True,
                text=True,
                timeout=10,
            )

            if result.returncode == 0:
                # Parse service status from output
                for line in result.stdout.split("\n"):
                    if "STATE" in line:
                        return line.split(":")[1].strip()
            return None
        except (OSError, ValueError, RuntimeError):
            return None

    def _check_linux_service(self, service_name: str) -> Optional[str]:
        """Check Linux service status."""
        try:
            result = subprocess.run(
                ["systemctl", "is-active", service_name],
                capture_output=True,
                text=True,
                timeout=10,
            )
            return result.stdout.strip()
        except (OSError, ValueError, RuntimeError):
            return None

    def _check_macos_service(self, service_name: str) -> Optional[str]:
        """Check macOS service status."""
        try:
            result = subprocess.run(
                ["launchctl", "list", service_name],
                capture_output=True,
                text=True,
                timeout=10,
            )
            return "running" if result.returncode == 0 else "stopped"
        except (OSError, ValueError, RuntimeError):
            return None

    def _manage_windows_firewall_rule(
            self, rule_name: str, action: str, **kwargs
    ) -> bool:
        """Manage Windows Firewall rules."""
        try:
            base_cmd = ["netsh", "advfirewall", "firewall"]

            if action == "add":
                cmd = base_cmd + [
                    "add",
                    "rule",
                    f"name={rule_name}",
                    f"dir={kwargs.get('direction', 'in')}",
                    f"action={kwargs.get('action', 'allow')}",
                    f"protocol={kwargs.get('protocol', 'TCP')}",
                    f"localport={kwargs.get('port', '8080')}",
                ]
            elif action == "remove":
                cmd = base_cmd + ["delete", "rule", f"name={rule_name}"]
            else:
                logger.error(f"Unknown firewall action: {action}")
                return False

            result = subprocess.run(cmd, capture_output=True, timeout=30)
            return result.returncode == 0

        except (OSError, ValueError, RuntimeError) as e:
            logger.error(f"Firewall rule management failed: {e}")
            return False
