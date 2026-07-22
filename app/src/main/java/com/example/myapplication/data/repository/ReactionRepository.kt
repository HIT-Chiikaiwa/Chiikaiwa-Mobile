package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.model.Reaction
import com.example.myapplication.data.remote.dto.response.BaseResponse
import com.example.myapplication.data.remote.dto.response.CommonResponse
import com.example.myapplication.data.remote.network.RetrofitClient
import com.example.myapplication.utils.resource.Resource

class ReactionRepository(context: Context) : BaseRepository() {
    private val api = RetrofitClient.create(context)

    suspend fun pinMessage(messageId: Long): Resource<BaseResponse<CommonResponse>> {
        return safeApiCall { api.pinMessage(messageId) }
    }

    suspend fun unpinMessage(messageId: Long): Resource<BaseResponse<CommonResponse>> {
        return safeApiCall { api.unpinMessage(messageId) }
    }

    suspend fun getPinnedMessages(conversationId: Long): Resource<BaseResponse<List<Message>>> {
        return safeApiCall { api.getPinnedMessages(conversationId) }
    }

    suspend fun addReaction(messageId: Long, emoji: String): Resource<BaseResponse<Reaction>> {
        return safeApiCall { api.addReaction(messageId, emoji) }
    }

    suspend fun removeReaction(messageId: Long): Resource<BaseResponse<CommonResponse>> {
        return safeApiCall { api.removeReaction(messageId) }
    }
}
