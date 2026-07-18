package com.example.myapplication.ui.home.map

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

class RadarRenderer(private val mapLibreMap: MapLibreMap) {

    private val RADAR_RADIUS_KM = 6.0
    private var radarBeamAnimator: ValueAnimator? = null
    private var currentRadarAngle = 0f
    private var centerLatLng: LatLng? = null

    fun setupRadarLayers() {
        val style = mapLibreMap.style ?: return
        if (!style.isFullyLoaded) return

        if (style.getSource("radar-circle-source") == null) {
            val radarCircleSource = GeoJsonSource("radar-circle-source", FeatureCollection.fromFeatures(emptyList()))
            style.addSource(radarCircleSource)

            style.addLayer(
                FillLayer("radar-circle-fill-layer", "radar-circle-source").withProperties(
                    PropertyFactory.fillColor(android.graphics.Color.parseColor("#26000000")),
                    PropertyFactory.visibility(Property.NONE)
                )
            )

            style.addLayer(
                LineLayer("radar-circle-stroke-layer", "radar-circle-source").withProperties(
                    PropertyFactory.lineColor(android.graphics.Color.parseColor("#4DFFFFFF")),
                    PropertyFactory.lineWidth(2f),
                    PropertyFactory.visibility(Property.NONE)
                )
            )
        }

        if (style.getSource("radar-beam-source") == null) {
            val radarBeamSource = GeoJsonSource("radar-beam-source", FeatureCollection.fromFeatures(emptyList()))
            style.addSource(radarBeamSource)

            style.addLayer(
                FillLayer("radar-beam-layer", "radar-beam-source").withProperties(
                    PropertyFactory.fillColor(android.graphics.Color.parseColor("#4DFFFFFF")),
                    PropertyFactory.visibility(Property.NONE)
                )
            )
        }
    }

    fun startRadar(center: LatLng) {
        centerLatLng = center
        showRadarLayers(true)
        updateRadarCircle(center)

        if (radarBeamAnimator == null) {
            radarBeamAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
                duration = 3000
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener { animator ->
                    val angle = animator.animatedValue as Float
                    currentRadarAngle = angle
                    updateRadarBeamRotation()
                }
            }
        }
        if (radarBeamAnimator?.isRunning != true) {
            radarBeamAnimator?.start()
        }
    }

    fun stopRadar() {
        radarBeamAnimator?.cancel()
        showRadarLayers(false)
    }

    private fun showRadarLayers(show: Boolean) {
        val mapStyle = mapLibreMap.style ?: return
        if (!mapStyle.isFullyLoaded) return
        val visibility = if (show) Property.VISIBLE else Property.NONE
        mapStyle.getLayer("radar-circle-fill-layer")?.setProperties(PropertyFactory.visibility(visibility))
        mapStyle.getLayer("radar-circle-stroke-layer")?.setProperties(PropertyFactory.visibility(visibility))
        mapStyle.getLayer("radar-beam-layer")?.setProperties(PropertyFactory.visibility(visibility))
    }

    private fun updateRadarCircle(center: LatLng) {
        val mapStyle = mapLibreMap.style ?: return
        if (!mapStyle.isFullyLoaded) return
        val circleSource = mapStyle.getSourceAs<GeoJsonSource>("radar-circle-source") ?: return
        val circlePoly = getCirclePolygon(center.latitude, center.longitude, RADAR_RADIUS_KM)
        circleSource.setGeoJson(FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(circlePoly))))
    }

    private fun updateRadarBeamRotation() {
        val mapStyle = mapLibreMap.style ?: return
        if (!mapStyle.isFullyLoaded) return
        val center = centerLatLng ?: return
        val beamSource = mapStyle.getSourceAs<GeoJsonSource>("radar-beam-source") ?: return
        val beamPoly = getSectorPolygon(center.latitude, center.longitude, RADAR_RADIUS_KM, currentRadarAngle.toDouble(), 40.0)
        beamSource.setGeoJson(FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(beamPoly))))
    }

    private fun getSectorPolygon(centerLat: Double, centerLng: Double, radiusKm: Double, startAngleDeg: Double, sweepAngleDeg: Double): Polygon {
        val points = mutableListOf<Point>()
        points.add(Point.fromLngLat(centerLng, centerLat))

        val rEarth = 6371.0
        val latRad = Math.toRadians(centerLat)
        val lngRad = Math.toRadians(centerLng)
        val dDivR = radiusKm / rEarth
        val sinDDivR = Math.sin(dDivR)
        val cosDDivR = Math.cos(dDivR)

        val steps = 30
        for (i in 0..steps) {
            val angleDeg = startAngleDeg + (sweepAngleDeg * i / steps)
            val bearingRad = Math.toRadians(angleDeg)

            val pointLatRad = Math.asin(Math.sin(latRad) * cosDDivR + Math.cos(latRad) * sinDDivR * Math.cos(bearingRad))
            val pointLngRad = lngRad + Math.atan2(
                Math.sin(bearingRad) * sinDDivR * Math.cos(latRad),
                cosDDivR - Math.sin(latRad) * Math.sin(pointLatRad)
            )

            points.add(Point.fromLngLat(Math.toDegrees(pointLngRad), Math.toDegrees(pointLatRad)))
        }

        points.add(Point.fromLngLat(centerLng, centerLat))
        return Polygon.fromLngLats(listOf(points))
    }

    private fun getCirclePolygon(centerLat: Double, centerLng: Double, radiusKm: Double): Polygon {
        val points = mutableListOf<Point>()
        val rEarth = 6371.0
        val latRad = Math.toRadians(centerLat)
        val lngRad = Math.toRadians(centerLng)
        val dDivR = radiusKm / rEarth
        val sinDDivR = Math.sin(dDivR)
        val cosDDivR = Math.cos(dDivR)

        val steps = 64
        for (i in 0..steps) {
            val angleDeg = 360.0 * i / steps
            val bearingRad = Math.toRadians(angleDeg)

            val pointLatRad = Math.asin(Math.sin(latRad) * cosDDivR + Math.cos(latRad) * sinDDivR * Math.cos(bearingRad))
            val pointLngRad = lngRad + Math.atan2(
                Math.sin(bearingRad) * sinDDivR * Math.cos(latRad),
                cosDDivR - Math.sin(latRad) * Math.sin(pointLatRad)
            )

            points.add(Point.fromLngLat(Math.toDegrees(pointLngRad), Math.toDegrees(pointLatRad)))
        }

        return Polygon.fromLngLats(listOf(points))
    }
}
