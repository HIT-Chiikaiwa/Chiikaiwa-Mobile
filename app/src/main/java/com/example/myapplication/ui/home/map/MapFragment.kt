package com.example.myapplication.ui.home.map

import com.example.myapplication.R
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentMapBinding
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

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

        binding.mapView.onCreate(savedInstanceState)

        binding.mapView.getMapAsync { map ->

            map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->

                style.addImage(
                    "marker",
                    BitmapFactory.decodeResource(
                        resources,
                        R.drawable.ic_marker
                    )
                )

                val haNoi = LatLng(21.028511, 105.804817)

                val cameraPosition = CameraPosition.Builder()
                    .target(haNoi)
                    .zoom(15.0)
                    .build()

                map.animateCamera(
                    CameraUpdateFactory.newCameraPosition(cameraPosition)
                )

                val symbolManager = SymbolManager(
                    binding.mapView,
                    map,
                    style
                )

                symbolManager.create(
                    SymbolOptions()
                        .withLatLng(haNoi)
                        .withIconImage("marker")
                        .withIconAnchor("bottom")
                        .withIconSize(0.8f)
                )

            }
        }
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