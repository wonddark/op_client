package com.opclient.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.opclient.resources.Res
import com.opclient.resources.JosefinSlab
import com.opclient.resources.MavenPro
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
fun josefinSlabFamily(): FontFamily {
    val light  = Font(resource = Res.font.JosefinSlab, weight = FontWeight.Light)
    val medium = Font(resource = Res.font.JosefinSlab, weight = FontWeight.Medium)
    return remember(light, medium) { FontFamily(light, medium) }
}

@Composable
fun mavenProFamily(): FontFamily {
    val regular = Font(resource = Res.font.MavenPro, weight = FontWeight.Normal)
    val medium  = Font(resource = Res.font.MavenPro, weight = FontWeight.Medium)
    return remember(regular, medium) { FontFamily(regular, medium) }
}

fun buildTypography(headingFamily: FontFamily, bodyFamily: FontFamily) = AppTypography(
    screenTitle  = TextStyle(fontFamily = headingFamily, fontWeight = FontWeight.Light,  fontSize = 16.sp, letterSpacing = 3.sp),
    sectionLabel = TextStyle(fontFamily = headingFamily, fontWeight = FontWeight.Light,  fontSize = 11.sp, letterSpacing = 2.5.sp),
    bookTitle    = TextStyle(fontFamily = headingFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.3.sp),
    navLabel     = TextStyle(fontFamily = bodyFamily,    fontWeight = FontWeight.Normal, fontSize = 10.sp, letterSpacing = 1.5.sp),
    bookAuthor   = TextStyle(fontFamily = bodyFamily,    fontWeight = FontWeight.Normal, fontSize = 12.sp, letterSpacing = 2.sp),
    tag          = TextStyle(fontFamily = bodyFamily,    fontWeight = FontWeight.Normal, fontSize = 11.sp, letterSpacing = 1.sp),
    body         = TextStyle(fontFamily = bodyFamily,    fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.sp),
    button       = TextStyle(fontFamily = bodyFamily,    fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 2.sp),
)

val LocalAppTypography = staticCompositionLocalOf<AppTypography> {
    error("No AppTypography provided — wrap content in AppTheme")
}
