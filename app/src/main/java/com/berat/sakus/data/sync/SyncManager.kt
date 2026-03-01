package com.berat.sakus.data.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * WorkManager periyodik senkronizasyonu yönetir.
 * Her saat başı API'den güncel verileri kontrol eder.
 */
object SyncManager {

    private const val TAG = "SyncManager"
    private const val PERIODIC_SYNC_WORK = "periodic_data_sync"
    private const val NEWS_CHECK_WORK = "news_check_periodic"
    private const val LINE_UPDATE_CHECK_WORK = "line_update_check_periodic"

    /**
     * Saatlik periyodik senkronizasyonu başlatır.
     * Uygulama ilk açıldığında çağrılmalıdır.
     */
    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<DataSyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(PERIODIC_SYNC_WORK)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWork
        )

        Log.d(TAG, "Periyodik senkronizasyon planlandı (her 15 dakika)")
    }

    /**
     * Periyodik haber kontrolünü başlatır.
     * Yeni haber geldiğinde bildirim gösterir.
     */
    fun scheduleNewsCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val newsWork = PeriodicWorkRequestBuilder<NewsCheckWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(NEWS_CHECK_WORK)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NEWS_CHECK_WORK,
            ExistingPeriodicWorkPolicy.REPLACE,
            newsWork
        )

        Log.d(TAG, "Haber kontrolü planlandı (her 15 dakika)")
    }

    /**
     * Periyodik ulaşım değişiklikleri kontrolünü başlatır.
     * Hat güncellemelerinde bildirim gösterir.
     */
    fun scheduleLineUpdateCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val lineUpdateWork = PeriodicWorkRequestBuilder<LineUpdateWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(LINE_UPDATE_CHECK_WORK)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            LINE_UPDATE_CHECK_WORK,
            ExistingPeriodicWorkPolicy.REPLACE,
            lineUpdateWork
        )

        Log.d(TAG, "Hat güncellemeleri kontrolü planlandı (her 15 dakika)")
    }

    /**
     * Periyodik senkronizasyonu iptal eder.
     */
    fun cancelPeriodicSync(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_SYNC_WORK)
        Log.d(TAG, "Periyodik senkronizasyon iptal edildi")
    }
}
