package com.berat.sakus.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.berat.sakus.data.SbbApiServisi
import com.berat.sakus.data.HatBilgisi
import com.berat.sakus.data.local.SakusDatabase
import com.berat.sakus.data.notification.LineUpdateNotificationHelper
import com.berat.sakus.data.local.entity.GuzergahEntity
import com.berat.sakus.data.local.entity.HatEntity
import com.berat.sakus.data.local.entity.SeferSaatleriEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Periyodik hat ve duyuru kontrolü. Hat eklendiğinde/kaldırıldığında veya sefer/güzergah değişikliği olduğunda bildirim gösterir.
 */
class LineUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "LineUpdateWorker"
        private const val PREFS_NAME = "line_update_prefs"
        private const val KEY_KNOWN_DUYURU_IDS = "known_duyuru_ids"
        private const val KEY_KNOWN_LINES_JSON = "known_lines_json"
        
        // Caching hashes for checking changes
        private const val KEY_SEFER_HASH = "sefer_hash_"
        private const val KEY_GUZERGAH_HASH = "guzergah_hash_"
        private const val KEY_FIRST_RUN = "first_run_line"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Hat ve sefer değişikliği kontrolü başladı...")
        return@withContext try {
            val api = SbbApiServisi.getInstance(applicationContext)
            val db = SakusDatabase.getInstance(applicationContext)
            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val firstRun = prefs.getBoolean(KEY_FIRST_RUN, true)
            val gson = Gson()
            var notificationCount = 0

            // ----------------------------------------------------
            // 1. HAT, SEFER SAATİ ve GÜZERGAH APİ'LERİNDEN KONTROLLER
            // ----------------------------------------------------
            
            // a. Hat Ekleme/Düzenleme/Kaldırma (Tüm hatların olduğu API'den kontrol edilir)
            val hatlar = api.tumHatlariGetir()
            val newHatMap = hatlar.associateBy { it.id }

            if (hatlar.isNotEmpty()) {
                val knownLinesJson = prefs.getString(KEY_KNOWN_LINES_JSON, "")
                
                if (firstRun || knownLinesJson.isNullOrEmpty()) {
                    prefs.edit()
                        .putString(KEY_KNOWN_LINES_JSON, gson.toJson(hatlar))
                        .putBoolean(KEY_FIRST_RUN, false)
                        .apply()
                } else {
                    val type = object : TypeToken<List<HatBilgisi>>() {}.type
                    val oldHatlar: List<HatBilgisi> = try { gson.fromJson(knownLinesJson, type) } catch (e: Exception) { emptyList() }
                    val oldHatMap = oldHatlar.associateBy { it.id }

                    val addedHats = hatlar.filter { !oldHatMap.containsKey(it.id) }
                    val removedHats = oldHatlar.filter { !newHatMap.containsKey(it.id) }
                    val changedHats = hatlar.filter { newHat -> 
                        val old = oldHatMap[newHat.id]
                        old != null && (old.ad != newHat.ad || old.hatNumarasi != newHat.hatNumarasi)
                    }

                    // Hat eklendi bildirimi
                    for (hat in addedHats.take(3)) {
                        LineUpdateNotificationHelper.showLineUpdateNotification(
                            context = applicationContext, title = "Yeni Hat Eklendi",
                            body = "${hat.ad} (${hat.hatNumarasi}) seferlerine başlamıştır.",
                            notificationId = 100000 + hat.id, isDuyuru = false
                        )
                    }

                    // Hat kaldırıldı bildirimi
                    for (hat in removedHats.take(3)) {
                        LineUpdateNotificationHelper.showLineUpdateNotification(
                            context = applicationContext, title = "Hat Kaldırıldı",
                            body = "${hat.ad} (${hat.hatNumarasi}) seferden kaldırılmıştır.",
                            notificationId = 200000 + hat.id, isDuyuru = false
                        )
                    }

                    // Hat düzenlendi bildirimi (İsim, Numaralama)
                    for (hat in changedHats.take(3)) {
                        LineUpdateNotificationHelper.showLineUpdateNotification(
                            context = applicationContext, title = "Hat Güncellendi",
                            body = "${hat.ad} güzergah adında/numarasında değişiklik yapıldı.",
                            notificationId = 300000 + hat.id, isDuyuru = false
                        )
                    }

                    // Taranan en güncel hat listesini veritabanına kaydet (Kullanıcı Çevrimdışı Girebilsin/Güncel Kalsın)
                    val entities = hatlar.map { HatEntity.fromHatBilgisi(it) }
                    db.hatDao().tumunuSil()
                    db.hatDao().hatlariKaydet(entities)

                    prefs.edit().putString(KEY_KNOWN_LINES_JSON, gson.toJson(hatlar)).apply()
                }
            }

            if (firstRun) {
                return@withContext Result.success()
            }

            // b & c. Sefer Saati ve Güzergah/Durak Değişiklikleri (Tüm Hatlar İçin)
            val allHatIds = newHatMap.keys
            val currentCalendar = Calendar.getInstance()
            
            for (hatId in allHatIds) {
                if (notificationCount > 10) break // Aşırı bildirim spam'ini önlemek için

                // 1.5 saniye bekle (Rate limits'i aşmamak için)
                delay(1500)

                val hatNumarasi = newHatMap[hatId]?.hatNumarasi ?: "Bilinmeyen"

                // Sefer saati kontrolü
                try {
                    val newSefer = api.seferSaatleriGetirByDate(hatId, currentCalendar)
                    if (newSefer != null) {
                        val newHash = gson.toJson(newSefer.seferler).hashCode()
                        val prefKey = KEY_SEFER_HASH + hatId
                        val oldHash = prefs.getInt(prefKey, newHash)
                        
                        // Günün tipine hesapla ve veritabanına doğrudan kaydet
                        val dayOfWeek = currentCalendar.get(Calendar.DAY_OF_WEEK)
                        val dayType = when (dayOfWeek) {
                            Calendar.SATURDAY -> 1
                            Calendar.SUNDAY -> 2
                            else -> 0
                        }

                        if (newHash != oldHash) {
                            // Değişikliği Diff etmek için eskisini önce al
                            val oldEntity = db.seferSaatleriDao().seferSaatleriGetir(hatId, dayType)
                            val oldSefer = oldEntity?.seferBilgisiJson?.let { gson.fromJson(it, com.berat.sakus.data.HatSeferBilgisi::class.java) }
                            
                            if (oldSefer != null) {
                                // Fetch guzergah from API or DB to get start/end location names
                                val sbbId = newHatMap[hatId]?.id ?: hatId
                                val guzergahEntity = db.guzergahDao().guzergahGetir(sbbId)
                                val guzergahBilgisi = if (guzergahEntity != null) {
                                    com.berat.sakus.data.HatGuzergahBilgisi(
                                        guzergahKoordinatlari = gson.fromJson(guzergahEntity.koordinatlarJson, object : com.google.gson.reflect.TypeToken<List<List<Double>>>() {}.type),
                                        guzergahlar = gson.fromJson(guzergahEntity.guzergahlarJson, object : com.google.gson.reflect.TypeToken<Map<Int, List<List<Double>>>>() {}.type),
                                        duraklar = gson.fromJson(guzergahEntity.duraklarJson, object : com.google.gson.reflect.TypeToken<List<com.berat.sakus.data.DurakBilgisi>>() {}.type),
                                        yonler = gson.fromJson(guzergahEntity.yonlerJson, object : com.google.gson.reflect.TypeToken<List<com.berat.sakus.data.YonBilgisi>>() {}.type)
                                    )
                                } else {
                                    api.guzergahVeDuraklariGetir(sbbId)
                                }

                                val htmlDiff = DiffHelper.generateSeferDiffHtml(oldSefer, newSefer, guzergahBilgisi)
                                val notifDate = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(currentCalendar.time)
                                val notif = com.berat.sakus.data.local.entity.AppNotificationEntity(
                                    hatNumarasi = hatNumarasi,
                                    hatAdi = newHatMap[hatId]?.ad,
                                    baslik = "Sefer Saatleri Güncellendi",
                                    aciklama = "$hatNumarasi numaralı hattın sefer saatlerinde değişiklik yapılmıştır. Detayları inceleyin.",
                                    icerikHtml = htmlDiff,
                                    islemTarihi = notifDate
                                )
                                db.appNotificationDao().insertNotification(notif)
                            }

                            LineUpdateNotificationHelper.showLineUpdateNotification(
                                context = applicationContext, title = "Sefer Saati Güncellemesi",
                                body = "$hatNumarasi numaralı hattın sefer saatleri güncellendi.",
                                notificationId = 400000 + hatId, isDuyuru = false
                            )
                            notificationCount++
                        }

                        val seferEntity = SeferSaatleriEntity(
                            hatId = hatId,
                            dayType = dayType,
                            seferBilgisiJson = gson.toJson(newSefer)
                        )
                        db.seferSaatleriDao().seferSaatleriKaydet(seferEntity)

                        prefs.edit().putInt(prefKey, newHash).apply()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Sefer saati kontrolü hatası hatId=$hatId: ${e.message}")
                }

                // Güzergah kontrolü
                try {
                    val newGuzergah = api.guzergahVeDuraklariGetir(hatId)
                    if (newGuzergah != null) {
                        val newHash = gson.toJson(newGuzergah.duraklar).hashCode()
                        val prefKey = KEY_GUZERGAH_HASH + hatId
                        val oldHash = prefs.getInt(prefKey, newHash)

                        // Taranan en güncel güzergah / durak listesini veritabanına kaydet
                        val guzergahEntity = GuzergahEntity(
                            hatId = hatId,
                            koordinatlarJson = gson.toJson(newGuzergah.guzergahKoordinatlari),
                            guzergahlarJson = gson.toJson(newGuzergah.guzergahlar),
                            duraklarJson = gson.toJson(newGuzergah.duraklar),
                            yonlerJson = gson.toJson(newGuzergah.yonler)
                        )
                        db.guzergahDao().guzergahKaydet(guzergahEntity)

                        if (newHash != oldHash) {
                            LineUpdateNotificationHelper.showLineUpdateNotification(
                                context = applicationContext, title = "Güzergah/Durak Değişikliği",
                                body = "$hatNumarasi numaralı hattın güzergah duraklarında değişiklik yapıldı.",
                                notificationId = 500000 + hatId, isDuyuru = false
                            )
                            notificationCount++
                        }
                        prefs.edit().putInt(prefKey, newHash).apply()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Güzergah kontrolü hatası hatId=$hatId: ${e.message}")
                }
            }

            // ----------------------------------------------------
            // 2. DUYURULAR KONTROLÜ (Acil bildirimler, Anonslar)
            // ----------------------------------------------------
            val genelDuyurular = api.duyurulariGetir(20)
            val hatDuyurular = api.tumHatDuyurulariGetir(50)
            val allDuyurular = (genelDuyurular + hatDuyurular).distinctBy { it.id }.sortedByDescending { it.id }
            val knownDuyuruIds = prefs.getStringSet(KEY_KNOWN_DUYURU_IDS, emptySet()) ?: emptySet()
            var newDuyuruIdsList = knownDuyuruIds.toMutableSet()

            val newDuyurular = allDuyurular.filter { it.id.toString() !in knownDuyuruIds }
            if (newDuyurular.isNotEmpty()) {
                val toNotify = newDuyurular.take(3)
                for (duyuru in toNotify) {
                    val body = duyuru.duzMetin.take(150).plus(if (duyuru.duzMetin.length > 150) "..." else "")
                    LineUpdateNotificationHelper.showLineUpdateNotification(
                        context = applicationContext, title = duyuru.baslik,
                        body = body.ifEmpty { "Ulaşım duyurusu ve hat güncellemesi." },
                        notificationId = duyuru.id, isDuyuru = true
                    )
                }
                newDuyuruIdsList.addAll(newDuyurular.map { it.id.toString() })
            }
            prefs.edit().putStringSet(KEY_KNOWN_DUYURU_IDS, newDuyuruIdsList).apply()

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Özel API senkronizasyon kontrolü hatası: ${e.message}")
            Result.retry()
        }
    }
}
