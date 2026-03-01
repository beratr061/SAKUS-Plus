package com.berat.sakus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Güzergah bilgisini hat bazında saklar.
 * Karmaşık nested veriler (koordinatlar, duraklar, yönler) JSON string olarak depolanır.
 */
@Entity(tableName = "guzergahlar")
data class GuzergahEntity(
    @PrimaryKey val hatId: Int,
    val koordinatlarJson: String,       // List<List<Double>> -> JSON
    val guzergahlarJson: String,        // Map<Int, List<List<Double>>> -> JSON
    val duraklarJson: String,           // List<DurakBilgisi> -> JSON
    val yonlerJson: String,             // List<YonBilgisi> -> JSON
    val lastUpdated: Long = System.currentTimeMillis()
)
