package com.berat.sakus.data.models

import com.google.gson.JsonObject

@androidx.compose.runtime.Stable
data class Duyuru(
    val id: Int,
    val baslik: String,
    val aciklama: String,
    val icerik: String,
    val baslangicTarih: String,
    val bitisTarih: String,
    val hatId: Int?,
    val hatAdi: String?,
    val hatNumarasi: String?,
    val kategoriAdi: String?,
    val renk: String?
) {
    val duzMetin: String
        get() {
            return icerik
                .replace(Regex("<[^>]*>"), "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .trim()
        }

    companion object {
        fun fromJson(json: JsonObject): Duyuru {
            return Duyuru(
                id = if (json.has("id") && !json.get("id").isJsonNull) json.get("id").asInt else 0,
                baslik = if (json.has("title") && !json.get("title").isJsonNull) json.get("title").asString else "",
                aciklama = if (json.has("description") && !json.get("description").isJsonNull) json.get("description").asString else "",
                icerik = if (json.has("content") && !json.get("content").isJsonNull) json.get("content").asString else "",
                baslangicTarih = if (json.has("startDate") && !json.get("startDate").isJsonNull) json.get("startDate").asString else "",
                bitisTarih = if (json.has("endDate") && !json.get("endDate").isJsonNull) json.get("endDate").asString else "",
                hatId = if (json.has("lineId") && !json.get("lineId").isJsonNull) json.get("lineId").asInt else null,
                hatAdi = if (json.has("lineName") && !json.get("lineName").isJsonNull) json.get("lineName").asString else null,
                hatNumarasi = if (json.has("lineNumber") && !json.get("lineNumber").isJsonNull) json.get("lineNumber").asString else null,
                kategoriAdi = if (json.has("categoryName") && !json.get("categoryName").isJsonNull) json.get("categoryName").asString else null,
                renk = if (json.has("color") && !json.get("color").isJsonNull) json.get("color").asString else null
            )
        }
    }
}
