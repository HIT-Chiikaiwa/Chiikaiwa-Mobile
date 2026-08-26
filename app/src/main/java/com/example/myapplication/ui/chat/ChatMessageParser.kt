package com.example.myapplication.ui.chat

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

        val replyToObj = if (data.has("replyToMessage") && data.get("replyToMessage")?.isJsonObject == true) {
            data.getAsJsonObject("replyToMessage")
        } else null

        val replyTo = replyToObj?.let {
            com.example.myapplication.data.model.ReplyMessage(
                id = it.get("id").asStringOrNull(),
                senderName = it.get("senderName").asStringOrNull(),
                content = it.get("content").asStringOrNull(),
                messageType = it.get("messageType").asStringOrNull()
            )
        }

        val reactionsList = mutableListOf<com.example.myapplication.data.model.Reaction>()
        if (data.has("reactions") && data.get("reactions")?.isJsonArray == true) {
            val arr = data.getAsJsonArray("reactions")
            for (elem in arr) {
                if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    val emoji = obj.get("emoji").asStringOrNull()
                    val count = obj.get("count")?.takeIf { !it.isJsonNull }?.asInt ?: 0
                    val userIds = mutableListOf<String>()
                    if (obj.has("userIds") && obj.get("userIds")?.isJsonArray == true) {
                        obj.getAsJsonArray("userIds").forEach { el ->
                            el.asStringOrNull()?.let { userIds.add(it) }
                        }
                    }
                    val userNames = mutableListOf<String>()
                    if (obj.has("userNames") && obj.get("userNames")?.isJsonArray == true) {
                        obj.getAsJsonArray("userNames").forEach { el ->
                            el.asStringOrNull()?.let { userNames.add(it) }
                        }
                    }
                    reactionsList.add(com.example.myapplication.data.model.Reaction(emoji = emoji, count = count, userIds = userIds, userNames = userNames))
                }
            }
        }

        return Message(
            id = msgId,
            conversationId = convId,
            sender = User(id = senderId, fullName = senderName, avatar = senderAvatar),
            content = cleanContent,
            type = messageType,
            status = MessageStatus.SENT,
            createdAt = if (rawCreatedDate.isNotEmpty()) rawCreatedDate else "Vừa xong",
            updatedAt = "",
            isRecalled = false,
            replyToMessage = replyTo,
            reactions = reactionsList
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
