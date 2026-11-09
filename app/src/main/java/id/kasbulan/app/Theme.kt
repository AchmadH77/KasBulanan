package id.kasbulan.app

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Tema "Arang Emas" KasBulanan (W6): monokrom arang dengan aksen emas.
// Mode gelap: latar arang pekat, wadah arang muda, emas sebagai warna utama.
// Mode terang: latar krem pucat dengan permukaan putih; wadah utama tetap
// gelap (arang zaitun) supaya teks putih di hero dan pita header tetap
// terbaca, sama seperti pola hero di aplikasi Kasir. Skema warna ditulis
// tangan (bukan skema dinamis) supaya identitasnya sama di semua HP.

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD4AF37),
    onPrimary = Color(0xFF1C1603),
    primaryContainer = Color(0xFF28282C),
    onPrimaryContainer = Color(0xFFF2EEE3),
    secondary = Color(0xFFB9B3A4),
    onSecondary = Color(0xFF23211B),
    secondaryContainer = Color(0xFF33302A),
    onSecondaryContainer = Color(0xFFE5DFD2),
    tertiary = Color(0xFF9DB4D0),
    onTertiary = Color(0xFF14202E),
    tertiaryContainer = Color(0xFF26303D),
    onTertiaryContainer = Color(0xFFD3E0F2),
    error = Color(0xFFFFB4A6),
    onError = Color(0xFF5F1410),
    errorContainer = Color(0xFF5C2018),
    onErrorContainer = Color(0xFFFFDAD2),
    background = Color(0xFF0E0E10),
    onBackground = Color(0xFFECE9E2),
    surface = Color(0xFF1B1B1E),
    onSurface = Color(0xFFECE9E2),
    surfaceVariant = Color(0xFF2A2A2E),
    onSurfaceVariant = Color(0xFFA09E96),
    surfaceContainerLowest = Color(0xFF0A0A0B),
    surfaceContainerLow = Color(0xFF141416),
    surfaceContainer = Color(0xFF1E1E21),
    surfaceContainerHigh = Color(0xFF26262A),
    surfaceContainerHighest = Color(0xFF313136),
    outline = Color(0xFF55534C),
    outlineVariant = Color(0xFF33322E)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF96761C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF4A3F28),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF6B675C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8E4D8),
    onSecondaryContainer = Color(0xFF2E2A20),
    tertiary = Color(0xFF5B6B82),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDCE4EF),
    onTertiaryContainer = Color(0xFF1E2A3A),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF5F1410),
    background = Color(0xFFF7F6F2),
    onBackground = Color(0xFF1C1B17),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B17),
    surfaceVariant = Color(0xFFE3DFD2),
    onSurfaceVariant = Color(0xFF5C594F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1EFE8),
    surfaceContainer = Color(0xFFEBE8DF),
    surfaceContainerHigh = Color(0xFFE3DFD2),
    surfaceContainerHighest = Color(0xFFDAD4C2),
    outline = Color(0xFF8A8578),
    outlineVariant = Color(0xFFD8D3C6)
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
