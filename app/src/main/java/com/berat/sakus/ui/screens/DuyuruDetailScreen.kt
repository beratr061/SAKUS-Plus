package com.berat.sakus.ui.screens

import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import com.berat.sakus.data.Duyuru
import java.text.SimpleDateFormat
import java.util.*

@Composable
private fun s(value: Float): androidx.compose.ui.unit.Dp {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val designWidth = 430f
    val scale = (screenWidthDp / designWidth).coerceIn(0f, 1.1f)
    return (value * scale).dp
}

@Composable
private fun scaledSp(value: Float): androidx.compose.ui.unit.TextUnit {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val designWidth = 430f
    val scale = (screenWidthDp / designWidth).coerceIn(0f, 1.1f)
    return (value * scale).sp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuyuruDetailScreen(
    duyuru: Duyuru,
    onNavigateBack: () -> Unit
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val topBarColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val contentTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f)
    val dateColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)

    val formattedStartDate = if (duyuru.baslangicTarih.isNotEmpty()) {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale("tr"))
            val date = inputFormat.parse(duyuru.baslangicTarih)
            date?.let { outputFormat.format(it) } ?: duyuru.baslangicTarih
        } catch (_: Exception) {
            duyuru.baslangicTarih
        }
    } else ""

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Duyuru Detayı",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Geri",
                                tint = textColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarColor)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f), thickness = 1.dp)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Başlık ve Üst Bilgiler
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                // Hat Kodu ve Adı Etiketi (Badge)
                if (!duyuru.hatNumarasi.isNullOrEmpty() || !duyuru.hatAdi.isNullOrEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFBA47E7).copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            val hatMetni = buildString {
                                if (!duyuru.hatNumarasi.isNullOrEmpty()) append(duyuru.hatNumarasi)
                                if (!duyuru.hatAdi.isNullOrEmpty()) {
                                    if (isNotEmpty()) append(" - ")
                                    append(duyuru.hatAdi.trim())
                                }
                            }
                            Text(
                                text = hatMetni,
                                color = Color(0xFFBA47E7),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Ana Başlık
                Text(
                    text = duyuru.baslik,
                    modifier = Modifier.fillMaxWidth(),
                    color = textColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 32.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Tarih (Date)
                if (formattedStartDate.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.berat.sakus.R.drawable.ic_calendar),
                            contentDescription = "Tarih",
                            tint = dateColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formattedStartDate,
                            fontSize = 14.sp,
                            color = dateColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            // İçerik Alanı
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(20.dp)
            ) {
                val hexColor = "#" + Integer.toHexString(contentTextColor.toArgb()).substring(2)
                val androidColor = android.graphics.Color.parseColor(hexColor)

                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx ->
                        TextView(ctx).apply {
                            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15f)
                            setLineSpacing(0f, 1.5f)
                            movementMethod = android.text.method.LinkMovementMethod.getInstance()
                        }
                    },
                    update = { view ->
                        view.setTextColor(androidColor)
                        view.text = HtmlCompat.fromHtml(
                            duyuru.icerik ?: "",
                            HtmlCompat.FROM_HTML_MODE_COMPACT
                        )
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
