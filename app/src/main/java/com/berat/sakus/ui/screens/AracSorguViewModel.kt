package com.berat.sakus.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.berat.sakus.data.SbbApiServisi
import com.berat.sakus.data.models.AracKonumu
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

enum class SorguTipi {
    PLAKA, KAPI_NO
}

data class AracSorguState(
    val sorguMetni: String = "",
    val sorguTipi: SorguTipi = SorguTipi.PLAKA,
    val sonuclar: List<AracKonumu> = emptyList(),
    val yukleniyor: Boolean = false,
    val hata: String? = null,
    val canliTakip: Boolean = false
)

class AracSorguViewModel(application: Application) : AndroidViewModel(application) {
    private val api = SbbApiServisi.getInstance(application)
    
    private val _state = MutableStateFlow(AracSorguState())
    val state: StateFlow<AracSorguState> = _state.asStateFlow()
    
    private var streamJob: Job? = null

    fun sorguMetniGuncelle(metin: String) {
        _state.value = _state.value.copy(sorguMetni = metin, hata = null)
    }

    fun sorguTipiGuncelle(tip: SorguTipi) {
        _state.value = _state.value.copy(sorguTipi = tip, hata = null)
    }

    fun sorgula() {
        val metin = _state.value.sorguMetni.trim()
        if (metin.isEmpty()) {
            _state.value = _state.value.copy(hata = "Lütfen bir plaka veya kapı numarası girin")
            return
        }
        
        // Önceki stream'i durdur
        streamJob?.cancel()
        
        _state.value = _state.value.copy(
            yukleniyor = true,
            hata = null,
            sonuclar = emptyList(),
            canliTakip = true
        )
        
        streamJob = viewModelScope.launch {
            api.aracSorgula(metin)
                .catch { e ->
                    _state.value = _state.value.copy(
                        yukleniyor = false,
                        hata = "Bağlantı hatası: ${e.message}",
                        canliTakip = false
                    )
                }
                .collect { araclar ->
                    _state.value = _state.value.copy(
                        sonuclar = araclar,
                        yukleniyor = false
                    )
                }
        }
    }

    fun durdur() {
        streamJob?.cancel()
        streamJob = null
        _state.value = _state.value.copy(
            canliTakip = false,
            yukleniyor = false
        )
    }

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
    }
}
