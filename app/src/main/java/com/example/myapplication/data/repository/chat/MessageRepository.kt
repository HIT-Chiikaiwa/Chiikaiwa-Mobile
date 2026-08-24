package com.example.myapplication.data.repository.chat
import com.example.myapplication.data.repository.BaseRepository

import android.content.Context
import com.example.myapplication.data.remote.dto.response.BaseResponse
import com.example.myapplication.data.remote.dto.response.MessageResponse
import com.example.myapplication.data.remote.dto.response.PageResponse
import com.example.myapplication.data.remote.network.RetrofitClient
import com.example.myapplication.utils.resource.Resource
import okhttp3.MultipartBody

class MessageRepository(context: Context) : BaseRepository() {
    private val api = RetrofitClient.getApiService(context)

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

    suspend fun uploadImage(
        conversationId: String,
        file: MultipartBody.Part
    ): Resource<BaseResponse<Any>> = safeApiCall {
        api.uploadImage(conversationId, file)
    }

    suspend fun recallMessage(messageId: String): Resource<BaseResponse<Any>> = safeApiCall {
        api.recallMessage(messageId)
    }

    suspend fun deleteMessage(messageId: String): Resource<BaseResponse<Any>> = safeApiCall {
        api.deleteMessage(messageId)
    }
}
