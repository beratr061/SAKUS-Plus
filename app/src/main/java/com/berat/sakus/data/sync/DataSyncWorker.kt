package com.berat.sakus.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.berat.sakus.data.repository.TransportRepository

/**
 * WorkManager Worker — Saatlik periyodik senkronizasyon.
 * API'den güncel verileri çeker ve değişiklikleri DB'ye yazar.
 */
class DataSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "DataSyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Periyodik senkronizasyon başladı...")
        return try {
            val repository = TransportRepository.getInstance(applicationContext)
            repository.periodicSync()
            Log.d(TAG, "Periyodik senkronizasyon tamamlandı.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Periyodik senkronizasyon hatası: ${e.message}")
            Result.retry()
        }
    }
}
