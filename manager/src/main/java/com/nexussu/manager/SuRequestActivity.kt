package com.nexussu.manager

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexussu.manager.core.NexusEngine
import com.nexussu.manager.ui.AccentTheme
import com.nexussu.manager.ui.LocalNexusPalette
import com.nexussu.manager.ui.MonoFont
import com.nexussu.manager.ui.NexusSUTheme
import java.io.File

class SuRequestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val callerUidStr = intent.getStringExtra("caller_uid") ?: "-1"
        val pinStr = intent.getStringExtra("pin") ?: "0"
        val pidStr = intent.getStringExtra("pid") ?: "0"
        
        val callerUid = callerUidStr.toIntOrNull() ?: -1
        val pin = pinStr
        val pid = pidStr
        
        if (callerUid == -1) {
            finish()
            return
        }

        val prefs = getSharedPreferences("nexussu_prefs", 0)
        val isTimeBoxed = prefs.getBoolean("time_boxed_grants", false)

        val pm = packageManager
        val packageName = pm.getNameForUid(callerUid)?.split(":")?.firstOrNull() ?: "Unknown"
        val appName = try {
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            "UID: $callerUid"
        }
        
        val appIcon: Bitmap? = try {
            val drawable = pm.getApplicationIcon(packageName)
            if (drawable is BitmapDrawable) drawable.bitmap else null
        } catch (e: Exception) {
            null
        }

        setContent {
            NexusSUTheme(darkTheme = true, accentTheme = AccentTheme.Nexus) {
                SuRequestDialog(
                    appName = appName,
                    appIcon = appIcon,
                    isTimeBoxed = isTimeBoxed,
                    onGrant = { rememberChoice ->
                        Thread {
                            if (isTimeBoxed || !rememberChoice) {
                                NexusEngine.grantUidTemporary(callerUid)
                                if (isTimeBoxed) NexusEngine.scheduleRevoke(callerUid)
                            } else {
                                NexusEngine.saveGrantedUid(callerUid)
                            }
                            createResponseFile(pin, pid)
                            Handler(Looper.getMainLooper()).post { finish() }
                        }.start()
                    },
                    onDeny = { rememberChoice ->
                        Thread {
                            if (!isTimeBoxed && rememberChoice) {
                                NexusEngine.saveDeniedUid(callerUid)
                            }
                            createResponseFile("0", pid)
                            Handler(Looper.getMainLooper()).post { finish() }
                        }.start()
                    }
                )
            }
        }
    }
    
    private fun createResponseFile(pin: String, pid: String) {
        try {
            val file = File("/data/local/tmp/.nexussu_response_$pid")
            file.writeText(pin)
            file.setReadable(true, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun SuRequestDialog(
    appName: String,
    appIcon: Bitmap?,
    isTimeBoxed: Boolean,
    onGrant: (rememberChoice: Boolean) -> Unit,
    onDeny: (rememberChoice: Boolean) -> Unit
) {
    val p = LocalNexusPalette.current
    var timeLeft by remember { mutableStateOf(10) }
    var rememberChoice by remember { mutableStateOf(true) }

    if (isTimeBoxed) {
        rememberChoice = false
    }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            kotlinx.coroutines.delay(1000)
            timeLeft--
        }
        if (timeLeft == 0) onDeny(rememberChoice)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(p.void, p.voidGradientEnd)))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
                )
            } else {
                Box(
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(p.glassFill),
                    contentAlignment = Alignment.Center
                ) {
                    Text(appName.firstOrNull()?.uppercase() ?: "U", color = p.ink, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            Text("Superuser Request", color = p.dim, fontSize = 12.sp, fontFamily = MonoFont)
            Spacer(Modifier.height(4.dp))
            Text(appName, color = p.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("is requesting root access.", color = p.dim, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))
            
            if (!isTimeBoxed) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.glassFill)
                        .clickable(remember { MutableInteractionSource() }, indication = null) { rememberChoice = !rememberChoice }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(if (rememberChoice) p.accent else Color.Transparent))
                    Text("Remember choice", color = p.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(20.dp))
            } else {
                Text("Time-boxed mode active. Root will be revoked in 10 minutes.", color = p.accent, fontSize = 11.sp, fontFamily = MonoFont)
                Spacer(Modifier.height(20.dp))
            }
            
            Button(
                onClick = { onGrant(rememberChoice) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = Color(0xFF0A0E14))
            ) {
                Text("Grant", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
            }
            
            Spacer(Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = { onDeny(rememberChoice) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = p.dim)
            ) {
                Text("Deny", fontWeight = FontWeight.Medium, modifier = Modifier.padding(vertical = 4.dp))
            }
            
            Spacer(Modifier.height(12.dp))
            Text("Auto-denying in $timeLeft seconds...", color = p.dim.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = MonoFont)
        }
    }
}
