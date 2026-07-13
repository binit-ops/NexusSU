package com.nexussu.manager.core

import android.content.Context
import android.util.Log
import com.nexussu.manager.ui.ModuleItem
import java.io.File
import java.io.FileOutputStream

object NexusEngine {
    private const val TAG = "NexusEngine"
    private const val CONFIG_PATH = "/data/adb/nexussu/granted_uids.txt"
    const val ADB_UID = 2000 // Standard Android ADB Shell UID

    init { try { System.loadLibrary("nexus_bridge") } catch (e: UnsatisfiedLinkError) {} }

    external fun registerManager(): Boolean
    external fun grantUidAccess(uid: Int): Boolean
    external fun revokeUidAccess(uid: Int): Boolean
    external fun getEngineVersion(): Int
    external fun escalateSelf(): Boolean

    fun isKernelActive(): Boolean {
        if (getEngineVersion() != 100) return false
        return registerManager()
    }

    fun installSuBinary(context: Context): Boolean {
        try {
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

    fun removeGrantedUid(uid: Int) {
        revokeUidAccess(uid)
        RootShell.execute("sed -i '/^$uid$/d' $CONFIG_PATH")
    }

    fun clearAllRootGrants() {
        RootShell.execute("rm -f $CONFIG_PATH")
        getGrantedUids().forEach { revokeUidAccess(it) }
    }

    // NEW: ADB Root Toggle
    fun setAdbRootEnabled(enabled: Boolean): Boolean {
        return if (enabled) {
            grantUidAccess(ADB_UID)
        } else {
            revokeUidAccess(ADB_UID)
        }
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

    fun getInstalledModules(): List<ModuleItem> {
        val modules = mutableListOf<ModuleItem>()
        val result = RootShell.execute("ls /data/adb/nexussu/modules")
        if (result == "Error" || result.isBlank()) return modules

        result.split("\n").forEach { id ->
            if (id.isNotBlank()) {
                val prop = RootShell.execute("cat /data/adb/nexussu/modules/$id/module.prop")
                val name = prop.substringAfter("name=").substringBefore("\n").ifBlank { id }
                val desc = prop.substringAfter("description=").substringBefore("\n").ifBlank { "No description" }
                val isDisabled = RootShell.execute("[ -f /data/adb/nexussu/modules/$id/disable ] && echo 1 || echo 0").trim() == "1"
                modules.add(ModuleItem(id, name.firstOrNull()?.uppercase() ?: "M", name, desc, !isDisabled))
            }
        }
        return modules
    }

    fun setModuleEnabled(id: String, enabled: Boolean): Boolean {
        return RootShell.setModuleEnabled(id, enabled)
    }
}
