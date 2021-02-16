package com.vsulimov.httpclient.configuration

import java.io.InputStream

/**
 * Configuration for [com.vsulimov.httpclient.HttpClient].
 *
 * **Default values:**
 *
 * Read timeout - 3000 ms.
 *
 * Connect timeout - 3000 ms.
 *
 * Certificate input stream - null
 */
class HttpClientConfiguration(
    val readTimeout: Int = 3000,
    val connectTimeout: Int = 3000,
    val certificateInputStream: InputStream? = null
)
