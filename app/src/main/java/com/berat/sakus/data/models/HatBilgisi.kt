package com.berat.sakus.data.models

import com.berat.sakus.data.SbbApiServisi
import com.google.gson.JsonObject

@androidx.compose.runtime.Stable
data class HatBilgisi(
    val id: Int,
    val ad: String,
    val hatNumarasi: String,
    val aracTipAdi: String,
    val aracTipAciklama: String,
    val aracTipRenk: String,
    val aracTipId: Int,
    val asisId: Int?,
    val slug: String
) {
    val kategori: String
        get() {
            if (aracTipId == SbbApiServisi.BUS_TYPE_METROBUS) return "metrobus"
            if (aracTipId == SbbApiServisi.BUS_TYPE_ADARAY) return "adaray"
            if (aracTipId == SbbApiServisi.BUS_TYPE_TRAMVAY) return "tramvay"
            if (aracTipId == SbbApiServisi.BUS_TYPE_OZEL_HALK) return "ozel_halk"
            if (aracTipId == SbbApiServisi.BUS_TYPE_BELEDIYE) return "belediye"
            if (aracTipId == SbbApiServisi.BUS_TYPE_BELEDIYE_ESKI) return "belediye"
            if (aracTipId == SbbApiServisi.BUS_TYPE_EUTS_OZEL_HALK) return "ozel_halk"
            val desc = aracTipAciklama.uppercase()
            if (desc.contains("METROBÜS") || desc.contains("METROBUS")) return "metrobus"
            if (desc.contains("BELEDİYE")) return "belediye"
            if (desc.contains("ÖZEL HALK") || desc.contains("OZEL HALK")) return "ozel_halk"
            if (desc.contains("TAKSİ") || desc.contains("TAKSI")) return "taksi_dolmus"
            if (desc.contains("MİNİBÜS") || desc.contains("MINIBUS")) return "minibus"
            if (desc.contains("TRAMVAY")) return "tramvay"
            if (desc.contains("ADARAY") || desc.contains("RAY")) return "adaray"
            return "diger"
        }

    companion object {
        fun fromJson(json: JsonObject): HatBilgisi {
            return HatBilgisi(
                id = if (json.has("id") && !json.get("id").isJsonNull) json.get("id").asInt else 0,
                ad = if (json.has("name") && !json.get("name").isJsonNull) json.get("name").asString else "",
                hatNumarasi = if (json.has("lineNumber") && !json.get("lineNumber").isJsonNull) json.get("lineNumber").asString else "",
                aracTipAdi = if (json.has("busTypeName") && !json.get("busTypeName").isJsonNull) json.get("busTypeName").asString else "",
                aracTipAciklama = if (json.has("busTypeDescription") && !json.get("busTypeDescription").isJsonNull) json.get("busTypeDescription").asString else "",
                aracTipRenk = if (json.has("busTypeColor") && !json.get("busTypeColor").isJsonNull) json.get("busTypeColor").asString else "#68bd9c",
                aracTipId = if (json.has("busTypeId") && !json.get("busTypeId").isJsonNull) json.get("busTypeId").asInt else 0,
                asisId = if (json.has("asisIntegrationId") && !json.get("asisIntegrationId").isJsonNull) json.get("asisIntegrationId").asInt else null,
                slug = if (json.has("slug") && !json.get("slug").isJsonNull) json.get("slug").asString else ""
            )
        }
    }
}
