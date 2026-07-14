package com.nexussu.manager

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.nexussu.manager.core.NexusEngine
import java.io.File

class SuRequestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val callerUidStr = intent.getStringExtra("caller_uid")
        val pinStr = intent.getStringExtra("pin")
        
        val callerUid = callerUidStr?.toIntOrNull() ?: -1
        val pin = pinStr ?: "0"
        
        if (callerUid == -1) {
            finish()
            return
        }
        
        // Look up app name from UID
        val pm = packageManager
        val packageName = pm.getNameForUid(callerUid)?.split(":")?.firstOrNull() ?: "Unknown"
        val appName = try {
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            "UID: $callerUid"
        }
        
        AlertDialog.Builder(this)
            .setTitle("Superuser Request")
            .setMessage("$appName is requesting superuser access.")
            .setPositiveButton("Grant") { _, _ ->
                Thread {
                    // Grant root via prctl and persist to file
                    NexusEngine.saveGrantedUid(callerUid)
                    // Signal the su binary to proceed by writing the exact PIN
                    createResponseFile(pin)
                    Handler(Looper.getMainLooper()).post { finish() }
                }.start()
            }
            .setNegativeButton("Deny") { _, _ ->
                Thread {
                    // Just signal the su binary with an invalid PIN so it denies
                    createResponseFile("0")
                    Handler(Looper.getMainLooper()).post { finish() }
                }.start()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun createResponseFile(pin: String) {
        try {
            val file = File("/data/local/tmp/.nexussu_response")
            file.writeText(pin)
            file.setReadable(true, false)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
