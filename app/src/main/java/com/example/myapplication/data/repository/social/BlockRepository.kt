package com.example.myapplication.data.repository.social
import com.example.myapplication.data.repository.BaseRepository

import android.content.Context
import com.example.myapplication.data.remote.dto.response.*
import com.example.myapplication.data.remote.network.RetrofitClient
import com.example.myapplication.utils.resource.Resource

class BlockRepository(context: Context) : BaseRepository() {
    private val api = RetrofitClient.create(context)

    suspend fun blockUser(userId: String): Resource<BaseResponse<CommonResponse>> {
        return safeApiCall { api.blockUser(userId) }
    }

    suspend fun unblockUser(userId: String): Resource<BaseResponse<CommonResponse>> {
        return safeApiCall { api.unblockUser(userId) }
    }

    suspend fun getBlockedUsers(): Resource<BaseResponse<List<BlockedUserDto>>> {
        return safeApiCall { api.getBlockedUsers() }
    }
}
