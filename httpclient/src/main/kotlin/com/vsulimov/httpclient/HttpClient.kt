package com.vsulimov.httpclient

import com.vsulimov.httpclient.configuration.HttpClientConfiguration
import com.vsulimov.httpclient.exception.RedirectException
import com.vsulimov.httpclient.exception.UnsuccessfulResponseStatusCodeException
import com.vsulimov.httpclient.extensions.readTextAndClose
import com.vsulimov.httpclient.interceptor.RequestInterceptor
import com.vsulimov.httpclient.request.DeleteRequest
import com.vsulimov.httpclient.request.GetRequest
import com.vsulimov.httpclient.request.PostRequest
import com.vsulimov.httpclient.request.PutRequest
import com.vsulimov.httpclient.request.Request
import com.vsulimov.httpclient.request.RequestWithBody
import com.vsulimov.httpclient.response.Response
import com.vsulimov.httpclient.security.CompositeX509TrustManager
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory

/**
 * Http client implementation.
 * You can configure it by passing [HttpClientConfiguration] object to the
 * class constructor. No-args constructor instantiate client with
 * default configuration.
 */
class HttpClient(
    private val configuration: HttpClientConfiguration = HttpClientConfiguration()
) {

    /**
     * Currently attached [RequestInterceptor]'s.
     */
    private val requestInterceptors = mutableListOf<RequestInterceptor>()

    /**
     * Custom [SSLSocketFactory] for this client.
     */
    private val sslSocketFactory: SSLSocketFactory

    init {
        val keyStores = mutableListOf<KeyStore>()
        configuration.certificateInputStream?.let {
            keyStores += getKeyStoreForCertificateInputStream(it)
        }
        val sslContext = SSLContext.getInstance("TLS")
        val trustManagers = arrayOf(CompositeX509TrustManager(keyStores))
        sslContext.init(null, trustManagers, null)
        sslSocketFactory = sslContext.socketFactory
    }

    private fun getKeyStoreForCertificateInputStream(inputStream: InputStream): KeyStore {
        val certificate = inputStream.use {
            CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
        }
        return KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setCertificateEntry("ca", certificate)
        }
    }

    /**
     * Adds given interceptor to the [HttpClient] interceptors list.
     */
    fun addRequestInterceptor(interceptor: RequestInterceptor) {
        requestInterceptors.add(interceptor)
    }

    /**
     * Removes given interceptor from the [HttpClient] interceptors list.
     */
    fun removeRequestInterceptor(interceptor: RequestInterceptor) {
        requestInterceptors.remove(interceptor)
    }

    /**
     * Removes all interceptors from the [HttpClient] interceptors list.
     */
    fun removeAllRequestInterceptors() {
        requestInterceptors.clear()
    }

    /**
     * Executes given GET request and returns response or error using the [onResult]
     * function.
     */
    fun executeGetRequest(request: GetRequest, onResult: (Result<Response>) -> Unit) {
        executeRequestInternal(request, onResult)
    }

    /**
     * Executes given POST request and returns response or error using the [onResult]
     * function.
     */
    fun executePostRequest(request: PostRequest, onResult: (Result<Response>) -> Unit) {
        executeRequestWithBodyInternal(request, onResult)
    }

    /**
     * Executes given PUT request and returns response or error using the [onResult]
     * function.
     */
    fun executePutRequest(request: PutRequest, onResult: (Result<Response>) -> Unit) {
        executeRequestWithBodyInternal(request, onResult)
    }

    /**
     * Executes given DELETE request and returns response or error using the [onResult]
     * function.
     */
    fun executeDeleteRequest(request: DeleteRequest, onResult: (Result<Response>) -> Unit) {
        executeRequestInternal(request, onResult)
    }

    /**
     * Executes given request and returns response or error using [onResult] function.
     */
    private fun executeRequestInternal(
        request: Request,
        onResult: (Result<Response>) -> Unit
    ) {
        var connection: HttpURLConnection? = null
        applyInterceptors(request)
        val url = URL(request.url)
        try {
            connection = url.openConnection() as HttpURLConnection
            if (connection is HttpsURLConnection) {
                connection.sslSocketFactory = sslSocketFactory
            }
            connection.run {
                readTimeout = configuration.readTimeout
                connectTimeout = configuration.connectTimeout
                requestMethod = request.requestMethod.toString()
                doInput = true
                setRequestProperty("Accept", "application/json")
                request.headers.forEach {
                    addRequestProperty(it.name, it.value)
                }
                connect()
                if (isRequestSuccessful(responseCode)) {
                    val responseBody = inputStream.readTextAndClose()
                    val response = Response(responseCode, responseBody)
                    onResult(Result.success(response))
                } else {
                    if (isRedirect(responseCode)) {
                        val responseBody = inputStream.readTextAndClose()
                        val exception = RedirectException(
                            request.url,
                            responseCode,
                            responseBody
                        )
                        onResult(Result.failure(exception))
                    } else {
                        val errorBody = errorStream?.readTextAndClose().orEmpty()
                        val exception = UnsuccessfulResponseStatusCodeException(
                            requestUrl = request.url,
                            responseCode = responseCode,
                            errorBody = errorBody
                        )
                        onResult(Result.failure(exception))
                    }
                }
            }
        } catch (exception: IOException) {
            onResult(Result.failure(exception))
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Executes given request with body and returns response or error using [onResult] function.
     */
    private fun executeRequestWithBodyInternal(
        requestWithBody: RequestWithBody,
        onResult: (Result<Response>) -> Unit
    ) {
        var connection: HttpURLConnection? = null
        applyInterceptors(requestWithBody)
        val url = URL(requestWithBody.url)
        try {
            connection = url.openConnection() as HttpURLConnection
            if (connection is HttpsURLConnection) {
                connection.sslSocketFactory = sslSocketFactory
            }
            connection.run {
                readTimeout = configuration.readTimeout
                connectTimeout = configuration.connectTimeout
                requestMethod = requestWithBody.requestMethod.toString()
                doInput = true
                doOutput = true
                setRequestProperty("Content-Type", "application/json; utf-8")
                setRequestProperty("Accept", "application/json")
                requestWithBody.headers.forEach {
                    addRequestProperty(it.name, it.value)
                }
                val requestBodyByteArray = requestWithBody.body.toByteArray()
                outputStream.write(requestBodyByteArray)
                if (isRequestSuccessful(responseCode)) {
                    val responseBody = inputStream.readTextAndClose()
                    val response = Response(responseCode, responseBody)
                    onResult(Result.success(response))
                } else {
                    if (isRedirect(responseCode)) {
                        val responseBody = inputStream.readTextAndClose()
                        val exception = RedirectException(
                            requestWithBody.url,
                            responseCode,
                            responseBody
                        )
                        onResult(Result.failure(exception))
                    } else {
                        val errorBody = errorStream?.readTextAndClose().orEmpty()
                        val exception = UnsuccessfulResponseStatusCodeException(
                            requestUrl = requestWithBody.url,
                            responseCode = responseCode,
                            errorBody = errorBody
                        )
                        onResult(Result.failure(exception))
                    }
                }
            }
        } catch (exception: IOException) {
            onResult(Result.failure(exception))
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Apply every interceptor from [requestInterceptors] list to the given
     * request.
     */
    private fun applyInterceptors(request: Request) {
        for (requestInterceptor in requestInterceptors) {
            requestInterceptor.intercept(request)
        }
    }

    /**
     * Returns true if the response status code code is in [200..300), which means
     * the request was successfully received, understood, and accepted.
     */
    private fun isRequestSuccessful(responseCode: Int) =
        responseCode in 200..299

    /**
     * Returns true if response status code is in [300..400), which means
     * the request was received the redirect.
     */
    private fun isRedirect(responseCode: Int) =
        responseCode in 300..399
}
