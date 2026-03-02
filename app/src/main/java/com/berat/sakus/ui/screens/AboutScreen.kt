package com.berat.sakus.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.berat.sakus.R
import com.berat.sakus.ui.theme.ThemeManager
import java.util.Calendar
import com.berat.sakus.ui.theme.PrimaryPurple

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
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTheme by ThemeManager.getInstance(context).isDarkTheme.collectAsState()
    
    val scaffoldBgColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val primaryColor = MaterialTheme.colorScheme.primary
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    // Background Gradient (Subtle)
    val gradientColors = listOf(scaffoldBgColor, cardColor)

    Scaffold(
        containerColor = Color.Transparent, // Let the Box behind provide the background
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth().padding(end = 48.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Hakkımızda", 
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = gradientColors))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                
                Spacer(modifier = Modifier.height(20.dp))

                // Logo Container with soft glow
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(140.dp) // roughly 80 + 30 + 30 padding equivalent
                            .shadow(
                                elevation = 20.dp, 
                                shape = CircleShape, 
                                spotColor = primaryColor, 
                                ambientColor = primaryColor
                            )
                            .background(cardColor, CircleShape)
                            .padding(30.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_bus),
                            contentDescription = "Logo",
                            tint = if (isDarkTheme) Color.White else PrimaryPurple,
                            modifier = Modifier.size(80.dp).align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = "Otobüs Takip",
                    color = textColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = primaryColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "v1.0.0",
                        color = Color(0xFFBD8AFF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = "Otobüs Takip Uygulaması ile şehir içi yolculuklarınız artık daha kolay ve planlı.",
                    textAlign = TextAlign.Center,
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else textColor.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    lineHeight = 25.sp // rough equivalent of 1.6 height
                )

                Spacer(modifier = Modifier.height(60.dp))

                InfoRow(icon = Icons.Outlined.Email, text = "byayla82@gmail.com")

                Spacer(modifier = Modifier.weight(1f))

                val year = Calendar.getInstance().get(Calendar.YEAR)
                Text(
                    text = "© $year Berat Yayla",
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.38f) else Color.Black.copy(alpha = 0.38f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    val cardColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    Surface(
        color = cardColor,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, dividerColor)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
