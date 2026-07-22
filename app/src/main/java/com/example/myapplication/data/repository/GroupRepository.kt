package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.remote.dto.request.AddMemberRequest
import com.example.myapplication.data.remote.dto.request.UpdateGroupRequest
import com.example.myapplication.data.remote.dto.response.ActionStatusDto
import com.example.myapplication.data.remote.dto.response.BaseResponse
import com.example.myapplication.data.remote.dto.response.ConversationResponse
import com.example.myapplication.data.remote.network.RetrofitClient
import com.example.myapplication.utils.resource.Resource

class GroupRepository(context: Context) : BaseRepository() {
    private val api = RetrofitClient.create(context)

    suspend fun updateGroup(
        conversationId: String,
        groupName: String?,
        groupAvatar: String?
    ): Resource<BaseResponse<ConversationResponse>> {
        return safeApiCall { api.updateGroup(conversationId, UpdateGroupRequest(groupName, groupAvatar)) }
    }

    suspend fun addMembers(
        conversationId: String,
        memberIds: List<String>
    ): Resource<BaseResponse<ActionStatusDto>> {
        return safeApiCall { api.addMembers(conversationId, AddMemberRequest(memberIds)) }
    }

    suspend fun removeMember(
        conversationId: String,
        userId: String
    ): Resource<BaseResponse<ActionStatusDto>> {
        return safeApiCall { api.removeMember(conversationId, userId) }
    }

    suspend fun dissolveGroup(conversationId: String): Resource<BaseResponse<ActionStatusDto>> {
        return safeApiCall { api.dissolveGroup(conversationId) }
    }

    suspend fun transferOwnership(
        conversationId: String,
        newOwnerId: String
    ): Resource<BaseResponse<ActionStatusDto>> {
        return safeApiCall { api.transferOwnership(conversationId, newOwnerId) }
    }
}
