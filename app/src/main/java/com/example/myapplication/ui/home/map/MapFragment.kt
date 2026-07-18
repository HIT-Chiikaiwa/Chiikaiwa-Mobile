package com.example.myapplication.ui.home.map

import android.content.Context
import android.content.pm.PackageManager
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

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MapViewModel
    private var mapLibreMap: MapLibreMap? = null
    private var mapManager: MapManager? = null

    private val defaultLatLng = LatLng(21.028511, 105.804817)
    private var currentUserLatLng: LatLng? = null
    private val RADAR_RADIUS_KM = 6.0

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            getUserLocationAndScan()
        } else {
            Toast.makeText(requireContext(), "Quyền vị trí bị từ chối. Vui lòng cấp quyền vị trí để quét radar.", Toast.LENGTH_LONG).show()
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
        binding.imgGroupRadar.setOnClickListener {
            if (binding.btnRadar.isEnabled) {
                checkLocationPermissionsAndScan()
            }
        }
        binding.layoutRadar.setOnClickListener {
            if (binding.btnRadar.isEnabled) {
                checkLocationPermissionsAndScan()
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                com.example.myapplication.ui.base.UiState.Loading -> {
                    binding.btnRadar.setImageResource(R.drawable.radaring)
                    binding.btnRadar.isEnabled = false
                    mapManager?.startRadar(currentUserLatLng ?: defaultLatLng)
                }
                is com.example.myapplication.ui.base.UiState.Success -> {
                    binding.btnRadar.setImageResource(R.drawable.radar)
                    binding.btnRadar.isEnabled = true
                    mapManager?.stopRadar()
                }
                is com.example.myapplication.ui.base.UiState.Error -> {
                    binding.btnRadar.setImageResource(R.drawable.radar)
                    binding.btnRadar.isEnabled = true
                    mapManager?.stopRadar()
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
                com.example.myapplication.ui.base.UiState.Idle -> {
                    binding.btnRadar.setImageResource(R.drawable.radar)
                    binding.btnRadar.isEnabled = true
                    mapManager?.stopRadar()
                }
            }
        }

        binding.mapView.onCreate(savedInstanceState)

        binding.mapView.getMapAsync { map ->
            mapLibreMap = map
            mapManager = MapManager(this, map, viewModel)
            mapManager?.setup()
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
            Toast.makeText(requireContext(), "Vui lòng bật định vị (GPS) để thực hiện quét radar.", Toast.LENGTH_LONG).show()
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
            Toast.makeText(requireContext(), "Đang xác định vị trí của bạn...", Toast.LENGTH_SHORT).show()
            scanNearby(defaultLatLng.latitude, defaultLatLng.longitude)
        }
    }

    private fun scanNearby(latitude: Double, longitude: Double) {
        currentUserLatLng = LatLng(latitude, longitude)
        mapLibreMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 15.0)
        )
        mapManager?.updateMyLocationMarker(latitude, longitude)
        viewModel.getNearbyUsers(latitude, longitude, RADAR_RADIUS_KM)
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
        mapManager?.clearCache()
        binding.mapView.onDestroy()
        _binding = null
        super.onDestroyView()
    }
}