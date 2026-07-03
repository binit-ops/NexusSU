package com.nexussu.manager.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- Home ----------
@Composable
fun HomeScreen(onOpenAdvanced: () -> Unit) {
    val p = LocalNexusPalette.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            RootLens()
            Spacer(Modifier.height(12.dp))
            Text("3 apps granted · 3 modules active", color = p.dim, fontSize = 11.sp, fontFamily = MonoFont)
        }
        DeviceCard(onOpenAdvanced = onOpenAdvanced)
    }
}

@Composable
fun DeviceCard(modifier: Modifier = Modifier, onOpenAdvanced: () -> Unit) {
    val p = LocalNexusPalette.current
    var expanded by remember { mutableStateOf(false) }
    GlassCard(
        modifier.fillMaxWidth()
            .clickable(remember { MutableInteractionSource() }, indication = null) { expanded = !expanded }
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("RMX2151 · Android 15", color = p.ink, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                    Text("kernel 5.4.x-nexus", color = p.dim, fontSize = 10.5.sp, fontFamily = MonoFont)
                }
                val rotation by animateFloatAsState(if (expanded) 90f else 0f, label = "chevron")
                Box(Modifier.rotate(rotation)) { ChevronIcon(tint = p.dim) }
            }
            AnimatedVisibility(expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column(Modifier.padding(top = 12.dp)) {
                    KeyValueRow("SUSFS", "v1.5.2")
                    KeyValueRow("SELinux", "Enforcing")
                    KeyValueRow("Verified boot", "green")
                    TextButton(onClick = onOpenAdvanced, contentPadding = PaddingValues(vertical = 8.dp)) {
                        Text("Advanced options →", color = p.accent, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun KeyValueRow(key: String, value: String) {
    val p = LocalNexusPalette.current
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, color = p.dim, fontSize = 12.sp)
        Text(value, color = p.ink, fontSize = 12.sp, fontFamily = MonoFont)
    }
}

@Composable
fun SectionLabel(text: String) {
    val p = LocalNexusPalette.current
    Text(text.uppercase(), color = p.dim, fontSize = 11.sp, letterSpacing = 1.5.sp, fontFamily = MonoFont)
}

// ---------- Superuser ----------
data class GrantedApp(val initial: String, val name: String, val sub: String, val chip: String, val chipOn: Boolean, val toggledOn: Boolean)

@Composable
fun SuperuserScreen() {
    val p = LocalNexusPalette.current
    var scope by remember { mutableStateOf(1) }
    val apps = remember {
        mutableStateListOf(
            GrantedApp("C", "Camera Tuner", "FULL root", "10m left", true, true),
            GrantedApp("A", "Ad Auditor", "NET", "Always", true, true),
            GrantedApp("T", "Terminal", "FULL root", "Always", true, true),
            GrantedApp("R", "Root Explorer", "FS", "Once", true, true),
            GrantedApp("B", "Banking+", "—", "Denied", false, false)
        )
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Superuser", color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        GlassSegmented(listOf("Minimal", "Standard", "Full"), scope) { scope = it }
        GlassCard {
            Column {
                apps.forEachIndexed { index, app ->
                    AppRow(app) { checked -> apps[index] = app.copy(toggledOn = checked) }
                    if (index < apps.lastIndex) Divider(color = p.glassEdge, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun AppRow(app: GrantedApp, onToggle: (Boolean) -> Unit) {
    val p = LocalNexusPalette.current
    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val bg = if (app.chipOn) Brush.linearGradient(listOf(p.accent, p.accent2)) else Brush.linearGradient(listOf(Color(0xFF3A4050), Color(0xFF20232C)))
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
            Text(app.initial, color = if (app.chipOn) Color(0xFF0A0E14) else p.dim, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.weight(1f)) {
            Text(app.name, color = p.ink, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            Text(app.sub, color = p.dim, fontSize = 10.5.sp, fontFamily = MonoFont)
        }
        Text(
            app.chip, color = if (app.chipOn) p.accent else p.dim.copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = MonoFont,
            modifier = Modifier.border(1.dp, if (app.chipOn) p.accent else p.glassEdge, RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
        )
        GlassToggle(app.toggledOn, onToggle)
    }
}

// ---------- Module ----------
data class ModuleItem(val initial: String, val name: String, val desc: String, val enabled: Boolean)

@Composable
fun ModuleScreen() {
    val p = LocalNexusPalette.current
    val modules = remember {
        mutableStateListOf(
            ModuleItem("S", "SUSFS Hide", "Kernel-level artifact hiding · v1.5.2", true),
            ModuleItem("I", "Integrity Helper", "Passes basic attestation · v3.1", true),
            ModuleItem("B", "Busybox", "Shell utilities · v1.36", true)
        )
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Modules", color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        GlassCard {
            Column {
                modules.forEachIndexed { i, m ->
                    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(p.accent, p.accent2))), contentAlignment = Alignment.Center) {
                            Text(m.initial, color = Color(0xFF0A0E14), fontWeight = FontWeight.SemiBold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(m.name, color = p.ink, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                            Text(m.desc, color = p.dim, fontSize = 10.5.sp, fontFamily = MonoFont)
                        }
                        GlassToggle(m.enabled) { checked -> modules[i] = m.copy(enabled = checked) }
                    }
                    if (i < modules.lastIndex) Divider(color = p.glassEdge, thickness = 0.5.dp)
                }
            }
        }
        OutlinedButton(
            onClick = { /* module install flow */ },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = p.dim),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) { Text("+ Install module") }
    }
}

// ---------- Settings ----------
@Composable
fun SettingsScreen(
    darkTheme: Boolean, onDarkThemeChange: (Boolean) -> Unit,
    accent: AccentTheme, onAccentChange: (AccentTheme) -> Unit,
    onBack: () -> Unit
) {
    val p = LocalNexusPalette.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(p.glassFill).border(1.dp, p.glassEdge, CircleShape)
                    .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onBack),
                contentAlignment = Alignment.Center
            ) { BackIcon(tint = p.ink) }
            Text("Settings", color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        SectionLabel("Appearance")
        GlassSegmented(listOf("Light", "Dark"), if (darkTheme) 1 else 0) { onDarkThemeChange(it == 1) }
        GlassCard {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccentTheme.entries.forEach { theme ->
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).background(theme.accent)
                            .border(2.dp, if (theme == accent) p.ink else Color.White.copy(alpha = 0.15f), CircleShape)
                            .clickable(remember { MutableInteractionSource() }, indication = null) { onAccentChange(theme) }
                    )
                }
            }
        }

        SectionLabel("Behavior")
        GlassCard {
            Column {
                BehaviorToggle("Time-boxed grants", "Auto-revoke after a set duration")
                Divider(color = p.glassEdge, thickness = 0.5.dp)
                BehaviorToggle("Capability scoping", "Grant specific caps, not full root")
                Divider(color = p.glassEdge, thickness = 0.5.dp)
                BehaviorToggle("New-request alerts", "Notify on first-time su requests")
            }
        }

        SectionLabel("Advanced")
        GlassCard {
            Column(Modifier.padding(14.dp)) {
                KeyValueRow("Kernel", "5.4.x-nexus")
                KeyValueRow("SUSFS", "v1.5.2")
                KeyValueRow("Verified boot", "green")
            }
        }
    }
}

@Composable
fun BehaviorToggle(title: String, subtitle: String) {
    val p = LocalNexusPalette.current
    var checked by remember { mutableStateOf(true) }
    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = p.ink, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = p.dim, fontSize = 10.5.sp, fontFamily = MonoFont)
        }
        GlassToggle(checked, { checked = it })
    }
}
