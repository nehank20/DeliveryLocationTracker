package com.poilkar.nehank.firebaselocationtracking.marker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.ui.IconGenerator
import com.poilkar.nehank.firebaselocationtracking.animate.LatLngInterpolator
import com.poilkar.nehank.firebaselocationtracking.animate.MarkerAnimation
import com.poilkar.nehank.firebaselocationtracking.marker.ClusterMarker


class ClusterManagerRenderer(
    var context: Context, googleMap: GoogleMap?,
    clusterManager: ClusterManager<ClusterMarker?>?
) :
    DefaultClusterRenderer<ClusterMarker>(context, googleMap, clusterManager) {

    private val iconGenerator: IconGenerator = IconGenerator(context.applicationContext)
    private val imageView: ImageView = ImageView(context.applicationContext)

    override fun onBeforeClusterItemRendered(
        item: ClusterMarker,
        markerOptions: MarkerOptions
    ) {
        imageView.setImageResource(item.mIconPicture)

        val circleDrawable: Drawable = context.resources.getDrawable(item.mIconPicture)
        val markerIcon = getMarkerIconFromDrawable(circleDrawable)

        markerOptions
            .icon(markerIcon)
            .title(item.title)
            .snippet(item.snippet)
    }

    override fun shouldRenderAsCluster(cluster: Cluster<ClusterMarker>): Boolean {
        return false
    }

     fun getMarkerIconFromDrawable(drawable: Drawable): BitmapDescriptor? {
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        canvas.setBitmap(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    fun setUpdateMarker(
        deliveryLocation: LatLng,
        clusterMarker: ClusterMarker,
        finalPosition: LatLng
    ) {
        val marker = getMarker(clusterMarker)
        if (marker != null) {


            MarkerAnimation.animateMarkerToGB(deliveryLocation,marker, finalPosition, LatLngInterpolator.Spherical());

//            marker.position = clusterMarker.position

        }
    }


    init {

//        initialize cluster item icon generator
//        markerWidth = context.resources.getDimension(R.dimen.custom_marker_image).toInt()
//        markerHeight = context.resources.getDimension(R.dimen.custom_marker_image).toInt()
//        imageView.setLayoutParams(ViewGroup.LayoutParams(markerWidth, markerHeight))
//        val padding = context.resources.getDimension(R.dimen.custom_marker_padding).toInt()
//        imageView.setPadding(padding, padding, padding, padding)
        iconGenerator.setContentView(imageView)
    }



}
