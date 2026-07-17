package com.nexussu.manager.ui

import com.nexussu.manager.core.NexusEngine
import com.nexussu.manager.core.RootShell
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Convert Native App Icon to Jetpack Compose Image
fun Drawable.toImageBitmap(): androidx.compose.ui.graphics.ImageBitmap {
    if (this is BitmapDrawable) {
        val bmp = this.bitmap
        if (bmp != null) return bmp.asImageBitmap()
    }
    val bitmap = Bitmap.createBitmap(if (intrinsicWidth > 0) intrinsicWidth else 96, if (intrinsicHeight > 0) intrinsicHeight else 96, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap.asImageBitmap()
}

// ---------- Home ----------
@Composable
fun HomeScreen(isRootActive: Boolean, onOpenAdvanced: () -> Unit) {
    val p = LocalNexusPalette.current
    val scope = rememberCoroutineScope()
    
    var grantedCount by remember { mutableStateOf(0) }
    var moduleCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        if (isRootActive) {
            scope.launch(Dispatchers.IO) {
                grantedCount = NexusEngine.getGrantedUids().size
                moduleCount = NexusEngine.getActiveModulesCount()
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Kernel Warning Banner
        if (!isRootActive) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    Modifier.padding(16.dp), 
                    verticalAlignment = Alignment.CenterVertically, 
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFFF5252).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("!", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Kernel Not Patched", color = Color(0xFFFF5252), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("NexusSU kernel hooks are missing. Root features are disabled.", color = p.dim, fontSize = 11.sp, fontFamily = MonoFont)
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            RootLens(isActive = isRootActive)
            Spacer(Modifier.height(12.dp))
            Text("$grantedCount apps granted · $moduleCount modules active", color = p.dim, fontSize = 11.sp, fontFamily = MonoFont)
        }
        DeviceCard(isRootActive, onOpenAdvanced)
    }
}

@Composable
fun DeviceCard(isRootActive: Boolean, modifier: Modifier = Modifier, onOpenAdvanced: () -> Unit) {
    val p = LocalNexusPalette.current
    var expanded by remember { mutableStateOf(false) }

    val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    val model = Build.MODEL
    val deviceName = "$manufacturer $model"
    val buildDisplay = Build.DISPLAY

    var kernelVersion by remember { mutableStateOf("Loading...") }
    var selinuxStatus by remember { mutableStateOf("Loading...") }
    var verifiedBootState by remember { mutableStateOf("Loading...") }
    var busyboxStatus by remember { mutableStateOf("Loading...") }

    LaunchedEffect(Unit) {
        if (isRootActive) {
            kernelVersion = RootShell.getKernelVersion()
            selinuxStatus = RootShell.getSelinuxStatus()
            verifiedBootState = RootShell.getVerifiedBootState()
            busyboxStatus = if (RootShell.execute("mount | grep busybox").isNotBlank()) "Active (v1.0.0)" else "Inactive"
        } else {
            kernelVersion = "Root not available"
            selinuxStatus = "Root not available"
            verifiedBootState = "Root not available"
            busyboxStatus = "Root not available"
        }
    }

    GlassCard(modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, indication = null) { expanded = !expanded }) {
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
                    KeyValueRow("Android", "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                    KeyValueRow("Kernel", kernelVersion)
                    KeyValueRow("SUSFS", if (isRootActive) "Active" else "Unavailable")
                    KeyValueRow("SELinux", selinuxStatus)
                    KeyValueRow("Verified boot", verifiedBootState)
                    KeyValueRow("BusyBox", busyboxStatus)
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
    val packageName: String, val name: String, val uid: Int, val icon: Drawable,
    val isSystem: Boolean, val excludeMod: Boolean, val toggledOn: Boolean
)

@Composable
fun SuperuserScreen(isRootActive: Boolean) {
    val p = LocalNexusPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val apps = remember { mutableStateListOf<GrantedApp>() }
    var showSystem by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            if (!isRootActive) { isLoading = false; return@withContext }
            val grantedUids = NexusEngine.getGrantedUids()
            val deniedUids = NexusEngine.getDeniedUids()
            val pm = context.packageManager
            val flags = PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
            val installed = pm.getInstalledApplications(flags)
            
            val appList = installed.mapNotNull { info ->
                if (info.packageName == context.packageName) return@mapNotNull null
                val name = pm.getApplicationLabel(info).toString()
                if (name.isBlank()) return@mapNotNull null
                val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                GrantedApp(
                    packageName = info.packageName, name = name, uid = info.uid,
                    icon = pm.getApplicationIcon(info), isSystem = isSystem,
                    excludeMod = deniedUids.contains(info.uid),
                    toggledOn = grantedUids.contains(info.uid)
                )
            }.sortedBy { it.name.lowercase() }
            
            withContext(Dispatchers.Main) {
                apps.clear(); apps.addAll(appList); isLoading = false
            }
        }
    }

    val filteredApps = apps.filter { it.isSystem == showSystem && it.name.contains(searchQuery, ignoreCase = true) }

    Column(Modifier.fillMaxSize()) {
        Text("Superuser", color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))
        
        GlassCard(modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(16.dp)) {
            BasicTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                textStyle = TextStyle(color = p.ink, fontSize = 14.sp), cursorBrush = SolidColor(p.accent),
                modifier = Modifier.fillMaxSize(), singleLine = true,
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
            if (!isRootActive) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Root not available.", color = p.dim, fontSize = 13.sp, fontFamily = MonoFont) }
            } else if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Scanning installed apps...", color = p.dim, fontSize = 13.sp, fontFamily = MonoFont) }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(filteredApps, key = { _, app -> app.packageName }) { index, app ->
                        AppRow(app = app, isRootActive = isRootActive, onToggleRoot = { checked -> 
                            val idx = apps.indexOf(app)
                            if (idx >= 0) apps[idx] = app.copy(toggledOn = checked)
                            scope.launch(Dispatchers.IO) {
                                if (checked) NexusEngine.saveGrantedUid(app.uid) else NexusEngine.removeGrantedUid(app.uid)
                            }
                        }, onToggleExclude = { checked ->
                            val idx = apps.indexOf(app)
                            if (idx >= 0) apps[idx] = app.copy(excludeMod = checked)
                            scope.launch(Dispatchers.IO) {
                                if (checked) NexusEngine.saveDeniedUid(app.uid) else NexusEngine.removeDeniedUid(app.uid)
                            }
                        })
                        if (index < filteredApps.lastIndex) Divider(color = p.glassEdge, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun AppRow(app: GrantedApp, isRootActive: Boolean, onToggleRoot: (Boolean) -> Unit, onToggleExclude: (Boolean) -> Unit) {
    val p = LocalNexusPalette.current
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clickable(remember { MutableInteractionSource() }, indication = null) { expanded = !expanded }) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val imageBitmap = remember(app.icon) { app.icon.toImageBitmap() }
            Image(bitmap = imageBitmap, contentDescription = null, modifier = Modifier.size(38.dp).clip(RoundedCornerShape(8.dp)))
            Column(Modifier.weight(1f)) {
                Text(app.name, color = p.ink, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(app.packageName, color = p.dim, fontSize = 9.sp, fontFamily = MonoFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ROOT", color = if (app.toggledOn) p.accent else p.dim, fontSize = 9.sp, fontFamily = MonoFont, fontWeight = FontWeight.Bold)
                GlassToggle(app.toggledOn, onCheckedChange = { onToggleRoot(it) }, enabled = isRootActive)
            }
        }
        AnimatedVisibility(expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Row(Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.03f)).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Exclude Modifications", color = p.ink, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                    Text("Hide root and SUSFS from this app", color = p.dim, fontSize = 10.sp, fontFamily = MonoFont)
                }
                GlassToggle(app.excludeMod, onCheckedChange = { onToggleExclude(it) }, enabled = isRootActive)
            }
        }
    }
}

// ---------- Log ----------
@Composable
fun LogScreen(isRootActive: Boolean) {
    val p = LocalNexusPalette.current
    val scope = rememberCoroutineScope()
    val logs = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()

    DisposableEffect(Unit) {
        var process: Process? = null
        var reader: java.io.BufferedReader? = null
        
        if (isRootActive) {
            scope.launch(Dispatchers.IO) {
                try {
                    val cmd = "touch /data/adb/nexussu/logs.txt && tail -n 20 -f /data/adb/nexussu/logs.txt"
                    process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                    reader = java.io.BufferedReader(java.io.InputStreamReader(process!!.inputStream))
                    
                    var line: String?
                    while (reader!!.readLine().also { line = it } != null) {
                        val currentLine = line
                        if (currentLine != null) {
                            withContext(Dispatchers.Main) {
                                logs.add(currentLine)
                                if (logs.size > 200) logs.removeAt(0)
                                scope.launch { listState.animateScrollToItem(logs.size - 1) }
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        logs.add("Error reading logs or root denied.")
                    }
                }
            }
        }

        onDispose {
            process?.destroy()
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
            Text("Root Access Logs", color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Box(
                Modifier.clip(CircleShape).background(p.glassFill).clickable(enabled = isRootActive, remember { MutableInteractionSource() }, indication = null) {
                    scope.launch(Dispatchers.IO) {
                        RootShell.execute("rm -f /data/adb/nexussu/logs.txt")
                        withContext(Dispatchers.Main) { logs.clear() }
                    }
                }.padding(8.dp)
            ) {
                Text("Clear", color = if (isRootActive) p.accent else p.dim, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
        
        GlassCard(modifier = Modifier.fillMaxSize()) {
            if (!isRootActive) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Root not available.", color = p.dim, fontSize = 13.sp, fontFamily = MonoFont) }
            } else if (logs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Waiting for root requests...", color = p.dim, fontSize = 13.sp, fontFamily = MonoFont) }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { logLine ->
                        Text(text = logLine, color = p.accent, fontSize = 12.sp, fontFamily = MonoFont)
                    }
                }
            }
        }
    }
}

// ---------- Module ----------
data class ModuleItem(val id: String, val initial: String, val name: String, val version: String, val versionCode: Int, val author: String, val desc: String, val updateJson: String, val enabled: Boolean, val isPendingRemoval: Boolean = false)

@Composable
fun ModuleScreen(isRootActive: Boolean) {
    val p = LocalNexusPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val modules = remember { mutableStateListOf<ModuleItem>() }
    var isInstalling by remember { mutableStateOf(false) }
    var showInstallLog by remember { mutableStateOf(false) }
    var installLogContent by remember { mutableStateOf("") }
    
    val moduleUpdates = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(Unit) {
        if (!isRootActive) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val installed = NexusEngine.getInstalledModules()
            withContext(Dispatchers.Main) {
                modules.clear(); modules.addAll(installed)
            }
            
            installed.forEach { module ->
                if (module.updateJson.isNotBlank()) {
                    val update = NexusEngine.checkModuleUpdate(module.updateJson)
                    if (update != null && update.versionCode > module.versionCode) {
                        withContext(Dispatchers.Main) {
                            moduleUpdates[module.id] = update.zipUrl
                        }
                    }
                }
            }
        }
    }

    val zipPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            isInstalling = true
            val cacheFile = File(context.cacheDir, "module.zip")
            context.contentResolver.openInputStream(uri)?.use { input -> cacheFile.outputStream().use { output -> input.copyTo(output) } }
            
            Thread {
                val success = RootShell.installModule(cacheFile.absolutePath)
                cacheFile.delete()
                Handler(Looper.getMainLooper()).post {
                    isInstalling = false
                    if (success) {
                        Toast.makeText(context, "Module installed!", Toast.LENGTH_SHORT).show()
                        modules.clear(); modules.addAll(NexusEngine.getInstalledModules())
                    } else {
                        Toast.makeText(context, "Installation failed. Check log.", Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Modules", color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        if (!isRootActive) {
            GlassCard { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("Root not available.", color = p.dim, fontSize = 13.sp, fontFamily = MonoFont) } }
        } else if (isInstalling) {
            GlassCard { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("Installing module...", color = p.accent, fontSize = 13.sp, fontFamily = MonoFont) } }
        } else if (modules.isEmpty()) {
            GlassCard { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No modules installed", color = p.dim, fontSize = 13.sp, fontFamily = MonoFont) } }
        } else {
            GlassCard {
                Column {
                    modules.forEachIndexed { i, m ->
                        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(p.accent, p.accent2))), contentAlignment = Alignment.Center) { 
                                Text(m.initial, color = Color(0xFF0A0E14), fontWeight = FontWeight.SemiBold) 
                            }
                            Column(Modifier.weight(1f)) {
                                Text(m.name, color = p.ink, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("v${m.version} by ${m.author}", color = p.dim, fontSize = 9.sp, fontFamily = MonoFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                
                                if (m.isPendingRemoval) {
                                    Text("Pending removal on next reboot", color = Color(0xFFFF5252), fontSize = 10.sp, fontFamily = MonoFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                } else {
                                    Text(m.desc, color = p.dim, fontSize = 10.5.sp, fontFamily = MonoFont, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }

                            if (m.isPendingRemoval) {
                                Text(
                                    text = "Undo",
                                    color = p.accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable(remember { MutableInteractionSource() }, indication = null) {
                                        scope.launch(Dispatchers.IO) {
                                            NexusEngine.restoreModule(m.id)
                                            modules.clear(); modules.addAll(NexusEngine.getInstalledModules())
                                        }
                                    }.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            } else {
                                if (moduleUpdates.containsKey(m.id)) {
                                    Text(
                                        text = "Update",
                                        color = p.accent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable(remember { MutableInteractionSource() }, indication = null) {
                                            val zipUrl = moduleUpdates[m.id] ?: return@clickable
                                            isInstalling = true
                                            scope.launch(Dispatchers.IO) {
                                                val cacheFile = File(context.cacheDir, "module_update.zip")
                                                try {
                                                    val url = java.net.URL(zipUrl)
                                                    val conn = url.openConnection() as java.net.HttpURLConnection
                                                    conn.connect()
                                                    java.io.FileOutputStream(cacheFile).use { output -> conn.inputStream.copyTo(output) }
                                                    
                                                    val success = RootShell.installModule(cacheFile.absolutePath)
                                                    cacheFile.delete()
                                                    
                                                    withContext(Dispatchers.Main) {
                                                        isInstalling = false
                                                        if (success) {
                                                            Toast.makeText(context, "Module updated!", Toast.LENGTH_SHORT).show()
                                                            moduleUpdates.remove(m.id)
                                                            modules.clear(); modules.addAll(NexusEngine.getInstalledModules())
                                                        } else {
                                                            Toast.makeText(context, "Update failed.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        isInstalling = false
                                                        Toast.makeText(context, "Download failed.", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                
                                Text(
                                    text = "Uninstall",
                                    color = p.dim,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable(remember { MutableInteractionSource() }, indication = null) {
                                        scope.launch(Dispatchers.IO) {
                                            NexusEngine.deleteModule(m.id)
                                            modules.clear(); modules.addAll(NexusEngine.getInstalledModules())
                                        }
                                    }.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                                
                                GlassToggle(m.enabled, onCheckedChange = { checked ->
                                    modules[i] = m.copy(enabled = checked)
                                    scope.launch(Dispatchers.IO) { NexusEngine.setModuleEnabled(m.id, checked) }
                                })
                            }
                        }
                        if (i < modules.lastIndex) Divider(color = p.glassEdge, thickness = 0.5.dp)
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { 
                scope.launch(Dispatchers.IO) {
                    installLogContent = NexusEngine.getInstallLog()
                    showInstallLog = true
                }
            },
            enabled = isRootActive,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = p.dim),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) { Text("View Installation Log") }

        OutlinedButton(onClick = { zipPickerLauncher.launch("application/zip") }, enabled = isRootActive && !isInstalling, colors = ButtonDefaults.outlinedButtonColors(contentColor = p.dim), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) { Text(if (isInstalling) "Installing..." else "+ Install module") }
    }

    if (showInstallLog) {
        AlertDialog(
            onDismissRequest = { showInstallLog = false },
            confirmButton = { TextButton(onClick = { showInstallLog = false }) { Text("Close", color = p.accent) } },
            title = { Text("Installation Log", color = p.ink) },
            text = { 
                Text(
                    text = installLogContent, 
                    color = p.accent, 
                    fontSize = 10.sp, 
                    fontFamily = MonoFont,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) 
            },
            containerColor = p.void,
            titleContentColor = p.ink,
            textContentColor = p.accent
        )
    }
}

// ---------- Settings ----------
@Composable
fun SettingsScreen(
    darkTheme: Boolean, onDarkThemeChange: (Boolean) -> Unit,
    accent: AccentTheme, onAccentChange: (AccentTheme) -> Unit,
    isRootActive: Boolean, // NEW
    onBack: () -> Unit
) {
    val p = LocalNexusPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engineVersion = remember { NexusEngine.getEngineVersion() }
    var systemlessHosts by remember { mutableStateOf(false) }
    var safeMode by remember { mutableStateOf(RootShell.execute("[ -f /data/adb/nexussu/safemode ] && echo 1 || echo 0").trim() == "1") }
    var adbRoot by remember { mutableStateOf(NexusEngine.isAdbRootEnabled()) }
    var tempRoot by remember { mutableStateOf(RootShell.execute("mount | grep '/system/bin/su'").isBlank()) }
    
    val prefs = remember { context.getSharedPreferences("nexussu_prefs", 0) }
    var timeBoxedGrants by remember { mutableStateOf(prefs.getBoolean("time_boxed_grants", false)) }
    
    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateUrl by remember { mutableStateOf<String?>(null) }

    val currentVersion = remember {
        try {
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "v${pkgInfo.versionName}"
        } catch (e: Exception) {
            "v1.0.0"
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(p.glassFill).border(1.dp, p.glassEdge, CircleShape).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onBack), contentAlignment = Alignment.Center) { BackIcon(tint = p.ink) }
            Text("Settings", color = p.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        SectionLabel("Appearance")
        GlassSegmented(listOf("Light", "Dark"), if (darkTheme) 1 else 0, onSelect = { onDarkThemeChange(it == 1) })
        GlassCard {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccentTheme.entries.forEach { theme ->
                    Box(Modifier.size(26.dp).clip(CircleShape).background(theme.accent).border(2.dp, if (theme == accent) p.ink else Color.White.copy(alpha = 0.15f), CircleShape).clickable(remember { MutableInteractionSource() }, indication = null) { onAccentChange(theme) })
                }
            }
        }

        SectionLabel("Stealth & Security")
        GlassCard {
            Column {
                SettingsRow("Hide NexusSU App", "Remove icon from launcher. Dial *#*#63987378#*#* to restore.") {
                    val pm = context.packageManager
                    val componentName = android.content.ComponentName(context, "com.nexussu.manager.MainActivity")
                    pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                    Toast.makeText(context, "App hidden. Use dialer code to restore.", Toast.LENGTH_LONG).show()
                }
            }
        }

        SectionLabel("Root Behavior")
        GlassCard {
            Column {
                BehaviorToggle("Disable Root", "Temporarily unmount su and busybox", checked = tempRoot, enabled = isRootActive) { isChecked ->
                    scope.launch(Dispatchers.IO) {
                        val success = if (isChecked) NexusEngine.disableRoot() else NexusEngine.enableRoot()
                        if (success) tempRoot = isChecked
                    }
                }
                Divider(color = p.glassEdge, thickness = 0.5.dp)
                BehaviorToggle("Time-boxed grants", "Auto-revoke root after 10 minutes", checked = timeBoxedGrants, enabled = isRootActive) { isChecked ->
                    prefs.edit().putBoolean("time_boxed_grants", isChecked).apply()
                    timeBoxedGrants = isChecked
                }
                Divider(color = p.glassEdge, thickness = 0.5.dp)
                BehaviorToggle("Allow ADB Root", "Grant root permissions to ADB Shell (UID 2000)", checked = adbRoot, enabled = isRootActive) { isChecked ->
                    scope.launch(Dispatchers.IO) {
                        val success = NexusEngine.setAdbRootEnabled(isChecked)
                        if (success) adbRoot = isChecked
                    }
                }
                Divider(color = p.glassEdge, thickness = 0.5.dp)
                BehaviorToggle("Systemless Hosts", "Redirect /system/etc/hosts for AdAway", checked = systemlessHosts, enabled = isRootActive) { isChecked ->
                    scope.launch(Dispatchers.IO) {
                        val success = if (isChecked) NexusEngine.enableSystemlessHosts() else NexusEngine.disableSystemlessHosts()
                        if (success) systemlessHosts = isChecked
                    }
                }
                Divider(color = p.glassEdge, thickness = 0.5.dp)
                BehaviorToggle("Safe Mode", "Disable all modules on next boot", checked = safeMode, enabled = isRootActive) { isChecked ->
                    scope.launch(Dispatchers.IO) {
                        if (isChecked) RootShell.execute("touch /data/adb/nexussu/safemode")
                        else RootShell.execute("rm -f /data/adb/nexussu/safemode")
                        safeMode = isChecked
                    }
                }
            }
        }

        SectionLabel("Updates")
        GlassCard {
            Column {
                SettingsRow("Check for Updates", if (isCheckingUpdates) "Checking..." else "Check for new NexusSU releases") {
                    if (!isCheckingUpdates) {
                        isCheckingUpdates = true
                        scope.launch(Dispatchers.IO) {
                            val url = UpdateChecker.checkForUpdates(context, currentVersion)
                            isCheckingUpdates = false
                            updateUrl = url
                            if (url == null) {
                                Toast.makeText(context, "NexusSU is up to date!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }

        if (updateUrl != null) {
            GlassCard {
                SettingsRow("Update Available!", "Tap here to download the latest version") {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
                    context.startActivity(intent)
                }
            }
        }

        SectionLabel("System Operations")
        GlassCard {
            Column {
                SettingsRow("Reboot Device", "Standard system reboot", enabled = isRootActive) { scope.launch(Dispatchers.IO) { RootShell.execute("setprop sys.powerctl reboot") } }
                Divider(color = p.glassEdge, thickness = 0.5.dp)
                SettingsRow("Reboot to Recovery", "Restart into recovery mode", enabled = isRootActive) { scope.launch(Dispatchers.IO) { RootShell.execute("setprop sys.powerctl reboot,recovery") } }
                Divider(color = p.glassEdge, thickness = 0.5.dp)
                SettingsRow("Reboot to Bootloader", "Restart into fastboot mode", enabled = isRootActive) { scope.launch(Dispatchers.IO) { RootShell.execute("setprop sys.powerctl reboot,bootloader") } }
            }
        }

        SectionLabel("Data Management")
        GlassCard {
            Column {
                SettingsRow("Clear Root Logs", "Delete all stored su request logs", enabled = isRootActive) { scope.launch(Dispatchers.IO) { RootShell.execute("rm -f /data/adb/nexussu/logs.txt") } }
                Divider(color = p.glassEdge, thickness = 0.5.dp)
                SettingsRow("Reset Superuser List", "Revoke all granted root permissions", enabled = isRootActive) {
                    scope.launch(Dispatchers.IO) { NexusEngine.clearAllRootGrants() }
                    Toast.makeText(context, "All root permissions revoked", Toast.LENGTH_SHORT).show()
                }
                Divider(color = p.glassEdge, thickness = 0.5.dp)
                SettingsRow("Uninstall NexusSU", "Remove all binaries, modules, and data", enabled = isRootActive) {
                    scope.launch(Dispatchers.IO) {
                        RootShell.execute("umount /system/bin/su")
                        RootShell.execute("umount /system/etc/hosts")
                        RootShell.execute("rm -rf /data/adb/nexussu")
                        Toast.makeText(context, "NexusSU uninstalled. Reboot your device.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        SectionLabel("About")
        GlassCard {
            Column {
                KeyValueRow("Manager Version", currentVersion.removePrefix("v"))
                Divider(color = p.glassEdge, thickness = 0.5.dp)
                KeyValueRow("Engine Version", if (engineVersion == 100) "v100 (Active)" else "Not Found")
                Divider(color = p.glassEdge, thickness = 0.5.dp)
                KeyValueRow("GitHub", "NexusSU/manager")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun BehaviorToggle(title: String, subtitle: String, checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    val p = LocalNexusPalette.current
    Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) p.ink else p.dim, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = p.dim, fontSize = 10.5.sp, fontFamily = MonoFont)
        }
        GlassToggle(checked, onCheckedChange = { onCheckedChange(it) }, enabled = enabled)
    }
}

@Composable
fun SettingsRow(title: String, subtitle: String, enabled: Boolean = true, onClick: () -> Unit) {
    val p = LocalNexusPalette.current
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled, remember { MutableInteractionSource() }, indication = null) { onClick() }.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) p.ink else p.dim, fontSize = 13.5.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = p.dim, fontSize = 10.5.sp, fontFamily = MonoFont)
        }
        if (enabled) {
            Box(Modifier.rotate(180f)) { BackIcon(tint = p.dim) }
        }
    }
}

// ---------- Root Lens ----------
@Composable
fun RootLens(modifier: Modifier = Modifier, isActive: Boolean = false) {
    val p = LocalNexusPalette.current
    val infinite = rememberInfiniteTransition(label = "lensIdle")
    val idleX by infinite.animateFloat(-0.08f, 0.12f,
        infiniteRepeatable(tween(3500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "idleX")
    val idleY by infinite.animateFloat(-0.06f, 0.10f,
        infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "idleY")

    var dragOffset by remember { mutableStateOf<Offset?>(null) }
    val scope = rememberCoroutineScope()
    var idleJob by remember { mutableStateOf<Job?>(null) }

    Box(
        modifier.size(172.dp).pointerInput(Unit) {
            detectDragGestures(
                onDrag = { change, _ ->
                    idleJob?.cancel()
                    dragOffset = Offset((change.position.x / size.width) - 0.5f, (change.position.y / size.height) - 0.5f)
                },
                onDragEnd = { idleJob = scope.launch { delay(1100); dragOffset = null } }
            )
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.sweepGradient(listOf(p.accent, p.accent2, p.accent)))
            val hx = dragOffset?.x ?: idleX
            val hy = dragOffset?.y ?: idleY
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.75f), Color.Transparent)),
                radius = size.minDimension * 0.4f,
                center = Offset(size.width * (0.5f + hx), size.height * (0.5f + hy)),
                blendMode = BlendMode.Softlight
            )
        }
        Box(Modifier.size(110.dp).clip(CircleShape).background(p.void.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ROOT", color = p.dim, fontSize = 11.sp, letterSpacing = 1.sp)
                Text(if (isActive) "Active" else "Inactive", color = if (isActive) p.ink else p.dim, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            }
        }
    }
}
