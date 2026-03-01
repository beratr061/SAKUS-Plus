package com.berat.sakus.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berat.sakus.R
import com.berat.sakus.data.DurakBilgisi
import com.berat.sakus.data.DurakVarisi
import com.berat.sakus.ui.theme.ThemeManager
import com.berat.sakus.ui.theme.MapDarkBackground
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurakDetailScreen(
    durak: DurakBilgisi,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: DurakDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    val arrivals by viewModel.arrivals.collectAsState()
    val isLoadingArrivals by viewModel.isLoadingArrivals.collectAsState()

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
        viewModel.loadArrivals(durak)
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
                    Text(
                        text = durak.durakAdi,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Yaklaşan Otobüsler",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoadingArrivals) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (arrivals.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DirectionsBus,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Şu an yaklaşan otobüs bulunmuyor",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                fontSize = 15.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(arrivals, key = { "${it.plaka}-${it.hatNo}-${it.dakika}" }) { v ->
                            ArrivalCard(v)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArrivalCard(v: DurakVarisi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = v.hatNo,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = v.guzergahAdi.ifEmpty { v.hatAdi },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Plaka: ${v.plaka}${if (v.aracNumarasi > 0) " • Araç #${v.aracNumarasi}" else ""}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (v.dakika <= 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = v.dakikaMetin,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
