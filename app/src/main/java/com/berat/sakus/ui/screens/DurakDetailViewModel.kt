package com.berat.sakus.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.berat.sakus.data.DurakBilgisi
import com.berat.sakus.data.models.StationEstimate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class DurakDetailViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "DurakDetailVM"
        private const val BASE_URL = "https://sbbpublicapi.sakarya.bel.tr/api/v1/estimates"
        private const val CITY_ID = 54
        private const val REFRESH_INTERVAL_MS = 30_000L // 30 saniyede bir güncelle
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private val _estimates = MutableStateFlow<List<StationEstimate>>(emptyList())
    val estimates: StateFlow<List<StationEstimate>> = _estimates.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasError = MutableStateFlow(false)
    val hasError: StateFlow<Boolean> = _hasError.asStateFlow()

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.asStateFlow()

    fun loadEstimates(durak: DurakBilgisi) {
        viewModelScope.launch {
            // İlk yükleme
            fetchEstimates(durak.durakId)

            // Otomatik yenileme
            while (isActive) {
                delay(REFRESH_INTERVAL_MS)
                fetchEstimates(durak.durakId, isRefresh = true)
            }
        }
    }

    fun refresh(stationId: Int) {
        viewModelScope.launch {
            fetchEstimates(stationId)
        }
    }

    private suspend fun fetchEstimates(stationId: Int, isRefresh: Boolean = false) {
        if (!isRefresh) _isLoading.value = true
        _hasError.value = false

        try {
            val list = withContext(Dispatchers.IO) {
                val url = "$BASE_URL?stationId=$stationId&cityId=$CITY_ID"
                Log.d(TAG, "İstek: $url")

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Origin", "https://ulasim.sakarya.bel.tr")
                    .header("Referer", "https://ulasim.sakarya.bel.tr/")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw Exception("Sunucu hatası: ${response.code}")
                }

                val bodyString = response.body?.string()
                if (bodyString.isNullOrEmpty()) {
                    throw Exception("Boş yanıt")
                }

                Log.d(TAG, "Yanıt: ${bodyString.take(300)}")

                val type = object : TypeToken<List<StationEstimate>>() {}.type
                val result: List<StationEstimate> = gson.fromJson(bodyString, type)
                // remainingTimeCurr'a göre sırala
                result.sortedBy { it.remainingTimeCurr }
            }

            _estimates.value = list
            Log.d(TAG, "${list.size} tahmin bulundu")

        } catch (e: Exception) {
            Log.e(TAG, "Hata: ${e.message}", e)
            if (!isRefresh) {
                _hasError.value = true
                _errorMessage.value = e.localizedMessage ?: "Veriler yüklenemedi"
            }
        } finally {
            _isLoading.value = false
        }
    }
}
