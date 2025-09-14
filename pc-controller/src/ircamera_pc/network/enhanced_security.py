"""
Enhanced Security Module for PC Controller - Phase 4 Implementation

Provides advanced authentication, certificate management, and security monitoring
to match the Android implementation.
"""

import asyncio
import hashlib
import hmac
import json
import logging
import secrets
import ssl
import time
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from dataclasses import asdict, dataclass
from datetime import datetime, timedelta
from enum import Enum
from pathlib import Path
from typing import Any, Dict, List, Optional, Set, Tuple

try:
    from loguru import logger
except ImportError:
    logger = logging.getLogger(__name__)

# Configuration
CERTIFICATE_VALIDITY_DAYS = 365
TOKEN_VALIDITY_HOURS = 24
MAX_FAILED_ATTEMPTS = 5
LOCKOUT_DURATION_MINUTES = 15
MONITORING_INTERVAL_SECONDS = 30


class AuthLevel(Enum):
    """Authentication levels matching Android implementation"""

    NONE = 0
    BASIC = 1
    CERTIFICATE = 2
    TOKEN = 3
    BIOMETRIC = 4


class DeviceRole(Enum):
    """Device roles with permissions"""

    GUEST = (0, {"view_status"})
    OBSERVER = (1, {"view_status", "view_sessions", "download_data"})
    OPERATOR = (
        2,
        {
            "view_status",
            "view_sessions",
            "download_data",
            "start_recording",
            "stop_recording",
        },
    )
    RESEARCHER = (
        3,
        {
            "view_status",
            "view_sessions",
            "download_data",
            "start_recording",
            "stop_recording",
            "manage_sessions",
            "export_data",
        },
    )
    ADMINISTRATOR = (4, {"*"})

    def __init__(self, level: int, permissions: Set[str]):
        self.level = level
        self.permissions = permissions

    def has_permission(self, permission: str) -> bool:
        return "*" in self.permissions or permission in self.permissions


class AlertSeverity(Enum):
    """Security alert severity levels"""

    LOW = 1
    MEDIUM = 2
    HIGH = 3
    CRITICAL = 4


@dataclass
class AuthenticationContext:
    """Authentication context for authenticated devices"""

    device_id: str
    auth_level: AuthLevel
    role: DeviceRole
    session_token: str
    expiry_time: float
    capabilities: Set[str]
    created_at: Optional[float] = None

    def __post_init__(self):
        if self.created_at is None:
            self.created_at = time.time()

    def is_valid(self) -> bool:
        return time.time() < self.expiry_time

    def to_dict(self) -> Dict[str, Any]:
        return {
            "device_id": self.device_id,
            "auth_level": self.auth_level.value,
            "role": self.role.name,
            "session_token": self.session_token,
            "expiry_time": self.expiry_time,
            "capabilities": list(self.capabilities),
            "created_at": self.created_at,
        }


@dataclass
class SecurityAlert:
    """Security alert data structure"""

    id: str
    alert_type: str
    severity: AlertSeverity
    device_id: str
    timestamp: float
    description: str
    details: Dict[str, Any]
    acknowledged: bool = False

    def to_dict(self) -> Dict[str, Any]:
        return {
            "id": self.id,
            "type": self.alert_type,
            "severity": self.severity.name,
            "device_id": self.device_id,
            "timestamp": self.timestamp,
            "description": self.description,
            "details": self.details,
            "acknowledged": self.acknowledged,
        }


class EnhancedAuthenticationManager:
    """Advanced authentication manager for PC Controller"""

    def __init__(self, cert_dir: Optional[Path] = None):
        self.cert_dir = cert_dir or Path("certificates")
        self.cert_dir.mkdir(exist_ok=True)

        # Authentication state
        self.authenticated_devices: Dict[str, AuthenticationContext] = {}
        self.failed_attempts: Dict[str, List[float]] = {}
        self.locked_devices: Dict[str, float] = {}  # device_id: unlock_time

        # Enhanced credentials beyond admin/admin
        self.enhanced_credentials = {
            "admin": "admin",
            "researcher": "research2024!",
            "operator": "operate@safe",
            "observer": "view_only_123",
        }

        # Session management
        self.session_tokens: Dict[str, AuthenticationContext] = {}

        logger.info("Enhanced authentication manager initialized")

    async def authenticate(
            self, device_id: str, auth_level: AuthLevel, credentials: Dict[str, Any]
    ) -> Tuple[bool, Optional[AuthenticationContext], str]:
        """
        Perform multi-tier authentication

        Returns:
            Tuple[bool, Optional[AuthenticationContext], str]: (success, context,
                reason)
        """

        # Check if device is locked
        if self._is_device_locked(device_id):
            return False, None, "device_locked"

        try:
            # Perform authentication based on level
            if auth_level == AuthLevel.BASIC:
                success = await self._authenticate_basic(credentials)
            elif auth_level == AuthLevel.CERTIFICATE:
                success = await self._authenticate_certificate(device_id, credentials)
            elif auth_level == AuthLevel.TOKEN:
                success = await self._authenticate_token(device_id, credentials)
            elif auth_level == AuthLevel.BIOMETRIC:
                success = await self._authenticate_biometric(device_id, credentials)
            else:
                success = False

            if success:
                # Create authentication context
                context = await self._create_auth_context(
                    device_id, auth_level, credentials
                )
                self.authenticated_devices[device_id] = context
                self.session_tokens[context.session_token] = context

                # Clear failed attempts
                self.failed_attempts.pop(device_id, None)
                self.locked_devices.pop(device_id, None)

                logger.info(
                    f"Authentication successful for device {device_id} at level {auth_level.name}"
                )
                return True, context, "success"
            else:
                # Handle failed authentication
                await self._handle_auth_failure(device_id)
                return False, None, "invalid_credentials"

        except Exception as e:
            logger.error(f"Authentication error for device {device_id}: {e}")
            return False, None, "authentication_error"

    async def _authenticate_basic(self, credentials: Dict[str, Any]) -> bool:
        """Basic username/password authentication"""
        username = credentials.get("username", "")
        password = credentials.get("password", "")

        return (
                username in self.enhanced_credentials
                and self.enhanced_credentials[username] == password
        )

    async def _authenticate_certificate(
            self, device_id: str, credentials: Dict[str, Any]
    ) -> bool:
        """Certificate-based authentication"""
        cert_data = credentials.get("certificate")
        signature = credentials.get("signature")
        challenge = credentials.get("challenge")

        if not all([cert_data, signature, challenge]):
            return False

        try:
            # Load and validate certificate
            certificate = x509.load_pem_x509_certificate(cert_data.encode())

            # Check certificate validity
            now = datetime.utcnow()
            if now < certificate.not_valid_before or now > certificate.not_valid_after:
                return False

            # Verify signature (simplified - in production would verify with certificate public key)
            return True  # Placeholder for certificate validation

        except Exception as e:
            logger.error(f"Certificate authentication failed for {device_id}: {e}")
            return False

    async def _authenticate_token(
            self, device_id: str, credentials: Dict[str, Any]
    ) -> bool:
        """Token-based authentication with HMAC"""
        token = credentials.get("token")
        timestamp = credentials.get("timestamp")
        hmac_signature = credentials.get("hmac")

        if not all([token, timestamp, hmac_signature]):
            return False

        # Check token age
        if time.time() - timestamp > TOKEN_VALIDITY_HOURS * 3600:
            return False

        # Verify HMAC
        expected_hmac = self._generate_hmac(device_id, token, timestamp)
        return hmac.compare_digest(expected_hmac, hmac_signature)

    async def _authenticate_biometric(
            self, device_id: str, credentials: Dict[str, Any]
    ) -> bool:
        """Biometric/hardware key authentication"""
        hardware_key = credentials.get("hardware_key")
        biometric_signature = credentials.get("biometric_signature")

        if not all([hardware_key, biometric_signature]):
            return False

        # Placeholder for hardware key verification
        # In production, this would verify against stored hardware keys
        return True

    async def _create_auth_context(
            self, device_id: str, auth_level: AuthLevel, credentials: Dict[str, Any]
    ) -> AuthenticationContext:
        """Create authentication context for successful authentication"""

        # Determine role based on device type and auth level
        device_type = credentials.get("device_type", "unknown")
        role = self._determine_role(device_type, auth_level, credentials)

        # Generate session token
        session_token = self._generate_session_token(device_id, role)

        # Set expiry time
        expiry_time = time.time() + (TOKEN_VALIDITY_HOURS * 3600)

        return AuthenticationContext(
            device_id=device_id,
            auth_level=auth_level,
            role=role,
            session_token=session_token,
            expiry_time=expiry_time,
            capabilities=role.permissions,
        )

    def _determine_role(
            self, device_type: str, auth_level: AuthLevel, credentials: Dict[str, Any]
    ) -> DeviceRole:
        """Determine device role based on context"""

        # Role mapping based on device type and auth level
        if device_type == "PC_CONTROLLER":
            return DeviceRole.ADMINISTRATOR
        elif device_type == "ANDROID_PHONE":
            if auth_level in [AuthLevel.TOKEN, AuthLevel.BIOMETRIC]:
                return DeviceRole.RESEARCHER
            elif auth_level == AuthLevel.CERTIFICATE:
                return DeviceRole.OPERATOR
            else:
                return DeviceRole.OBSERVER
        elif device_type in ["THERMAL_CAMERA", "SHIMMER_SENSOR"]:
            return DeviceRole.OBSERVER
        else:
            return DeviceRole.GUEST

    def _generate_session_token(self, device_id: str, role: DeviceRole) -> str:
        """Generate secure session token"""
        token_data = f"{device_id}:{role.name}:{time.time()}:{secrets.token_hex(16)}"
        return hashlib.sha256(token_data.encode()).hexdigest()

    def _generate_hmac(self, device_id: str, token: str, timestamp: float) -> str:
        """Generate HMAC for token authentication"""
        key = f"hmac_key_{device_id}".encode()
        message = f"{device_id}:{token}:{timestamp}".encode()
        return hmac.new(key, message, hashlib.sha256).hexdigest()

    async def _handle_auth_failure(self, device_id: str):
        """Handle authentication failure"""
        current_time = time.time()

        # Track failed attempts
        if device_id not in self.failed_attempts:
            self.failed_attempts[device_id] = []

        self.failed_attempts[device_id].append(current_time)

        # Remove old attempts (older than 1 hour)
        cutoff_time = current_time - 3600
        self.failed_attempts[device_id] = [
            attempt
            for attempt in self.failed_attempts[device_id]
            if attempt > cutoff_time
        ]

        # Lock device if too many failures
        if len(self.failed_attempts[device_id]) >= MAX_FAILED_ATTEMPTS:
            self.locked_devices[device_id] = current_time + (
                    LOCKOUT_DURATION_MINUTES * 60
            )
            logger.warning(
                f"Device {device_id} locked due to {MAX_FAILED_ATTEMPTS} failed attempts"
            )

    def _is_device_locked(self, device_id: str) -> bool:
        """Check if device is currently locked"""
        if device_id in self.locked_devices:
            if time.time() < self.locked_devices[device_id]:
                return True
            else:
                # Lock expired
                del self.locked_devices[device_id]
        return False

    def validate_session_token(self, token: str) -> Optional[AuthenticationContext]:
        """Validate session token and return context if valid"""
        context = self.session_tokens.get(token)
        if context and context.is_valid():
            return context
        elif context:
            # Token expired, remove it
            self.session_tokens.pop(token, None)
            self.authenticated_devices.pop(context.device_id, None)
        return None

    def has_permission(self, token: str, permission: str) -> bool:
        """Check if token has specific permission"""
        context = self.validate_session_token(token)
        return context and context.role.has_permission(permission)

    def logout_device(self, device_id: str) -> bool:
        """Logout device and invalidate session"""
        context = self.authenticated_devices.pop(device_id, None)
        if context:
            self.session_tokens.pop(context.session_token, None)
            logger.info(f"Device {device_id} logged out")
            return True
        return False

    def get_active_sessions(self) -> List[AuthenticationContext]:
        """Get all active authentication sessions"""
        current_time = time.time()
        active_sessions = []

        for context in list(self.authenticated_devices.values()):
            if context.is_valid():
                active_sessions.append(context)
            else:
                # Remove expired session
                self.authenticated_devices.pop(context.device_id, None)
                self.session_tokens.pop(context.session_token, None)

        return active_sessions

    def get_diagnostics(self) -> Dict[str, Any]:
        """Get authentication system diagnostics"""
        return {
            "active_sessions": len(self.get_active_sessions()),
            "locked_devices": len(
                [d for d, t in self.locked_devices.items() if time.time() < t]
            ),
            "total_devices_seen": len(self.failed_attempts),
            "enhanced_auth_enabled": True,
            "supported_auth_levels": [level.name for level in AuthLevel],
        }


class EnhancedSecurityMonitor:
    """Security monitoring system for PC Controller"""

    def __init__(self):
        self.is_monitoring = False
        self.security_alerts: List[SecurityAlert] = []
        self.connection_attempts: Dict[str, List[float]] = {}
        self.suspicious_activities: Dict[str, int] = {}

        # Statistics
        self.total_connections = 0
        self.total_failed_logins = 0
        self.total_alerts = 0

        logger.info("Enhanced security monitor initialized")

    async def start_monitoring(self) -> Any:
        """Start security monitoring"""
        if self.is_monitoring:
            return

        self.is_monitoring = True

        # Start monitoring loop
        asyncio.create_task(self._monitoring_loop())

        logger.info("Security monitoring started")

    def stop_monitoring(self) -> Any:
        """Stop security monitoring"""
        self.is_monitoring = False
        logger.info("Security monitoring stopped")

    async def _monitoring_loop(self):
        """Main monitoring loop"""
        while self.is_monitoring:
            try:
                await self._perform_security_check()
                await asyncio.sleep(MONITORING_INTERVAL_SECONDS)
            except Exception as e:
                logger.error(f"Error in security monitoring loop: {e}")

    async def _perform_security_check(self):
        """Perform comprehensive security check"""
        current_time = time.time()

        # Check for brute force attacks
        await self._check_brute_force_attacks()

        # Check for unusual connection patterns
        await self._check_connection_patterns()

        # Clean up old data
        await self._cleanup_old_data(current_time)

    async def _check_brute_force_attacks(self):
        """Check for brute force attack patterns"""
        current_time = time.time()
        cutoff_time = current_time - 3600  # Last hour

        for device_id, attempts in self.connection_attempts.items():
            recent_attempts = [a for a in attempts if a > cutoff_time]

            if len(recent_attempts) >= 10:  # 10 attempts in 1 hour
                await self._generate_alert(
                    "brute_force_attack",
                    AlertSeverity.HIGH,
                    device_id,
                    f"Brute force attack detected: {len(recent_attempts)} attempts in 1 hour",
                    {"attempts_count": len(recent_attempts), "time_window": "1_hour"},
                )

    async def _check_connection_patterns(self):
        """Check for suspicious connection patterns"""
        current_time = time.time()
        cutoff_time = current_time - 60  # Last minute

        for device_id, attempts in self.connection_attempts.items():
            recent_attempts = [a for a in attempts if a > cutoff_time]

            if len(recent_attempts) > 10:  # More than 10 attempts per minute
                await self._generate_alert(
                    "suspicious_connection",
                    AlertSeverity.MEDIUM,
                    device_id,
                    f"Suspicious connection pattern: {len(recent_attempts)} attempts/minute",
                    {"connections_per_minute": len(recent_attempts)},
                )

    async def _cleanup_old_data(self, current_time: float):
        """Clean up old monitoring data"""
        cutoff_time = current_time - (24 * 3600)  # 24 hours

        # Clean old connection attempts
        for device_id in list(self.connection_attempts.keys()):
            self.connection_attempts[device_id] = [
                a for a in self.connection_attempts[device_id] if a > cutoff_time
            ]
            if not self.connection_attempts[device_id]:
                del self.connection_attempts[device_id]

        # Clean old alerts
        self.security_alerts = [
            alert for alert in self.security_alerts if alert.timestamp > cutoff_time
        ]

    def report_connection_attempt(
            self, device_id: str, successful: bool, details: Optional[Dict[str, Any]] = None
    ) -> None:
        """Report connection attempt for monitoring"""
        current_time = time.time()

        if device_id not in self.connection_attempts:
            self.connection_attempts[device_id] = []

        self.connection_attempts[device_id].append(current_time)
        self.total_connections += 1

        if not successful:
            self.total_failed_logins += 1

        logger.debug(
            f"Connection attempt reported: {device_id} ({'success' if successful else 'failed'})"
        )

    async def _generate_alert(
            self,
            alert_type: str,
            severity: AlertSeverity,
            device_id: str,
            description: str,
            details: Dict[str, Any],
    ):
        """Generate security alert"""
        alert = SecurityAlert(
            id=f"ALERT_{int(time.time())}_{secrets.randbelow(1000)}",
            alert_type=alert_type,
            severity=severity,
            device_id=device_id,
            timestamp=time.time(),
            description=description,
            details=details or {},
        )

        self.security_alerts.append(alert)
        self.total_alerts += 1

        # Keep only last 1000 alerts
        if len(self.security_alerts) > 1000:
            self.security_alerts = self.security_alerts[-1000:]

        logger.warning(
            f"Security alert generated: {alert.alert_type} for device {device_id}"
        )

    def get_security_alerts(self, limit: int = 100) -> List[Dict[str, Any]]:
        """Get recent security alerts"""
        return [alert.to_dict() for alert in self.security_alerts[-limit:]]

    def acknowledge_alert(self, alert_id: str) -> bool:
        """Acknowledge security alert"""
        for alert in self.security_alerts:
            if alert.id == alert_id:
                alert.acknowledged = True
                logger.info(f"Alert acknowledged: {alert_id}")
                return True
        return False

    def get_monitoring_statistics(self) -> Dict[str, Any]:
        """Get monitoring statistics"""
        return {
            "monitoring_active": self.is_monitoring,
            "total_connections": self.total_connections,
            "total_failed_logins": self.total_failed_logins,
            "total_alerts": self.total_alerts,
            "recent_alerts_count": len(
                [a for a in self.security_alerts if time.time() - a.timestamp < 3600]
            ),
            "monitored_devices": len(self.connection_attempts),
        }


class EnhancedSecurityManager:
    """Main security manager integrating all Phase 4 security features"""

    def __init__(self, cert_dir: Optional[Path] = None):
        self.auth_manager = EnhancedAuthenticationManager(cert_dir)
        self.security_monitor = EnhancedSecurityMonitor()

        logger.info("Enhanced security manager initialized")

    async def initialize(self) -> Any:
        """Initialize all security components"""
        await self.security_monitor.start_monitoring()
        logger.info("Enhanced security system fully initialized")

    def shutdown(self) -> Any:
        """Shutdown security system"""
        self.security_monitor.stop_monitoring()
        logger.info("Enhanced security system shutdown complete")

    async def authenticate_device(
            self, device_id: str, auth_level: AuthLevel, credentials: Dict[str, Any]
    ) -> Tuple[bool, Optional[AuthenticationContext], str]:
        """Authenticate device with security monitoring"""

        # Report connection attempt to monitor
        success, context, reason = await self.auth_manager.authenticate(
            device_id, auth_level, credentials
        )

        self.security_monitor.report_connection_attempt(
            device_id, success, {"auth_level": auth_level.name, "reason": reason}
        )

        return success, context, reason

    def validate_session(self, token: str) -> Optional[AuthenticationContext]:
        """Validate session token"""
        return self.auth_manager.validate_session_token(token)

    def check_permission(self, token: str, permission: str) -> bool:
        """Check if session has permission"""
        return self.auth_manager.has_permission(token, permission)

    def get_comprehensive_diagnostics(self) -> Dict[str, Any]:
        """Get comprehensive security diagnostics"""
        return {
            "authentication": self.auth_manager.get_diagnostics(),
            "monitoring": self.security_monitor.get_monitoring_statistics(),
            "security_alerts": self.security_monitor.get_security_alerts(10),
            "phase4_enabled": True,
            "timestamp": time.time(),
        }
