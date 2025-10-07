// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\security' subtree
// Files: 1; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\security\CertificateManager.kt =====

package com.mpdc4gsr.libunified.app.security

import android.content.Context
import android.util.Log
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*

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
    fun initialize(): Boolean {
        return try {
            deviceKeyStore = KeyStore.getInstance(KEY_STORE_TYPE)
            deviceKeyStore?.load(null, null)
            trustManager = createCustomTrustManager()
            keyManager = createKeyManager()
            Log.i(TAG, "Certificate manager initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize certificate manager", e)
            false
        }
    }

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

    fun createSSLSocketFactory(): SSLSocketFactory? {
        return createSSLContext()?.socketFactory
    }

    fun getTrustManager(): X509TrustManager? = trustManager
    fun validateDeviceCertificate(certificate: X509Certificate): Boolean {
        return try {
            val subject = certificate.subjectDN.name
            val issuer = certificate.issuerDN.name
            val isValidDevice =
                subject.contains("CN=TOPDON") ||
                        subject.contains("CN=TC001") ||
                        subject.contains("CN=TS004") ||
                        subject.contains("CN=TC007")
            if (!isValidDevice) {
                Log.w(TAG, "Invalid device certificate subject: $subject")
                return false
            }
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
            if (!validateDeviceCertificate(certificate)) {
                Log.w(TAG, "Refusing to install invalid certificate")
                return false
            }
            deviceKeyStore?.setCertificateEntry(alias, certificate)
            Log.i(TAG, "Device certificate installed: $alias")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install device certificate", e)
            false
        }
    }

    private fun createCustomTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(
                chain: Array<X509Certificate>,
                authType: String,
            ) {
                validateCertificateChain(chain, "client")
            }

            override fun checkServerTrusted(
                chain: Array<X509Certificate>,
                authType: String,
            ) {
                validateCertificateChain(chain, "server")
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
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
                if (!validateDeviceCertificate(leafCertificate)) {
                    throw CertificateException("Invalid $type certificate")
                }
                Log.d(TAG, "Certificate chain validated for $type")
            }
        }
    }

    private fun createKeyManager(): X509KeyManager? {
        return try {
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create key manager", e)
            null
        }
    }

    fun createHostnameVerifier(): HostnameVerifier {
        return HostnameVerifier { hostname, session ->
            val validHosts =
                setOf(
                    "192.168.40.1",
                    "localhost",
                    "127.0.0.1",
                )
            val isValid =
                validHosts.contains(hostname) ||
                        hostname.matches(Regex("192\\.168\\.\\d+\\.\\d+"))
            if (!isValid) {
                Log.w(TAG, "Hostname verification failed for: $hostname")
            }
            isValid
        }
    }

    fun generateAuthToken(): String {
        val deviceId =
            android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID,
            )
        val timestamp = System.currentTimeMillis()
        val nonce = SecureRandom().nextLong()
        val payload = "$deviceId:$timestamp:$nonce"
        val hash = payload.hashCode().toString(16)
        return "$payload:$hash"
    }

    fun validateAuthToken(
        token: String,
        maxAgeMs: Long = 300000,
    ): Boolean {
        return try {
            val parts = token.split(":")
            if (parts.size != 4) return false
            val timestamp = parts[1].toLong()
            val currentTime = System.currentTimeMillis()
            if (currentTime - timestamp > maxAgeMs) {
                Log.w(TAG, "Auth token expired")
                return false
            }
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


