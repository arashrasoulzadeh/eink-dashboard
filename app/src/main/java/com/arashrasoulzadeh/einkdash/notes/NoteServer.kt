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

    /** Called on the main thread after a backup was restored over Wi-Fi (layouts changed: rebuild). */
    @Volatile var onRestore: (() -> Unit)? = null

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
        val path = request.split(" ").getOrElse(1) { "/" }
        val query = path.substringAfter("?", "").split("&").associate {
            val kv = it.split("=", limit = 2)
            URLDecoder.decode(kv[0], "UTF-8") to URLDecoder.decode(kv.getOrElse(1) { "" }, "UTF-8")
        }
        val pinOk = query["pin"] == AppPrefs(ctx).noteServerPin
        var length = 0
        while (true) {
            val h = reader.readLine() ?: break
            if (h.isEmpty()) break
            if (h.startsWith("Content-Length:", ignoreCase = true)) length = h.substringAfter(":").trim().toIntOrNull() ?: 0
        }
        // GET /backup?pin=…  → the same JSON as Settings → Export.
        if (request.startsWith("GET") && path.startsWith("/backup")) {
            if (!pinOk) return respond(client, 403, "text/plain; charset=utf-8", "Wrong PIN".toByteArray())
            val json = com.arashrasoulzadeh.einkdash.settings.Backup.export(ctx).toByteArray(Charsets.UTF_8)
            return respond(client, 200, "application/json; charset=utf-8", json, "Content-Disposition: attachment; filename=\"einkdash-backup.json\"\r\n")
        }
        // POST /restore?pin=…  with the backup JSON as the body (sent by the page's file picker).
        if (request.startsWith("POST") && path.startsWith("/restore")) {
            if (!pinOk) return respond(client, 403, "text/plain; charset=utf-8", "Wrong PIN / پین اشتباه است".toByteArray())
            if (length !in 1..2_000_000) return respond(client, 400, "text/plain; charset=utf-8", "Bad size".toByteArray())
            val body = readBody(reader, length)
            val ok = runCatching { com.arashrasoulzadeh.einkdash.settings.Backup.import(ctx, body) }.isSuccess
            if (ok) main.post { onRestore?.invoke() }
            return respond(client, if (ok) 200 else 400, "text/plain; charset=utf-8",
                (if (ok) "Restored / بازگردانی شد" else "Not an Eink Dash backup / فایل پشتیبان معتبر نیست").toByteArray())
        }
        var message = ""
        if (request.startsWith("POST") && length in 1..8192) {
            val form = readBody(reader, length).split("&").associate {
                val kv = it.split("=", limit = 2)
                URLDecoder.decode(kv[0], "UTF-8") to URLDecoder.decode(kv.getOrElse(1) { "" }, "UTF-8")
            }
            val text = form["text"]?.trim().orEmpty().take(200)
            message = when {
                form["pin"] != AppPrefs(ctx).noteServerPin -> "Wrong PIN / پین اشتباه است"
                text.isEmpty() -> "Empty note / یادداشت خالی است"
                else -> {
                    val due = form["due"].orEmpty().replace("-", "").toIntOrNull() ?: 0 // yyyy-MM-dd from <input type=date>
                    NotesDb.of(ctx).add(text, due); main.post { onNote?.invoke() }; "Added: " + esc(text)
                }
            }
        }
        val open = NotesDb.of(ctx).all().filter { !it.done }.take(20).joinToString("") { "<li>" + esc(it.text) + "</li>" }
        val html = """<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Eink Dash notes</title><style>body{font:18px sans-serif;max-width:480px;margin:24px auto;padding:0 16px}
input,button{font:inherit;width:100%;box-sizing:border-box;padding:12px;margin:6px 0;border:1.5px solid #000;border-radius:4px;background:#fff}
button{font-weight:bold}li{padding:6px 0;border-bottom:1px solid #000;list-style:none}ul{padding:0}p{font-weight:bold}</style></head>
<body><h1>Eink Dash</h1><p>$message</p><form method="post"><input name="text" placeholder="New note / یادداشت جدید" autofocus>
<input name="due" type="date"><input name="pin" id="pin" placeholder="PIN" inputmode="numeric"><button>Add / افزودن</button></form><ul dir="auto">$open</ul>
<h2>Backup / پشتیبان</h2><button type="button" onclick="location='/backup?pin='+encodeURIComponent(pin.value)">Download backup / دریافت</button>
<input type="file" id="f" accept=".json,application/json"><button type="button" onclick="restore()">Restore / بازگردانی</button><p id="r"></p>
<script>function restore(){var file=f.files[0];if(!file){r.textContent='Choose a file first';return}
var x=new XMLHttpRequest();x.open('POST','/restore?pin='+encodeURIComponent(pin.value));x.onload=function(){r.textContent=x.responseText};x.send(file)}</script>
</body></html>"""
        respond(client, 200, "text/html; charset=utf-8", html.toByteArray(Charsets.UTF_8))
    }

    private fun readBody(reader: BufferedReader, length: Int): String {
        // Content-Length counts bytes; reading chars stops at the end of the stream for multibyte text.
        val buf = CharArray(length)
        var read = 0
        while (read < length) { val n = reader.read(buf, read, length - read); if (n < 0) break; read += n; if (!reader.ready()) break }
        return String(buf, 0, read)
    }

    private fun respond(client: Socket, code: Int, type: String, body: ByteArray, extraHeaders: String = "") {
        val status = when (code) { 200 -> "OK"; 403 -> "Forbidden"; else -> "Bad Request" }
        val out = client.getOutputStream()
        out.write(("HTTP/1.1 $code $status\r\nContent-Type: $type\r\nContent-Length: ${body.size}\r\n${extraHeaders}Connection: close\r\n\r\n").toByteArray())
        out.write(body)
        out.flush()
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
