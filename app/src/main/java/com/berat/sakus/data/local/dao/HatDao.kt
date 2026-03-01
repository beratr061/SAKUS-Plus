package com.berat.sakus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.berat.sakus.data.local.entity.HatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HatDao {
    @Query("SELECT * FROM hatlar ORDER BY hatNumarasi ASC")
    fun tumHatlariGetir(): Flow<List<HatEntity>>

    @Query("SELECT * FROM hatlar ORDER BY hatNumarasi ASC")
    suspend fun tumHatlariGetirOnce(): List<HatEntity>

    @Query("SELECT COUNT(*) FROM hatlar")
    suspend fun hatSayisi(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hatlariKaydet(hatlar: List<HatEntity>)

    @Query("DELETE FROM hatlar")
    suspend fun tumunuSil()
}
