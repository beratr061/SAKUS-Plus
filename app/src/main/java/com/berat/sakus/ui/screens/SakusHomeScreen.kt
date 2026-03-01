package com.berat.sakus.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.vectorResource
import com.berat.sakus.R
import coil3.compose.AsyncImage
import com.berat.sakus.ui.theme.DrawerBackground
import com.berat.sakus.ui.theme.DrawerDivider
import com.berat.sakus.ui.theme.PrimaryPurple
import kotlinx.coroutines.launch

// Screen width scaling helper simulating Flutter's 's(double val)'
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

@Composable
fun SakusHomeScreen(
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DrawerBackground,
                modifier = Modifier.width(320.dp),
                drawerShape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp)
            ) {
                DrawerContent(
                    onCloseDrawer = { scope.launch { drawerState.close() } },
                    onNavigate = onNavigate
                )
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Top Bar
                TopBar(
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onNavigate = onNavigate
                )
                
                // Main Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    SearchBarWidget(onClick = { onNavigate("transportation") })
                    
                    Kart54Section()
                    
                    QuickActionsRow(onNavigate = onNavigate)
                    
                    BottomGrid(onNavigate = onNavigate)
                }
            }
        }
    }
}

@Composable
private fun TopBar(onOpenDrawer: () -> Unit, onNavigate: (String) -> Unit) {
    val isDarkTheme by com.berat.sakus.ui.theme.ThemeManager.getInstance(LocalContext.current).isDarkTheme.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )
        
        IconButton(
            onClick = onOpenDrawer,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(Icons.Filled.Menu, contentDescription = "Menu", modifier = Modifier.size(30.dp))
        }

        Image(
            painter = painterResource(id = if (isDarkTheme) R.drawable.ic_logo_dark else R.drawable.ic_logo_light),
            contentDescription = "Logo",
            modifier = Modifier
                .align(Alignment.Center)
                .height(52.dp)
        )

        Row(
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            IconButton(onClick = { onNavigate("duyurular") }) {
                Icon(
                    painter = painterResource(R.drawable.ic_notification_bell),
                    contentDescription = "Duyurular",
                    tint = LocalContentColor.current,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchBarWidget(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = "Search",
            tint = LocalContentColor.current,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Hat/Durak Arama",
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = LocalContentColor.current.copy(alpha = 0.54f)
        )
    }
}

@Composable
private fun Kart54Section() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(166.dp)
    ) {
        // Card Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Burada Kart54’ler Sıralanacak",
                fontSize = 12.sp,
                color = LocalContentColor.current.copy(alpha = 0.38f)
            )
        }

        // Ellipse
        AsyncImage(
            model = "file:///android_asset/icons/ellipse_1.svg",
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = -19.dp, y = 100.dp)
                .size(58.dp)
        )

        // QR Icon
        Icon(
            painter = painterResource(R.drawable.ic_qr_code),
            contentDescription = "QR Code",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = -35.dp, y = 116.dp)
                .size(26.dp)
        )
        
        // Label
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = -7.dp, y = 144.dp)
                .size(width = 83.dp, height = 22.dp)
                .background(PrimaryPurple, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "QR ile Öde",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun QuickActionsRow(onNavigate: (String) -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        QuickActionItem("file:///android_asset/icons/card.svg", "Kartlarım")
        QuickActionItem("file:///android_asset/icons/store.svg", "Satış\nNoktaları")
        QuickActionItem("file:///android_asset/icons/wallet.svg", "Bakiye\nİşlemleri")
        QuickActionItem(
            iconPath = "",
            label = "Yakınımdaki\nHatlar",
            useIcon = Icons.Default.NearMe,
            onClick = { onNavigate("yakinimdaki_hatlar") }
        )
    }
}

@Composable
private fun QuickActionItem(
    iconPath: String,
    label: String,
    useIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(7.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(7.dp))
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (useIcon != null) {
                Icon(
                    imageVector = useIcon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                AsyncImage(
                    model = iconPath,
                    contentDescription = label,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            color = LocalContentColor.current,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun BottomGrid(onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MenuCard(R.drawable.ic_schedule, "Hat Hareket Saatleri", modifier = Modifier.weight(1f)) { onNavigate("hat_saatleri") }
            MenuCard(R.drawable.ic_bus, "Ulaşım Araçları", modifier = Modifier.weight(1f)) { onNavigate("transportation") }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MenuCard(R.drawable.ic_bus_stop, "Duraklar", modifier = Modifier.weight(1f)) { onNavigate("duraklar") }
            MenuCard(R.drawable.ic_location_arrow, "Nasıl Giderim ?", modifier = Modifier.weight(1f)) { onNavigate("nasil_giderim") }
        }
    }
}

@Composable
private fun MenuCard(iconResId: Int, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .height(124.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = label,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = label,
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = LocalContentColor.current
        )
    }
}

@Composable
private fun DrawerContent(onCloseDrawer: () -> Unit, onNavigate: (String) -> Unit) {
    val bgColor = DrawerBackground
    val dividerColor = DrawerDivider

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Header - logo centered
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_sbbyatay),
                contentDescription = "SBB Logo",
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(horizontal = 24.dp)
            )
        }

        HorizontalDivider(color = dividerColor, modifier = Modifier.fillMaxWidth())

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 16.dp)
        ) {
            item { 
                DrawerItem(
                    icon = { Icon(androidx.compose.ui.graphics.vector.ImageVector.vectorResource(id = R.drawable.ic_notification_bell), contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(24.dp)) }, 
                    title = "Duyurular", 
                    onClick = { onCloseDrawer(); onNavigate("duyurular") }
                ) 
            }
            item { 
                DrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.Article, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(24.dp)) }, 
                    title = "Haberler", 
                    onClick = { onCloseDrawer(); onNavigate("haberler") }
                ) 
            }
            item { 
                DrawerItem(
                    icon = { Icon(Icons.Default.Menu, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(24.dp)) }, 
                    title = "Kayıp Eşya Bildirimi", 
                    onClick = { onCloseDrawer(); onNavigate("kayip_esya") }
                ) 
            }
            item { 
                DrawerItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(24.dp)) }, 
                    title = "Araç Sorgula", 
                    onClick = { onCloseDrawer(); onNavigate("arac_sorgu") }
                ) 
            }
            item { 
                DrawerItem(
                    icon = { Icon(Icons.Default.Email, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(24.dp)) }, 
                    title = "İletişim ve Geri Bildirim", 
                    onClick = { onCloseDrawer(); onNavigate("iletisim") }
                ) 
            }
            item { 
                DrawerItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(24.dp)) }, 
                    title = "Favoriler", 
                    onClick = { onCloseDrawer(); onNavigate("favoriler") }
                ) 
            }
            item { 
                DrawerItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(24.dp)) }, 
                    title = "Sık Sorulan Sorular", 
                    onClick = { onCloseDrawer(); onNavigate("sss") }
                ) 
            }
            item { 
                DrawerItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(24.dp)) }, 
                    title = "Hakkımızda", 
                    onClick = { onCloseDrawer(); onNavigate("hakkimizda") }
                ) 
            }
            item { 
                DrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(24.dp)) }, 
                    title = "Ayarlar", 
                    onClick = { onCloseDrawer(); onNavigate("ayarlar") }
                ) 
            }
        }
    }
}

@Composable
private fun DrawerItem(icon: @Composable () -> Unit, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(24.dp))
        Text(
            text = title,
            color = androidx.compose.ui.graphics.Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
