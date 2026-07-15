package com.example.myapplication.ui.home.map

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentMapBinding
import com.example.myapplication.databinding.DialogUserInfoBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.BitmapShader
import android.graphics.Shader
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MapViewModel
    private var mapLibreMap: MapLibreMap? = null

    private val defaultLatLng = LatLng(21.028511, 105.804817)
    private var currentUserLatLng: LatLng? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            getUserLocationAndScan()
        } else {
            Toast.makeText(requireContext(), "Quyền vị trí bị từ chối, quét xung quanh Hà Nội làm mặc định", Toast.LENGTH_LONG).show()
            scanNearby(defaultLatLng.latitude, defaultLatLng.longitude)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[MapViewModel::class.java]

        binding.btnSchedule.setOnClickListener {
            Toast.makeText(requireContext(), "Tính năng Lịch trình đang được phát triển", Toast.LENGTH_SHORT).show()
        }
        binding.btnFriend.setOnClickListener {
            Toast.makeText(requireContext(), "Tính năng Bạn bè đang được phát triển", Toast.LENGTH_SHORT).show()
        }
        binding.btnNotification.setOnClickListener {
            Toast.makeText(requireContext(), "Tính năng Thông báo đang được phát triển", Toast.LENGTH_SHORT).show()
        }
        binding.btnCalendar.setOnClickListener {
            Toast.makeText(requireContext(), "Tính năng Lịch hẹn đang được phát triển", Toast.LENGTH_SHORT).show()
        }
        binding.btnRadar.setOnClickListener {
            checkLocationPermissionsAndScan()
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                com.example.myapplication.ui.base.UiState.Loading -> {
                    binding.btnRadar.setImageResource(R.drawable.radaring)
                    binding.btnRadar.isEnabled = false
                }
                is com.example.myapplication.ui.base.UiState.Success -> {
                    binding.btnRadar.setImageResource(R.drawable.radar)
                    binding.btnRadar.isEnabled = true
                }
                is com.example.myapplication.ui.base.UiState.Error -> {
                    binding.btnRadar.setImageResource(R.drawable.radar)
                    binding.btnRadar.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                com.example.myapplication.ui.base.UiState.Idle -> {
                    binding.btnRadar.setImageResource(R.drawable.radar)
                    binding.btnRadar.isEnabled = true
                }
            }
        }

        viewModel.nearbyUsers.observe(viewLifecycleOwner) { users ->
            val mapStyle = mapLibreMap?.style
            if (mapStyle != null && mapStyle.isFullyLoaded) {
                updateUserMarkers(users)
            }
        }

        viewModel.avatarBitmaps.observe(viewLifecycleOwner) { bitmaps ->
            val mapStyle = mapLibreMap?.style
            if (mapStyle != null && mapStyle.isFullyLoaded) {
                var needsUpdate = false
                bitmaps.forEach { (userId, bitmap) ->
                    val avatarImageId = "avatar_$userId"
                    if (mapStyle.getImage(avatarImageId) == null) {
                        val roundedBitmap = getRoundedAvatarWithBorder(bitmap)
                        mapStyle.addImage(avatarImageId, roundedBitmap)
                        needsUpdate = true
                    }
                }
                if (needsUpdate) {
                    viewModel.nearbyUsers.value?.let { users ->
                        updateUserMarkers(users)
                    }
                }
            }
        }

        binding.mapView.onCreate(savedInstanceState)

        binding.mapView.getMapAsync { map ->
            mapLibreMap = map
            map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->
                val bitmap = BitmapFactory.decodeResource(
                    resources,
                    R.drawable.ic_marker
                )
                if (bitmap != null) {
                    style.addImage("my_marker", bitmap)
                } else {
                    android.util.Log.e("MapFragment", "Failed to decode ic_marker resource")
                }

                val myLocationBitmap = BitmapFactory.decodeResource(
                    resources,
                    R.drawable.frame3
                )
                if (myLocationBitmap != null) {
                    val scaledBitmap = Bitmap.createScaledBitmap(myLocationBitmap, 120, 120, false)
                    style.addImage("my_location_marker", scaledBitmap)
                }

                val geoJsonSource = GeoJsonSource(
                    "user-source",
                    FeatureCollection.fromFeatures(emptyList())
                )
                style.addSource(geoJsonSource)

                val myLocationSource = GeoJsonSource(
                    "my-location-source",
                    FeatureCollection.fromFeatures(emptyList())
                )
                style.addSource(myLocationSource)

                style.addLayer(
                    SymbolLayer(
                        "user-layer",
                        "user-source"
                    ).withProperties(
                        iconImage("{avatar_id}"),
                        iconAllowOverlap(true)
                    )
                )

                style.addLayer(
                    SymbolLayer(
                        "my-location-layer",
                        "my-location-source"
                    ).withProperties(
                        iconImage("my_location_marker"),
                        iconAllowOverlap(true)
                    )
                )

                viewModel.avatarBitmaps.value?.forEach { (userId, bitmap) ->
                    val avatarImageId = "avatar_$userId"
                    if (style.getImage(avatarImageId) == null) {
                        val roundedBitmap = getRoundedAvatarWithBorder(bitmap)
                        style.addImage(avatarImageId, roundedBitmap)
                    }
                }

                viewModel.nearbyUsers.value?.let { users ->
                    updateUserMarkers(users)
                }

                currentUserLatLng?.let { latLng ->
                    val myLocSource = style.getSourceAs<GeoJsonSource>("my-location-source")
                    if (myLocSource != null) {
                        val pointFeature = Feature.fromGeometry(Point.fromLngLat(latLng.longitude, latLng.latitude))
                        myLocSource.setGeoJson(FeatureCollection.fromFeatures(listOf(pointFeature)))
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

                        showUserInfoDialog(name, university, major, statusTag, distance)
                    }
                    true
                }

                checkLocationPermissionsAndScan()
            }
        }
    }

    private fun checkLocationPermissionsAndScan() {
        val hasFine = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            getUserLocationAndScan()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun getUserLocationAndScan() {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            Toast.makeText(requireContext(), "GPS/Mạng chưa được bật, quét tại Hà Nội", Toast.LENGTH_SHORT).show()
            scanNearby(defaultLatLng.latitude, defaultLatLng.longitude)
            return
        }

        var location: Location? = null
        try {
            if (isNetworkEnabled) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
            if (location == null && isGpsEnabled) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        if (location != null) {
            scanNearby(location.latitude, location.longitude)
        } else {
            scanNearby(defaultLatLng.latitude, defaultLatLng.longitude)
        }
    }

    private fun updateMyLocationMarker(latitude: Double, longitude: Double) {
        val mapStyle = mapLibreMap?.style
        if (mapStyle != null && mapStyle.isFullyLoaded) {
            val source = mapStyle.getSourceAs<GeoJsonSource>("my-location-source")
            if (source != null) {
                val pointFeature = Feature.fromGeometry(Point.fromLngLat(longitude, latitude))
                source.setGeoJson(FeatureCollection.fromFeatures(listOf(pointFeature)))
            }
        }
    }

    private fun scanNearby(latitude: Double, longitude: Double) {
        currentUserLatLng = LatLng(latitude, longitude)
        mapLibreMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 15.0)
        )
        updateMyLocationMarker(latitude, longitude)
        viewModel.getNearbyUsers(latitude, longitude, 5.0)
    }
    private fun getRoundedAvatarWithBorder(srcBitmap: Bitmap): Bitmap {
        val size = 120
        val borderSize = 10f
        val cornerRadius = 24f

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val frameDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.bg_avatar_marker_frame)
        frameDrawable?.setBounds(0, 0, size, size)
        frameDrawable?.draw(canvas)

        val innerPaint = Paint().apply {
            isAntiAlias = true
        }
        val innerRect = RectF(
            borderSize,
            borderSize,
            size - borderSize,
            size - borderSize
        )

        val scaledBitmap = Bitmap.createScaledBitmap(
            srcBitmap,
            (size - 2 * borderSize).toInt(),
            (size - 2 * borderSize).toInt(),
            false
        )
        val shader = BitmapShader(scaledBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        innerPaint.shader = shader

        canvas.drawRoundRect(innerRect, cornerRadius - 4f, cornerRadius - 4f, innerPaint)

        return output
    }

    private fun updateUserMarkers(users: List<com.example.myapplication.data.model.response.NearbyUserResponse>) {
        val mapStyle = mapLibreMap?.style ?: return
        if (!mapStyle.isFullyLoaded) return
        val geoJsonSource = mapStyle.getSourceAs<GeoJsonSource>("user-source") ?: return

        val features = users.map { user ->
            val feature = Feature.fromGeometry(
                Point.fromLngLat(user.longitude, user.latitude)
            )
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

    private fun showUserInfoDialog(name: String, university: String, major: String, statusTag: String, distance: Double) {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogUserInfoBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.tvUserName.text = name
        dialogBinding.tvSchool.text = university
        dialogBinding.tvMajor.text = major
        dialogBinding.tvStatusTag.text = statusTag
        dialogBinding.tvDistance.text = "Cách bạn %.2f km".format(distance)

        dialogBinding.btnSendMessage.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        binding.mapView.onPause()
        super.onPause()
    }

    override fun onStop() {
        binding.mapView.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroyView() {
        binding.mapView.onDestroy()
        _binding = null
        super.onDestroyView()
    }
}