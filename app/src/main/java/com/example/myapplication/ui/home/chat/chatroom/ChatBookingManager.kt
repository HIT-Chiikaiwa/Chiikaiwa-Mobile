package com.example.myapplication.ui.home.chat.chatroom

import android.util.Log
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.model.MessageType
import com.example.myapplication.utils.BookingMessageHelper
import com.google.gson.Gson
import com.google.gson.JsonObject

class ChatBookingManager {

    private val bookingStatusOverrides = mutableMapOf<String, Pair<String, String?>>()
    private val bookingRatingOverrides = mutableMapOf<String, Int>()
    private val gson = Gson()

    fun getOverrideStatus(bookingId: String): String? {
        return bookingStatusOverrides[bookingId]?.first
    }

    fun updateBookingMessageRating(
        messages: MutableList<Message>,
        bookingId: String,
        score: Int
    ): Boolean {
        bookingRatingOverrides[bookingId] = score
        var updated = false
        for (i in messages.indices) {
            val msg = messages[i]
            if (msg.content.contains(bookingId)) {
                try {
                    val jsonObj = gson.fromJson(msg.content, JsonObject::class.java)
                    jsonObj.addProperty("hasRated", true)
                    jsonObj.addProperty("myRating", score)
                    messages[i] = msg.copy(content = gson.toJson(jsonObj))
                    updated = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return updated
    }

    fun updateBookingMessageStatus(
        messages: MutableList<Message>,
        bookingId: String,
        newStatus: String,
        reason: String? = null
    ): Boolean {
        Log.d("CHAT_BOOKING_DEBUG", "[UPDATE_STATUS_CALL] bookingId=$bookingId newStatus=$newStatus reason=$reason messagesCount=${messages.size}")
        bookingStatusOverrides[bookingId] = Pair(newStatus, reason)
        var updated = false
        for (i in messages.indices) {
            val msg = messages[i]
            val bIdInMsg = try {
                gson.fromJson(msg.content, JsonObject::class.java)?.get("bookingId")?.takeIf { !it.isJsonNull }?.asString
            } catch (e: Exception) { null }

            if (msg.content.contains(bookingId) || bIdInMsg == bookingId) {
                try {
                    val jsonObj = gson.fromJson(msg.content, JsonObject::class.java)
                    jsonObj.addProperty("status", newStatus)
                    if (!reason.isNullOrEmpty()) {
                        jsonObj.addProperty("cancelReason", reason)
                    }
                    messages[i] = msg.copy(content = gson.toJson(jsonObj))
                    updated = true
                    Log.d("CHAT_BOOKING_DEBUG", "[UPDATE_STATUS_SUCCESS] Updated message index $i to status=$newStatus")
                } catch (e: Exception) {
                    Log.e("CHAT_BOOKING_DEBUG", "[UPDATE_STATUS_ERR] Failed to update message index $i", e)
                }
            }
        }
        return updated
    }

    fun processIncomingSystemMessage(messages: MutableList<Message>, incomingMsg: Message) {
        if (incomingMsg.type != MessageType.SYSTEM) return
        val statusPair = BookingMessageHelper.extractStatusFromSystemMessage(incomingMsg.content) ?: return
        val (newStatus, reason) = statusPair

        var targetBookingId: String? = null
        try {
            val sysJson = gson.fromJson(incomingMsg.content, JsonObject::class.java)
            targetBookingId = sysJson.get("bookingId")?.takeIf { !it.isJsonNull }?.asString
        } catch (e: Exception) {
            targetBookingId = null
        }

        if (!targetBookingId.isNullOrEmpty()) {
            updateBookingMessageStatus(messages, targetBookingId, newStatus, reason)
        } else {
            val activeBookingIndices = messages.indices.filter { i ->
                val msg = messages[i]
                if (msg.type == MessageType.BOOKING || BookingMessageHelper.isBookingMessage(msg.content)) {
                    val status = try {
                        gson.fromJson(msg.content, JsonObject::class.java)?.get("status")?.takeIf { !it.isJsonNull }?.asString
                    } catch (e: Exception) { null }
                    status != "CANCELLED" && status != "REJECTED" && status != "COMPLETED"
                } else false
            }

            if (activeBookingIndices.size == 1) {
                val targetIndex = activeBookingIndices[0]
                val m = messages[targetIndex]
                try {
                    val jsonObj = gson.fromJson(m.content, JsonObject::class.java)
                    jsonObj.addProperty("status", newStatus)
                    if (!reason.isNullOrEmpty()) {
                        jsonObj.addProperty("cancelReason", reason)
                    }
                    val bId = jsonObj.get("bookingId")?.takeIf { !it.isJsonNull }?.asString
                    if (!bId.isNullOrEmpty()) {
                        bookingStatusOverrides[bId] = Pair(newStatus, reason)
                    }
                    messages[targetIndex] = m.copy(content = gson.toJson(jsonObj))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun processMessageList(rawList: List<Message>): List<Message> {
        val filteredList = rawList.filterNot { msg ->
            msg.type == MessageType.SYSTEM && BookingMessageHelper.extractStatusFromSystemMessage(msg.content) != null
        }

        return filteredList.map { msg ->
            if (msg.type == MessageType.BOOKING || BookingMessageHelper.isBookingMessage(msg.content)) {
                try {
                    val jsonObj = gson.fromJson(msg.content, JsonObject::class.java)
                    val bId = jsonObj.get("bookingId")?.takeIf { !it.isJsonNull }?.asString

                    val override = if (!bId.isNullOrEmpty() && bookingStatusOverrides.containsKey(bId)) {
                        bookingStatusOverrides[bId]
                    } else null

                    if (!bId.isNullOrEmpty() && bookingRatingOverrides.containsKey(bId)) {
                        jsonObj.addProperty("hasRated", true)
                        jsonObj.addProperty("myRating", bookingRatingOverrides[bId])
                    }

                    if (override != null) {
                        val (overrideStatus, overrideReason) = override
                        jsonObj.addProperty("status", overrideStatus)
                        if (!overrideReason.isNullOrEmpty()) {
                            jsonObj.addProperty("cancelReason", overrideReason)
                        }
                        msg.copy(content = gson.toJson(jsonObj))
                    } else if (!bId.isNullOrEmpty() && bookingRatingOverrides.containsKey(bId)) {
                        msg.copy(content = gson.toJson(jsonObj))
                    } else msg
                } catch (e: Exception) {
                    msg
                }
            } else msg
        }
    }
}
