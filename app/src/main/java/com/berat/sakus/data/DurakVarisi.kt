package com.berat.sakus.data

/**
 * Durağa yaklaşan otobüs bilgisi - canlı veriden türetilir.
 */
data class DurakVarisi(
    val hatNo: String,
    val hatAdi: String,
    val plaka: String,
    val aracNumarasi: Int,
    val dakika: Int,
    val guzergahAdi: String
) {
    val dakikaMetin: String
        get() = when {
            dakika <= 0 -> "Durakta"
            dakika == 1 -> "1 dk"
            else -> "$dakika dk"
        }
}
