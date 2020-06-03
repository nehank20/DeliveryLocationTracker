package com.poilkar.nehank.firebaselocationtracking

import android.Manifest
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.clustering.ClusterManager


class MapsActivity : AppCompatActivity(), OnMapReadyCallback , GoogleMap.OnInfoWindowClickListener{

    private lateinit var mMap: GoogleMap
    lateinit var locationProviderClient: FusedLocationProviderClient
    lateinit var firebaseFirestore: FirebaseFirestore
    lateinit var mGoogleMap: GoogleMap
    lateinit var boundaryMaps: LatLngBounds
    lateinit var userPosition: DriverLocation

    lateinit var mClusterManager: ClusterManager<ClusterMarker?>
    lateinit var mClusterManagerRenderer: ClusterManagerRenderer

    private val mHandler: Handler = Handler()
    private var mRunnable: Runnable? = null
    private val LOCATION_UPDATE_INTERVAL = 3000

    lateinit var clusterMarker : ClusterMarker


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        firebaseFirestore = FirebaseFirestore.getInstance()
        locationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
//        mMap.isMyLocationEnabled = true
        mGoogleMap = googleMap
        mGoogleMap.setOnInfoWindowClickListener(this)


    }


    private fun getLastLocation() {
        Log.d("TAGG", "getLastLocation method called")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        locationProviderClient.lastLocation.addOnCompleteListener {
            if (it.isSuccessful) {
                val location = it.result
                val geoPoints = GeoPoint(location?.latitude!!, location.longitude)
                saveDriverLocation(geoPoints)
                startLocationService()
                Log.d("TAGG", "GEO Points : ${location.latitude} and ${location.longitude}")
            } else {
                Log.d("TAGG", "task unscuccesfull")
            }
        }
    }

    private fun saveDriverLocation(geoPoints: GeoPoint) {
        val driverLocation = DriverLocation(geoPoints, FieldValue.serverTimestamp())
        val documentReference = firebaseFirestore.collection("DriverLocation").document("DriverID")
        documentReference.set(driverLocation).addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d("TAGG", "Location Added")
                addCustomMarkers(geoPoints)
//                setCameraView(geoPoints)
            } else {
                Log.d("TAGG", "Something went wrong")
            }
        }
    }

    private fun setCameraView(geoPoints: GeoPoint) {
        val bottomBoundary = geoPoints.latitude - .1
        val leftBoundary = geoPoints.longitude - .1
        val topBoundary = geoPoints.latitude + .1
        val rightBoundary = geoPoints.longitude + .1
        boundaryMaps =
            LatLngBounds(LatLng(bottomBoundary, leftBoundary), LatLng(topBoundary, rightBoundary))
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundaryMaps, 10))
    }

    override fun onResume() {
        super.onResume()
        getLastLocation()
        startUserLocationsRunnable()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    private fun addCustomMarkers(geoPoints: GeoPoint) {
        if (mGoogleMap != null) {

            if (!this::mClusterManager.isInitialized) {
                mClusterManager = ClusterManager<ClusterMarker?>(this, mGoogleMap)
            }
            if (!this::mClusterManagerRenderer.isInitialized) {
                mClusterManagerRenderer = ClusterManagerRenderer(this, mGoogleMap, mClusterManager)
                mClusterManager.setRenderer(mClusterManagerRenderer)
            }


            try {
                val snippet = "Drivers location"
                val avatar: Int =
                    resources.getIdentifier("ic_car_top_view", "drawable", this.packageName);
                clusterMarker = ClusterMarker(
                    LatLng(geoPoints.latitude, geoPoints.longitude),
                    "Driver",
                    snippet,
                    avatar
                )
                mClusterManager.addItem(clusterMarker)
            } catch (e: NullPointerException) {

            }

            mClusterManager.cluster()
            setCameraView(geoPoints)
        }
    }

    private fun startLocationService() {
        if (!isLocationServiceRunning()) {
            val serviceIntent = Intent(this, LocationService::class.java)
            //        this.startService(serviceIntent);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this@MapsActivity.startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    private fun isLocationServiceRunning(): Boolean {
        val manager =
            getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if ("com.poilkar.nehank.firebaselocationtracking.LocationService" == service.service.className) {
                Log.d("TAGG", "isLocationServiceRunning: location service is already running.")
                return true
            }
        }
        Log.d("TAGG", "isLocationServiceRunning: location service is not running.")
        return false
    }


    private fun startUserLocationsRunnable() {
        Log.d(
            "TAGG",
            "startUserLocationsRunnable: starting runnable for retrieving updated locations."
        )
        mHandler.postDelayed(Runnable {
            retrieveUserLocations()
            mHandler.postDelayed(mRunnable, LOCATION_UPDATE_INTERVAL.toLong())
        }.also { mRunnable = it }, LOCATION_UPDATE_INTERVAL.toLong())
    }

    private fun stopLocationUpdates() {
        mHandler.removeCallbacks(mRunnable)
    }

    private fun retrieveUserLocations() {
        Log.d("TAGG", "retrieveUserLocations: retrieving location of all users in the chatroom.")
        try {

            val userLocationRef: DocumentReference = FirebaseFirestore.getInstance()
                .collection("DriverLocation")
                .document("DriverID")

            userLocationRef.get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val updatedUserLocation: DriverLocation =
                            task.result?.toObject(DriverLocation::class.java)!!

                        // update the location

                        try {
                            val snippet = "Drivers location"
                            val avatar: Int = resources.getIdentifier(
                                "ic_car_top_view",
                                "drawable",
                                this@MapsActivity.packageName
                            )

                            clusterMarker.mPosition = LatLng(updatedUserLocation.geo_points!!.latitude, updatedUserLocation.geo_points.longitude)


                            mClusterManagerRenderer.setUpdateMarker(clusterMarker)

                        } catch (e: java.lang.NullPointerException) {
                            Log.e(
                                "TAGG",
                                "retrieveUserLocations: NullPointerException: " + e.message
                            )
                        }

                    }
                }

        } catch (e: IllegalStateException) {
            Log.e(
                "TAGG",
                "retrieveUserLocations: Fragment was destroyed during Firestore query. Ending query." + e.message
            )
        }
    }

    override fun onInfoWindowClick(marker: Marker?) {
        if (marker != null) {
            if (marker.getSnippet().equals("This is you")) {
                marker.hideInfoWindow()
            } else {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setMessage(marker.snippet)
                    .setCancelable(true)
                    .setPositiveButton("Yes"
                    ) { dialog, id -> dialog.dismiss() }
                    .setNegativeButton("No"
                    ) { dialog, id -> dialog.cancel() }
                val alert: AlertDialog = builder.create()
                alert.show()
            }
        }
    }
}