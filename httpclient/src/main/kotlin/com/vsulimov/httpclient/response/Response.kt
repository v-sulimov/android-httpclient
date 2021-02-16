package com.vsulimov.httpclient.response

/**
 * Response for [com.vsulimov.httpclient.request.Request] executed
 * with [com.vsulimov.httpclient.HttpClient].
 */
class Response(
    val statusCode: Int,
    val body: String
)
