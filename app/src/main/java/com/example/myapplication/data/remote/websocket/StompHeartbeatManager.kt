package com.example.myapplication.data.remote.websocket

import android.os.Handler
import android.os.Looper

class StompHeartbeatManager(
    private val intervalMs: Long = 10000L,
    private val onSendHeartbeat: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                onSendHeartbeat()
                handler.postDelayed(this, intervalMs)
            }
        }
    }

    fun start() {
        stop()
        isRunning = true
        handler.post(heartbeatRunnable)
    }

    fun stop() {
        isRunning = false
        handler.removeCallbacks(heartbeatRunnable)
    }
}
