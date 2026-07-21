package com.example.myapplication.data.remote.websocket

interface SocketListener {
    fun onConnected()
    fun onDisconnected()
    fun onMessageReceived(message: String)
}
