package fr.mastersid.etudiant.template.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary          = Color(0xFFE8500A),
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFFFDBCF),
    secondary        = Color(0xFF775650),
    background       = Color(0xFFFFFBFF),
    surface          = Color(0xFFFFFBFF),
)

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        content     = content
    )
}