package com.nexussu.manager.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class RootGrantedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.nexussu.manager.ROOT_GRANTED") {
            val uidStr = intent.getStringExtra("caller_uid") ?: return
            val uid = uidStr.toIntOrNull() ?: return
            
            val pm = context.packageManager
            val packageName = pm.getNameForUid(uid)?.split(":")?.firstOrNull() ?: "Unknown"
            val appName = try {
                val info = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(info).toString()
            } catch (e: Exception) {
                "UID: $uid"
            }
            
            showNotification(context, appName)
        }
    }
    
    private fun showNotification(context: Context, appName: String) {
        val channelId = "nexussu_root"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Root Access", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Root Access Granted")
            .setContentText("$appName was granted superuser access.")
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
