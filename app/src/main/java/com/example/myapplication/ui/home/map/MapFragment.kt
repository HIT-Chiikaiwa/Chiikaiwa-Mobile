package com.example.myapplication.ui.home.map

import android.content.Context
import android.content.Intent
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
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentMapBinding
import com.example.myapplication.ui.base.UiState
import com.example.myapplication.ui.home.chat.FriendsListActivity
import com.example.myapplication.ui.profile.ProfileActivity
import com.example.myapplication.utils.extension.observeState
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

    var currentUserLatLng: LatLng? = null
        private set

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            getUserLocationAndScan()
        } else {
            Toast.makeText(requireContext(), "Quyền vị trí bị từ chối. Vui lòng cấp quyền GPS để quét radar.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[MapViewModel::class.java]

        setupClickListeners()
        setupObservers()
        viewModel.loadCurrentUserAvatar()

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync { map ->
            mapLibreMap = map
            mapManager = MapManager(this, map, viewModel).also { it.setup() }
            showMyLocationOnMapOpen()
        }
    }

    private fun setupClickListeners() {
        binding.imgAvatar.setOnClickListener { findNavController().navigate(R.id.action_mapFragment_to_profileFragment) }
        binding.btnProfile.setOnClickListener { findNavController().navigate(R.id.action_mapFragment_to_profileFragment) }
        binding.btnFriend.setOnClickListener { findNavController().navigate(R.id.action_mapFragment_to_friendsListFragment) }
        binding.btnNotification.setOnClickListener { findNavController().navigate(R.id.action_mapFragment_to_notificationFragment) }
        binding.btnCalendar.setOnClickListener { findNavController().navigate(R.id.action_mapFragment_to_scheduleFragment) }

        binding.imgCrown.setOnClickListener {
            showToast("Tính năng đang được phát triển")
        }

        val scanAction = View.OnClickListener { checkLocationPermissionsAndScan() }
        binding.btnRadar.setOnClickListener(scanAction)
        binding.imgGroupRadar.setOnClickListener { if (binding.btnRadar.isEnabled) checkLocationPermissionsAndScan() }
        binding.layoutRadar.setOnClickListener { if (binding.btnRadar.isEnabled) checkLocationPermissionsAndScan() }
    }

    private fun setupObservers() {
        observeState(viewModel.currentUserAvatar) { avatarUrl ->
            if (!avatarUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .circleCrop()
                    .into(binding.imgAvatar)
            } else {
                binding.imgAvatar.setImageResource(R.drawable.ic_launcher_foreground)
            }
        }

        observeState(viewModel.uiState) { state ->
            val isLoading = state is UiState.Loading
            binding.btnRadar.setImageResource(if (isLoading) R.drawable.radaring else R.drawable.radar)
            binding.btnRadar.isEnabled = !isLoading

            if (isLoading) {
                currentUserLatLng?.let { location ->
                    val radiusKm = getSelectedRadiusKm()
                    mapManager?.startRadar(location, radiusKm)
                }
            } else {
                mapManager?.stopRadar()
                if (state is UiState.Error) {
                    showToast(state.message)
                }
            }
        }
    }

    private fun checkLocationPermissionsAndScan() {
        val hasFine = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            getUserLocationAndScan()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun showMyLocationOnMapOpen() {
        val hasFine = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return

        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var location: Location? = null
        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
            if (location == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        location?.let {
            currentUserLatLng = LatLng(it.latitude, it.longitude)
            currentUserLatLng?.let { latLng ->
                mapLibreMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0))
            }
            mapManager?.updateMyLocationMarker(it.latitude, it.longitude)
        }
    }

    private fun getUserLocationAndScan() {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            Toast.makeText(requireContext(), "Vui lòng bật định vị (GPS) trên thiết bị để thực hiện quét radar.", Toast.LENGTH_LONG).show()
            try {
                val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        var location: Location? = null
        try {
            if (isGpsEnabled) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
            if (location == null && isNetworkEnabled) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        if (location != null) {
            scanNearby(location.latitude, location.longitude)
        } else {
            showToast("Đang xác định vị trí GPS từ thiết bị...")
        }
    }

    private fun getSelectedRadiusKm(): Double {
        val distanceStr = binding.etDistance.text.toString().trim()
        var radiusKm = distanceStr.toDoubleOrNull() ?: 3.0
        if (radiusKm < 1.0) {
            radiusKm = 1.0
            binding.etDistance.setText("1.0")
            Toast.makeText(requireContext(), "Khoảng cách tối thiểu là 1km", Toast.LENGTH_SHORT).show()
        } else if (radiusKm > 5.0) {
            radiusKm = 5.0
            binding.etDistance.setText("5.0")
            Toast.makeText(requireContext(), "Khoảng cách tối đa là 5km", Toast.LENGTH_SHORT).show()
        }
        return radiusKm
    }

    private fun scanNearby(latitude: Double, longitude: Double) {
        currentUserLatLng = LatLng(latitude, longitude)
        currentUserLatLng?.let { latLng ->
            mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.0))
        }
        mapManager?.updateMyLocationMarker(latitude, longitude)
        val radiusKm = getSelectedRadiusKm()
        viewModel.getNearbyUsers(latitude, longitude, radiusKm)
    }

    private fun navigateTo(clazz: Class<*>) = startActivity(Intent(requireContext(), clazz))

    private fun showToast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        viewModel.loadCurrentUserAvatar()
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
        mapManager = null
        mapLibreMap = null
        _binding = null
        super.onDestroyView()
    }
}