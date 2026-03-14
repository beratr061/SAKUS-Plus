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
 * Periyodik hat ve duyuru kontrolü.
 * Hat eklendiğinde/kaldırıldığında veya sefer/güzergah değişikliği olduğunda bildirim gösterir.
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
        private const val KEY_SEFER_HASH = "sefer_hash_"
        private const val KEY_GUZERGAH_HASH = "guzergah_hash_"
        private const val KEY_FIRST_RUN = "first_run_line"
        private const val KEY_HASHES_INITIALIZED = "hashes_initialized"
        private const val MAX_NOTIFICATIONS_PER_RUN = 5
        // Her çalışmada kontrol edilecek hat sayısı (rate limit aşımını önler)
        private const val BATCH_SIZE = 15
        private const val KEY_LAST_BATCH_INDEX = "last_batch_index"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Hat ve sefer değişikliği kontrolü başladı...")
        return@withContext try {
            val api = SbbApiServisi.getInstance(applicationContext)
            val db = SakusDatabase.getInstance(applicationContext)
            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val firstRun = prefs.getBoolean(KEY_FIRST_RUN, true)
            val hashesInitialized = prefs.getBoolean(KEY_HASHES_INITIALIZED, false)
            val gson = Gson()
            var notificationCount = 0

            // ──────────────────────────────────────────────
            // 1. HAT LİSTESİ KONTROLÜ
            // ──────────────────────────────────────────────
            val hatlar = api.tumHatlariGetir()
            val newHatMap = hatlar.associateBy { it.id }

            if (hatlar.isNotEmpty()) {
                val knownLinesJson = prefs.getString(KEY_KNOWN_LINES_JSON, "")

                if (firstRun || knownLinesJson.isNullOrEmpty()) {
                    // İlk çalışma: mevcut hatları kaydet, bildirim gösterme
                    val entities = hatlar.map { HatEntity.fromHatBilgisi(it) }
                    db.hatDao().tumunuSil()
                    db.hatDao().hatlariKaydet(entities)
                    prefs.edit()
                        .putString(KEY_KNOWN_LINES_JSON, gson.toJson(hatlar))
                        .putBoolean(KEY_FIRST_RUN, false)
                        .apply()
                    Log.d(TAG, "İlk çalışma: ${hatlar.size} hat kaydedildi")
                } else {
                    val type = object : TypeToken<List<HatBilgisi>>() {}.type
                    val oldHatlar: List<HatBilgisi> = try {
                        gson.fromJson(knownLinesJson, type)
                    } catch (e: Exception) {
                        emptyList()
                    }
                    val oldHatMap = oldHatlar.associateBy { it.id }

                    val addedHats = hatlar.filter { !oldHatMap.containsKey(it.id) }
                    val removedHats = oldHatlar.filter { !newHatMap.containsKey(it.id) }
                    val changedHats = hatlar.filter { newHat ->
                        val old = oldHatMap[newHat.id]
                        old != null && (old.ad != newHat.ad || old.hatNumarasi != newHat.hatNumarasi)
                    }

                    for (hat in addedHats.take(3)) {
                        if (notificationCount >= MAX_NOTIFICATIONS_PER_RUN) break
                        LineUpdateNotificationHelper.showLineUpdateNotification(
                            context = applicationContext, title = "Yeni Hat Eklendi",
                            body = "${hat.ad} (${hat.hatNumarasi}) seferlerine başlamıştır.",
                            notificationId = 100000 + hat.id, isDuyuru = false
                        )
                        notificationCount++
                    }

                    for (hat in removedHats.take(3)) {
                        if (notificationCount >= MAX_NOTIFICATIONS_PER_RUN) break
                        LineUpdateNotificationHelper.showLineUpdateNotification(
                            context = applicationContext, title = "Hat Kaldırıldı",
                            body = "${hat.ad} (${hat.hatNumarasi}) seferden kaldırılmıştır.",
                            notificationId = 200000 + hat.id, isDuyuru = false
                        )
                        notificationCount++
                    }

                    for (hat in changedHats.take(3)) {
                        if (notificationCount >= MAX_NOTIFICATIONS_PER_RUN) break
                        LineUpdateNotificationHelper.showLineUpdateNotification(
                            context = applicationContext, title = "Hat Güncellendi",
                            body = "${hat.ad} güzergah adında/numarasında değişiklik yapıldı.",
                            notificationId = 300000 + hat.id, isDuyuru = false
                        )
                        notificationCount++
                    }

                    // DB'yi güncelle
                    val entities = hatlar.map { HatEntity.fromHatBilgisi(it) }
                    db.hatDao().tumunuSil()
                    db.hatDao().hatlariKaydet(entities)
                    prefs.edit().putString(KEY_KNOWN_LINES_JSON, gson.toJson(hatlar)).apply()
                }
            }

            // ──────────────────────────────────────────────
            // 2. SEFER & GÜZERGAH DEĞİŞİKLİKLERİ (batch halinde)
            // ──────────────────────────────────────────────
            val allHatIds = newHatMap.keys.toList().sorted()
            val currentCalendar = Calendar.getInstance()

            if (allHatIds.isNotEmpty()) {
                // Hash'ler henüz initialize edilmediyse, sadece kaydet, bildirim gösterme
                val isInitRun = !hashesInitialized

                val lastIndex = prefs.getInt(KEY_LAST_BATCH_INDEX, 0)
                val startIndex = if (lastIndex >= allHatIds.size) 0 else lastIndex
                val batch = allHatIds.drop(startIndex).take(BATCH_SIZE)
                val nextIndex = if (startIndex + BATCH_SIZE >= allHatIds.size) 0 else startIndex + BATCH_SIZE

                val dayOfWeek = currentCalendar.get(Calendar.DAY_OF_WEEK)
                val dayType = when (dayOfWeek) {
                    Calendar.SATURDAY -> 1
                    Calendar.SUNDAY -> 2
                    else -> 0
                }

                for (hatId in batch) {
                    if (notificationCount >= MAX_NOTIFICATIONS_PER_RUN) break
                    delay(1000)

                    val hatNumarasi = newHatMap[hatId]?.hatNumarasi ?: "?"

                    // Sefer saati kontrolü
                    try {
                        val newSefer = api.seferSaatleriGetirByDate(hatId, currentCalendar)
                        if (newSefer != null) {
                            val newHash = gson.toJson(newSefer.seferler).hashCode()
                            val prefKey = KEY_SEFER_HASH + hatId
                            val hasOldHash = prefs.contains(prefKey)
                            val oldHash = prefs.getInt(prefKey, 0)

                            if (hasOldHash && !isInitRun && newHash != oldHash) {
                                // Değişiklik var — diff oluştur ve bildir
                                val oldEntity = db.seferSaatleriDao().seferSaatleriGetir(hatId, dayType)
                                val oldSefer = oldEntity?.seferBilgisiJson?.let {
                                    gson.fromJson(it, com.berat.sakus.data.HatSeferBilgisi::class.java)
                                }

                                if (oldSefer != null) {
                                    val sbbId = newHatMap[hatId]?.id ?: hatId
                                    val guzergahEntity = db.guzergahDao().guzergahGetir(sbbId)
                                    val guzergahBilgisi = if (guzergahEntity != null) {
                                        com.berat.sakus.data.HatGuzergahBilgisi(
                                            guzergahKoordinatlari = gson.fromJson(guzergahEntity.koordinatlarJson, object : com.google.gson.reflect.TypeToken<List<List<Double>>>() {}.type),
                                            guzergahlar = gson.fromJson(guzergahEntity.guzergahlarJson, object : com.google.gson.reflect.TypeToken<Map<Int, List<List<Double>>>>() {}.type),
                                            duraklar = gson.fromJson(guzergahEntity.duraklarJson, object : com.google.gson.reflect.TypeToken<List<com.berat.sakus.data.DurakBilgisi>>() {}.type),
                                            yonler = gson.fromJson(guzergahEntity.yonlerJson, object : com.google.gson.reflect.TypeToken<List<com.berat.sakus.data.YonBilgisi>>() {}.type)
                                        )
                                    } else null

                                    val htmlDiff = DiffHelper.generateSeferDiffHtml(oldSefer, newSefer, guzergahBilgisi)
                                    val notifDate = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(currentCalendar.time)
                                    val notif = com.berat.sakus.data.local.entity.AppNotificationEntity(
                                        hatNumarasi = hatNumarasi,
                                        hatAdi = newHatMap[hatId]?.ad,
                                        baslik = "Sefer Saatleri Güncellendi",
                                        aciklama = "$hatNumarasi numaralı hattın sefer saatlerinde değişiklik yapılmıştır.",
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

                            // Hash'i ve sefer verisini her zaman kaydet
                            val seferEntity = SeferSaatleriEntity(
                                hatId = hatId, dayType = dayType,
                                seferBilgisiJson = gson.toJson(newSefer)
                            )
                            db.seferSaatleriDao().seferSaatleriKaydet(seferEntity)
                            prefs.edit().putInt(prefKey, newHash).apply()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Sefer kontrolü hatası hatId=$hatId: ${e.message}")
                    }

                    // Güzergah kontrolü
                    try {
                        val newGuzergah = api.guzergahVeDuraklariGetir(hatId)
                        if (newGuzergah != null) {
                            val newHash = gson.toJson(newGuzergah.duraklar).hashCode()
                            val prefKey = KEY_GUZERGAH_HASH + hatId
                            val hasOldHash = prefs.contains(prefKey)
                            val oldHash = prefs.getInt(prefKey, 0)

                            val guzergahEntity = GuzergahEntity(
                                hatId = hatId,
                                koordinatlarJson = gson.toJson(newGuzergah.guzergahKoordinatlari),
                                guzergahlarJson = gson.toJson(newGuzergah.guzergahlar),
                                duraklarJson = gson.toJson(newGuzergah.duraklar),
                                yonlerJson = gson.toJson(newGuzergah.yonler)
                            )
                            db.guzergahDao().guzergahKaydet(guzergahEntity)

                            if (hasOldHash && !isInitRun && newHash != oldHash) {
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

                prefs.edit()
                    .putInt(KEY_LAST_BATCH_INDEX, nextIndex)
                    .putBoolean(KEY_HASHES_INITIALIZED, true)
                    .apply()
            }

            // ──────────────────────────────────────────────
            // 3. DUYURULAR KONTROLÜ
            // ──────────────────────────────────────────────
            val genelDuyurular = api.duyurulariGetir(20)
            val hatDuyurular = api.tumHatDuyurulariGetir(50)
            val allDuyurular = (genelDuyurular + hatDuyurular).distinctBy { it.id }.sortedByDescending { it.id }
            val knownDuyuruIds = prefs.getStringSet(KEY_KNOWN_DUYURU_IDS, emptySet())?.toSet() ?: emptySet()

            val newDuyurular = allDuyurular.filter { it.id.toString() !in knownDuyuruIds }
            if (newDuyurular.isNotEmpty() && !firstRun) {
                for (duyuru in newDuyurular.take(3)) {
                    if (notificationCount >= MAX_NOTIFICATIONS_PER_RUN) break
                    val body = duyuru.duzMetin.take(150).plus(if (duyuru.duzMetin.length > 150) "..." else "")
                    LineUpdateNotificationHelper.showLineUpdateNotification(
                        context = applicationContext, title = duyuru.baslik,
                        body = body.ifEmpty { "Ulaşım duyurusu ve hat güncellemesi." },
                        notificationId = duyuru.id, isDuyuru = true
                    )
                    notificationCount++
                }
            }
            // Bilinen duyuru ID'lerini her zaman güncelle
            val updatedDuyuruIds = knownDuyuruIds + allDuyurular.map { it.id.toString() }
            prefs.edit().putStringSet(KEY_KNOWN_DUYURU_IDS, updatedDuyuruIds.toSet()).apply()

            Log.d(TAG, "Kontrol tamamlandı. $notificationCount bildirim gönderildi.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Hat güncelleme kontrolü hatası: ${e.message}")
            Result.retry()
        }
    }
}
