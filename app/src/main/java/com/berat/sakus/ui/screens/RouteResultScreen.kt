package com.berat.sakus.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berat.sakus.data.models.Itinerary
import com.berat.sakus.data.models.Leg
import com.berat.sakus.ui.theme.PrimaryPurple
import java.text.SimpleDateFormat
import java.util.*

// Renkler
private val WalkColor = Color(0xFF4CAF50)
private val BusColor = Color(0xFF2196F3)
private val BestBadgeColor = Color(0xFF4CAF50)

/**
 * Güzergah sonuçlarını gösteren ekran.
 * Referans tasarıma uygun: her adım açıklayıcı Türkçe metin ile gösterilir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteResultScreen(
    itineraries: List<Itinerary>,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Güzergah Sonuçları",
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
        if (itineraries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Sonuç bulunamadı",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Farklı noktalar seçmeyi deneyin",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "${itineraries.size} güzergah bulundu",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                itemsIndexed(itineraries) { index, itinerary ->
                    ItineraryCard(
                        itinerary = itinerary,
                        index = index,
                        isBest = index == 0
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun ItineraryCard(
    itinerary: Itinerary,
    index: Int,
    isBest: Boolean
) {
    var expanded by remember { mutableStateOf(index == 0) } // İlk kart açık gelsin
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "rotation"
    )

    val durationMin = itinerary.duration / 60
    val transferText = if (itinerary.transfer == 0) "aktarmasız" else "${itinerary.transfer} aktarma"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // ── Başlık: Süre + Aktarma + Badge ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Süre ve aktarma bilgisi
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)) {
                            append("$durationMin dk")
                        }
                        withStyle(SpanStyle(fontSize = 14.sp, color = Color.Gray)) {
                            append(", $transferText")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // En Uygun badge (sadece ilk sonuç)
                if (isBest) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BestBadgeColor
                    ) {
                        Text(
                            text = "En Uygun",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Expand/collapse
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationAngle)
                )
            }

            // ── Genişletilmiş detay adımlar ──
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    itinerary.legs.forEachIndexed { legIndex, leg ->
                        val isFirst = legIndex == 0
                        val isLast = legIndex == itinerary.legs.lastIndex

                        when (leg.mode) {
                            "WALK" -> {
                                WalkStepRow(leg = leg, isFirst = isFirst, isLast = isLast)
                            }
                            "BUS" -> {
                                // Otobüse bin
                                BusBoardRow(leg = leg)
                                // Otobüsten in
                                BusAlightRow(leg = leg)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Yürüme Adımı ──
@Composable
private fun WalkStepRow(leg: Leg, isFirst: Boolean, isLast: Boolean) {
    val durationMin = leg.duration / 60
    val distanceFormatted = formatDistance(leg.distance)

    val description = when {
        isFirst && !isLast -> {
            // İlk adım: durağa yürü
            val stopName = leg.to.name ?: "durağa"
            val stopCode = leg.to.stopCode
            if (stopCode != null) {
                "$stopName isimli $stopCode nolu durağa gidiniz."
            } else {
                "$stopName durağına gidiniz."
            }
        }
        isLast -> {
            "Yürüdükten sonra hedefinize ulaşacaksınız."
        }
        else -> {
            val stopName = leg.to.name ?: "durağa"
            val stopCode = leg.to.stopCode
            if (stopCode != null) {
                "$stopName isimli $stopCode nolu durağa yürüyünüz."
            } else {
                "$stopName durağına yürüyünüz."
            }
        }
    }

    val durationText = if (durationMin > 0) "≈ $durationMin dk" else "< 1 dk"

    StepRow(
        icon = {
            Icon(
                Icons.AutoMirrored.Filled.DirectionsWalk,
                contentDescription = null,
                tint = WalkColor,
                modifier = Modifier.size(20.dp)
            )
        },
        iconBgColor = WalkColor.copy(alpha = 0.12f)
    ) {
        Text(
            text = buildAnnotatedString {
                append(description)
                withStyle(SpanStyle(color = Color.Gray, fontSize = 13.sp)) {
                    append(" $durationText • $distanceFormatted")
                }
            },
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Otobüse Bin ──
@Composable
private fun BusBoardRow(leg: Leg) {
    val routeCode = leg.routeCode ?: "Otobüs"

    StepRow(
        icon = {
            Icon(
                Icons.Filled.DirectionsBus,
                contentDescription = null,
                tint = BusColor,
                modifier = Modifier.size(20.dp)
            )
        },
        iconBgColor = BusColor.copy(alpha = 0.12f)
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(routeCode)
                }
                append(" numaralı otobüse binin.")
            },
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Otobüsten İn ──
@Composable
private fun BusAlightRow(leg: Leg) {
    val stopName = leg.to.name ?: "Durak"
    val stopCode = leg.to.stopCode

    val description = if (stopCode != null) {
        "$stopName isimli $stopCode nolu durakta inin."
    } else {
        "$stopName durağında inin."
    }

    StepRow(
        icon = {
            Icon(
                Icons.Filled.DirectionsBus,
                contentDescription = null,
                tint = BusColor,
                modifier = Modifier.size(20.dp)
            )
        },
        iconBgColor = BusColor.copy(alpha = 0.12f)
    ) {
        Text(
            text = description,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Ortak Adım Satırı ──
@Composable
private fun StepRow(
    icon: @Composable () -> Unit,
    iconBgColor: Color,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        // İkon dairesi
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Spacer(modifier = Modifier.width(12.dp))

        // İçerik
        Box(modifier = Modifier.weight(1f).padding(top = 6.dp)) {
            content()
        }
    }
}

private fun formatDistance(meters: Double): String {
    return if (meters >= 1000) {
        String.format("%.1f km", meters / 1000)
    } else {
        "${meters.toInt()} m"
    }
}
