package com.poilkar.nehank.firebaselocationtracking.animate

import android.os.Handler
import android.os.SystemClock
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


object MarkerAnimation {
    fun animateMarkerToGB(
        deliveryLocation: LatLng,
        marker: Marker,
        finalPosition: LatLng?,
        latLngInterpolator: LatLngInterpolator
    ) {
        val startPosition: LatLng = marker.position
        val handler = Handler()
        val start = SystemClock.uptimeMillis()
        val interpolator: Interpolator = AccelerateDecelerateInterpolator()
        val durationInMs = 2000f
        handler.post(object : Runnable {
            var elapsed: Long = 0
            var t = 0f
            var v = 0f
            override fun run() {
                // Calculate progress using interpolator
                elapsed = SystemClock.uptimeMillis() - start
                t = elapsed / durationInMs
                v = interpolator.getInterpolation(t)

                marker.position = latLngInterpolator.interpolate(v, startPosition, finalPosition)!!

                CoroutineScope(Dispatchers.IO).launch {
                    val bearing =
                        calculateBearing(
                            deliveryLocation.latitude,
                            deliveryLocation.longitude,
                            finalPosition!!.latitude,
                            finalPosition.longitude
                        )

                    withContext(Dispatchers.Main){
                        marker.rotation = bearing
//                        rotateMarker(marker,bearing)
                    }
                }


                // Repeat till progress is complete.
                if (t < 1) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16)
                }
            }
        })
    }

    fun calculateBearing(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val sourceLatLng = LatLng(lat1, lng1)
        val destinationLatLng = LatLng(lat2, lng2)
        return SphericalUtil.computeHeading(sourceLatLng, destinationLatLng).toFloat()
    }

    private fun rotateMarker(
        marker: Marker,
        toRotation: Float
    ) {
        val handler = Handler()
        val start = SystemClock.uptimeMillis()
        val startRotation = marker.rotation
        val duration: Long = 1000
        val interpolator: Interpolator = LinearInterpolator()
        handler.post(object : Runnable {
            override fun run() {
                val elapsed = SystemClock.uptimeMillis() - start
                val t =
                    interpolator.getInterpolation(elapsed.toFloat() / duration)
                val rot = t * toRotation + (1 - t) * startRotation
                marker.rotation = if (-rot > 180) rot / 2 else rot
                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16)
                }
            }
        })
    }
}