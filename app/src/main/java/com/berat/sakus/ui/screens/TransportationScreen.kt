package com.berat.sakus.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Stable
import com.berat.sakus.R
import com.berat.sakus.data.HatBilgisi
import com.berat.sakus.data.repository.TransportRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

enum class TransportType { BUS, TRAM, METROBUS, ADARAY, MINIBUS, DOLMUS, OTHER }

@Stable
data class TransportLine(
    val id: Int,
    val code: String,
    val description: String,
    val type: TransportType,
    val isFavorite: Boolean = false,
    val category: String,
    val routeSlug: String,
    val sourceData: HatBilgisi? = null
) {
    companion object {
        fun fromApi(apiData: HatBilgisi): TransportLine {
            val type = when (apiData.kategori) {
                "tramvay" -> TransportType.TRAM
                "adaray" -> TransportType.ADARAY
                "minibus" -> TransportType.MINIBUS
                "taksi_dolmus" -> TransportType.DOLMUS
                "metrobus" -> TransportType.METROBUS
                "diger" -> TransportType.OTHER
                else -> TransportType.BUS
            }
            return TransportLine(
                id = apiData.id,
                code = apiData.hatNumarasi,
                description = apiData.ad,
                type = type,
                category = apiData.kategori,
                routeSlug = apiData.slug,
                sourceData = apiData
            )
        }
    }
}

data class FilterOption(
    val id: String,
    val label: String
)

private const val INITIAL_PAGE_SIZE = 30
private const val PAGE_SIZE = 20

@Composable
@androidx.compose.runtime.ReadOnlyComposable
private fun s(value: Float): Dp {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val designWidth = 430f
    val scale = (screenWidthDp / designWidth).coerceIn(0f, 1.1f)
    return (value * scale).dp
}

@Composable
@androidx.compose.runtime.ReadOnlyComposable
private fun scaledSp(value: Float): androidx.compose.ui.unit.TextUnit {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val designWidth = 430f
    val scale = (screenWidthDp / designWidth).coerceIn(0f, 1.1f)
    return (value * scale).sp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportationScreen(
    onNavigateRouteMap: (HatBilgisi) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToSchedule: ((HatBilgisi) -> Unit)? = null
) {
    val context = LocalContext.current
    val themeColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val primaryColor = MaterialTheme.colorScheme.primary
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    
    val repository = remember { TransportRepository.getInstance(context) }
    var allLines by remember { mutableStateOf<List<TransportLine>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(350) // Animasyonun tamamen bitmesini bekle
        withContext(Dispatchers.IO) {
            repository.getHatlarFlow().collectLatest { apiHatlar ->
                if (apiHatlar.isNotEmpty()) {
                    val mapped = apiHatlar.map { TransportLine.fromApi(it) }
                    withContext(Dispatchers.Main) {
                        allLines = mapped
                        isLoading = false
                    }
                }
            }
        }
    }
    
    var filters by remember { mutableStateOf<List<FilterOption>>(emptyList()) }
    var selectedFilterId by remember { mutableStateOf("all") }
    
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val fallbackFilters = listOf(
        FilterOption("all", "Tümü"),
        FilterOption("belediye", "Belediye"),
        FilterOption("ozel_halk", "Özel Halk"),
        FilterOption("minibus", "Minibüs"),
        FilterOption("dolmus", "Dolmuş"),
        FilterOption("tram", "Tramvay"),
        FilterOption("metrobus", "Metrobüs"),
        FilterOption("adaray", "Adaray")
    )

    LaunchedEffect(Unit) {
        delay(300) // Animasyon bitmesini bekle
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.assets.open("data/filters.json")
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val type = object : TypeToken<List<FilterOption>>(){}.type
                filters = Gson().fromJson(jsonString, type)
            } catch (_: Exception) {
                filters = fallbackFilters
            }
            
            if (filters.isNotEmpty() && filters.none { it.id == selectedFilterId }) {
                selectedFilterId = filters.first().id
            }
        }
    }

    var filteredLines by remember { mutableStateOf<List<TransportLine>>(emptyList()) }
    
    LaunchedEffect(allLines, selectedFilterId, searchQuery) {
        if (allLines.isEmpty()) return@LaunchedEffect
        withContext(Dispatchers.Default) {
            val filtered = allLines.filter { line ->
                var typeMatches = true
                when (selectedFilterId) {
                    "belediye" -> typeMatches = line.category == "belediye"
                    "ozel_halk" -> typeMatches = line.category == "ozel_halk"
                    "minibus" -> typeMatches = line.category == "minibus"
                    "dolmus" -> typeMatches = line.category == "taksi_dolmus"
                    "tram" -> typeMatches = line.category == "tramvay"
                    "metrobus" -> typeMatches = line.category == "metrobus"
                    "adaray" -> typeMatches = line.category == "adaray"
                }
                
                val searchMatches = if (searchQuery.isNotEmpty()) {
                    line.code.contains(searchQuery, ignoreCase = true) || line.description.contains(searchQuery, ignoreCase = true)
                } else true
                
                typeMatches && searchMatches
            }

            val padRegex = Regex("\\d+")
            val finalFiltered = filtered.sortedWith(compareBy<TransportLine> { line ->
                if (selectedFilterId == "all") {
                    when (line.category) {
                        "metrobus" -> 1
                        "adaray" -> 2
                        "tramvay" -> 3
                        "belediye" -> 4
                        "ozel_halk" -> 5
                        "minibus" -> 6
                        "taksi_dolmus" -> 7
                        else -> 8
                    }
                } else {
                    0
                }
            }.thenBy { line ->
                line.code.replace(padRegex) { match -> match.value.padStart(5, '0') }
            })
            
            withContext(Dispatchers.Main) {
                filteredLines = finalFiltered
            }
        }
    }
    
    val favoriteCount = remember(allLines) { allLines.count { it.isFavorite } }
    val totalFilteredCount = filteredLines.size

    val listState = rememberLazyListState()

    Scaffold(
        containerColor = themeColor,
        topBar = {
            Column(modifier = Modifier.background(cardColor).statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .background(cardColor)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSearching) {
                        IconButton(onClick = { 
                            isSearching = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = textColor, modifier = Modifier.size(24.dp))
                        }
                    } else {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = textColor, modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (isSearching) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = textColor, fontSize = 16.sp),
                            singleLine = true,
                            cursorBrush = SolidColor(primaryColor),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Hat Ara...", color = textColor.copy(alpha = 0.54f), fontSize = 16.sp)
                                }
                                innerTextField()
                            }
                        )
                    } else {
                        Text(
                            text = "Hatlar",
                            color = textColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!isSearching) {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Ara", tint = textColor, modifier = Modifier.size(24.dp))
                        }
                    }
                }
                HorizontalDivider(color = dividerColor, thickness = 1.dp)
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primaryColor)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Filters Row
                if (filters.isNotEmpty()) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(filters, key = { it.id }) { filter ->
                                val isSelected = filter.id == selectedFilterId
                                FilterTab(
                                    filter = filter,
                                    isSelected = isSelected,
                                    onSelect = { selectedFilterId = filter.id }
                                )
                            }
                        }
                    }
                }

                if (favoriteCount > 0 && searchQuery.isEmpty()) {
                    item {
                        Text(
                            text = "Favori Hatlar $favoriteCount",
                            color = textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                item {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Arama Sonuçları $totalFilteredCount" else "Tüm Hatlar $totalFilteredCount",
                        color = textColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (filteredLines.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 20.dp), contentAlignment = Alignment.Center) {
                            Text(
                                "Hat bulunamadı.",
                                color = textColor.copy(alpha = 0.54f),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(
                        items = filteredLines,
                        key = { it.id }
                    ) { line ->
                        LineItem(
                            line = line,
                            onTap = {
                                if (line.sourceData != null) {
                                    if (onNavigateToSchedule != null) {
                                        onNavigateToSchedule(line.sourceData)
                                    } else {
                                        onNavigateRouteMap(line.sourceData)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterTab(
    filter: FilterOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
    val borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant

    Surface(
        onClick = onSelect,
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (filter.id) {
                "all" -> Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                "belediye" -> Icon(painterResource(R.drawable.ic_bus), contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                "ozel_halk" -> Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                "minibus" -> Icon(Icons.Default.AirportShuttle, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                "dolmus" -> Icon(Icons.Default.LocalTaxi, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                "tram" -> Icon(painterResource(R.drawable.ic_tram), contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                "metrobus" -> Icon(painterResource(R.drawable.ic_bus), contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                "adaray" -> Icon(painterResource(R.drawable.ic_tram), contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            }
            if (filter.id in listOf("all", "belediye", "ozel_halk", "minibus", "dolmus", "tram", "metrobus", "adaray")) {
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = filter.label,
                color = contentColor,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LineItem(
    line: TransportLine,
    onTap: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onBackground
    val cardColor = MaterialTheme.colorScheme.surface

    Surface(
        onClick = onTap,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = cardColor
    ) {
        Row(
            modifier = Modifier
                .height(60.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
         when (line.type) {
             TransportType.BUS -> Icon(painterResource(R.drawable.ic_bus), contentDescription = null, tint = primaryColor, modifier = Modifier.size(22.dp))
             TransportType.TRAM -> Icon(painterResource(R.drawable.ic_tram), contentDescription = null, tint = primaryColor, modifier = Modifier.size(22.dp))
             TransportType.METROBUS -> Icon(painterResource(R.drawable.ic_bus), contentDescription = null, tint = primaryColor, modifier = Modifier.size(22.dp))
             TransportType.ADARAY -> Icon(painterResource(R.drawable.ic_tram), contentDescription = null, tint = primaryColor, modifier = Modifier.size(22.dp))
             TransportType.MINIBUS -> Icon(Icons.Default.AirportShuttle, contentDescription = null, tint = primaryColor, modifier = Modifier.size(22.dp))
             TransportType.DOLMUS -> Icon(Icons.Default.LocalTaxi, contentDescription = null, tint = primaryColor, modifier = Modifier.size(22.dp))
             else -> {}
         }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = line.code,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = line.description,
                color = textColor.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        }
    }
}

