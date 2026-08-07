package com.example.myapplication.ui.home.chat.chatroom

import android.util.Log
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.model.MessageStatus
import com.example.myapplication.data.model.MessageType
import com.example.myapplication.data.model.User
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
            ?: senderObj?.get("id").asStringOrNull()
            ?: data.get("userId").asStringOrNull()
            ?: ""
        val senderName = data.get("senderName").asStringOrNull()
            ?: senderObj?.get("fullName").asStringOrNull() ?: "Hệ thống"
        val senderAvatar = data.get("senderAvatar").asStringOrNull()
            ?: senderObj?.get("avatar").asStringOrNull()
        val convId = data.get("conversationId").asStringOrNull() ?: activeConversationId

        val rawCreatedDate = data.get("createdDate").asStringOrNull()
            ?: data.get("createdAt").asStringOrNull()
            ?: data.get("timestamp").asStringOrNull()
            ?: ""

        val rawType = data.get("messageType").asStringOrNull()
            ?: data.get("type").asStringOrNull()
            ?: "TEXT"

        val messageType = try {
            MessageType.valueOf(rawType.uppercase())
        } catch (e: Exception) {
            MessageType.TEXT
        }

        val cleanContent = if (messageType == MessageType.IMAGE) {
            content.trimEnd(',', ';', ' ', '"', '\'')
        } else content

        return Message(
            id = msgId,
            conversationId = convId,
            sender = User(id = senderId, fullName = senderName, avatar = senderAvatar),
            content = cleanContent,
            type = messageType,
            status = MessageStatus.SENT,
            createdAt = if (rawCreatedDate.isNotEmpty()) rawCreatedDate else "Vừa xong",
            updatedAt = "",
            isRecalled = false
        )
    }

    fun isImageUrl(url: String): Boolean {
        if (url.startsWith("content://") || url.startsWith("file://") || url.contains("/cache/") || url.contains("upload_")) {
            return true
        }
        val lower = url.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
                lower.endsWith(".gif") || lower.endsWith(".webp") || lower.contains("cloudinary")
    }
}
