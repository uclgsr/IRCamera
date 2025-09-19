

import hashlib
import ipaddress
import secrets
import ssl
import time
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID
from datetime import datetime, timedelta
from pathlib import Path
from typing import Dict, Optional, Tuple, Any

try:
    from loguru import logger
except ImportError:
    try:
        from ..utils.simple_logger import logger
    except ImportError:

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

    class FallbackConfig:
        def get(self, key, default=None) -> Any:
            config_map = {
                "security.cert_directory": "certificates",
            }
            return config_map.get(key, default)

    config = FallbackConfig()


class SecurityManager:

    def __init__(self):

        self.cert_dir = Path(config.get("security.cert_directory", "certificates"))
        self.cert_dir.mkdir(exist_ok=True)

        self.ca_cert_path = self.cert_dir / "ca_cert.pem"
        self.ca_key_path = self.cert_dir / "ca_key.pem"
        self.server_cert_path = self.cert_dir / "server_cert.pem"
        self.server_key_path = self.cert_dir / "server_key.pem"

        self.device_certificates: Dict[str, x509.Certificate] = {}
        self.auth_tokens: Dict[str, Tuple[str, float]] = (
            {}
        )

    def initialize(self) -> bool:

        try:
            logger.info("Initializing security manager...")

            if not self._load_ca_certificate():
                logger.info("Generating new CA certificate...")
                self._generate_ca_certificate()

            if not self._load_server_certificate():
                logger.info("Generating new server certificate...")
                self._generate_server_certificate()

            logger.info("Security manager initialized successfully")
            return True

        except Exception as e:
            logger.error(f"Failed to initialize security manager: {e}")
            return False

    def create_ssl_context(self, for_client_auth: bool = True) -> ssl.SSLContext:

        context = ssl.create_default_context(ssl.Purpose.CLIENT_AUTH)

        context.load_cert_chain(str(self.server_cert_path), str(self.server_key_path))

        if for_client_auth:

            context.load_verify_locations(str(self.ca_cert_path))
            context.verify_mode = ssl.CERT_OPTIONAL
        else:
            context.verify_mode = ssl.CERT_NONE

        context.check_hostname = False
        context.minimum_version = ssl.TLSVersion.TLSv1_2

        return context

    def validate_device_certificate(
            self, cert_data: bytes
    ) -> Tuple[bool, Optional[str]]:

        try:
            certificate = x509.load_pem_x509_certificate(cert_data)

            subject = certificate.subject
            common_name = None
            organization = None

            for attribute in subject:
                if attribute.oid == NameOID.COMMON_NAME:
                    common_name = attribute.value
                elif attribute.oid == NameOID.ORGANIZATION_NAME:
                    organization = attribute.value

            if organization and "topdon" in organization.lower():
                if common_name:
                    if "tc001" in common_name.lower():
                        return True, "TC001"
                    elif "ts004" in common_name.lower():
                        return True, "TS004"
                    elif "tc007" in common_name.lower():
                        return True, "TC007"

            logger.warning(
                f"Unknown device certificate: {common_name} from {organization}"
            )
            return True, "UNKNOWN"

        except Exception as e:
            logger.error(f"Certificate validation failed: {e}")
            return False, None

    def generate_auth_token(self, device_id: str, duration_minutes: int = 5) -> str:

        timestamp = str(int(time.time()))
        nonce = secrets.token_hex(8)

        token_data = f"{device_id}:{timestamp}:{nonce}"
        token_hash = hashlib.sha256(token_data.encode()).hexdigest()[:16]

        token = f"{device_id}:{timestamp}:{nonce}:{token_hash}"

        expiry_time = time.time() + (duration_minutes * 60)
        self.auth_tokens[token] = (device_id, expiry_time)

        logger.debug(f"Generated auth token for device {device_id}: {token[:20]}...")
        return token

    def validate_auth_token(
            self, token: str, max_age_seconds: int = 300
    ) -> Tuple[bool, Optional[str]]:

        try:

            if token in self.auth_tokens:
                device_id, expiry_time = self.auth_tokens[token]
                if time.time() < expiry_time:
                    return True, device_id
                else:

                    del self.auth_tokens[token]
                    return False, None

            parts = token.split(":")
            if len(parts) != 4:
                return False, None

            device_id, timestamp, nonce, provided_hash = parts

            token_time = int(timestamp)
            if time.time() - token_time > max_age_seconds:
                return False, None

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

        try:
            if self.ca_cert_path.exists() and self.ca_key_path.exists():

                with open(self.ca_cert_path, "rb") as f:
                    x509.load_pem_x509_certificate(f.read())
                logger.debug("Loaded existing CA certificate")
                return True
        except Exception as e:
            logger.warning(f"Failed to load CA certificate: {e}")
        return False

    def _load_server_certificate(self) -> bool:

        try:
            if self.server_cert_path.exists() and self.server_key_path.exists():

                with open(self.server_cert_path, "rb") as f:
                    x509.load_pem_x509_certificate(f.read())
                logger.debug("Loaded existing server certificate")
                return True
        except Exception as e:
            logger.warning(f"Failed to load server certificate: {e}")
        return False

    def _generate_ca_certificate(self):

        private_key = rsa.generate_private_key(
            public_exponent=65537,
            key_size=2048,
        )

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

        with open(self.ca_cert_path, "rb") as f:
            ca_cert = x509.load_pem_x509_certificate(f.read())

        with open(self.ca_key_path, "rb") as f:
            ca_key = serialization.load_pem_private_key(f.read(), password=None)

        private_key = rsa.generate_private_key(
            public_exponent=65537,
            key_size=2048,
        )

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
