package com.vsulimov.httpclient.extensions

import java.io.InputStream
import java.nio.charset.Charset

/**
 * This file contains extension functions for the InputStream.
 **/

/**
 * Reads [InputStream] content and returns the result as a [String].
 */
fun InputStream.readTextAndClose(charset: Charset = Charsets.UTF_8) =
    this.bufferedReader(charset).use { it.readText() }
