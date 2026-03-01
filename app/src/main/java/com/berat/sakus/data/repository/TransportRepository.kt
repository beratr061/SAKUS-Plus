package com.berat.sakus.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.berat.sakus.data.*
import com.berat.sakus.data.local.SakusDatabase
import com.berat.sakus.data.local.entity.GuzergahEntity
import com.berat.sakus.data.local.entity.HatEntity
import com.berat.sakus.data.local.entity.SeferSaatleriEntity
import com.berat.sakus.data.local.entity.TarifeEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Merkezi veri yönetim katmanı.
 * Önce yerel veritabanından veri sunar, arka planda API'den güncelleme kontrolü yapar.
 */
class TransportRepository private constructor(context: Context) {

    companion object {
        private const val TAG = "TransportRepository"
        private const val PREFS_NAME = "sakus_sync_prefs"
        private const val KEY_LAST_HATLAR_SYNC = "last_hatlar_sync"
        private const val KEY_HATLAR_HASH = "hatlar_hash"

        @Volatile
        private var INSTANCE: TransportRepository? = null

        fun getInstance(context: Context): TransportRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TransportRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val db = SakusDatabase.getInstance(context)
    private val api = SbbApiServisi.getInstance(context)
    private val gson = Gson()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ──────────────────────────────────────────────
    // HATLAR
    // ──────────────────────────────────────────────

    /**
     * Hat listesini Flow olarak döndürür. DB güncellendiğinde otomatik güncellenir.
     */
    fun getHatlarFlow(): Flow<List<HatBilgisi>> {
        return db.hatDao().tumHatlariGetir().map { entities ->
            entities.map { it.toHatBilgisi() }
        }
    }

    /**
     * DB'de hat verisi var mı kontrol eder.
     */
    suspend fun hasHatlar(): Boolean = db.hatDao().hatSayisi() > 0

    /**
     * API'den hatları çekip DB'ye kaydeder.
     * @return true ise veri güncellendi, false ise değişiklik yok/hata.
     */
    suspend fun syncHatlar(): Boolean = withContext(Dispatchers.IO) {
        try {
            val apiHatlar = api.tumHatlariGetir()
            if (apiHatlar.isEmpty()) return@withContext false

            // Hash kontrolü
            val newHash = apiHatlar.map { "${it.id}-${it.ad}-${it.hatNumarasi}" }
                .sorted()
                .joinToString(",")
                .hashCode()
                .toString()

            val oldHash = prefs.getString(KEY_HATLAR_HASH, "")
            if (newHash == oldHash) {
                Log.d(TAG, "Hatlar değişmedi, güncelleme gerekmiyor.")
                prefs.edit().putLong(KEY_LAST_HATLAR_SYNC, System.currentTimeMillis()).apply()
                return@withContext false
            }

            val entities = apiHatlar.map { HatEntity.fromHatBilgisi(it) }
            db.hatDao().tumunuSil()
            db.hatDao().hatlariKaydet(entities)

            prefs.edit()
                .putLong(KEY_LAST_HATLAR_SYNC, System.currentTimeMillis())
                .putString(KEY_HATLAR_HASH, newHash)
                .apply()

            Log.d(TAG, "Hatlar güncellendi: ${entities.size} kayıt")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Hatlar senkronizasyon hatası: ${e.message}")
            false
        }
    }

    // ──────────────────────────────────────────────
    // GÜZERGAHLAR
    // ──────────────────────────────────────────────

    /**
     * Önce DB'den güzergah verisi dener, yoksa API'den çekip kaydeder.
     */
    suspend fun getGuzergah(hatId: Int): HatGuzergahBilgisi? = withContext(Dispatchers.IO) {
        // Önce DB'ye bak
        val cached = db.guzergahDao().guzergahGetir(hatId)
        if (cached != null) {
            return@withContext parseGuzergahEntity(cached)
        }

        // DB'de yok, API'den çek
        return@withContext fetchAndCacheGuzergah(hatId)
    }

    /**
     * API'den güzergah bilgisini çekip DB'ye kaydeder.
     */
    suspend fun syncGuzergah(hatId: Int): HatGuzergahBilgisi? = withContext(Dispatchers.IO) {
        fetchAndCacheGuzergah(hatId)
    }

    private suspend fun fetchAndCacheGuzergah(hatId: Int): HatGuzergahBilgisi? {
        try {
            val result = api.guzergahVeDuraklariGetir(hatId) ?: return null

            val entity = GuzergahEntity(
                hatId = hatId,
                koordinatlarJson = gson.toJson(result.guzergahKoordinatlari),
                guzergahlarJson = gson.toJson(result.guzergahlar),
                duraklarJson = gson.toJson(result.duraklar),
                yonlerJson = gson.toJson(result.yonler)
            )
            db.guzergahDao().guzergahKaydet(entity)
            Log.d(TAG, "Güzergah kaydedildi: hatId=$hatId")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Güzergah çekme hatası: ${e.message}")
            return null
        }
    }

    private fun parseGuzergahEntity(entity: GuzergahEntity): HatGuzergahBilgisi {
        val koordinatlarType = object : TypeToken<List<List<Double>>>() {}.type
        val guzergahlarType = object : TypeToken<Map<Int, List<List<Double>>>>() {}.type
        val duraklarType = object : TypeToken<List<DurakBilgisi>>() {}.type
        val yonlerType = object : TypeToken<List<YonBilgisi>>() {}.type

        return HatGuzergahBilgisi(
            guzergahKoordinatlari = gson.fromJson(entity.koordinatlarJson, koordinatlarType),
            guzergahlar = gson.fromJson(entity.guzergahlarJson, guzergahlarType),
            duraklar = gson.fromJson(entity.duraklarJson, duraklarType),
            yonler = gson.fromJson(entity.yonlerJson, yonlerType)
        )
    }

    // ──────────────────────────────────────────────
    // SEFER SAATLERİ
    // ──────────────────────────────────────────────

    /**
     * Sefer saatlerini öncelikle DB'den okur, yoksa API'den çeker.
     * @param dayType 0=hafta içi, 1=cumartesi, 2=pazar
     */
    suspend fun getSeferSaatleri(hatId: Int, dayType: Int): HatSeferBilgisi? = withContext(Dispatchers.IO) {
        val cached = db.seferSaatleriDao().seferSaatleriGetir(hatId, dayType)
        if (cached != null) {
            return@withContext parseSeferSaatleriEntity(cached)
        }
        return@withContext fetchAndCacheSeferSaatleri(hatId, dayType)
    }

    /**
     * 3 gün tipi için sefer saatlerini toplu getir (önce DB, yoksa API).
     */
    suspend fun getAllSeferSaatleri(hatId: Int): List<HatSeferBilgisi?> = withContext(Dispatchers.IO) {
        listOf(
            getSeferSaatleri(hatId, 0),
            getSeferSaatleri(hatId, 1),
            getSeferSaatleri(hatId, 2)
        )
    }

    suspend fun syncSeferSaatleri(hatId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            fetchAndCacheSeferSaatleri(hatId, 0)
            fetchAndCacheSeferSaatleri(hatId, 1)
            fetchAndCacheSeferSaatleri(hatId, 2)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Sefer saatleri senkronizasyon hatası: ${e.message}")
            false
        }
    }

    private suspend fun fetchAndCacheSeferSaatleri(hatId: Int, dayType: Int): HatSeferBilgisi? {
        try {
            val calendar = getCalendarForDayType(dayType)
            val result = api.seferSaatleriGetirByDate(hatId, calendar) ?: return null

            val entity = SeferSaatleriEntity(
                hatId = hatId,
                dayType = dayType,
                seferBilgisiJson = gson.toJson(result)
            )
            db.seferSaatleriDao().seferSaatleriKaydet(entity)
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Sefer saatleri çekme hatası: ${e.message}")
            return null
        }
    }

    private fun getCalendarForDayType(dayType: Int): Calendar {
        val calendar = Calendar.getInstance()
        when (dayType) {
            0 -> { // Hafta içi
                while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                    calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            1 -> { // Cumartesi
                val offset = (Calendar.SATURDAY - calendar.get(Calendar.DAY_OF_WEEK) + 7) % 7
                calendar.add(Calendar.DAY_OF_MONTH, offset)
            }
            2 -> { // Pazar
                val offset = (Calendar.SUNDAY - calendar.get(Calendar.DAY_OF_WEEK) + 7) % 7
                calendar.add(Calendar.DAY_OF_MONTH, offset)
            }
        }
        return calendar
    }

    private fun parseSeferSaatleriEntity(entity: SeferSaatleriEntity): HatSeferBilgisi {
        return gson.fromJson(entity.seferBilgisiJson, HatSeferBilgisi::class.java)
    }

    // ──────────────────────────────────────────────
    // FİYAT TARİFESİ
    // ──────────────────────────────────────────────

    /**
     * Tarife bilgisini önce DB'den dener, yoksa API'den çekip kaydeder.
     */
    suspend fun getTarife(hatId: Int, aracTipId: Int): TarifeBilgisi? = withContext(Dispatchers.IO) {
        val cached = db.tarifeDao().tarifeGetir(hatId, aracTipId)
        if (cached != null) {
            return@withContext parseTarifeEntity(cached)
        }
        return@withContext fetchAndCacheTarife(hatId, aracTipId)
    }



    private suspend fun fetchAndCacheTarife(hatId: Int, aracTipId: Int): TarifeBilgisi? {
        try {
            val result = api.tarifeGetir(hatId, aracTipId) ?: return null

            val entity = TarifeEntity(
                hatId = hatId,
                aracTipId = aracTipId,
                tarifeBilgisiJson = gson.toJson(result)
            )
            db.tarifeDao().tarifeKaydet(entity)
            Log.d(TAG, "Tarife kaydedildi: hatId=$hatId, aracTipId=$aracTipId")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Tarife çekme hatası: ${e.message}")
            return null
        }
    }

    private fun parseTarifeEntity(entity: TarifeEntity): TarifeBilgisi {
        return gson.fromJson(entity.tarifeBilgisiJson, TarifeBilgisi::class.java)
    }

    // ──────────────────────────────────────────────
    // TOPLU SENKRONİZASYON
    // ──────────────────────────────────────────────

    /**
     * İlk açılışta tüm temel verileri API'den çekip kaydeder.
     * Hepsi başarılı olursa true döner.
     */
    suspend fun initialSync(): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "İlk senkronizasyon başlıyor...")
        val hatlarResult = syncHatlar()
        Log.d(TAG, "İlk senkronizasyon tamamlandı. Hatlar güncellendi: $hatlarResult")
        // Güzergahlar, sefer saatleri ve tarifeler hat bazlı olduğu için
        // kullanıcı ilgili hatta girdiğinde lazy olarak çekilecek
        true
    }

    /**
     * Saatlik senkronizasyon — sadece değişenleri günceller.
     */
    suspend fun periodicSync(): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "Periyodik senkronizasyon başlıyor...")
        val hatlarUpdated = syncHatlar()
        Log.d(TAG, "Periyodik senkronizasyon tamamlandı. Hatlar güncellendi: $hatlarUpdated")
        true
    }

    // ──────────────────────────────────────────────
    // TÜM DURAKLAR (Harita için - sayfalı API)
    // ──────────────────────────────────────────────

    /**
     * Tüm otobüs duraklarını API'den çeker (bus-stops-pagination).
     * Harita ekranı için kullanılır, önbellek yok.
     */
    suspend fun tumDuraklariGetir(): List<DurakBilgisi> = withContext(Dispatchers.IO) {
        api.tumDuraklariGetir()
    }



    /**
     * Durağa yaklaşan otobüsleri canlı veriden getirir.
     */
    suspend fun durakYaklasanAraclariGetir(durak: DurakBilgisi): List<DurakVarisi> = withContext(Dispatchers.IO) {
        val hatlar = api.tumHatlariGetir()
        api.durakYaklasanAraclariGetir(durak, hatlar)
    }
}
