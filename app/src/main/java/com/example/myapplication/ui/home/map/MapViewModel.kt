package com.example.myapplication.ui.home.map

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.model.response.NearbyUserResponse
import com.example.myapplication.data.repository.MapRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.Resource
import kotlinx.coroutines.launch

class MapViewModel(application: Application) : BaseViewModel<List<NearbyUserResponse>>(application) {

    private val repository = MapRepository(application)

    private val _nearbyUsers = MutableLiveData<List<NearbyUserResponse>>()
    val nearbyUsers: LiveData<List<NearbyUserResponse>> get() = _nearbyUsers

    fun getNearbyUsers(lat: Double, lng: Double, radius: Double = 5.0) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            when (val result = repository.getNearbyUsers(lat, lng, radius)) {
                is Resource.Success -> {
                    val usersList = result.data?.data ?: emptyList()
                    _nearbyUsers.value = usersList
                    _uiState.value = UiState.Success(usersList)
                }
                is Resource.Error -> {
                    _uiState.value = UiState.Error(result.message ?: "Không thể lấy danh sách người dùng xung quanh")
                }
            }
        }
    }
}