package com.berat.sakus.data.repository

import android.util.Log
import com.berat.sakus.data.models.*
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * "Nasıl Giderim" güzergah arama repository'si.
 * POST /api/v1/Route/how-to-go endpoint'ini kullanır.
 */
class RouteRepository {

    companion object {
        private const val TAG = "RouteRepository"
        private const val BASE_URL = "https://sbbpublicapi.sakarya.bel.tr/"
        private const val ROUTE_ENDPOINT = "api/v1/Route/how-to-go"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .proxy(java.net.Proxy.NO_PROXY)
        .build()

    private val gson = Gson()

    private fun currentIsoTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date())
    }

    /**
     * İki nokta arasındaki güzergah seçeneklerini API'den getirir.
     * @param from Başlangıç noktası
     * @param to Bitiş noktası
     * @return Itinerary listesi veya boş liste
     */
    suspend fun findRoute(from: RouteLocation, to: RouteLocation): Result<List<Itinerary>> =
        withContext(Dispatchers.IO) {
            try {
                val requestBody = RouteRequest(
                    from = RouteLocation(from.latitude, from.longitude),
                    to = RouteLocation(to.latitude, to.longitude),
                    time = currentIsoTime()
                )

                val jsonBody = gson.toJson(requestBody)
                Log.d(TAG, "İstek gönderiliyor: $jsonBody")

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = jsonBody.toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("$BASE_URL$ROUTE_ENDPOINT")
                    .post(body)
                    .header("Content-Type", "application/json")
                    .header("Origin", "https://ulasim.sakarya.bel.tr")
                    .header("Referer", "https://ulasim.sakarya.bel.tr/")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "tr,en-US;q=0.9,en;q=0.8")
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e(TAG, "API Hata: ${response.code}")
                    return@withContext Result.failure(
                        Exception("Sunucu hatası: ${response.code}")
                    )
                }

                val bodyString = response.body?.string()
                if (bodyString.isNullOrEmpty()) {
                    return@withContext Result.failure(
                        Exception("Sunucudan boş yanıt geldi")
                    )
                }

                Log.d(TAG, "Yanıt alındı: ${bodyString.take(500)}")

                val parsed = JsonParser.parseString(bodyString)
                if (!parsed.isJsonObject) {
                    return@withContext Result.failure(
                        Exception("Beklenmeyen yanıt formatı")
                    )
                }

                val root = parsed.asJsonObject
                val dataObj = root.getAsJsonObject("data")
                    ?: return@withContext Result.success(emptyList())

                val planObj = dataObj.getAsJsonObject("plan")
                    ?: return@withContext Result.success(emptyList())

                val itinerariesArray = planObj.getAsJsonArray("itineraries")
                    ?: return@withContext Result.success(emptyList())

                val itineraries = itinerariesArray.mapNotNull { element ->
                    if (!element.isJsonObject) return@mapNotNull null
                    val itObj = element.asJsonObject

                    val legs = itObj.getAsJsonArray("legs")?.mapNotNull { legElement ->
                        if (!legElement.isJsonObject) return@mapNotNull null
                        val legObj = legElement.asJsonObject

                        val fromObj = legObj.getAsJsonObject("from")
                        val toObj = legObj.getAsJsonObject("to")

                        val routeCodeRaw = legObj.get("routeCode")?.let {
                            if (it.isJsonNull) null else it.asString
                        }

                        fun parseStop(obj: com.google.gson.JsonObject?): Stop {
                            val stopObj = obj?.getAsJsonObject("stop")
                            return Stop(
                                name = obj?.get("name")?.let { if (it.isJsonNull) null else it.asString },
                                latitude = obj?.get("latitude")?.let { if (it.isJsonNull) null else it.asDouble },
                                longitude = obj?.get("longitude")?.let { if (it.isJsonNull) null else it.asDouble },
                                stopCode = stopObj?.get("code")?.let { if (it.isJsonNull) null else it.asString },
                                stopId = stopObj?.get("id")?.let { if (it.isJsonNull) null else it.asString }
                            )
                        }

                        Leg(
                            mode = legObj.get("mode")?.asString ?: "WALK",
                            routeCode = if (routeCodeRaw.isNullOrEmpty()) null else routeCodeRaw,
                            from = parseStop(fromObj),
                            to = parseStop(toObj),
                            duration = legObj.get("duration")?.asInt ?: 0,
                            distance = legObj.get("distance")?.asDouble ?: 0.0
                        )
                    } ?: emptyList()

                    Itinerary(
                        startTime = itObj.get("startTime")?.asLong ?: 0L,
                        endTime = itObj.get("endTime")?.asLong ?: 0L,
                        duration = itObj.get("duration")?.asInt ?: 0,
                        walkDistance = itObj.get("walkDistance")?.asDouble ?: 0.0,
                        transfer = itObj.get("transfer")?.asInt ?: 0,
                        legs = legs
                    )
                }

                Log.d(TAG, "${itineraries.size} güzergah bulundu")
                Result.success(itineraries)

            } catch (e: Exception) {
                Log.e(TAG, "Güzergah arama hatası: ${e.message}", e)
                Result.failure(Exception("Bağlantı hatası: ${e.localizedMessage}"))
            }
        }
}
