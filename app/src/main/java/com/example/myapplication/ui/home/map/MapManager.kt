package com.example.myapplication.ui.home.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import com.example.myapplication.R
import com.example.myapplication.data.model.response.NearbyUserResponse
import com.example.myapplication.databinding.DialogUserInfoBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
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
    private val defaultLatLng = LatLng(21.028511, 105.804817)
    private val roundedAvatarsCache = mutableMapOf<String, Bitmap>()
    private val radarRenderer = RadarRenderer(map)

    fun setup() {
        radarRenderer.setupRadarLayers()

        map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->

            BitmapFactory.decodeResource(context.resources, R.drawable.ic_marker)?.let { bitmap ->
                style.addImage("my_marker", bitmap)
            }

            BitmapFactory.decodeResource(context.resources, R.drawable.ic_marker)?.let { myLocationBitmap ->
                val myLocationSize = context.resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._48sdp)
                val scaledBitmap = Bitmap.createScaledBitmap(myLocationBitmap, myLocationSize, myLocationSize, false)
                style.addImage("my_location_marker", scaledBitmap)
            }

            radarRenderer.setupRadarLayers()

            style.addSource(GeoJsonSource("user-source", FeatureCollection.fromFeatures(emptyList())))
            style.addLayer(
                SymbolLayer("user-layer", "user-source").withProperties(
                    org.maplibre.android.style.layers.PropertyFactory.iconImage("{avatar_id}"),
                    org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap(true)
                )
            )

            style.addSource(GeoJsonSource("my-location-source", FeatureCollection.fromFeatures(emptyList())))
            style.addLayer(
                SymbolLayer("my-location-layer", "my-location-source").withProperties(
                    org.maplibre.android.style.layers.PropertyFactory.iconImage("my_location_marker"),
                    org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap(true)
                )
            )

            viewModel.nearbyUsers.observe(fragment.viewLifecycleOwner) { users ->
                updateUserMarkers(users)
            }

            viewModel.avatarBitmaps.observe(fragment.viewLifecycleOwner) { bitmaps ->
                var needsUpdate = false
                bitmaps.forEach { (userId, bitmap) ->
                    val avatarImageId = "avatar_$userId"
                    if (style.getImage(avatarImageId) == null) {
                        val roundedBitmap = roundedAvatarsCache.getOrPut(userId) {
                            createMarkerBitmapFromLayout(bitmap)
                        }
                        style.addImage(avatarImageId, roundedBitmap)
                        needsUpdate = true
                    }
                }
                if (needsUpdate) {
                    viewModel.nearbyUsers.value?.let { users ->
                        updateUserMarkers(users)
                    }
                }
            }

            map.addOnMapClickListener { point ->
                val screenPoint = map.projection.toScreenLocation(point)
                val clickedFeatures = map.queryRenderedFeatures(screenPoint, "user-layer")
                if (clickedFeatures.isNotEmpty()) {
                    val feature = clickedFeatures[0]
                    val name = feature.getStringProperty("name")
                    val university = feature.getStringProperty("university") ?: "N/A"
                    val major = feature.getStringProperty("major") ?: "N/A"
                    val statusTag = feature.getStringProperty("statusTag") ?: "N/A"
                    val distance = feature.getNumberProperty("distance")?.toDouble() ?: 0.0
                    val userId = feature.getStringProperty("id")
                    val avatarBitmap = userId?.let { viewModel.avatarBitmaps.value?.get(it) }

                    showUserInfoDialog(name, university, major, statusTag, distance, avatarBitmap)
                }
                true
            }

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 15.0))
        }
    }

    fun startRadar(center: LatLng) {
        radarRenderer.startRadar(center)
    }

    fun stopRadar() {
        radarRenderer.stopRadar()
    }

    fun clearCache() {
        roundedAvatarsCache.clear()
        radarRenderer.stopRadar()
    }

    fun updateMyLocationMarker(latitude: Double, longitude: Double) {
        val mapStyle = map.style
        if (mapStyle != null && mapStyle.isFullyLoaded) {
            val source = mapStyle.getSourceAs<GeoJsonSource>("my-location-source")
            if (source != null) {
                val pointFeature = Feature.fromGeometry(Point.fromLngLat(longitude, latitude))
                source.setGeoJson(FeatureCollection.fromFeatures(listOf(pointFeature)))
            }
        }
    }

    private fun createMarkerBitmapFromLayout(srcBitmap: Bitmap): Bitmap {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_marker_avatar, null)
        val ivAvatar = view.findViewById<android.widget.ImageView>(R.id.ivAvatar)
        ivAvatar.setImageBitmap(srcBitmap)

        view.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val output = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        view.draw(canvas)

        return output
    }

    private fun updateUserMarkers(users: List<NearbyUserResponse>) {
        val mapStyle = map.style ?: return
        if (!mapStyle.isFullyLoaded) return
        val geoJsonSource = mapStyle.getSourceAs<GeoJsonSource>("user-source") ?: return

        val features = users.map { user ->
            val feature = Feature.fromGeometry(Point.fromLngLat(user.longitude, user.latitude))
            feature.addStringProperty("id", user.userId)
            feature.addStringProperty("name", "${user.lastName} ${user.firstName}".trim())
            feature.addStringProperty("avatar", user.avatar)
            feature.addStringProperty("university", user.university)
            feature.addStringProperty("major", user.majorName)
            feature.addStringProperty("statusTag", user.statusTag)
            feature.addNumberProperty("distance", user.distanceKm)

            val avatarImageId = "avatar_${user.userId}"
            if (mapStyle.getImage(avatarImageId) != null) {
                feature.addStringProperty("avatar_id", avatarImageId)
            } else {
                feature.addStringProperty("avatar_id", "my_marker")
            }
            feature
        }
        geoJsonSource.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    private fun showUserInfoDialog(name: String, university: String, major: String, statusTag: String, distance: Double, avatarBitmap: Bitmap?) {
        val dialog = BottomSheetDialog(context)
        val dialogBinding = DialogUserInfoBinding.inflate(fragment.layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.tvUserName.text = name
        dialogBinding.tvSchool.text = university
        dialogBinding.tvMajor.text = major
        dialogBinding.tvStatusTag.text = statusTag
        dialogBinding.tvDistance.text = context.getString(R.string.distance_format, distance)
        if (avatarBitmap != null) {
            dialogBinding.ivAvatar.setImageBitmap(avatarBitmap)
        } else {
            dialogBinding.ivAvatar.setImageResource(R.drawable.ic_launcher_foreground)
        }

        dialogBinding.btnSendMessage.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
