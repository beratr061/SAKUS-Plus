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
        const val BUS_TYPE_OZEL_HALK = 7355
        const val BUS_TYPE_TAKSI_DOLMUS = 5733
        const val BUS_TYPE_MINIBUS = 5732
        const val BUS_TYPE_METROBUS = 6904
        const val BUS_TYPE_ADARAY = 6905
        const val BUS_TYPE_TRAMVAY = 7416
        const val BUS_TYPE_BELEDIYE_ESKI = 6907
        const val BUS_TYPE_EUTS_OZEL_HALK = 7354

        // Bu busType'lardan gelen veriler yansıtılmaz (filtrelenir)
        private val HIDDEN_BUS_TYPES = setOf(7354, 5731)

        // BusType listesinde görünmeyen, sadece lineId ile erişilebilen hatlar
        private val GIZLI_HAT_IDS = listOf(96, 294, 295, 296, 299)

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
        val deferredTramvay = async { hatlariGetir(BUS_TYPE_TRAMVAY) }
        val deferredEutsOzelHalk = async { hatlariGetir(BUS_TYPE_EUTS_OZEL_HALK) }
        
        val results = awaitAll(deferredBelediye, deferredOzelHalk, deferredTaksiDolmus, deferredMinibus, deferredMetrobus, deferredAdaray, deferredTramvay, deferredEutsOzelHalk)
        for (list in results) {
            tumHatlar.addAll(list)
        }

        // Gizli busType'lardan gelen hatları filtrele
        val filteredHatlar = tumHatlar.filter { it.aracTipId !in HIDDEN_BUS_TYPES }.toMutableList()

        // BusType listesinde görünmeyen gizli hatları ekle
        filteredHatlar.addAll(gizliHatlariGetir())

        val uniqueHatlar = filteredHatlar.distinctBy { it.id }.toMutableList()

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

    /**
     * BusType listesinde görünmeyen gizli hatları lineId üzerinden çeker.
     * /Ulasim/route-and-busstops/{lineId} endpoint'inden hat bilgisini parse eder.
     */
    private suspend fun gizliHatlariGetir(): List<HatBilgisi> = withContext(Dispatchers.IO) {
        GIZLI_HAT_IDS.mapNotNull { lineId ->
            async {
                try {
                    val data = get("$BASE_URL/Ulasim/route-and-busstops/$lineId")
                    if (data != null && data.isJsonObject) {
                        val obj = data.asJsonObject
                        HatBilgisi(
                            id = if (obj.has("lineId") && !obj.get("lineId").isJsonNull) obj.get("lineId").asInt else lineId,
                            ad = if (obj.has("lineName") && !obj.get("lineName").isJsonNull) obj.get("lineName").asString else "",
                            hatNumarasi = if (obj.has("lineNumber") && !obj.get("lineNumber").isJsonNull) obj.get("lineNumber").asString else "",
                            aracTipAdi = "",
                            aracTipAciklama = "",
                            aracTipRenk = "#68bd9c",
                            aracTipId = if (obj.has("typeValueId") && !obj.get("typeValueId").isJsonNull) obj.get("typeValueId").asInt else 0,
                            asisId = if (obj.has("ekentLineIntegrationId") && !obj.get("ekentLineIntegrationId").isJsonNull) obj.get("ekentLineIntegrationId").asInt else null,
                            slug = ""
                        )
                    } else null
                } catch (e: Exception) {
                    Log.e(TAG, "Gizli hat çekme hatası (lineId=$lineId): ${e.message}")
                    null
                }
            }
        }.awaitAll().filterNotNull()
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

    // 5. Fiyat Tarifesi
    suspend fun tarifeGetir(lineId: Int, busType: Int = 3869): TarifeBilgisi? {
        val data = get("$BASE_URL/Ulasim/line-fare/$lineId?busType=$busType")
        if (data != null && data.isJsonObject) {
            return TarifeBilgisi.fromJson(data.asJsonObject)
        }
        return null
    }

    // 6a. Tüm duraklar (sayfalı - harita için)
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

    // 6b. Güzergah ve Duraklar
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

    // 7. Canlı Araç
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

    /**
     * SSE stream üzerinden TÜM araçları akış olarak döndürür (filtresiz).
     * Araç sorgulama ekranında tüm araçları listelemek için kullanılır.
     */
    fun tumAraclariStreamle(): Flow<List<AracKonumu>> = flow {
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
                    Log.e(TAG, "SSE tüm araçlar bağlantı hatası: ${response.code}")
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

                Log.d(TAG, "SSE tüm araçlar stream bağlantısı kuruldu")

                var line: String?
                var currentEventType = ""
                while (currentCoroutineContext().isActive) {
                    line = reader.readLine()
                    if (line == null) {
                        Log.w(TAG, "SSE tüm araçlar stream kapandı, yeniden bağlanılıyor...")
                        break
                    }

                    if (line.startsWith("event:")) {
                        currentEventType = line.removePrefix("event:").trim()
                        continue
                    }

                    if (line.startsWith("data:")) {
                        val jsonStr = line.removePrefix("data:").trim()
                        if (jsonStr.isEmpty()) continue

                        if (currentEventType == "server-disconnect") {
                            Log.w(TAG, "SSE tüm araçlar sunucu bağlantıyı kesti — yeniden bağlanılıyor...")
                            currentEventType = ""
                            break
                        }

                        currentEventType = ""
                        try {
                            val parsed = JsonParser.parseString(jsonStr)
                            if (parsed.isJsonArray) {
                                val araclar = parsed.asJsonArray
                                    .filter { it.isJsonObject }
                                    .mapNotNull { AracKonumu.fromJson(it.asJsonObject) }

                                emit(araclar)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "SSE tüm araçlar veri ayrıştırma hatası: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Log.e(TAG, "SSE tüm araçlar stream hatası: ${e.message}")
                    delay(2000)
                }
            } finally {
                try { reader?.close() } catch (_: Exception) {}
            }
        }
    }.flowOn(Dispatchers.IO)

    // 8. Araç Sorgulama (plaka veya kapı numarası ile)
    
    /**
     * SSE stream üzerinden araç sorgulama.
     * Plaka veya kapı numarasına göre arama yapar.
     * Her stream güncellemesinde eşleşen araçları emit eder.
     */
    fun aracSorgula(sorgu: String): Flow<List<AracKonumu>> = flow {
        val normalizedQuery = sorgu.trim().uppercase().replace("\\s+".toRegex(), " ")
        
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
                    Log.e(TAG, "SSE sorgu bağlantı hatası: ${response.code}")
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

                Log.d(TAG, "SSE araç sorgu stream bağlantısı kuruldu (sorgu=$sorgu)")

                var line: String?
                var currentEventType = ""
                while (currentCoroutineContext().isActive) {
                    line = reader.readLine()
                    if (line == null) {
                        Log.w(TAG, "SSE sorgu stream kapandı, yeniden bağlanılıyor...")
                        break
                    }

                    if (line.startsWith("event:")) {
                        currentEventType = line.removePrefix("event:").trim()
                        continue
                    }

                    if (line.startsWith("data:")) {
                        val jsonStr = line.removePrefix("data:").trim()
                        if (jsonStr.isEmpty()) continue

                        if (currentEventType == "server-disconnect") {
                            Log.w(TAG, "SSE sorgu sunucu bağlantıyı kesti — yeniden bağlanılıyor...")
                            currentEventType = ""
                            break
                        }

                        currentEventType = ""
                        try {
                            val parsed = JsonParser.parseString(jsonStr)
                            if (parsed.isJsonArray) {
                                val allVehicles = parsed.asJsonArray
                                    .filter { it.isJsonObject }
                                    .map { it.asJsonObject }

                                val filtered = allVehicles.filter { obj ->
                                    val plate = if (obj.has("plate") && !obj.get("plate").isJsonNull)
                                        obj.get("plate").asString.trim().uppercase().replace("\\s+".toRegex(), " ") else ""
                                    val busNum = if (obj.has("busNumber") && !obj.get("busNumber").isJsonNull)
                                        obj.get("busNumber").asString.trim() else ""
                                    
                                    plate.contains(normalizedQuery) || 
                                    normalizedQuery.contains(plate.filter { it != ' ' }) && plate.isNotEmpty() ||
                                    busNum == normalizedQuery ||
                                    busNum.contains(normalizedQuery)
                                }.mapNotNull { AracKonumu.fromJson(it) }

                                if (filtered.isNotEmpty()) {
                                    emit(filtered)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "SSE sorgu veri ayrıştırma hatası: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                if (currentCoroutineContext().isActive) {
                    Log.e(TAG, "SSE sorgu stream hatası: ${e.message}")
                    delay(2000)
                }
            } finally {
                try { reader?.close() } catch (_: Exception) {}
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * REST API ile araç sorgulama (fallback).
     * Tüm hatların araçlarını tarar, plaka veya kapı numarasına göre filtreler.
     */
    suspend fun aracSorgulaRest(sorgu: String): List<AracKonumu> = withContext(Dispatchers.IO) {
        val normalizedQuery = sorgu.trim().uppercase().replace("\\s+".toRegex(), " ")
        val map = loadAsisMap()
        val sonuclar = mutableListOf<AracKonumu>()
        
        for ((key, _) in map.entrySet()) {
            val asisId = key.toIntOrNull() ?: continue
            val araclar = aracKonumlariniGetir(asisId)
            for (arac in araclar) {
                val plakaUpper = arac.plaka.trim().uppercase().replace("\\s+".toRegex(), " ")
                val busNumStr = arac.aracNumarasi.toString()
                if (plakaUpper.contains(normalizedQuery) ||
                    normalizedQuery.contains(plakaUpper.filter { it != ' ' }) && plakaUpper.isNotEmpty() ||
                    busNumStr == normalizedQuery ||
                    busNumStr.contains(normalizedQuery)
                ) {
                    sonuclar.add(arac)
                }
            }
            if (sonuclar.isNotEmpty()) break // Bulundu, diğer hatları tarama
        }
        sonuclar
    }

    // 9. Durağa yaklaşan otobüsler (canlı araç verisinden)
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

