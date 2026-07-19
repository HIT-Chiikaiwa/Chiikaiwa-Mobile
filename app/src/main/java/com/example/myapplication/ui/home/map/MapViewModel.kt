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

    private val fetchingUserIds = mutableSetOf<String>()

    fun getNearbyUsers(lat: Double, lng: Double, radius: Double = 6.0) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            val filterAndDistance = { users: List<NearbyUserResponse> ->
                users.mapNotNull { user ->
                    val distanceResults = FloatArray(1)
                    try {
                        android.location.Location.distanceBetween(lat, lng, user.latitude, user.longitude, distanceResults)
                        val distanceKm = distanceResults[0] / 1000.0
                        if (distanceKm <= radius) {
                            user.copy(distanceKm = distanceKm)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        user
                    }
                }
            }

            when (val result = repository.getNearbyUsers(lat, lng, radius)) {
                is Resource.Success -> {
                    val remoteUsers = result.data?.data ?: emptyList()
                    val fakeUsers = FakeData.getFakeNearbyUsers(lat, lng)
                    val combinedList = filterAndDistance(remoteUsers) + filterAndDistance(fakeUsers)
                    _nearbyUsers.value = combinedList
                    _uiState.value = UiState.Success(combinedList)
                    fetchAvatars(combinedList)
                }
                is Resource.Error -> {
                    val fakeUsers = FakeData.getFakeNearbyUsers(lat, lng)
                    val filteredFake = filterAndDistance(fakeUsers)
                    _nearbyUsers.value = filteredFake
                    _uiState.value = UiState.Success(filteredFake)
                    fetchAvatars(filteredFake)
                }
            }
        }
    }

    private fun fetchAvatars(users: List<NearbyUserResponse>) {
        val context = getApplication<Application>()
        val currentBitmaps = _avatarBitmaps.value ?: emptyMap()
        users.forEach { user ->
            val avatarUrl = user.avatar
            val userId = user.userId
            if (!avatarUrl.isNullOrEmpty() && !currentBitmaps.containsKey(userId)) {
                synchronized(fetchingUserIds) {
                    if (fetchingUserIds.contains(userId)) return@forEach
                    fetchingUserIds.add(userId)
                }
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val bitmap = if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://")) {
                            BitmapFactory.decodeStream(URL(avatarUrl).openStream())
                        } else {
                            val resId = context.resources.getIdentifier(avatarUrl, "drawable", context.packageName)
                            if (resId != 0) {
                                BitmapFactory.decodeResource(context.resources, resId)
                            } else {
                                null
                            }
                        }
                        if (bitmap != null) {
                            withContext(Dispatchers.Main) {
                                val current = _avatarBitmaps.value?.toMutableMap() ?: mutableMapOf()
                                current[userId] = bitmap
                                _avatarBitmaps.value = current
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        synchronized(fetchingUserIds) {
                            fetchingUserIds.remove(userId)
                        }
                    }
                }
            }
        }
    }
}