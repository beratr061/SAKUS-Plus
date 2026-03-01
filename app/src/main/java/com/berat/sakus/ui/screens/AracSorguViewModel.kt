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
    val tumAraclar: List<AracKonumu> = emptyList(),
    val filtrelenmis: List<AracKonumu> = emptyList(),
    val yukleniyor: Boolean = true,
    val hata: String? = null,
    val canliTakip: Boolean = false
)

class AracSorguViewModel(application: Application) : AndroidViewModel(application) {
    private val api = SbbApiServisi.getInstance(application)
    
    private val _state = MutableStateFlow(AracSorguState())
    val state: StateFlow<AracSorguState> = _state.asStateFlow()
    
    private var streamJob: Job? = null

    init {
        baslat()
    }

    private fun baslat() {
        streamJob?.cancel()
        _state.value = _state.value.copy(yukleniyor = true, hata = null, canliTakip = true)

        streamJob = viewModelScope.launch {
            api.tumAraclariStreamle()
                .catch { e ->
                    _state.value = _state.value.copy(
                        yukleniyor = false,
                        hata = "Bağlantı hatası: ${e.message}",
                        canliTakip = false
                    )
                }
                .collect { araclar ->
                    val sirali = araclar.sortedWith(compareBy({ it.plaka }, { it.aracNumarasi }))
                    _state.value = _state.value.copy(
                        tumAraclar = sirali,
                        filtrelenmis = filtrele(sirali, _state.value.sorguMetni, _state.value.sorguTipi),
                        yukleniyor = false
                    )
                }
        }
    }

    fun sorguMetniGuncelle(metin: String) {
        _state.value = _state.value.copy(
            sorguMetni = metin,
            hata = null,
            filtrelenmis = filtrele(_state.value.tumAraclar, metin, _state.value.sorguTipi)
        )
    }

    fun sorguTipiGuncelle(tip: SorguTipi) {
        _state.value = _state.value.copy(
            sorguTipi = tip,
            hata = null,
            filtrelenmis = filtrele(_state.value.tumAraclar, _state.value.sorguMetni, tip)
        )
    }

    private fun filtrele(araclar: List<AracKonumu>, metin: String, tip: SorguTipi): List<AracKonumu> {
        val sorgu = metin.trim().uppercase().replace("\\s+".toRegex(), " ")
        if (sorgu.isEmpty()) return araclar

        return araclar.filter { arac ->
            when (tip) {
                SorguTipi.PLAKA -> {
                    val plaka = arac.plaka.uppercase().replace("\\s+".toRegex(), " ")
                    plaka.contains(sorgu) || sorgu.contains(plaka.replace(" ", "")) && plaka.isNotEmpty()
                }
                SorguTipi.KAPI_NO -> {
                    arac.aracNumarasi.toString().contains(sorgu)
                }
            }
        }
    }

    fun yenidenBaglan() {
        baslat()
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
