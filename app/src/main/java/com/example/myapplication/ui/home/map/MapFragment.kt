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

import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource

import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point

import org.maplibre.geojson.FeatureCollection

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

                val haNoi = LatLng(
                    21.028511,
                    105.804817
                )

                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        haNoi,
                        15.0
                    )
                )

                val bitmap = BitmapFactory.decodeResource(
                    resources,
                    R.drawable.ic_marker
                )

                style.addImage("my_marker", bitmap)

                val features = listOf(

                    Feature.fromGeometry(
                        Point.fromLngLat(
                            105.804817,
                            21.028511
                        )
                    ),

                    Feature.fromGeometry(
                        Point.fromLngLat(
                            105.808000,
                            21.030000
                        )
                    ),

                    Feature.fromGeometry(
                        Point.fromLngLat(
                            105.801500,
                            21.025500
                        )
                    )

                )

                style.addSource(
                    GeoJsonSource(
                        "user-source",
                        FeatureCollection.fromFeatures(features)
                    )
                )

                style.addLayer(
                    SymbolLayer(
                        "user-layer",
                        "user-source"
                    ).withProperties(
                        iconImage("my_marker"),
                        iconAllowOverlap(true)
                    )
                )
                Log.d("MAP_TEST", "Image = ${style.getImage("marker") != null}")
                Log.d("MAP_TEST", "Source = ${style.getSource("user-source") != null}")
                Log.d("MAP_TEST", "Layer = ${style.getLayer("user-layer") != null}")
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