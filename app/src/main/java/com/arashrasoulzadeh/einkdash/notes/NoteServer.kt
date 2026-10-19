package com.arashrasoulzadeh.einkdash.notes

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import com.arashrasoulzadeh.einkdash.settings.AppPrefs
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

/**
 * Tiny LAN web page for adding notes from a phone or PC. Plain sockets keep it working on Android 4.2.
 * A PIN shown in Settings is required, so other people on the same Wi-Fi can't add notes.
 */
object NoteServer {
    const val PORT = 8080
    @Volatile private var socket: ServerSocket? = null
    private val main = Handler(Looper.getMainLooper())

    /** Called on the main thread after a note arrives, so the dashboard can redraw. */
    @Volatile var onNote: (() -> Unit)? = null

    val running get() = socket != null

    fun sync(ctx: Context) {
        if (AppPrefs(ctx).noteServer) start(ctx.applicationContext) else stop()
    }

    @Synchronized private fun start(ctx: Context) {
        if (socket != null) return
        val s = runCatching { ServerSocket(PORT) }.getOrNull() ?: return
        socket = s
        Thread({
            while (!s.isClosed) {
                val client = runCatching { s.accept() }.getOrNull() ?: break
                runCatching { handle(ctx, client) }
                runCatching { client.close() }
            }
        }, "note-server").apply { isDaemon = true }.start()
    }

    @Synchronized fun stop() {
        runCatching { socket?.close() }
        socket = null
    }

    /** http://<wifi-ip>:8080, or null when not on Wi-Fi. */
    fun address(ctx: Context): String? {
        @Suppress("DEPRECATION")
        val ip = (ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).connectionInfo?.ipAddress ?: 0
        if (ip == 0) return null
        return "http://${ip and 0xff}.${ip shr 8 and 0xff}.${ip shr 16 and 0xff}.${ip shr 24 and 0xff}:$PORT"
    }

    private fun handle(ctx: Context, client: Socket) {
        client.soTimeout = 5000
        val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
        val request = reader.readLine() ?: return
        var length = 0
        while (true) {
            val h = reader.readLine() ?: break
            if (h.isEmpty()) break
            if (h.startsWith("Content-Length:", ignoreCase = true)) length = h.substringAfter(":").trim().toIntOrNull() ?: 0
        }
        var message = ""
        if (request.startsWith("POST") && length in 1..8192) {
            val buf = CharArray(length)
            var read = 0
            while (read < length) { val n = reader.read(buf, read, length - read); if (n < 0) break; read += n }
            val form = String(buf, 0, read).split("&").associate {
                val kv = it.split("=", limit = 2)
                URLDecoder.decode(kv[0], "UTF-8") to URLDecoder.decode(kv.getOrElse(1) { "" }, "UTF-8")
            }
            val text = form["text"]?.trim().orEmpty().take(200)
            message = when {
                form["pin"] != AppPrefs(ctx).noteServerPin -> "Wrong PIN / پین اشتباه است"
                text.isEmpty() -> "Empty note / یادداشت خالی است"
                else -> { NotesDb(ctx).add(text); main.post { onNote?.invoke() }; "Added: " + esc(text) }
            }
        }
        val open = NotesDb(ctx).all().filter { !it.done }.take(20).joinToString("") { "<li>" + esc(it.text) + "</li>" }
        val html = """<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Eink Dash notes</title><style>body{font:18px sans-serif;max-width:480px;margin:24px auto;padding:0 16px}
input,button{font:inherit;width:100%;box-sizing:border-box;padding:12px;margin:6px 0;border:1.5px solid #000;border-radius:4px;background:#fff}
button{font-weight:bold}li{padding:6px 0;border-bottom:1px solid #000;list-style:none}ul{padding:0}p{font-weight:bold}</style></head>
<body><h1>Eink Dash</h1><p>$message</p><form method="post"><input name="text" placeholder="New note / یادداشت جدید" autofocus>
<input name="pin" placeholder="PIN" inputmode="numeric"><button>Add / افزودن</button></form><ul dir="auto">$open</ul></body></html>"""
        val bytes = html.toByteArray(Charsets.UTF_8)
        val out = client.getOutputStream()
        out.write(("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n").toByteArray())
        out.write(bytes)
        out.flush()
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
