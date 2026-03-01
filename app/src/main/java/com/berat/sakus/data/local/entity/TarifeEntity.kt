package com.berat.sakus.data.local.entity

import androidx.room.Entity

/**
 * Fiyat tarifesi bilgisini hat bazında saklar.
 */
@Entity(tableName = "tarifeler", primaryKeys = ["hatId", "aracTipId"])
data class TarifeEntity(
    val hatId: Int,
    val aracTipId: Int,
    val tarifeBilgisiJson: String,  // TarifeBilgisi -> JSON
    val lastUpdated: Long = System.currentTimeMillis()
)
