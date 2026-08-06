package com.example.myapplication.ui.home.chat.chatroom

import android.util.Log
import com.example.myapplication.data.model.Message
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject

class ChatMessageParser(private val gson: Gson = Gson()) {

    private fun JsonElement?.asStringOrNull(): String? {
        return if (this != null && !this.isJsonNull) this.asString else null
    }

    fun parseWsMessage(body: String, activeConversationId: String): Message? {
        val jsonObj = gson.fromJson(body, JsonObject::class.java) ?: return null
        val data = if (jsonObj.has("data") && jsonObj.get("data")?.isJsonObject == true) {
            jsonObj.getAsJsonObject("data")
        } else jsonObj

        val content = data.get("content").asStringOrNull() ?: data.get("text").asStringOrNull() ?: ""
        if (content.isEmpty()) return null

        val msgId = data.get("id").asStringOrNull()
            ?: data.get("messageId").asStringOrNull()
            ?: System.currentTimeMillis().toString()

        val senderObj = if (data.has("sender") && data.get("sender")?.isJsonObject == true) data.getAsJsonObject("sender") else null
        val senderId = data.get("senderId").asStringOrNull()
            ?: senderObj?.get("id").asStringOrNull() ?: ""
        val senderName = data.get("senderName").asStringOrNull()
            ?: senderObj?.get("fullName").asStringOrNull() ?: "Hệ thống"
        val convId = data.get("conversationId").asStringOrNull() ?: activeConversationId

        val rawCreatedDate = data.get("createdDate").asStringOrNull()
            ?: data.get("createdAt").asStringOrNull()
            ?: data.get("timestamp").asStringOrNull()
            ?: ""
        Log.d("CHAT_REALTIME_LOG", "[SERVER_TIME_LOG] Raw createdDate from server: '$rawCreatedDate' | Full JSON: $data")

        val rawType = data.get("messageType").asStringOrNull()
            ?: data.get("type").asStringOrNull()
            ?: "TEXT"

        val messageType = try {
            com.example.myapplication.data.model.MessageType.valueOf(rawType.uppercase())
        } catch (e: Exception) {
            com.example.myapplication.data.model.MessageType.TEXT
        }

        return Message(
            id = msgId,
            conversationId = convId,
            sender = com.example.myapplication.data.model.User(id = senderId, fullName = senderName, avatar = null),
            content = content,
            type = messageType,
            status = com.example.myapplication.data.model.MessageStatus.SENT,
            createdAt = if (rawCreatedDate.isNotEmpty()) {
                com.example.myapplication.utils.TimeUtils.formatChatTime(rawCreatedDate)
            } else "Vừa xong",
            updatedAt = "",
            isRecalled = false
        )
    }

    fun extractImageUrl(data: Any?): String {
        return when (data) {
            is String -> data
            is Map<*, *> -> data["url"]?.toString() ?: data["fileUrl"]?.toString() ?: data["path"]?.toString() ?: ""
            is JsonObject -> data.get("url").asStringOrNull() ?: data.get("fileUrl").asStringOrNull() ?: data.get("path").asStringOrNull() ?: ""
            else -> data?.toString() ?: ""
        }
    }
}
