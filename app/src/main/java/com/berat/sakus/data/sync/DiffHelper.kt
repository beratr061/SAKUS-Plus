package com.berat.sakus.data.sync

import com.berat.sakus.data.HatSeferBilgisi
import com.berat.sakus.data.models.GuzergahSefer

object DiffHelper {

    fun generateSeferDiffHtml(
        oldSefer: HatSeferBilgisi,
        newSefer: HatSeferBilgisi,
        guzergahBilgisi: com.berat.sakus.data.HatGuzergahBilgisi? = null
    ): String {
        val sb = StringBuilder()
        sb.append("<p>Aşağıdaki yönlerde ve saatlerde sefer değişiklikleri tespit edilmiştir:</p>")

        // Önce guzergahId ile eşle, eşleşmeyenleri yon bazlı eşle
        val oldByRoute = oldSefer.seferler.associateBy { it.guzergahId }
        val newByRoute = newSefer.seferler.associateBy { it.guzergahId }

        // Eşleşen çiftleri bul
        data class MatchedPair(val old: GuzergahSefer?, val new: GuzergahSefer?)

        val matched = mutableListOf<MatchedPair>()
        val usedOldIds = mutableSetOf<Int>()
        val usedNewIds = mutableSetOf<Int>()

        // 1. guzergahId ile doğrudan eşleşenler
        for ((routeId, newRoute) in newByRoute) {
            val oldRoute = oldByRoute[routeId]
            if (oldRoute != null) {
                matched.add(MatchedPair(oldRoute, newRoute))
                usedOldIds.add(routeId)
                usedNewIds.add(routeId)
            }
        }

        // 2. Eşleşmeyenleri yon bazlı eşle
        val unmatchedOld = oldSefer.seferler.filter { it.guzergahId !in usedOldIds }
        val unmatchedNew = newSefer.seferler.filter { it.guzergahId !in usedNewIds }
        val oldByYon = unmatchedOld.groupBy { it.yon }
        val newByYon = unmatchedNew.groupBy { it.yon }

        val allYons = (oldByYon.keys + newByYon.keys).toSet()
        for (yon in allYons) {
            val oldRoutes = oldByYon[yon] ?: emptyList()
            val newRoutes = newByYon[yon] ?: emptyList()
            val maxSize = maxOf(oldRoutes.size, newRoutes.size)
            for (i in 0 until maxSize) {
                matched.add(MatchedPair(oldRoutes.getOrNull(i), newRoutes.getOrNull(i)))
            }
        }

        var hasDiff = false

        for (pair in matched) {
            val oldRoute = pair.old
            val newRoute = pair.new
            val yon = newRoute?.yon ?: oldRoute?.yon ?: 0

            var routeName = newRoute?.guzergahAdi ?: oldRoute?.guzergahAdi ?: "Bilinmeyen Yön"
            routeName = resolveRouteName(routeName, yon, guzergahBilgisi)

            val oldTimes = oldRoute?.detaylar?.map { it.baslangicSaat }?.toSet() ?: emptySet()
            val newTimes = newRoute?.detaylar?.map { it.baslangicSaat }?.toSet() ?: emptySet()

            val addedTimes = newTimes.subtract(oldTimes).sorted()
            val removedTimes = oldTimes.subtract(newTimes).sorted()

            // Sefer sayısı değişikliği
            val oldCount = oldRoute?.detaylar?.size ?: 0
            val newCount = newRoute?.detaylar?.size ?: 0

            // Bitiş saati değişiklikleri
            val oldEndTimes = oldRoute?.detaylar?.associate { it.baslangicSaat to it.bitisSaat } ?: emptyMap()
            val newEndTimes = newRoute?.detaylar?.associate { it.baslangicSaat to it.bitisSaat } ?: emptyMap()
            val changedEndTimes = newEndTimes.filter { (start, end) ->
                oldEndTimes.containsKey(start) && oldEndTimes[start] != end
            }

            val hasChanges = addedTimes.isNotEmpty() || removedTimes.isNotEmpty() ||
                    changedEndTimes.isNotEmpty() || (oldRoute == null && newRoute != null) ||
                    (oldRoute != null && newRoute == null)

            if (hasChanges) {
                hasDiff = true
                sb.append("<br/><b>Yön: $routeName</b><br/>")
                sb.append("<ul>")

                if (oldRoute == null && newRoute != null) {
                    sb.append("<li><font color='#4CAF50'>Yeni güzergah eklendi ($newCount sefer)</font></li>")
                } else if (oldRoute != null && newRoute == null) {
                    sb.append("<li><font color='#F44336'>Güzergah kaldırıldı ($oldCount sefer)</font></li>")
                } else {
                    if (addedTimes.isNotEmpty()) {
                        sb.append("<li><b>Eklenen Saatler:</b> <font color='#4CAF50'>${addedTimes.joinToString(", ")}</font></li>")
                    }
                    if (removedTimes.isNotEmpty()) {
                        sb.append("<li><b>Kaldırılan Saatler:</b> <font color='#F44336'><strike>${removedTimes.joinToString(", ")}</strike></font></li>")
                    }
                    if (changedEndTimes.isNotEmpty()) {
                        val details = changedEndTimes.entries.joinToString(", ") { (start, newEnd) ->
                            val oldEnd = oldEndTimes[start] ?: "?"
                            "$start: $oldEnd → $newEnd"
                        }
                        sb.append("<li><b>Varış Saati Değişiklikleri:</b> $details</li>")
                    }
                    if (oldCount != newCount && addedTimes.isEmpty() && removedTimes.isEmpty()) {
                        sb.append("<li>Sefer sayısı: $oldCount → $newCount</li>")
                    }
                }
                sb.append("</ul>")
            }
        }

        if (!hasDiff) {
            // Saat bazlı fark yok ama hash değişmiş — genel bilgi ver
            val oldTotal = oldSefer.seferler.sumOf { it.detaylar.size }
            val newTotal = newSefer.seferler.sumOf { it.detaylar.size }
            return if (oldTotal != newTotal) {
                "<p>Toplam sefer sayısı değişti: $oldTotal → $newTotal</p>"
            } else {
                "<p>Sefer detaylarında güncelleme yapıldı (saat dışı değişiklik).</p>"
            }
        }

        return sb.toString()
    }

    private fun resolveRouteName(
        name: String,
        yon: Int,
        guzergahBilgisi: com.berat.sakus.data.HatGuzergahBilgisi?
    ): String {
        if (guzergahBilgisi != null) {
            val baseRoute = guzergahBilgisi.yonler.firstOrNull()
            val loc = if (yon == 0) baseRoute?.startLocation else baseRoute?.endLocation
            if (!loc.isNullOrBlank()) {
                val endLoc = if (yon == 0) baseRoute?.endLocation else baseRoute?.startLocation
                return if (!endLoc.isNullOrBlank()) "$loc - $endLoc" else loc
            }
        }
        // Dönüş yönü ise isimleri ters çevir
        if (yon == 1 && name.contains("-")) {
            return name.split("-").map { it.trim() }.reversed().joinToString(" - ")
        }
        return name
    }
}
