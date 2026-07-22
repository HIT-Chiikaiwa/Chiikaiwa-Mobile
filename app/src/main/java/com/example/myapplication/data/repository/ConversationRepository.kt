package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.remote.dto.request.CreateGroupRequest
import com.example.myapplication.data.remote.dto.request.DirectChatRequest
import com.example.myapplication.data.remote.dto.request.ScheduleInviteRequest
import com.example.myapplication.data.remote.dto.response.BaseResponse
import com.example.myapplication.data.remote.dto.response.ConversationResponse
import com.example.myapplication.data.remote.dto.response.PageResponse
import com.example.myapplication.data.remote.network.RetrofitClient
import com.example.myapplication.utils.resource.Resource
import okhttp3.MultipartBody

class ConversationRepository(context: Context) : BaseRepository() {
    private val api = RetrofitClient.create(context)

    suspend fun getConversations(
        page: Int = 0,
        size: Int = 20,
        sort: String? = null
    ): Resource<BaseResponse<PageResponse<ConversationResponse>>> {
        return safeApiCall { api.getConversations(page, size, sort) }
    }

    suspend fun searchConversations(
        keyword: String,
        page: Int = 0,
        size: Int = 20,
        sort: String? = null
    ): Resource<BaseResponse<PageResponse<ConversationResponse>>> {
        return safeApiCall { api.searchConversations(keyword, page, size, sort) }
    }

    suspend fun createOrGetDirectConversation(targetUserId: String): Resource<BaseResponse<ConversationResponse>> {
        return safeApiCall { api.createOrGetDirectConversation(DirectChatRequest(targetUserId)) }
    }

    suspend fun createGroup(
        groupName: String,
        groupAvatar: String?,
        memberIds: List<String>
    ): Resource<BaseResponse<ConversationResponse>> {
        return safeApiCall { api.createGroup(CreateGroupRequest(groupName, groupAvatar, memberIds)) }
    }

    suspend fun uploadAttachment(
        conversationId: String,
        file: MultipartBody.Part
    ): Resource<BaseResponse<Any>> {
        return safeApiCall { api.uploadAttachment(conversationId, file) }
    }

    suspend fun scheduleInvite(
        conversationId: String,
        subject: String,
        location: String?,
        scheduledAt: String,
        duration: Int = 60,
        note: String?
    ): Resource<BaseResponse<Any>> {
        return safeApiCall {
            api.scheduleInvite(
                conversationId,
                ScheduleInviteRequest(subject, location, scheduledAt, duration, note)
            )
        }
    }
}
