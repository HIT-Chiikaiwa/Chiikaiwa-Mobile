package com.example.myapplication.ui.home.chat.chatroom

import com.example.myapplication.data.model.Message
import com.example.myapplication.data.model.MessageType
import com.example.myapplication.utils.BookingMessageHelper
import com.google.gson.Gson
import com.google.gson.JsonObject

class ChatBookingManager {

    private val bookingStatusOverrides = mutableMapOf<String, Pair<String, String?>>()
    private val bookingRatingOverrides = mutableMapOf<String, Int>()
    private val gson = Gson()

    private companion object {
        val TERMINAL_STATUSES = setOf("CANCELLED", "REJECTED", "COMPLETED")
    }

    fun getOverrideStatus(bookingId: String): String? = bookingStatusOverrides[bookingId]?.first

    fun clearBookingOverride(bookingId: String) {
        bookingStatusOverrides.remove(bookingId)
    }

    fun updateBookingMessageRating(messages: MutableList<Message>, bookingId: String, score: Int): Boolean {
        bookingRatingOverrides[bookingId] = score
        var updated = false
        for (i in messages.indices) {
            if (!messages[i].content.contains(bookingId)) continue
            try {
                val jsonObj = gson.fromJson(messages[i].content, JsonObject::class.java)
                jsonObj.addProperty("hasRated", true)
                jsonObj.addProperty("myRating", score)
                messages[i] = messages[i].copy(content = gson.toJson(jsonObj))
                updated = true
            } catch (_: Exception) {}
        }
        return updated
    }

    fun updateBookingMessageStatus(
        messages: MutableList<Message>,
        bookingId: String,
        newStatus: String,
        reason: String? = null
    ): Boolean {
        bookingStatusOverrides[bookingId] = Pair(newStatus, reason)
        var updated = false
        for (i in messages.indices) {
            val msg = messages[i]
            val bIdInMsg = try {
                gson.fromJson(msg.content, JsonObject::class.java)
                    ?.get("bookingId")?.takeIf { !it.isJsonNull }?.asString
            } catch (_: Exception) { null }

            if (msg.content.contains(bookingId) || bIdInMsg == bookingId) {
                try {
                    val jsonObj = gson.fromJson(msg.content, JsonObject::class.java)
                    jsonObj.addProperty("status", newStatus)
                    if (!reason.isNullOrEmpty()) jsonObj.addProperty("cancelReason", reason)
                    messages[i] = msg.copy(content = gson.toJson(jsonObj))
                    updated = true
                } catch (_: Exception) {}
            }
        }
        return updated
    }

    fun processIncomingSystemMessage(messages: MutableList<Message>, incomingMsg: Message) {
        if (incomingMsg.type != MessageType.SYSTEM) return
        val (newStatus, reason) = BookingMessageHelper.extractStatusFromSystemMessage(incomingMsg.content) ?: return

        val targetBookingId = try {
            gson.fromJson(incomingMsg.content, JsonObject::class.java)
                ?.get("bookingId")?.takeIf { !it.isJsonNull }?.asString
        } catch (_: Exception) { null }

        if (!targetBookingId.isNullOrEmpty()) {
            updateBookingMessageStatus(messages, targetBookingId, newStatus, reason)
            return
        }

        val activeBookingIndices = messages.indices.filter { i ->
            val msg = messages[i]
            if (msg.type != MessageType.BOOKING && !BookingMessageHelper.isBookingMessage(msg.content)) return@filter false
            val status = try {
                gson.fromJson(msg.content, JsonObject::class.java)
                    ?.get("status")?.takeIf { !it.isJsonNull }?.asString
            } catch (_: Exception) { null }
            status !in TERMINAL_STATUSES
        }

        if (activeBookingIndices.size == 1) {
            val idx = activeBookingIndices[0]
            val m = messages[idx]
            try {
                val jsonObj = gson.fromJson(m.content, JsonObject::class.java)
                jsonObj.addProperty("status", newStatus)
                if (!reason.isNullOrEmpty()) jsonObj.addProperty("cancelReason", reason)
                val bId = jsonObj.get("bookingId")?.takeIf { !it.isJsonNull }?.asString
                if (!bId.isNullOrEmpty()) bookingStatusOverrides[bId] = Pair(newStatus, reason)
                messages[idx] = m.copy(content = gson.toJson(jsonObj))
            } catch (_: Exception) {}
        }
    }

    fun processMessageList(rawList: List<Message>): List<Message> {
        return rawList
            .filterNot { (it.type == MessageType.SYSTEM || it.type == MessageType.TEXT) && BookingMessageHelper.extractStatusFromSystemMessage(it.content) != null }
            .map { msg -> applyOverrides(msg) }
    }

    private fun applyOverrides(msg: Message): Message {
        if (msg.type != MessageType.BOOKING && !BookingMessageHelper.isBookingMessage(msg.content)) return msg
        return try {
            val jsonObj = gson.fromJson(msg.content, JsonObject::class.java)
            val bId = jsonObj.get("bookingId")?.takeIf { !it.isJsonNull }?.asString ?: return msg
            var modified = false

            bookingStatusOverrides[bId]?.let { (status, reason) ->
                jsonObj.addProperty("status", status)
                if (!reason.isNullOrEmpty()) jsonObj.addProperty("cancelReason", reason)
                modified = true
            }

            bookingRatingOverrides[bId]?.let { score ->
                jsonObj.addProperty("hasRated", true)
                jsonObj.addProperty("myRating", score)
                modified = true
            }

            if (modified) msg.copy(content = gson.toJson(jsonObj)) else msg
        } catch (_: Exception) { msg }
    }
}
