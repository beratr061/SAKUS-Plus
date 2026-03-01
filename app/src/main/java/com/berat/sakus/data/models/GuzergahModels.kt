package com.berat.sakus.data.models

import com.google.gson.JsonArray
import com.google.gson.JsonObject

data class YonBilgisi(
    val id: Int,
    val ad: String,
    val startLocation: String = "",
    val endLocation: String = ""
) {
    companion object {
        fun fromJson(json: JsonObject): YonBilgisi {
            return YonBilgisi(
                id = if (json.has("directionId") && !json.get("directionId").isJsonNull) json.get("directionId").asInt else 0,
                ad = if (json.has("directionName") && !json.get("directionName").isJsonNull) json.get("directionName").asString else "",
                startLocation = if (json.has("startLocation") && !json.get("startLocation").isJsonNull) json.get("startLocation").asString else "",
                endLocation = if (json.has("endLocation") && !json.get("endLocation").isJsonNull) json.get("endLocation").asString else ""
            )
        }
    }
}

@androidx.compose.runtime.Stable
data class DurakBilgisi(
    val durakId: Int,
    val durakAdi: String,
    val siraNo: Int,
    val lat: Double,
    val lng: Double,
    val yon: Int
) {
    companion object {
        fun fromJson(json: JsonObject, defaultYon: Int = 0): DurakBilgisi {
            var latitude = 0.0
            var longitude = 0.0

            val coordsArray = if (json.has("busStopGeometry") && json.getAsJsonObject("busStopGeometry").has("coordinates")) {
                json.getAsJsonObject("busStopGeometry").getAsJsonArray("coordinates")
            } else if (json.has("location") && json.getAsJsonObject("location").has("coordinates")) {
                json.getAsJsonObject("location").getAsJsonArray("coordinates")
            } else null

            if (coordsArray != null && coordsArray.size() >= 2) {
                longitude = coordsArray.get(0).asDouble
                latitude = coordsArray.get(1).asDouble
            } else if (json.has("latitude") && json.has("longitude")) {
                latitude = json.get("latitude").asDouble
                longitude = json.get("longitude").asDouble
            } else if (json.has("lat") && json.has("lng")) {
                latitude = json.get("lat").asDouble
                longitude = json.get("lng").asDouble
            }

            return DurakBilgisi(
                durakId = if (json.has("stationId") && !json.get("stationId").isJsonNull) json.get("stationId").asInt 
                          else if (json.has("id") && !json.get("id").isJsonNull) json.get("id").asInt else 0,
                durakAdi = run {
                    val nameFields = listOf("stopName", "name", "stationName", "busStopName", "title", "label", "displayName", "adi", "ad", "isim", "description")
                    var raw = nameFields.firstOrNull { json.has(it) && !json.get(it).isJsonNull }
                        ?.let { json.get(it).asString } ?: ""
                    if (raw.isBlank()) {
                        json.entrySet().firstOrNull { (_, v) -> v.isJsonPrimitive && v.asJsonPrimitive.isString && v.asString.length in 2..150 }
                            ?.let { raw = it.value.asJsonPrimitive.asString }
                    }
                    raw.trim().ifEmpty {
                        val id = if (json.has("stationId") && !json.get("stationId").isJsonNull) json.get("stationId").asInt
                            else if (json.has("id") && !json.get("id").isJsonNull) json.get("id").asInt else 0
                        "Durak $id"
                    }
                },
                siraNo = if (json.has("order") && !json.get("order").isJsonNull) json.get("order").asInt
                         else if (json.has("sequence") && !json.get("sequence").isJsonNull) json.get("sequence").asInt else 0,
                lat = latitude,
                lng = longitude,
                yon = if (json.has("direction") && !json.get("direction").isJsonNull) json.get("direction").asInt else defaultYon
            )
        }
    }
}

data class HatGuzergahBilgisi(
    val guzergahKoordinatlari: List<List<Double>>,
    val guzergahlar: Map<Int, List<List<Double>>>,
    val duraklar: List<DurakBilgisi>,
    val yonler: List<YonBilgisi>
) {
    companion object {
        fun fromJson(json: JsonObject): HatGuzergahBilgisi {
            val allCoords = mutableListOf<List<Double>>()
            val directionCoords = mutableMapOf<Int, MutableList<List<Double>>>()
            val stopsList = mutableListOf<DurakBilgisi>()
            val directionsList = mutableListOf<YonBilgisi>()

            if (json.has("routes") && json.get("routes").isJsonArray) {
                val routes = json.getAsJsonArray("routes")
                for (i in 0 until routes.size()) {
                    val routeElement = routes.get(i)
                    if (!routeElement.isJsonObject) continue
                    val route = routeElement.asJsonObject
                    val routeCoords = mutableListOf<List<Double>>()

                    if (route.has("routeGeometry") && route.getAsJsonObject("routeGeometry").has("coordinates")) {
                        val rawCoords = route.getAsJsonObject("routeGeometry").getAsJsonArray("coordinates")
                        parseCoordinates(rawCoords, routeCoords, allCoords)
                    }

                    directionCoords[i] = routeCoords

                    if (route.has("busStops") && route.get("busStops").isJsonArray) {
                        val stops = route.getAsJsonArray("busStops")
                        for (stopElement in stops) {
                            if (stopElement.isJsonObject) {
                                stopsList.add(DurakBilgisi.fromJson(stopElement.asJsonObject, defaultYon = i))
                            }
                        }
                    }

                    val routeName = if (route.has("routeName") && !route.get("routeName").isJsonNull) route.get("routeName").asString else ""
                    val sl = if (route.has("startLocation") && !route.get("startLocation").isJsonNull) route.get("startLocation").asString else ""
                    val el = if (route.has("endLocation") && !route.get("endLocation").isJsonNull) route.get("endLocation").asString else ""
                    directionsList.add(YonBilgisi(id = i, ad = routeName, startLocation = sl, endLocation = el))
                }
            }

            if (directionsList.isEmpty()) {
                if (json.has("directions") && json.get("directions").isJsonArray) {
                    val directions = json.getAsJsonArray("directions")
                    for (dir in directions) {
                        if (dir.isJsonObject) directionsList.add(YonBilgisi.fromJson(dir.asJsonObject))
                    }
                } else {
                    directionsList.add(YonBilgisi(id = 0, ad = "Gidiş"))
                    directionsList.add(YonBilgisi(id = 1, ad = "Dönüş"))
                }
            }

            return HatGuzergahBilgisi(
                guzergahKoordinatlari = allCoords,
                guzergahlar = directionCoords,
                duraklar = stopsList,
                yonler = directionsList
            )
        }

        private fun parseCoordinates(rawCoords: JsonArray, routeCoords: MutableList<List<Double>>, allCoords: MutableList<List<Double>>) {
            for (element in rawCoords) {
                if (element.isJsonArray) {
                    val arr = element.asJsonArray
                    if (arr.size() >= 2 && arr.get(0).isJsonPrimitive && arr.get(0).asJsonPrimitive.isNumber) {
                        val point = listOf(
                            arr.get(1).asDouble, // lat
                            arr.get(0).asDouble  // lng
                        )
                        routeCoords.add(point)
                        allCoords.add(point)
                    } else {
                        parseCoordinates(arr, routeCoords, allCoords)
                    }
                }
            }
        }
    }
}
