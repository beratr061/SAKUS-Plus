package com.berat.sakus.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.berat.sakus.data.models.AracKonumu
import com.berat.sakus.ui.theme.PrimaryPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AracSorguScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit = {},
    viewModel: AracSorguViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current

    DisposableEffect(Unit) {
        onDispose {
            viewModel.durdur()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Araç Sorgula",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Section
            SearchSection(
                sorguMetni = state.sorguMetni,
                sorguTipi = state.sorguTipi,
                yukleniyor = state.yukleniyor,
                canliTakip = state.canliTakip,
                onSorguMetniDegisti = { viewModel.sorguMetniGuncelle(it) },
                onSorguTipiDegisti = { viewModel.sorguTipiGuncelle(it) },
                onSorgula = {
                    focusManager.clearFocus()
                    viewModel.sorgula()
                },
                onDurdur = { viewModel.durdur() }
            )

            // Error Message
            AnimatedVisibility(
                visible = state.hata != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                state.hata?.let { hata ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = hata,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Live Tracking Indicator
            AnimatedVisibility(
                visible = state.canliTakip,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LiveTrackingBanner()
            }

            // Results
            if (state.sonuclar.isEmpty() && !state.yukleniyor && state.hata == null) {
                EmptyState(hasSorgu = state.sorguMetni.isNotEmpty() && state.canliTakip)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.sonuclar.isNotEmpty()) {
                        item {
                            Text(
                                text = "${state.sonuclar.size} araç bulundu",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                    items(state.sonuclar, key = { "${it.plaka}_${it.aracNumarasi}" }) { arac ->
                        AracSonucKarti(
                            arac = arac,
                            onClick = { onNavigateToDetail(arac.aracNumarasi) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSection(
    sorguMetni: String,
    sorguTipi: SorguTipi,
    yukleniyor: Boolean,
    canliTakip: Boolean,
    onSorguMetniDegisti: (String) -> Unit,
    onSorguTipiDegisti: (SorguTipi) -> Unit,
    onSorgula: () -> Unit,
    onDurdur: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Sorgu Tipi Seçici
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SorguTipiChip(
                    label = "Plaka",
                    icon = Icons.Default.DirectionsBus,
                    selected = sorguTipi == SorguTipi.PLAKA,
                    onClick = { onSorguTipiDegisti(SorguTipi.PLAKA) },
                    modifier = Modifier.weight(1f)
                )
                SorguTipiChip(
                    label = "Kapı No",
                    icon = Icons.Default.Tag,
                    selected = sorguTipi == SorguTipi.KAPI_NO,
                    onClick = { onSorguTipiDegisti(SorguTipi.KAPI_NO) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arama Alanı
            OutlinedTextField(
                value = sorguMetni,
                onValueChange = onSorguMetniDegisti,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        if (sorguTipi == SorguTipi.PLAKA) "Örn: 54 J 0165"
                        else "Örn: 19165",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = PrimaryPurple
                    )
                },
                trailingIcon = {
                    if (sorguMetni.isNotEmpty()) {
                        IconButton(onClick = { onSorguMetniDegisti("") }) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Temizle",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = if (sorguTipi == SorguTipi.PLAKA) KeyboardCapitalization.Characters else KeyboardCapitalization.None,
                    keyboardType = if (sorguTipi == SorguTipi.KAPI_NO) KeyboardType.Number else KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onSorgula() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    cursorColor = PrimaryPurple
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sorgula / Durdur Butonu
            if (canliTakip) {
                OutlinedButton(
                    onClick = onDurdur,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFFE53935), Color(0xFFFF5252))
                        )
                    )
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Canlı Takibi Durdur",
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Button(
                    onClick = onSorgula,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    enabled = !yukleniyor && sorguMetni.isNotBlank()
                ) {
                    if (yukleniyor) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        if (yukleniyor) "Aranıyor..." else "Canlı Sorgula",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun SorguTipiChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) PrimaryPurple.copy(alpha = 0.12f) else Color.Transparent
    val borderColor = if (selected) PrimaryPurple else MaterialTheme.colorScheme.outlineVariant
    val contentColor = if (selected) PrimaryPurple else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun LiveTrackingBanner() {
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
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF4CAF50).copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
private fun AracSonucKarti(arac: AracKonumu, onClick: () -> Unit = {}) {
    val durumRenk = when (arac.durum.uppercase()) {
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

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Üst Bölüm - Plaka, Hat ve Durum
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Plaka
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PrimaryPurple.copy(alpha = 0.1f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = arac.plaka.ifEmpty { "—" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = PrimaryPurple
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "#${arac.aracNumarasi}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Hat Bilgisi
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DirectionsBus,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Hat ${arac.hatNo}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Durum Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(durumRenk.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = durumRenk
                        )
                    }
                }
            }

            // Güzergah adı
            if (arac.guzergahAdi.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = arac.guzergahAdi,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Alt Bölüm - Detay Bilgileri
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Mevcut Durak
                DetailItem(
                    icon = Icons.Default.LocationOn,
                    label = "Bulunduğu Durak",
                    value = arac.mevcutDurak.ifEmpty { "—" },
                    modifier = Modifier.weight(1f)
                )
                // Sonraki Durak
                DetailItem(
                    icon = Icons.AutoMirrored.Filled.NavigateNext,
                    label = "Sonraki Durak",
                    value = arac.sonrakiDurak.ifEmpty { "—" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Hız
                InfoChip(
                    icon = Icons.Default.Speed,
                    text = arac.hizFormati,
                    color = Color(0xFF42A5F5)
                )
                // Mesafe
                if (arac.sonrakiDurakMesafe > 0) {
                    InfoChip(
                        icon = Icons.Default.Straighten,
                        text = if (arac.sonrakiDurakMesafe >= 1000) {
                            "%.1f km".format(arac.sonrakiDurakMesafe / 1000)
                        } else {
                            "%.0f m".format(arac.sonrakiDurakMesafe)
                        },
                        color = Color(0xFFFFA726)
                    )
                }
                // ETA
                if (arac.etaSaniye > 0) {
                    val dakika = (arac.etaSaniye / 60).toInt()
                    InfoChip(
                        icon = Icons.Default.Schedule,
                        text = if (dakika > 0) "$dakika dk" else "< 1 dk",
                        color = Color(0xFF66BB6A)
                    )
                }
            }

            // Başlangıç - Bitiş
            if (arac.baslangicYer.isNotEmpty() && arac.bitisYer.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = arac.baslangicYer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = PrimaryPurple.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp).padding(horizontal = 4.dp)
                    )
                    Text(
                        text = arac.bitisYer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 18.dp)
        )
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun EmptyState(hasSorgu: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (hasSorgu) Icons.Default.SearchOff else Icons.Default.DirectionsBus,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = PrimaryPurple.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (hasSorgu) "Araç bulunamadı" else "Araç Sorgulama",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasSorgu)
                "Girdiğiniz plaka veya kapı numarasına ait\naktif bir araç bulunamadı."
            else
                "Plaka numarası veya kapı numarası ile\naraç konumunu canlı olarak sorgulayın.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}
