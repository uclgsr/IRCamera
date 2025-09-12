package com.topdon.lib.core.security

import android.content.Context
import android.util.Log
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*

/**
 * Manages TLS certificates for secure communication with PC controllers and thermal cameras.
 * Handles device authentication and certificate validation.
 */
class CertificateManager(private val context: Context) {
    companion object {
        private const val TAG = "CertificateManager"
        private const val TRUST_STORE_ALIAS = "topdon_devices"
        private const val KEY_STORE_TYPE = "PKCS12"
        private const val TLS_PROTOCOL = "TLS"
    }

    private var trustManager: X509TrustManager? = null
    private var keyManager: X509KeyManager? = null
    private var deviceKeyStore: KeyStore? = null

    /**
     * Initialize certificate manager with device-specific certificates
     */
    fun initialize(): Boolean {
        return try {
            // Initialize device keystore for client certificates
            deviceKeyStore = KeyStore.getInstance(KEY_STORE_TYPE)
            deviceKeyStore?.load(null, null)

            // Create custom trust manager for device validation
            trustManager = createCustomTrustManager()

            // Initialize key manager for client authentication
            keyManager = createKeyManager()

            Log.i(TAG, "Certificate manager initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize certificate manager", e)
            false
        }
    }

    /**
     * Create SSL context for secure WebSocket connections
     */
    fun createSSLContext(): SSLContext? {
        return try {
            val sslContext = SSLContext.getInstance(TLS_PROTOCOL)
            sslContext.init(
                keyManager?.let { arrayOf(it) },
                trustManager?.let { arrayOf(it) },
                SecureRandom(),
            )
            Log.d(TAG, "SSL context created successfully")
            sslContext
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create SSL context", e)
            null
        }
    }

    /**
     * Create SSL socket factory for OkHttp client
     */
    fun createSSLSocketFactory(): SSLSocketFactory? {
        return createSSLContext()?.socketFactory
    }

    /**
     * Get trust manager for certificate validation
     */
    fun getTrustManager(): X509TrustManager? = trustManager

    /**
     * Validate device certificate for thermal camera connections
     */
    fun validateDeviceCertificate(certificate: X509Certificate): Boolean {
        return try {
            // Check if certificate is from a valid Topdon device
            val subject = certificate.subjectDN.name
            val issuer = certificate.issuerDN.name

            // Validate certificate attributes
            val isValidDevice =
                subject.contains("CN=TOPDON") ||
                    subject.contains("CN=TC001") ||
                    subject.contains("CN=TS004") ||
                    subject.contains("CN=TC007")

            if (!isValidDevice) {
                Log.w(TAG, "Invalid device certificate subject: $subject")
                return false
            }

            // Check certificate validity period
            certificate.checkValidity()

            Log.d(TAG, "Device certificate validated: $subject")
            true
        } catch (e: CertificateException) {
            Log.e(TAG, "Certificate validation failed", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during certificate validation", e)
            false
        }
    }

    /**
     * Install device certificate for trusted communications
     */
    fun installDeviceCertificate(
        certificateData: ByteArray,
        alias: String,
    ): Boolean {
        return try {
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val certificate =
                certificateFactory.generateCertificate(
                    ByteArrayInputStream(certificateData),
                ) as X509Certificate

            // Validate the certificate before installing
            if (!validateDeviceCertificate(certificate)) {
                Log.w(TAG, "Refusing to install invalid certificate")
                return false
            }

            // Add to trust store
            deviceKeyStore?.setCertificateEntry(alias, certificate)

            Log.i(TAG, "Device certificate installed: $alias")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install device certificate", e)
            false
        }
    }

    /**
     * Create custom trust manager that validates device certificates
     */
    private fun createCustomTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(
                chain: Array<X509Certificate>,
                authType: String,
            ) {
                // For client certificates (when we're the server)
                validateCertificateChain(chain, "client")
            }

            override fun checkServerTrusted(
                chain: Array<X509Certificate>,
                authType: String,
            ) {
                // For server certificates (thermal cameras, PC controllers)
                validateCertificateChain(chain, "server")
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                // Return trusted certificate authorities
                return deviceKeyStore?.let { ks ->
                    val aliases = ks.aliases()
                    val certificates = mutableListOf<X509Certificate>()
                    while (aliases.hasMoreElements()) {
                        val alias = aliases.nextElement()
                        val cert = ks.getCertificate(alias) as? X509Certificate
                        cert?.let { certificates.add(it) }
                    }
                    certificates.toTypedArray()
                } ?: emptyArray()
            }

            private fun validateCertificateChain(
                chain: Array<X509Certificate>,
                type: String,
            ) {
                if (chain.isEmpty()) {
                    throw CertificateException("Empty certificate chain")
                }

                val leafCertificate = chain[0]

                // Validate the leaf certificate
                if (!validateDeviceCertificate(leafCertificate)) {
                    throw CertificateException("Invalid $type certificate")
                }

                // Additional chain validation could be added here
                Log.d(TAG, "Certificate chain validated for $type")
            }
        }
    }

    /**
     * Create key manager for client certificate authentication
     */
    private fun createKeyManager(): X509KeyManager? {
        return try {
            // For now, return null as we don't have client certificates
            // This can be extended to load client certificates from Android Keystore
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create key manager", e)
            null
        }
    }

    /**
     * Create hostname verifier for WebSocket connections
     */
    fun createHostnameVerifier(): HostnameVerifier {
        return HostnameVerifier { hostname, session ->
            // Allow connections to known thermal camera IP addresses
            val validHosts =
                setOf(
                    "192.168.40.1", // Standard thermal camera IP
                    "localhost", // Local testing
                    "127.0.0.1", // Local testing
                )

            val isValid =
                validHosts.contains(hostname) ||
                    hostname.matches(Regex("192\\.168\\.\\d+\\.\\d+")) // Local network IPs

            if (!isValid) {
                Log.w(TAG, "Hostname verification failed for: $hostname")
            }

            isValid
        }
    }

    /**
     * Generate device authentication token
     */
    fun generateAuthToken(): String {
        val deviceId =
            android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID,
            )

        val timestamp = System.currentTimeMillis()
        val nonce = SecureRandom().nextLong()

        // Simple token format: deviceId:timestamp:nonce:hash
        val payload = "$deviceId:$timestamp:$nonce"
        val hash = payload.hashCode().toString(16)

        return "$payload:$hash"
    }

    /**
     * Validate authentication token from remote device
     */
    fun validateAuthToken(
        token: String,
        maxAgeMs: Long = 300000,
    ): Boolean { // 5 minutes default
        return try {
            val parts = token.split(":")
            if (parts.size != 4) return false

            val timestamp = parts[1].toLong()
            val currentTime = System.currentTimeMillis()

            // Check token age
            if (currentTime - timestamp > maxAgeMs) {
                Log.w(TAG, "Auth token expired")
                return false
            }

            // Validate hash
            val payload = "${parts[0]}:${parts[1]}:${parts[2]}"
            val expectedHash = payload.hashCode().toString(16)

            if (parts[3] != expectedHash) {
                Log.w(TAG, "Auth token hash mismatch")
                return false
            }

            Log.d(TAG, "Auth token validated successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Auth token validation failed", e)
            false
        }
    }
}
