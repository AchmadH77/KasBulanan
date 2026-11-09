package id.kasbulan.app

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Tema "Biru Malam" KasBulanan (R2): navy pekat dengan aksen cyan elektrik.
// Mode gelap: latar navy sangat gelap, wadah navy, cyan sebagai warna utama.
// Mode terang: latar biru pucat dengan permukaan putih dan biru azure;
// wadah utama tetap navy gelap supaya teks putih di hero dan pita header
// tetap terbaca, sama seperti pola hero di aplikasi Kasir. Skema warna
// ditulis tangan (bukan skema dinamis) supaya identitasnya sama di semua HP.

private val DarkColors = darkColorScheme(
    primary = Color(0xFF40C4FF),
    onPrimary = Color(0xFF041828),
    primaryContainer = Color(0xFF102A56),
    onPrimaryContainer = Color(0xFFE8F0FA),
    secondary = Color(0xFF94A8C6),
    onSecondary = Color(0xFF0D1B2E),
    secondaryContainer = Color(0xFF1B3358),
    onSecondaryContainer = Color(0xFFD6E4F5),
    tertiary = Color(0xFF69DCB4),
    onTertiary = Color(0xFF06382A),
    tertiaryContainer = Color(0xFF14503C),
    onTertiaryContainer = Color(0xFFC8F2E2),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF5F1410),
    errorContainer = Color(0xFF5C2018),
    onErrorContainer = Color(0xFFFFDAD2),
    background = Color(0xFF060C1A),
    onBackground = Color(0xFFE8F0FA),
    surface = Color(0xFF0D182E),
    onSurface = Color(0xFFE8F0FA),
    surfaceVariant = Color(0xFF1B3358),
    onSurfaceVariant = Color(0xFF94A8C6),
    surfaceContainerLowest = Color(0xFF040910),
    surfaceContainerLow = Color(0xFF0A1424),
    surfaceContainer = Color(0xFF101E38),
    surfaceContainerHigh = Color(0xFF16294A),
    surfaceContainerHighest = Color(0xFF1D3560),
    outline = Color(0xFF3D587F),
    outlineVariant = Color(0xFF22375A)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF102A56),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF4A6484),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD8E4F2),
    onSecondaryContainer = Color(0xFF10233E),
    tertiary = Color(0xFF0E7A5F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCEEE2),
    onTertiaryContainer = Color(0xFF06382A),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF5F1410),
    background = Color(0xFFF2F6FB),
    onBackground = Color(0xFF0A1830),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A1830),
    surfaceVariant = Color(0xFFD8E4F2),
    onSurfaceVariant = Color(0xFF42597A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEDF3FA),
    surfaceContainer = Color(0xFFE2EBF6),
    surfaceContainerHigh = Color(0xFFD5E2F0),
    surfaceContainerHighest = Color(0xFFC6D6EA),
    outline = Color(0xFF6B829E),
    outlineVariant = Color(0xFFC4D4E8)
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
