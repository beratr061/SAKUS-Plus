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
 * Arka planda çalışarak uygulama kapalıyken bile kontrol yapar.
 */
object SyncManager {

    private const val TAG = "SyncManager"
    private const val PERIODIC_SYNC_WORK = "periodic_data_sync"
    private const val NEWS_CHECK_WORK = "news_check_periodic"
    private const val LINE_UPDATE_CHECK_WORK = "line_update_check_periodic"

    private val networkConstraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedulePeriodicSync(context: Context) {
        val work = PeriodicWorkRequestBuilder<DataSyncWorker>(
            30, TimeUnit.MINUTES
        )
            .setConstraints(networkConstraints)
            .setInitialDelay(2, TimeUnit.MINUTES)
            .addTag(PERIODIC_SYNC_WORK)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            work
        )
        Log.d(TAG, "Periyodik senkronizasyon planlandı (her 30 dakika)")
    }

    fun scheduleNewsCheck(context: Context) {
        val work = PeriodicWorkRequestBuilder<NewsCheckWorker>(
            30, TimeUnit.MINUTES
        )
            .setConstraints(networkConstraints)
            .setInitialDelay(1, TimeUnit.MINUTES)
            .addTag(NEWS_CHECK_WORK)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NEWS_CHECK_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            work
        )
        Log.d(TAG, "Haber kontrolü planlandı (her 30 dakika)")
    }

    fun scheduleLineUpdateCheck(context: Context) {
        val work = PeriodicWorkRequestBuilder<LineUpdateWorker>(
            60, TimeUnit.MINUTES
        )
            .setConstraints(networkConstraints)
            .setInitialDelay(5, TimeUnit.MINUTES)
            .addTag(LINE_UPDATE_CHECK_WORK)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            LINE_UPDATE_CHECK_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            work
        )
        Log.d(TAG, "Hat güncellemeleri kontrolü planlandı (her 60 dakika)")
    }
}
