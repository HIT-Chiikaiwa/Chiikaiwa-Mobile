package com.example.myapplication.ui.home.map

import android.app.AlertDialog
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

                val geoJsonSource = GeoJsonSource(
                    "user-source",
                    FeatureCollection.fromFeatures(emptyList())
                )
                style.addSource(geoJsonSource)

                style.addLayer(
                    SymbolLayer(
                        "user-layer",
                        "user-source"
                    ).withProperties(
                        iconImage("my_marker"),
                        iconAllowOverlap(true)
                    )
                )

                viewModel.nearbyUsers.observe(viewLifecycleOwner) { users ->
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
                        feature
                    }
                    geoJsonSource.setGeoJson(FeatureCollection.fromFeatures(features))
                }

                binding.btnRadar.setOnClickListener {
                    checkLocationPermissionsAndScan()
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

    private fun scanNearby(latitude: Double, longitude: Double) {
        mapLibreMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 15.0)
        )
        viewModel.getNearbyUsers(latitude, longitude, 5.0)
    }

    private fun showUserInfoDialog(name: String, university: String, major: String, statusTag: String, distance: Double) {
        val message = "Trường học: $university\nNgành học: $major\nTrạng thái: $statusTag\nKhoảng cách: %.2f km".format(distance)
        AlertDialog.Builder(requireContext())
            .setTitle(name)
            .setMessage(message)
            .setPositiveButton("Đóng", null)
            .show()
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