package com.nexussu.manager.core

import android.content.Context
import android.util.Log
import com.nexussu.manager.ui.ModuleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object NexusEngine {
    private const val TAG = "NexusEngine"
    private const val CONFIG_PATH = "/data/adb/nexussu/granted_uids.txt"
    private const val DENYLIST_PATH = "/data/adb/nexussu/denylist.txt"
    const val ADB_UID = 2000

        init { try { System.loadLibrary("nexus_bridge") } catch (e: UnsatisfiedLinkError) {} }

    external fun registerManager(): Boolean
    external fun grantUidAccess(uid: Int): Boolean
    external fun revokeUidAccess(uid: Int): Boolean
    external fun getEngineVersion(): Int
    external fun escalateSelf(): Boolean
    external fun addDenyUid(uid: Int): Boolean
    external fun removeDenyUid(uid: Int): Boolean
    external fun checkManager(): Boolean // NEW
    external fun resetManager() // NEW

    fun isKernelActive(): Boolean {
        if (getEngineVersion() != 100) return false
        
        // 1. If we are already the manager, we are good.
        if (checkManager()) return true
        
        // 2. Try to register. If it succeeds, we are good.
        if (registerManager()) return true
        
        // 3. Registration failed. This means another app (or a previous install) is registered.
        // If we have root (e.g., we are reinstalling but daemon already gave us root), we can reset.
        if (RootShell.isRootAvailable()) {
            RootShell.execute("/data/adb/nexussu/bin/nexussu_daemon --reset-manager")
            // Try to register again after reset
            return registerManager()
        }
        
        return false
    }

    fun installSuBinary(context: Context): Boolean {
        try {
            if (!registerManager()) return false
            if (!escalateSelf()) return false

            val suAsset = context.assets.open("su.bin")
            val suFile = File(context.filesDir, "su")
            FileOutputStream(suFile).use { output -> suAsset.copyTo(output) }
            suFile.setExecutable(true, false)

            val daemonAsset = context.assets.open("nexussu_daemon")
            val daemonFile = File(context.filesDir, "nexussu_daemon")
            FileOutputStream(daemonFile).use { output -> daemonAsset.copyTo(output) }
            daemonFile.setExecutable(true, false)

            val commands = arrayOf(
                "sh", "-c",
                "mkdir -p /data/adb/nexussu/bin && " +
                "cp ${suFile.absolutePath} /data/adb/nexussu/bin/su && " +
                "cp ${daemonFile.absolutePath} /data/adb/nexussu/bin/nexussu_daemon && " +
                "chmod 0755 /data/adb/nexussu/bin/su && " +
                "chmod 0755 /data/adb/nexussu/bin/nexussu_daemon && " +
                "mount --bind /data/adb/nexussu/bin/su /system/bin/su"
            )
            
            val process = Runtime.getRuntime().exec(commands)
            process.waitFor()
            
            if (process.exitValue() == 0) {
                grantUidAccess(android.os.Process.myUid())
                return true
            }
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install su binary: ${e.message}")
            return false
        }
    }

    fun installBusyBox(context: Context): Boolean {
        try {
            if (!isKernelActive()) return false

            val bbAsset = context.assets.open("busybox.bin")
            val bbFile = File(context.filesDir, "busybox")
            FileOutputStream(bbFile).use { output -> bbAsset.copyTo(output) }
            bbFile.setExecutable(true, false)

            val cmd = "sh -c \"cp ${bbFile.absolutePath} /data/adb/nexussu/bin/busybox && " +
                      "chmod 0755 /data/adb/nexussu/bin/busybox && " +
                      "mount --bind /data/adb/nexussu/bin/busybox /system/bin/busybox && " +
                      "/data/adb/nexussu/bin/busybox --install -s /data/adb/nexussu/bin\""
            
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            process.waitFor()
            return process.exitValue() == 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install busybox: ${e.message}")
            return false
        }
    }

    fun disableRoot(): Boolean {
        val cmd = "umount /system/bin/su 2>/dev/null; umount /system/bin/busybox 2>/dev/null; echo 'SUCCESS'"
        return RootShell.executeBoolean(cmd)
    }

    fun enableRoot(): Boolean {
        val cmd = "mount --bind /data/adb/nexussu/bin/su /system/bin/su 2>/dev/null; " +
                  "mount --bind /data/adb/nexussu/bin/busybox /system/bin/busybox 2>/dev/null; " +
                  "echo 'SUCCESS'"
        return RootShell.executeBoolean(cmd)
    }

    fun applySavedRootGrants() {
        val uids = RootShell.execute("cat $CONFIG_PATH")
        uids.split("\n").forEach { uidStr ->
            val uid = uidStr.trim().toIntOrNull()
            if (uid != null) grantUidAccess(uid)
        }
    }

    fun getGrantedUids(): List<Int> {
        val uids = RootShell.execute("cat $CONFIG_PATH")
        return uids.split("\n").mapNotNull { it.trim().toIntOrNull() }
    }

    fun saveGrantedUid(uid: Int) {
        grantUidAccess(uid)
        RootShell.execute("echo $uid >> $CONFIG_PATH")
    }

    fun grantUidTemporary(uid: Int) {
        grantUidAccess(uid)
    }

    fun removeGrantedUid(uid: Int) {
        revokeUidAccess(uid)
        RootShell.execute("sed -i '/^$uid$/d' $CONFIG_PATH")
    }

    fun clearAllRootGrants() {
        RootShell.execute("rm -f $CONFIG_PATH")
        getGrantedUids().forEach { revokeUidAccess(it) }
    }

    fun setAdbRootEnabled(enabled: Boolean): Boolean {
        return if (enabled) grantUidAccess(ADB_UID) else revokeUidAccess(ADB_UID)
    }

    fun isAdbRootEnabled(): Boolean {
        return getGrantedUids().contains(ADB_UID)
    }

    fun enableSystemlessHosts(): Boolean {
        val cmd = "mkdir -p /data/adb/nexussu && echo '127.0.0.1 localhost' > /data/adb/nexussu/hosts && mount -o bind /data/adb/nexussu/hosts /system/etc/hosts"
        return RootShell.executeBoolean(cmd)
    }

    fun disableSystemlessHosts(): Boolean {
        return RootShell.executeBoolean("umount /system/etc/hosts")
    }

    fun saveDeniedUid(uid: Int) {
        addDenyUid(uid)
        RootShell.execute("echo $uid >> $DENYLIST_PATH")
    }

    fun removeDeniedUid(uid: Int) {
        removeDenyUid(uid)
        RootShell.execute("sed -i '/^$uid$/d' $DENYLIST_PATH")
    }

    fun getDeniedUids(): List<Int> {
        val uids = RootShell.execute("cat $DENYLIST_PATH")
        return uids.split("\n").mapNotNull { it.trim().toIntOrNull() }
    }

        fun getInstalledModules(): List<ModuleItem> {
        val modules = mutableListOf<ModuleItem>()
        val result = RootShell.execute("ls /data/adb/nexussu/modules")
        if (result == "Error" || result.isBlank()) return modules

        result.split("\n").forEach { id ->
            if (id.isNotBlank()) {
                val prop = RootShell.execute("cat /data/adb/nexussu/modules/$id/module.prop")
                val name = prop.substringAfter("name=").substringBefore("\n").ifBlank { id }
                val version = prop.substringAfter("version=").substringBefore("\n").ifBlank { "Unknown" }
                val versionCode = prop.substringAfter("versionCode=").substringBefore("\n").toIntOrNull() ?: 0
                val author = prop.substringAfter("author=").substringBefore("\n").ifBlank { "Unknown" }
                val desc = prop.substringAfter("description=").substringBefore("\n").ifBlank { "No description" }
                val updateJson = prop.substringAfter("updateJson=").substringBefore("\n").ifBlank { "" }
                val isDisabled = RootShell.execute("[ -f /data/adb/nexussu/modules/$id/disable ] && echo 1 || echo 0").trim() == "1"
                
                // NEW: Check if pending removal
                val isPendingRemoval = RootShell.execute("[ -f /data/adb/nexussu/modules/$id/remove ] && echo 1 || echo 0").trim() == "1"
                
                modules.add(ModuleItem(id, name.firstOrNull()?.uppercase() ?: "M", name, version, versionCode, author, desc, updateJson, !isDisabled, isPendingRemoval))
            }
        }
        return modules
    }
    
    fun getActiveModulesCount(): Int {
        val result = RootShell.execute("ls /data/adb/nexussu/modules 2>/dev/null")
        if (result == "Error" || result.isBlank()) return 0
        
        var count = 0
        result.split("\n").forEach { id ->
            if (id.isNotBlank()) {
                val isDisabled = RootShell.execute("[ -f /data/adb/nexussu/modules/$id/disable ] && echo 1 || echo 0").trim() == "1"
                if (!isDisabled) count++
            }
        }
        return count
    }

    fun setModuleEnabled(id: String, enabled: Boolean): Boolean {
        return RootShell.setModuleEnabled(id, enabled)
    }

    fun deleteModule(id: String): Boolean {
        return RootShell.deleteModule(id)
    }

    // NEW: Wrapper for restoring a module
    fun restoreModule(id: String): Boolean {
        return RootShell.restoreModule(id)
    }

    fun getInstallLog(): String {
        return RootShell.execute("cat /data/adb/nexussu/install.log 2>/dev/null || echo 'No log found.'")
    }

    // NEW: Check for module updates via updateJson
    suspend fun checkModuleUpdate(updateJsonUrl: String): ModuleUpdate? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(updateJsonUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val versionCode = json.optInt("versionCode", 0)
                    val zipUrl = json.optString("zipUrl", "")
                    val changelog = json.optString("changelog", "")
                    if (versionCode > 0 && zipUrl.isNotBlank()) {
                        return@withContext ModuleUpdate(versionCode, zipUrl, changelog)
                    }
                }
                null
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class ModuleUpdate(val versionCode: Int, val zipUrl: String, val changelog: String)
