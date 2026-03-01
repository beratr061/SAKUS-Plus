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

data class AracDetayState(
    val arac: AracKonumu? = null,
    val yukleniyor: Boolean = true,
    val hata: String? = null,
    val canliTakip: Boolean = false
)

class AracDetayViewModel(application: Application) : AndroidViewModel(application) {
    private val api = SbbApiServisi.getInstance(application)

    private val _state = MutableStateFlow(AracDetayState())
    val state: StateFlow<AracDetayState> = _state.asStateFlow()

    private var streamJob: Job? = null
    private var aracNumarasi: Int = 0

    fun baslat(busNumber: Int) {
        aracNumarasi = busNumber
        streamJob?.cancel()

        _state.value = AracDetayState(yukleniyor = true, canliTakip = true)

        streamJob = viewModelScope.launch {
            api.aracSorgula(busNumber.toString())
                .catch { e ->
                    _state.value = _state.value.copy(
                        yukleniyor = false,
                        hata = "Bağlantı hatası: ${e.message}",
                        canliTakip = false
                    )
                }
                .collect { araclar ->
                    val bulunan = araclar.firstOrNull { it.aracNumarasi == aracNumarasi }
                    _state.value = _state.value.copy(
                        arac = bulunan ?: _state.value.arac,
                        yukleniyor = false,
                        hata = if (bulunan == null && _state.value.arac == null) "Araç bulunamadı" else null
                    )
                }
        }
    }

    fun durdur() {
        streamJob?.cancel()
        streamJob = null
        _state.value = _state.value.copy(canliTakip = false)
    }

    override fun onCleared() {
        super.onCleared()
        streamJob?.cancel()
    }
}
