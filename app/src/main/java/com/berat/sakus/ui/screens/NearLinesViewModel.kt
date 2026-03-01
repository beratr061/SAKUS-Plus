package com.berat.sakus.ui.screens

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.berat.sakus.data.models.NearLine
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class NearLinesUiState(
    val isLoading: Boolean = false,
    val nearLines: List<NearLine> = emptyList(),
    val error: String? = null,
    val userLocation: LatLng? = null,
    val radiusMeters: Int = 250,
    val hasSearched: Boolean = false
)

class NearLinesViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(NearLinesUiState())
    val uiState: StateFlow<NearLinesUiState> = _uiState

    private val locationClient = LocationServices.getFusedLocationProviderClient(application)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    fun updateRadius(radius: Int) {
        _uiState.value = _uiState.value.copy(radiusMeters = radius)
    }

    @SuppressLint("MissingPermission")
    fun fetchUserLocation() {
        locationClient.lastLocation
            .addOnSuccessListener { location ->
                location?.let {
                    _uiState.value = _uiState.value.copy(
                        userLocation = LatLng(it.latitude, it.longitude)
                    )
                }
            }
    }

    fun searchNearLines() {
        val loc = _uiState.value.userLocation ?: return
        val radius = _uiState.value.radiusMeters

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = "https://sbbpublicapi.sakarya.bel.tr/api/v1/Ulasim/nearlines" +
                        "?latitude=${loc.latitude}&longitude=${loc.longitude}" +
                        "&radiusMeters=$radius&busType=54"

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("Origin", "https://ulasim.sakarya.bel.tr")
                    .header("Referer", "https://ulasim.sakarya.bel.tr/")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                if (response.isSuccessful && body.isNotEmpty()) {
                    val jsonElement = JsonParser.parseString(body)
                    val valueArray = when {
                        jsonElement.isJsonArray -> jsonElement.asJsonArray
                        jsonElement.isJsonObject -> jsonElement.asJsonObject.getAsJsonArray("value")
                        else -> null
                    }
                    val lines = mutableListOf<NearLine>()

                    for (element in valueArray ?: com.google.gson.JsonArray()) {
                        val obj = element.asJsonObject
                        val routesArray = obj.getAsJsonArray("routes")
                        val routes = mutableListOf<com.berat.sakus.data.models.NearLineRoute>()

                        if (routesArray != null) {
                            for (routeEl in routesArray) {
                                val r = routeEl.asJsonObject
                                routes.add(
                                    com.berat.sakus.data.models.NearLineRoute(
                                        routeId = r.get("routeId")?.asInt ?: 0,
                                        routeName = r.get("routeName")?.asString ?: "",
                                        startLocation = r.get("startLocation")?.asString ?: "",
                                        endLocation = r.get("endLocation")?.asString ?: "",
                                        routeTypeId = r.get("routeTypeId")?.asInt ?: 0,
                                        distanceMeters = r.get("distanceMeters")?.asDouble ?: 0.0
                                    )
                                )
                            }
                        }

                        lines.add(
                            NearLine(
                                lineId = obj.get("lineId")?.asInt ?: 0,
                                typeValueId = obj.get("typeValueId")?.asInt ?: 0,
                                typeValueName = obj.get("typeValueName")?.asString ?: "",
                                typeValueColor = obj.get("typeValueColor")?.asString ?: "#68bd9c",
                                lineName = obj.get("lineName")?.asString ?: "",
                                lineNumber = obj.get("lineNumber")?.asString ?: "",
                                ekentLineIntegrationId = if (obj.has("ekentLineIntegrationId") && !obj.get("ekentLineIntegrationId").isJsonNull) obj.get("ekentLineIntegrationId").asInt else null,
                                nearestDistanceMeters = obj.get("nearestDistanceMeters")?.asDouble ?: 0.0,
                                routes = routes
                            )
                        )
                    }

                    // Mesafeye göre sırala
                    val sorted = lines.sortedBy { it.nearestDistanceMeters }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        nearLines = sorted,
                        hasSearched = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Veri alınamadı (${response.code})",
                        hasSearched = true
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Bağlantı hatası: ${e.localizedMessage}",
                    hasSearched = true
                )
            }
        }
    }
}
