package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.remote.dto.response.BaseResponse
import com.example.myapplication.data.remote.dto.response.MessageResponse
import com.example.myapplication.data.remote.dto.response.PageResponse
import com.example.myapplication.data.remote.network.RetrofitClient
import com.example.myapplication.utils.resource.Resource

class MessageRepository(context: Context) : BaseRepository() {
    private val api = RetrofitClient.create(context)

    suspend fun getMessages(
        conversationId: String,
        page: Int = 0,
        size: Int = 20,
        sort: String? = "createdDate,desc"
    ): Resource<BaseResponse<PageResponse<MessageResponse>>> = safeApiCall {
        api.getMessages(conversationId, page, size, sort)
    }

    suspend fun searchMessages(
        conversationId: String,
        keyword: String,
        page: Int = 0,
        size: Int = 20,
        sort: String? = "createdDate,desc"
    ): Resource<BaseResponse<PageResponse<MessageResponse>>> = safeApiCall {
        api.searchMessages(conversationId, keyword, page, size, sort)
    }

    suspend fun recallMessage(messageId: String): Resource<BaseResponse<Any>> = safeApiCall {
        api.recallMessage(messageId)
    }

    suspend fun deleteMessage(messageId: String): Resource<BaseResponse<Any>> = safeApiCall {
        api.deleteMessage(messageId)
    }
}