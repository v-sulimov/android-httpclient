package com.vsulimov.httpclient.exception

/**
 * Signals that request ended up with unsuccessful status code (not in range [200..300)).
 */
class UnsuccessfulResponseStatusCodeException(
    requestUrl: String,
    responseCode: Int,
    val errorBody: String
) : Exception() {

    override val message =
        "Request to $requestUrl was failed with status code $responseCode." + " " +
                "See exception errorBody for details."
}
