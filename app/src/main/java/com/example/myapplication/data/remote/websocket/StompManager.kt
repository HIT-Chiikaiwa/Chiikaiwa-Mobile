package com.example.myapplication.data.remote.websocket

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class StompManager {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    var listener: SocketListener? = null
    @Volatile
    private var isConnected = false

    private var savedUrl: String = ""
    private var savedToken: String = ""

    private val pendingSubscriptions = ConcurrentHashMap.newKeySet<String>()

    private val handler = Handler(Looper.getMainLooper())
    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            if (isConnected && webSocket != null) {
                webSocket?.send("\n")
                handler.postDelayed(this, 10000)
            }
        }
    }

    @Synchronized
    fun connect(url: String, accessToken: String) {
        if (url.isNotEmpty()) savedUrl = url
        if (accessToken.isNotEmpty()) savedToken = accessToken

        if (savedUrl.isEmpty() || savedToken.isEmpty()) return

        webSocket?.cancel()
        webSocket = null
        isConnected = false
        stopHeartbeat()

        val request = Request.Builder()
            .url(savedUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val connectFrame = "CONNECT\n" +
                        "accept-version:1.1,1.2\n" +
                        "heart-beat:10000,10000\n" +
                        "Authorization:Bearer $savedToken\n\n" +
                        "\u0000"
                webSocket.send(connectFrame)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseStompFrame(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                stopHeartbeat()
                isConnected = false
                listener?.onDisconnected()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                stopHeartbeat()
                isConnected = false
                listener?.onDisconnected()
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        handler.removeCallbacksAndMessages(null)
        if (savedUrl.isNotEmpty() && savedToken.isNotEmpty()) {
            handler.postDelayed({
                if (!isConnected) {
                    connect(savedUrl, savedToken)
                }
            }, 3000)
        }
    }

    fun subscribe(destination: String) {
        pendingSubscriptions.add(destination)
        if (isConnected && webSocket != null) {
            val subscribeFrame = "SUBSCRIBE\n" +
                    "id:sub-$destination\n" +
                    "destination:$destination\n\n" +
                    "\u0000"
            webSocket?.send(subscribeFrame)
        }
    }

    fun send(destination: String, body: String) {
        val sendFrame = "SEND\n" +
                "destination:$destination\n" +
                "content-type:application/json\n\n" +
                "$body\u0000"

        val sentSuccessfully = isConnected && webSocket?.send(sendFrame) == true
        if (!sentSuccessfully) {
            connect(savedUrl, savedToken)
            handler.postDelayed({
                webSocket?.send(sendFrame)
            }, 1000)
        }
    }

    fun disconnect() {
        stopHeartbeat()
        handler.removeCallbacksAndMessages(null)
        pendingSubscriptions.clear()
        val disconnectFrame = "DISCONNECT\n\n\u0000"
        webSocket?.send(disconnectFrame)
        webSocket?.cancel()
        webSocket = null
        isConnected = false
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        handler.post(heartbeatRunnable)
    }

    private fun stopHeartbeat() {
        handler.removeCallbacks(heartbeatRunnable)
    }

    private fun parseStompFrame(text: String) {
        val cleanText = text.replace("\r", "")
        val lines = cleanText.split("\n")
        if (lines.isEmpty()) return
        val command = lines[0].trim()

        when (command) {
            "CONNECTED" -> {
                isConnected = true
                startHeartbeat()

                pendingSubscriptions.forEach { dest ->
                    val subscribeFrame = "SUBSCRIBE\n" +
                            "id:sub-$dest\n" +
                            "destination:$dest\n\n" +
                            "\u0000"
                    webSocket?.send(subscribeFrame)
                }

                listener?.onConnected()
            }
            "MESSAGE" -> {
                var destination = ""
                var bodyIndex = -1
                for (i in 1 until lines.size) {
                    val line = lines[i].trim()
                    if (line.startsWith("destination:")) {
                        destination = line.substringAfter("destination:").trim()
                    }
                    if (line.isEmpty()) {
                        bodyIndex = i + 1
                        break
                    }
                }
                if (bodyIndex != -1 && bodyIndex < lines.size) {
                    val body = lines.subList(bodyIndex, lines.size).joinToString("\n").replace("\u0000", "").trim()
                    if (body.isNotEmpty()) {
                        listener?.onMessageReceived(destination, body)
                    }
                }
            }
        }
    }
}
