package cl.clinipets.core.ws

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.TimeUnit

/**
 * Cliente STOMP mínimo sobre OkHttp WebSocket para la junta.
 * Solo soporta SEND a destino y SUBSCRIBE (tema). Sin ACK, sin heartbeats.
 */
class StompClient(
    private val url: String,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()
) {
    private var webSocket: WebSocket? = null
    private val eventChannel = Channel<StompEvent>(Channel.BUFFERED)

    sealed class StompEvent {
        object Open : StompEvent()
        data class Message(val destination: String?, val body: String) : StompEvent()
        data class Error(val t: Throwable) : StompEvent()
        object Closed : StompEvent()
    }

    fun events(): Flow<StompEvent> = eventChannel.receiveAsFlow()

    fun connect(headers: Map<String, String> = emptyMap()) {
        if (webSocket != null) return
        val bearer = headers["Authorization"]?.removePrefix("Bearer ")?.trim()?.takeIf { it.isNotEmpty() }
        val finalUrl = if (bearer != null) {
            // Adjunta access_token en query string además del header
            val sep = if (url.contains("?")) "&" else "?"
            "$url${sep}access_token=$bearer"
        } else url
        val requestBuilder = Request.Builder().url(finalUrl)
        headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
        val request = requestBuilder.build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                eventChannel.trySend(StompEvent.Open)
                // STOMP CONNECT frame con heart-beat propuesto
                val connectFrame = buildString {
                    append("CONNECT\n")
                    append("accept-version:1.2\n")
                    append("heart-beat:10000,10000\n")
                    append("\n\u0000")
                }
                webSocket.send(connectFrame)
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                // Parse STOMP frame simple
                val lines = text.split('\n')
                val command = lines.firstOrNull()?.trim()?.uppercase()
                val bodyIndex = lines.indexOfFirst { it.isBlank() }
                val headersLines = if (bodyIndex > 0) lines.subList(1, bodyIndex) else emptyList()
                val body = if (bodyIndex >= 0 && bodyIndex < lines.size - 1) lines.subList(bodyIndex + 1, lines.size).joinToString("\n") else ""
                val headersMap = headersLines.mapNotNull { h ->
                    val i = h.indexOf(':'); if (i > 0) h.substring(0, i) to h.substring(i + 1) else null
                }.toMap()
                if (command == "MESSAGE") {
                    eventChannel.trySend(StompEvent.Message(headersMap["destination"], body.trimEnd('\u0000')))
                }
            }
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessage(webSocket, bytes.utf8())
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                eventChannel.trySend(StompEvent.Error(t))
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                eventChannel.trySend(StompEvent.Closed)
            }
        })
    }

    fun subscribe(destination: String) {
        webSocket?.send("SUBSCRIBE\nid:sub-${destination}\ndestination:${destination}\n\n\u0000")
    }

    fun send(destination: String, body: String) {
        webSocket?.send("SEND\ndestination:${destination}\ncontent-type:application/json\n\n${body}\u0000")
    }

    fun disconnect() {
        webSocket?.close(1000, "bye")
        webSocket = null
    }
}
