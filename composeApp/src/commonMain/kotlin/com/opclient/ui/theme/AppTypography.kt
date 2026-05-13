package com.opclient.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.opclient.resources.Res
import com.opclient.resources.Jost_Light
import com.opclient.resources.Jost_Medium
import com.opclient.resources.Jost_Regular
import org.jetbrains.compose.resources.Font

data class AppTypography(
    val screenTitle: TextStyle,
    val sectionLabel: TextStyle,
    val navLabel: TextStyle,
    val bookTitle: TextStyle,
    val bookAuthor: TextStyle,
    val tag: TextStyle,
    val body: TextStyle,
    val button: TextStyle,
)

@Composable
fun jostFontFamily(): FontFamily = FontFamily(
    Font(Res.font.Jost_Light,   weight = FontWeight.Light),
    Font(Res.font.Jost_Regular, weight = FontWeight.Normal),
    Font(Res.font.Jost_Medium,  weight = FontWeight.Medium),
)

fun buildTypography(fontFamily: FontFamily) = AppTypography(
    screenTitle  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Light,  fontSize = 14.sp, letterSpacing = 3.sp),
    sectionLabel = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Light,  fontSize = 9.sp,  letterSpacing = 2.5.sp),
    navLabel     = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Light,  fontSize = 8.sp,  letterSpacing = 1.5.sp),
    bookTitle    = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.3.sp),
    bookAuthor   = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Light,  fontSize = 10.sp, letterSpacing = 2.sp),
    tag          = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 9.sp,  letterSpacing = 1.sp),
    body         = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Light,  fontSize = 12.sp, letterSpacing = 0.sp),
    button       = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 9.sp,  letterSpacing = 2.sp),
)

val LocalAppTypography = staticCompositionLocalOf<AppTypography> {
    error("No AppTypography provided — wrap content in AppTheme")
}
