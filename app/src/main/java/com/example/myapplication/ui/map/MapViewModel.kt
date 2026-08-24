package com.example.myapplication.ui.map

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.PreferenceManager
import com.example.myapplication.data.remote.dto.response.NearbyUserResponse
import com.example.myapplication.data.repository.map.MapRepository
import com.example.myapplication.data.repository.profile.ProfileRepository
import com.example.myapplication.ui.base.BaseViewModel
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.utils.resource.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class MapViewModel(application: Application) : BaseViewModel<List<NearbyUserResponse>>(application) {

    private val mapRepository = MapRepository(application)
    private val profileRepository = ProfileRepository(application)
    private val preferenceManager = PreferenceManager(application)

    val currentUserId: String get() = preferenceManager.getUserId() ?: ""

    private val _nearbyUsers = MutableStateFlow<List<NearbyUserResponse>>(emptyList())
    val nearbyUsers: StateFlow<List<NearbyUserResponse>> get() = _nearbyUsers

    private val _avatarBitmaps = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val avatarBitmaps: StateFlow<Map<String, Bitmap>> get() = _avatarBitmaps

    private val _currentUserAvatar = MutableStateFlow<String?>(null)
    val currentUserAvatar: StateFlow<String?> get() = _currentUserAvatar

    private val fetchingUserIds = ConcurrentHashMap.newKeySet<String>()

    fun loadCurrentUserAvatar() {
        val userId = preferenceManager.getUserId() ?: return
        viewModelScope.launch {
            when (val result = profileRepository.getProfile(userId)) {
                is Resource.Success -> {
                    _currentUserAvatar.value = result.data.data.avatar
                }
                else -> {}
            }
        }
    }

    fun getNearbyUsers(lat: Double, lng: Double, radiusKm: Double = 6.0) {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _uiState.value = UiState.Loading

            mapRepository.updateLocation(lat, lng)

            when (val result = mapRepository.getNearbyUsers(lat, lng, radiusKm)) {
                is Resource.Success -> {
                    val remoteUsers = result.data?.data ?: emptyList()
                    val filtered = withContext(Dispatchers.Default) {
                        remoteUsers.mapNotNull { user ->
                            try {
                                val distanceResults = FloatArray(1)
                                Location.distanceBetween(lat, lng, user.latitude, user.longitude, distanceResults)
                                val dist = distanceResults[0] / 1000.0
                                if (dist <= radiusKm) user.copy(distanceKm = dist) else null
                            } catch (e: Exception) {
                                user
                            }
                        }
                    }

                    val elapsedTime = System.currentTimeMillis() - startTime
                    val minScanDuration = 5000L
                    if (elapsedTime < minScanDuration) {
                        kotlinx.coroutines.delay(minScanDuration - elapsedTime)
                    }

                    _nearbyUsers.value = filtered
                    _uiState.value = UiState.Success(filtered)
                    fetchAvatars(filtered)
                }
                is Resource.Error -> {
                    val elapsedTime = System.currentTimeMillis() - startTime
                    val minScanDuration = 5000L
                    if (elapsedTime < minScanDuration) {
                        kotlinx.coroutines.delay(minScanDuration - elapsedTime)
                    }
                    _uiState.value = UiState.Error(result.message ?: "Lỗi tải dữ liệu vị trí")
                }
            }
        }
    }

    private fun fetchAvatars(users: List<NearbyUserResponse>) {
        val context = getApplication<Application>()
        val currentMap = _avatarBitmaps.value

        val newUsersToFetch = users.filter { user ->
            !user.avatar.isNullOrEmpty() &&
            !currentMap.containsKey(user.userId) &&
            fetchingUserIds.add(user.userId)
        }

        if (newUsersToFetch.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val newlyLoaded = ConcurrentHashMap<String, Bitmap>()
            val jobs = newUsersToFetch.map { user ->
                launch {
                    try {
                        val url = user.avatar ?: return@launch
                        val bitmap = if (url.startsWith("http://") || url.startsWith("https://")) {
                            com.bumptech.glide.Glide.with(context)
                                .asBitmap()
                                .load(url)
                                .submit()
                                .get()
                        } else {
                            val resId = context.resources.getIdentifier(url, "drawable", context.packageName)
                            if (resId != 0) BitmapFactory.decodeResource(context.resources, resId) else null
                        }
                        if (bitmap != null) {
                            newlyLoaded[user.userId] = bitmap
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        fetchingUserIds.remove(user.userId)
                    }
                }
            }
            jobs.forEach { it.join() }

            if (newlyLoaded.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    _avatarBitmaps.value = _avatarBitmaps.value + newlyLoaded
                }
            }
        }
    }
}