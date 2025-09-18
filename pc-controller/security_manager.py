#!/usr/bin/env python3
"""
Security Configuration for PC Controller
Implements TLS certificate generation and basic authentication
"""

import ssl
import os
import subprocess
from pathlib import Path
from typing import Optional

try:
    from loguru import logger
except ImportError:
    import logging
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger(__name__)


class SecurityManager:
    """Manages security features for PC Controller"""
    
    def __init__(self, cert_dir: Optional[Path] = None):
        self.cert_dir = cert_dir or Path(__file__).parent / "certificates"
        self.cert_dir.mkdir(exist_ok=True)
        
        self.cert_file = self.cert_dir / "server.crt"
        self.key_file = self.cert_dir / "server.key"
        
    def generate_self_signed_certificate(self, 
                                      common_name: str = "localhost",
                                      days: int = 365) -> bool:
        """Generate self-signed certificate for development"""
        try:
            # OpenSSL command to generate self-signed certificate
            cmd = [
                "openssl", "req", "-x509", "-newkey", "rsa:4096",
                "-keyout", str(self.key_file),
                "-out", str(self.cert_file),
                "-days", str(days), "-nodes",
                "-subj", f"/C=US/ST=CA/L=SF/O=IRCamera/CN={common_name}"
            ]
            
            result = subprocess.run(cmd, capture_output=True, text=True)
            
            if result.returncode == 0:
                logger.info(f"✅ Self-signed certificate generated: {self.cert_file}")
                return True
            else:
                logger.error(f"❌ Certificate generation failed: {result.stderr}")
                return False
                
        except FileNotFoundError:
            logger.error("❌ OpenSSL not found. Please install OpenSSL.")
            return False
        except Exception as e:
            logger.error(f"❌ Error generating certificate: {e}")
            return False
    
    def create_ssl_context(self) -> Optional[ssl.SSLContext]:
        """Create SSL context for TLS connections"""
        try:
            # Check if certificates exist
            if not (self.cert_file.exists() and self.key_file.exists()):
                logger.info("📜 Certificates not found, generating new ones...")
                if not self.generate_self_signed_certificate():
                    return None
            
            # Create SSL context
            context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
            context.load_cert_chain(self.cert_file, self.key_file)
            
            # Security settings
            context.minimum_version = ssl.TLSVersion.TLSv1_2
            context.set_ciphers('HIGH:!aNULL:!eNULL:!EXPORT:!DES:!RC4:!MD5:!PSK:!SRP:!CAMELLIA')
            
            logger.info("✅ SSL context created successfully")
            return context
            
        except Exception as e:
            logger.error(f"❌ Error creating SSL context: {e}")
            return None
    
    def validate_client_credentials(self, username: str, password: str) -> bool:
        """Basic authentication validation"""
        # For development - use environment variables or config file in production
        valid_users = {
            "admin": "admin123",
            "researcher": "research2025",
            "demo": "demo123"
        }
        
        return valid_users.get(username) == password
    
    def get_certificate_info(self) -> dict:
        """Get certificate information"""
        info = {
            "cert_file_exists": self.cert_file.exists(),
            "key_file_exists": self.key_file.exists(),
            "cert_path": str(self.cert_file),
            "key_path": str(self.key_file)
        }
        
        if self.cert_file.exists():
            try:
                # Get certificate details using OpenSSL
                cmd = ["openssl", "x509", "-in", str(self.cert_file), "-text", "-noout"]
                result = subprocess.run(cmd, capture_output=True, text=True)
                
                if result.returncode == 0:
                    # Parse relevant info
                    output = result.stdout
                    if "Subject:" in output:
                        subject_line = [line for line in output.split('\n') if 'Subject:' in line][0]
                        info["subject"] = subject_line.strip()
                    
                    if "Not After" in output:
                        expiry_line = [line for line in output.split('\n') if 'Not After' in line][0]
                        info["expires"] = expiry_line.strip()
                        
            except Exception as e:
                logger.warning(f"Could not read certificate info: {e}")
        
        return info


def demo_security_features():
    """Demonstrate security features"""
    print("🔐 PC Controller Security Features Demo")
    print("=" * 50)
    
    # Initialize security manager
    security_manager = SecurityManager()
    
    # Show certificate status
    cert_info = security_manager.get_certificate_info()
    print(f"📜 Certificate exists: {cert_info['cert_file_exists']}")
    print(f"🔑 Private key exists: {cert_info['key_file_exists']}")
    print(f"📁 Certificate directory: {security_manager.cert_dir}")
    
    # Generate certificate if needed
    if not cert_info['cert_file_exists']:
        print("\n🔧 Generating self-signed certificate...")
        if security_manager.generate_self_signed_certificate():
            print("✅ Certificate generated successfully")
            cert_info = security_manager.get_certificate_info()
        else:
            print("❌ Certificate generation failed")
            return
    
    # Show certificate details
    print(f"\n📜 Certificate Details:")
    if 'subject' in cert_info:
        print(f"   Subject: {cert_info['subject']}")
    if 'expires' in cert_info:
        print(f"   {cert_info['expires']}")
    
    # Test SSL context creation
    print("\n🔐 Testing SSL context creation...")
    ssl_context = security_manager.create_ssl_context()
    if ssl_context:
        print("✅ SSL context created successfully")
        print(f"   Protocol: {ssl_context.protocol}")
        print(f"   Minimum version: {ssl_context.minimum_version}")
    else:
        print("❌ SSL context creation failed")
    
    # Test authentication
    print("\n🔒 Testing authentication...")
    test_users = [
        ("admin", "admin123", True),
        ("researcher", "research2025", True),
        ("demo", "demo123", True),
        ("admin", "wrongpassword", False),
        ("unknown", "password", False)
    ]
    
    for username, password, expected in test_users:
        result = security_manager.validate_client_credentials(username, password)
        status = "✅" if result == expected else "❌"
        print(f"   {status} {username}:{password} -> {'Valid' if result else 'Invalid'}")
    
    print("\n🎉 Security features demo completed!")
    print("\nTo use with enhanced TCP server:")
    print("   server = EnhancedTCPServer(port=8443, use_tls=True)")
    print("   server.start()")


if __name__ == "__main__":
    demo_security_features()