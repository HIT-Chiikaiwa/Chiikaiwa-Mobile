package com.example.myapplication.ui.home.map

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import com.example.myapplication.R
import com.example.myapplication.data.remote.dto.response.NearbyUserResponse
import com.example.myapplication.databinding.DialogUserInfoBinding
import com.example.myapplication.ui.home.chat.chatroom.ChatActivity
import com.example.myapplication.utils.extension.observeState
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.geojson.Point

class MapManager(
    private val fragment: MapFragment,
    private val map: MapLibreMap,
    private val viewModel: MapViewModel
) {
    private val context = fragment.requireContext()
    private val avatarBitmapCache = HashMap<String, Bitmap>()
    private val radarRenderer = RadarRenderer(map)
    private val markerSize by lazy { context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._24sdp) }
    private val friendRepository by lazy { com.example.myapplication.data.repository.FriendRepository(context) }

    fun setup() {
        map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->

            BitmapFactory.decodeResource(context.resources, R.drawable.ic_marker)?.let { bitmap ->
                val scaledChicken = Bitmap.createScaledBitmap(bitmap, markerSize, markerSize, false)
                style.addImage("my_location_marker", scaledChicken)
            }

            BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_foreground)?.let { bitmap ->
                val defaultUserMarker = createMarkerBitmapFromLayout(bitmap)
                style.addImage("my_marker", defaultUserMarker)
            }

            radarRenderer.setupRadarLayers()

            style.addSource(GeoJsonSource("user-source", FeatureCollection.fromFeatures(emptyList())))
            style.addLayer(
                SymbolLayer("user-layer", "user-source").withProperties(
                    PropertyFactory.iconImage("{avatar_id}"),
                    PropertyFactory.iconAllowOverlap(true)
                )
            )

            style.addSource(GeoJsonSource("my-location-source", FeatureCollection.fromFeatures(emptyList())))
            style.addLayer(
                SymbolLayer("my-location-layer", "my-location-source").withProperties(
                    PropertyFactory.iconImage("my_location_marker"),
                    PropertyFactory.iconAllowOverlap(true)
                )
            )

            fragment.observeState(viewModel.nearbyUsers) { users ->
                updateUserMarkers(users)
            }

            fragment.observeState(viewModel.avatarBitmaps) { bitmaps ->
                var needsUpdate = false
                bitmaps.forEach { (userId, bitmap) ->
                    val imageId = "avatar_$userId"
                    if (style.getImage(imageId) == null) {
                        val avatarMarker = avatarBitmapCache.getOrPut(userId) {
                            createMarkerBitmapFromLayout(bitmap)
                        }
                        style.addImage(imageId, avatarMarker)
                        needsUpdate = true
                    }
                }
                if (needsUpdate) {
                    viewModel.nearbyUsers.value?.let { updateUserMarkers(it) }
                }
            }



            map.addOnMapClickListener { point ->
                val screenPoint = map.projection.toScreenLocation(point)
                val clicked = map.queryRenderedFeatures(screenPoint, "user-layer")
                if (clicked.isNotEmpty()) {
                    val feat = clicked[0]

                    fun safeString(key: String, default: String = ""): String {
                        val prop = feat.getProperty(key)
                        return if (prop != null && !prop.isJsonNull && prop.isJsonPrimitive) {
                            prop.asString
                        } else default
                    }

                    fun safeDouble(key: String, default: Double = 0.0): Double {
                        val prop = feat.getProperty(key)
                        return if (prop != null && !prop.isJsonNull && prop.isJsonPrimitive) {
                            try { prop.asDouble } catch (e: Exception) { default }
                        } else default
                    }

                    val userId = safeString("id")
                    val name = safeString("name", "Người dùng")
                    val university = safeString("university", "N/A")
                    val major = safeString("major", "N/A")
                    val statusTag = safeString("statusTag", "N/A")
                    val avatarUrl = safeString("avatar")
                    val distance = safeDouble("distance", 0.0)
                    val avatar = if (userId.isNotEmpty()) viewModel.avatarBitmaps.value[userId] else null

                    showUserInfoDialog(userId, name, university, major, statusTag, distance, avatar, avatarUrl)
                }
                true
            }

            fragment.currentUserLatLng?.let { location ->
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15.0))
            }
        }
    }

    fun startRadar(center: LatLng, radiusKm: Double) = radarRenderer.startRadar(center, radiusKm)

    fun stopRadar() = radarRenderer.stopRadar()

    fun clearCache() {
        avatarBitmapCache.clear()
        radarRenderer.stopRadar()
    }

    fun updateMyLocationMarker(latitude: Double, longitude: Double) {
        val style = map.style ?: return
        if (!style.isFullyLoaded) return
        val source = style.getSourceAs<GeoJsonSource>("my-location-source") ?: return
        source.setGeoJson(FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(Point.fromLngLat(longitude, latitude)))))
    }

    private fun createMarkerBitmapFromLayout(srcBitmap: Bitmap): Bitmap {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_avatar, null)
        val ivAvatar = view.findViewById<android.widget.ImageView>(R.id.ivAvatar)
        
        val roundedBitmap = getRoundedCornerBitmap(srcBitmap, 24f)
        ivAvatar.setImageBitmap(roundedBitmap)

        view.measure(
            View.MeasureSpec.makeMeasureSpec(markerSize, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(markerSize, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, markerSize, markerSize)

        val output = Bitmap.createBitmap(markerSize, markerSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        view.draw(canvas)
        return output
    }

    private fun getRoundedCornerBitmap(bitmap: Bitmap, pixels: Float): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        val color = -0xbdbdbe
        val paint = android.graphics.Paint()
        val rect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
        val rectF = android.graphics.RectF(rect)
        val roundPx = pixels

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint)

        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }

    private fun updateUserMarkers(users: List<NearbyUserResponse>) {
        val style = map.style ?: return
        if (!style.isFullyLoaded) return
        val source = style.getSourceAs<GeoJsonSource>("user-source") ?: return

        val features = users.filter { it.userId != viewModel.currentUserId }.map { user ->
            val feature = Feature.fromGeometry(Point.fromLngLat(user.longitude, user.latitude))
            feature.addStringProperty("id", user.userId ?: "")
            feature.addStringProperty("name", "${user.lastName ?: ""} ${user.firstName ?: ""}".trim())
            feature.addStringProperty("avatar", user.avatar ?: "")
            feature.addStringProperty("university", user.university ?: "")
            feature.addStringProperty("major", user.majorName ?: "")
            feature.addStringProperty("statusTag", user.statusTag ?: "")
            feature.addNumberProperty("distance", user.distanceKm ?: 0.0)

            val avatarId = "avatar_${user.userId}"
            feature.addStringProperty("avatar_id", if (style.getImage(avatarId) != null) avatarId else "my_marker")
            feature
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    private fun showUserInfoDialog(
        userId: String,
        name: String,
        university: String,
        major: String,
        statusTag: String,
        distance: Double,
        avatarBitmap: Bitmap?,
        avatarUrl: String? = null
    ) {
        val dialog = BottomSheetDialog(context)
        val binding = DialogUserInfoBinding.inflate(fragment.layoutInflater)
        dialog.setContentView(binding.root)

        binding.tvUserName.text = name
        binding.tvSchool.text = university
        binding.tvMajor.text = major
        binding.tvStatusTag.text = statusTag
        binding.tvDistance.text = context.getString(R.string.distance_format, distance)

        if (!avatarUrl.isNullOrEmpty()) {
            com.bumptech.glide.Glide.with(binding.ivAvatar.context)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .into(binding.ivAvatar)
        } else if (avatarBitmap != null) {
            binding.ivAvatar.setImageBitmap(avatarBitmap)
        } else {
            binding.ivAvatar.setImageResource(R.drawable.ic_launcher_foreground)
        }

        if (userId == viewModel.currentUserId) {
            binding.layoutActionButtons.visibility = View.GONE
        } else {
            binding.layoutActionButtons.visibility = View.VISIBLE
            binding.btnAddFriend.text = "Thêm bạn"
            binding.btnAddFriend.isEnabled = true
            binding.btnAddFriend.alpha = 1.0f

            fragment.lifecycleScope.launch {
                val friendsRes = friendRepository.getFriends(0, 100)
                if (friendsRes is com.example.myapplication.utils.resource.Resource.Success) {
                    val friends = friendsRes.data.data.content
                    if (friends.any { it.userId == userId }) {
                        binding.btnAddFriend.text = "Bạn bè"
                        binding.btnAddFriend.isEnabled = false
                        binding.btnAddFriend.alpha = 0.7f
                        return@launch
                    }
                }

                val pendingRes = friendRepository.getPendingFriendRequests(0, 100)
                if (pendingRes is com.example.myapplication.utils.resource.Resource.Success) {
                    val pending = pendingRes.data.data.content
                    if (pending.any { it.userId == userId }) {
                        binding.btnAddFriend.text = "Đã gửi yêu cầu"
                        binding.btnAddFriend.isEnabled = false
                        binding.btnAddFriend.alpha = 0.6f
                        return@launch
                    }
                }
            }

            binding.btnAddFriend.setOnClickListener {
                if (userId.isEmpty()) {
                    Toast.makeText(context, "Lỗi: ID người dùng không hợp lệ", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                binding.btnAddFriend.text = "Đã gửi yêu cầu"
                binding.btnAddFriend.isEnabled = false
                binding.btnAddFriend.alpha = 0.6f

                fragment.lifecycleScope.launch {
                    when (val result = friendRepository.sendFriendRequest(userId)) {
                        is com.example.myapplication.utils.resource.Resource.Success -> {
                            Toast.makeText(context, "Đã gửi lời mời kết bạn thành công", Toast.LENGTH_SHORT).show()
                        }
                        is com.example.myapplication.utils.resource.Resource.Error -> {
                            binding.btnAddFriend.text = "Thêm bạn"
                            binding.btnAddFriend.isEnabled = true
                            binding.btnAddFriend.alpha = 1.0f
                            Toast.makeText(context, result.message ?: "Gửi lời mời kết bạn thất bại", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            binding.btnViewProfile.setOnClickListener {
                dialog.dismiss()
                val intent = Intent(context, com.example.myapplication.ui.profile.ProfileActivity::class.java).apply {
                    putExtra("target_user_id", userId)
                }
                context.startActivity(intent)
            }
        }

        dialog.show()
    }
}
