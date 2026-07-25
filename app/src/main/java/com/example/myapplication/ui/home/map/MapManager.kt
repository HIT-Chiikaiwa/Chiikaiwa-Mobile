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
import org.maplibre.geojson.Point

class MapManager(
    private val fragment: MapFragment,
    private val map: MapLibreMap,
    private val viewModel: MapViewModel
) {
    private val context = fragment.requireContext()
    private val avatarBitmapCache = HashMap<String, Bitmap>()
    private val radarRenderer = RadarRenderer(map)
    private val markerSize by lazy { context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._48sdp) }

    fun setup() {
        map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->

            BitmapFactory.decodeResource(context.resources, R.drawable.ic_marker)?.let { bitmap ->
                val scaled = Bitmap.createScaledBitmap(bitmap, markerSize, markerSize, false)
                style.addImage("my_marker", scaled)
                style.addImage("my_location_marker", scaled)
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
                    val userId = feat.getStringProperty("id") ?: ""
                    val name = feat.getStringProperty("name") ?: "Người dùng"
                    val university = feat.getStringProperty("university") ?: "N/A"
                    val major = feat.getStringProperty("major") ?: "N/A"
                    val statusTag = feat.getStringProperty("statusTag") ?: "N/A"
                    val distance = feat.getNumberProperty("distance")?.toDouble() ?: 0.0
                    val avatar = if (userId.isNotEmpty()) viewModel.avatarBitmaps.value[userId] else null

                    showUserInfoDialog(userId, name, university, major, statusTag, distance, avatar)
                }
                true
            }

            fragment.currentUserLatLng?.let { location ->
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15.0))
            }
        }
    }

    fun startRadar(center: LatLng) = radarRenderer.startRadar(center)

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
        ivAvatar.setImageBitmap(srcBitmap)

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

    private fun updateUserMarkers(users: List<NearbyUserResponse>) {
        val style = map.style ?: return
        if (!style.isFullyLoaded) return
        val source = style.getSourceAs<GeoJsonSource>("user-source") ?: return

        val features = users.map { user ->
            val feature = Feature.fromGeometry(Point.fromLngLat(user.longitude, user.latitude))
            feature.addStringProperty("id", user.userId)
            feature.addStringProperty("name", "${user.lastName ?: ""} ${user.firstName ?: ""}".trim())
            feature.addStringProperty("avatar", user.avatar)
            feature.addStringProperty("university", user.university)
            feature.addStringProperty("major", user.majorName)
            feature.addStringProperty("statusTag", user.statusTag)
            feature.addNumberProperty("distance", user.distanceKm)

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
        avatarBitmap: Bitmap?
    ) {
        val dialog = BottomSheetDialog(context)
        val binding = DialogUserInfoBinding.inflate(fragment.layoutInflater)
        dialog.setContentView(binding.root)

        binding.tvUserName.text = name
        binding.tvSchool.text = university
        binding.tvMajor.text = major
        binding.tvStatusTag.text = statusTag
        binding.tvDistance.text = context.getString(R.string.distance_format, distance)

        if (avatarBitmap != null) {
            binding.ivAvatar.setImageBitmap(avatarBitmap)
        } else {
            binding.ivAvatar.setImageResource(R.drawable.ic_launcher_foreground)
        }

        if (userId == viewModel.currentUserId) {
            binding.btnSendMessage.visibility = View.GONE
        } else {
            binding.btnSendMessage.visibility = View.VISIBLE
            binding.btnSendMessage.setOnClickListener {
                dialog.dismiss()
                val intent = Intent(context, ChatActivity::class.java).apply {
                    putExtra("target_user_id", userId)
                    putExtra("user_name", name)
                }
                context.startActivity(intent)
            }
        }

        dialog.show()
    }
}
