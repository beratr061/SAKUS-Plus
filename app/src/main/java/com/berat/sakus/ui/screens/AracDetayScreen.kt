package com.berat.sakus.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.berat.sakus.data.models.AracKonumu
import com.berat.sakus.ui.theme.PrimaryPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AracDetayScreen(
    busNumber: Int,
    onNavigateBack: () -> Unit,
    viewModel: AracDetayViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(busNumber) {
        viewModel.baslat(busNumber)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.durdur() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Araç Detay",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.yukleniyor && state.arac == null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryPurple)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Araç bilgileri yükleniyor...",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }

                state.hata != null && state.arac == null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            state.hata ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                state.arac != null -> {
                    val arac = state.arac!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Canlı takip banner
                        if (state.canliTakip) {
                            CanliTakipBanner()
                        }

                        // Üst header kartı
                        AracHeaderKarti(arac)

                        // Durum kartı
                        DurumKarti(arac)

                        // Güzergah kartı
                        GuzergahKarti(arac)

                        // Durak bilgileri kartı
                        DurakBilgileriKarti(arac)

                        // Hız & Mesafe kartı
                        HizMesafeKarti(arac)

                        // Konum kartı
                        KonumKarti(arac)

                        // Teknik bilgiler kartı
                        TeknikBilgilerKarti(arac)

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CanliTakipBanner() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF4CAF50).copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFF4CAF50).copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Canlı takip aktif — veriler otomatik güncelleniyor",
            fontSize = 13.sp,
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AracHeaderKarti(arac: AracKonumu) {
    val durumRenk = durumRengiGetir(arac.durum)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Gradient header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(PrimaryPurple, PrimaryPurple.copy(alpha = 0.7f))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Plaka
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = arac.plaka.ifEmpty { "—" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Color.White
                            )
                        }

                        // Durum badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(durumRenk.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(durumRenk)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = arac.durumTr,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DirectionsBus,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hat ${arac.hatNo}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Kapı No: ${arac.aracNumarasi}",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Güzergah adı
            if (arac.guzergahAdi.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Route,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = arac.guzergahAdi,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DurumKarti(arac: AracKonumu) {
    val durumRenk = durumRengiGetir(arac.durum)

    DetayKartBaslik(
        icon = Icons.Default.Info,
        baslik = "Araç Durumu",
        renk = durumRenk
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = arac.durumTr,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = durumRenk
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "API: ${arac.durum}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(durumRenk.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (arac.durum.uppercase()) {
                        "AT_STOP", "DWELL" -> Icons.Default.LocalParking
                        "IN_TRAFFIC" -> Icons.Default.Traffic
                        "MOVING", "IN_MOTION", "CRUISE" -> Icons.Default.DirectionsBus
                        "DEPARTING" -> Icons.Default.PlayArrow
                        "APPROACH", "ARRIVING" -> Icons.Default.NearMe
                        "OUT_OF_SERVICE" -> Icons.Default.Cancel
                        else -> Icons.Default.Help
                    },
                    contentDescription = null,
                    tint = durumRenk,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun GuzergahKarti(arac: AracKonumu) {
    if (arac.baslangicYer.isEmpty() && arac.bitisYer.isEmpty()) return

    DetayKartBaslik(
        icon = Icons.Default.SwapHoriz,
        baslik = "Güzergah",
        renk = PrimaryPurple
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Başlangıç
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.TripOrigin,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Başlangıç",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = arac.baslangicYer.ifEmpty { "—" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Ok
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = PrimaryPurple.copy(alpha = 0.4f),
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(24.dp)
            )

            // Bitiş
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Flag,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Bitiş",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = arac.bitisYer.ifEmpty { "—" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Yön bilgisi
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DetayBilgiChip(
                icon = Icons.Default.Explore,
                etiket = "Yön",
                deger = if (arac.yon == 0) "Gidiş" else "Dönüş"
            )
            if (arac.guzergahId != null) {
                DetayBilgiChip(
                    icon = Icons.Default.Tag,
                    etiket = "Güzergah ID",
                    deger = arac.guzergahId.toString()
                )
            }
        }
    }
}

@Composable
private fun DurakBilgileriKarti(arac: AracKonumu) {
    DetayKartBaslik(
        icon = Icons.Default.LocationOn,
        baslik = "Durak Bilgileri",
        renk = Color(0xFF42A5F5)
    ) {
        // Mevcut durak
        DurakSatiri(
            ikon = Icons.Default.LocationOn,
            etiket = "Bulunduğu Durak",
            deger = arac.mevcutDurak.ifEmpty { "—" },
            durakId = arac.mevcutDurakId,
            renk = Color(0xFF4CAF50)
        )

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Sonraki durak
        DurakSatiri(
            ikon = Icons.Default.NearMe,
            etiket = "Sonraki Durak",
            deger = arac.sonrakiDurak.ifEmpty { "—" },
            durakId = arac.sonrakiDurakId,
            renk = Color(0xFF42A5F5)
        )

        // Mesafe ve ETA
        if (arac.sonrakiDurakMesafe > 0 || arac.etaSaniye > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF42A5F5).copy(alpha = 0.06f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (arac.sonrakiDurakMesafe > 0) {
                    val mesafeStr = if (arac.sonrakiDurakMesafe >= 1000) {
                        "%.1f km".format(arac.sonrakiDurakMesafe / 1000)
                    } else {
                        "%.0f m".format(arac.sonrakiDurakMesafe)
                    }
                    MesafeEtaChip(
                        ikon = Icons.Default.Straighten,
                        etiket = "Mesafe",
                        deger = mesafeStr,
                        renk = Color(0xFFFFA726)
                    )
                }
                if (arac.etaSaniye > 0) {
                    val dakika = (arac.etaSaniye / 60).toInt()
                    val saniye = (arac.etaSaniye % 60).toInt()
                    val etaStr = if (dakika > 0) "$dakika dk $saniye sn" else "$saniye sn"
                    MesafeEtaChip(
                        ikon = Icons.Default.Schedule,
                        etiket = "Tahmini Varış",
                        deger = etaStr,
                        renk = Color(0xFF66BB6A)
                    )
                }
            }
        }
    }
}

@Composable
private fun HizMesafeKarti(arac: AracKonumu) {
    DetayKartBaslik(
        icon = Icons.Default.Speed,
        baslik = "Hız & Hareket",
        renk = Color(0xFF42A5F5)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Hız
            HizGostergesi(
                deger = arac.hiz.toString(),
                birim = "km/h",
                etiket = "Anlık Hız",
                renk = when {
                    arac.hiz > 60 -> Color(0xFFE53935)
                    arac.hiz > 30 -> Color(0xFFFFA726)
                    arac.hiz > 0 -> Color(0xFF4CAF50)
                    else -> Color(0xFF9E9E9E)
                }
            )

            // Yön derecesi
            HizGostergesi(
                deger = "%.0f°".format(arac.baslik),
                birim = "",
                etiket = "Yön Açısı",
                renk = Color(0xFF42A5F5)
            )
        }
    }
}

@Composable
private fun KonumKarti(arac: AracKonumu) {
    if (arac.lat == 0.0 && arac.lng == 0.0) return

    DetayKartBaslik(
        icon = Icons.Default.MyLocation,
        baslik = "Konum",
        renk = Color(0xFF66BB6A)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            KonumBilgi(etiket = "Enlem", deger = "%.6f".format(arac.lat))
            KonumBilgi(etiket = "Boylam", deger = "%.6f".format(arac.lng))
        }
    }
}

@Composable
private fun TeknikBilgilerKarti(arac: AracKonumu) {
    DetayKartBaslik(
        icon = Icons.Default.Settings,
        baslik = "Teknik Bilgiler",
        renk = Color(0xFF78909C)
    ) {
        val bilgiler = mutableListOf<Pair<String, String>>()
        bilgiler.add("Plaka" to arac.plaka.ifEmpty { "—" })
        bilgiler.add("Kapı Numarası" to arac.aracNumarasi.toString())
        bilgiler.add("Hat Numarası" to arac.hatNo.ifEmpty { "—" })
        if (arac.hatId != 0) bilgiler.add("Hat ID" to arac.hatId.toString())
        if (arac.guzergahId != null) bilgiler.add("Güzergah ID" to arac.guzergahId.toString())
        if (arac.takipId != 0L) bilgiler.add("Takip ID" to arac.takipId.toString())
        if (arac.mevcutDurakId != 0) bilgiler.add("Mevcut Durak ID" to arac.mevcutDurakId.toString())
        if (arac.sonrakiDurakId != 0) bilgiler.add("Sonraki Durak ID" to arac.sonrakiDurakId.toString())
        if (arac.nhatNo != 0) bilgiler.add("NHAT No" to arac.nhatNo.toString())
        bilgiler.add("Durum (Ham)" to arac.durum.ifEmpty { "—" })
        bilgiler.add("Yön Kodu" to arac.yon.toString())

        bilgiler.forEachIndexed { index, (etiket, deger) ->
            if (index > 0) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = etiket,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = deger,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ─── Yardımcı Composable'lar ───

@Composable
private fun DetayKartBaslik(
    icon: ImageVector,
    baslik: String,
    renk: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(renk.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = renk,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = baslik,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun DurakSatiri(
    ikon: ImageVector,
    etiket: String,
    deger: String,
    durakId: Int,
    renk: Color
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(renk.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ikon,
                contentDescription = null,
                tint = renk,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = etiket,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = deger,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (durakId != 0) {
                Text(
                    text = "Durak ID: $durakId",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun MesafeEtaChip(
    ikon: ImageVector,
    etiket: String,
    deger: String,
    renk: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = ikon,
            contentDescription = null,
            tint = renk,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = deger,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = etiket,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun HizGostergesi(
    deger: String,
    birim: String,
    etiket: String,
    renk: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(renk.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = deger,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = renk
                )
                if (birim.isNotEmpty()) {
                    Text(
                        text = birim,
                        fontSize = 10.sp,
                        color = renk.copy(alpha = 0.7f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = etiket,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun KonumBilgi(etiket: String, deger: String) {
    Column {
        Text(
            text = etiket,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        Text(
            text = deger,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DetayBilgiChip(
    icon: ImageVector,
    etiket: String,
    deger: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$etiket: $deger",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium
        )
    }
}

private fun durumRengiGetir(durum: String): Color = when (durum.uppercase()) {
    "AT_STOP", "DWELL" -> Color(0xFF4CAF50)
    "IN_TRAFFIC" -> Color(0xFFFFA726)
    "MOVING", "IN_MOTION", "CRUISE" -> Color(0xFF42A5F5)
    "OUT_OF_SERVICE", "DEADHEAD" -> Color(0xFFE53935)
    "IDLE", "LAYOVER" -> Color(0xFF9E9E9E)
    "DEPARTING" -> Color(0xFF66BB6A)
    "APPROACH", "ARRIVING" -> Color(0xFFAB47BC)
    "OFF_ROUTE" -> Color(0xFFFF7043)
    else -> Color(0xFF78909C)
}
