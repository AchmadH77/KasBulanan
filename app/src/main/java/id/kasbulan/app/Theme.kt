package id.kasbulan.app

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Tema "Hijau Farmasi" KasBulanan: hijau zamrud gelap untuk mode gelap,
// hijau bersih di atas latar terang untuk mode terang. Skema warna ditulis
// tangan (bukan skema dinamis) supaya identitasnya tetap sama di semua HP.

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5ED4A0),
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF103A28),
    onPrimaryContainer = Color(0xFFB0E7CB),
    secondary = Color(0xFFA9CDBB),
    onSecondary = Color(0xFF14352A),
    secondaryContainer = Color(0xFF2A4A3A),
    onSecondaryContainer = Color(0xFFC5E8D4),
    tertiary = Color(0xFFF2B45C),
    onTertiary = Color(0xFF4A2E00),
    tertiaryContainer = Color(0xFF5C3D10),
    onTertiaryContainer = Color(0xFFFFDDA8),
    error = Color(0xFFFFB4A6),
    onError = Color(0xFF5F1410),
    errorContainer = Color(0xFF5C2018),
    onErrorContainer = Color(0xFFFFDAD2),
    background = Color(0xFF080E0B),
    onBackground = Color(0xFFEAF3EE),
    surface = Color(0xFF111A15),
    onSurface = Color(0xFFEAF3EE),
    surfaceVariant = Color(0xFF243529),
    onSurfaceVariant = Color(0xFF9EB2A6),
    surfaceContainerLowest = Color(0xFF050907),
    surfaceContainerLow = Color(0xFF0D130F),
    surfaceContainer = Color(0xFF18241D),
    surfaceContainerHigh = Color(0xFF1E2D24),
    surfaceContainerHighest = Color(0xFF243529),
    outline = Color(0xFF5F7267),
    outlineVariant = Color(0xFF34443A)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0E7A46),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF0E5C3A),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF3E6B52),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDEEDD),
    onSecondaryContainer = Color(0xFF0E3524),
    tertiary = Color(0xFF8A5A17),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF3E3C2),
    onTertiaryContainer = Color(0xFF4A2E00),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF5F1410),
    background = Color(0xFFF2F7F3),
    onBackground = Color(0xFF14231A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14231A),
    surfaceVariant = Color(0xFFD9E6DC),
    onSurfaceVariant = Color(0xFF42564A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEDF4EE),
    surfaceContainer = Color(0xFFE4EEE6),
    surfaceContainerHigh = Color(0xFFD9E6DC),
    surfaceContainerHighest = Color(0xFFCEDDD2),
    outline = Color(0xFF6B7F72),
    outlineVariant = Color(0xFFC4D4C8)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KasBulananTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}
