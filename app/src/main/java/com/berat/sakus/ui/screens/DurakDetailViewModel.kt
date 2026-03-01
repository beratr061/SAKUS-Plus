package com.berat.sakus.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.berat.sakus.data.DurakBilgisi
import com.berat.sakus.data.DurakVarisi
import com.berat.sakus.data.repository.TransportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class DurakDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TransportRepository.getInstance(application)

    private val _arrivals = MutableStateFlow<List<DurakVarisi>>(emptyList())
    val arrivals: StateFlow<List<DurakVarisi>> = _arrivals.asStateFlow()

    private val _isLoadingArrivals = MutableStateFlow(true)
    val isLoadingArrivals: StateFlow<Boolean> = _isLoadingArrivals.asStateFlow()

    fun loadArrivals(durak: DurakBilgisi) {
        viewModelScope.launch {
            _isLoadingArrivals.value = true
            try {
                val list = withContext(Dispatchers.IO) {
                    repository.durakYaklasanAraclariGetir(durak)
                }
                _arrivals.value = list
            } catch (e: Exception) {
                _arrivals.value = emptyList()
            } finally {
                _isLoadingArrivals.value = false
            }
        }
    }
}
