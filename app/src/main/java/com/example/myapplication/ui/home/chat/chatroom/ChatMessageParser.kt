package com.example.myapplication.ui.home.chat.chatroom

import com.example.myapplication.data.model.Message
import com.google.gson.Gson
import com.google.gson.JsonObject

class ChatMessageParser(private val gson: Gson = Gson()) {

    fun parseWsMessage(body: String, activeConversationId: String): Message? {
        val jsonObj = gson.fromJson(body, JsonObject::class.java) ?: return null
        val data = if (jsonObj.has("data") && jsonObj.get("data").isJsonObject) {
            jsonObj.getAsJsonObject("data")
        } else jsonObj

        val content = data.get("content")?.asString ?: data.get("text")?.asString ?: ""
        if (content.isEmpty()) return null

        val msgId = data.get("id")?.asString ?: data.get("messageId")?.asString ?: System.currentTimeMillis().toString()
        val senderId = data.get("senderId")?.asString
            ?: data.getAsJsonObject("sender")?.get("id")?.asString ?: ""
        val senderName = data.get("senderName")?.asString
            ?: data.getAsJsonObject("sender")?.get("fullName")?.asString ?: "Người dùng"
        val convId = data.get("conversationId")?.asString ?: activeConversationId

        val rawType = data.get("messageType")?.asString 
            ?: data.get("type")?.asString 
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
            createdAt = "Vừa xong",
            updatedAt = "",
            isRecalled = false
        )
    }

    fun extractImageUrl(data: Any?): String {
        return when (data) {
            is String -> data
            is Map<*, *> -> data["url"]?.toString() ?: data["fileUrl"]?.toString() ?: data["path"]?.toString() ?: ""
            is JsonObject -> data.get("url")?.asString ?: data.get("fileUrl")?.asString ?: data.get("path")?.asString ?: ""
            else -> data?.toString() ?: ""
        }
    }
}
