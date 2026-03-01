package com.berat.sakus.data.sync

import com.berat.sakus.data.HatSeferBilgisi

object DiffHelper {

    fun generateSeferDiffHtml(oldSefer: HatSeferBilgisi, newSefer: HatSeferBilgisi, guzergahBilgisi: com.berat.sakus.data.HatGuzergahBilgisi? = null): String {
        val buildStr = java.lang.StringBuilder()
        buildStr.append("<p>Aşağıdaki yönlerde ve saatlerde sefer değişiklikleri tespit edilmiştir:</p>")
        
        // Group by guzergah (0: Gidiş, 1: Dönüş or by routeName)
        val oldMap = oldSefer.seferler.associateBy { it.guzergahId }
        val newMap = newSefer.seferler.associateBy { it.guzergahId }

        val allRouteIds = (oldMap.keys + newMap.keys).toSet()

        for (routeId in allRouteIds) {
            val oldRoute = oldMap[routeId]
            val newRoute = newMap[routeId]

            val yon = newRoute?.yon ?: oldRoute?.yon ?: 0
            
            var routeName = newRoute?.guzergahAdi ?: oldRoute?.guzergahAdi ?: "Bilinmeyen Yön"
            if (guzergahBilgisi != null) {
                val baseRoute = guzergahBilgisi.yonler.firstOrNull()
                val loc = if (yon == 0) baseRoute?.startLocation else baseRoute?.endLocation
                if (!loc.isNullOrBlank()) {
                    val endLoc = if (yon == 0) baseRoute?.endLocation else baseRoute?.startLocation
                    routeName = if (!endLoc.isNullOrBlank()) {
                        "$loc - $endLoc"
                    } else {
                        loc
                    }
                } else if (yon == 1 && routeName.contains("-")) {
                    val parts = routeName.split("-").map { it.trim() }
                    routeName = parts.reversed().joinToString(" - ")
                }
            } else if (yon == 1 && routeName.contains("-")) {
                 val parts = routeName.split("-").map { it.trim() }
                 routeName = parts.reversed().joinToString(" - ")
            }
            
            val oldTimes = oldRoute?.detaylar?.map { it.baslangicSaat }?.toSet() ?: emptySet()
            val newTimes = newRoute?.detaylar?.map { it.baslangicSaat }?.toSet() ?: emptySet()

            val added = newTimes.subtract(oldTimes).sorted()
            val removed = oldTimes.subtract(newTimes).sorted()

            if (added.isNotEmpty() || removed.isNotEmpty()) {
                buildStr.append("<br/><b>Yön: $routeName</b><br/>")
                buildStr.append("<ul>")
                
                if (added.isNotEmpty()) {
                    buildStr.append("<li><b>Eklenen Saatler:</b> <font color='#4CAF50'>${added.joinToString(", ")}</font></li>")
                }
                if (removed.isNotEmpty()) {
                    buildStr.append("<li><b>Kaldırılan Saatler:</b> <font color='#F44336'><strike>${removed.joinToString(", ")}</strike></font></li>")
                }
                
                buildStr.append("</ul>")
            }
        }
        
        if (buildStr.length < 150) {
           return "<p>Güzergahta sefer saatleri güncellendi fakat detaylı değişiklik bulunamadı.</p>"
        }

        return buildStr.toString()
    }
}
