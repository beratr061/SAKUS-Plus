package com.berat.sakus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berat.sakus.data.HatBilgisi
import com.berat.sakus.ui.screens.groupTimesByHour

// Figma design specs (430px width, from HTML export)
private val ColorMainBg = Color(0xFF303030)
private val ColorStatusBar = Color(0xFF1D1D1D)
private val ColorCardBg = Color(0xFF222222)
private val ColorRowBg = Color(0xFF2F2F2F)      // Satır arka planı
private val ColorPurpleAccent = Color(0xFF64217E)
private val ColorTimeChip = Color(0xFF212121)   // Saat chip arka planı

// Responsive layout - minimal side margins
private val HorizontalPadding = 12.dp
private val CardCornerRadius = 8.dp
private val PurpleButtonCornerRadius = 10.dp
private val TabCornerRadius = 5.dp
private val RowCornerRadius = 5.dp
private val TimeChipCornerRadius = 10.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HatSeferSaatleriScreen(
    hat: HatBilgisi,
    onNavigateBack: () -> Unit
) {
    val viewModel: HatSeferSaatleriViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val schedules by viewModel.schedules.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val guzergahBilgisi by viewModel.guzergah.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedDirection by remember { mutableIntStateOf(0) }

    LaunchedEffect(hat.id) {
        viewModel.loadSchedules(hat.id)
    }

    val currentData = schedules.getOrNull(selectedTabIndex)
    val seferlerSorted = remember(currentData) {
        val list = currentData?.seferler ?: emptyList()
        list.sortedBy { it.yon }
    }
    val routeForDirection = remember(seferlerSorted, selectedDirection) {
        if (seferlerSorted.isEmpty()) null else {
            seferlerSorted.getOrNull(selectedDirection.coerceIn(0, seferlerSorted.size - 1))
        }
    }

    val realStartLocation = remember(guzergahBilgisi, selectedDirection) {
        val baseRoute = guzergahBilgisi?.yonler?.firstOrNull()
        val loc = if (selectedDirection == 0) baseRoute?.startLocation else baseRoute?.endLocation
        
        loc?.takeIf { it.isNotBlank() } ?: run {
            val parts = hat.ad.split("-")
            val depPoint = if (selectedDirection == 0) parts.firstOrNull() else parts.lastOrNull()
            depPoint?.trim() ?: hat.ad
        }
    }

    val selectedStopName = remember(realStartLocation) {
        "$realStartLocation KALKIŞ"
    }

    val timesByHour = remember(seferlerSorted, selectedDirection) {
        val route = if (seferlerSorted.isEmpty()) null else {
            val oppositeDirection = if (selectedDirection == 0) 1 else 0
            seferlerSorted.getOrNull(oppositeDirection.coerceIn(0, seferlerSorted.size - 1))
        }
        route?.detaylar?.let { groupTimesByHour(it) } ?: emptyMap()
    }

    val hourList = remember(timesByHour) {
        timesByHour.keys.sorted()
    }

    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }

    Scaffold(
        containerColor = ColorMainBg,
        topBar = {
            Box(modifier = Modifier.fillMaxWidth().background(ColorStatusBar)) {
                Spacer(modifier = Modifier.fillMaxWidth().statusBarsPadding())
            }
        }
    ) { paddingValues ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(ColorMainBg)
    ) {
            // Header card - #222, rounded 8
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HorizontalPadding)
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(CardCornerRadius),
                color = ColorCardBg
            ) {
                Box(modifier = Modifier.height(56.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Geri",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${hat.hatNumarasi} (${if (selectedDirection == 0) "GİDİŞ" else "DÖNÜŞ"})",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = hat.ad,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            val count = seferlerSorted.size.coerceAtLeast(1)
                            selectedDirection = (selectedDirection + 1) % count
                        }) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Yön değiştir",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Purple stop button - rounded 10
            if (selectedStopName.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = HorizontalPadding)
                        .padding(top = 10.dp),
                        shape = RoundedCornerShape(PurpleButtonCornerRadius),
                        color = ColorPurpleAccent
                ) {
                    Text(
                        text = selectedStopName.uppercase(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Schedule card - #222, rounded 8
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = HorizontalPadding)
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(CardCornerRadius),
                color = ColorCardBg
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Day tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        listOf("Hafta İçi", "Cumartesi", "Pazar").forEachIndexed { index, label ->
                            val isSelected = selectedTabIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(TabCornerRadius))
                                    .clickable { selectedTabIndex = index }
                            ) {
                                if (isSelected) {
                                    Surface(
                                        modifier = Modifier.matchParentSize(),
                                        color = ColorPurpleAccent,
                                        shape = RoundedCornerShape(TabCornerRadius)
                                    ) {}
                                }
                                Text(
                                    text = label,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 1.dp),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    } else if (timesByHour.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Sefer bulunamadı", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    } else {
                        // Hour rows - 2. fotoğraftaki gibi: mor blok inset, pill şeklinde saatler
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(hourList) { hour ->
                                val times = timesByHour[hour] ?: emptyList()
                                // Satır: Stack yapısı - gri arka plan + sol mor blok (67x39)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(39.dp)
                                        .clip(RoundedCornerShape(RowCornerRadius))
                                ) {
                                    // 1. Gri arka plan
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(ColorRowBg, RoundedCornerShape(RowCornerRadius))
                                    )
                                    // 2. Mor blok - sol taraf, sadece sol köşeler yuvarlatılmış
                                    Box(
                                        modifier = Modifier
                                            .width(67.dp)
                                            .height(39.dp)
                                            .align(Alignment.CenterStart)
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = RowCornerRadius,
                                                    bottomStart = RowCornerRadius,
                                                    topEnd = 0.dp,
                                                    bottomEnd = 0.dp
                                                )
                                            )
                                            .background(ColorPurpleAccent)
                                    )
                                    // 3. İçerik: saat numarası + pill'ler
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.width(67.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = String.format(java.util.Locale.US, "%02d", hour),
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                                .horizontalScroll(rememberScrollState())
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                times.forEach { detay ->
                                                    val hasDescription = !detay.aciklama.isNullOrBlank()
                                                    Row(
                                                        modifier = Modifier
                                                            .height(24.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(ColorTimeChip)
                                                            .clickable(enabled = hasDescription) {
                                                                if (hasDescription) {
                                                                    dialogMessage = detay.aciklama!!
                                                                    showDialog = true
                                                                }
                                                            }
                                                            .padding(horizontal = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = detay.baslangicSaat,
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        if (hasDescription) {
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            androidx.compose.material3.Icon(
                                                                painter = androidx.compose.ui.res.painterResource(id = com.berat.sakus.R.drawable.ic_info),
                                                                contentDescription = "Bilgilendirme",
                                                                tint = Color.Unspecified, // XML rengini koru (ic_info : kırmızı yuvarlak ve beyaz çizgi var)
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = {
                        Text(text = "Bilgilendirme", fontWeight = FontWeight.Bold, color = Color.White)
                    },
                    text = {
                        Text(text = dialogMessage, color = Color.White.copy(alpha = 0.9f))
                    },
                    confirmButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Tamam", color = ColorPurpleAccent, fontWeight = FontWeight.Bold)
                        }
                    },
                    containerColor = ColorCardBg,
                    titleContentColor = Color.White,
                    textContentColor = Color.White
                )
            }
        }
    }
}
