package com.berat.sakus.data.models

import com.google.gson.JsonObject

@androidx.compose.runtime.Stable
data class NewsItem(
    val id: Int,
    val title: String,
    val newsMainImage: String?,
    val createdDate: String,
    val isHomePageNews: Boolean,
    val foreword: String?,
    val displayOrder: Int
) {
    companion object {
        fun fromJson(json: JsonObject): NewsItem {
            return NewsItem(
                id = if (json.has("id") && !json.get("id").isJsonNull) json.get("id").asInt else 0,
                title = if (json.has("title") && !json.get("title").isJsonNull) json.get("title").asString else "",
                newsMainImage = if (json.has("newsMainImage") && !json.get("newsMainImage").isJsonNull) json.get("newsMainImage").asString else null,
                createdDate = if (json.has("createdDate") && !json.get("createdDate").isJsonNull) json.get("createdDate").asString else "",
                isHomePageNews = if (json.has("isHomePageNews") && !json.get("isHomePageNews").isJsonNull) json.get("isHomePageNews").asBoolean else false,
                foreword = if (json.has("foreword") && !json.get("foreword").isJsonNull) json.get("foreword").asString else null,
                displayOrder = if (json.has("displayOrder") && !json.get("displayOrder").isJsonNull) json.get("displayOrder").asInt else 0
            )
        }
    }
}
