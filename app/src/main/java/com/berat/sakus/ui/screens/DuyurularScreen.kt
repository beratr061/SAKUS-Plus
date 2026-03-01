package com.berat.sakus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.berat.sakus.data.Duyuru
import com.berat.sakus.data.SbbApiServisi
import com.berat.sakus.ui.theme.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
private fun s(value: Float): Dp {
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
fun DuyurularScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Duyuru) -> Unit = {}
) {
    val context = LocalContext.current
    val isDarkTheme by ThemeManager.getInstance(context).isDarkTheme.collectAsState()

    val themeColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val primaryColor = MaterialTheme.colorScheme.primary
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    var isLoading by remember { mutableStateOf(true) }
    var duyurular by remember { mutableStateOf<List<Duyuru>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var dbHatlar by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(Unit) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val db = com.berat.sakus.data.local.SakusDatabase.getInstance(context)
                val hatlarList = db.hatDao().tumHatlariGetirOnce()
                dbHatlar = hatlarList.associate { it.hatNumarasi to it.ad }

                val api = SbbApiServisi.getInstance(context)
                val localNotifications = db.appNotificationDao().getAllNotifications().map { it.toDuyuru() }
                
                val genel = api.duyurulariGetir()
                val hat = api.tumHatDuyurulariGetir()
                
                val combinedList = (localNotifications + genel + hat)
                    .distinctBy { it.id }
                    .sortedByDescending { it.baslangicTarih } // Assuming dates sort well or keep as id for API + date for locals
                
                duyurular = combinedList
            } catch (e: Exception) {
                errorMessage = "Duyurular yüklenemedi."
            }
        }
        isLoading = false
    }

    Scaffold(
        containerColor = themeColor,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(end = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Duyurular",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = cardColor)
                )
                HorizontalDivider(color = dividerColor, thickness = 1.dp)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        color = primaryColor,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage!!,
                        color = textColor,
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                duyurular.isEmpty() -> {
                    Text(
                        text = "Aktif duyuru bulunmamaktadır.",
                        color = textColor,
                        fontSize = 16.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(duyurular, key = { it.id }) { duyuru ->
                            DuyuruItemCard(
                                duyuru = duyuru,
                                hatAdiFromDb = dbHatlar[duyuru.hatNumarasi],
                                isDarkTheme = isDarkTheme,
                                onClick = { onNavigateToDetail(duyuru) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DuyuruItemCard(duyuru: Duyuru, hatAdiFromDb: String?, isDarkTheme: Boolean, onClick: () -> Unit = {}) {
    val formattedDate = if (duyuru.baslangicTarih.isNotEmpty()) {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("tr"))
            val date = inputFormat.parse(duyuru.baslangicTarih)
            date?.let { outputFormat.format(it) } ?: duyuru.baslangicTarih
        } catch (_: Exception) {
            duyuru.baslangicTarih
        }
    } else ""

    val finalHatAdi = duyuru.hatAdi?.takeIf { it.isNotBlank() } ?: hatAdiFromDb

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .background(Color(0xFF232323), RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        // Tarih (Date)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, top = 9.dp)
        ) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = com.berat.sakus.R.drawable.ic_calendar),
                contentDescription = null,
                tint = Color(0xFFFFFFFF),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = formattedDate,
                color = Color(0xFFBA47E7),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Hat Kodu ve Başlık
        androidx.compose.material3.Text(
            text = androidx.compose.ui.text.buildAnnotatedString {
                if (!duyuru.hatNumarasi.isNullOrEmpty() || !finalHatAdi.isNullOrEmpty()) {
                    withStyle(style = androidx.compose.ui.text.SpanStyle(color = Color(0xFFBA47E7))) {
                        val hatMetni = buildString {
                            if (!duyuru.hatNumarasi.isNullOrEmpty()) append(duyuru.hatNumarasi)
                            if (!finalHatAdi.isNullOrEmpty()) {
                                if (isNotEmpty()) append(" ")
                                append(finalHatAdi.trim())
                            }
                        }
                        append(hatMetni)
                    }
                    withStyle(style = androidx.compose.ui.text.SpanStyle(color = Color.White)) {
                        val gosterilecekMetin = if (duyuru.baslik.trim().equals("Duyuru", ignoreCase = true) || duyuru.baslik.trim().equals("Hat Duyurusu", ignoreCase = true)) {
                            duyuru.aciklama?.takeIf { it.isNotBlank() } ?: duyuru.baslik
                        } else {
                            duyuru.baslik
                        }
                        append(" - $gosterilecekMetin")
                    }
                } else {
                    withStyle(style = androidx.compose.ui.text.SpanStyle(color = Color.White)) {
                        append(duyuru.baslik)
                    }
                }
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(start = 14.dp, top = 41.dp)
                .padding(end = 14.dp)
        )
    }
}
