package com.example.myapplication.data.repository.profile
import com.example.myapplication.data.repository.BaseRepository

import android.content.Context
import com.example.myapplication.data.remote.dto.request.*
import com.example.myapplication.data.remote.dto.response.BaseResponse
import com.example.myapplication.data.remote.dto.response.CommonResponse
import com.example.myapplication.data.remote.dto.response.UserDto
import com.example.myapplication.data.remote.dto.response.SubjectDto
import com.example.myapplication.data.remote.dto.response.OnlineStatusDto
import com.example.myapplication.data.remote.network.RetrofitClient
import com.example.myapplication.utils.resource.Resource
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MultipartBody

class ProfileRepository(context: Context) : BaseRepository() {
    private val api = RetrofitClient.getApiService(context)

    suspend fun getCurrentUser(): Resource<BaseResponse<UserDto>> {
        return safeApiCall { api.getCurrentUser() }
    }

    suspend fun getProfile(userId: String): Resource<BaseResponse<UserDto>> {
        return safeApiCall { api.getProfile(userId) }
    }

    suspend fun uploadAvatar(userId: String, imageFile: java.io.File): Resource<BaseResponse<UserDto>> {
        val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
        val body = okhttp3.MultipartBody.Part.createFormData("file", imageFile.name, requestFile)
        return safeApiCall { api.uploadAvatar(userId, body) }
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

    suspend fun updateStatusTag(userId: String, request: UpdateStatusTagRequest): Resource<BaseResponse<UserDto>> {
        return safeApiCall { api.updateStatusTag(userId, request) }
    }

    suspend fun updatePersonalInfo(userId: String, request: UpdatePersonalInfoRequest): Resource<BaseResponse<UserDto>> {
        return safeApiCall { api.updatePersonalInfo(userId, request) }
    }

    suspend fun updateAcademicInfo(userId: String, request: UpdateAcademicInfoRequest): Resource<BaseResponse<UserDto>> {
        return safeApiCall { api.updateAcademicInfo(userId, request) }
    }

    suspend fun updateProfileLocation(userId: String, request: UpdateProfileLocationRequest): Resource<BaseResponse<UserDto>> {
        return safeApiCall { api.updateProfileLocation(userId, request) }
    }

    suspend fun getUserOnlineStatus(userId: String): Resource<BaseResponse<OnlineStatusDto>> {
        return safeApiCall { api.getUserOnlineStatus(userId) }
    }
}
