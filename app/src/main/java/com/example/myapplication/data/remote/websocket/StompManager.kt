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
    companion object {
        private const val TAG = "CHAT_REALTIME_LOG"
    }

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

        Log.d(TAG, "[CONNECT_START] URL: $savedUrl")

        webSocket?.cancel()
        webSocket = null
        isConnected = false
        heartbeatManager.stop()

        val request = Request.Builder()
            .url(savedUrl)
            .addHeader("Authorization", "Bearer $savedToken")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "[WS_OPEN] Sending CONNECT frame...")
                webSocket.send(parser.buildConnectFrame(savedToken))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingText(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "[WS_CLOSED] Code: $code, Reason: $reason")
                heartbeatManager.stop()
                isConnected = false
                listener?.onDisconnected()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "[WS_FAILURE] Error: ${t.message}", t)
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
                Log.d(TAG, "[STOMP_CONNECTED] Connected successfully! Subscribing pending topics count: ${pendingSubscriptions.size}")
                isConnected = true
                heartbeatManager.start()
                pendingSubscriptions.forEach { dest ->
                    Log.d(TAG, "[SUBSCRIBE_AUTO] -> Destination: $dest")
                    webSocket?.send(parser.buildSubscribeFrame(dest))
                }
                listener?.onConnected()
            }
            "MESSAGE" -> {
                val destination = frame.headers["destination"] ?: ""
                Log.d(TAG, "[MESSAGE_RECEIVED] <- Destination: $destination | Body: ${frame.body}")
                if (frame.body.isNotEmpty()) {
                    listener?.onMessageReceived(destination, frame.body)
                }
            }
            "ERROR" -> {
                Log.e(TAG, "[STOMP_ERROR] Server returned error: ${frame.body}")
            }
        }
    }

    private fun scheduleReconnect() {
        reconnectHandler.removeCallbacksAndMessages(null)
        if (savedUrl.isNotEmpty() && savedToken.isNotEmpty()) {
            Log.d(TAG, "[RECONNECT_SCHEDULED] Will reconnect in 3s...")
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
            Log.d(TAG, "[SUBSCRIBE] -> Destination: $destination")
            webSocket?.send(parser.buildSubscribeFrame(destination))
        } else {
            Log.d(TAG, "[SUBSCRIBE_QUEUED] -> Destination: $destination (waiting for connection)")
        }
    }

    fun send(destination: String, body: String) {
        val sendFrame = parser.buildSendFrame(destination, body)
        Log.d(TAG, "[SEND_MESSAGE] -> Destination: $destination | Body: $body")
        val sent = isConnected && webSocket?.send(sendFrame) == true
        if (!sent) {
            Log.w(TAG, "[SEND_FAILED] WebSocket not connected or send failed. Retrying connect...")
            connect(savedUrl, savedToken)
            reconnectHandler.postDelayed({
                webSocket?.send(sendFrame)
            }, 1000)
        }
    }

    fun disconnect() {
        Log.d(TAG, "[DISCONNECT] Disconnecting WebSocket...")
        heartbeatManager.stop()
        reconnectHandler.removeCallbacksAndMessages(null)
        pendingSubscriptions.clear()
        webSocket?.send(parser.buildDisconnectFrame())
        webSocket?.cancel()
        webSocket = null
        isConnected = false
    }
}
