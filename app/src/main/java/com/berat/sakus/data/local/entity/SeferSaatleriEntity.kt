package com.berat.sakus.data.local.entity

import androidx.room.Entity

/**
 * Sefer saatleri bilgisini hat ve gün tipi bazında saklar.
 * dayType: 0 = Hafta içi, 1 = Cumartesi, 2 = Pazar
 */
@Entity(tableName = "sefer_saatleri", primaryKeys = ["hatId", "dayType"])
data class SeferSaatleriEntity(
    val hatId: Int,
    val dayType: Int,  // 0=weekday, 1=saturday, 2=sunday
    val seferBilgisiJson: String,  // HatSeferBilgisi -> JSON
    val lastUpdated: Long = System.currentTimeMillis()
)
