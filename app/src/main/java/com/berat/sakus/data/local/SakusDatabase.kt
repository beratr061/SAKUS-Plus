package com.berat.sakus.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.berat.sakus.data.local.dao.AppNotificationDao
import com.berat.sakus.data.local.dao.GuzergahDao
import com.berat.sakus.data.local.dao.HatDao
import com.berat.sakus.data.local.dao.SeferSaatleriDao
import com.berat.sakus.data.local.dao.TarifeDao
import com.berat.sakus.data.local.entity.AppNotificationEntity
import com.berat.sakus.data.local.entity.GuzergahEntity
import com.berat.sakus.data.local.entity.HatEntity
import com.berat.sakus.data.local.entity.SeferSaatleriEntity
import com.berat.sakus.data.local.entity.TarifeEntity

@Database(
    entities = [
        HatEntity::class,
        GuzergahEntity::class,
        SeferSaatleriEntity::class,
        TarifeEntity::class,
        AppNotificationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SakusDatabase : RoomDatabase() {
    abstract fun hatDao(): HatDao
    abstract fun guzergahDao(): GuzergahDao
    abstract fun seferSaatleriDao(): SeferSaatleriDao
    abstract fun tarifeDao(): TarifeDao
    abstract fun appNotificationDao(): AppNotificationDao

    companion object {
        @Volatile
        private var INSTANCE: SakusDatabase? = null

        fun getInstance(context: Context): SakusDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SakusDatabase::class.java,
                    "sakus_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
