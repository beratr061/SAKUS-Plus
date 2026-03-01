package com.berat.sakus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.berat.sakus.data.local.entity.SeferSaatleriEntity

@Dao
interface SeferSaatleriDao {
    @Query("SELECT * FROM sefer_saatleri WHERE hatId = :hatId AND dayType = :dayType LIMIT 1")
    suspend fun seferSaatleriGetir(hatId: Int, dayType: Int): SeferSaatleriEntity?

    @Query("SELECT * FROM sefer_saatleri WHERE hatId = :hatId")
    suspend fun hatSeferSaatleriGetir(hatId: Int): List<SeferSaatleriEntity>

    @Query("SELECT DISTINCT hatId FROM sefer_saatleri")
    suspend fun distinctHatIdleriGetir(): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun seferSaatleriKaydet(entity: SeferSaatleriEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun seferSaatleriTopluKaydet(entities: List<SeferSaatleriEntity>)

    @Query("DELETE FROM sefer_saatleri WHERE hatId = :hatId")
    suspend fun hatSeferSaatleriSil(hatId: Int)

    @Query("DELETE FROM sefer_saatleri")
    suspend fun tumunuSil()
}
