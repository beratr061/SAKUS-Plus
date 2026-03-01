package com.berat.sakus.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.berat.sakus.data.AracKonumu
import com.berat.sakus.data.HatBilgisi
import com.berat.sakus.data.HatGuzergahBilgisi
import com.berat.sakus.data.HatSeferBilgisi
import com.berat.sakus.data.TarifeBilgisi
import com.berat.sakus.data.Duyuru
import com.berat.sakus.data.SbbApiServisi
import com.berat.sakus.data.repository.TransportRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

class LineMapViewModel(application: Application) : AndroidViewModel(application) {
    private val sbbApiServisi = SbbApiServisi.getInstance(application)
    private val repository = TransportRepository.getInstance(application)

    private val _routeData = MutableStateFlow<HatGuzergahBilgisi?>(null)
    val routeData: StateFlow<HatGuzergahBilgisi?> = _routeData.asStateFlow()

    private val _vehicles = MutableStateFlow<List<AracKonumu>>(emptyList())
    val vehicles: StateFlow<List<AracKonumu>> = _vehicles.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> = _hasError.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()
    
    private val _hasSetInitialBounds = MutableStateFlow(false)
    val hasSetInitialBounds: StateFlow<Boolean> = _hasSetInitialBounds.asStateFlow()

    private val prefs = application.getSharedPreferences("SAKUS_PREFS", Context.MODE_PRIVATE)
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val locationClient = LocationServices.getFusedLocationProviderClient(application)
    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation.asStateFlow()
    private var locationJob: Job? = null

    // Dialog States
    private val _schedules = MutableStateFlow<List<HatSeferBilgisi?>>(emptyList())
    val schedules: StateFlow<List<HatSeferBilgisi?>> = _schedules.asStateFlow()
    private val _isScheduleLoading = MutableStateFlow(false)
    val isScheduleLoading: StateFlow<Boolean> = _isScheduleLoading.asStateFlow()

    private val _fares = MutableStateFlow<TarifeBilgisi?>(null)
    val fares: StateFlow<TarifeBilgisi?> = _fares.asStateFlow()
    private val _isFaresLoading = MutableStateFlow(false)
    val isFaresLoading: StateFlow<Boolean> = _isFaresLoading.asStateFlow()

    private val _announcements = MutableStateFlow<List<Duyuru>>(emptyList())
    val announcements: StateFlow<List<Duyuru>> = _announcements.asStateFlow()
    private val _isAnnouncementsLoading = MutableStateFlow(false)
    val isAnnouncementsLoading: StateFlow<Boolean> = _isAnnouncementsLoading.asStateFlow()

    private var trackingJob: Job? = null
    var currentHat: HatBilgisi? = null
    private var isLifecycleActive = true

    fun loadData(hat: HatBilgisi, forceRefresh: Boolean = false) {
        // Prevent reloading if already loaded for the same line
        if (!forceRefresh && currentHat == hat) return
        currentHat = hat
        
        _hasSetInitialBounds.value = false
        trackingJob?.cancel()
        trackingJob = null

        viewModelScope.launch {
            val favs = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                prefs.getStringSet("favorite_lines", emptySet()) ?: emptySet()
            }
            _isFavorite.value = favs.contains(hat.id.toString())

            _isLoading.value = true
            _hasError.value = false
            try {
                // Güzergah verisini Repository'den al (önce DB, yoksa API)
                _routeData.value = repository.getGuzergah(hat.id)
                _isLoading.value = false

                if (isLifecycleActive) {
                    startTracking(hat)
                }
            } catch (e: Exception) {
                _hasError.value = true
                _errorMessage.value = e.message ?: "Bilinmeyen hata"
                _isLoading.value = false
            }
        }
    }

    private fun startTracking(hat: HatBilgisi) {
        if (trackingJob != null) return
        trackingJob = viewModelScope.launch {
            sbbApiServisi.aracKonumlariniStreamle(hat.id).collect { vehicles ->
                _vehicles.value = vehicles
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        trackingJob?.cancel()
        locationJob?.cancel()
    }

    fun markInitialBoundsSet() {
        _hasSetInitialBounds.value = true
    }

    fun toggleFavorite(hatId: Int) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val favs = prefs.getStringSet("favorite_lines", emptySet())?.toMutableSet() ?: mutableSetOf()
            val idStr = hatId.toString()
            val newFavStatus = if (favs.contains(idStr)) {
                favs.remove(idStr)
                false
            } else {
                favs.add(idStr)
                true
            }
            prefs.edit { putStringSet("favorite_lines", favs) }
            _isFavorite.value = newFavStatus
        }
    }

    fun onLifecycleResume() {
        isLifecycleActive = true
        currentHat?.let { startTracking(it) }
        startLocationTracking()
    }

    fun onLifecyclePause() {
        isLifecycleActive = false
        trackingJob?.cancel()
        trackingJob = null
        locationJob?.cancel()
        locationJob = null
    }

    @SuppressLint("MissingPermission")
    private fun startLocationTracking() {
        if (locationJob != null) return
        
        val request = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 10000L
        ).setMinUpdateIntervalMillis(5000L).build()

        val callback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                result.lastLocation?.let { loc ->
                    _userLocation.value = LatLng(loc.latitude, loc.longitude)
                }
            }
        }

        locationJob = viewModelScope.launch {
            val handlerThread = android.os.HandlerThread("LocationUpdateThread").apply { start() }
            val handler = android.os.Handler(handlerThread.looper)
            try {
                locationClient.requestLocationUpdates(request, callback, handler.looper)
                while (isActive) { delay(10000L) }
            } catch (_: Exception) {
            } finally {
                locationClient.removeLocationUpdates(callback)
                handlerThread.quitSafely()
            }
        }
    }

    // Sefer saatlerini Repository'den al (önce DB, yoksa API)
    fun loadSchedules(hatId: Int) {
        if (_isScheduleLoading.value) return
        viewModelScope.launch {
            _isScheduleLoading.value = true
            try {
                _schedules.value = repository.getAllSeferSaatleri(hatId)
            } catch (_: Exception) {
                // Ignore API errors
            } finally {
                _isScheduleLoading.value = false
            }
        }
    }

    // Tarife bilgisini Repository'den al (önce DB, yoksa API)
    fun loadFares(hatId: Int, aracTipId: Int) {
        if (_isFaresLoading.value) return
        viewModelScope.launch {
            _isFaresLoading.value = true
            try {
                _fares.value = repository.getTarife(hatId, aracTipId)
            } catch (_: Exception) {
                // Ignore API errors
            } finally {
                _isFaresLoading.value = false
            }
        }
    }

    // Hat duyuruları — sadece bu hatta ait duyurular
    fun loadAnnouncements(hatId: Int) {
        if (_isAnnouncementsLoading.value) return
        viewModelScope.launch {
            _isAnnouncementsLoading.value = true
            try {
                _announcements.value = sbbApiServisi.hatDuyurulariGetir(hatId)
            } catch (_: Exception) {
                _announcements.value = emptyList()
            } finally {
                _isAnnouncementsLoading.value = false
            }
        }
    }
}
