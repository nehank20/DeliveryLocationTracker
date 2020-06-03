package com.poilkar.nehank.firebaselocationtracking

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

class ClusterMarker(position: LatLng, title: String, snippet: String, iconPicture: Int) : ClusterItem {

    var mPosition = position
    var mTitle = title
    var mSnippet = snippet
    var mIconPicture = iconPicture

    override fun getSnippet(): String? {
        return mSnippet
    }

    override fun getTitle(): String? {
        return mTitle
    }

    override fun getPosition(): LatLng {
        return mPosition
    }
}