package com.nexussu.manager

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexussu.manager.core.NexusEngine
import com.nexussu.manager.ui.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.supportedModes?.maxByOrNull { it.refreshRate }?.let { maxMode ->
                window.attributes = window.attributes.apply { preferredDisplayModeId = maxMode.modeId }
            }
        }

        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("nexussu_prefs", 0) }
            
            // Load saved theme, default to true (Dark Mode)
            var darkTheme by remember { mutableStateOf(prefs.getBoolean("dark_theme", true)) }
            
            // Load saved accent, default to Nexus
            val savedAccentName = prefs.getString("accent_theme", AccentTheme.Nexus.name)
            var accent by remember { mutableStateOf(AccentTheme.valueOf(savedAccentName ?: AccentTheme.Nexus.name)) }
            
            NexusSUTheme(darkTheme, accent) {
                NexusSUApp(
                    darkTheme = darkTheme,
                    onDarkThemeChange = { 
                        darkTheme = it
                        prefs.edit().putBoolean("dark_theme", it).apply() // Save theme
                    },
                    accent = accent,
                    onAccentChange = { 
                        accent = it
                        prefs.edit().putString("accent_theme", it.name).apply() // Save accent
                    }
                )
            }
        }
    }
}

@Composable
fun NexusSUApp(
    darkTheme: Boolean, onDarkThemeChange: (Boolean) -> Unit,
    accent: AccentTheme, onAccentChange: (AccentTheme) -> Unit
) {
    val p = LocalNexusPalette.current
    val hazeState = remember { HazeState() }
    var tab by remember { mutableStateOf(Tab.Home) }
    var showSettings by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nexussu_prefs", 0) }
    var isSetupComplete by remember { mutableStateOf(prefs.getBoolean("is_su_installed", false)) }

    LaunchedEffect(isSetupComplete) {
        if (!isSetupComplete) {
            withContext(Dispatchers.IO) {
                NexusEngine.installSuBinary(context)
                NexusEngine.applySavedRootGrants()
                prefs.edit().putBoolean("is_su_installed", true).apply()
                isSetupComplete = true
            }
        }
    }

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(p.void, p.voidGradientEnd))).hazeSource(state = hazeState)
            ) { LiquidBlobs() }

            Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(color = p.ink)) { append("Nexus") }
                        withStyle(SpanStyle(color = p.accent)) { append("SU") }
                    }, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(p.glassFill)
                            .clickable(remember { MutableInteractionSource() }, indication = null) { showSettings = true },
                        contentAlignment = Alignment.Center
                    ) { SettingsIcon(tint = p.ink) }
                }

                Box(Modifier.weight(1f)) {
                    if (!isSetupComplete) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Initializing Root Environment...", color = p.accent, fontFamily = MonoFont, fontSize = 14.sp)
                        }
                    } else {
                        AnimatedContent(
                            targetState = if (showSettings) "settings" else tab.name,
                            transitionSpec = { (fadeIn() + slideInVertically { it / 8 }) togetherWith (fadeOut() + slideOutVertically { -it / 8 }) },
                            label = "screen"
                        ) { state ->
                            when (state) {
                                "settings" -> SettingsScreen(darkTheme, onDarkThemeChange, accent, onAccentChange) { showSettings = false }
                                "Home" -> HomeScreen(onOpenAdvanced = { showSettings = true })
                                "Log" -> LogScreen()
                                "Superuser" -> SuperuserScreen()
                                else -> ModuleScreen()
                            }
                        }
                    }
                }

                if (!showSettings && isSetupComplete) {
                    Spacer(Modifier.height(12.dp))
                    LiquidTabBar(selected = tab, onSelect = { tab = it })
                }
            }
        }
    }
}

@Composable
fun LiquidBlobs() {
    val p = LocalNexusPalette.current
    val infinite = rememberInfiniteTransition(label = "blobs")
    val d by infinite.animateFloat(0f, 1f, infiniteRepeatable(tween(17000, easing = LinearEasing), RepeatMode.Reverse), label = "d")
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.graphicsLayer { translationX = (-60 + d * 40).dp.toPx(); translationY = (-60 + d * 30).dp.toPx() }
                .size(300.dp).blur(70.dp).background(p.accent.copy(alpha = 0.5f), CircleShape)
        )
        Box(
            Modifier.align(Alignment.BottomEnd)
                .graphicsLayer { translationX = (60 - d * 30).dp.toPx(); translationY = (60 - d * 40).dp.toPx() }
                .size(260.dp).blur(70.dp).background(p.accent2.copy(alpha = 0.5f), CircleShape)
        )
    }
}
