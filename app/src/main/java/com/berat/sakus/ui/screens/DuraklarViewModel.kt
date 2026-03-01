package com.berat.sakus.ui.screens

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.berat.sakus.data.DurakBilgisi
import com.berat.sakus.data.repository.TransportRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlinx.coroutines.withContext

class DuraklarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TransportRepository.getInstance(application)
    private val locationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _duraklar = MutableStateFlow<List<DurakBilgisi>>(emptyList())
    val duraklar: StateFlow<List<DurakBilgisi>> = _duraklar.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> = _hasError.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadDuraklar() {
        viewModelScope.launch {
            _isLoading.value = true
            _hasError.value = false
            try {
                _duraklar.value = withContext(Dispatchers.IO) {
                    repository.tumDuraklariGetir()
                }
            } catch (e: Exception) {
                _hasError.value = true
                _errorMessage.value = e.message ?: "Duraklar yüklenemedi"
            } finally {
                _isLoading.value = false
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchUserLocation(hasPermission: Boolean) {
        if (!hasPermission) return
        viewModelScope.launch {
            try {
                val loc = kotlin.coroutines.suspendCoroutine<android.location.Location?> { cont ->
                    locationClient.lastLocation
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resume(null) }
                }
                loc?.let { _userLocation.value = LatLng(it.latitude, it.longitude) }
            } catch (_: Exception) {}
        }
    }
}
