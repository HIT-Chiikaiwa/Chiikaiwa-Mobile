package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.remote.dto.response.ActionStatusDto
import com.example.myapplication.data.remote.dto.response.BaseResponse
import com.example.myapplication.data.remote.dto.response.MessageResponse
import com.example.myapplication.data.remote.network.RetrofitClient
import com.example.myapplication.utils.resource.Resource

class ReactionRepository(context: Context) : BaseRepository() {
    private val api = RetrofitClient.create(context)

    suspend fun pinMessage(messageId: String): Resource<BaseResponse<ActionStatusDto>> {
        return safeApiCall { api.pinMessage(messageId) }
    }

    suspend fun unpinMessage(messageId: String): Resource<BaseResponse<ActionStatusDto>> {
        return safeApiCall { api.unpinMessage(messageId) }
    }

    suspend fun getPinnedMessages(conversationId: String): Resource<BaseResponse<List<MessageResponse>>> {
        return safeApiCall { api.getPinnedMessages(conversationId) }
    }

    suspend fun addReaction(messageId: String, emoji: String): Resource<BaseResponse<ActionStatusDto>> {
        return safeApiCall { api.addReaction(messageId, emoji) }
    }

    suspend fun removeReaction(messageId: String): Resource<BaseResponse<ActionStatusDto>> {
        return safeApiCall { api.removeReaction(messageId) }
    }
}
