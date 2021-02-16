package com.vsulimov.httpclient.exception

/**
 * Signals that request ended up with redirect (status code [300..400)).
 */
class RedirectException(
    requestUrl: String,
    responseCode: Int,
    val responseBody: String
) : Exception() {

    override val message =
        "Request to $requestUrl was redirected (status code $responseCode)." + " " +
                "See exception responseBody for details."
}
