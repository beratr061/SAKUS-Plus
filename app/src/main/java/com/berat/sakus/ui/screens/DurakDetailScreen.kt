package com.berat.sakus.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berat.sakus.data.DurakBilgisi
import com.berat.sakus.data.models.StationEstimate
import com.berat.sakus.ui.theme.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*

// ── Solid renkler (gradyen yok) ──
private val CardBg = Color(0xFFF5F5F5)
private val CardBgDark = Color(0xFF2A2D34)
private val AccentGreen = Color(0xFF2E7D32)
private val AccentOrange = Color(0xFFE65100)
private val AccentBlue = Color(0xFF1565C0)
private val TimeBadgeBg = Color(0xFFE8F5E9)
private val TimeBadgeBgUrgent = Color(0xFFFFF3E0)
private val TimeBadgeBgDark = Color(0xFF1B3A1F)
private val TimeBadgeBgUrgentDark = Color(0xFF3A2A10)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurakDetailScreen(
    durak: DurakBilgisi,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: DurakDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    val estimates by viewModel.estimates.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasError by viewModel.hasError.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val isDarkTheme by ThemeManager.getInstance(context).isDarkTheme.collectAsState()
    var mapStyle by remember { mutableStateOf<MapStyleOptions?>(null) }
    var isMapReady by remember { mutableStateOf(false) }
    var locationGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        locationGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(durak) {
        viewModel.loadEstimates(durak)
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        kotlinx.coroutines.delay(200)
        isMapReady = true
    }

    LaunchedEffect(isDarkTheme) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val fileName = if (isDarkTheme) "data/map_style_dark.json" else "data/map_style_light.json"
            try {
                val jsonString = context.assets.open(fileName).use { stream ->
                    stream.bufferedReader().use { it.readText() }
                }
                mapStyle = MapStyleOptions(jsonString)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    val stopPosition = LatLng(durak.lat, durak.lng)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(stopPosition, 16f)
    }
    LaunchedEffect(isMapReady, stopPosition) {
        if (isMapReady) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(stopPosition, 16f))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = durak.durakAdi,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Durak No: ${durak.durakId}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh(durak.durakId) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Yenile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Harita ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MapDarkBackground)
            ) {
                if (isMapReady) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            mapStyleOptions = mapStyle,
                            isMyLocationEnabled = locationGranted
                        ),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            myLocationButtonEnabled = false,
                            mapToolbarEnabled = false
                        )
                    ) {
                        Marker(
                            state = MarkerState(position = stopPosition),
                            title = durak.durakAdi,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
                        )
                    }
                }
            }

            // ── İçerik ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Başlık satırı
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.DirectionsBus,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Yaklaşan Otobüsler",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (estimates.isNotEmpty()) {
                        Text(
                            text = "${estimates.size} hat",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Loading
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryPurple)
                    }
                }
                // Error
                else if (hasError) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF0F0)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = errorMessage.ifEmpty { "Veriler yüklenemedi" },
                                fontSize = 14.sp,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                }
                // Boş
                else if (estimates.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkTheme) CardBgDark else CardBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.DirectionsBus,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Şu an yaklaşan otobüs bulunmuyor",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
                // Liste
                else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            estimates,
                            key = { "${it.busLineCode}-${it.busPlate}-${it.remainingTimeCurr}" }
                        ) { estimate ->
                            EstimateCard(estimate, isDarkTheme)
                        }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EstimateCard(
    estimate: StationEstimate,
    isDarkTheme: Boolean
) {
    val isUrgent = estimate.remainingTimeCurr <= 5
    val timeColor = when {
        estimate.remainingTimeCurr <= 0 -> AccentGreen
        estimate.remainingTimeCurr <= 5 -> AccentOrange
        else -> AccentBlue
    }
    val timeBg = when {
        isDarkTheme && isUrgent -> TimeBadgeBgUrgentDark
        isDarkTheme -> TimeBadgeBgDark
        isUrgent -> TimeBadgeBgUrgent
        else -> TimeBadgeBg
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) CardBgDark else Color.White
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hat numarası badge
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryPurple),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = estimate.busLineCode,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (estimate.busLineCode.length > 3) 12.sp else 15.sp,
                    color = Color.White,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Bilgi
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = estimate.busLineLongName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Plaka
                    Text(
                        text = estimate.busPlate,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                    // Kalan durak sayısı
                    if (estimate.remainingNumberOfBusStops > 0) {
                        Text(
                            text = " • ",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        )
                        Text(
                            text = estimate.remainingStopsText,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Süre bilgisi
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mevcut süre
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = timeBg
                ) {
                    Text(
                        text = estimate.remainingTimeText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = timeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Sonraki süre
                estimate.nextTimeText?.let { nextTime ->
                    Text(
                        text = "sonra: $nextTime",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }
    }
}
