package com.berat.sakus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berat.sakus.data.*
import com.berat.sakus.ui.theme.MapDarkBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDialog(hat: HatBilgisi, viewModel: LineMapViewModel, onDismiss: () -> Unit) {
    val isLoading by viewModel.isScheduleLoading.collectAsState()
    val schedules by viewModel.schedules.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadSchedules(hat.id)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MapDarkBackground,
        modifier = Modifier.fillMaxHeight(0.8f)
    ) {
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            var selectedTabIndex by remember { mutableIntStateOf(0) }
            Column {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color.Blue
                        )
                    }
                ) {
                    Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("Hafta İçi") })
                    Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("Cumartesi") })
                    Tab(selected = selectedTabIndex == 2, onClick = { selectedTabIndex = 2 }, text = { Text("Pazar") })
                }
                
                val currentData = schedules.getOrNull(selectedTabIndex)
                val seferlerSorted = remember(currentData) {
                    (currentData?.seferler ?: emptyList()).sortedBy { it.yon }
                }
                if (currentData == null || seferlerSorted.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Sefer bulunamadı", color = Color.White) }
                } else {
                    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                        itemsIndexed(
                            items = seferlerSorted,
                            key = { index, route -> "${route.guzergahAdi}_$index" }
                        ) { index, route ->
                            Column {
                                val yonLabel = if (route.yon == 0) "Gidiş" else "Dönüş"
                                Text("${route.guzergahAdi} ($yonLabel)", color = Color(0xFF448AFF), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    route.detaylar.chunked(4).forEach { chunk ->
                                        Column {
                                            chunk.forEach { detay ->
                                                Box(
                                                    modifier = Modifier
                                                        .padding(bottom=4.dp)
                                                        .background(Color.White.copy(alpha=0.1f), RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Text(detay.baslangicSaat, color = Color.White, fontSize = 14.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                                if (index < seferlerSorted.size - 1) HorizontalDivider(color = Color.White.copy(alpha=0.24f), modifier = Modifier.padding(vertical = 12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaresDialog(hat: HatBilgisi, viewModel: LineMapViewModel, onDismiss: () -> Unit) {
    val isLoading by viewModel.isFaresLoading.collectAsState()
    val fares by viewModel.fares.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadFares(hat.id, hat.aracTipId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MapDarkBackground,
    ) {
        if (isLoading) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (fares == null || fares?.gruplar?.isEmpty() == true) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Text("Tarife bilgisi bulunamadı.", color = Color.White) }
        } else {
            Column(Modifier.padding(16.dp)) {
                Text("Fiyat Tarifesi", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    val groupped = fares?.gruplar?.flatMap { group -> 
                        group.guzergahlar.map { route -> Pair(group, route) } 
                    } ?: emptyList()
                    
                    itemsIndexed(
                        items = groupped,
                        key = { index, pair -> "${pair.first.ad}_${pair.second.guzergahAdi}_$index" }
                    ) { index, (group, route) ->
                        if (index == 0 || groupped[index - 1].first.ad != group.ad) {
                            Text(group.ad, color = Color(0xFF448AFF), fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                        }
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha=0.1f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(route.guzergahAdi, color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                route.ucretler.forEach { u ->
                                    val tipAdi = fares?.tarifeTipleri?.find { it.id == u.tarifeTipId }?.tipAdi ?: "Kart Tipi: ${u.tarifeTipId}"
                                    Row(Modifier.fillMaxWidth().padding(vertical=4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(tipAdi, color = Color.White.copy(alpha=0.7f))
                                        Text("${u.sonUcret} ₺", color = Color(0xFFFFC107), fontWeight = FontWeight.Bold)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineDuyurularFullScreen(
    hat: HatBilgisi,
    viewModel: LineMapViewModel,
    onDismiss: () -> Unit,
    onDuyuruClick: (Duyuru) -> Unit
) {
    val isLoading by viewModel.isAnnouncementsLoading.collectAsState()
    val announcements by viewModel.announcements.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MapDarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        "${hat.hatNumarasi} Hat Duyuruları",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MapDarkBackground)
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (announcements.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Bu hat için duyuru bulunamadı",
                        color = Color.White.copy(alpha = 0.54f),
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(announcements, key = { it.id }) { duyuru ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDuyuruClick(duyuru) },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.08f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = duyuru.baslik,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = duyuru.duzMetin,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
