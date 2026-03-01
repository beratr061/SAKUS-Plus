package com.berat.sakus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.berat.sakus.data.local.entity.AppNotificationEntity

@Dao
interface AppNotificationDao {
    @Query("SELECT * FROM app_notifications ORDER BY id DESC")
    suspend fun getAllNotifications(): List<AppNotificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotificationEntity)
}
