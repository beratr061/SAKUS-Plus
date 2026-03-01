package com.berat.sakus.data.models

import com.google.gson.JsonObject

@androidx.compose.runtime.Stable
data class AracKonumu(
    val plaka: String,
    val hatNo: String,
    val lat: Double,
    val lng: Double,
    val durum: String,
    val sonrakiDurak: String,
    val sonrakiDurakMesafe: Double,
    val mevcutDurak: String,
    val hiz: Int,
    val yon: Int,
    val baslik: Double,
    val etaSaniye: Double,
    val guzergahAdi: String,
    val baslangicYer: String,
    val bitisYer: String,
    val aracNumarasi: Int,
    val guzergahId: Int?
) {
    val hizFormati: String get() = "$hiz km/h"

    val durumTr: String get() = when (durum.uppercase()) {
        "AT_STOP" -> "Durakta"
        "IN_TRAFFIC" -> "Trafikte"
        "MOVING", "IN_MOTION" -> "Hareket Halinde"
        "OUT_OF_SERVICE" -> "Hizmet Dışı"
        "IDLE" -> "Beklemede"
        "STOPPED" -> "Durdu"
        "NOT_REPORTING" -> "Sinyal Yok"
        else -> durum.replace("_", " ")
            .lowercase()
            .replaceFirstChar { it.uppercase() }
    }

    val aktifMi: Boolean
        get() = durum.uppercase() != "OUT_OF_SERVICE" && durum.uppercase() != "IDLE" && lat != 0.0 && lng != 0.0

    companion object {
        fun fromJson(json: JsonObject): AracKonumu {
            var latitude = 0.0
            var longitude = 0.0
            if (json.has("location") && json.get("location").isJsonObject) {
                val loc = json.getAsJsonObject("location")
                if (loc.has("coordinates") && loc.get("coordinates").isJsonArray) {
                    val coords = loc.getAsJsonArray("coordinates")
                    if (coords.size() >= 2) {
                        longitude = coords.get(0).asDouble
                        latitude = coords.get(1).asDouble
                    }
                }
            }

            val routeId = if (json.has("routeId") && !json.get("routeId").isJsonNull) json.get("routeId").asInt else null
            
            val direction = if (json.has("direction") && !json.get("direction").isJsonNull) {
                json.get("direction").asInt
            } else if (routeId != null) {
                if (routeId % 2 == 0) 1 else 0
            } else 0

            return AracKonumu(
                plaka = if (json.has("plate") && !json.get("plate").isJsonNull) json.get("plate").asString 
                        else if (json.has("busNumber") && !json.get("busNumber").isJsonNull) json.get("busNumber").asString else "",
                hatNo = if (json.has("lineNumber") && !json.get("lineNumber").isJsonNull) json.get("lineNumber").asString else "",
                lat = latitude,
                lng = longitude,
                durum = if (json.has("status") && !json.get("status").isJsonNull) json.get("status").asString else "",
                sonrakiDurak = if (json.has("nextStopName") && !json.get("nextStopName").isJsonNull) json.get("nextStopName").asString else "",
                sonrakiDurakMesafe = if (json.has("distNextStopMeter") && !json.get("distNextStopMeter").isJsonNull) json.get("distNextStopMeter").asDouble else 0.0,
                mevcutDurak = if (json.has("atStopName") && !json.get("atStopName").isJsonNull) json.get("atStopName").asString else "",
                hiz = if (json.has("speed") && !json.get("speed").isJsonNull) json.get("speed").asInt else 0,
                yon = direction,
                baslik = if (json.has("headingDegree") && !json.get("headingDegree").isJsonNull) json.get("headingDegree").asDouble else 0.0,
                etaSaniye = if (json.has("etaS") && !json.get("etaS").isJsonNull) json.get("etaS").asDouble else 0.0,
                guzergahAdi = if (json.has("routeName") && !json.get("routeName").isJsonNull) json.get("routeName").asString else "",
                baslangicYer = if (json.has("startLocation") && !json.get("startLocation").isJsonNull) json.get("startLocation").asString else "",
                bitisYer = if (json.has("endLocation") && !json.get("endLocation").isJsonNull) json.get("endLocation").asString else "",
                aracNumarasi = if (json.has("busNumber") && !json.get("busNumber").isJsonNull) {
                    try { json.get("busNumber").asInt } catch (_: Exception) { json.get("busNumber").asString.filter { it.isDigit() }.toIntOrNull() ?: 0 }
                } else 0,
                guzergahId = routeId
            )
        }
    }
}
