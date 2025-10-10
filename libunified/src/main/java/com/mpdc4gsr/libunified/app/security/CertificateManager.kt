package com.mpdc4gsr.libunified.app.security

import android.content.Context
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.*

class CertificateManager(
    private val context: Context,
) {
    companion object {
        private const val TAG = "CertificateManager"
        private const val TRUST_STORE_ALIAS = "topdon_devices"
        private const val KEY_STORE_TYPE = "PKCS12"
        private const val TLS_PROTOCOL = "TLS"
    }

    private var trustManager: X509TrustManager? = null
    private var keyManager: X509KeyManager? = null
    private var deviceKeyStore: KeyStore? = null

    fun initialize(): Boolean =
        try {
            deviceKeyStore = KeyStore.getInstance(KEY_STORE_TYPE)
            deviceKeyStore?.load(null, null)
            trustManager = createCustomTrustManager()
            keyManager = createKeyManager()
            true
        } catch (e: Exception) {
            false
        }

    fun createSSLContext(): SSLContext? =
        try {
            val sslContext = SSLContext.getInstance(TLS_PROTOCOL)
            sslContext.init(
                keyManager?.let { arrayOf(it) },
                trustManager?.let { arrayOf(it) },
                SecureRandom(),
            )
            sslContext
        } catch (e: Exception) {
            null
        }

    fun createSSLSocketFactory(): SSLSocketFactory? = createSSLContext()?.socketFactory

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
                return false
            }
            certificate.checkValidity()
            true
        } catch (e: CertificateException) {
            false
        } catch (e: Exception) {
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
                return false
            }
            deviceKeyStore?.setCertificateEntry(alias, certificate)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun createCustomTrustManager(): X509TrustManager =
        object : X509TrustManager {
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

            override fun getAcceptedIssuers(): Array<X509Certificate> =
                deviceKeyStore?.let { ks ->
                    val aliases = ks.aliases()
                    val certificates = mutableListOf<X509Certificate>()
                    while (aliases.hasMoreElements()) {
                        val alias = aliases.nextElement()
                        val cert = ks.getCertificate(alias) as? X509Certificate
                        cert?.let { certificates.add(it) }
                    }
                    certificates.toTypedArray()
                } ?: emptyArray()

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
            }
        }

    private fun createKeyManager(): X509KeyManager? =
        try {
            null
        } catch (e: Exception) {
            null
        }

    fun createHostnameVerifier(): HostnameVerifier =
        HostnameVerifier { hostname, session ->
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
            }
            isValid
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
                return false
            }
            val payload = "${parts[0]}:${parts[1]}:${parts[2]}"
            val expectedHash = payload.hashCode().toString(16)
            if (parts[3] != expectedHash) {
                return false
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
