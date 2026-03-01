package com.berat.sakus.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FaqItem(
    val question: String,
    val answer: String
)

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
fun FaqScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val themeColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    
    var faqList by remember { mutableStateOf<List<FaqItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val fallbackFaqs = listOf(
        FaqItem(
            question = "Sakarya Kart54 nedir?",
            answer = "Kart54 Sakarya Büyükşehir Belediyesi sınırları içinde toplu taşımada kullanılan akıllı karttır."
        ),
        FaqItem(
            question = "Bakiye nasıl yüklenir?",
            answer = "Kart54 dolum noktalarından, kiosk cihazlarından veya online olarak web sitemiz/uygulamamız üzerinden yükleme yapabilirsiniz."
        ),
        FaqItem(
            question = "Kayıp kart durumunda ne yapmalıyım?",
            answer = "Kayıp veya çalıntı durumlarında öncelikle Kart54 merkezine başvurarak kartınızı kapattırmalısınız. Kişiselleştirilmiş kartların içindeki bakiye yeni karta aktarılabilir."
        )
    )

    LaunchedEffect(Unit) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                // Attempt to load faq.json from assets
                val inputStream = context.assets.open("data/faq.json")
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val type = object : TypeToken<List<FaqItem>>(){}.type
                faqList = Gson().fromJson(jsonString, type)
            } catch (e: Exception) {
                // Fallback to hardcoded list if file is not present
                faqList = fallbackFaqs
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
                        Box(modifier = Modifier.fillMaxWidth().padding(end = 48.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Sık Sorulan Sorular", 
                                fontSize = 18.sp, 
                                fontWeight = FontWeight.Medium, 
                                color = textColor
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = textColor, modifier = Modifier.size(24.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = cardColor)
                )
                HorizontalDivider(color = dividerColor, thickness = 1.dp)
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(faqList, key = { it.question }) { item ->
                    FaqAccordionItem(item = item)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun FaqAccordionItem(item: FaqItem) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val cardColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = cardColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, dividerColor.copy(alpha = 0.5f)) // subtle border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.question,
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Daralt" else "Genişlet",
                    tint = if (isExpanded) primaryColor else textColor.copy(alpha = 0.54f),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Text(
                    text = item.answer,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
            }
        }
    }
}
