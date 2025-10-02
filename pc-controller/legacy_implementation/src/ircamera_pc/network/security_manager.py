import asyncio
import hashlib
import hmac
import logging
import secrets
import time
from cryptography import x509
from dataclasses import dataclass
from datetime import datetime
from enum import Enum
from pathlib import Path
from typing import Any, Dict, List, Optional, Set, Tuple

try:
    from loguru import logger
except ImportError:
    logger = logging.getLogger(__name__)

CERTIFICATE_VALIDITY_DAYS = 365
TOKEN_VALIDITY_HOURS = 24
MAX_FAILED_ATTEMPTS = 5
LOCKOUT_DURATION_MINUTES = 15
MONITORING_INTERVAL_SECONDS = 30

class AuthLevel(Enum):
    NONE = 0
    BASIC = 1
    CERTIFICATE = 2
    TOKEN = 3
    BIOMETRIC = 4

class DeviceRole(Enum):
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
    LOW = 1
    MEDIUM = 2
    HIGH = 3
    CRITICAL = 4

@dataclass
class AuthenticationContext:
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

class AdvancedAuthenticationManager:

    def __init__(self, cert_dir: Optional[Path] = None):
        self.cert_dir = cert_dir or Path("certificates")
        self.cert_dir.mkdir(exist_ok=True)

        self.authenticated_devices: Dict[str, AuthenticationContext] = {}
        self.failed_attempts: Dict[str, List[float]] = {}
        self.locked_devices: Dict[str, float] = {}

        self.advanced_credentials = {
            "admin": "admin",
            "researcher": "research2024!",
            "operator": "operate@safe",
            "observer": "view_only_123",
        }

        self.session_tokens: Dict[str, AuthenticationContext] = {}

        logger.info("Advanced authentication manager initialized")

    async def authenticate(
            self, device_id: str, auth_level: AuthLevel, credentials: Dict[str, Any]
    ) -> Tuple[bool, Optional[AuthenticationContext], str]:

        if self._is_device_locked(device_id):
            return False, None, "device_locked"

        try:

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

                context = await self._create_auth_context(
                    device_id, auth_level, credentials
                )
                self.authenticated_devices[device_id] = context
                self.session_tokens[context.session_token] = context

                self.failed_attempts.pop(device_id, None)
                self.locked_devices.pop(device_id, None)

                logger.info(
                    f"Authentication successful for device {device_id} at level {auth_level.name}"
                )
                return True, context, "success"
            else:

                await self._handle_auth_failure(device_id)
                return False, None, "invalid_credentials"

        except Exception as e:
            logger.error(f"Authentication error for device {device_id}: {e}")
            return False, None, "authentication_error"

    async def _authenticate_basic(self, credentials: Dict[str, Any]) -> bool:

        username = credentials.get("username", "")
        password = credentials.get("password", "")

        return (
                username in self.advanced_credentials
                and self.advanced_credentials[username] == password
        )

    async def _authenticate_certificate(
            self, device_id: str, credentials: Dict[str, Any]
    ) -> bool:

        cert_data = credentials.get("certificate")
        signature = credentials.get("signature")
        challenge = credentials.get("challenge")

        if not all([cert_data, signature, challenge]):
            return False

        try:

            certificate = x509.load_pem_x509_certificate(cert_data.encode())

            now = datetime.utcnow()
            if now < certificate.not_valid_before or now > certificate.not_valid_after:
                return False

            return True  # Placeholder for certificate validation

        except Exception as e:
            logger.error(f"Certificate authentication failed for {device_id}: {e}")
            return False

    async def _authenticate_token(
            self, device_id: str, credentials: Dict[str, Any]
    ) -> bool:

        token = credentials.get("token")
        timestamp = credentials.get("timestamp")
        hmac_signature = credentials.get("hmac")

        if not all([token, timestamp, hmac_signature]):
            return False

        if time.time() - timestamp > TOKEN_VALIDITY_HOURS * 3600:
            return False

        expected_hmac = self._generate_hmac(device_id, token, timestamp)
        return hmac.compare_digest(expected_hmac, hmac_signature)

    async def _authenticate_biometric(
            self, device_id: str, credentials: Dict[str, Any]
    ) -> bool:

        hardware_key = credentials.get("hardware_key")
        biometric_signature = credentials.get("biometric_signature")

        if not all([hardware_key, biometric_signature]):
            return False

        # Placeholder for hardware key verification

        return True

    async def _create_auth_context(
            self, device_id: str, auth_level: AuthLevel, credentials: Dict[str, Any]
    ) -> AuthenticationContext:

        device_type = credentials.get("device_type", "unknown")
        role = self._determine_role(device_type, auth_level, credentials)

        session_token = self._generate_session_token(device_id, role)

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

        token_data = f"{device_id}:{role.name}:{time.time()}:{secrets.token_hex(16)}"
        return hashlib.sha256(token_data.encode()).hexdigest()

    def _generate_hmac(self, device_id: str, token: str, timestamp: float) -> str:

        key = f"hmac_key_{device_id}".encode()
        message = f"{device_id}:{token}:{timestamp}".encode()
        return hmac.new(key, message, hashlib.sha256).hexdigest()

    async def _handle_auth_failure(self, device_id: str):

        current_time = time.time()

        if device_id not in self.failed_attempts:
            self.failed_attempts[device_id] = []

        self.failed_attempts[device_id].append(current_time)

        cutoff_time = current_time - 3600
        self.failed_attempts[device_id] = [
            attempt
            for attempt in self.failed_attempts[device_id]
            if attempt > cutoff_time
        ]

        if len(self.failed_attempts[device_id]) >= MAX_FAILED_ATTEMPTS:
            self.locked_devices[device_id] = current_time + (
                    LOCKOUT_DURATION_MINUTES * 60
            )
            logger.warning(
                f"Device {device_id} locked due to {MAX_FAILED_ATTEMPTS} failed attempts"
            )

    def _is_device_locked(self, device_id: str) -> bool:

        if device_id in self.locked_devices:
            if time.time() < self.locked_devices[device_id]:
                return True
            else:

                del self.locked_devices[device_id]
        return False

    def validate_session_token(self, token: str) -> Optional[AuthenticationContext]:

        context = self.session_tokens.get(token)
        if context and context.is_valid():
            return context
        elif context:

            self.session_tokens.pop(token, None)
            self.authenticated_devices.pop(context.device_id, None)
        return None

    def has_permission(self, token: str, permission: str) -> bool:

        context = self.validate_session_token(token)
        return context and context.role.has_permission(permission)

    def logout_device(self, device_id: str) -> bool:

        context = self.authenticated_devices.pop(device_id, None)
        if context:
            self.session_tokens.pop(context.session_token, None)
            logger.info(f"Device {device_id} logged out")
            return True
        return False

    def get_active_sessions(self) -> List[AuthenticationContext]:

        time.time()
        active_sessions = []

        for context in list(self.authenticated_devices.values()):
            if context.is_valid():
                active_sessions.append(context)
            else:

                self.authenticated_devices.pop(context.device_id, None)
                self.session_tokens.pop(context.session_token, None)

        return active_sessions

    def get_diagnostics(self) -> Dict[str, Any]:

        return {
            "active_sessions": len(self.get_active_sessions()),
            "locked_devices": len(
                [d for d, t in self.locked_devices.items() if time.time() < t]
            ),
            "total_devices_seen": len(self.failed_attempts),
            "advanced_auth_enabled": True,
            "supported_auth_levels": [level.name for level in AuthLevel],
        }

class AdvancedSecurityMonitor:

    def __init__(self):
        self.is_monitoring = False
        self.security_alerts: List[SecurityAlert] = []
        self.connection_attempts: Dict[str, List[float]] = {}
        self.suspicious_activities: Dict[str, int] = {}

        self.total_connections = 0
        self.total_failed_logins = 0
        self.total_alerts = 0

        logger.info("Advanced security monitor initialized")

    async def start_monitoring(self) -> Any:

        if self.is_monitoring:
            return

        self.is_monitoring = True

        asyncio.create_task(self._monitoring_loop())

        logger.info("Security monitoring started")

    def stop_monitoring(self) -> Any:

        self.is_monitoring = False
        logger.info("Security monitoring stopped")

    async def _monitoring_loop(self):

        while self.is_monitoring:
            try:
                await self._perform_security_check()
                await asyncio.sleep(MONITORING_INTERVAL_SECONDS)
            except Exception as e:
                logger.error(f"Error in security monitoring loop: {e}")

    async def _perform_security_check(self):

        current_time = time.time()

        await self._check_brute_force_attacks()

        await self._check_connection_patterns()

        await self._cleanup_old_data(current_time)

    async def _check_brute_force_attacks(self):

        current_time = time.time()
        cutoff_time = current_time - 3600

        for device_id, attempts in self.connection_attempts.items():
            recent_attempts = [a for a in attempts if a > cutoff_time]

            if len(recent_attempts) >= 10:
                await self._generate_alert(
                    "brute_force_attack",
                    AlertSeverity.HIGH,
                    device_id,
                    f"Brute force attack detected: {len(recent_attempts)} attempts in 1 hour",
                    {"attempts_count": len(recent_attempts), "time_window": "1_hour"},
                )

    async def _check_connection_patterns(self):

        current_time = time.time()
        cutoff_time = current_time - 60

        for device_id, attempts in self.connection_attempts.items():
            recent_attempts = [a for a in attempts if a > cutoff_time]

            if len(recent_attempts) > 10:
                await self._generate_alert(
                    "suspicious_connection",
                    AlertSeverity.MEDIUM,
                    device_id,
                    f"Suspicious connection pattern: {len(recent_attempts)} attempts/minute",
                    {"connections_per_minute": len(recent_attempts)},
                )

    async def _cleanup_old_data(self, current_time: float):

        cutoff_time = current_time - (24 * 3600)

        for device_id in list(self.connection_attempts.keys()):
            self.connection_attempts[device_id] = [
                a for a in self.connection_attempts[device_id] if a > cutoff_time
            ]
            if not self.connection_attempts[device_id]:
                del self.connection_attempts[device_id]

        self.security_alerts = [
            alert for alert in self.security_alerts if alert.timestamp > cutoff_time
        ]

    def report_connection_attempt(
            self, device_id: str, successful: bool, details: Optional[Dict[str, Any]] = None
    ) -> None:

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

        if len(self.security_alerts) > 1000:
            self.security_alerts = self.security_alerts[-1000:]

        logger.warning(
            f"Security alert generated: {alert.alert_type} for device {device_id}"
        )

    def get_security_alerts(self, limit: int = 100) -> List[Dict[str, Any]]:

        return [alert.to_dict() for alert in self.security_alerts[-limit:]]

    def acknowledge_alert(self, alert_id: str) -> bool:

        for alert in self.security_alerts:
            if alert.id == alert_id:
                alert.acknowledged = True
                logger.info(f"Alert acknowledged: {alert_id}")
                return True
        return False

    def get_monitoring_statistics(self) -> Dict[str, Any]:

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

    def __init__(self, cert_dir: Optional[Path] = None):
        self.auth_manager = AdvancedAuthenticationManager(cert_dir)
        self.security_monitor = AdvancedSecurityMonitor()

        logger.info("Enhanced security manager initialized")

    async def initialize(self) -> Any:
        await self.security_monitor.start_monitoring()
        logger.info("Enhanced security system fully initialized")

    def shutdown(self) -> Any:
        self.security_monitor.stop_monitoring()
        logger.info("Enhanced security system shutdown complete")

    async def authenticate_device(
            self, device_id: str, auth_level: AuthLevel, credentials: Dict[str, Any]
    ) -> Tuple[bool, Optional[AuthenticationContext], str]:
        success, context, reason = await self.auth_manager.authenticate(
            device_id, auth_level, credentials
        )

        self.security_monitor.report_connection_attempt(
            device_id, success, {"auth_level": auth_level.name, "reason": reason}
        )

        return success, context, reason

    def validate_session(self, token: str) -> Optional[AuthenticationContext]:
        return self.auth_manager.validate_session_token(token)

    def check_permission(self, token: str, permission: str) -> bool:
        return self.auth_manager.has_permission(token, permission)

    def get_comprehensive_diagnostics(self) -> Dict[str, Any]:
        return {
            "authentication": self.auth_manager.get_diagnostics(),
            "monitoring": self.security_monitor.get_monitoring_statistics(),
            "security_alerts": self.security_monitor.get_security_alerts(10),
            "phase4_enabled": True,
            "timestamp": time.time(),
        }
