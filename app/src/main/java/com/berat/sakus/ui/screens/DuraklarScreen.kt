package com.berat.sakus.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.berat.sakus.R
import com.berat.sakus.data.DurakBilgisi
import com.berat.sakus.data.DurakClusterItem
import com.berat.sakus.ui.theme.ThemeManager
import com.berat.sakus.ui.theme.MapDarkBackground
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import com.google.maps.android.compose.clustering.Clustering
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val SAKARYA_CENTER = LatLng(40.7750, 30.3950)
private const val DEFAULT_ZOOM = 12f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuraklarScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (DurakBilgisi) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: DuraklarViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    val duraklar by viewModel.duraklar.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasError by viewModel.hasError.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()

    val filteredDuraklar = remember(duraklar, searchQuery) {
        if (searchQuery.isBlank()) duraklar
        else duraklar.filter {
            it.durakAdi.contains(searchQuery, ignoreCase = true) ||
                    it.durakId.toString().contains(searchQuery)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(SAKARYA_CENTER, DEFAULT_ZOOM)
    }

    val isDarkTheme by ThemeManager.getInstance(context).isDarkTheme.collectAsState()
    var mapStyle by remember { mutableStateOf<MapStyleOptions?>(null) }
    var isMapReady by remember { mutableStateOf(false) }
    var locationGranted by remember { mutableStateOf(false) }
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        locationGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted) viewModel.fetchUserLocation(true)
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        kotlinx.coroutines.delay(300)
        isMapReady = true
        viewModel.loadDuraklar()
    }

    LaunchedEffect(isDarkTheme) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val fileName = if (isDarkTheme) "data/map_style_dark.json" else "data/map_style_light.json"
            try {
                val jsonString = context.assets.open(fileName).use { stream ->
                    stream.bufferedReader().use { it.readText() }
                }
                mapStyle = MapStyleOptions(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val filteredClusterItems = remember(filteredDuraklar) {
        filteredDuraklar.map { DurakClusterItem(it) }
    }

    var selectedDurak by remember { mutableStateOf<DurakBilgisi?>(null) }
    var requestCenterOnUser by remember { mutableStateOf(false) }

    LaunchedEffect(userLocation, requestCenterOnUser) {
        if (requestCenterOnUser && userLocation != null) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLocation!!, 15f))
            requestCenterOnUser = false
        }
    }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    LaunchedEffect(bottomSheetScaffoldState.bottomSheetState.targetValue) {
        if (bottomSheetScaffoldState.bottomSheetState.targetValue == SheetValue.PartiallyExpanded) {
            focusManager.clearFocus()
        }
    }

    BottomSheetScaffold(
        scaffoldState = bottomSheetScaffoldState,
        sheetPeekHeight = 180.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetDragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                )
            }
        },
        sheetContent = {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp - WindowInsets.statusBars.asPaddingValues().calculateTopPadding() - 82.dp),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    items = filteredDuraklar,
                    key = { "${it.durakId}_${it.lat}_${it.lng}" }
                ) { durak ->
                    Surface(
                        onClick = { onNavigateToDetail(durak) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        com.berat.sakus.ui.theme.PrimaryPurple.copy(alpha = 0.12f),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_bus_stop),
                                    contentDescription = "Durak",
                                    tint = com.berat.sakus.ui.theme.PrimaryPurple,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = durak.durakAdi.ifEmpty { "Durak ${durak.durakId}" },
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Durak No: ${durak.durakId}",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                            Icon(
                                painter = painterResource(R.drawable.baseline_arrow_forward_ios_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    ) {
    Box(modifier = Modifier.fillMaxSize().background(MapDarkBackground)) {
        if (isMapReady) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { 
                    selectedDurak = null
                    focusManager.clearFocus()
                },
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
                @OptIn(com.google.maps.android.compose.MapsComposeExperimentalApi::class)
                Clustering(
                    items = filteredClusterItems,
                    onClusterClick = { cluster ->
                        focusManager.clearFocus()
                        scope.launch(Dispatchers.Main) {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    cluster.position,
                                    cameraPositionState.position.zoom + 2f
                                )
                            )
                        }
                        true
                    },
                    onClusterItemClick = { item ->
                        selectedDurak = item.durak
                        focusManager.clearFocus()
                        scope.launch(Dispatchers.Main) {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(item.durak.lat, item.durak.lng),
                                    17f
                                )
                            )
                        }
                        true
                    },
                    clusterContent = { cluster ->
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = cluster.size.toString(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    },
                    clusterItemContent = { item ->
                        Icon(
                            painter = painterResource(R.drawable.ic_bus_stop_marker),
                            contentDescription = item.durak.durakAdi,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Unspecified
                        )
                    }
                )
            }
        }

        // Üst panel: Geri Dönüş ve Arama Barı
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Geri Butonu
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f), RoundedCornerShape(14.dp))
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Geri",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Arama Barı
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.3f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Ara",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Durak listesinde ara",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontSize = 15.sp
                            )
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        scope.launch { bottomSheetScaffoldState.bottomSheetState.expand() }
                                    }
                                }
                        )
                    }
                }
            }
        }

        // Mevcut konum butonu
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 90.dp,
                    end = 16.dp
                )
                .size(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .clickable {
                    if (userLocation != null) {
                        requestCenterOnUser = true
                    } else if (locationGranted) {
                        viewModel.fetchUserLocation(true)
                        requestCenterOnUser = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.MyLocation,
                contentDescription = "Konumuma git",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }

        // Loading overlay
        if (isLoading && duraklar.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = com.berat.sakus.ui.theme.PrimaryPurple)
            }
        }

        // Durak tooltip - ikonun üstünde küçük dikdörtgen (durak ikonuna basınca üstünde çıkar)
        selectedDurak?.let { durak ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-72).dp)
                        .padding(horizontal = 16.dp)
                        .clickable { selectedDurak = null },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = durak.durakAdi.ifEmpty { "Durak ${durak.durakId}" },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Detay",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable {
                                onNavigateToDetail(durak)
                                selectedDurak = null
                            }
                        )
                    }
                }
        }

        // Error overlay
        if (hasError && duraklar.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp
                )
            }
        }
    }
    }
}
