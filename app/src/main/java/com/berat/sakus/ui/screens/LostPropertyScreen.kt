package com.berat.sakus.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
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
import com.berat.sakus.theme.ThemeManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

data class BasicLine(
    val code: String,
    val description: String
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
fun LostPropertyScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTheme by ThemeManager.getInstance(context).isDarkTheme.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    val themeColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val primaryColor = MaterialTheme.colorScheme.primary
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    var lines by remember { mutableStateOf<List<BasicLine>>(emptyList()) }
    var selectedLine by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf<Calendar?>(null) }
    var selectedTime by remember { mutableStateOf<Calendar?>(null) }
    var isLinePickerOpen by remember { mutableStateOf(false) }

    var nameValue by remember { mutableStateOf("Berat Yayla") }
    var emailValue by remember { mutableStateOf("byayla82@gmail.com") }
    var phoneValue by remember { mutableStateOf("0538 064 34 30") }
    var descriptionValue by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.assets.open("data/lines.json")
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val type = object : TypeToken<List<BasicLine>>(){}.type
                lines = Gson().fromJson(jsonString, type)
            } catch (e: Exception) {
                // Ignore or handle missing file
            }
        }
    }

    Scaffold(
        containerColor = themeColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Kayıp Eşya Bildirimi",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = textColor, modifier = Modifier.size(24.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = themeColor)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.weight(0.4f))
            Text(
                text = "Kayıp Eşya Bildirim Formu",
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(0.4f))

            CustomTextField(
                label = "Ad Soyad",
                value = nameValue,
                onValueChange = { nameValue = it },
                hint = "Ad Soyad Giriniz",
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.weight(0.3f))
            CustomTextField(
                label = "Email",
                value = emailValue,
                onValueChange = { emailValue = it },
                hint = "Email Giriniz",
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.weight(0.3f))
            CustomTextField(
                label = "Telefon",
                value = phoneValue,
                onValueChange = { phoneValue = it },
                hint = "Telefon Giriniz",
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.weight(0.3f))

            // Hat Seçimi
            SelectorField(
                label = "Hat",
                value = selectedLine ?: "Hat Seçiniz",
                isPlaceholder = selectedLine == null,
                icon = Icons.Default.ArrowDropDown,
                onTap = { isLinePickerOpen = true },
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.weight(0.3f))

            // Tarih ve Saat
            Row(modifier = Modifier.weight(1f, fill = false)) {
                Box(modifier = Modifier.weight(1f)) {
                    SelectorField(
                        label = "Kayıp Tarihi",
                        value = selectedDate?.let { "${it.get(Calendar.DAY_OF_MONTH)}/${it.get(Calendar.MONTH) + 1}/${it.get(Calendar.YEAR)}" } ?: "Tarih Seçiniz",
                        isPlaceholder = selectedDate == null,
                        icon = Icons.Outlined.CalendarToday,
                        onTap = {
                            val c = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val newDate = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                    }
                                    selectedDate = newDate
                                },
                                c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH)
                            ).apply {
                                datePicker.maxDate = System.currentTimeMillis()
                            }.show()
                        }
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f)) {
                    SelectorField(
                        label = "Kayıp Saati",
                        value = selectedTime?.let { String.format("%02d:%02d", it.get(Calendar.HOUR_OF_DAY), it.get(Calendar.MINUTE)) } ?: "Saat Seçiniz",
                        isPlaceholder = selectedTime == null,
                        icon = Icons.Outlined.AccessTime,
                        onTap = {
                            val c = Calendar.getInstance()
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    val newTime = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        set(Calendar.MINUTE, minute)
                                    }
                                    selectedTime = newTime
                                },
                                c.get(Calendar.HOUR_OF_DAY),
                                c.get(Calendar.MINUTE),
                                true
                            ).show()
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.weight(0.3f))

            // Description Area
            Column(modifier = Modifier.weight(2.5f)) {
                Surface(
                    color = cardColor,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, dividerColor),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxSize()) {
                        if (descriptionValue.isEmpty()) {
                            Text(
                                "Eşya Detayları (Renk, marka, model vb.)",
                                color = textColor.copy(alpha = 0.54f),
                                fontSize = 15.sp
                            )
                        }
                        BasicTextField(
                            value = descriptionValue,
                            onValueChange = {
                                if (it.length <= 500) descriptionValue = it
                            },
                            textStyle = TextStyle(
                                color = textColor,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(primaryColor),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${descriptionValue.length}/500",
                    color = Color.White.copy(alpha = 0.38f),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            Spacer(modifier = Modifier.weight(0.6f))

            // Buttons
            Button(
                onClick = { /* Handle form history */ },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cardColor,
                    contentColor = textColor
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Geçmiş Bildirimlerim", fontSize = 15.sp, fontWeight = FontWeight.Normal)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { /* Submit logic */ },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text("Gönder", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Bottom Sheet for Line Selection
        if (isLinePickerOpen) {
            ModalBottomSheet(
                onDismissRequest = { isLinePickerOpen = false },
                containerColor = themeColor,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                dragHandle = { BottomSheetDefaults.DragHandle() },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight * 0.5f)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Hat Seçiniz",
                        color = textColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(lines, key = { "${it.code}_${it.description}" }) { line ->
                            Column(modifier = Modifier.clickable {
                                selectedLine = "${line.code} - ${line.description}"
                                isLinePickerOpen = false
                            }) {
                                Text(
                                    text = "${line.code} - ${line.description}",
                                    color = textColor,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                                HorizontalDivider(color = dividerColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier
) {
    val cardColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier,
        color = cardColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, dividerColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                color = textColor.copy(alpha = 0.54f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box {
                if (value.isEmpty()) {
                    Text(text = hint, color = textColor.copy(alpha = 0.38f), fontSize = 15.sp)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(primaryColor),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SelectorField(
    label: String,
    value: String,
    isPlaceholder: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    
    Surface(
        modifier = modifier.fillMaxWidth().clickable { onTap() },
        color = cardColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, dividerColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = label,
                color = textColor.copy(alpha = 0.54f),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    color = if (isPlaceholder) textColor.copy(alpha = 0.38f) else textColor,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.54f),
                    modifier = Modifier.size(if (icon == Icons.Default.ArrowDropDown) 24.dp else 18.dp)
                )
            }
        }
    }
}
