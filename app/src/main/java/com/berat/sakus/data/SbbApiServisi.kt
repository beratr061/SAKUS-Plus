package com.berat.sakus.data

import android.content.Context
import android.util.Log
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Calendar
import java.util.concurrent.TimeUnit

import android.annotation.SuppressLint

@SuppressLint("StaticFieldLeak")
class SbbApiServisi private constructor(private val context: Context) {

    companion object {
        private const val BASE_URL = "https://sbbpublicapi.sakarya.bel.tr/api/v1"
        private const val STREAM_URL = "https://sbbpublicapi.sakarya.bel.tr/api/v1/sakus/vehicle-tracking/stream"
        private const val NEWS_BASE_URL = "https://api.sakarya.bel.tr/Mobil/News"
        private const val TAG = "SbbApiServisi"

        const val BUS_TYPE_BELEDIYE = 3869
        const val BUS_TYPE_OZEL_HALK = 5731
        const val BUS_TYPE_TAKSI_DOLMUS = 5733
        const val BUS_TYPE_MINIBUS = 5732
        const val BUS_TYPE_METROBUS = 6904
        const val BUS_TYPE_ADARAY = 6905

        @Volatile
        private var instance: SbbApiServisi? = null

        fun getInstance(context: Context): SbbApiServisi {
            return instance ?: synchronized(this) {
                instance ?: SbbApiServisi(context.applicationContext).also { instance = it }
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .proxy(java.net.Proxy.NO_PROXY)
        .build()

    // SSE stream için uzun timeout'lu ayrı bir client (ana client'tan türetildi)
    private val streamClient = client.newBuilder()
        .readTimeout(0, TimeUnit.SECONDS) // SSE stream için timeout yok
        .build()

    private suspend fun get(url: String): JsonElement? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("Origin", "https://ulasim.sakarya.bel.tr")
                .header("Referer", "https://ulasim.sakarya.bel.tr")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string()
                if (bodyString.isNullOrEmpty() || bodyString == "\"\"") {
                    return@withContext null
                }
                return@withContext JsonParser.parseString(bodyString)
            } else {
                Log.e(TAG, "API Hata: ${response.code} - $url")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "API İstek Hatası: ${e.message}")
            return@withContext null
        }
    }

    private suspend fun getExternal(url: String): JsonElement? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("Origin", "https://www.sakarya.bel.tr")
                .header("Referer", "https://www.sakarya.bel.tr")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                .header("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string()
                if (bodyString.isNullOrEmpty() || bodyString == "\"\"") {
                    return@withContext null
                }
                return@withContext JsonParser.parseString(bodyString)
            } else {
                Log.e(TAG, "News API Hata: ${response.code} - $url")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "News API İstek Hatası: ${e.message}")
            return@withContext null
        }
    }

    // 1. Hat Listesi
    suspend fun tumHatlariGetir(): List<HatBilgisi> = withContext(Dispatchers.IO) {
        val tumHatlar = mutableListOf<HatBilgisi>()
        
        val deferredBelediye = async { hatlariGetir(BUS_TYPE_BELEDIYE) }
        val deferredOzelHalk = async { hatlariGetir(BUS_TYPE_OZEL_HALK) }
        val deferredTaksiDolmus = async { hatlariGetir(BUS_TYPE_TAKSI_DOLMUS) }
        val deferredMinibus = async { hatlariGetir(BUS_TYPE_MINIBUS) }
        val deferredMetrobus = async { hatlariGetir(BUS_TYPE_METROBUS) }
        val deferredAdaray = async { hatlariGetir(BUS_TYPE_ADARAY) }
        
        val results = awaitAll(deferredBelediye, deferredOzelHalk, deferredTaksiDolmus, deferredMinibus, deferredMetrobus, deferredAdaray)
        for (list in results) {
            tumHatlar.addAll(list)
        }

        val uniqueHatlar = tumHatlar.distinctBy { it.id }.toMutableList()

        uniqueHatlar.sortWith { a, b ->
            val numRegex = Regex("[^0-9]")
            val aNum = a.hatNumarasi.replace(numRegex, "").toIntOrNull()
            val bNum = b.hatNumarasi.replace(numRegex, "").toIntOrNull()
            if (aNum != null && bNum != null && aNum != bNum) {
                aNum.compareTo(bNum)
            } else {
                a.hatNumarasi.compareTo(b.hatNumarasi)
            }
        }
        uniqueHatlar
    }

    private suspend fun hatlariGetir(busType: Int): List<HatBilgisi> {
        val data = get("$BASE_URL/Ulasim?busType=$busType") ?: return emptyList()
        var items: JsonArray? = null
        if (data.isJsonArray) {
            items = data.asJsonArray
        } else if (data.isJsonObject) {
            val details = data.asJsonObject
            items = details.getAsJsonArray("items") ?: details.getAsJsonArray("Items")
        }
        return items?.mapNotNull { if (it.isJsonObject) HatBilgisi.fromJson(it.asJsonObject) else null } ?: emptyList()
    }

    // 2. Hat Sefer Saatleri

    suspend fun seferSaatleriGetirByDate(lineId: Int, calendar: Calendar): HatSeferBilgisi? {
        val year = calendar.get(Calendar.YEAR)
        val month = String.format(java.util.Locale.US, "%02d", calendar.get(Calendar.MONTH) + 1)
        val day = String.format(java.util.Locale.US, "%02d", calendar.get(Calendar.DAY_OF_MONTH))
        val dateStr = "${year}-${month}-${day}T00:00:00.000Z"
        val encodedDate = java.net.URLEncoder.encode(dateStr, "UTF-8")
        
        val data = get("$BASE_URL/Ulasim/line-schedule?date=$encodedDate&lineId=$lineId")
        if (data != null && data.isJsonObject) {
            return HatSeferBilgisi.fromJson(data.asJsonObject)
        }
        return null
    }

    // 3. Haberler (Toplu Taşıma kategorisi - CategoryId=52)
    suspend fun haberleriGetir(categoryId: Int = 52): List<NewsItem> {
        val data = getExternal("$NEWS_BASE_URL/GetListNews?CategoryId=$categoryId&NewsCount=50") ?: return emptyList()
        if (data.isJsonArray) {
            return data.asJsonArray.mapNotNull { if (it.isJsonObject) NewsItem.fromJson(it.asJsonObject) else null }
        }
        return emptyList()
    }

    // 4. Duyurular - Genel duyurular (hattan bağımsız)
    suspend fun duyurulariGetir(pageSize: Int = 100): List<Duyuru> {
        val data = get("$BASE_URL/Ulasim/announcement?isLineAnnouncement=false&pageSize=$pageSize") ?: return emptyList()
        if (data.isJsonObject) {
            val items = data.asJsonObject.getAsJsonArray("items")
            return items?.mapNotNull { if (it.isJsonObject) Duyuru.fromJson(it.asJsonObject) else null } ?: emptyList()
        }
        return emptyList()
    }

    // 4a. Tüm hat duyuruları
    suspend fun tumHatDuyurulariGetir(pageSize: Int = 100): List<Duyuru> {
        val data = get("$BASE_URL/Ulasim/announcement?isLineAnnouncement=true&pageSize=$pageSize") ?: return emptyList()
        if (data.isJsonObject) {
            val items = data.asJsonObject.getAsJsonArray("items")
            return items?.mapNotNull { if (it.isJsonObject) Duyuru.fromJson(it.asJsonObject) else null } ?: emptyList()
        }
        return emptyList()
    }

    // 4b. Hat duyuruları - Belirli bir hatta ait duyurular
    suspend fun hatDuyurulariGetir(lineId: Int, pageSize: Int = 100): List<Duyuru> {
        val data = get("$BASE_URL/Ulasim/announcement?isLineAnnouncement=true&lineId=$lineId&pageSize=$pageSize")
            ?: return emptyList()
        if (data.isJsonObject) {
            val items = data.asJsonObject.getAsJsonArray("items")
            val all = items?.mapNotNull { if (it.isJsonObject) Duyuru.fromJson(it.asJsonObject) else null } ?: emptyList()
            return all.filter { it.hatId == lineId }
        }
        return emptyList()
    }

    // 4. Fiyat Tarifesi
    suspend fun tarifeGetir(lineId: Int, busType: Int = 3869): TarifeBilgisi? {
        val data = get("$BASE_URL/Ulasim/line-fare/$lineId?busType=$busType")
        if (data != null && data.isJsonObject) {
            return TarifeBilgisi.fromJson(data.asJsonObject)
        }
        return null
    }

    // 5a. Tüm duraklar (sayfalı - harita için)
    suspend fun tumDuraklariGetir(): List<DurakBilgisi> = withContext(Dispatchers.IO) {
        val allStops = mutableListOf<DurakBilgisi>()
        var pageNumber = 1
        val pageSize = 100
        while (true) {
            val pageStops = duraklariGetirSayfa(pageNumber, pageSize)
            if (pageStops.isEmpty()) break
            allStops.addAll(pageStops)
            if (pageStops.size < pageSize) break
            pageNumber++
        }
        allStops
    }

    /**
     * Tek sayfa durak verisi (drawer sayfalama için).
     */
    suspend fun duraklariGetirSayfa(pageNumber: Int, pageSize: Int): List<DurakBilgisi> =
        withContext(Dispatchers.IO) {
            val data = get("$BASE_URL/Ulasim/bus-stops-pagination?pageNumber=$pageNumber&pageSize=$pageSize")
                ?: return@withContext emptyList()
            var items: JsonArray? = null
            if (data.isJsonArray) {
                items = data.asJsonArray
            } else if (data.isJsonObject) {
                val obj = data.asJsonObject
                items = obj.getAsJsonArray("items") ?: obj.getAsJsonArray("Items")
            }
            items?.mapNotNull { elem ->
                if (elem.isJsonObject) {
                    val obj = elem.asJsonObject
                    val toParse = when {
                        obj.has("busStop") && obj.get("busStop").isJsonObject -> obj.getAsJsonObject("busStop")
                        obj.has("station") && obj.get("station").isJsonObject -> obj.getAsJsonObject("station")
                        obj.has("stop") && obj.get("stop").isJsonObject -> obj.getAsJsonObject("stop")
                        else -> obj
                    }
                    DurakBilgisi.fromJson(toParse)
                } else null
            }?.filter { it.lat != 0.0 && it.lng != 0.0 } ?: emptyList()
        }

    // 5. Güzergah ve Duraklar
    suspend fun guzergahVeDuraklariGetir(lineId: Int, calendar: Calendar? = null): HatGuzergahBilgisi? {
        val cal = calendar ?: Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = String.format(java.util.Locale.US, "%02d", cal.get(Calendar.MONTH) + 1)
        val day = String.format(java.util.Locale.US, "%02d", cal.get(Calendar.DAY_OF_MONTH))
        val dateStr = "${year}-${month}-${day}"
        
        val data = get("$BASE_URL/Ulasim/route-and-busstops/$lineId?date=$dateStr")
        if (data != null && data.isJsonObject) {
            return HatGuzergahBilgisi.fromJson(data.asJsonObject)
        }
        return null
    }

    // 6. Canlı Araç
    private var asisMapCache: JsonObject? = null
    
    private suspend fun loadAsisMap(): JsonObject = withContext(Dispatchers.IO) {
        if (asisMapCache != null) return@withContext asisMapCache!!
        try {
            val inputStream = context.assets.open("json/asis_map.json") // Changed from /data/ to /json/ based on user folders
            val reader = InputStreamReader(inputStream)
            val root = JsonParser.parseReader(reader)
            asisMapCache = root.asJsonObject
        } catch (e: Exception) {
            Log.e(TAG, "ASIS map yükleme hatası: ${e.message}")
            asisMapCache = JsonObject()
        }
        return@withContext asisMapCache!!
    }

    suspend fun asisIdGetir(hat: HatBilgisi): Int? {
        if (hat.asisId != null) return hat.asisId
        val map = loadAsisMap()

        for ((key, element) in map.entrySet()) {
            if (element.isJsonObject) {
                val data = element.asJsonObject
                if (data.has("sbb_id") && !data.get("sbb_id").isJsonNull && data.get("sbb_id").asInt == hat.id) {
                    return key.toIntOrNull()
                }
            }
        }

        val hatNo = hat.hatNumarasi.trim().uppercase()
        if (hatNo.isNotEmpty()) {
            for ((key, element) in map.entrySet()) {
                if (element.isJsonObject) {
                    val data = element.asJsonObject
                    val lineNo = if (data.has("line_no") && !data.get("line_no").isJsonNull) {
                        data.get("line_no").asString.trim().uppercase()
                    } else ""

                    if (lineNo == hatNo) {
                        return key.toIntOrNull()
                    }
                }
            }
        }
        return null
    }

    suspend fun aracKonumlariniGetir(asisId: Int): List<AracKonumu> {
        val data = getWithRetry("$BASE_URL/VehicleTracking?AsisId=$asisId") ?: return emptyList()
        var items: JsonArray? = null
        if (data.isJsonArray) {
            items = data.asJsonArray
        } else if (data.isJsonObject && data.asJsonObject.has("items")) {
            items = data.asJsonObject.getAsJsonArray("items")
        }
        return items?.mapNotNull { if (it.isJsonObject) AracKonumu.fromJson(it.asJsonObject) else null } ?: emptyList()
    }

    /**
     * SSE stream üzerinden canlı araç konumlarını akış olarak döndürür.
     * Stream tüm araçları döndürür, lineId'ye göre filtrelenir.
     * Her ~1 saniyede bir yeni veri emit eder.
     */
    fun aracKonumlariniStreamle(lineId: Int): Flow<List<AracKonumu>> = flow {
        while (currentCoroutineContext().isActive) {
            var reader: BufferedReader? = null
            try {
                val request = Request.Builder()
                    .url(STREAM_URL)
                    .header("Origin", "https://ulasim.sakarya.bel.tr")
                    .header("Referer", "https://ulasim.sakarya.bel.tr")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
                    .header("Accept", "text/event-stream")
                    .build()

                val response = streamClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.e(TAG, "SSE bağlantı hatası: ${response.code}")
                    response.close()
                    delay(2000)
                    continue
                }

                reader = response.body?.byteStream()?.bufferedReader()
                if (reader == null) {
                    response.close()
                    delay(2000)
                    continue
                }

                Log.d(TAG, "SSE stream bağlantısı kuruldu (lineId=$lineId)")

                var line: String?
                var currentEventType = ""
                while (currentCoroutineContext().isActive) {
                    line = reader.readLine()
                    if (line == null) {
                        // Stream kapandı, yeniden bağlan
                        Log.w(TAG, "SSE stream kapandı, yeniden bağlanılıyor...")
                        break
                    }

                    // SSE event tipini takip et
                    if (line.startsWith("event:")) {
                        currentEventType = line.removePrefix("event:").trim()
                        continue
                    }

                    if (line.startsWith("data:")) {
                        val jsonStr = line.removePrefix("data:").trim()
                        if (jsonStr.isEmpty()) continue

                        // Sunucu 10 dk sonra bağlantıyı kesiyor, hemen yeniden bağlan
                        if (currentEventType == "server-disconnect") {
                            Log.w(TAG, "SSE sunucu bağlantıyı kesti: $jsonStr — yeniden bağlanılıyor...")
                            currentEventType = ""
                            break
                        }

                        // Normal vehicle-update verisi
                        currentEventType = ""
                        try {
                            val parsed = JsonParser.parseString(jsonStr)
                            if (parsed.isJsonArray) {
                                val allVehicles = parsed.asJsonArray
                                    .filter { it.isJsonObject }
                                    .map { it.asJsonObject }

                                // lineId'ye göre filtrele
                                val filtered = allVehicles
                                    .filter { obj ->
                                        val vLineId = if (obj.has("lineId") && !obj.get("lineId").isJsonNull) {
                                            obj.get("lineId").asInt
                                        } else -1
                                        vLineId == lineId
                                    }
                                    .mapNotNull { AracKonumu.fromJson(it) }

                                emit(filtered)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "SSE veri ayrıştırma hatası: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Log.e(TAG, "SSE stream hatası: ${e.message}")
                    delay(2000) // Yeniden bağlanmadan önce bekle
                }
            } finally {
                try { reader?.close() } catch (_: Exception) {}
            }
        }
    }.flowOn(Dispatchers.IO)

    // 7. Durağa yaklaşan otobüsler (canlı araç verisinden)
    suspend fun durakYaklasanAraclariGetir(durak: DurakBilgisi, hatlar: List<HatBilgisi>): List<DurakVarisi> =
        withContext(Dispatchers.IO) {
            val sonuclar = mutableListOf<DurakVarisi>()
            val durakAdiNorm = durak.durakAdi.trim().lowercase()
            if (durakAdiNorm.isEmpty()) return@withContext sonuclar

            for (hat in hatlar.take(30)) {
                val asisId = asisIdGetir(hat) ?: continue
                val araclar = aracKonumlariniGetir(asisId)
                for (a in araclar) {
                    if (!a.aktifMi) continue
                    val sonrakiNorm = a.sonrakiDurak.trim().lowercase()
                    val mevcutNorm = a.mevcutDurak.trim().lowercase()
                    val hedefeGidiyor = sonrakiNorm == durakAdiNorm || sonrakiNorm.contains(durakAdiNorm) || durakAdiNorm.contains(sonrakiNorm)
                    val durakta = mevcutNorm == durakAdiNorm || mevcutNorm.contains(durakAdiNorm) || durakAdiNorm.contains(mevcutNorm)
                    if (hedefeGidiyor || durakta) {
                        val dakika = if (durakta) 0 else (a.etaSaniye / 60).toInt().coerceAtLeast(0)
                        sonuclar.add(
                            DurakVarisi(
                                hatNo = a.hatNo.ifEmpty { hat.hatNumarasi },
                                hatAdi = a.guzergahAdi.ifEmpty { hat.ad },
                                plaka = a.plaka,
                                aracNumarasi = a.aracNumarasi,
                                dakika = dakika,
                                guzergahAdi = a.guzergahAdi
                            )
                        )
                    }
                }
            }
            sonuclar.sortedBy { it.dakika }
        }

    private suspend fun getWithRetry(url: String, maxRetry: Int = 3): JsonElement? {
        for (attempt in 1..maxRetry) {
            val result = get(url)
            if (result != null) return result
            if (attempt < maxRetry) {
                val waitMs = 500L * attempt
                delay(waitMs)
            }
        }
        return null
    }
}

// ─────────────────────────────────────────────────────
// MODELS (Data Classes)
// ─────────────────────────────────────────────────────

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
            // API'de direction/directionId yoksa routeId % 2: 0=Gidiş (çift), 1=Dönüş (tek)
            val yon = if (json.has("direction") && !json.get("direction").isJsonNull) {
                json.get("direction").asInt
            } else if (json.has("directionId") && !json.get("directionId").isJsonNull) {
                json.get("directionId").asInt
            } else {
                if (routeId % 2 == 0) 0 else 1  // 630→Gidiş, 629→Dönüş
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
                aracNumarasi = if (json.has("busNumber") && !json.get("busNumber").isJsonNull) json.get("busNumber").asInt else 0,
                guzergahId = routeId
            )
        }
    }
}

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
