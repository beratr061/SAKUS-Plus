package com.berat.sakus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.berat.sakus.data.HatBilgisi

@Entity(tableName = "hatlar")
data class HatEntity(
    @PrimaryKey val id: Int,
    val ad: String,
    val hatNumarasi: String,
    val aracTipAdi: String,
    val aracTipAciklama: String,
    val aracTipRenk: String,
    val aracTipId: Int,
    val asisId: Int?,
    val slug: String
) {
    fun toHatBilgisi(): HatBilgisi {
        return HatBilgisi(
            id = id,
            ad = ad,
            hatNumarasi = hatNumarasi,
            aracTipAdi = aracTipAdi,
            aracTipAciklama = aracTipAciklama,
            aracTipRenk = aracTipRenk,
            aracTipId = aracTipId,
            asisId = asisId,
            slug = slug
        )
    }

    companion object {
        fun fromHatBilgisi(hat: HatBilgisi): HatEntity {
            return HatEntity(
                id = hat.id,
                ad = hat.ad,
                hatNumarasi = hat.hatNumarasi,
                aracTipAdi = hat.aracTipAdi,
                aracTipAciklama = hat.aracTipAciklama,
                aracTipRenk = hat.aracTipRenk,
                aracTipId = hat.aracTipId,
                asisId = hat.asisId,
                slug = hat.slug
            )
        }
    }
}
