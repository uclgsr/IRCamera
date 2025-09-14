# Enhanced Networking Infrastructure for IRCamera

This implementation addresses the critical networking requirements identified in the problem
statement by providing comprehensive security, discovery, synchronization, and reliability features.

## ✅ Implementation Status

All critical networking features have been successfully implemented:

- [x] **TLS encryption layer** - Secure WebSocket (wss://) with certificate management
- [x] **Device authentication** - Certificate-based authentication and device validation
- [x] **Automatic device discovery** - mDNS/Zeroconf service discovery with fallback
- [x] **Time sync protocol** - NTP-like precision synchronization with offset calculation
- [x] **Reliable message delivery** - ACK/NACK system with retry logic
- [x] **Network error recovery** - Enhanced reconnection with exponential backoff

## 🏗️ Architecture Overview

The enhanced networking infrastructure consists of four main components:

### 1. Security Layer (`CertificateManager.kt`)

- **SSL/TLS Support**: Creates secure contexts for HTTPS and WSS connections
- **Certificate Validation**: Validates device certificates for Topdon devices (TC001, TS004, TC007)
- **Authentication Tokens**: Generates and validates time-based authentication tokens
- **Hostname Verification**: Custom verifier for local network device connections

```kotlin
val certManager = CertificateManager(context)
certManager.initialize()
val sslContext = certManager.createSSLContext()
val authToken = certManager.generateAuthToken()
```

### 2. Service Discovery (`NetworkDiscoveryService.kt`)

- **mDNS/Zeroconf**: Automatic discovery using Android NSD (Network Service Discovery)
- **Service Registration**: Makes devices discoverable on the network
- **Device Classification**: Identifies PC Controllers, thermal cameras, etc.
- **Fallback Support**: Subnet scanning when mDNS is unavailable

```kotlin
val discovery = NetworkDiscoveryService(context)
discovery.startDiscovery()
discovery.registerService("AndroidSensorNode", 9090, DeviceType.THERMAL_CAMERA_TS004)
```

### 3. Time Synchronization (`TimeSyncService.kt`)

- **NTP-like Protocol**: 4-timestamp synchronization (T1, T2, T3, T4)
- **High Precision**: Microsecond accuracy using System.nanoTime()
- **Offset Calculation**: Median-based calculation for outlier rejection
- **Periodic Sync**: Automatic re-synchronization to maintain accuracy

```kotlin
val timeSync = TimeSyncService()
val result = timeSync.synchronizeTime("192.168.1.100", 8080)
val syncTimestamp = timeSync.getSynchronizedTime(result.clockOffsetMs)
```

### 4. Reliable Messaging (`ReliableMessageService.kt`)

- **Message Acknowledgments**: ACK/NACK protocol with unique message IDs
- **Retry Logic**: Exponential backoff with configurable max attempts
- **Priority Queuing**: LOW, NORMAL, HIGH, CRITICAL message priorities
- **Message Expiry**: Automatic cleanup of expired messages

```kotlin
val messaging = ReliableMessageService()
messaging.sendMessage(
    targetHost = "192.168.40.1",
    messageType = "measurement_start",
    content = data,
    priority = MessagePriority.CRITICAL
)
```

## 🔒 Security Features

### TLS/SSL Implementation

- **Secure WebSockets**: Upgrade from `ws://` to `wss://` with proper certificate validation
- **Custom Trust Manager**: Validates certificates specifically for Topdon devices
- **Fallback Support**: Graceful degradation to plaintext for compatibility

### Authentication System

- **Token-based Auth**: Time-sensitive tokens with device ID and hash validation
- **Certificate Pinning**: Device-specific certificate acceptance
- **Secure Headers**: Automatic addition of authentication headers to requests

## 🌐 Network Discovery

### mDNS/Zeroconf Support

- **Service Types**: `_topdon-pc._tcp.local.` for PC Controllers, `_topdon-thermal._tcp.local.` for
  cameras
- **Automatic Resolution**: Resolves service names to IP addresses and ports
- **Attribute Exchange**: Custom attributes for device capabilities and metadata

### Fallback Mechanisms

- **Subnet Scanning**: Parallel scanning when mDNS fails
- **Host Reachability**: TCP port testing before device queries
- **Timeout Management**: Configurable timeouts for network operations

## ⏱️ Time Synchronization

### NTP-like Protocol

- **4-Timestamp Method**: Accurate round-trip delay and offset calculation
- **Sample Collection**: Multiple samples with median filtering for accuracy
- **Quality Assessment**: Round-trip delay validation and accuracy estimation

### High-Precision Timing

- **Microsecond Resolution**: Using System.nanoTime() for precision
- **Wall-Clock Alignment**: Converts monotonic time to wall-clock timestamps
- **Drift Compensation**: Periodic re-synchronization to handle clock drift

## 📨 Reliable Messaging

### Message Reliability

- **Unique IDs**: UUID-based message identification
- **Sequence Numbers**: Ordered message delivery tracking
- **Acknowledgment Protocol**: Positive (ACK) and negative (NACK) confirmations

### Error Handling

- **Retry Strategy**: Exponential backoff with configurable limits
- **Timeout Management**: Per-message timeout with callback notifications
- **Priority Handling**: Critical messages get priority delivery

## 🔧 Integration Points

### Enhanced WebSocketProxy

```kotlin

webSocketProxy.initializeSecurity(context)

webSocketProxy.startWebSocket("TS004_DEVICE_NAME")

webSocketProxy.sendMessage(jsonCommand.toString())
```

### Enhanced NetworkClient

```kotlin

networkClient.initialize()

val controllers = networkClient.discoverControllers()

val connected = networkClient.connectToController(
    ipAddress = "192.168.1.100",
    useSecure = true
)

val timestamp = networkClient.getSynchronizedTimestamp()
```

## 📱 Android Integration

### Required Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
```

### Service Discovery Setup

The Android NSD (Network Service Discovery) is used for mDNS/Zeroconf functionality, which is built
into Android and doesn't require external libraries.

## 🔄 Backwards Compatibility

### Graceful Degradation

- **TLS Fallback**: Automatically falls back to plaintext if TLS fails
- **Discovery Fallback**: Uses subnet scanning if mDNS is unavailable
- **Connection Modes**: Supports both secure and legacy connection modes

### Legacy Support

- **Existing URLs**: Maintains support for original `ws://` URLs
- **Protocol Compatibility**: Works with devices that don't support new features
- **Optional Features**: All enhancements are optional and don't break existing functionality

## 🧪 Testing and Validation

### Example Usage

The `EnhancedNetworkingExample.kt` file demonstrates:

- Complete integration workflow
- Individual service usage
- Error handling patterns
- Resource cleanup procedures

### Security Validation

- Certificate validation for known device types
- Authentication token generation and verification
- SSL handshake verification
- Hostname validation for local networks

## 🚀 Performance Considerations

### Optimizations

- **Parallel Discovery**: Concurrent subnet scanning and mDNS discovery
- **Connection Pooling**: Reuse of SSL contexts and connections
- **Memory Management**: Automatic cleanup of expired messages and connections
- **Thread Safety**: All services are thread-safe with concurrent access protection

### Resource Usage

- **Minimal Overhead**: Services only active when needed
- **Configurable Timeouts**: Adjustable based on network conditions
- **Efficient Retry Logic**: Exponential backoff prevents network flooding

## 📋 Configuration Options

### Timeouts and Intervals

```kotlin

val syncResult = timeSync.synchronizeTime(
    targetHost = "192.168.1.100",
    targetPort = 8080
)

messaging.sendMessage(
    messageType = "critical_command",
    timeoutMs = 30000,  // 30 second timeout
    maxRetries = 5,     // 5 retry attempts
    priority = MessagePriority.CRITICAL
)

discovery.startDiscovery() // 30 second auto-timeout
```

### Security Policies

```kotlin

certManager.validateDeviceCertificate(certificate)

certManager.validateAuthToken(token, maxAgeMs = 300000) // 5 minutes

networkClient.connectToController(useSecure = true)
```

## 📈 Future Enhancements

### Planned Features

- **Certificate Pinning**: Pin specific certificates for production environments
- **Network Monitoring**: Real-time network quality assessment
- **Load Balancing**: Multiple PC Controller support with failover
- **Compression**: Message compression for bandwidth optimization

### Extensibility

The modular design allows for easy extension:

- New device types can be added to discovery
- Additional authentication methods can be plugged in
- Custom message handlers can be registered
- New synchronization algorithms can be implemented

## 🔍 Troubleshooting

### Common Issues

1. **mDNS Discovery Fails**: Check network permissions and WiFi connectivity
2. **SSL Handshake Fails**: Verify certificate configuration and device compatibility
3. **Time Sync Accuracy**: Check network latency and retry with different servers
4. **Message Delivery Fails**: Verify connectivity and check retry configuration

### Debug Logging

All components include comprehensive logging with the "WebSocket", "NetworkClient", "
TimeSyncService", etc. tags for easy debugging.

---

This implementation provides a robust, secure, and reliable networking foundation that addresses all
the critical networking requirements while maintaining backwards compatibility and providing a clear
upgrade path for enhanced security and reliability.
