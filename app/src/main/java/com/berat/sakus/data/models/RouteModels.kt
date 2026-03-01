package com.berat.sakus.data.models

/**
 * "Nasıl Giderim" (How to Go) özelliği için veri modelleri.
 * API: POST https://sbbpublicapi.sakarya.bel.tr/api/v1/Route/how-to-go
 */

// ── Request ──

data class RouteLocation(
    val latitude: Double,
    val longitude: Double
)

data class RouteRequest(
    val from: RouteLocation,
    val to: RouteLocation,
    val time: String // ISO 8601 format: "2026-03-01T19:32:30.398Z"
)

// ── Response ──

data class RouteResponse(
    val data: RouteData?
)

data class RouteData(
    val plan: RoutePlan?
)

data class RoutePlan(
    val itineraries: List<Itinerary>
)

data class Itinerary(
    val startTime: Long,
    val endTime: Long,
    val duration: Int, // saniye
    val walkDistance: Double, // metre
    val transfer: Int, // API "transfer" döndürüyor
    val legs: List<Leg>
)

data class Leg(
    val mode: String, // "WALK" veya "BUS"
    val routeCode: String?,
    val from: Stop,
    val to: Stop,
    val duration: Int, // saniye
    val distance: Double // metre
)

data class Stop(
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    val stopCode: String?, // durak numarası (örn: "1521")
    val stopId: String? // durak ID (örn: "12:263")
)
