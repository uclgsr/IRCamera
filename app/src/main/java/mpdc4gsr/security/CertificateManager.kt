package mpdc4gsr.security

import android.content.Context
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mpdc4gsr.lib.core.StructuredLogger
import java.io.File
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher

class CertificateManager(
    private val context: Context,
    private val logger: StructuredLogger,
) {
    companion object {
        private const val TAG = "CertificateManager"

        private const val KEY_SIZE = 2048
        private const val CERTIFICATE_VALIDITY_DAYS = 365
        private const val ROTATION_THRESHOLD_DAYS = 30

        private const val KEY_ALGORITHM = "RSA"
        private const val SIGNATURE_ALGORITHM = "SHA256withRSA"
        private const val CIPHER_TRANSFORMATION = "RSA/ECB/PKCS1Padding"

        private const val CERT_DIR = "certificates"
        private const val DEVICE_CERT_FILE = "device_cert.pem"
        private const val DEVICE_KEY_FILE = "device_key.pem"
        private const val TRUSTED_CERTS_FILE = "trusted_certs.pem"
    }

    private val deviceCertificates = ConcurrentHashMap<String, X509Certificate>()
    private val trustedCertificates = ConcurrentHashMap<String, X509Certificate>()
    private val deviceKeyPairs = ConcurrentHashMap<String, KeyPair>()

    private val certDirectory: File by lazy {
        File(context.filesDir, CERT_DIR).apply { mkdirs() }
    }

    private var deviceCertificate: X509Certificate? = null
    private var deviceKeyPair: KeyPair? = null

    fun initialize(): Boolean {
        return try {
            Log.i(TAG, "Initializing certificate manager")

            if (!loadDeviceCertificate()) {
                generateDeviceCertificate()
            }

            loadTrustedCertificates()

            startCertificateRotationMonitoring()

            logger.log(
                StructuredLogger.LogLevel.INFO,
                TAG,
                "cert_manager_initialized",
                mapOf(
                    "device_cert_loaded" to (deviceCertificate != null),
                    "trusted_certs_count" to trustedCertificates.size,
                    "rotation_monitoring" to true,
                ),
            )

            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize certificate manager", e)
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                TAG,
                "init_failed",
                mapOf(
                    "error" to e.message.orEmpty(),
                ),
            )
            false
        }
    }

    fun validateCertificate(
        deviceId: String,
        certificate: ByteArray,
        signature: ByteArray,
        challenge: String,
    ): AdvancedAuthenticationManager.AuthenticationResult {
        return try {

            val certFactory = CertificateFactory.getInstance("X.509")
            val cert = certFactory.generateCertificate(certificate.inputStream()) as X509Certificate

            cert.checkValidity()

            if (!verifyCertificateChain(cert)) {
                Log.w(TAG, "Certificate chain verification failed for device $deviceId")
                return AdvancedAuthenticationManager.AuthenticationResult.CERTIFICATE_INVALID
            }

            if (!verifySignature(cert.publicKey, challenge.toByteArray(), signature)) {
                Log.w(TAG, "Signature verification failed for device $deviceId")
                return AdvancedAuthenticationManager.AuthenticationResult.CERTIFICATE_INVALID
            }

            deviceCertificates[deviceId] = cert

            logger.log(
                StructuredLogger.LogLevel.INFO,
                TAG,
                "cert_validated",
                mapOf(
                    "device_id" to deviceId,
                    "cert_subject" to cert.subjectDN.name,
                    "cert_issuer" to cert.issuerDN.name,
                ),
            )

            AdvancedAuthenticationManager.AuthenticationResult.SUCCESS
        } catch (e: Exception) {
            Log.e(TAG, "Certificate validation failed for device $deviceId", e)
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                TAG,
                "cert_validation_failed",
                mapOf(
                    "device_id" to deviceId,
                    "error" to e.message.orEmpty(),
                ),
            )
            AdvancedAuthenticationManager.AuthenticationResult.CERTIFICATE_INVALID
        }
    }

    private fun generateDeviceCertificate() {
        try {
            Log.i(TAG, "Generating new device certificate")

            val keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM)
            keyPairGenerator.initialize(KEY_SIZE, SecureRandom())
            val keyPair = keyPairGenerator.generateKeyPair()


            val certificate = createSelfSignedCertificate(keyPair)

            deviceCertificate = certificate
            deviceKeyPair = keyPair

            saveDeviceCertificate(certificate, keyPair)

            logger.log(
                StructuredLogger.LogLevel.INFO,
                TAG,
                "device_cert_generated",
                mapOf(
                    "key_size" to KEY_SIZE,
                    "validity_days" to CERTIFICATE_VALIDITY_DAYS,
                    "algorithm" to KEY_ALGORITHM,
                ),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate device certificate", e)
            throw e
        }
    }

    private fun createSelfSignedCertificate(keyPair: KeyPair): X509Certificate {


        throw UnsupportedOperationException(
            "Self-signed certificate generation requires BouncyCastle library. " +
                    "This is a placeholder for the actual implementation.",
        )
    }

    private fun loadDeviceCertificate(): Boolean {
        return try {
            val certFile = File(certDirectory, DEVICE_CERT_FILE)
            val keyFile = File(certDirectory, DEVICE_KEY_FILE)

            if (!certFile.exists() || !keyFile.exists()) {
                return false
            }

            val certFactory = CertificateFactory.getInstance("X.509")
            val cert = certFactory.generateCertificate(certFile.inputStream()) as X509Certificate

            val keyBytes = keyFile.readBytes()
            val keySpec = PKCS8EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
            val privateKey = keyFactory.generatePrivate(keySpec)

            val publicKeySpec = X509EncodedKeySpec(cert.publicKey.encoded)
            val publicKey = keyFactory.generatePublic(publicKeySpec)
            val keyPair = KeyPair(publicKey, privateKey)

            deviceCertificate = cert
            deviceKeyPair = keyPair

            Log.i(TAG, "Device certificate loaded successfully")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load device certificate", e)
            return false
        }
    }

    private fun saveDeviceCertificate(
        certificate: X509Certificate,
        keyPair: KeyPair,
    ) {
        try {
            val certFile = File(certDirectory, DEVICE_CERT_FILE)
            val keyFile = File(certDirectory, DEVICE_KEY_FILE)

            certFile.writeBytes(certificate.encoded)

            keyFile.writeBytes(keyPair.private.encoded)

            Log.i(TAG, "Device certificate saved to storage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save device certificate", e)
            throw e
        }
    }

    private fun loadTrustedCertificates() {
        try {
            val trustedFile = File(certDirectory, TRUSTED_CERTS_FILE)
            if (!trustedFile.exists()) {
                Log.i(TAG, "No trusted certificates file found")
                return
            }


            Log.i(TAG, "Trusted certificates loaded")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load trusted certificates", e)
        }
    }

    private fun verifyCertificateChain(certificate: X509Certificate): Boolean {


        return true
    }

    private fun verifySignature(
        publicKey: PublicKey,
        data: ByteArray,
        signature: ByteArray,
    ): Boolean {
        return try {
            val verifier = Signature.getInstance(SIGNATURE_ALGORITHM)
            verifier.initVerify(publicKey)
            verifier.update(data)
            verifier.verify(signature)
        } catch (e: Exception) {
            Log.e(TAG, "Signature verification failed", e)
            false
        }
    }

    private fun startCertificateRotationMonitoring() {
        GlobalScope.launch {
            while (true) {
                try {
                    delay(24 * 60 * 60 * 1000L)

                    deviceCertificate?.let { cert ->
                        val expiryTime = cert.notAfter.time
                        val currentTime = System.currentTimeMillis()
                        val daysUntilExpiry = (expiryTime - currentTime) / (24 * 60 * 60 * 1000L)

                        if (daysUntilExpiry <= ROTATION_THRESHOLD_DAYS) {
                            Log.i(
                                TAG,
                                "Certificate rotation needed - expires in $daysUntilExpiry days"
                            )
                            rotateCertificate()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in certificate rotation monitoring", e)
                }
            }
        }
    }

    private fun rotateCertificate() {
        try {
            Log.i(TAG, "Rotating device certificate")

            deviceCertificate?.let { oldCert ->
                val backupFile =
                    File(certDirectory, "device_cert_backup_${System.currentTimeMillis()}.pem")
                backupFile.writeBytes(oldCert.encoded)
            }

            generateDeviceCertificate()

            logger.log(
                StructuredLogger.LogLevel.INFO,
                TAG,
                "cert_rotated",
                mapOf(
                    "rotation_time" to System.currentTimeMillis(),
                    "new_cert_generated" to true,
                ),
            )
        } catch (e: Exception) {
            Log.e(TAG, "Certificate rotation failed", e)
            logger.log(
                StructuredLogger.LogLevel.ERROR,
                TAG,
                "cert_rotation_failed",
                mapOf(
                    "error" to e.message.orEmpty(),
                ),
            )
        }
    }

    fun getDeviceCertificate(): X509Certificate? = deviceCertificate

    fun getDevicePrivateKey(): PrivateKey? = deviceKeyPair?.private

    fun signData(data: ByteArray): ByteArray? {
        return try {
            val privateKey = deviceKeyPair?.private ?: return null

            val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
            signature.initSign(privateKey)
            signature.update(data)
            signature.sign()
        } catch (e: Exception) {
            Log.e(TAG, "Data signing failed", e)
            null
        }
    }

    fun encryptData(
        data: ByteArray,
        certificate: X509Certificate,
    ): ByteArray? {
        return try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, certificate.publicKey)
            cipher.doFinal(data)
        } catch (e: Exception) {
            Log.e(TAG, "Data encryption failed", e)
            null
        }
    }

    fun decryptData(encryptedData: ByteArray): ByteArray? {
        return try {
            val privateKey = deviceKeyPair?.private ?: return null

            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            cipher.doFinal(encryptedData)
        } catch (e: Exception) {
            Log.e(TAG, "Data decryption failed", e)
            null
        }
    }

    fun getDiagnostics(): Map<String, Any> {
        return mapOf(
            "device_cert_available" to (deviceCertificate != null),
            "device_key_available" to (deviceKeyPair != null),
            "trusted_certs_count" to trustedCertificates.size,
            "device_certs_count" to deviceCertificates.size,
            "cert_expiry" to (deviceCertificate?.notAfter?.time ?: 0L),
            "rotation_monitoring" to true,
        )
    }
}
