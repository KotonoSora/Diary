package com.kotonosora.todolist.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.googlefonts.R as GoogleFontsR

/**
 * Google Fonts Provider using Google Play Services.
 * Automatically downloads and caches modern custom typefaces (Inter, Merriweather, Fira Code).
 * Configured with system fallback fonts for offline/pending download robustness.
 */
val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = GoogleFontsR.array.com_google_android_gms_fonts_certs
)

val InterFont = GoogleFont("Inter")
val FiraCodeFont = GoogleFont("Fira Code")
val MerriweatherFont = GoogleFont("Merriweather")

val InterFontFamily = FontFamily(
    Font(googleFont = InterFont, fontProvider = GoogleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = InterFont, fontProvider = GoogleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = InterFont, fontProvider = GoogleFontProvider, weight = FontWeight.Bold)
)

val FiraCodeFontFamily = FontFamily(
    Font(googleFont = FiraCodeFont, fontProvider = GoogleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = FiraCodeFont, fontProvider = GoogleFontProvider, weight = FontWeight.Bold)
)

val MerriweatherFontFamily = FontFamily(
    Font(googleFont = MerriweatherFont, fontProvider = GoogleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = MerriweatherFont, fontProvider = GoogleFontProvider, weight = FontWeight.Bold)
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp // 1.5x line height for optimal note reading ergonomics
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)
