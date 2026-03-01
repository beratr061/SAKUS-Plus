package com.berat.sakus.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.berat.sakus.ui.theme.PrimaryPurple
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

/**
 * "Nasıl Giderim" ekranı.
 * Google Maps üzerinde başlangıç ve bitiş noktası seçimi yapılır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToGoScreen(
    onNavigateBack: () -> Unit,
    onNavigateToResults: () -> Unit,
    viewModel: RouteViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val startPoint by viewModel.startPoint.collectAsState()
    val endPoint by viewModel.endPoint.collectAsState()
    val selectingStart by viewModel.selectingStart.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Sakarya merkez koordinatları
    val sakaryaCenter = LatLng(40.6940, 30.4028)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(sakaryaCenter, 12f)
    }

    // Konum izni
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val latLng = LatLng(location.latitude, location.longitude)
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                            )
                        }
                    }
                }
            } catch (_: SecurityException) { }
        }
    }

    // Sonuç başarılı olunca otomatik olarak sonuç ekranına yönlendir
    LaunchedEffect(uiState) {
        if (uiState is RouteUiState.Success) {
            onNavigateToResults()
        }
    }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState) {
        if (uiState is RouteUiState.Error) {
            snackbarHostState.showSnackbar(
                message = (uiState as RouteUiState.Error).message,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Nasıl Giderim?",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Google Maps ──
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = hasLocationPermission
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false
                ),
                onMapClick = { latLng ->
                    viewModel.onMapClick(latLng)
                }
            ) {
                // Başlangıç marker
                startPoint?.let { point ->
                    Marker(
                        state = MarkerState(position = point),
                        title = "Başlangıç",
                        snippet = "Kalkış noktası",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_GREEN
                        )
                    )
                }

                // Bitiş marker
                endPoint?.let { point ->
                    Marker(
                        state = MarkerState(position = point),
                        title = "Bitiş",
                        snippet = "Varış noktası",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_RED
                        )
                    )
                }
            }

            // ── Üst Bilgi Kartı (Nokta seçim durumu) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.TopCenter)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Haritaya dokunarak nokta seçin",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Başlangıç noktası satırı
                        PointSelectionRow(
                            label = "Başlangıç",
                            point = startPoint,
                            isSelected = selectingStart,
                            dotColor = Color(0xFF4CAF50),
                            onClick = { viewModel.toggleSelecting(true) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Bitiş noktası satırı
                        PointSelectionRow(
                            label = "Bitiş",
                            point = endPoint,
                            isSelected = !selectingStart,
                            dotColor = Color(0xFFF44336),
                            onClick = { viewModel.toggleSelecting(false) }
                        )
                    }
                }
            }

            // ── Sağ alt butonlar ──
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 140.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Konumum butonu
                FloatingActionButton(
                    onClick = {
                        if (hasLocationPermission) {
                            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                            try {
                                fusedClient.lastLocation.addOnSuccessListener { location ->
                                    if (location != null) {
                                        scope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(location.latitude, location.longitude), 15f
                                                )
                                            )
                                        }
                                    }
                                }
                            } catch (_: SecurityException) { }
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = PrimaryPurple,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.MyLocation,
                        contentDescription = "Konumum",
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Temizle butonu
                AnimatedVisibility(
                    visible = startPoint != null || endPoint != null,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    FloatingActionButton(
                        onClick = { viewModel.clearPoints() },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = Color(0xFFF44336),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = "Temizle",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // ── Alt "Güzergah Bul" Butonu ──
            AnimatedVisibility(
                visible = startPoint != null && endPoint != null,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Button(
                    onClick = { viewModel.findRoute() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryPurple,
                        contentColor = Color.White
                    ),
                    enabled = uiState !is RouteUiState.Loading
                ) {
                    if (uiState is RouteUiState.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Aranıyor...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Icon(
                            Icons.Filled.Route,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Güzergah Bul",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PointSelectionRow(
    label: String,
    point: LatLng?,
    isSelected: Boolean,
    dotColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) PrimaryPurple.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) PrimaryPurple.copy(alpha = 0.3f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Renkli nokta
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(dotColor)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (point != null) {
                Text(
                    text = "${String.format("%.5f", point.latitude)}, ${String.format("%.5f", point.longitude)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = if (isSelected) "Haritaya dokunun..." else "Seçilmedi",
                    fontSize = 11.sp,
                    color = if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
            }
        }

        if (point != null) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = dotColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
