package com.poilkar.nehank.firebaselocationtracking.ui

import android.Manifest
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.clustering.ClusterManager
import com.poilkar.nehank.firebaselocationtracking.R
import com.poilkar.nehank.firebaselocationtracking.marker.ClusterManagerRenderer
import com.poilkar.nehank.firebaselocationtracking.marker.ClusterMarker
import com.poilkar.nehank.firebaselocationtracking.model.DriverLocation
import com.poilkar.nehank.firebaselocationtracking.services.LocationService


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


    lateinit var deliveryLocation: LatLng
//    lateinit var mGeoApiContext: GeoApiContext




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



        deliveryLocation =
            LatLng(19.094295, 72.837567)
        googleMap.addMarker(
            MarkerOptions().position(deliveryLocation)
                .title("Your Location")
        )



//        if(!this::mGeoApiContext.isInitialized){
//            mGeoApiContext = GeoApiContext.Builder()
//                .apiKey(resources.getString(R.string.google_maps_key))
//                .build()
//        }


        mGoogleMap = googleMap
        mGoogleMap.setOnInfoWindowClickListener(this@MapsActivity)

//        mGoogleMap.setOnMarkerClickListener(object: GoogleMap.OnMarkerClickListener{
//            override fun onMarkerClick(marker: Marker?): Boolean {
//                if (marker != null) {
//                    if (marker.getSnippet().equals("This is you")) {
//                        marker.hideInfoWindow()
//                    } else {
//                        val builder: AlertDialog.Builder = AlertDialog.Builder(this@MapsActivity)
//                        builder.setMessage(marker.snippet)
//                            .setCancelable(true)
//                            .setPositiveButton("Yes"
//                            ) { dialog, id -> dialog.dismiss() }
//                            .setNegativeButton("No"
//                            ) { dialog, id -> dialog.cancel() }
//                        val alert: AlertDialog = builder.create()
//                        alert.show()
//                    }
//                }else{
//                    Log.d("TAGG","marker null")
//                }
//                return true
//            }
//
//        })




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
//                startLocationService()
                Log.d("TAGG", "GEO Points : ${location.latitude} and ${location.longitude}")
            } else {
                Log.d("TAGG", "task unscuccesfull")
            }
        }
    }

    private fun saveDriverLocation(geoPoints: GeoPoint) {
        val driverLocation =
            DriverLocation(
                geoPoints,
                FieldValue.serverTimestamp()
            )
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

    private fun setCameraView(
        riderGeoPoint: GeoPoint,
        deliveryGeoPoints: LatLng
    ) {


        //determine where is rider w.r.t delivery location
        val topBoundary: Double?
        val rightBoundary: Double?
        val bottomBoundary: Double?
        val leftBoundary: Double?

        //rider is above deliveryPT
        if(riderGeoPoint.latitude > deliveryGeoPoints.latitude){
            topBoundary = riderGeoPoint.latitude
            bottomBoundary = deliveryGeoPoints.latitude

        }
        //rider is above deliveryPT
        else if(riderGeoPoint.latitude < deliveryGeoPoints.latitude){
            topBoundary = deliveryGeoPoints.latitude
            bottomBoundary = riderGeoPoint.latitude
        }
        // both on same point
        else{

            //default first if else loop

            topBoundary = riderGeoPoint.latitude
            bottomBoundary = deliveryGeoPoints.latitude
        }




        //rider is to right of deliverPt
        if(riderGeoPoint.longitude > deliveryGeoPoints.longitude){
            rightBoundary = riderGeoPoint.longitude
            leftBoundary = deliveryGeoPoints.longitude
        }

        //rider is to left of deliverPt
        else if(riderGeoPoint.longitude < deliveryGeoPoints.longitude){
            rightBoundary =  deliveryGeoPoints.longitude
            leftBoundary =  riderGeoPoint.longitude
        }
        else{

            //default first if else loop

            rightBoundary = riderGeoPoint.longitude
            leftBoundary = deliveryGeoPoints.longitude
        }



//        topBoundary = riderGeoPoint.latitude
//        rightBoundary = riderGeoPoint.longitude
//
//        bottomBoundary = deliveryGeoPoints.latitude
//        leftBoundary = deliveryGeoPoints.longitude


        boundaryMaps = LatLngBounds(LatLng(bottomBoundary, leftBoundary), LatLng(topBoundary, rightBoundary))

        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundaryMaps, 100)) // padding from the edges
//        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(geoPoints.latitude, geoPoints.longitude), 14.0f))
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
                mClusterManagerRenderer =
                    ClusterManagerRenderer(
                        this,
                        mGoogleMap,
                        mClusterManager
                    )
                mClusterManager.setRenderer(mClusterManagerRenderer)
            }


            try {
                val snippet = "Drivers location"
                val avatar: Int =
                    resources.getIdentifier("ic_car_top_view", "drawable", this.packageName);
                clusterMarker =
                    ClusterMarker(
                        LatLng(geoPoints.latitude, geoPoints.longitude),
                        "Driver",
                        snippet,
                        avatar
                    )
                mClusterManager.addItem(clusterMarker)
            } catch (e: NullPointerException) {

            }

            mClusterManager.cluster()
//            calculateDirections(deliveryLocation)
            setCameraView(geoPoints,deliveryLocation)
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
            if ("com.poilkar.nehank.firebaselocationtracking.services.LocationService" == service.service.className) {
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

                        try {
                            val snippet = "Drivers location"
                            val avatar: Int = resources.getIdentifier(
                                "ic_car_top_view",
                                "drawable",
                                this@MapsActivity.packageName
                            )
                            clusterMarker.mPosition = LatLng(updatedUserLocation.geo_points!!.latitude, updatedUserLocation.geo_points.longitude)
                            mClusterManagerRenderer.setUpdateMarker(deliveryLocation, clusterMarker, LatLng(updatedUserLocation.geo_points!!.latitude, updatedUserLocation.geo_points.longitude))
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
        }else{
            Log.d("TAGG","marker null")
        }
    }


    fun animateMarker(
        marker: ClusterMarker,
        toPosition: LatLng,
        hideMarker: Boolean
    ) {
        val handler = Handler()
        val start: Long = SystemClock.uptimeMillis()
        val proj: Projection = mGoogleMap.getProjection()
        val startPoint: Point = proj.toScreenLocation(marker.position)
        val startLatLng: LatLng =
            proj.fromScreenLocation(startPoint)
        val duration: Long = 500
        val interpolator: LinearInterpolator = LinearInterpolator()
        handler.post(object : Runnable {
            override fun run() {
                val elapsed: Long = SystemClock.uptimeMillis() - start
                val t: Float = interpolator.getInterpolation(
                    elapsed.toFloat()
                            / duration
                )
                val lng = t * toPosition.longitude + (1 - t)* startLatLng.longitude
                val lat = t * toPosition.latitude + (1 - t)* startLatLng.latitude
                marker.mPosition = LatLng(lat, lng)
                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16)
                } else {
//                    if (hideMarker) {
//                        marker.isVisible = false
//                    } else {
//                        marker.isVisible = true
//                    }
                }
            }
        })
    }


//    private fun calculateDirections(latLng: LatLng) {
//        Log.d("TAGG", "calculateDirections: calculating directions.")
//        val destination = com.google.maps.model.LatLng(
//            latLng.latitude,
//            latLng.longitude
//        )
//        val directions = DirectionsApiRequest(mGeoApiContext)
//        directions.alternatives(true)
//        directions.origin(
//            com.google.maps.model.LatLng(
//                clusterMarker.mPosition.latitude,
//                clusterMarker.mPosition.longitude
//            )
//        )
//
//        directions.destination(destination).setCallback(object :
//            PendingResult.Callback<DirectionsResult?> {
//            override fun onResult(result: DirectionsResult?) {
////                Log.d(TAG, "calculateDirections: routes: " + result.routes[0].toString());
////                Log.d(TAG, "calculateDirections: duration: " + result.routes[0].legs[0].duration);
////                Log.d(TAG, "calculateDirections: distance: " + result.routes[0].legs[0].distance);
////                Log.d(TAG, "calculateDirections: geocodedWayPoints: " + result.geocodedWaypoints[0].toString());
//                Log.d(
//                    "TAGG",
//                    "onResult: successfully retrieved directions."
//                )
////                addPolylinesToMap(result)
//            }
//
//            override fun onFailure(e: Throwable) {
//                Log.e(
//                    "TAGG",
//                    "calculateDirections: Failed to get directions: " + e.message
//                )
//            }
//        })
//    }
}
