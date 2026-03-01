package com.berat.sakus.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.berat.sakus.data.models.Itinerary
import com.berat.sakus.data.models.RouteLocation
import com.berat.sakus.data.repository.RouteRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * "Nasıl Giderim" ekranı için ViewModel.
 * Harita üzerinden seçilen başlangıç/bitiş noktalarını ve
 * API sonuçlarını yönetir.
 */

sealed class RouteUiState {
    data object Idle : RouteUiState()
    data object Loading : RouteUiState()
    data class Success(val itineraries: List<Itinerary>) : RouteUiState()
    data class Error(val message: String) : RouteUiState()
}

class RouteViewModel : ViewModel() {

    private val repository = RouteRepository()

    private val _startPoint = MutableStateFlow<LatLng?>(null)
    val startPoint: StateFlow<LatLng?> = _startPoint.asStateFlow()

    private val _endPoint = MutableStateFlow<LatLng?>(null)
    val endPoint: StateFlow<LatLng?> = _endPoint.asStateFlow()

    private val _uiState = MutableStateFlow<RouteUiState>(RouteUiState.Idle)
    val uiState: StateFlow<RouteUiState> = _uiState.asStateFlow()

    // Hangi nokta seçiliyor: true = başlangıç, false = bitiş
    private val _selectingStart = MutableStateFlow(true)
    val selectingStart: StateFlow<Boolean> = _selectingStart.asStateFlow()

    fun setStartPoint(latLng: LatLng) {
        _startPoint.value = latLng
        // Başlangıç seçildikten sonra otomatik olarak bitiş seçimine geç
        _selectingStart.value = false
    }

    fun setEndPoint(latLng: LatLng) {
        _endPoint.value = latLng
    }

    fun onMapClick(latLng: LatLng) {
        if (_selectingStart.value) {
            setStartPoint(latLng)
        } else {
            setEndPoint(latLng)
        }
    }

    fun toggleSelecting(isStart: Boolean) {
        _selectingStart.value = isStart
    }

    fun clearPoints() {
        _startPoint.value = null
        _endPoint.value = null
        _selectingStart.value = true
        _uiState.value = RouteUiState.Idle
    }

    fun findRoute() {
        val start = _startPoint.value
        val end = _endPoint.value

        if (start == null || end == null) {
            _uiState.value = RouteUiState.Error("Lütfen başlangıç ve bitiş noktalarını seçin")
            return
        }

        viewModelScope.launch {
            _uiState.value = RouteUiState.Loading

            val from = RouteLocation(start.latitude, start.longitude)
            val to = RouteLocation(end.latitude, end.longitude)

            repository.findRoute(from, to)
                .onSuccess { itineraries ->
                    if (itineraries.isEmpty()) {
                        _uiState.value = RouteUiState.Error("Bu güzergah için sonuç bulunamadı")
                    } else {
                        _uiState.value = RouteUiState.Success(itineraries)
                    }
                }
                .onFailure { error ->
                    _uiState.value = RouteUiState.Error(
                        error.message ?: "Bilinmeyen bir hata oluştu"
                    )
                }
        }
    }
}
