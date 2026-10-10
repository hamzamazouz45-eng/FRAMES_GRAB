package com.framegrab.app

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Big raised tile with an icon, a title and a dim subtitle (the "app icon" look). */
@Composable
fun TileCard(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(FG.TileShape)
            .background(FG.Bevel)
            .padding(2.dp)
            .clip(FG.InnerTileShape)
            .background(FG.Tile)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(FG.Bevel)
                .padding(2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(FG.Tile),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = FG.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = FG.TextDim,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Flat colored action bar (like the colored bars on the lock screen). */
@Composable
fun BarButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = FG.TextOnBar,
    icon: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(FG.BarShape)
            .background(if (enabled) color else FG.Track)
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text,
            color = if (enabled) textColor else FG.TextDim,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Glossy white pill number field (like the search bar). */
@Composable
fun PillField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(modifier) {
        Text(
            label,
            color = FG.TextDim,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 6.dp, end = 6.dp, bottom = 4.dp),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(FG.PillShape)
                .background(FG.Pill)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = value,
                onValueChange = { onChange(it.filter(Char::isDigit).take(4)) },
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(
                    color = Color(0xFF222222),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(FG.Teal),
            )
        }
    }
}

/** Small language switch chip in the header. */
@Composable
fun LangChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(52.dp, 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(FG.Bevel)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(FG.Tile)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = FG.Text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

/** Frame preview: the dimmed top part is discarded, the outlined bottom part is kept. */
@Composable
fun CropPreview(bmp: Bitmap, cropPct: Int) {
    val ratio = bmp.width.toFloat() / bmp.height.toFloat()
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(12.dp))
            .background(FG.BgDeep),
    ) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        Canvas(Modifier.fillMaxSize()) {
            val cut = if (cropPct > 0) size.height * (1f - cropPct / 100f) else 0f
            if (cut > 0f) {
                drawRect(Color(0xB3000000), Offset(0f, 0f), Size(size.width, cut))
            }
            drawRect(
                FG.Teal,
                Offset(0f, cut),
                Size(size.width, size.height - cut),
                style = Stroke(width = 6f),
            )
        }
    }
}

// ---- Tiny canvas icons (no icon library needed) ----

@Composable
fun IconFilm(color: Color = FG.Text) {
    Canvas(Modifier.size(26.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color,
            Offset(w * 0.1f, h * 0.15f),
            Size(w * 0.8f, h * 0.7f),
            CornerRadius(w * 0.1f),
            style = Stroke(width = w * 0.08f),
        )
        for (i in 0..2) {
            val y = h * (0.28f + i * 0.2f)
            drawRect(color, Offset(w * 0.16f, y), Size(w * 0.08f, h * 0.08f))
            drawRect(color, Offset(w * 0.76f, y), Size(w * 0.08f, h * 0.08f))
        }
        drawRect(color, Offset(w * 0.34f, h * 0.32f), Size(w * 0.32f, h * 0.36f))
    }
}

@Composable
fun IconFolder(color: Color = FG.Text) {
    Canvas(Modifier.size(26.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color,
            Offset(w * 0.08f, h * 0.18f),
            Size(w * 0.38f, h * 0.2f),
            CornerRadius(w * 0.06f),
        )
        drawRoundRect(
            color,
            Offset(w * 0.08f, h * 0.28f),
            Size(w * 0.84f, h * 0.56f),
            CornerRadius(w * 0.08f),
        )
    }
}

@Composable
fun IconPlay(color: Color = FG.TextOnBar) {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(w * 0.25f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.5f)
            lineTo(w * 0.25f, h * 0.85f)
            close()
        }
        drawPath(p, color)
    }
}

@Composable
fun IconStop(color: Color = Color.White) {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color,
            Offset(w * 0.2f, h * 0.2f),
            Size(w * 0.6f, h * 0.6f),
            CornerRadius(w * 0.08f),
        )
    }
}
