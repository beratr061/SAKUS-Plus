package com.berat.sakus.ui.screens

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import com.berat.sakus.data.AracKonumu
import com.berat.sakus.ui.theme.MapDarkBackground
import com.berat.sakus.ui.theme.MapDarkCard

@Composable
fun MapButton(painter: androidx.compose.ui.graphics.painter.Painter, iconTint: Color = Color.White, onTap: () -> Unit) {
    Surface(
        onClick = onTap,
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = MapDarkBackground
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painter, contentDescription = null, tint = iconTint, modifier = Modifier.size(26.dp))
        }
    }
}

class TooltipShape(
    private val cornerRadiusDp: Float = 8f,
    private val arrowWidthDp: Float = 16f,
    private val arrowHeightDp: Float = 8f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val r = cornerRadiusDp * density.density
        val aw = arrowWidthDp * density.density
        val ah = arrowHeightDp * density.density
        val rectHeight = size.height - ah
        
        val path = Path().apply {
            moveTo(0f, r)
            arcTo(Rect(0f, 0f, 2 * r, 2 * r), 180f, 90f, false)
            lineTo(size.width - r, 0f)
            arcTo(Rect(size.width - 2 * r, 0f, size.width, 2 * r), -90f, 90f, false)
            lineTo(size.width, rectHeight - r)
            arcTo(Rect(size.width - 2 * r, rectHeight - 2 * r, size.width, rectHeight), 0f, 90f, false)
            
            lineTo((size.width + aw) / 2f, rectHeight)
            lineTo(size.width / 2f, size.height)
            lineTo((size.width - aw) / 2f, rectHeight)
            
            lineTo(r, rectHeight)
            arcTo(Rect(0f, rectHeight - 2 * r, 2 * r, rectHeight), 90f, 90f, false)
            lineTo(0f, r)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun VehicleTooltip(v: AracKonumu) {
    val tooltipShape = remember { TooltipShape() }
    
    Box(
        modifier = Modifier
            .padding(bottom = 36.dp)
            .shadow(6.dp, tooltipShape)
            .background(MapDarkCard, tooltipShape)
            .border(1.dp, Color.White.copy(alpha = 0.2f), tooltipShape)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .width(150.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = Color.White.copy(alpha=0.7f), modifier = Modifier.size(20.dp))
                Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White.copy(alpha=0.7f), modifier = Modifier.size(20.dp))
                Icon(Icons.Default.Sync, contentDescription = null, tint = Color.White.copy(alpha=0.7f), modifier = Modifier.size(20.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                Text(
                    text = "${v.plaka} - ${if (v.aracNumarasi > 0) v.aracNumarasi.toString() else "Yok"}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(v.hizFormati, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Canlı", color = Color.White.copy(alpha=0.7f), fontSize = 13.sp)
            }
        }
    }
}

object MapAssets {
    private var directionalArrowBitmap: Bitmap? = null

    fun getDirectionalArrowBitmap(): Bitmap {
        directionalArrowBitmap?.let { return it }

        val boxWidth = 100f
        val boxHeight = 250f 
        
        val bitmap = createBitmap(boxWidth.toInt(), boxHeight.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        val arrowPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }
        
        val path = android.graphics.Path().apply {
            moveTo(0f, 87f)
            lineTo(211f, 87f)
            lineTo(211f, 0f)
            lineTo(420f, 121f)
            lineTo(211f, 244f)
            lineTo(211f, 157f)
            lineTo(0f, 157f)
            close()
        }
        
        val matrix = android.graphics.Matrix()
        matrix.postTranslate(-210f, -122f)
        matrix.postRotate(90f)
        val scale = 70f / 244f 
        matrix.postScale(scale, scale)
        matrix.postTranslate(boxWidth / 2f, boxHeight / 2f)
        
        path.transform(matrix)
        canvas.drawPath(path, arrowPaint)
        
        directionalArrowBitmap = bitmap
        return bitmap
    }

    fun getBitmapFromVector(context: Context, vectorResId: Int, targetHeightPx: Int): Bitmap? {
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, vectorResId) ?: return null
        val ratio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight.toFloat()
        val targetWidthPx = (targetHeightPx * ratio).toInt()
        
        drawable.setBounds(0, 0, targetWidthPx, targetHeightPx)
        val bm = createBitmap(targetWidthPx, targetHeightPx, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bm)
        drawable.draw(canvas)
        return bm
    }
}

// Distance Helper
fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371000.0 // meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return earthRadius * c
}
