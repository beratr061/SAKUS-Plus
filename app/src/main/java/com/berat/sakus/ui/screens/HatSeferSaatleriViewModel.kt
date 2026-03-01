package com.berat.sakus.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.berat.sakus.data.HatBilgisi
import com.berat.sakus.data.HatSeferBilgisi
import com.berat.sakus.data.SeferDetay
import com.berat.sakus.data.repository.TransportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HatSeferSaatleriViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TransportRepository.getInstance(application)

    private val _schedules = MutableStateFlow<List<HatSeferBilgisi?>>(emptyList())
    val schedules: StateFlow<List<HatSeferBilgisi?>> = _schedules.asStateFlow()

    private val _guzergah = MutableStateFlow<com.berat.sakus.data.HatGuzergahBilgisi?>(null)
    val guzergah: StateFlow<com.berat.sakus.data.HatGuzergahBilgisi?> = _guzergah.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadSchedules(hatId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Paralel çekim
                val (seferler, guz) = withContext(Dispatchers.IO) {
                    val s = repository.getAllSeferSaatleri(hatId)
                    val g = repository.syncGuzergah(hatId)
                    Pair(s, g)
                }
                _schedules.value = seferler
                _guzergah.value = guz
            } catch (_: Exception) {
                _schedules.value = emptyList()
                _guzergah.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
}

/** Saatleri saate göre grupla ve kronolojik sırala */
fun groupTimesByHour(detaylar: List<SeferDetay>): Map<Int, List<SeferDetay>> {
    return detaylar
        .filter { it.baslangicSaat.isNotBlank() }
        .groupBy { detay ->
            val parts = detay.baslangicSaat.split(":")
            parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
        }
        .mapValues { (_, list) -> list.sortedBy { it.baslangicSaat } }
}
