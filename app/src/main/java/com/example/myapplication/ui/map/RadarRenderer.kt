package com.example.myapplication.ui.map

import android.animation.ValueAnimator
import android.graphics.Color
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
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

class RadarRenderer(private val mapLibreMap: MapLibreMap) {

    companion object {
        private const val R_EARTH_KM = 6371.0
        private const val CIRCLE_STEPS = 48
        private const val SECTOR_STEPS = 20
        private const val SWEEP_ANGLE = 40.0
    }

    private var radarRadiusKm: Double = 3.0

    private var radarBeamAnimator: ValueAnimator? = null
    private var centerLatLng: LatLng? = null

    fun setupRadarLayers() {
        val style = mapLibreMap.style ?: return
        if (!style.isFullyLoaded) return

        if (style.getSource("radar-circle-source") == null) {
            val circleSource = GeoJsonSource("radar-circle-source", FeatureCollection.fromFeatures(emptyList()))
            style.addSource(circleSource)

            style.addLayer(
                FillLayer("radar-circle-fill-layer", "radar-circle-source").withProperties(
                    PropertyFactory.fillColor(Color.parseColor("#26000000")),
                    PropertyFactory.visibility(Property.NONE)
                )
            )

            val strokeSource = GeoJsonSource("radar-circle-stroke-source", FeatureCollection.fromFeatures(emptyList()))
            style.addSource(strokeSource)

            style.addLayer(
                LineLayer("radar-circle-stroke-layer", "radar-circle-stroke-source").withProperties(
                    PropertyFactory.lineColor(Color.parseColor("#4DFFFFFF")),
                    PropertyFactory.lineWidth(2f),
                    PropertyFactory.visibility(Property.NONE)
                )
            )
        }

        if (style.getSource("radar-beam-source") == null) {
            val beamSource = GeoJsonSource("radar-beam-source", FeatureCollection.fromFeatures(emptyList()))
            style.addSource(beamSource)

            style.addLayer(
                FillLayer("radar-beam-layer", "radar-beam-source").withProperties(
                    PropertyFactory.fillColor(Color.parseColor("#4DFFFFFF")),
                    PropertyFactory.visibility(Property.NONE)
                )
            )
        }
    }

    fun startRadar(center: LatLng, radiusKm: Double = 3.0) {
        if (center.latitude.isNaN() || center.longitude.isNaN() || (center.latitude == 0.0 && center.longitude == 0.0)) {
            return
        }
        radarRadiusKm = radiusKm
        centerLatLng = center
        showRadarLayers(true)
        updateRadarCircle(center)

        if (radarBeamAnimator == null) {
            radarBeamAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
                duration = 2500
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener { animator ->
                    val angle = animator.animatedValue as Float
                    updateRadarBeamRotation(angle.toDouble())
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
        val style = mapLibreMap.style ?: return
        if (!style.isFullyLoaded) return
        val visibility = if (show) Property.VISIBLE else Property.NONE
        style.getLayer("radar-circle-fill-layer")?.setProperties(PropertyFactory.visibility(visibility))
        style.getLayer("radar-circle-stroke-layer")?.setProperties(PropertyFactory.visibility(visibility))
        style.getLayer("radar-beam-layer")?.setProperties(PropertyFactory.visibility(visibility))
    }

    private fun updateRadarCircle(center: LatLng) {
        val style = mapLibreMap.style ?: return
        if (!style.isFullyLoaded) return
        val source = style.getSourceAs<GeoJsonSource>("radar-circle-source") ?: return
        val strokeSource = style.getSourceAs<GeoJsonSource>("radar-circle-stroke-source") ?: return
        
        val circlePoly = getCirclePolygon(center.latitude, center.longitude)
        source.setGeoJson(FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(circlePoly))))
        
        val points = circlePoly.coordinates()[0]
        val circleLine = LineString.fromLngLats(points)
        strokeSource.setGeoJson(FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(circleLine))))
    }

    private fun updateRadarBeamRotation(startAngleDeg: Double) {
        val style = mapLibreMap.style ?: return
        if (!style.isFullyLoaded) return
        val center = centerLatLng ?: return
        val source = style.getSourceAs<GeoJsonSource>("radar-beam-source") ?: return
        val beamPoly = getSectorPolygon(center.latitude, center.longitude, startAngleDeg)
        source.setGeoJson(FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(beamPoly))))
    }

    private fun getSectorPolygon(centerLat: Double, centerLng: Double, startAngleDeg: Double): Polygon {
        val points = ArrayList<Point>(SECTOR_STEPS + 2)
        points.add(Point.fromLngLat(centerLng, centerLat))

        val latRad = Math.toRadians(centerLat)
        val lngRad = Math.toRadians(centerLng)
        val dDivR = radarRadiusKm / R_EARTH_KM
        val sinD = Math.sin(dDivR)
        val cosD = Math.cos(dDivR)
        val cosLat = Math.cos(latRad)
        val sinLat = Math.sin(latRad)

        for (i in 0..SECTOR_STEPS) {
            val angleDeg = startAngleDeg + (SWEEP_ANGLE * i / SECTOR_STEPS)
            val bearingRad = Math.toRadians(angleDeg)

            val pointLatRad = Math.asin(sinLat * cosD + cosLat * sinD * Math.cos(bearingRad))
            val pointLngRad = lngRad + Math.atan2(
                Math.sin(bearingRad) * sinD * cosLat,
                cosD - sinLat * Math.sin(pointLatRad)
            )

            points.add(Point.fromLngLat(Math.toDegrees(pointLngRad), Math.toDegrees(pointLatRad)))
        }

        points.add(Point.fromLngLat(centerLng, centerLat))
        if (points.isNotEmpty()) {
            points[points.size - 1] = points[0]
        }
        return Polygon.fromLngLats(listOf(points))
    }

    private fun getCirclePolygon(centerLat: Double, centerLng: Double): Polygon {
        val points = ArrayList<Point>(CIRCLE_STEPS + 1)
        val latRad = Math.toRadians(centerLat)
        val lngRad = Math.toRadians(centerLng)
        val dDivR = radarRadiusKm / R_EARTH_KM
        val sinD = Math.sin(dDivR)
        val cosD = Math.cos(dDivR)
        val cosLat = Math.cos(latRad)
        val sinLat = Math.sin(latRad)

        for (i in 0..CIRCLE_STEPS) {
            val angleDeg = 360.0 * i / CIRCLE_STEPS
            val bearingRad = Math.toRadians(angleDeg)

            val pointLatRad = Math.asin(sinLat * cosD + cosLat * sinD * Math.cos(bearingRad))
            val pointLngRad = lngRad + Math.atan2(
                Math.sin(bearingRad) * sinD * cosLat,
                cosD - sinLat * Math.sin(pointLatRad)
            )

            points.add(Point.fromLngLat(Math.toDegrees(pointLngRad), Math.toDegrees(pointLatRad)))
        }

        if (points.isNotEmpty()) {
            points[points.size - 1] = points[0]
        }
        return Polygon.fromLngLats(listOf(points))
    }
}
