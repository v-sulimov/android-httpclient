package com.vsulimov.httpclient.request

/**
 * Base class for all requests which can be executed by [com.vsulimov.httpclient.HttpClient].
 */
abstract class Request(
    val headers: MutableList<Header> = mutableListOf(),
    val requestMethod: RequestMethod,
    var url: String
)

/**
 * Base class for all requests with body (POST, PUT).
 */
abstract class RequestWithBody(
    requestMethod: RequestMethod,
    url: String,
    var body: String
) : Request(requestMethod = requestMethod, url = url)

/**
 * Represents GET request.
 */
class GetRequest(
    url: String
) : Request(requestMethod = RequestMethod.GET, url = url)

/**
 * Represents POST request.
 */
class PostRequest(
    url: String,
    body: String
) : RequestWithBody(requestMethod = RequestMethod.POST, url = url, body = body)

/**
 * Represents PUT request.
 */
class PutRequest(
    url: String,
    body: String
) : RequestWithBody(requestMethod = RequestMethod.PUT, url = url, body = body)

/**
 * Represents DELETE request.
 */
class DeleteRequest(
    url: String
) : Request(requestMethod = RequestMethod.DELETE, url = url)
