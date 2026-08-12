package com.example.myapplication.data.remote.websocket

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
    private var tokenProvider: (() -> String)? = null
    private var refreshTokenProvider: (() -> String)? = null
    private var tokenSaver: ((String, String) -> Unit)? = null
    private val pendingSubscriptions = ConcurrentHashMap.newKeySet<String>()

    @Synchronized
    fun connect(url: String, accessToken: String) {
        connect(
            url = url,
            tokenProvider = { accessToken },
            refreshTokenProvider = { "" },
            tokenSaver = { _, _ -> }
        )
    }

    @Synchronized
    fun connect(
        url: String,
        tokenProvider: () -> String,
        refreshTokenProvider: () -> String,
        tokenSaver: (accessToken: String, refreshToken: String) -> Unit
    ) {
        if (url.isNotEmpty()) savedUrl = url
        this.tokenProvider = tokenProvider
        this.refreshTokenProvider = refreshTokenProvider
        this.tokenSaver = tokenSaver

        if (savedUrl.isEmpty()) return

        val currentToken = tokenProvider()
        val wsUrl = if (savedUrl.contains("?token=")) {
            savedUrl.substringBefore("?token=") + "?token=$currentToken"
        } else {
            savedUrl + "?token=$currentToken"
        }

        Log.d(TAG, "[CONNECT_START] URL: $wsUrl")

        webSocket?.cancel()
        webSocket = null
        isConnected = false
        heartbeatManager.stop()

        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("Authorization", "Bearer $currentToken")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "[WS_OPEN] Sending CONNECT frame...")
                webSocket.send(parser.buildConnectFrame(currentToken))
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

                val isUnauthorized = response?.code == 401 || t.message?.contains("401") == true
                if (isUnauthorized) {
                    Log.w(TAG, "[WS_FAILURE] Unauthorized (401). Attempting token refresh...")
                    val refreshed = performTokenRefreshSync()
                    if (refreshed) {
                        Log.d(TAG, "[WS_FAILURE] Token refreshed successfully. Reconnecting...")
                        connect(savedUrl, tokenProvider, refreshTokenProvider, tokenSaver)
                        return
                    } else {
                        Log.e(TAG, "[WS_FAILURE] Token refresh failed. Cannot reconnect.")
                    }
                }

                scheduleReconnect()
            }
        })
    }

    private fun performTokenRefreshSync(): Boolean {
        val refreshTok = refreshTokenProvider?.invoke() ?: return false
        if (refreshTok.isEmpty()) return false

        return try {
            val jsonBody = com.google.gson.JsonObject().apply {
                addProperty("refreshToken", refreshTok)
            }.toString()

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = jsonBody.toRequestBody(mediaType)

            val refreshRequest = Request.Builder()
                .url(com.example.myapplication.data.remote.network.NetworkConstants.BASE_URL + "api/v1/auth/refresh")
                .post(requestBody)
                .build()

            val refreshResponse = client.newCall(refreshRequest).execute()

            if (refreshResponse.isSuccessful) {
                val responseBodyStr = refreshResponse.body?.string()
                refreshResponse.close()
                if (!responseBodyStr.isNullOrEmpty()) {
                    val jsonObject = com.google.gson.JsonParser.parseString(responseBodyStr).asJsonObject
                    if (jsonObject.has("data") && !jsonObject.get("data").isJsonNull) {
                        val dataObj = jsonObject.getAsJsonObject("data")
                        val newAccessToken = if (dataObj.has("accessToken") && !dataObj.get("accessToken").isJsonNull) dataObj.get("accessToken").asString else null
                        val newRefreshToken = if (dataObj.has("refreshToken") && !dataObj.get("refreshToken").isJsonNull) dataObj.get("refreshToken").asString else refreshTok

                        if (!newAccessToken.isNullOrEmpty()) {
                            tokenSaver?.invoke(newAccessToken, newRefreshToken)
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                } else {
                    false
                }
            } else {
                refreshResponse.close()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing token inside StompManagerSync", e)
            false
        }
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
        if (savedUrl.isNotEmpty() && tokenProvider != null && refreshTokenProvider != null && tokenSaver != null) {
            Log.d(TAG, "[RECONNECT_SCHEDULED] Will reconnect in 3s...")
            reconnectHandler.postDelayed({
                if (!isConnected) {
                    val url = savedUrl
                    val provider = tokenProvider
                    val refreshProvider = refreshTokenProvider
                    val saver = tokenSaver
                    if (url.isNotEmpty() && provider != null && refreshProvider != null && saver != null) {
                        connect(url, provider, refreshProvider, saver)
                    }
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

    fun unsubscribe(destination: String) {
        pendingSubscriptions.remove(destination)
        if (isConnected && webSocket != null) {
            Log.d(TAG, "[UNSUBSCRIBE] -> Destination: $destination")
            webSocket?.send(parser.buildUnsubscribeFrame(destination))
        } else {
            Log.d(TAG, "[UNSUBSCRIBE_QUEUED] -> Destination: $destination (waiting for connection)")
        }
    }

    fun send(destination: String, body: String) {
        val sendFrame = parser.buildSendFrame(destination, body)
        Log.d(TAG, "[SEND_MESSAGE] -> Destination: $destination | Body: $body")
        val sent = isConnected && webSocket?.send(sendFrame) == true
        if (!sent) {
            Log.w(TAG, "[SEND_FAILED] WebSocket not connected or send failed. Retrying connect...")
            val provider = tokenProvider
            val refreshProvider = refreshTokenProvider
            val saver = tokenSaver
            if (provider != null && refreshProvider != null && saver != null) {
                connect(savedUrl, provider, refreshProvider, saver)
            } else {
                connect(savedUrl, "")
            }
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
