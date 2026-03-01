package com.berat.sakus.data.models

data class NearLineResponse(
    val value: List<NearLine>,
    val Count: Int
)

data class NearLine(
    val lineId: Int,
    val typeValueId: Int,
    val typeValueName: String,
    val typeValueColor: String,
    val lineName: String,
    val lineNumber: String,
    val ekentLineIntegrationId: Int?,
    val nearestDistanceMeters: Double,
    val routes: List<NearLineRoute>
)

data class NearLineRoute(
    val routeId: Int,
    val routeName: String,
    val startLocation: String,
    val endLocation: String,
    val routeTypeId: Int,
    val distanceMeters: Double
)
