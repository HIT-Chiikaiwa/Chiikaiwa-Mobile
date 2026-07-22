package com.example.myapplication.data.remote.websocket

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class StompManager {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    var listener: SocketListener? = null
    private var isConnected = false

    fun connect(url: String, accessToken: String) {
        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val connectFrame = "CONNECT\n" +
                        "accept-version:1.1,1.2\n" +
                        "heart-beat:10000,10000\n" +
                        "Authorization:Bearer $accessToken\n\n" +
                        "\u0000"
                webSocket.send(connectFrame)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseStompFrame(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                listener?.onDisconnected()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                listener?.onDisconnected()
            }
        })
    }

    fun subscribe(destination: String) {
        val subscribeFrame = "SUBSCRIBE\n" +
                "id:sub-$destination\n" +
                "destination:$destination\n\n" +
                "\u0000"
        webSocket?.send(subscribeFrame)
    }

    fun send(destination: String, body: String) {
        val sendFrame = "SEND\n" +
                "destination:$destination\n" +
                "content-type:application/json\n\n" +
                "$body\u0000"
        webSocket?.send(sendFrame)
    }

    fun disconnect() {
        val disconnectFrame = "DISCONNECT\n\n\u0000"
        webSocket?.send(disconnectFrame)
        webSocket?.close(1000, "Normal Closure")
        isConnected = false
    }

    private fun parseStompFrame(text: String) {
        val lines = text.split("\n")
        if (lines.isEmpty()) return
        val command = lines[0]

        when (command) {
            "CONNECTED" -> {
                isConnected = true
                listener?.onConnected()
            }
            "MESSAGE" -> {
                var destination = ""
                var bodyIndex = 0
                for (i in 1 until lines.size) {
                    if (lines[i].startsWith("destination:")) {
                        destination = lines[i].substringAfter("destination:")
                    }
                    if (lines[i].isEmpty()) {
                        bodyIndex = i + 1
                        break
                    }
                }
                if (bodyIndex < lines.size) {
                    val body = lines.subList(bodyIndex, lines.size).joinToString("\n").replace("\u0000", "")
                    listener?.onMessageReceived(destination, body)
                }
            }
        }
    }
}
