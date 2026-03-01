package com.berat.sakus.data

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

/**
 * DurakBilgisi'ni harita clustering için ClusterItem'a saran sınıf.
 */
data class DurakClusterItem(
    val durak: DurakBilgisi
) : ClusterItem {
    override fun getPosition(): LatLng = LatLng(durak.lat, durak.lng)
    override fun getTitle(): String? = durak.durakAdi
    override fun getSnippet(): String? = "Durak No: ${durak.durakId}"
    override fun getZIndex(): Float? = null
}
