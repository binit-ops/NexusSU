package com.nexussu.manager.core

import android.content.Context
import android.util.Log
import com.nexussu.manager.ui.ModuleItem
import java.io.File
import java.io.FileOutputStream

object NexusEngine {
    private const val TAG = "NexusEngine"
    private const val CONFIG_PATH = "/data/adb/nexussu/granted_uids.txt"

    init {
        try { System.loadLibrary("nexus_bridge") } catch (e: UnsatisfiedLinkError) {}
    }

    external fun grantUidAccess(uid: Int): Boolean
    external fun revokeUidAccess(uid: Int): Boolean // NEW
    external fun getEngineVersion(): Int
    external fun escalateSelf(): Boolean

    fun isKernelActive(): Boolean = getEngineVersion() == 100

    fun installSuBinary(context: Context): Boolean {
        if (!escalateSelf()) return false
        try {
            val suAsset = context.assets.open("su.bin")
            val suFile = File(context.filesDir, "su")
            FileOutputStream(suFile).use { output -> suAsset.copyTo(output) }
            suFile.setExecutable(true, false)
            val cmd = "mount -o rw,remount /system && cp ${suFile.absolutePath} /system/bin/su && chmod 0755 /system/bin/su && mount -o ro,remount /system"
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            process.waitFor()
            if (process.exitValue() == 0) {
                grantUidAccess(android.os.Process.myUid()) // Grant manager root
                return true
            }
        } catch (e: Exception) {}
        return false
    }

    // REAL PERSISTENCE: Re-apply saved UIDs on boot
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

    fun enableSystemlessHosts(): Boolean {
        val cmd = "mkdir -p /data/adb/nexussu && echo '127.0.0.1 localhost' > /data/adb/nexussu/hosts && mount -o bind /data/adb/nexussu/hosts /system/etc/hosts"
        return RootShell.executeBoolean(cmd)
    }

    fun disableSystemlessHosts(): Boolean {
        return RootShell.executeBoolean("umount /system/etc/hosts")
    }

    // REAL MODULES: Read installed modules from the filesystem
    fun getInstalledModules(): List<ModuleItem> {
        val modules = mutableListOf<ModuleItem>()
        val result = RootShell.execute("ls /data/adb/nexussu/modules")
        if (result == "Error" || result.isBlank()) return modules

        result.split("\n").forEach { id ->
            if (id.isNotBlank()) {
                val prop = RootShell.execute("cat /data/adb/nexussu/modules/$id/module.prop")
                val name = prop.substringAfter("name=").substringBefore("\n").ifBlank { id }
                val desc = prop.substringAfter("description=").substringBefore("\n").ifBlank { "No description" }
                modules.add(ModuleItem(name.firstOrNull()?.uppercase() ?: "M", name, desc, true))
            }
        }
        return modules
    }
}
