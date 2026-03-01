package com.berat.sakus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.berat.sakus.data.local.entity.TarifeEntity

@Dao
interface TarifeDao {
    @Query("SELECT * FROM tarifeler WHERE hatId = :hatId AND aracTipId = :aracTipId LIMIT 1")
    suspend fun tarifeGetir(hatId: Int, aracTipId: Int): TarifeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun tarifeKaydet(entity: TarifeEntity)

    @Query("DELETE FROM tarifeler WHERE hatId = :hatId")
    suspend fun hatTarifeSil(hatId: Int)

    @Query("DELETE FROM tarifeler")
    suspend fun tumunuSil()
}
