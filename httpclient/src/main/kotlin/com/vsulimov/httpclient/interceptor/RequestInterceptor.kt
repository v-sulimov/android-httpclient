package com.vsulimov.httpclient.interceptor

import com.vsulimov.httpclient.request.Request

/**
 * Request interceptor provides a third-party extension point between execution of request
 * and the moment it reaches the [com.vsulimov.httpclient.HttpClient].
 * You can modify request here (i.e. add some query parameter, header), or log it
 * for better debugging.
 */
abstract class RequestInterceptor {

    /**
     * Intercepts and modify incoming request.
     */
    abstract fun intercept(request: Request)
}
