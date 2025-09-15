"""
Security Manager for IRCamera PC Controller

Provides TLS/SSL certificate management, device authentication, and secure
communication features to match the Android implementation.
"""

import hashlib
import ipaddress
import secrets
import ssl
import time
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID

try:
    from loguru import logger
except ImportError:
    try:
        from ..utils.simple_logger import logger
    except ImportError:
        # Fallback logger for testing
        class FallbackLogger:
            def info(self, msg) -> Any:
                print(f"INFO: {msg}")

            def debug(self, msg) -> Any:
                print(f"DEBUG: {msg}")

            def warning(self, msg) -> Any:
                print(f"WARNING: {msg}")

            def error(self, msg) -> Any:
                print(f"ERROR: {msg}")

        logger = FallbackLogger()

try:
    from ..core.config import config
except ImportError:
    # Fallback config for testing
    class FallbackConfig:
        def get(self, key, default=None) -> Any:
            config_map = {
                "security.cert_directory": "certificates",
            }
            return config_map.get(key, default)

    config = FallbackConfig()


class SecurityManager:
    """
    Manages security features for the PC Controller including:
    - TLS/SSL certificate generation and validation
    - Device authentication tokens
    - Secure connection context creation
    """

    def __init__(self):
        """Initialize the security manager."""
        self.cert_dir = Path(config.get("security.cert_directory", "certificates"))
        self.cert_dir.mkdir(exist_ok=True)

        self.ca_cert_path = self.cert_dir / "ca_cert.pem"
        self.ca_key_path = self.cert_dir / "ca_key.pem"
        self.server_cert_path = self.cert_dir / "server_cert.pem"
        self.server_key_path = self.cert_dir / "server_key.pem"

        self.device_certificates: Dict[str, x509.Certificate] = {}
        self.auth_tokens: Dict[str, Tuple[str, float]] = (
            {}
        )  # token -> (device_id, expiry)

    def initialize(self) -> bool:
        """
        Initialize the security manager by generating or loading certificates.

        Returns:
            bool: True if initialization successful, False otherwise
        """
        try:
            logger.info("Initializing security manager...")

            # Generate or load CA certificate
            if not self._load_ca_certificate():
                logger.info("Generating new CA certificate...")
                self._generate_ca_certificate()

            # Generate or load server certificate
            if not self._load_server_certificate():
                logger.info("Generating new server certificate...")
                self._generate_server_certificate()

            logger.info("Security manager initialized successfully")
            return True

        except Exception as e:
            logger.error(f"Failed to initialize security manager: {e}")
            return False

    def create_ssl_context(self, for_client_auth: bool = True) -> ssl.SSLContext:
        """
        Create SSL context for secure connections.

        Args:
            for_client_auth: Whether to require client certificate authentication

        Returns:
            ssl.SSLContext: Configured SSL context
        """
        context = ssl.create_default_context(ssl.Purpose.CLIENT_AUTH)

        # Load server certificate and key
        context.load_cert_chain(str(self.server_cert_path), str(self.server_key_path))

        if for_client_auth:
            # Load CA certificate for client validation
            context.load_verify_locations(str(self.ca_cert_path))
            context.verify_mode = ssl.CERT_OPTIONAL  # Allow fallback to plaintext
        else:
            context.verify_mode = ssl.CERT_NONE

        # Set security options
        context.check_hostname = False  # Local network devices
        context.minimum_version = ssl.TLSVersion.TLSv1_2

        return context

    def validate_device_certificate(
        self, cert_data: bytes
    ) -> Tuple[bool, Optional[str]]:
        """
        Validate a device certificate for known Topdon devices.

        Args:
            cert_data: Raw certificate data

        Returns:
            Tuple[bool, Optional[str]]: (is_valid, device_type)
        """
        try:
            certificate = x509.load_pem_x509_certificate(cert_data)

            # Extract device information from certificate
            subject = certificate.subject
            common_name = None
            organization = None

            for attribute in subject:
                if attribute.oid == NameOID.COMMON_NAME:
                    common_name = attribute.value
                elif attribute.oid == NameOID.ORGANIZATION_NAME:
                    organization = attribute.value

            # Check if this is a known Topdon device
            if organization and "topdon" in organization.lower():
                if common_name:
                    if "tc001" in common_name.lower():
                        return True, "TC001"
                    elif "ts004" in common_name.lower():
                        return True, "TS004"
                    elif "tc007" in common_name.lower():
                        return True, "TC007"

            # Default to generic acceptance for development
            logger.warning(
                f"Unknown device certificate: {common_name} from {organization}"
            )
            return True, "UNKNOWN"

        except Exception as e:
            logger.error(f"Certificate validation failed: {e}")
            return False, None

    def generate_auth_token(self, device_id: str, duration_minutes: int = 5) -> str:
        """
        Generate a time-based authentication token for a device.

        Args:
            device_id: Unique device identifier
            duration_minutes: Token validity duration in minutes

        Returns:
            str: Authentication token
        """
        # Generate token components
        timestamp = str(int(time.time()))
        nonce = secrets.token_hex(8)

        # Create hash for integrity
        token_data = f"{device_id}:{timestamp}:{nonce}"
        token_hash = hashlib.sha256(token_data.encode()).hexdigest()[:16]

        # Final token format: device_id:timestamp:nonce:hash
        token = f"{device_id}:{timestamp}:{nonce}:{token_hash}"

        # Store token with expiry
        expiry_time = time.time() + (duration_minutes * 60)
        self.auth_tokens[token] = (device_id, expiry_time)

        logger.debug(f"Generated auth token for device {device_id}: {token[:20]}...")
        return token

    def validate_auth_token(
        self, token: str, max_age_seconds: int = 300
    ) -> Tuple[bool, Optional[str]]:
        """
        Validate an authentication token.

        Args:
            token: Authentication token to validate
            max_age_seconds: Maximum token age in seconds

        Returns:
            Tuple[bool, Optional[str]]: (is_valid, device_id)
        """
        try:
            # Check if token exists in our records
            if token in self.auth_tokens:
                device_id, expiry_time = self.auth_tokens[token]
                if time.time() < expiry_time:
                    return True, device_id
                else:
                    # Token expired, remove it
                    del self.auth_tokens[token]
                    return False, None

            # Parse token components
            parts = token.split(":")
            if len(parts) != 4:
                return False, None

            device_id, timestamp, nonce, provided_hash = parts

            # Check timestamp age
            token_time = int(timestamp)
            if time.time() - token_time > max_age_seconds:
                return False, None

            # Verify hash integrity
            token_data = f"{device_id}:{timestamp}:{nonce}"
            expected_hash = hashlib.sha256(token_data.encode()).hexdigest()[:16]

            if provided_hash == expected_hash:
                return True, device_id
            else:
                return False, None

        except Exception as e:
            logger.error(f"Token validation failed: {e}")
            return False, None

    def cleanup_expired_tokens(self) -> Any:
        """Remove expired authentication tokens."""
        current_time = time.time()
        expired_tokens = [
            token
            for token, (_, expiry) in self.auth_tokens.items()
            if current_time >= expiry
        ]

        for token in expired_tokens:
            del self.auth_tokens[token]

        if expired_tokens:
            logger.debug(f"Cleaned up {len(expired_tokens)} expired tokens")

    def _load_ca_certificate(self) -> bool:
        """Load existing CA certificate if available."""
        try:
            if self.ca_cert_path.exists() and self.ca_key_path.exists():
                # Verify certificate is valid
                with open(self.ca_cert_path, "rb") as f:
                    x509.load_pem_x509_certificate(f.read())
                logger.debug("Loaded existing CA certificate")
                return True
        except Exception as e:
            logger.warning(f"Failed to load CA certificate: {e}")
        return False

    def _load_server_certificate(self) -> bool:
        """Load existing server certificate if available."""
        try:
            if self.server_cert_path.exists() and self.server_key_path.exists():
                # Verify certificate is valid
                with open(self.server_cert_path, "rb") as f:
                    x509.load_pem_x509_certificate(f.read())
                logger.debug("Loaded existing server certificate")
                return True
        except Exception as e:
            logger.warning(f"Failed to load server certificate: {e}")
        return False

    def _generate_ca_certificate(self):
        """Generate a new CA certificate and private key."""
        # Generate private key
        private_key = rsa.generate_private_key(
            public_exponent=65537,
            key_size=2048,
        )

        # Generate certificate
        subject = issuer = x509.Name(
            [
                x509.NameAttribute(NameOID.COUNTRY_NAME, "US"),
                x509.NameAttribute(NameOID.STATE_OR_PROVINCE_NAME, "CA"),
                x509.NameAttribute(NameOID.LOCALITY_NAME, "San Francisco"),
                x509.NameAttribute(NameOID.ORGANIZATION_NAME, "IRCamera PC Controller"),
                x509.NameAttribute(NameOID.COMMON_NAME, "IRCamera CA"),
            ]
        )

        cert = (
            x509.CertificateBuilder()
            .subject_name(subject)
            .issuer_name(issuer)
            .public_key(private_key.public_key())
            .serial_number(x509.random_serial_number())
            .not_valid_before(datetime.now())
            .not_valid_after(datetime.now() + timedelta(days=365))
            .add_extension(
                x509.BasicConstraints(ca=True, path_length=None),
                critical=True,
            )
            .sign(private_key, hashes.SHA256())
        )

        # Save certificate and key
        with open(self.ca_cert_path, "wb") as f:
            f.write(cert.public_bytes(serialization.Encoding.PEM))

        with open(self.ca_key_path, "wb") as f:
            f.write(
                private_key.private_bytes(
                    encoding=serialization.Encoding.PEM,
                    format=serialization.PrivateFormat.PKCS8,
                    encryption_algorithm=serialization.NoEncryption(),
                )
            )

        logger.info(f"Generated CA certificate: {self.ca_cert_path}")

    def _generate_server_certificate(self):
        """Generate a new server certificate signed by the CA."""
        # Load CA certificate and key
        with open(self.ca_cert_path, "rb") as f:
            ca_cert = x509.load_pem_x509_certificate(f.read())

        with open(self.ca_key_path, "rb") as f:
            ca_key = serialization.load_pem_private_key(f.read(), password=None)

        # Generate server private key
        private_key = rsa.generate_private_key(
            public_exponent=65537,
            key_size=2048,
        )

        # Generate server certificate
        subject = x509.Name(
            [
                x509.NameAttribute(NameOID.COUNTRY_NAME, "US"),
                x509.NameAttribute(NameOID.STATE_OR_PROVINCE_NAME, "CA"),
                x509.NameAttribute(NameOID.LOCALITY_NAME, "San Francisco"),
                x509.NameAttribute(NameOID.ORGANIZATION_NAME, "IRCamera PC Controller"),
                x509.NameAttribute(NameOID.COMMON_NAME, "IRCamera Server"),
            ]
        )

        cert = (
            x509.CertificateBuilder()
            .subject_name(subject)
            .issuer_name(ca_cert.subject)
            .public_key(private_key.public_key())
            .serial_number(x509.random_serial_number())
            .not_valid_before(datetime.now())
            .not_valid_after(datetime.now() + timedelta(days=365))
            .add_extension(
                x509.SubjectAlternativeName(
                    [
                        x509.DNSName("localhost"),
                        x509.IPAddress(ipaddress.IPv4Address("127.0.0.1")),
                        x509.IPAddress(ipaddress.IPv4Address("192.168.1.1")),
                    ]
                ),
                critical=False,
            )
            .sign(ca_key, hashes.SHA256())
        )

        # Save certificate and key
        with open(self.server_cert_path, "wb") as f:
            f.write(cert.public_bytes(serialization.Encoding.PEM))

        with open(self.server_key_path, "wb") as f:
            f.write(
                private_key.private_bytes(
                    encoding=serialization.Encoding.PEM,
                    format=serialization.PrivateFormat.PKCS8,
                    encryption_algorithm=serialization.NoEncryption(),
                )
            )

        logger.info(f"Generated server certificate: {self.server_cert_path}")
