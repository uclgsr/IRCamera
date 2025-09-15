# Enterprise Security Guidelines & Implementation

## 🛡️ Enterprise Security Overview

The IRCamera platform implements **military-grade comprehensive security measures** to protect
thermal imaging data, physiological measurements, system communications, cloud integrations, and
enterprise deployments. This document outlines enterprise security architecture, implementation
guidelines, compliance frameworks, threat modeling, and production security best practices.

## 🏗️ Enterprise Multi-Layer Security Architecture

### 🔒 Advanced Enterprise Security Model

```mermaid
graph TB
    subgraph "🛡️ Application Security Layer"
        Auth[Multi-Factor Authentication<br/>SSO + LDAP + OAuth2]
        Authz[Role-Based Authorization<br/>RBAC + ABAC + JWT]
        Input[Advanced Input Validation<br/>XSS + SQL Injection Protection]
        Output[Secure Output Sanitization<br/>Data Leakage Prevention]
        Session[Session Management<br/>Secure Token Handling]
    end
    
    subgraph "🔐 Data Security Layer"
        Encrypt[AES-256-GCM Encryption<br/>Hardware Security Module]
        Hash[SHA-256 Data Integrity<br/>Digital Signatures]
        Backup[Enterprise Secure Backup<br/>Geo-Redundant Storage]
        Anonymize[HIPAA-Compliant Anonymization<br/>Differential Privacy]
        KeyMgmt[Enterprise Key Management<br/>Azure Key Vault / AWS KMS]
    end
    
    subgraph "🌐 Communication Security Layer"
        TLS[TLS 1.3 Encryption<br/>Perfect Forward Secrecy]
        Cert[Enterprise Certificate Management<br/>PKI Infrastructure]
        VPN[Enterprise VPN Support<br/>Zero Trust Architecture]
        API[API Security<br/>Rate Limiting + WAF]
        Firewall[Advanced Firewall<br/>DPI + IDS/IPS]
    end
    
    subgraph "☁️ Cloud Security Layer"
        CloudAuth[Cloud IAM Integration<br/>AWS/Azure/GCP Security]
        CloudEncrypt[Cloud-Native Encryption<br/>Customer-Managed Keys]
        CloudMonitor[Security Monitoring<br/>SIEM + SOC Integration]
        Compliance[Compliance Framework<br/>SOC2 + HIPAA + GDPR]
    end
    
    subgraph "🤖 AI/ML Security Layer"
        ModelSec[Model Security<br/>Adversarial Protection]
        DataPrivacy[Privacy-Preserving ML<br/>Federated Learning]
        ModelValidation[Model Validation<br/>Security Testing]
        AIGovernance[AI Governance<br/>Ethical AI Framework]
    end
    
    subgraph "🔍 Monitoring & Incident Response"
        Monitor[Real-Time Monitoring<br/>24/7 SOC]
        Audit[Comprehensive Audit Logging<br/>Immutable Logs]
        Incident[Incident Response<br/>Automated Playbooks]
        Forensics[Digital Forensics<br/>Evidence Collection]
    end
        Firewall[Network Filtering]
    end
    
    subgraph "Device Security Layer"
        Keystore[Hardware Keystore]
        Biometric[Biometric Authentication]
        DeviceAdmin[Device Administration]
        Sandbox[Application Sandboxing]
    end
    
    Auth --> Encrypt
    Authz --> Hash
    Input --> TLS
    Output --> Cert
    Encrypt --> Keystore
    Hash --> Biometric
    TLS --> DeviceAdmin
    Cert --> Sandbox
```

### Security Threat Model

| Threat Category         | Risk Level | Mitigation Strategy                     |
|-------------------------|------------|-----------------------------------------|
| **Data Interception**   | High       | End-to-end encryption, TLS 1.3          |
| **Unauthorized Access** | High       | Multi-factor authentication, biometrics |
| **Data Tampering**      | Medium     | Digital signatures, integrity checks    |
| **Device Compromise**   | Medium     | Hardware security modules, sandboxing   |
| **Network Attacks**     | Medium     | VPN tunneling, certificate pinning      |
| **Data Leakage**        | High       | Data classification, access controls    |

## 🔐 Authentication & Authorization

### Android Authentication Implementation

#### Biometric Authentication

```kotlin
class BiometricAuthManager(private val context: Context) {
    private val biometricPrompt by lazy {
        BiometricPrompt(context as FragmentActivity, 
                       ContextCompat.getMainExecutor(context), 
                       authenticationCallback)
    }
    
    private val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("IRCamera Secure Access")
        .setSubtitle("Authenticate to access thermal imaging data")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                                 BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build()
    
    fun authenticateUser(): Flow<AuthResult> = callbackFlow {
        biometricPrompt.authenticate(promptInfo)
        awaitClose { /* cleanup */ }
    }
    
    private val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)

            generateSecureSession()
        }
        
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)

            handleAuthenticationFailure(errorCode, errString.toString())
        }
    }
}
```

#### Role-Based Access Control

```kotlin
enum class UserRole(val permissions: Set<Permission>) {
    RESEARCHER(setOf(
        Permission.VIEW_DATA,
        Permission.RECORD_SESSION,
        Permission.EXPORT_DATA,
        Permission.CONFIGURE_SENSORS
    )),
    TECHNICIAN(setOf(
        Permission.VIEW_DATA,
        Permission.RECORD_SESSION,
        Permission.CALIBRATE_DEVICES
    )),
    VIEWER(setOf(
        Permission.VIEW_DATA
    ))
}

class AccessControlManager {
    fun checkPermission(user: User, permission: Permission): Boolean {
        return user.role.permissions.contains(permission)
    }
    
    fun enforcePermission(user: User, permission: Permission) {
        if (!checkPermission(user, permission)) {
            throw SecurityException("Insufficient permissions for ${permission.name}")
        }
    }
}
```

### PC Controller Authentication

#### Multi-Factor Authentication

```python
import pyotp
import qrcode
from cryptography.fernet import Fernet
from typing import Optional

class MFAuthenticator:
    def __init__(self):
        self.fernet = Fernet(Fernet.generate_key())
        self.failed_attempts = {}
        self.max_attempts = 3
        self.lockout_time = 300  # 5 minutes
    
    def setup_totp(self, username: str) -> tuple[str, str]:
        """Setup Time-based One-Time Password for user"""
        secret = pyotp.random_base32()
        totp_uri = pyotp.totp.TOTP(secret).provisioning_uri(
            name=username,
            issuer_name="IRCamera Platform"
        )
        
        # Generate QR code for easy setup
        qr = qrcode.QRCode(version=1, box_size=10, border=5)
        qr.add_data(totp_uri)
        qr.make(fit=True)
        
        return secret, totp_uri
    
    def verify_totp(self, username: str, token: str, secret: str) -> bool:
        """Verify TOTP token"""
        if self.is_locked_out(username):
            raise SecurityException("Account temporarily locked due to failed attempts")
        
        totp = pyotp.TOTP(secret)
        is_valid = totp.verify(token, valid_window=1)
        
        if not is_valid:
            self.record_failed_attempt(username)
        else:
            self.clear_failed_attempts(username)
        
        return is_valid
    
    def is_locked_out(self, username: str) -> bool:
        """Check if user is locked out due to failed attempts"""
        if username not in self.failed_attempts:
            return False
        
        attempts, last_attempt = self.failed_attempts[username]
        if attempts >= self.max_attempts:
            return time.time() - last_attempt < self.lockout_time
        
        return False
```

## 🔒 Data Encryption

### Android Data Encryption

#### Thermal Data Encryption

```kotlin
class ThermalDataEncryption {
    private val keyAlias = "IRCamera_Thermal_Key"
    private val transformation = "AES/GCM/NoPadding"
    
    private fun getOrCreateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
    
    suspend fun encryptThermalFrame(frameData: ByteArray): EncryptedFrame = withContext(Dispatchers.IO) {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(frameData)
        
        EncryptedFrame(
            data = encryptedData,
            iv = iv,
            timestamp = System.currentTimeMillis()
        )
    }
    
    suspend fun decryptThermalFrame(encryptedFrame: EncryptedFrame): ByteArray = withContext(Dispatchers.IO) {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(transformation)
        
        val spec = GCMParameterSpec(128, encryptedFrame.iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        
        cipher.doFinal(encryptedFrame.data)
    }
}
```

#### GSR Data Protection

```kotlin
class GSRDataProtection {
    private val keyAlias = "IRCamera_GSR_Key"
    
    fun encryptGSRData(gsrData: GSRSample): EncryptedGSRSample {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        
        val secretKey = keyStore.getKey(keyAlias, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val serializedData = Json.encodeToString(gsrData)
        val encryptedData = cipher.doFinal(serializedData.toByteArray())
        
        return EncryptedGSRSample(
            encryptedData = encryptedData,
            iv = cipher.iv,
            timestamp = gsrData.timestamp
        )
    }
    
    fun anonymizeGSRData(gsrData: GSRSample, participantId: String): GSRSample {

        val hashedId = MessageDigest.getInstance("SHA-256")
            .digest(participantId.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(8) // Use first 8 characters as anonymous ID
        
        return gsrData.copy(
            participantId = hashedId,
            deviceId = "anonymized",
            sessionMetadata = gsrData.sessionMetadata.copy(
                location = null,
                experimentalNotes = null
            )
        )
    }
}
```

### PC Controller Encryption

#### File System Encryption

```python
from cryptography.fernet import Fernet
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.kdf.pbkdf2 import PBKDF2HMAC
import os
import base64

class FileSystemEncryption:
    def __init__(self, password: str):
        self.password = password.encode()
        self.salt = os.urandom(16)
        self.key = self.derive_key()
        self.fernet = Fernet(self.key)
    
    def derive_key(self) -> bytes:
        """Derive encryption key from password using PBKDF2"""
        kdf = PBKDF2HMAC(
            algorithm=hashes.SHA256(),
            length=32,
            salt=self.salt,
            iterations=100000,
        )
        key = base64.urlsafe_b64encode(kdf.derive(self.password))
        return key
    
    def encrypt_file(self, file_path: str) -> str:
        """Encrypt a file and save with .encrypted extension"""
        with open(file_path, 'rb') as file:
            file_data = file.read()
        
        encrypted_data = self.fernet.encrypt(file_data)
        
        encrypted_path = f"{file_path}.encrypted"
        with open(encrypted_path, 'wb') as encrypted_file:
            encrypted_file.write(self.salt)  # Store salt with encrypted data
            encrypted_file.write(encrypted_data)
        
        # Securely delete original file
        self.secure_delete(file_path)
        
        return encrypted_path
    
    def decrypt_file(self, encrypted_path: str) -> str:
        """Decrypt a file and restore original"""
        with open(encrypted_path, 'rb') as encrypted_file:
            stored_salt = encrypted_file.read(16)  # Read salt
            encrypted_data = encrypted_file.read()
        
        # Verify salt matches
        if stored_salt != self.salt:
            # Re-derive key with stored salt
            self.salt = stored_salt
            self.key = self.derive_key()
            self.fernet = Fernet(self.key)
        
        decrypted_data = self.fernet.decrypt(encrypted_data)
        
        original_path = encrypted_path.replace('.encrypted', '')
        with open(original_path, 'wb') as decrypted_file:
            decrypted_file.write(decrypted_data)
        
        return original_path
    
    def secure_delete(self, file_path: str):
        """Securely delete file by overwriting with random data"""
        if not os.path.exists(file_path):
            return
        
        file_size = os.path.getsize(file_path)
        
        with open(file_path, 'r+b') as file:
            # Overwrite with random data multiple times
            for _ in range(3):
                file.seek(0)
                file.write(os.urandom(file_size))
                file.flush()
                os.fsync(file.fileno())
        
        os.remove(file_path)
```

## 🌐 Network Security

### Secure Communication Protocol

#### TLS Configuration

```python
import ssl
import socket
from typing import Dict, Any

class SecureNetworkManager:
    def __init__(self):
        self.ssl_context = self.create_ssl_context()
        self.certificate_pinning = True
    
    def create_ssl_context(self) -> ssl.SSLContext:
        """Create secure SSL context with strong settings"""
        context = ssl.create_default_context(ssl.Purpose.SERVER_AUTH)
        
        # Require TLS 1.3 minimum
        context.minimum_version = ssl.TLSVersion.TLSv1_3
        context.maximum_version = ssl.TLSVersion.TLSv1_3
        
        # Strong cipher suites
        context.set_ciphers('TLS_AES_256_GCM_SHA384:TLS_CHACHA20_POLY1305_SHA256')
        
        # Certificate verification
        context.check_hostname = True
        context.verify_mode = ssl.CERT_REQUIRED
        
        return context
    
    def establish_secure_connection(self, host: str, port: int) -> ssl.SSLSocket:
        """Establish secure connection with certificate pinning"""
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        
        # Wrap socket with SSL
        secure_sock = self.ssl_context.wrap_socket(
            sock, 
            server_hostname=host
        )
        
        try:
            secure_sock.connect((host, port))
            
            # Verify certificate pinning
            if self.certificate_pinning:
                self.verify_certificate_pin(secure_sock)
            
            return secure_sock
            
        except Exception as e:
            secure_sock.close()
            raise SecurityException(f"Failed to establish secure connection: {e}")
    
    def verify_certificate_pin(self, sock: ssl.SSLSocket):
        """Verify certificate pinning for additional security"""
        cert = sock.getpeercert(binary_form=True)
        cert_hash = hashlib.sha256(cert).hexdigest()
        
        # Check against known good certificate hashes
        known_hashes = self.load_certificate_pins()
        
        if cert_hash not in known_hashes:
            raise SecurityException("Certificate pinning verification failed")
```

#### Message Integrity

```python
import hmac
import hashlib
import json
from typing import Dict, Any

class MessageSecurity:
    def __init__(self, shared_secret: bytes):
        self.shared_secret = shared_secret
    
    def sign_message(self, message: Dict[str, Any]) -> Dict[str, Any]:
        """Sign message with HMAC for integrity verification"""
        message_json = json.dumps(message, sort_keys=True)
        message_bytes = message_json.encode('utf-8')
        
        signature = hmac.new(
            self.shared_secret,
            message_bytes,
            hashlib.sha256
        ).hexdigest()
        
        return {
            'message': message,
            'signature': signature,
            'timestamp': time.time()
        }
    
    def verify_message(self, signed_message: Dict[str, Any]) -> bool:
        """Verify message integrity using HMAC"""
        message = signed_message['message']
        received_signature = signed_message['signature']
        timestamp = signed_message['timestamp']
        
        # Check message age (prevent replay attacks)
        if time.time() - timestamp > 300:  # 5 minutes
            raise SecurityException("Message too old - possible replay attack")
        
        # Verify signature
        message_json = json.dumps(message, sort_keys=True)
        message_bytes = message_json.encode('utf-8')
        
        expected_signature = hmac.new(
            self.shared_secret,
            message_bytes,
            hashlib.sha256
        ).hexdigest()
        
        return hmac.compare_digest(received_signature, expected_signature)
```

## 🔍 Security Monitoring & Logging

### Security Event Monitoring

```python
import logging
from enum import Enum
from dataclasses import dataclass
from typing import Optional, Dict, Any
import json

class SecurityEventType(Enum):
    AUTHENTICATION_SUCCESS = "auth_success"
    AUTHENTICATION_FAILURE = "auth_failure"
    AUTHORIZATION_DENIED = "authz_denied"
    DATA_ACCESS = "data_access"
    DATA_EXPORT = "data_export"
    CONFIGURATION_CHANGE = "config_change"
    NETWORK_ANOMALY = "network_anomaly"
    ENCRYPTION_ERROR = "encryption_error"

@dataclass
class SecurityEvent:
    event_type: SecurityEventType
    user_id: Optional[str]
    source_ip: str
    timestamp: float
    details: Dict[str, Any]
    risk_level: str  # low, medium, high, critical

class SecurityMonitor:
    def __init__(self):
        self.logger = logging.getLogger('security_monitor')
        self.setup_security_logging()
        self.event_handlers = {
            SecurityEventType.AUTHENTICATION_FAILURE: self.handle_auth_failure,
            SecurityEventType.AUTHORIZATION_DENIED: self.handle_authz_denied,
            SecurityEventType.NETWORK_ANOMALY: self.handle_network_anomaly
        }
    
    def setup_security_logging(self):
        """Setup secure logging with proper formatting"""
        handler = logging.FileHandler('/var/log/ircamera/security.log')
        formatter = logging.Formatter(
            '%(asctime)s - %(levelname)s - %(name)s - %(message)s'
        )
        handler.setFormatter(formatter)
        handler.setLevel(logging.INFO)
        self.logger.addHandler(handler)
        self.logger.setLevel(logging.INFO)
    
    def log_security_event(self, event: SecurityEvent):
        """Log security event with structured format"""
        event_data = {
            'event_type': event.event_type.value,
            'user_id': event.user_id,
            'source_ip': event.source_ip,
            'timestamp': event.timestamp,
            'details': event.details,
            'risk_level': event.risk_level
        }
        
        log_message = json.dumps(event_data)
        
        if event.risk_level in ['high', 'critical']:
            self.logger.error(log_message)
        elif event.risk_level == 'medium':
            self.logger.warning(log_message)
        else:
            self.logger.info(log_message)
        
        # Handle specific event types
        if event.event_type in self.event_handlers:
            self.event_handlers[event.event_type](event)
    
    def handle_auth_failure(self, event: SecurityEvent):
        """Handle authentication failure events"""
        user_id = event.user_id or "unknown"
        source_ip = event.source_ip
        
        # Increment failure count for IP and user
        self.increment_failure_count(source_ip, user_id)
        
        # Check if should trigger lockout
        if self.should_trigger_lockout(source_ip, user_id):
            self.trigger_security_lockout(source_ip, user_id)
    
    def handle_network_anomaly(self, event: SecurityEvent):
        """Handle network anomaly detection"""
        if event.risk_level == 'critical':
            # Trigger immediate response
            self.trigger_incident_response(event)
```

## 🚨 Incident Response

### Automated Security Response

```python
class IncidentResponseSystem:
    def __init__(self):
        self.response_procedures = {
            'data_breach': self.handle_data_breach,
            'unauthorized_access': self.handle_unauthorized_access,
            'network_intrusion': self.handle_network_intrusion,
            'malware_detection': self.handle_malware_detection
        }
        self.notification_channels = []
    
    def trigger_incident_response(self, incident_type: str, details: Dict[str, Any]):
        """Trigger automated incident response"""
        if incident_type in self.response_procedures:
            self.response_procedures[incident_type](details)
        
        # Send notifications
        self.send_security_alerts(incident_type, details)
    
    def handle_data_breach(self, details: Dict[str, Any]):
        """Handle suspected data breach"""
        # Immediate actions
        actions = [
            self.revoke_all_sessions,
            self.enable_enhanced_monitoring,
            self.backup_security_logs,
            self.notify_security_team
        ]
        
        for action in actions:
            try:
                action(details)
            except Exception as e:
                logging.error(f"Failed to execute security action: {e}")
    
    def handle_unauthorized_access(self, details: Dict[str, Any]):
        """Handle unauthorized access attempts"""
        user_id = details.get('user_id')
        source_ip = details.get('source_ip')
        
        # Block source IP
        self.add_ip_to_blocklist(source_ip)
        
        # Disable user account if identified
        if user_id:
            self.disable_user_account(user_id)
        
        # Force re-authentication for all sessions
        self.force_reauthentication()
```

## 📋 Security Configuration Checklist

### Android Security Checklist

- [ ] Enable biometric authentication
- [ ] Implement certificate pinning
- [ ] Use Android Keystore for encryption keys
- [ ] Enable app sandboxing
- [ ] Implement proper permission handling
- [ ] Use encrypted shared preferences
- [ ] Enable code obfuscation (ProGuard/R8)
- [ ] Implement root detection
- [ ] Use secure network communications
- [ ] Implement proper session management

### PC Controller Security Checklist

- [ ] Enable multi-factor authentication
- [ ] Implement file system encryption
- [ ] Use secure password policies
- [ ] Enable comprehensive logging
- [ ] Implement intrusion detection
- [ ] Use VPN for remote access
- [ ] Enable automatic security updates
- [ ] Implement backup encryption
- [ ] Use secure communication protocols
- [ ] Enable network monitoring

### System-Wide Security Checklist

- [ ] Regular security assessments
- [ ] Penetration testing
- [ ] Security training for users
- [ ] Incident response procedures
- [ ] Data retention policies
- [ ] Privacy impact assessments
- [ ] Compliance with regulations (GDPR, HIPAA)
- [ ] Regular security updates
- [ ] Backup and recovery procedures
- [ ] Security monitoring and alerting

## 🔐 Security Best Practices

### Development Security Practices

1. **Secure Coding Standards**: Follow OWASP secure coding guidelines
2. **Code Review**: Mandatory security-focused code reviews
3. **Static Analysis**: Use automated security scanning tools
4. **Dependency Management**: Regular security updates for dependencies
5. **Secrets Management**: Never commit secrets to version control
6. **Testing**: Include security testing in CI/CD pipeline

### Deployment Security Practices

1. **Infrastructure Security**: Secure server configurations
2. **Network Security**: Proper firewall and network segmentation
3. **Access Controls**: Principle of least privilege
4. **Monitoring**: Continuous security monitoring
5. **Incident Response**: Well-defined incident response procedures
6. **Compliance**: Regular compliance audits and assessments

This comprehensive security guide ensures the IRCamera platform maintains the highest security
standards for protecting sensitive thermal imaging and physiological data across all components and
deployment scenarios.
