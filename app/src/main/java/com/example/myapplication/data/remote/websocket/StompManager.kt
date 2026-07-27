package com.example.myapplication.data.remote.websocket

import android.os.Handler
import android.os.Looper
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

    private val parser = StompFrameParser()
    private val heartbeatManager = StompHeartbeatManager { webSocket?.send("\n") }
    private val reconnectHandler = Handler(Looper.getMainLooper())

    var listener: SocketListener? = null
    @Volatile
    private var isConnected = false

    private var savedUrl: String = ""
    private var savedToken: String = ""
    private val pendingSubscriptions = ConcurrentHashMap.newKeySet<String>()

    @Synchronized
    fun connect(url: String, accessToken: String) {
        if (url.isNotEmpty()) savedUrl = url
        if (accessToken.isNotEmpty()) savedToken = accessToken
        if (savedUrl.isEmpty() || savedToken.isEmpty()) return

        webSocket?.cancel()
        webSocket = null
        isConnected = false
        heartbeatManager.stop()

        val request = Request.Builder().url(savedUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(parser.buildConnectFrame(savedToken))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingText(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                heartbeatManager.stop()
                isConnected = false
                listener?.onDisconnected()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                heartbeatManager.stop()
                isConnected = false
                listener?.onDisconnected()
                scheduleReconnect()
            }
        })
    }

    private fun handleIncomingText(text: String) {
        val frame = parser.parse(text) ?: return
        when (frame.command) {
            "CONNECTED" -> {
                isConnected = true
                heartbeatManager.start()
                pendingSubscriptions.forEach { dest ->
                    webSocket?.send(parser.buildSubscribeFrame(dest))
                }
                listener?.onConnected()
            }
            "MESSAGE" -> {
                val destination = frame.headers["destination"] ?: ""
                if (frame.body.isNotEmpty()) {
                    listener?.onMessageReceived(destination, frame.body)
                }
            }
        }
    }

    private fun scheduleReconnect() {
        reconnectHandler.removeCallbacksAndMessages(null)
        if (savedUrl.isNotEmpty() && savedToken.isNotEmpty()) {
            reconnectHandler.postDelayed({
                if (!isConnected) {
                    connect(savedUrl, savedToken)
                }
            }, 3000)
        }
    }

    fun subscribe(destination: String) {
        pendingSubscriptions.add(destination)
        if (isConnected && webSocket != null) {
            webSocket?.send(parser.buildSubscribeFrame(destination))
        }
    }

    fun send(destination: String, body: String) {
        val sendFrame = parser.buildSendFrame(destination, body)
        val sent = isConnected && webSocket?.send(sendFrame) == true
        if (!sent) {
            connect(savedUrl, savedToken)
            reconnectHandler.postDelayed({
                webSocket?.send(sendFrame)
            }, 1000)
        }
    }

    fun disconnect() {
        heartbeatManager.stop()
        reconnectHandler.removeCallbacksAndMessages(null)
        pendingSubscriptions.clear()
        webSocket?.send(parser.buildDisconnectFrame())
        webSocket?.cancel()
        webSocket = null
        isConnected = false
    }
}
