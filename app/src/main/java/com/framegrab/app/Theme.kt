package com.framegrab.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Everything about the look lives in this one file.
 * Inspired by the "Clean Classic" theme: charcoal background, raised metallic-bevel tiles,
 * glossy white pill fields and flat colored action bars.
 * Change a color or shape here and the whole app follows.
 */
object FG {
    // Backgrounds
    val Bg = Color(0xFF2B2B2B)
    val BgDeep = Color(0xFF1E1E1E)

    // Raised tile (like the icons on the home screen)
    val TileTop = Color(0xFF4D4D5A)
    val TileBottom = Color(0xFF2C2C36)
    val BevelLight = Color(0xFFA3A3A3)
    val BevelDark = Color(0xFF6B6B6B)

    // Text
    val Text = Color(0xFFE9E9E9)
    val TextDim = Color(0xFFA6A6A6)
    val TextOnBar = Color(0xFF0E2020)

    // Accent bars (lock-screen colors)
    val Teal = Color(0xFF2DB5B5)
    val Red = Color(0xFFB8403A)
    val Olive = Color(0xFFB5C46A)
    val Green = Color(0xFF8CC96A)
    val Track = Color(0xFF444444)

    // Brushes
    val Tile = Brush.verticalGradient(listOf(TileTop, TileBottom))
    val Bevel = Brush.verticalGradient(listOf(BevelLight, BevelDark))
    val Pill = Brush.horizontalGradient(listOf(Color(0xFFF4F4F4), Color(0xFFCFCFCF)))
    val ScreenBg = Brush.verticalGradient(listOf(Bg, BgDeep))

    // Shapes
    val TileShape = RoundedCornerShape(14.dp)
    val InnerTileShape = RoundedCornerShape(12.dp)
    val PillShape = RoundedCornerShape(50)
    val BarShape = RoundedCornerShape(8.dp)
}

@Composable
fun FGTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = FG.Teal,
            background = FG.Bg,
            surface = FG.TileBottom,
            onSurface = FG.Text,
            onBackground = FG.Text,
        ),
        content = content,
    )
}
