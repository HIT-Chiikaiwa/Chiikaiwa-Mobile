package com.example.myapplication.data.repository.social
import com.example.myapplication.data.repository.BaseRepository

import android.content.Context
import com.example.myapplication.data.remote.dto.response.*
import com.example.myapplication.data.remote.network.RetrofitClient
import com.example.myapplication.utils.resource.Resource

class FriendRepository(context: Context) : BaseRepository() {
    private val api = RetrofitClient.getApiService(context)

    suspend fun searchUsers(keyword: String): Resource<BaseResponse<List<UserSearchDto>>> {
        return safeApiCall { api.searchUsers(keyword) }
    }

    suspend fun sendFriendRequest(targetUserId: String): Resource<BaseResponse<FriendActionResponse>> {
        return safeApiCall { api.sendFriendRequest(targetUserId) }
    }

    suspend fun acceptFriendRequest(requestId: String): Resource<BaseResponse<FriendActionResponse>> {
        return safeApiCall { api.acceptFriendRequest(requestId) }
    }

    suspend fun rejectFriendRequest(requestId: String): Resource<BaseResponse<FriendActionResponse>> {
        return safeApiCall { api.rejectFriendRequest(requestId) }
    }

    suspend fun getFriends(page: Int = 0, size: Int = 20): Resource<BaseResponse<PageResponse<FriendDto>>> {
        return safeApiCall { api.getFriends(page, size) }
    }

    suspend fun searchFriends(keyword: String, page: Int = 0, size: Int = 20): Resource<BaseResponse<PageResponse<FriendDto>>> {
        return safeApiCall { api.searchFriends(keyword, page, size) }
    }

    suspend fun getPendingFriendRequests(page: Int = 0, size: Int = 20): Resource<BaseResponse<PageResponse<FriendDto>>> {
        return safeApiCall { api.getPendingFriendRequests(page, size) }
    }

    suspend fun unfriend(friendId: String): Resource<BaseResponse<FriendActionResponse>> {
        return safeApiCall { api.unfriend(friendId) }
    }
}
