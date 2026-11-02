package com.quotely.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val EsquemaClaro = lightColorScheme(
    primary = Cobalto,
    onPrimary = PapelElevado,
    primaryContainer = CobaltoSuave,
    onPrimaryContainer = CobaltoOscuro,
    secondary = Senal,
    onSecondary = PapelElevado,
    secondaryContainer = SenalSuave,
    onSecondaryContainer = ColorToken.SenalOscuro,
    tertiary = Exito,
    onTertiary = PapelElevado,
    tertiaryContainer = ExitoSuave,
    onTertiaryContainer = ColorToken.ExitoOscuro,
    background = Papel,
    onBackground = Tinta,
    surface = PapelElevado,
    onSurface = Tinta,
    surfaceVariant = ColorToken.SuperficieVariante,
    onSurfaceVariant = TintaSuave,
    outline = Linea,
    outlineVariant = ColorToken.LineaSuave,
    error = Rojo,
    onError = PapelElevado,
    errorContainer = RojoSuave,
    onErrorContainer = ColorToken.RojoOscuro
)

private val EsquemaOscuro = darkColorScheme(
    primary = CobaltoClaro,
    onPrimary = ColorToken.AzulMuyOscuro,
    primaryContainer = ColorToken.CobaltoProfundo,
    onPrimaryContainer = ColorToken.CobaltoSuaveOscuro,
    secondary = SenalClara,
    onSecondary = ColorToken.NaranjaMuyOscuro,
    secondaryContainer = ColorToken.SenalProfundo,
    onSecondaryContainer = ColorToken.SenalSuaveOscuro,
    tertiary = ExitoClaro,
    onTertiary = ColorToken.VerdeMuyOscuro,
    tertiaryContainer = ColorToken.ExitoProfundo,
    onTertiaryContainer = ColorToken.ExitoSuaveOscuro,
    background = FondoOscuro,
    onBackground = TextoOscuro,
    surface = SuperficieOscura,
    onSurface = TextoOscuro,
    surfaceVariant = SuperficieOscuraAlta,
    onSurfaceVariant = TextoOscuroSuave,
    outline = LineaOscura,
    outlineVariant = ColorToken.LineaOscuraSuave,
    error = RojoClaro,
    onError = ColorToken.RojoMuyOscuro,
    errorContainer = ColorToken.RojoProfundo,
    onErrorContainer = ColorToken.RojoSuaveOscuro
)

// Tonos auxiliares que Material 3 no expone directamente en todas las versiones.
private object ColorToken {
    val SenalOscuro = androidx.compose.ui.graphics.Color(0xFF8A2F00)
    val ExitoOscuro = androidx.compose.ui.graphics.Color(0xFF0C5438)
    val SuperficieVariante = androidx.compose.ui.graphics.Color(0xFFE8EBE3)
    val LineaSuave = androidx.compose.ui.graphics.Color(0xFFE4E7DF)
    val RojoOscuro = androidx.compose.ui.graphics.Color(0xFF6E1712)
    val AzulMuyOscuro = androidx.compose.ui.graphics.Color(0xFF07102A)
    val CobaltoProfundo = androidx.compose.ui.graphics.Color(0xFF2A3F9E)
    val CobaltoSuaveOscuro = androidx.compose.ui.graphics.Color(0xFFDDE4FF)
    val NaranjaMuyOscuro = androidx.compose.ui.graphics.Color(0xFF2E1104)
    val SenalProfundo = androidx.compose.ui.graphics.Color(0xFF6B2B07)
    val SenalSuaveOscuro = androidx.compose.ui.graphics.Color(0xFFFFD9C4)
    val VerdeMuyOscuro = androidx.compose.ui.graphics.Color(0xFF062D1E)
    val ExitoProfundo = androidx.compose.ui.graphics.Color(0xFF0B4A33)
    val ExitoSuaveOscuro = androidx.compose.ui.graphics.Color(0xFFC8F2DF)
    val LineaOscuraSuave = androidx.compose.ui.graphics.Color(0xFF263448)
    val RojoMuyOscuro = androidx.compose.ui.graphics.Color(0xFF3B0A06)
    val RojoProfundo = androidx.compose.ui.graphics.Color(0xFF5C1510)
    val RojoSuaveOscuro = androidx.compose.ui.graphics.Color(0xFFFFDAD5)
}

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun QuotelyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controlador = WindowCompat.getInsetsController(window, view)
            controlador.isAppearanceLightStatusBars = !darkTheme
            controlador.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
        typography = QuotelyTypography,
        shapes = Formas,
        content = content
    )
}
