package com.berat.sakus.data.models

import com.google.gson.JsonObject

data class TarifeBilgisi(
    val tarifeTipleri: List<TarifeTipi>,
    val gruplar: List<TarifeGrubu>
) {
    companion object {
        fun fromJson(json: JsonObject): TarifeBilgisi {
            val tariffs = if (json.has("tariffList") && json.get("tariffList").isJsonArray) {
                json.getAsJsonArray("tariffList").mapNotNull { if (it.isJsonObject) TarifeTipi.fromJson(it.asJsonObject) else null }
            } else emptyList()

            val groups = if (json.has("groups") && json.get("groups").isJsonArray) {
                json.getAsJsonArray("groups").mapNotNull { if (it.isJsonObject) TarifeGrubu.fromJson(it.asJsonObject) else null }
            } else emptyList()

            return TarifeBilgisi(tarifeTipleri = tariffs, gruplar = groups)
        }
    }
}

data class TarifeTipi(
    val id: Int,
    val tipAdi: String
) {
    companion object {
        fun fromJson(json: JsonObject): TarifeTipi {
            return TarifeTipi(
                id = if (json.has("lineFareTypeId") && !json.get("lineFareTypeId").isJsonNull) json.get("lineFareTypeId").asInt else 0,
                tipAdi = if (json.has("typeName") && !json.get("typeName").isJsonNull) json.get("typeName").asString else ""
            )
        }
    }
}

data class TarifeGrubu(
    val ad: String,
    val guzergahlar: List<TarifeGuzergah>
) {
    companion object {
        fun fromJson(json: JsonObject): TarifeGrubu {
            val routes = if (json.has("routes") && json.get("routes").isJsonArray) {
                json.getAsJsonArray("routes").mapNotNull { if (it.isJsonObject) TarifeGuzergah.fromJson(it.asJsonObject) else null }
            } else emptyList()

            return TarifeGrubu(
                ad = if (json.has("name") && !json.get("name").isJsonNull) json.get("name").asString else "",
                guzergahlar = routes
            )
        }
    }
}

data class TarifeGuzergah(
    val guzergahAdi: String,
    val temelUcret: Double,
    val ucretler: List<TarifeUcret>
) {
    companion object {
        fun fromJson(json: JsonObject): TarifeGuzergah {
            val tariffs = if (json.has("tariffs") && json.get("tariffs").isJsonArray) {
                json.getAsJsonArray("tariffs").mapNotNull { if (it.isJsonObject) TarifeUcret.fromJson(it.asJsonObject) else null }
            } else emptyList()

            return TarifeGuzergah(
                guzergahAdi = if (json.has("routeName") && !json.get("routeName").isJsonNull) json.get("routeName").asString else "",
                temelUcret = if (json.has("baseFare") && !json.get("baseFare").isJsonNull) json.get("baseFare").asDouble else 0.0,
                ucretler = tariffs
            )
        }
    }
}

data class TarifeUcret(
    val tarifeTipId: Int,
    val sonUcret: Double
) {
    companion object {
        fun fromJson(json: JsonObject): TarifeUcret {
            return TarifeUcret(
                tarifeTipId = if (json.has("lineFareTypeId") && !json.get("lineFareTypeId").isJsonNull) json.get("lineFareTypeId").asInt else 0,
                sonUcret = if (json.has("finalFare") && !json.get("finalFare").isJsonNull) json.get("finalFare").asDouble else 0.0
            )
        }
    }
}
