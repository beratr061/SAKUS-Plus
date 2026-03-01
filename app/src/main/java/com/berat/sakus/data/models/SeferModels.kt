package com.berat.sakus.data.models

import com.google.gson.JsonObject

data class HatSeferBilgisi(
    val lineId: Int,
    val hatAdi: String,
    val hatNumarasi: String,
    val seferler: List<GuzergahSefer>
) {
    companion object {
        fun fromJson(json: JsonObject): HatSeferBilgisi {
            val schedules = if (json.has("schedules") && json.get("schedules").isJsonArray) {
                json.getAsJsonArray("schedules").mapNotNull { if (it.isJsonObject) GuzergahSefer.fromJson(it.asJsonObject) else null }
            } else emptyList()

            return HatSeferBilgisi(
                lineId = if (json.has("lineId") && !json.get("lineId").isJsonNull) json.get("lineId").asInt else 0,
                hatAdi = if (json.has("lineName") && !json.get("lineName").isJsonNull) json.get("lineName").asString else "",
                hatNumarasi = if (json.has("lineNumber") && !json.get("lineNumber").isJsonNull) json.get("lineNumber").asString else "",
                seferler = schedules
            )
        }
    }
}

data class GuzergahSefer(
    val guzergahId: Int,
    val guzergahAdi: String,
    val detaylar: List<SeferDetay>,
    /** API'den gelen yön: 0=Gidiş, 1=Dönüş. Yoksa routeId % 2 ile hesaplanır. */
    val yon: Int = 0
) {
    companion object {
        fun fromJson(json: JsonObject): GuzergahSefer {
            val details = if (json.has("routeDetail") && json.get("routeDetail").isJsonArray) {
                json.getAsJsonArray("routeDetail").mapNotNull { if (it.isJsonObject) SeferDetay.fromJson(it.asJsonObject) else null }
            } else emptyList()

            val routeId = if (json.has("routeId") && !json.get("routeId").isJsonNull) json.get("routeId").asInt else 0
            val yon = if (json.has("direction") && !json.get("direction").isJsonNull) {
                json.get("direction").asInt
            } else if (json.has("directionId") && !json.get("directionId").isJsonNull) {
                json.get("directionId").asInt
            } else {
                if (routeId % 2 == 0) 0 else 1
            }

            return GuzergahSefer(
                guzergahId = routeId,
                guzergahAdi = if (json.has("routeName") && !json.get("routeName").isJsonNull) json.get("routeName").asString else "",
                detaylar = details,
                yon = yon.coerceIn(0, 1)
            )
        }
    }
}

data class SeferDetay(
    val baslangicSaat: String,
    val bitisSaat: String,
    val seferNo: Int,
    val aciklama: String?
) {
    companion object {
        fun formatTime(time: String): String {
            if (time.isEmpty()) return ""
            val parts = time.split(":")
            if (parts.size >= 2) return "${parts[0]}:${parts[1]}"
            return time
        }

        fun fromJson(json: JsonObject): SeferDetay {
            return SeferDetay(
                baslangicSaat = formatTime(if (json.has("startTime") && !json.get("startTime").isJsonNull) json.get("startTime").asString else ""),
                bitisSaat = formatTime(if (json.has("endTime") && !json.get("endTime").isJsonNull) json.get("endTime").asString else ""),
                seferNo = if (json.has("tripNumber") && !json.get("tripNumber").isJsonNull) json.get("tripNumber").asInt else 0,
                aciklama = if (json.has("description") && !json.get("description").isJsonNull) json.get("description").asString else null
            )
        }
    }
}
