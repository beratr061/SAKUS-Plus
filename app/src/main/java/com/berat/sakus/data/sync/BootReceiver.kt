package com.berat.sakus.data.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Cihaz yeniden başlatıldığında veya uygulama güncellendikten sonra
 * periyodik Worker'ları otomatik olarak yeniden planlar.
 * Böylece kullanıcı uygulamayı açmasa bile bildirimler gelmeye devam eder.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            Log.d(TAG, "Boot/update algılandı, Worker'lar planlanıyor...")
            SyncManager.schedulePeriodicSync(context)
            SyncManager.scheduleNewsCheck(context)
            SyncManager.scheduleLineUpdateCheck(context)
        }
    }
}
