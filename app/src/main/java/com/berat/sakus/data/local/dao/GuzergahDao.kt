package com.berat.sakus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.berat.sakus.data.local.entity.GuzergahEntity

@Dao
interface GuzergahDao {
    @Query("SELECT * FROM guzergahlar WHERE hatId = :hatId LIMIT 1")
    suspend fun guzergahGetir(hatId: Int): GuzergahEntity?

    @Query("SELECT hatId FROM guzergahlar")
    suspend fun tumGuzergahHatIdleriGetir(): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guzergahKaydet(guzergah: GuzergahEntity)

    @Query("DELETE FROM guzergahlar WHERE hatId = :hatId")
    suspend fun guzergahSil(hatId: Int)

    @Query("DELETE FROM guzergahlar")
    suspend fun tumunuSil()
}
