package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ShareDabba Brand Palette: Pitch Black Background, Crisp White Typography, Cyan-to-Purple Accents
val ShareDabbaBlack = Color(0xFF050508)
val ShareDabbaSurface = Color(0xFF0E0E17)
val ShareDabbaSurfaceVariant = Color(0xFF171726)
val ShareDabbaCardElevated = Color(0xFF1E1E30)

val ShareDabbaCyan = Color(0xFF00F0FF)
val ShareDabbaCyanDim = Color(0xFF00B4D8)
val ShareDabbaPurple = Color(0xFF9D4EDD)
val ShareDabbaPurpleDeep = Color(0xFF5A189A)
val ShareDabbaMagenta = Color(0xFFC77DFF)

val ShareDabbaWhite = Color(0xFFFFFFFF)
val ShareDabbaTextSecondary = Color(0xFFB8C1EC)
val ShareDabbaTextMuted = Color(0xFF7F86A6)
val ShareDabbaOutline = Color(0xFF282B42)

val ShareDabbaSuccess = Color(0xFF00E676)
val ShareDabbaWarning = Color(0xFFFFB703)
val ShareDabbaError = Color(0xFFFF4D6D)

val ShareDabbaAccentGradient = Brush.linearGradient(
    colors = listOf(ShareDabbaCyan, ShareDabbaPurple)
)

val ShareDabbaSubtleCardGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF13182B),
        Color(0xFF1A1128)
    )
)
