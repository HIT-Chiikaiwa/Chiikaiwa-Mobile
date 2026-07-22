package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.model.Conversation
import com.example.myapplication.data.remote.dto.request.AddMemberRequest
import com.example.myapplication.data.remote.dto.request.UpdateGroupRequest
import com.example.myapplication.data.remote.dto.response.BaseResponse
import com.example.myapplication.data.remote.dto.response.CommonResponse
import com.example.myapplication.data.remote.network.RetrofitClient
import com.example.myapplication.utils.resource.Resource

class GroupRepository(context: Context) : BaseRepository() {
    private val api = RetrofitClient.create(context)

    suspend fun updateGroup(conversationId: Long, name: String?, avatar: String?): Resource<BaseResponse<Conversation>> {
        return safeApiCall { api.updateGroup(conversationId, UpdateGroupRequest(name, avatar)) }
    }

    suspend fun addMembers(conversationId: Long, memberIds: List<Long>): Resource<BaseResponse<Conversation>> {
        return safeApiCall { api.addMembers(conversationId, AddMemberRequest(memberIds)) }
    }

    suspend fun removeMember(conversationId: Long, userId: Long): Resource<BaseResponse<CommonResponse>> {
        return safeApiCall { api.removeMember(conversationId, userId) }
    }

    suspend fun dissolveGroup(conversationId: Long): Resource<BaseResponse<CommonResponse>> {
        return safeApiCall { api.dissolveGroup(conversationId) }
    }

    suspend fun transferOwnership(conversationId: Long, newOwnerId: Long): Resource<BaseResponse<Conversation>> {
        return safeApiCall { api.transferOwnership(conversationId, newOwnerId) }
    }
}
