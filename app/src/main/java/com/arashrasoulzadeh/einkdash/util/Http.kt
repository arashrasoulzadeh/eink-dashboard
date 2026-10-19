package com.arashrasoulzadeh.einkdash.util

import android.os.Build
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.Socket
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

object Http {
    class Response(val code: Int, val body: String)

    /** Android 4.1–4.4 support TLS 1.2 but leave it disabled; this turns it on. */
    private val tls12: SSLSocketFactory? by lazy {
        if (Build.VERSION.SDK_INT >= 20) null
        else runCatching {
            val ctx = SSLContext.getInstance("TLSv1.2").apply { init(null, null, null) }
            Tls12SocketFactory(ctx.socketFactory)
        }.getOrNull()
    }

    fun get(url: String, timeoutMs: Int = 10_000): Response {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            if (c is HttpsURLConnection) tls12?.let { c.sslSocketFactory = it }
            c.connectTimeout = timeoutMs
            c.readTimeout = timeoutMs
            c.setRequestProperty("User-Agent", "EinkDash")
            val code = c.responseCode
            val stream = if (code < 400) c.inputStream else c.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            return Response(code, body)
        } finally {
            c.disconnect()
        }
    }

    /**
     * Old devices lack newer root certificates; for public, non-sensitive data
     * (weather) retry over plain HTTP when the TLS handshake fails.
     */
    fun getAllowingHttpFallback(url: String): Response = try {
        get(url)
    } catch (e: SSLException) {
        if (url.startsWith("https://")) get("http://" + url.removePrefix("https://")) else throw e
    }

    private class Tls12SocketFactory(private val d: SSLSocketFactory) : SSLSocketFactory() {
        override fun getDefaultCipherSuites(): Array<String> = d.defaultCipherSuites
        override fun getSupportedCipherSuites(): Array<String> = d.supportedCipherSuites
        override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean) = patch(d.createSocket(s, host, port, autoClose))
        override fun createSocket(host: String, port: Int) = patch(d.createSocket(host, port))
        override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int) = patch(d.createSocket(host, port, localHost, localPort))
        override fun createSocket(host: InetAddress, port: Int) = patch(d.createSocket(host, port))
        override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int) = patch(d.createSocket(address, port, localAddress, localPort))

        private fun patch(s: Socket): Socket {
            (s as? SSLSocket)?.let { it.enabledProtocols = it.supportedProtocols.filter { p -> p.startsWith("TLSv1") }.toTypedArray() }
            return s
        }
    }
}
