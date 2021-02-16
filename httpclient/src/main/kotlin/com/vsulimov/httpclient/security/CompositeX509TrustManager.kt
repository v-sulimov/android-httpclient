package com.vsulimov.httpclient.security

import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Represents an ordered list of [X509TrustManager]'s with additive trust.
 * If any one of the composed managers trusts a certificate chain,
 * then it is trusted by the composite manager.
 */
class CompositeX509TrustManager : X509TrustManager {

    private val trustManagers: MutableList<X509TrustManager>

    constructor(keyStore: KeyStore) {
        trustManagers = mutableListOf(
            getDefaultTrustManager(),
            getTrustManagerForKeyStore(keyStore)
        )
    }

    constructor(keyStores: List<KeyStore>) {
        trustManagers = mutableListOf(getDefaultTrustManager())
        keyStores.map { trustManagers += getTrustManagerForKeyStore(it) }
    }

    private fun getDefaultTrustManager(): X509TrustManager =
        getTrustManagerForKeyStore(null)

    private fun getTrustManagerForKeyStore(keyStore: KeyStore?): X509TrustManager {
        val algorithm = TrustManagerFactory.getDefaultAlgorithm()
        val factory = TrustManagerFactory.getInstance(algorithm)
        factory.init(keyStore)
        return factory.trustManagers.first { it is X509TrustManager } as X509TrustManager
    }

    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        for (trustManager in trustManagers) {
            try {
                trustManager.checkClientTrusted(chain, authType)
                return
            } catch (e: CertificateException) {
//                ignored
            }
        }
        throw CertificateException("None of the TrustManagers trust this certificate chain")
    }

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        for (trustManager in trustManagers) {
            try {
                trustManager.checkServerTrusted(chain, authType)
                return
            } catch (e: CertificateException) {
//                ignored
            }
        }
        throw CertificateException("None of the TrustManagers trust this certificate chain")
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        val certificates = mutableListOf<X509Certificate>()
        for (trustManager in trustManagers) {
            for (certificate in trustManager.acceptedIssuers) {
                certificates.add(certificate)
            }
        }
        return certificates.toTypedArray()
    }
}
