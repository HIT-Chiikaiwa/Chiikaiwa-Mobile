package com.example.myapplication.ui.home.map

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.response.NearbyUserResponse
import com.example.myapplication.data.repository.MapRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class MapViewModel(application: Application) : BaseViewModel<List<NearbyUserResponse>>(application) {

    private val repository = MapRepository(application)

    private val _nearbyUsers = MutableLiveData<List<NearbyUserResponse>>()
    val nearbyUsers: LiveData<List<NearbyUserResponse>> get() = _nearbyUsers

    private val _avatarBitmaps = MutableLiveData<Map<String, Bitmap>>(emptyMap())
    val avatarBitmaps: LiveData<Map<String, Bitmap>> get() = _avatarBitmaps

    fun getNearbyUsers(lat: Double, lng: Double, radius: Double = 5.0) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.getNearbyUsers(lat, lng, radius)) {
                is Resource.Success -> {
                    val usersList = result.data?.data ?: emptyList()
                    _nearbyUsers.value = usersList
                    _uiState.value = UiState.Success(usersList)
                    fetchAvatars(usersList)
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message ?: "Không thể lấy danh sách người dùng xung quanh")
                }
            }
        }
    }

    private fun fetchAvatars(users: List<NearbyUserResponse>) {
        users.forEach { user ->
            val avatarUrl = user.avatar
            if (!avatarUrl.isNullOrEmpty() && _avatarBitmaps.value?.containsKey(user.userId) != true) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val bitmap = BitmapFactory.decodeStream(URL(avatarUrl).openStream())
                        if (bitmap != null) {
                            withContext(Dispatchers.Main) {
                                val current = _avatarBitmaps.value?.toMutableMap() ?: mutableMapOf()
                                current[user.userId] = bitmap
                                _avatarBitmaps.value = current
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}