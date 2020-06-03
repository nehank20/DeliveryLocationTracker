package com.poilkar.nehank.firebaselocationtracking.model

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp

class DriverLocation (val geo_points: GeoPoint? = null , val timestamp: Any ?= null ){
}