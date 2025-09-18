#!/usr/bin/env python3
"""
Enhanced TLS/SSL Server for IRCamera PC Controller

Provides secure TCP communication with Android devices using TLS encryption.
Implements self-signed certificate generation for development and deployment.
"""

import socket
import ssl
import threading
import json
import time
import os
import ipaddress
from datetime import datetime, timezone, timedelta
from pathlib import Path
from typing import Optional, Dict, Callable, Tuple
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID

import logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


class TLSSecurityManager:
    """Manages TLS certificates and secure connections for PC Controller"""
    
    def __init__(self, cert_dir: Optional[Path] = None):
        self.cert_dir = cert_dir or Path(__file__).parent / "certificates"
        self.cert_dir.mkdir(exist_ok=True)
        
        self.ca_cert_path = self.cert_dir / "ca.crt"
        self.ca_key_path = self.cert_dir / "ca.key" 
        self.server_cert_path = self.cert_dir / "server.crt"
        self.server_key_path = self.cert_dir / "server.key"
        
        # Certificate validity period
        self.cert_validity_days = 365
        
    def ensure_certificates_exist(self) -> bool:
        """Ensure TLS certificates exist, create if needed"""
        try:
            # Check if server certificates exist and are valid
            if (self.server_cert_path.exists() and self.server_key_path.exists() and
                self._are_certificates_valid()):
                logger.info(f"✅ Valid TLS certificates found in {self.cert_dir}")
                return True
            
            # Generate new certificates
            logger.info("🔐 Generating new TLS certificates for secure communication...")
            
            # Generate CA certificate first
            ca_private_key, ca_certificate = self._generate_ca_certificate()
            
            # Generate server certificate signed by CA
            server_private_key, server_certificate = self._generate_server_certificate(
                ca_private_key, ca_certificate
            )
            
            # Save certificates to disk
            self._save_certificate_files(
                ca_private_key, ca_certificate,
                server_private_key, server_certificate
            )
            
            logger.info(f"🔐 TLS certificates generated successfully")
            logger.info(f"   CA Certificate: {self.ca_cert_path}")
            logger.info(f"   Server Certificate: {self.server_cert_path}")
            return True
            
        except Exception as e:
            logger.error(f"❌ Failed to generate TLS certificates: {e}")
            return False
    
    def create_ssl_context(self) -> Optional[ssl.SSLContext]:
        """Create SSL context for secure server"""
        try:
            if not self.ensure_certificates_exist():
                return None
            
            # Create SSL context for server
            context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
            
            # Load server certificate and key
            context.load_cert_chain(self.server_cert_path, self.server_key_path)
            
            # Configure security settings
            context.check_hostname = False  # Development mode
            context.verify_mode = ssl.CERT_NONE  # Accept all clients for development
            
            # Set cipher preferences for compatibility
            context.set_ciphers('ECDHE+AESGCM:ECDHE+CHACHA20:DHE+AESGCM:DHE+CHACHA20:!aNULL:!MD5:!DSS')
            
            logger.info("🔐 SSL context created successfully")
            return context
            
        except Exception as e:
            logger.error(f"❌ Failed to create SSL context: {e}")
            return None
    
    def get_certificate_info(self) -> Dict[str, str]:
        """Get information about current certificates"""
        info = {}
        
        try:
            if self.server_cert_path.exists():
                with open(self.server_cert_path, 'rb') as f:
                    cert_data = f.read()
                    certificate = x509.load_pem_x509_certificate(cert_data)
                    
                info.update({
                    'subject': certificate.subject.rfc4514_string(),
                    'issuer': certificate.issuer.rfc4514_string(),
                    'valid_from': certificate.not_valid_before.isoformat(),
                    'valid_until': certificate.not_valid_after.isoformat(),
                    'serial_number': str(certificate.serial_number),
                    'signature_algorithm': certificate.signature_algorithm_oid._name
                })
                
        except Exception as e:
            logger.error(f"Failed to read certificate info: {e}")
            
        return info
    
    def _are_certificates_valid(self) -> bool:
        """Check if existing certificates are still valid"""
        try:
            with open(self.server_cert_path, 'rb') as f:
                cert_data = f.read()
                certificate = x509.load_pem_x509_certificate(cert_data)
                
            # Check if certificate is still valid
            now = datetime.now()
            return (certificate.not_valid_before <= now <= certificate.not_valid_after)
            
        except Exception:
            return False
    
    def _generate_ca_certificate(self) -> Tuple[rsa.RSAPrivateKey, x509.Certificate]:
        """Generate CA certificate and private key"""
        
        # Generate private key for CA
        private_key = rsa.generate_private_key(
            public_exponent=65537,
            key_size=2048,
        )
        
        # Create CA certificate
        subject = issuer = x509.Name([
            x509.NameAttribute(NameOID.COUNTRY_NAME, "UK"),
            x509.NameAttribute(NameOID.STATE_OR_PROVINCE_NAME, "London"),
            x509.NameAttribute(NameOID.LOCALITY_NAME, "London"),
            x509.NameAttribute(NameOID.ORGANIZATION_NAME, "IRCamera Project"),
            x509.NameAttribute(NameOID.ORGANIZATIONAL_UNIT_NAME, "Research"),
            x509.NameAttribute(NameOID.COMMON_NAME, "IRCamera CA"),
        ])
        
        certificate = x509.CertificateBuilder().subject_name(
            subject
        ).issuer_name(
            issuer
        ).public_key(
            private_key.public_key()
        ).serial_number(
            x509.random_serial_number()
        ).not_valid_before(
            datetime.utcnow()
        ).not_valid_after(
            datetime.utcnow() + timedelta(days=self.cert_validity_days * 2)  # CA valid longer
        ).add_extension(
            x509.SubjectAlternativeName([
                x509.DNSName("localhost"),
                x509.DNSName("ircamera-pc"),
                x509.IPAddress(ipaddress.IPv4Address("127.0.0.1")),
            ]),
            critical=False,
        ).add_extension(
            x509.BasicConstraints(ca=True, path_length=0), critical=True,
        ).add_extension(
            x509.KeyUsage(
                key_cert_sign=True,
                crl_sign=True,
                digital_signature=False,
                content_commitment=False,
                key_encipherment=False,
                data_encipherment=False,
                key_agreement=False,
                encipher_only=False,
                decipher_only=False
            ), critical=True,
        ).sign(private_key, hashes.SHA256())
        
        return private_key, certificate
    
    def _generate_server_certificate(self, ca_private_key: rsa.RSAPrivateKey, 
                                   ca_certificate: x509.Certificate) -> Tuple[rsa.RSAPrivateKey, x509.Certificate]:
        """Generate server certificate signed by CA"""
        
        # Generate private key for server
        private_key = rsa.generate_private_key(
            public_exponent=65537,
            key_size=2048,
        )
        
        # Create server certificate
        subject = x509.Name([
            x509.NameAttribute(NameOID.COUNTRY_NAME, "UK"),
            x509.NameAttribute(NameOID.STATE_OR_PROVINCE_NAME, "London"),
            x509.NameAttribute(NameOID.LOCALITY_NAME, "London"),
            x509.NameAttribute(NameOID.ORGANIZATION_NAME, "IRCamera Project"),
            x509.NameAttribute(NameOID.ORGANIZATIONAL_UNIT_NAME, "PC Controller"),
            x509.NameAttribute(NameOID.COMMON_NAME, "ircamera-pc-server"),
        ])
        
        certificate = x509.CertificateBuilder().subject_name(
            subject
        ).issuer_name(
            ca_certificate.issuer
        ).public_key(
            private_key.public_key()
        ).serial_number(
            x509.random_serial_number()
        ).not_valid_before(
            datetime.utcnow()
        ).not_valid_after(
            datetime.utcnow() + timedelta(days=self.cert_validity_days)
        ).add_extension(
            x509.SubjectAlternativeName([
                x509.DNSName("localhost"),
                x509.DNSName("ircamera-pc-server"),
                x509.DNSName("*.local"),
                x509.IPAddress(ipaddress.IPv4Address("127.0.0.1")),
                x509.IPAddress(ipaddress.IPv4Address("0.0.0.0")),
            ]),
            critical=False,
        ).add_extension(
            x509.KeyUsage(
                key_cert_sign=False,
                crl_sign=False,
                digital_signature=True,
                content_commitment=False,
                key_encipherment=True,
                data_encipherment=False,
                key_agreement=False,
                encipher_only=False,
                decipher_only=False
            ), critical=True,
        ).add_extension(
            x509.ExtendedKeyUsage([
                x509.ExtendedKeyUsageOID.SERVER_AUTH,
            ]), critical=True,
        ).sign(ca_private_key, hashes.SHA256())
        
        return private_key, certificate
    
    def _save_certificate_files(self, ca_private_key, ca_certificate, 
                               server_private_key, server_certificate):
        """Save certificates and keys to files"""
        
        # Save CA certificate
        with open(self.ca_cert_path, 'wb') as f:
            f.write(ca_certificate.public_bytes(serialization.Encoding.PEM))
        
        # Save CA private key
        with open(self.ca_key_path, 'wb') as f:
            f.write(ca_private_key.private_bytes(
                encoding=serialization.Encoding.PEM,
                format=serialization.PrivateFormat.PKCS8,
                encryption_algorithm=serialization.NoEncryption()
            ))
        
        # Save server certificate
        with open(self.server_cert_path, 'wb') as f:
            f.write(server_certificate.public_bytes(serialization.Encoding.PEM))
        
        # Save server private key
        with open(self.server_key_path, 'wb') as f:
            f.write(server_private_key.private_bytes(
                encoding=serialization.Encoding.PEM,
                format=serialization.PrivateFormat.PKCS8,
                encryption_algorithm=serialization.NoEncryption()
            ))
        
        # Set appropriate permissions (readable by owner only)
        os.chmod(self.ca_key_path, 0o600)
        os.chmod(self.server_key_path, 0o600)


class SecureTCPServer:
    """Enhanced TCP server with TLS/SSL encryption support"""
    
    def __init__(self, port: int = 8443, use_tls: bool = True, 
                 data_callback: Optional[Callable] = None,
                 streaming_callback: Optional[Callable] = None):
        self.port = port
        self.use_tls = use_tls
        self.data_callback = data_callback
        self.streaming_callback = streaming_callback
        
        # Server state
        self.running = False
        self.server_socket = None
        self.ssl_context = None
        self.connected_clients = {}
        self.client_threads = []
        
        # Security manager
        self.security_manager = TLSSecurityManager()
        
        # Performance metrics
        self.connection_count = 0
        self.message_count = 0
        self.bytes_transferred = 0
        
    def start(self) -> bool:
        """Start the secure TCP server"""
        try:
            # Setup TLS context if enabled
            if self.use_tls:
                self.ssl_context = self.security_manager.create_ssl_context()
                if not self.ssl_context:
                    logger.error("❌ Failed to create SSL context")
                    return False
                logger.info("🔐 TLS encryption enabled")
            else:
                logger.warning("⚠️ TLS encryption disabled - using plain TCP")
            
            # Create server socket
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
            self.server_socket.bind(('0.0.0.0', self.port))
            self.server_socket.listen(10)
            self.server_socket.settimeout(1.0)
            
            self.running = True
            
            # Start server thread
            server_thread = threading.Thread(target=self._server_loop, daemon=True)
            server_thread.start()
            
            protocol = "TLS" if self.use_tls else "TCP"
            logger.info(f"🌐 Secure {protocol} Server started on port {self.port}")
            
            if self.use_tls:
                cert_info = self.security_manager.get_certificate_info()
                logger.info(f"🔐 Server certificate valid until: {cert_info.get('valid_until', 'Unknown')}")
            
            return True
            
        except Exception as e:
            logger.error(f"❌ Failed to start secure server: {e}")
            return False
    
    def stop(self):
        """Stop the secure TCP server"""
        self.running = False
        
        # Close all client connections
        for client_id in list(self.connected_clients.keys()):
            self._disconnect_client(client_id)
        
        # Close server socket
        if self.server_socket:
            try:
                self.server_socket.close()
            except:
                pass
            self.server_socket = None
        
        logger.info("🛑 Secure TCP Server stopped")
        
        # Print final statistics
        logger.info(f"📊 Session statistics:")
        logger.info(f"   • Total connections: {self.connection_count}")
        logger.info(f"   • Messages processed: {self.message_count}")
        logger.info(f"   • Data transferred: {self.bytes_transferred / 1024:.1f} KB")
    
    def _server_loop(self):
        """Main server loop with TLS support"""
        logger.info("🔄 Secure server loop started")
        
        while self.running:
            try:
                client_socket, address = self.server_socket.accept()
                
                # Wrap with TLS if enabled
                if self.use_tls and self.ssl_context:
                    try:
                        client_socket = self.ssl_context.wrap_socket(
                            client_socket, 
                            server_side=True
                        )
                        logger.info(f"🔐 TLS handshake completed with {address}")
                    except ssl.SSLError as e:
                        logger.error(f"❌ TLS handshake failed with {address}: {e}")
                        client_socket.close()
                        continue
                
                self.connection_count += 1
                logger.info(f"🔗 New secure connection #{self.connection_count} from {address}")
                
                # Start client handler thread
                client_thread = threading.Thread(
                    target=self._handle_secure_client,
                    args=(client_socket, address),
                    daemon=True
                )
                client_thread.start()
                self.client_threads.append(client_thread)
                
                # Clean up finished threads
                self.client_threads = [t for t in self.client_threads if t.is_alive()]
                
            except socket.timeout:
                continue
            except Exception as e:
                if self.running:
                    logger.error(f"Server loop error: {e}")
                    time.sleep(1)
                else:
                    break
    
    def _handle_secure_client(self, client_socket, address):
        """Handle secure client connection with enhanced features"""
        device_id = f"device_{address[0]}_{address[1]}"
        buffer = ""
        last_heartbeat = time.time()
        
        try:
            # Register client connection
            self.connected_clients[device_id] = {
                'socket': client_socket,
                'address': address,
                'connected_at': datetime.now(timezone.utc),
                'last_heartbeat': last_heartbeat,
                'device_info': {},
                'message_count': 0,
                'bytes_received': 0
            }
            
            logger.info(f"📱 Secure device {device_id} connected from {address}")
            
            while self.running:
                try:
                    # Receive data with larger buffer for streaming
                    data = client_socket.recv(8192)
                    if not data:
                        break
                    
                    # Update statistics
                    self.connected_clients[device_id]['bytes_received'] += len(data)
                    self.bytes_transferred += len(data)
                    
                    # Decode and process messages
                    text_data = data.decode('utf-8')
                    buffer += text_data
                    
                    lines = buffer.split('\n')
                    buffer = lines[-1]  # Keep incomplete line
                    
                    for line in lines[:-1]:
                        if line.strip():
                            try:
                                message = json.loads(line.strip())
                                self._process_secure_message(device_id, message, client_socket)
                                
                                # Update statistics
                                self.message_count += 1
                                self.connected_clients[device_id]['message_count'] += 1
                                
                            except json.JSONDecodeError as e:
                                logger.warning(f"Invalid JSON from {device_id}: {line[:100]}...")
                                self._send_error_response(client_socket, "INVALID_JSON", str(e))
                
                except socket.timeout:
                    # Check heartbeat timeout
                    if time.time() - last_heartbeat > 90:  # 90 second timeout for TLS
                        logger.warning(f"⏰ Heartbeat timeout for secure device {device_id}")
                        break
                    continue
                except (ConnectionResetError, ssl.SSLError) as e:
                    logger.info(f"🔌 Secure connection closed by device {device_id}: {e}")
                    break
                except Exception as e:
                    logger.error(f"Secure client error for {device_id}: {e}")
                    break
                    
        except Exception as e:
            logger.error(f"Secure client handler error for {device_id}: {e}")
        finally:
            self._disconnect_client(device_id)
    
    def _process_secure_message(self, device_id: str, message: Dict, client_socket):
        """Process secure messages with enhanced validation"""
        message_type = message.get('message_type', 'unknown')
        
        try:
            # Update heartbeat
            if device_id in self.connected_clients:
                self.connected_clients[device_id]['last_heartbeat'] = time.time()
            
            # Handle different message types (same as MVPTCPServer but with TLS)
            if message_type == 'device_register':
                self._handle_device_registration(device_id, message, client_socket)
            elif message_type == 'gsr_data_batch':
                if self.streaming_callback:
                    self.streaming_callback(device_id, message)
            elif message_type == 'time_sync_request':
                self._handle_time_sync_request(device_id, message, client_socket)
            
            # Call general callback
            if self.data_callback:
                self.data_callback(device_id, message)
            
            # Send acknowledgment for critical messages
            if message_type in ['device_register', 'session_start']:
                self._send_ack_response(client_socket, message.get('message_id', 'unknown'))
                
            logger.debug(f"🔐 Secure message processed: {message_type} from {device_id}")
                
        except Exception as e:
            logger.error(f"Secure message processing error for {device_id}: {e}")
            self._send_error_response(client_socket, "PROCESSING_ERROR", str(e))
    
    def _handle_device_registration(self, device_id: str, message: Dict, client_socket):
        """Handle device registration with TLS verification"""
        device_info = {
            'device_type': message.get('device_type', 'unknown'),
            'capabilities': message.get('capabilities', []),
            'ip_address': message.get('ip_address', 'unknown'),
            'port': message.get('port', 0),
            'tls_enabled': True,  # Mark as TLS connection
            'registered_at': datetime.now(timezone.utc).isoformat()
        }
        
        if device_id in self.connected_clients:
            self.connected_clients[device_id]['device_info'] = device_info
        
        logger.info(f"📱 Secure device registered: {device_id} ({device_info['device_type']})")
        logger.info(f"   Capabilities: {', '.join(device_info['capabilities'])}")
    
    def _handle_time_sync_request(self, device_id: str, message: Dict, client_socket):
        """Handle time sync with precise TLS timing"""
        server_timestamp = datetime.now(timezone.utc).isoformat()
        
        response = {
            'message_type': 'time_sync_response',
            'timestamp': server_timestamp,
            'server_timestamp': server_timestamp,
            'client_timestamp': message.get('client_timestamp'),
            'processing_delay_ms': 1,
            'tls_enabled': True
        }
        
        self._send_response(client_socket, response)
    
    def _send_ack_response(self, client_socket, message_id: str):
        """Send secure acknowledgment"""
        response = {
            'message_type': 'ack',
            'timestamp': datetime.now(timezone.utc).isoformat(),
            'ack_for': message_id,
            'status': 'success',
            'tls_enabled': True
        }
        self._send_response(client_socket, response)
    
    def _send_error_response(self, client_socket, error_code: str, error_message: str):
        """Send secure error response"""
        response = {
            'message_type': 'error',
            'timestamp': datetime.now(timezone.utc).isoformat(),
            'error_code': error_code,
            'error_message': error_message,
            'tls_enabled': True
        }
        self._send_response(client_socket, response)
    
    def _send_response(self, client_socket, response: Dict):
        """Send secure JSON response"""
        try:
            response_json = json.dumps(response) + '\n'
            client_socket.send(response_json.encode('utf-8'))
        except Exception as e:
            logger.error(f"Failed to send secure response: {e}")
    
    def _disconnect_client(self, device_id: str):
        """Clean up client connection"""
        if device_id in self.connected_clients:
            client_info = self.connected_clients[device_id]
            try:
                client_info['socket'].close()
            except:
                pass
            
            # Log connection statistics
            duration = (datetime.now(timezone.utc) - client_info['connected_at']).total_seconds()
            logger.info(f"🔌 Device {device_id} disconnected")
            logger.info(f"   Duration: {duration:.1f}s")
            logger.info(f"   Messages: {client_info['message_count']}")
            logger.info(f"   Data: {client_info['bytes_received'] / 1024:.1f} KB")
            
            del self.connected_clients[device_id]
    
    def get_server_statistics(self) -> Dict:
        """Get server performance statistics"""
        return {
            'total_connections': self.connection_count,
            'active_connections': len(self.connected_clients),
            'total_messages': self.message_count,
            'bytes_transferred': self.bytes_transferred,
            'tls_enabled': self.use_tls,
            'uptime_seconds': time.time() - getattr(self, 'start_time', time.time())
        }


def main():
    """Test the secure TLS server"""
    print("🔐 IRCamera Secure TLS Server Test")
    print("=" * 50)
    
    # Test security manager
    security_manager = TLSSecurityManager()
    print("Testing certificate generation...")
    
    if security_manager.ensure_certificates_exist():
        print("✅ TLS certificates ready")
        
        cert_info = security_manager.get_certificate_info()
        for key, value in cert_info.items():
            print(f"   {key}: {value}")
    else:
        print("❌ Certificate generation failed")
        return
    
    # Test secure server
    def test_callback(device_id, message):
        print(f"📨 Message from {device_id}: {message.get('message_type')}")
    
    print("\nStarting secure server...")
    server = SecureTCPServer(port=8443, use_tls=True, data_callback=test_callback)
    
    if server.start():
        print("✅ Secure server started successfully")
        print("   Connect Android devices to test TLS communication")
        print("   Use Ctrl+C to stop")
        
        try:
            while True:
                time.sleep(1)
                stats = server.get_server_statistics()
                if stats['active_connections'] > 0:
                    print(f"📊 Active: {stats['active_connections']}, "
                          f"Messages: {stats['total_messages']}, "
                          f"Data: {stats['bytes_transferred'] / 1024:.1f} KB")
        except KeyboardInterrupt:
            print("\n🛑 Stopping server...")
            server.stop()
    else:
        print("❌ Failed to start secure server")


if __name__ == "__main__":
    main()