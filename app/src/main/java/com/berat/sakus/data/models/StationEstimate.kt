package com.berat.sakus.data.models

/**
 * Durak tahmini varış bilgisi modeli.
 * API: GET https://sbbpublicapi.sakarya.bel.tr/api/v1/estimates?stationId={id}&cityId=54
 */
data class StationEstimate(
    val busPlate: String,
    val busLineLongName: String,
    val busLineCode: String,
    val busLineNo: Int,
    val busLineShortName: String,
    val panelId: Int,
    val remainingTimeCurr: Int,        // mevcut tahmini dakika
    val remainingTimeNext: Int,        // sonraki tahmini dakika
    val isAccordingToTimeSchedule: String, // "A" = gerçek zamanlı
    val distance: Int,
    val remainingNumberOfBusStops: Int, // kalan durak sayısı
    val stationName: String,
    val busDate: String,
    val isDeleted: Boolean?
) {
    val remainingTimeText: String
        get() = when {
            remainingTimeCurr <= 0 -> "Durakta"
            remainingTimeCurr == 1 -> "1 dk"
            else -> "$remainingTimeCurr dk"
        }

    val nextTimeText: String?
        get() = if (remainingTimeNext > 0) "$remainingTimeNext dk" else null

    val remainingStopsText: String
        get() = "$remainingNumberOfBusStops durak"

    val isRealTime: Boolean
        get() = isAccordingToTimeSchedule == "A"
}
