package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.remote.dto.request.ReplyRequest
import com.example.myapplication.data.remote.dto.response.BaseResponse
import com.example.myapplication.data.remote.network.RetrofitClient
import com.example.myapplication.utils.resource.Resource

class MessageRepository(context: Context) : BaseRepository() {
    private val api = RetrofitClient.create(context)

    suspend fun getChatHistory(conversationId: Long): Resource<BaseResponse<List<Message>>> {
        return safeApiCall { api.getChatHistory(conversationId) }
    }

    suspend fun searchMessages(conversationId: Long, keyword: String): Resource<BaseResponse<List<Message>>> {
        return safeApiCall { api.searchMessages(conversationId, keyword) }
    }

    suspend fun replyMessage(messageId: Long, content: String): Resource<BaseResponse<Message>> {
        return safeApiCall { api.replyMessage(messageId, ReplyRequest(content)) }
    }

    suspend fun forwardMessage(messageId: Long, targetConversationId: Long): Resource<BaseResponse<Message>> {
        return safeApiCall { api.forwardMessage(messageId, targetConversationId) }
    }
}