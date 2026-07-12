package com.nexussu.manager.ui

import com.nexussu.manager.core.NexusEngine
import com.nexussu.manager.core.RootShell
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Convert Native App Icon to Jetpack Compose Image
fun Drawable.toImageBitmap(): androidx.compose.ui.graphics.ImageBitmap {
    if (this is BitmapDrawable) {
        val bmp = this.bitmap
        if (bmp != null) {
            return bmp.asImageBitmap()
        }
    }
    val bitmap = Bitmap.createBitmap(
        if (intrinsicWidth > 0) intrinsicWidth else 96,
        if (intrinsicHeight > 0) intrinsicHeight else 96,
        Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap.asImageBitmap()
}

// ---------- Home ----------
@Composable
fun HomeScreen(onOpenAdvanced: () -> Unit) {
    val p = LocalNexusPalette.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            RootLens()
            Spacer(Modifier.height(12.dp))
            Text("0 apps granted · 0 modules active", color = p.dim, fontSize = 11.sp, fontFamily = MonoFont)
        }
        DeviceCard(onOpenAdvanced = onOpenAdvanced)
    }
}

@Composable
fun DeviceCard(modifier: Modifier = Modifier, onOpenAdvanced: () -> Unit) {
    val p = LocalNexusPalette.current
    var expanded by remember { mutableStateOf(false) }

    // Static Device Info
    val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    val model = Build.MODEL
    val deviceName = "$manufacturer $model"
    val buildDisplay = Build.DISPLAY
    val androidVersion = Build.VERSION.RELEASE
    val sdkLevel = Build.VERSION.SDK_INT
    val securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "Unknown"
    val cpuAbi = Build.SUPPORTED_ABIS.joinToString(", ")
    val hardware = Build.HARDWARE
    val board = Build.BOARD

    // Root Shell States
    var kernelVersion by remember { mutableStateOf("Loading...") }
    var selinuxStatus by remember { mutableStateOf("Loading...") }
    var isRootAvailable by remember { mutableStateOf(false) }

    // Fetch root info when the card appears
    LaunchedEffect(Unit) {
        isRootAvailable = RootShell.isRootAvailable()
        if (isRootAvailable) {
            kernelVersion = RootShell.getKernelVersion()
            selinuxStatus = RootShell.getSelinuxStatus()
        } else {
            kernelVersion = "Root not available"
            selinuxStatus = "Root not available"
        }
    }

    GlassCard(
        modifier.fillMaxWidth()
            .clickable(remember { MutableInteractionSource() }, indication = null) { expanded = !expanded }
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(deviceName, color = p.ink, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
                    Text("Build: $buildDisplay", color = p.dim, fontSize = 10.5.sp, fontFamily = MonoFont)
                }
                val rotation by animateFloatAsState(if (expanded) 90f else 0f, label = "chevron")
                Box(Modifier.rotate(rotation).padding(start = 8.dp)) { ChevronIcon(tint = p.dim) }
            }
            AnimatedVisibility(expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column(Modifier.padding(top = 12.dp)) {
                    KeyValueRow("Android", "$androidVersion (SDK $sdkLevel)")
                    KeyValueRow("Patch level", securityPatch)
                    KeyValueRow("Kernel", kernelVersion)
                    KeyValueRow("Architecture", cpuAbi)
                    KeyValueRow("Hardware", hardware)
                    KeyValueRow("Board", board)
                    KeyValueRow("SUSFS", if (isRootAvailable) "Active" else "Unavailable")
                    KeyValueRow("SELinux", selinuxStatus)
                    KeyValueRow("Verified boot", "Awaiting root shell...")
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
        Text(value, color = p.ink, fontSize = 12.sp, fontFamily = MonoFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SectionLabel(text: String) {
    val p = LocalNexusPalette.current
    Text(text.uppercase(), color = p.dim, fontSize = 11.sp, letterSpacing = 1.5.sp, fontFamily = MonoFont)
}

// ---------- Superuser ----------
data class GrantedApp(
    val packageName: String,
    val name: String,
    val uid: Int, 
    val icon: Drawable,
    val isSystem: Boolean,
    val excludeMod: Boolean,
    val toggledOn: Boolean
)

@Composable
fun SuperuserScreen() {
    val p = LocalNexusPalette.current
    val context = LocalContext.current
    
    val apps = remember { mutableStateListOf<GrantedApp>() }
    var showSystem by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            
            val flags = PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            val installed = pm.getInstalledApplications(flags)
            val myPackageName = context.packageName

            val appList = installed.mapNotNull { info ->
                if (info.packageName == myPackageName) return@mapNotNull null

                val name = pm.getApplicationLabel(info).toString()
                if (name.isBlank() || name == info.packageName) return@mapNotNull null
                
                val isBaseSystemApp = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystemApp = (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                val isSystem = isBaseSystemApp || isUpdatedSystemApp
                
                val icon = pm.getApplicationIcon(info)
                GrantedApp(
                    packageName = info.packageName,
                    name = name,
                    uid = info.uid, 
                    icon = icon,
                    isSystem = isSystem,
                    excludeMod = false,
                    toggledOn = false
                )
            }.sortedBy { it.name.lowercase() }
            
            withContext(Dispatchers.Main) {
                apps.clear()
                apps.addAll(appList)
                isLoading = false
            }
        }
    }

    val filteredApps = apps.filter { 
        it.isSystem == showSystem && it.name.contains(searchQuery, ignoreCase = true)
    }

    Column(Modifier.fillMaxSize()) {
        Text("Superuser", color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))
        
        GlassCard(modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp)) {
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                textStyle = TextStyle(color = p.ink, fontSize = 14.sp),
                cursorBrush = SolidColor(p.accent),
                modifier = Modifier.fillMaxSize(),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) Text("Search apps...", color = p.dim, fontSize = 14.sp)
                            innerTextField()
                        }
                    }
                }
            )
        }
        
        Spacer(Modifier.height(14.dp))
        GlassSegmented(listOf("User Apps", "System Apps"), if (showSystem) 1 else 0, onSelect = { showSystem = it == 1 })
        Spacer(Modifier.height(14.dp))
        
        GlassCard(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Scanning installed apps...", color = p.dim, fontSize = 13.sp, fontFamily = MonoFont)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(filteredApps, key = { _, app -> app.packageName }) { index, app ->
                        AppRow(
                            app = app,
                            onToggleRoot = { checked -> 
                                val idx = apps.indexOf(app)
                                if (idx >= 0) apps[idx] = app.copy(toggledOn = checked)
                            },
                            onToggleExclude = { checked ->
                                val idx = apps.indexOf(app)
                                if (idx >= 0) apps[idx] = app.copy(excludeMod = checked)
                            }
                        )
                        if (index < filteredApps.lastIndex) Divider(color = p.glassEdge, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun AppRow(app: GrantedApp, onToggleRoot: (Boolean) -> Unit, onToggleExclude: (Boolean) -> Unit) {
    val p = LocalNexusPalette.current
    var expanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, indication = null) { expanded = !expanded }) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 13.dp), 
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val imageBitmap = remember(app.icon) { app.icon.toImageBitmap() }
            Image(bitmap = imageBitmap, contentDescription = null, modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)))
            
            Column(Modifier.weight(1f)) {
                Text(app.name, color = p.ink, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(app.packageName, color = p.dim, fontSize = 9.sp, fontFamily = MonoFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ROOT", color = if (app.toggledOn) p.accent else p.dim, fontSize = 9.sp, fontFamily = MonoFont, fontWeight = FontWeight.Bold)
                GlassToggle(app.toggledOn, onCheckedChange = { checked ->
                    if (checked) {
                        if (NexusEngine.grantUidAccess(app.uid)) { 
                             onToggleRoot(true) 
                        }
                    } else {
                        onToggleRoot(false)
                    }
                })
            }
        }
        
        AnimatedVisibility(expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Row(
                Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.03f)).padding(horizontal = 16.dp, vertical = 12.dp), 
                horizontalArrangement = Arrangement.SpaceBetween, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Exclude Modifications", color = p.ink, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                    Text("Hide root and SUSFS from this app", color = p.dim, fontSize = 10.sp, fontFamily = MonoFont)
                }
                GlassToggle(app.excludeMod, onCheckedChange = { onToggleExclude(it) })
            }
        }
    }
}

// ---------- Log ----------
@Composable
fun LogScreen() {
    val p = LocalNexusPalette.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Logs", color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        GlassCard {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No logs recorded yet", color = p.dim, fontSize = 13.sp, fontFamily = MonoFont)
            }
        }
    }
}

// ---------- Module ----------
data class ModuleItem(val initial: String, val name: String, val desc: String, val enabled: Boolean)

@Composable
fun ModuleScreen() {
    val p = LocalNexusPalette.current
    val context = LocalContext.current
    val modules = remember { mutableStateListOf<ModuleItem>() }

    // File Picker Launcher for installing modules
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // In a full implementation, you would pass this URI to your root shell 
            // to copy and extract to /data/adb/nexussu/modules/
            Toast.makeText(context, "Selected: ${uri.lastPathSegment}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Modules", color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        
        if (modules.isEmpty()) {
            GlassCard {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No modules installed", color = p.dim, fontSize = 13.sp, fontFamily = MonoFont)
                }
            }
        } else {
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
                            GlassToggle(m.enabled, onCheckedChange = { checked -> modules[i] = m.copy(enabled = checked) })
                        }
                        if (i < modules.lastIndex) Divider(color = p.glassEdge, thickness = 0.5.dp)
                    }
                }
            }
        }
        
        OutlinedButton(
            onClick = { 
                // Launch the file manager to pick a ZIP file
                zipPickerLauncher.launch("application/zip")
            },
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Fetch Engine Version from JNI
    val engineVersion = remember { NexusEngine.getEngineVersion() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(p.glassFill).border(1.dp, p.glassEdge, CircleShape)
                    .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onBack),
                contentAlignment = Alignment.Center
            ) { BackIcon(tint = p.ink) }
            Text("Settings", color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        // 1. Appearance
        SectionLabel("Appearance")
        GlassSegmented(listOf("Light", "Dark"), if (darkTheme) 1 else 0, onSelect = { onDarkThemeChange(it == 1) })
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

        // 2. Stealth & Security
        SectionLabel("Stealth & Security")
        GlassCard {
            Column {
                BehaviorToggle("Hide NexusSU App", "Hide manager icon from launcher")
                Divider(color = p.glassEdge, thickness = 0.5.dp)
                BehaviorToggle("Hide from Play Protect", "Spoof package visibility to avoid detection")
            }
        }

        // 3. Root Behavior
        SectionLabel("Root Behavior")
        GlassCard {
            Column {
                BehaviorToggle("Time-boxed grants", "Auto-revoke root after 10 minutes")
                Divider(color = p.glassEdge, thickness = 0.5.dp)
                BehaviorToggle("Capability scoping", "Grant specific caps, not full root")
                Divider(color = p.glassEdge, thickness = 0.5.dp)
                BehaviorToggle("Systemless Hosts", "Redirect /system/etc/hosts for AdAway")
                Divider(color = p.glassEdge, thickness = 0.5.dp)
                BehaviorToggle("Zygisk Compatibility", "Enable Zygisk module injection")
            }
        }

        // 4. Updates
        SectionLabel("Updates")
        GlassCard {
            Column {
                SettingsRow("Check for Updates", "Check for new NexusSU releases") {
                    // Update check logic here
                }
                Divider(color = p.glassEdge, thickness = 0.5.dp)
                SettingsRow("Update Channel", "Stable") {
                    // Channel selector here
                }
            }
        }

        // 5. System Operations
        SectionLabel("System Operations")
        GlassCard {
            Column {
                SettingsRow("Reboot Device", "Standard system reboot") {
                    scope.launch { RootShell.execute("setprop sys.powerctl reboot") }
                }
                Divider(color = p.glassEdge, thickness = 0.5.dp)
                SettingsRow("Reboot to Recovery", "Restart into recovery mode") {
                    scope.launch { RootShell.execute("setprop sys.powerctl reboot,recovery") }
                }
                Divider(color = p.glassEdge, thickness = 0.5.dp)
                SettingsRow("Reboot to Bootloader", "Restart into fastboot mode") {
                    scope.launch { RootShell.execute("setprop sys.powerctl reboot,bootloader") }
                }
            }
        }

        // 6. Data Management
        SectionLabel("Data Management")
        GlassCard {
            Column {
                SettingsRow("Clear Root Logs", "Delete all stored su request logs") {
                    // Clear logs logic here
                }
                Divider(color = p.glassEdge, thickness = 0.5.dp)
                SettingsRow("Reset Superuser List", "Revoke all granted root permissions") {
                    // Clear database logic here
                }
            }
        }

        // 7. About
        SectionLabel("About")
        GlassCard {
            Column {
                KeyValueRow("Manager Version", "1.0.0")
                Divider(color = p.glassEdge, thickness = 0.5.dp)
                KeyValueRow("Engine Version", if (engineVersion == 100) "v100 (Active)" else "Not Found")
                Divider(color = p.glassEdge, thickness = 0.5.dp)
                KeyValueRow("GitHub", "NexusSU/manager")
            }
        }
        
        Spacer(Modifier.height(16.dp)) // Bottom padding for scroll
    }
}

@Composable
fun BehaviorToggle(title: String, subtitle: String) {
    val p = LocalNexusPalette.current
    var checked by remember { mutableStateOf(false) } // Default to false
    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = p.ink, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = p.dim, fontSize = 10.5.sp, fontFamily = MonoFont)
        }
        GlassToggle(checked, onCheckedChange = { checked = it })
    }
}

@Composable
fun SettingsRow(title: String, subtitle: String, onClick: () -> Unit) {
    val p = LocalNexusPalette.current
    Row(
        Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, indication = null) { onClick() }.padding(13.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = p.ink, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = p.dim, fontSize = 10.5.sp, fontFamily = MonoFont)
        }
        Box(Modifier.rotate(180f)) { 
            BackIcon(tint = p.dim) // Reusing BackIcon but rotated 180deg to point right
        }
    }
}
