package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.model.Conversation
import com.example.myapplication.data.remote.dto.request.CreateGroupRequest
import com.example.myapplication.data.remote.dto.response.BaseResponse
import com.example.myapplication.data.remote.network.RetrofitClient
import com.example.myapplication.utils.resource.Resource

class ConversationRepository(context: Context) : BaseRepository() {
    private val api = RetrofitClient.create(context)

    suspend fun getConversations(): Resource<BaseResponse<List<Conversation>>> {
        return safeApiCall { api.getConversations() }
    }

    suspend fun getOrCreateDirectConversation(receiverId: Long): Resource<BaseResponse<Conversation>> {
        return safeApiCall { api.getOrCreateDirectConversation(receiverId) }
    }

    suspend fun createGroup(name: String, avatar: String?, memberIds: List<Long>): Resource<BaseResponse<Conversation>> {
        return safeApiCall { api.createGroup(CreateGroupRequest(name, avatar, memberIds)) }
    }

    suspend fun searchConversations(keyword: String): Resource<BaseResponse<List<Conversation>>> {
        return safeApiCall { api.searchConversations(keyword) }
    }
}
