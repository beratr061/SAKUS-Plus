package com.berat.sakus.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.berat.sakus.data.SbbApiServisi
import com.berat.sakus.data.notification.NewsNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Periyodik haber kontrolü. Yeni haber geldiğinde bildirim gösterir.
 */
class NewsCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "NewsCheckWorker"
        private const val PREFS_NAME = "news_check_prefs"
        private const val KEY_KNOWN_IDS = "known_news_ids"
        private const val KEY_FIRST_RUN = "first_run"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Haber kontrolü başladı...")
        return@withContext try {
            val api = SbbApiServisi.getInstance(applicationContext)
            val haberler = api.haberleriGetir(52)

            val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val firstRun = prefs.getBoolean(KEY_FIRST_RUN, true)

            val knownIds = prefs.getStringSet(KEY_KNOWN_IDS, emptySet()) ?: emptySet()

            if (firstRun && haberler.isNotEmpty()) {
                // İlk çalıştırma: mevcut haberleri kaydet, bildirim gösterme
                val newKnownIds = haberler.map { it.id.toString() }.toSet()
                prefs.edit()
                    .putStringSet(KEY_KNOWN_IDS, newKnownIds)
                    .putBoolean(KEY_FIRST_RUN, false)
                    .apply()
                Log.d(TAG, "İlk çalıştırma: ${newKnownIds.size} haber kaydedildi")
                return@withContext Result.success()
            }

            val newHaberler = haberler.filter { it.id.toString() !in knownIds }
            if (newHaberler.isEmpty()) {
                Log.d(TAG, "Yeni haber yok")
                return@withContext Result.success()
            }

            // Yeni haberlere bildirim göster (en fazla 3, en yeniden eskiye)
            val toNotify = newHaberler.sortedByDescending { it.id }.take(3)
            for (haber in toNotify) {
                val body = haber.foreword
                    ?.replace(Regex("<[^>]*>"), "")
                    ?.replace("&nbsp;", " ")
                    ?.replace("&amp;", "&")
                    ?.take(150)
                    ?.trim()
                    ?.plus(if ((haber.foreword?.length ?: 0) > 150) "..." else "")
                    ?: "Yeni haber yayınlandı."
                NewsNotificationHelper.showNewsNotification(
                    context = applicationContext,
                    title = haber.title,
                    body = body,
                    newsId = haber.id
                )
                Log.d(TAG, "Bildirim gösterildi: ${haber.title}")
            }

            // Güncel ID'leri kaydet
            val updatedIds = knownIds + newHaberler.map { it.id.toString() }
            prefs.edit().putStringSet(KEY_KNOWN_IDS, updatedIds).apply()

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Haber kontrolü hatası: ${e.message}")
            Result.retry()
        }
    }
}
