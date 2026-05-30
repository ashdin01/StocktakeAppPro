package au.com.harcourtapples.stocktake.api

import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

/**
 * Trust-on-first-use (TOFU) TLS trust manager for self-signed BackOfficePro certs.
 *
 * First connection (trustedFingerprint == null): accepts any certificate and records
 * its SHA-256 fingerprint in [observedFingerprint]. The caller must persist that
 * fingerprint so future connections can verify against it.
 *
 * Subsequent connections: accepts only a cert whose fingerprint matches
 * [trustedFingerprint]. Any mismatch throws [CertificateException] with a
 * human-readable message guiding the user to reset via Settings.
 */
class TofuTrustManager(private val trustedFingerprint: String?) : X509TrustManager {

    /** Populated after the first successful TLS handshake. */
    var observedFingerprint: String? = null
        private set

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) = Unit
    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
        val fp = fingerprint(chain[0])
        when {
            trustedFingerprint == null -> {
                // First use — learn the cert
                observedFingerprint = fp
            }
            fp == trustedFingerprint -> {
                // Matches stored fingerprint — OK
                observedFingerprint = fp
            }
            else -> throw CertificateException(
                "Server certificate has changed — possible network issue.\n" +
                "Tap \"Forget Certificate\" in Settings → Server Connection to re-trust."
            )
        }
    }

    companion object {
        fun fingerprint(cert: X509Certificate): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(cert.encoded)
            return digest.joinToString(":") { "%02X".format(it) }
        }

        /** Format a full fingerprint into short display form: first 4 bytes + "…" */
        fun shortForm(fp: String): String {
            val parts = fp.split(":")
            return if (parts.size >= 4) parts.take(4).joinToString(":") + "…" else fp
        }
    }
}
