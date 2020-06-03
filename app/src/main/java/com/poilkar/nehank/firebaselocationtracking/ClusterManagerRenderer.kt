package com.poilkar.nehank.firebaselocationtracking

import android.R
import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.ui.IconGenerator


class ClusterManagerRenderer(
    context: Context, googleMap: GoogleMap?,
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
        val icon = iconGenerator.makeIcon()
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon)).title(item.title).snippet(item.snippet)
    }

    override fun shouldRenderAsCluster(cluster: Cluster<ClusterMarker>): Boolean {
        return false
    }

    fun setUpdateMarker(clusterMarker: ClusterMarker) {
        val marker = getMarker(clusterMarker)
        if(marker != null){
            marker.position = clusterMarker.position
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
