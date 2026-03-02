package com.berat.sakus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.berat.sakus.R
import com.berat.sakus.data.repository.TransportRepository
import com.berat.sakus.data.sync.SyncManager
import com.berat.sakus.ui.theme.PrimaryPurple
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTheme by com.berat.sakus.ui.theme.ThemeManager.getInstance(context).isDarkTheme.collectAsState()

    var statusMessage by remember { mutableStateOf("") }
    var isSyncing by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        val repository = TransportRepository.getInstance(context)
        val hasData = repository.hasHatlar()

        if (!hasData) {
            // İlk açılış — verileri API'den indir
            isSyncing = true
            statusMessage = "Veriler yükleniyor..."
            repository.initialSync()
            statusMessage = "Hazır!"
            isSyncing = false
            delay(500)
        } else {
            // Veri zaten var — kısa splash göster
            delay(600)
        }

        // Saatlik senkronizasyonu planla
        SyncManager.schedulePeriodicSync(context)
        // Haber bildirimi kontrolünü planla
        SyncManager.scheduleNewsCheck(context)
        // Hat değişiklikleri kontrolünü planla
        SyncManager.scheduleLineUpdateCheck(context)


        onNavigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_bus),
                contentDescription = "App Logo",
                tint = if (isDarkTheme) Color.White else PrimaryPurple,
                modifier = Modifier.size(190.dp)
            )

            if (isSyncing) {
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = statusMessage,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
