package com.example.myapplication.data.repository

import android.content.Context
import com.example.myapplication.data.remote.dto.request.*
import com.example.myapplication.data.remote.dto.response.BaseResponse
import com.example.myapplication.data.remote.dto.response.CommonResponse
import com.example.myapplication.data.remote.dto.response.UserDto
import com.example.myapplication.data.remote.dto.response.SubjectDto
import com.example.myapplication.data.remote.network.RetrofitClient
import com.example.myapplication.utils.resource.Resource
import okhttp3.MultipartBody
import com.example.myapplication.data.remote.api.ApiService

class ProfileRepository(context: Context) : BaseRepository() {
    private val api = RetrofitClient.create(context)

    suspend fun getProfile(userId: String): Resource<BaseResponse<UserDto>> {
        return safeApiCall { api.getProfile(userId) }
    }

    suspend fun toggleBuddyStatus(userId: String, buddyActive: Boolean): Resource<BaseResponse<UserDto>> {
        return safeApiCall { api.toggleBuddyStatus(userId, ToggleBuddyStatusRequest(buddyActive)) }
    }

    suspend fun changePassword(userId: String, request: ChangePasswordRequest): Resource<BaseResponse<CommonResponse>> {
        return safeApiCall { api.changePassword(userId, request) }
    }

    suspend fun getSubjects(userId: String, type: String? = null): Resource<BaseResponse<List<SubjectDto>>> {
        return safeApiCall { api.getSubjects(userId, type) }
    }

    suspend fun addSubject(userId: String, request: AddSubjectRequest): Resource<BaseResponse<SubjectDto>> {
        return safeApiCall { api.addSubject(userId, request) }
    }

    suspend fun deleteSubject(userId: String, subjectId: String): Resource<BaseResponse<CommonResponse>> {
        return safeApiCall { api.deleteSubject(userId, subjectId) }
    }

    suspend fun deleteAccount(userId: String): Resource<BaseResponse<CommonResponse>> {
        return safeApiCall { api.deleteAccount(userId) }
    }
}
