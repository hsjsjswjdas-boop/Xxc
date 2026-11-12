package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SheshBachaColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = BattleDarkNavy,
    primaryContainer = BattlePanelNavy,
    onPrimaryContainer = AmberSecondary,
    secondary = CyanAccent,
    onSecondary = BattleDarkNavy,
    background = BattleDarkNavy,
    onBackground = BattleTextCream,
    surface = BattlePanelNavy,
    onSurface = BattleTextCream,
    error = RedDanger,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SheshBachaColorScheme,
        typography = Typography,
        content = content
    )
}
