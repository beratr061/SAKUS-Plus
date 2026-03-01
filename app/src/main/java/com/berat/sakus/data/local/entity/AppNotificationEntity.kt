package com.berat.sakus.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.berat.sakus.data.Duyuru

@Entity(tableName = "app_notifications")
data class AppNotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hatNumarasi: String?,
    val hatAdi: String?,
    val baslik: String,
    val aciklama: String?,
    val icerikHtml: String,
    val islemTarihi: String
) {
    fun toDuyuru(): Duyuru {
        return Duyuru(
            id = -id, // Return negative ID so it doesn't clash with actual API announcements
            baslik = baslik,
            aciklama = aciklama ?: baslik, // Provide default non-null via baslik
            icerik = icerikHtml,
            baslangicTarih = islemTarihi,
            bitisTarih = "", // Null bitisTarihi is actually a string type in Duyuru model
            hatId = null,
            hatAdi = hatAdi,
            hatNumarasi = hatNumarasi,
            kategoriAdi = "Uygulama İçi Bildirim",
            renk = "#BA47E7" // Default color
        )
    }
}
