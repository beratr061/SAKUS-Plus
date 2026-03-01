package com.berat.sakus.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

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
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark by com.berat.sakus.ui.theme.ThemeManager.getInstance(context).isDarkTheme.collectAsState()
    val themeColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val subTextColor = if (isDark) Color.LightGray else Color.DarkGray

    // Permission States
    var locationGranted by remember { mutableStateOf(checkPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) }
    var notificationGranted by remember { 
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                checkPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            } else {
                true // Granted implicitly on older versions
            }
        ) 
    }
    var cameraGranted by remember { mutableStateOf(checkPermission(context, Manifest.permission.CAMERA)) }

    // Permission Launchers
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        locationGranted = isGranted
        if (!isGranted) openAppSettings(context)
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        notificationGranted = isGranted
        if (!isGranted) openAppSettings(context)
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        cameraGranted = isGranted
        if (!isGranted) openAppSettings(context)
    }

    Scaffold(
        containerColor = themeColor,
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = textColor) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = textColor, modifier = Modifier.size(24.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = themeColor)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            item {
                SectionHeader("Uygulama Görünümü", subTextColor)
                Spacer(modifier = Modifier.height(10.dp))

                SettingItem(
                    icon = if (isDark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                    title = "Karanlık mod",
                    value = if (isDark) "Karanlık" else "Aydınlık",
                    isDark = isDark,
                    onTap = { com.berat.sakus.ui.theme.ThemeManager.getInstance(context).toggleTheme() }
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingItem(
                    icon = Icons.Outlined.Layers,
                    title = "Harita görünümü",
                    value = "Standart",
                    isDark = isDark
                )
                Spacer(modifier = Modifier.height(24.dp))

                SectionHeader("İzinler", subTextColor)
                Spacer(modifier = Modifier.height(10.dp))

                PermissionCard(
                    icon = Icons.Outlined.LocationOn,
                    title = "Konum izni",
                    description = "Harita, durak, otobüs konum hizmetlerimizi kullanabilmeniz için bu izne ihtiyacımız var.",
                    isGranted = locationGranted,
                    isDark = isDark,
                    onTap = {
                        if (!locationGranted) locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))

                PermissionCard(
                    icon = Icons.Outlined.Notifications,
                    title = "Bildirim izni",
                    description = "Geçiş bilgileri, duyurular, bakiye hatırlatıcı ve daha birçok özelliği kullanabilmeniz için bu izne ihtiyacımız var.",
                    isGranted = notificationGranted,
                    isDark = isDark,
                    onTap = {
                        if (!notificationGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } 
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))

                PermissionCard(
                    icon = Icons.Outlined.CameraAlt,
                    title = "Kamera izni",
                    description = "QR geçiş özelliğini kullanabilmeniz, profil fotoğrafı yükleyebilmeniz ve daha fazlası için bu izne ihtiyacımız var.",
                    isGranted = cameraGranted,
                    isDark = isDark,
                    onTap = {
                        if (!cameraGranted) cameraLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))

                SectionHeader("Diğer", subTextColor)
                Spacer(modifier = Modifier.height(10.dp))

                SettingItem(
                    icon = Icons.Outlined.Language,
                    title = "Uygulama dili",
                    value = "Türkçe",
                    isDark = isDark
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingItem(
                    icon = Icons.Outlined.Hub,
                    title = "Uygulama versiyonu",
                    value = "1.0.0",
                    isDark = isDark,
                    hideArrow = true
                )
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

// Permissions Check Helper method 
private fun checkPermission(context: android.content.Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

// Open App Settings directly if permissions are denied
private fun openAppSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

@Composable
private fun SectionHeader(title: String, color: Color) {
    Text(
        text = title,
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    isDark: Boolean,
    hideArrow: Boolean = false,
    onTap: () -> Unit = {}
) {
    val bgColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val subTextColor = if (isDark) Color.LightGray else Color.DarkGray

    val shadowModifier = if (!isDark) Modifier.shadow(4.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.05f)) else Modifier

    Surface(
        onClick = onTap,
        modifier = Modifier
            .fillMaxWidth()
            .then(shadowModifier),
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, tint = textColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = subTextColor,
            fontSize = 14.sp
        )
        if (!hideArrow) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = subTextColor, modifier = Modifier.size(16.dp))
        }
    }
    }
}


@Composable
private fun PermissionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    isDark: Boolean,
    onTap: () -> Unit
) {
    val bgColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface
    val subTextColor = if (isDark) Color.LightGray else Color.DarkGray

    val modifier = Modifier
        .fillMaxWidth()
        .then(if (!isDark) Modifier.shadow(4.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.05f)) else Modifier)
        .background(bgColor, RoundedCornerShape(12.dp))
        .padding(16.dp)

    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = title, tint = textColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = subTextColor,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        
        val btnBgColor = if (isGranted) {
            if (isDark) Color(0xFF2E2E2E) else Color(0xFFEEEEEE)
        } else {
            if (isDark) Color(0xFF3A3A3A) else MaterialTheme.colorScheme.primary
        }

        val btnBorder = if (isGranted) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
        
        val btnTextColor = if (isGranted) subTextColor else Color.White

        Surface(
            onClick = onTap,
            color = btnBgColor,
            shape = RoundedCornerShape(20.dp),
            border = btnBorder
        ) {
            Text(
                text = if (isGranted) "İzin verildi" else "İzin ver",
                color = btnTextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}
