package com.berat.sakus.ui.screens

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.core.graphics.createBitmap
import com.berat.sakus.R
import com.berat.sakus.data.*
import com.berat.sakus.ui.theme.ThemeManager
import com.berat.sakus.ui.theme.MapDarkBackground
import com.berat.sakus.ui.theme.MapDarkCard
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineMapScreen(
    hat: HatBilgisi,
    onNavigateBack: () -> Unit,
    onNavigateDuyuruDetail: (Duyuru) -> Unit = {},
    onNavigateSeferSaatleri: (HatBilgisi) -> Unit = {}
) {
    val localCtx = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: LineMapViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    
    val routeData by viewModel.routeData.collectAsState()
    val vehicles by viewModel.vehicles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasError by viewModel.hasError.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    var selectedDirection by remember { mutableIntStateOf(0) }
    val userLocation by viewModel.userLocation.collectAsState()
    var currentMapType by remember { mutableStateOf(MapType.NORMAL) }
    
    val isFavorite by viewModel.isFavorite.collectAsState()
    val hasSetInitialBounds by viewModel.hasSetInitialBounds.collectAsState()
    var trackedVehicleId by remember { mutableStateOf<String?>(null) }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(40.7750, 30.3950), 12f)
    }
    
    val isDarkTheme by ThemeManager.getInstance(localCtx).isDarkTheme.collectAsState()
    var mapStyle by remember { mutableStateOf<MapStyleOptions?>(null) }
    
    // Dialog states
    val (showScheduleDialog, setShowScheduleDialog) = remember { mutableStateOf(false) }
    val (showFaresDialog, setShowFaresDialog) = remember { mutableStateOf(false) }
    val (showAnnouncementsDialog, setShowAnnouncementsDialog) = remember { mutableStateOf(false) }
    
    // Background location logic
    var locationGranted by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        locationGranted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    
    var busBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var stopBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var arrowBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var isMapReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        kotlinx.coroutines.delay(400) // Delay to let navigation animation complete
        isMapReady = true
    }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                busBitmap = MapAssets.getBitmapFromVector(localCtx, R.drawable.ic_bus_location_marker, 95)
                stopBitmap = MapAssets.getBitmapFromVector(localCtx, R.drawable.ic_bus_stop_marker, 130)
                arrowBitmap = MapAssets.getDirectionalArrowBitmap()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Load map styles dynamically
    LaunchedEffect(isDarkTheme) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val fileName = if (isDarkTheme) "data/map_style_dark.json" else "data/map_style_light.json"
            try {
                val jsonString = localCtx.assets.open(fileName).use { stream ->
                    stream.bufferedReader().use { it.readText() }
                }
                mapStyle = MapStyleOptions(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (locationGranted) {
            viewModel.onLifecycleResume()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        viewModel.onLifecyclePause()
    }
    
    LaunchedEffect(hat) {
        viewModel.loadData(hat)
    }
    
    LaunchedEffect(routeData, hasSetInitialBounds, isMapReady) {
        val currentRouteData = routeData
        if (currentRouteData != null && currentRouteData.guzergahKoordinatlari.isNotEmpty() && !hasSetInitialBounds && isMapReady) {
            var minLat = Double.POSITIVE_INFINITY
            var maxLat = Double.NEGATIVE_INFINITY
            var minLng = Double.POSITIVE_INFINITY
            var maxLng = Double.NEGATIVE_INFINITY
            currentRouteData.guzergahKoordinatlari.forEach { coord ->
                if (coord.size >= 2) {
                    if (coord[0] < minLat) minLat = coord[0]
                    if (coord[0] > maxLat) maxLat = coord[0]
                    if (coord[1] < minLng) minLng = coord[1]
                    if (coord[1] > maxLng) maxLng = coord[1]
                }
            }
            if (minLat != Double.POSITIVE_INFINITY) {
                // To avoid CameraUpdateFactory not initialized crash, delay slightly after map is ready
                kotlinx.coroutines.delay(200)
                try {
                    val bounds = LatLngBounds(LatLng(minLat, minLng), LatLng(maxLat, maxLng))
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 60))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                viewModel.markInitialBoundsSet()
            }
        }
    }
    
    val trackedVehiclePosition = remember(trackedVehicleId, vehicles) {
        trackedVehicleId?.let { id -> vehicles.find { it.plaka == id }?.let { LatLng(it.lat, it.lng) } }
    }
    LaunchedEffect(trackedVehiclePosition) {
        trackedVehiclePosition?.let { cameraPositionState.animate(CameraUpdateFactory.newLatLng(it)) }
    }
    
    val activeVehicles by remember {
        derivedStateOf {
            val isRing = routeData?.let { it.guzergahlar.size <= 1 || it.yonler.size == 1 } ?: false
            vehicles.filter { it.aktifMi && (isRing || it.yon == selectedDirection) }
        }
    }
    val isRingLine = routeData?.let { it.guzergahlar.size <= 1 || it.yonler.size == 1 } ?: false
    val stopsWithVehicles by produceState(initialValue = emptySet(), activeVehicles, routeData) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val s = mutableSetOf<Int>()
            val currentRouteData = routeData
            if (currentRouteData != null) {
                // Approximate degree difference for 200 meters: ~0.0018 for Lat, ~0.0025 for Lng (in Turkey latitudes)
                val maxLatDiff = 0.0020
                val maxLngDiff = 0.0025
                for (v in activeVehicles) {
                    for (stop in currentRouteData.duraklar) {
                        if (Math.abs(v.lat - stop.lat) < maxLatDiff && Math.abs(v.lng - stop.lng) < maxLngDiff) {
                            if (distanceBetween(v.lat, v.lng, stop.lat, stop.lng) < 200) {
                                s.add(stop.durakId)
                            }
                        }
                    }
                }
            }
            value = s
        }
    }
    
    val bottomSheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    val filteredStops by remember {
        derivedStateOf {
            routeData?.duraklar?.filter { it.yon == selectedDirection } ?: emptyList()
        }
    }
    
    // Polylines - arka planda koordinat dönüşümü (main thread kasmayı önler)
    val dir0Coords by produceState(initialValue = emptyList<LatLng>(), routeData, selectedDirection) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            if (selectedDirection == 0) {
                val list = routeData?.guzergahlar
                if (list != null && list.isNotEmpty()) {
                    list[0]?.mapNotNull { if (it.size >= 2) LatLng(it[0], it[1]) else null } ?: emptyList()
                } else emptyList()
            } else emptyList()
        }
    }
    val dir1Coords by produceState(initialValue = emptyList<LatLng>(), routeData, selectedDirection) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            if (selectedDirection == 1) {
                val list = routeData?.guzergahlar
                if (list != null && list.size > 1) {
                    list[1]?.mapNotNull { if (it.size >= 2) LatLng(it[0], it[1]) else null } ?: emptyList()
                } else emptyList()
            } else emptyList()
        }
    }
    
    BottomSheetScaffold(
        scaffoldState = bottomSheetState,
        sheetPeekHeight = 140.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetContainerColor = MapDarkBackground,
        sheetDragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.Gray, RoundedCornerShape(2.dp))
                )
            }
        },
        sheetContent = {
            Column(modifier = Modifier.fillMaxHeight()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                Text(
                    text = hat.hatNumarasi,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = hat.ad,
                    color = Color.White.copy(alpha=0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha=0.12f))
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (hasError) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(errorMessage, color = Color.White.copy(alpha=0.7f), textAlign = TextAlign.Center)
                }
            } else if (filteredStops.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("Güzergah bulunamadı", color = Color.White)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(
                        items = filteredStops,
                        key = { _, stop -> stop.durakId }
                    ) { index, stop ->
                        val nearbyVehicle = stopsWithVehicles.contains(stop.durakId)
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                scope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(stop.lat, stop.lng), 16f))
                                }
                            },
                            headlineContent = {
                                Text(
                                    text = stop.durakAdi,
                                    color = if (nearbyVehicle) Color.Green else Color.White,
                                    fontWeight = if (nearbyVehicle) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = if (nearbyVehicle) "🚌 Araç durakta/yakında" else "Durak No: ${stop.durakId}",
                                    color = if (nearbyVehicle) Color(0xFFC8E6C9) else Color.White.copy(alpha=0.38f),
                                    fontSize = 12.sp
                                )
                            },
                            leadingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_bus_stop),
                                        contentDescription = "Durak İkonu",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(30.dp)
                                            .background(if (nearbyVehicle) Color.Green.copy(alpha=0.15f) else Color.Transparent, CircleShape)
                                            .border(if (nearbyVehicle) 2.dp else 1.dp, if (nearbyVehicle) Color.Green else Color.Red, CircleShape)
                                    ) {
                                        if (nearbyVehicle) {
                                            Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                                        } else {
                                            Text("${index + 1}", color = Color.Red, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    },
    content = { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(MapDarkBackground)) {
            if (isMapReady) {
                GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapStyleOptions = mapStyle,
                    mapType = currentMapType,
                    isMyLocationEnabled = locationGranted
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    mapToolbarEnabled = false
                ),
                onMapClick = {
                    trackedVehicleId = null
                }
            ) {
                val busIcon = remember(busBitmap) { busBitmap?.let { BitmapDescriptorFactory.fromBitmap(it) } }
                val stopIcon = remember(stopBitmap) { stopBitmap?.let { BitmapDescriptorFactory.fromBitmap(it) } }
                
                val arrowBitmapDescriptor = remember(arrowBitmap) { arrowBitmap?.let { BitmapDescriptorFactory.fromBitmap(it) } }
                
                val spansList = remember(arrowBitmapDescriptor) {
                    if (arrowBitmapDescriptor != null) {
                        listOf(
                            StyleSpan(
                                StrokeStyle.colorBuilder(android.graphics.Color.BLACK)
                                    .stamp(TextureStyle.newBuilder(arrowBitmapDescriptor).build())
                                    .build()
                            )
                        )
                    } else {
                        listOf(StyleSpan(android.graphics.Color.BLACK))
                    }
                }
                
                if (selectedDirection == 0 && dir0Coords.isNotEmpty()) {
                    key(arrowBitmapDescriptor) {
                        Polyline(
                            points = dir0Coords, 
                            width = 24f, 
                            spans = spansList, 
                            zIndex = 0.5f
                        )
                    }
                }
                if (selectedDirection == 1 && dir1Coords.isNotEmpty()) {
                    key(arrowBitmapDescriptor) {
                        Polyline(
                            points = dir1Coords, 
                            width = 24f, 
                            spans = spansList, 
                            zIndex = 0.5f
                        )
                    }
                }
                
                // Static Stop Markers
                filteredStops.forEach { stop ->
                    Marker(
                        state = MarkerState(position = LatLng(stop.lat, stop.lng)),
                        title = stop.durakAdi,
                        snippet = "Sıra: ${stop.siraNo}",
                        icon = stopIcon ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                        zIndex = 0f
                    )
                }
                
                // Vehicle Markers - Recomposition kullanmadan yüksek performanslı 60fps animasyon
                activeVehicles.forEach { v ->
                    key(v.plaka) {
                        val targetLatLng = LatLng(v.lat, v.lng)
                        val markerState = remember { MarkerState(position = targetLatLng) }
                        var previousLatLng by remember { mutableStateOf(targetLatLng) }

                        // Sadece pozisyon hedefini dinle, UI recomposition'ı atlayıp state'e gömülü yaz!
                        LaunchedEffect(targetLatLng) {
                            if (targetLatLng.latitude != previousLatLng.latitude || targetLatLng.longitude != previousLatLng.longitude) {
                                val fromLat = previousLatLng.latitude
                                val fromLng = previousLatLng.longitude
                                val anim = androidx.compose.animation.core.Animatable(0f)
                                
                                anim.animateTo(
                                    targetValue = 1f,
                                    animationSpec = androidx.compose.animation.core.tween(
                                        durationMillis = 900,
                                        easing = androidx.compose.animation.core.LinearEasing
                                    )
                                ) {
                                    val lat = fromLat + (targetLatLng.latitude - fromLat) * value
                                    val lng = fromLng + (targetLatLng.longitude - fromLng) * value
                                    // MarkerState'e doğrudan yazmak Maps SDK'yı tetikler, Compose Recomposition yapmaz.
                                    markerState.position = LatLng(lat, lng)
                                }
                                previousLatLng = targetLatLng
                            }
                        }

                        LaunchedEffect(v) {
                            if (trackedVehicleId == v.plaka) {
                                markerState.showInfoWindow()
                            }
                        }

                        MarkerInfoWindow(
                            state = markerState,
                            icon = busIcon ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                            rotation = v.baslik.toFloat(),
                            anchor = Offset(0.5f, 0.5f),
                            infoWindowAnchor = Offset(0.5f, 0.5f),
                            zIndex = 1f,
                            onClick = {
                                trackedVehicleId = v.plaka
                                scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(markerState.position, 16f)) }
                                false
                            }
                        ) {
                            VehicleTooltip(v)
                        }
                    }
                }
            }
            } else {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.White)
            }
            
            // Top Bar Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 10.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MapDarkBackground
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                if (vehicles.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .background(MapDarkBackground, RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = Color.Green, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${activeVehicles.size} / ${vehicles.size}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                if (!isRingLine) {
                    Surface(
                        onClick = { selectedDirection = if (selectedDirection == 0) 1 else 0 },
                        modifier = Modifier
                            .width(113.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MapDarkBackground
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_swap_direction),
                                contentDescription = "Yön Değiştir",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedDirection == 1) "Dönüş" else "Gidiş",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            
            // Right Side Buttons Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 70.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp) // 6px spacing
            ) {
                // 1. Mevcut Konum
                MapButton(painter = painterResource(id = R.drawable.ic_location_arrow), onTap = {
                    if (userLocation != null) {
                        scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLocation!!, 15f)) }
                    }
                })
                // 2. Harita Türü
                MapButton(painter = painterResource(id = R.drawable.ic_map), onTap = {
                    currentMapType = when (currentMapType) {
                        MapType.NORMAL -> MapType.SATELLITE
                        MapType.SATELLITE -> MapType.HYBRID
                        else -> MapType.NORMAL
                    }
                })
                // 3. Sefer Saatleri
                MapButton(painter = painterResource(id = R.drawable.ic_schedule), onTap = { onNavigateSeferSaatleri(hat) })
                // 4. Fiyat Tarifesi
                MapButton(painter = painterResource(id = R.drawable.ic_turkish_lira), onTap = { setShowFaresDialog(true) })
                // 5. Favoriye Ekle
                MapButton(
                    painter = painterResource(id = R.drawable.ic_star_empty),
                    iconTint = if (isFavorite) Color.Yellow else Color.White,
                    onTap = {
                        viewModel.toggleFavorite(hat.id)
                    }
                )
                // 6. Hat Duyuruları
                MapButton(painter = painterResource(id = R.drawable.ic_announcement), onTap = {
                    viewModel.loadAnnouncements(hat.id)
                    setShowAnnouncementsDialog(true)
                })
            }

            // Vehicles Horizontal List Overlay
            if (activeVehicles.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { activeVehicles.size })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = paddingValues.calculateBottomPadding() + 16.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    pageSpacing = 12.dp
                ) { page ->
                    val vehicle = activeVehicles[page]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(MapDarkBackground, RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_bus),
                            contentDescription = null,
                            tint = Color(0xFF4A90E2),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${vehicle.plaka} - ${if (vehicle.aracNumarasi > 0) vehicle.aracNumarasi.toString() else "Yok"}",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            onClick = {
                                trackedVehicleId = vehicle.plaka
                                scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(vehicle.lat, vehicle.lng), 16f)) }
                            },
                            modifier = Modifier
                                .border(1.dp, Color.White.copy(alpha=0.3f), RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Transparent
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text("Haritada göster", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
)

    if (showScheduleDialog) ScheduleDialog(hat, viewModel) { setShowScheduleDialog(false) }
    if (showFaresDialog) FaresDialog(hat, viewModel) { setShowFaresDialog(false) }
    if (showAnnouncementsDialog) LineDuyurularFullScreen(
        hat = hat,
        viewModel = viewModel,
        onDismiss = { setShowAnnouncementsDialog(false) },
        onDuyuruClick = { duyuru ->
            setShowAnnouncementsDialog(false)
            onNavigateDuyuruDetail(duyuru)
        }
    )
}
