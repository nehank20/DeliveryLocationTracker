package com.poilkar.nehank.firebaselocationtracking

import android.Manifest
import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.Service.START_NOT_STICKY
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.Nullable
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.gms.location.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.auth.User


class LocationService : Service() {
    private var mFusedLocationClient: FusedLocationProviderClient? = null

    @Nullable
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (Build.VERSION.SDK_INT >= 26) {
            val CHANNEL_ID = "my_channel_01"
            val channel = NotificationChannel(
                CHANNEL_ID,
                "My Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?)!!.createNotificationChannel(
                channel
            )
            val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("").build()
            startForeground(1, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: called.")
        location
        return START_NOT_STICKY
    }

    // ---------------------------------- LocationRequest ------------------------------------
    // Create the location request to start receiving updates
    private val location:


            // new Google API SDK v11 uses getFusedLocationProviderClient(this)
            // Looper.myLooper tells this to repeat forever until thread is destroyed
            Unit
        private get() {

            // ---------------------------------- LocationRequest ------------------------------------
            // Create the location request to start receiving updates
            val mLocationRequestHighAccuracy = LocationRequest()
            mLocationRequestHighAccuracy.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            mLocationRequestHighAccuracy.interval = UPDATE_INTERVAL
            mLocationRequestHighAccuracy.fastestInterval = FASTEST_INTERVAL


            // new Google API SDK v11 uses getFusedLocationProviderClient(this)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "getLocation: stopping the location service.")
                stopSelf()
                return
            }
            Log.d(TAG, "getLocation: getting location information.")
            mFusedLocationClient!!.requestLocationUpdates(
                mLocationRequestHighAccuracy, object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        Log.d(TAG, "onLocationResult: got location result.")
                        val location: Location? = locationResult.lastLocation
                        if (location != null) {
//                            val user: User =
//                                (ApplicationProvider.getApplicationContext() as UserClient).getUser()
                            val geoPoint =
                                GeoPoint(location.latitude, location.longitude)
                            val driverLocation = DriverLocation(geoPoint, FieldValue.serverTimestamp())
                            saveUserLocation(driverLocation)
                        }
                    }
                },
                Looper.myLooper()
            ) // Looper.myLooper tells this to repeat forever until thread is destroyed
        }

    private fun saveUserLocation(driverLocation: DriverLocation) {
        try {
            val locationRef =
                FirebaseFirestore.getInstance()
                    .collection("DriverLocation")
                    .document("DriverID")

            locationRef.set(driverLocation)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, """onComplete:inserted user location into database.latitude: """ + driverLocation.geo_points!!.latitude +
                                    "\n longitude: " + driverLocation.geo_points.longitude)
                    }
                }
        } catch (e: NullPointerException) {
            Log.e(
                TAG,
                "saveUserLocation: User instance is null, stopping location service."
            )
            Log.e(
                TAG,
                "saveUserLocation: NullPointerException: " + e.message
            )
            stopSelf()
        }
    }

    companion object {
        private const val TAG = "LocationService"
        private const val UPDATE_INTERVAL = 4 * 1000 /* 4 secs */.toLong()
        private const val FASTEST_INTERVAL: Long = 2000 /* 2 sec */
    }
}