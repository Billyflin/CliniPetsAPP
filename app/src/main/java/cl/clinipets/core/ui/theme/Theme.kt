// ui/theme/AppTheme.kt
package cl.clinipets.core.ui.theme


import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext


@Immutable
data class ExtendedColorScheme(
    val pink: ColorFamily,
    val mint: ColorFamily,
    val lavander: ColorFamily,
    val peach: ColorFamily,
)

private val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

private val mediumContrastLightColorScheme = lightColorScheme(
    primary = primaryLightMediumContrast,
    onPrimary = onPrimaryLightMediumContrast,
    primaryContainer = primaryContainerLightMediumContrast,
    onPrimaryContainer = onPrimaryContainerLightMediumContrast,
    secondary = secondaryLightMediumContrast,
    onSecondary = onSecondaryLightMediumContrast,
    secondaryContainer = secondaryContainerLightMediumContrast,
    onSecondaryContainer = onSecondaryContainerLightMediumContrast,
    tertiary = tertiaryLightMediumContrast,
    onTertiary = onTertiaryLightMediumContrast,
    tertiaryContainer = tertiaryContainerLightMediumContrast,
    onTertiaryContainer = onTertiaryContainerLightMediumContrast,
    error = errorLightMediumContrast,
    onError = onErrorLightMediumContrast,
    errorContainer = errorContainerLightMediumContrast,
    onErrorContainer = onErrorContainerLightMediumContrast,
    background = backgroundLightMediumContrast,
    onBackground = onBackgroundLightMediumContrast,
    surface = surfaceLightMediumContrast,
    onSurface = onSurfaceLightMediumContrast,
    surfaceVariant = surfaceVariantLightMediumContrast,
    onSurfaceVariant = onSurfaceVariantLightMediumContrast,
    outline = outlineLightMediumContrast,
    outlineVariant = outlineVariantLightMediumContrast,
    scrim = scrimLightMediumContrast,
    inverseSurface = inverseSurfaceLightMediumContrast,
    inverseOnSurface = inverseOnSurfaceLightMediumContrast,
    inversePrimary = inversePrimaryLightMediumContrast,
    surfaceDim = surfaceDimLightMediumContrast,
    surfaceBright = surfaceBrightLightMediumContrast,
    surfaceContainerLowest = surfaceContainerLowestLightMediumContrast,
    surfaceContainerLow = surfaceContainerLowLightMediumContrast,
    surfaceContainer = surfaceContainerLightMediumContrast,
    surfaceContainerHigh = surfaceContainerHighLightMediumContrast,
    surfaceContainerHighest = surfaceContainerHighestLightMediumContrast,
)

private val highContrastLightColorScheme = lightColorScheme(
    primary = primaryLightHighContrast,
    onPrimary = onPrimaryLightHighContrast,
    primaryContainer = primaryContainerLightHighContrast,
    onPrimaryContainer = onPrimaryContainerLightHighContrast,
    secondary = secondaryLightHighContrast,
    onSecondary = onSecondaryLightHighContrast,
    secondaryContainer = secondaryContainerLightHighContrast,
    onSecondaryContainer = onSecondaryContainerLightHighContrast,
    tertiary = tertiaryLightHighContrast,
    onTertiary = onTertiaryLightHighContrast,
    tertiaryContainer = tertiaryContainerLightHighContrast,
    onTertiaryContainer = onTertiaryContainerLightHighContrast,
    error = errorLightHighContrast,
    onError = onErrorLightHighContrast,
    errorContainer = errorContainerLightHighContrast,
    onErrorContainer = onErrorContainerLightHighContrast,
    background = backgroundLightHighContrast,
    onBackground = onBackgroundLightHighContrast,
    surface = surfaceLightHighContrast,
    onSurface = onSurfaceLightHighContrast,
    surfaceVariant = surfaceVariantLightHighContrast,
    onSurfaceVariant = onSurfaceVariantLightHighContrast,
    outline = outlineLightHighContrast,
    outlineVariant = outlineVariantLightHighContrast,
    scrim = scrimLightHighContrast,
    inverseSurface = inverseSurfaceLightHighContrast,
    inverseOnSurface = inverseOnSurfaceLightHighContrast,
    inversePrimary = inversePrimaryLightHighContrast,
    surfaceDim = surfaceDimLightHighContrast,
    surfaceBright = surfaceBrightLightHighContrast,
    surfaceContainerLowest = surfaceContainerLowestLightHighContrast,
    surfaceContainerLow = surfaceContainerLowLightHighContrast,
    surfaceContainer = surfaceContainerLightHighContrast,
    surfaceContainerHigh = surfaceContainerHighLightHighContrast,
    surfaceContainerHighest = surfaceContainerHighestLightHighContrast,
)

private val mediumContrastDarkColorScheme = darkColorScheme(
    primary = primaryDarkMediumContrast,
    onPrimary = onPrimaryDarkMediumContrast,
    primaryContainer = primaryContainerDarkMediumContrast,
    onPrimaryContainer = onPrimaryContainerDarkMediumContrast,
    secondary = secondaryDarkMediumContrast,
    onSecondary = onSecondaryDarkMediumContrast,
    secondaryContainer = secondaryContainerDarkMediumContrast,
    onSecondaryContainer = onSecondaryContainerDarkMediumContrast,
    tertiary = tertiaryDarkMediumContrast,
    onTertiary = onTertiaryDarkMediumContrast,
    tertiaryContainer = tertiaryContainerDarkMediumContrast,
    onTertiaryContainer = onTertiaryContainerDarkMediumContrast,
    error = errorDarkMediumContrast,
    onError = onErrorDarkMediumContrast,
    errorContainer = errorContainerDarkMediumContrast,
    onErrorContainer = onErrorContainerDarkMediumContrast,
    background = backgroundDarkMediumContrast,
    onBackground = onBackgroundDarkMediumContrast,
    surface = surfaceDarkMediumContrast,
    onSurface = onSurfaceDarkMediumContrast,
    surfaceVariant = surfaceVariantDarkMediumContrast,
    onSurfaceVariant = onSurfaceVariantDarkMediumContrast,
    outline = outlineDarkMediumContrast,
    outlineVariant = outlineVariantDarkMediumContrast,
    scrim = scrimDarkMediumContrast,
    inverseSurface = inverseSurfaceDarkMediumContrast,
    inverseOnSurface = inverseOnSurfaceDarkMediumContrast,
    inversePrimary = inversePrimaryDarkMediumContrast,
    surfaceDim = surfaceDimDarkMediumContrast,
    surfaceBright = surfaceBrightDarkMediumContrast,
    surfaceContainerLowest = surfaceContainerLowestDarkMediumContrast,
    surfaceContainerLow = surfaceContainerLowDarkMediumContrast,
    surfaceContainer = surfaceContainerDarkMediumContrast,
    surfaceContainerHigh = surfaceContainerHighDarkMediumContrast,
    surfaceContainerHighest = surfaceContainerHighestDarkMediumContrast,
)

private val highContrastDarkColorScheme = darkColorScheme(
    primary = primaryDarkHighContrast,
    onPrimary = onPrimaryDarkHighContrast,
    primaryContainer = primaryContainerDarkHighContrast,
    onPrimaryContainer = onPrimaryContainerDarkHighContrast,
    secondary = secondaryDarkHighContrast,
    onSecondary = onSecondaryDarkHighContrast,
    secondaryContainer = secondaryContainerDarkHighContrast,
    onSecondaryContainer = onSecondaryContainerDarkHighContrast,
    tertiary = tertiaryDarkHighContrast,
    onTertiary = onTertiaryDarkHighContrast,
    tertiaryContainer = tertiaryContainerDarkHighContrast,
    onTertiaryContainer = onTertiaryContainerDarkHighContrast,
    error = errorDarkHighContrast,
    onError = onErrorDarkHighContrast,
    errorContainer = errorContainerDarkHighContrast,
    onErrorContainer = onErrorContainerDarkHighContrast,
    background = backgroundDarkHighContrast,
    onBackground = onBackgroundDarkHighContrast,
    surface = surfaceDarkHighContrast,
    onSurface = onSurfaceDarkHighContrast,
    surfaceVariant = surfaceVariantDarkHighContrast,
    onSurfaceVariant = onSurfaceVariantDarkHighContrast,
    outline = outlineDarkHighContrast,
    outlineVariant = outlineVariantDarkHighContrast,
    scrim = scrimDarkHighContrast,
    inverseSurface = inverseSurfaceDarkHighContrast,
    inverseOnSurface = inverseOnSurfaceDarkHighContrast,
    inversePrimary = inversePrimaryDarkHighContrast,
    surfaceDim = surfaceDimDarkHighContrast,
    surfaceBright = surfaceBrightDarkHighContrast,
    surfaceContainerLowest = surfaceContainerLowestDarkHighContrast,
    surfaceContainerLow = surfaceContainerLowDarkHighContrast,
    surfaceContainer = surfaceContainerDarkHighContrast,
    surfaceContainerHigh = surfaceContainerHighDarkHighContrast,
    surfaceContainerHighest = surfaceContainerHighestDarkHighContrast,
)

val extendedLight = ExtendedColorScheme(
    pink = ColorFamily(
        pinkLight,
        onPinkLight,
        pinkContainerLight,
        onPinkContainerLight,
    ),
    mint = ColorFamily(
        mintLight,
        onMintLight,
        mintContainerLight,
        onMintContainerLight,
    ),
    lavander = ColorFamily(
        lavanderLight,
        onLavanderLight,
        lavanderContainerLight,
        onLavanderContainerLight,
    ),
    peach = ColorFamily(
        peachLight,
        onPeachLight,
        peachContainerLight,
        onPeachContainerLight,
    ),
)

val extendedDark = ExtendedColorScheme(
    pink = ColorFamily(
        pinkDark,
        onPinkDark,
        pinkContainerDark,
        onPinkContainerDark,
    ),
    mint = ColorFamily(
        mintDark,
        onMintDark,
        mintContainerDark,
        onMintContainerDark,
    ),
    lavander = ColorFamily(
        lavanderDark,
        onLavanderDark,
        lavanderContainerDark,
        onLavanderContainerDark,
    ),
    peach = ColorFamily(
        peachDark,
        onPeachDark,
        peachContainerDark,
        onPeachContainerDark,
    ),
)

val extendedLightMediumContrast = ExtendedColorScheme(
    pink = ColorFamily(
        pinkLightMediumContrast,
        onPinkLightMediumContrast,
        pinkContainerLightMediumContrast,
        onPinkContainerLightMediumContrast,
    ),
    mint = ColorFamily(
        mintLightMediumContrast,
        onMintLightMediumContrast,
        mintContainerLightMediumContrast,
        onMintContainerLightMediumContrast,
    ),
    lavander = ColorFamily(
        lavanderLightMediumContrast,
        onLavanderLightMediumContrast,
        lavanderContainerLightMediumContrast,
        onLavanderContainerLightMediumContrast,
    ),
    peach = ColorFamily(
        peachLightMediumContrast,
        onPeachLightMediumContrast,
        peachContainerLightMediumContrast,
        onPeachContainerLightMediumContrast,
    ),
)

val extendedLightHighContrast = ExtendedColorScheme(
    pink = ColorFamily(
        pinkLightHighContrast,
        onPinkLightHighContrast,
        pinkContainerLightHighContrast,
        onPinkContainerLightHighContrast,
    ),
    mint = ColorFamily(
        mintLightHighContrast,
        onMintLightHighContrast,
        mintContainerLightHighContrast,
        onMintContainerLightHighContrast,
    ),
    lavander = ColorFamily(
        lavanderLightHighContrast,
        onLavanderLightHighContrast,
        lavanderContainerLightHighContrast,
        onLavanderContainerLightHighContrast,
    ),
    peach = ColorFamily(
        peachLightHighContrast,
        onPeachLightHighContrast,
        peachContainerLightHighContrast,
        onPeachContainerLightHighContrast,
    ),
)

val extendedDarkMediumContrast = ExtendedColorScheme(
    pink = ColorFamily(
        pinkDarkMediumContrast,
        onPinkDarkMediumContrast,
        pinkContainerDarkMediumContrast,
        onPinkContainerDarkMediumContrast,
    ),
    mint = ColorFamily(
        mintDarkMediumContrast,
        onMintDarkMediumContrast,
        mintContainerDarkMediumContrast,
        onMintContainerDarkMediumContrast,
    ),
    lavander = ColorFamily(
        lavanderDarkMediumContrast,
        onLavanderDarkMediumContrast,
        lavanderContainerDarkMediumContrast,
        onLavanderContainerDarkMediumContrast,
    ),
    peach = ColorFamily(
        peachDarkMediumContrast,
        onPeachDarkMediumContrast,
        peachContainerDarkMediumContrast,
        onPeachContainerDarkMediumContrast,
    ),
)

val extendedDarkHighContrast = ExtendedColorScheme(
    pink = ColorFamily(
        pinkDarkHighContrast,
        onPinkDarkHighContrast,
        pinkContainerDarkHighContrast,
        onPinkContainerDarkHighContrast,
    ),
    mint = ColorFamily(
        mintDarkHighContrast,
        onMintDarkHighContrast,
        mintContainerDarkHighContrast,
        onMintContainerDarkHighContrast,
    ),
    lavander = ColorFamily(
        lavanderDarkHighContrast,
        onLavanderDarkHighContrast,
        lavanderContainerDarkHighContrast,
        onLavanderContainerDarkHighContrast,
    ),
    peach = ColorFamily(
        peachDarkHighContrast,
        onPeachDarkHighContrast,
        peachContainerDarkHighContrast,
        onPeachContainerDarkHighContrast,
    ),
)

@Immutable
data class ColorFamily(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color
)

val LocalExtendedColors = staticCompositionLocalOf { extendedLight }

/* ---------- CONTRAST SELECTOR ---------- */
enum class Contrast { Standard, Medium, High }

/* ---------- EXPRESSIVE CHECK ---------- */
private val HAS_EXPRESSIVE: Boolean = runCatching {
    Class.forName("androidx.compose.material3.MaterialExpressiveKt")
}.isSuccess

/* ---------- MAIN THEME (refactor) ---------- */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Suppress("FunctionNaming")
@Composable
fun ClinipetsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    contrast: Contrast = Contrast.Standard,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    // 1) Resolver esquema base (estable)
    val baseScheme = remember(darkTheme, contrast) {
        baseSchemeFor(darkTheme, contrast)
    }

    // 2) Resolver esquema final (dinámico solo si aplica)
    val colorScheme = remember(context, darkTheme, contrast, dynamicColor, baseScheme) {
        resolveColorScheme(context, darkTheme, contrast, dynamicColor, baseScheme)
    }

    // 3) Resolver paleta extendida (estable)
    val extendedColors = remember(key1 = darkTheme, key2 = contrast) {
        extendedColorsFor(darkTheme, contrast)
    }

    // 4) Aplicar tema (una sola decisión)
    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        ApplyMaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

/* ---------- Helpers puros (bajan la complejidad del composable) ---------- */

private fun baseSchemeFor(
    darkTheme: Boolean,
    contrast: Contrast,
): ColorScheme = if (darkTheme) {
    when (contrast) {
        Contrast.Standard -> darkScheme
        Contrast.Medium  -> mediumContrastDarkColorScheme
        Contrast.High    -> highContrastDarkColorScheme
    }
} else {
    when (contrast) {
        Contrast.Standard -> lightScheme
        Contrast.Medium  -> mediumContrastLightColorScheme
        Contrast.High    -> highContrastLightColorScheme
    }
}

private fun extendedColorsFor(
    darkTheme: Boolean,
    contrast: Contrast,
): ExtendedColors = if (darkTheme) {
    when (contrast) {
        Contrast.Standard -> extendedDark
        Contrast.Medium  -> extendedDarkMediumContrast
        Contrast.High    -> extendedDarkHighContrast
    }
} else {
    when (contrast) {
        Contrast.Standard -> extendedLight
        Contrast.Medium  -> extendedLightMediumContrast
        Contrast.High    -> extendedLightHighContrast
    }
}
private typealias ExtendedColors = ExtendedColorScheme

private fun isDynamicEligible(
    dynamicColor: Boolean,
    contrast: Contrast,
): Boolean = dynamicColor && contrast == Contrast.Standard

private fun resolveColorScheme(
    context: Context,
    darkTheme: Boolean,
    contrast: Contrast,
    dynamicColor: Boolean,
    fallback: ColorScheme,
): ColorScheme {
    if (!isDynamicEligible(dynamicColor, contrast)) return fallback
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return fallback
    return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

/* ---------- Capa de aplicación del tema (aisla el if) ---------- */

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ApplyMaterialTheme(
    colorScheme: ColorScheme,
    content: @Composable () -> Unit,
) {
    if (HAS_EXPRESSIVE) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            shapes = Shapes(),
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    } else {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography(),
            shapes = Shapes(),
            content = content,
        )
    }
}
