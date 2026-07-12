package com.nexussu.manager.core

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object NexusEngine {
    private const val TAG = "NexusEngine"

    init {
        try {
            System.loadLibrary("nexus_bridge")
            Log.d(TAG, "NexusSU C++ Bridge loaded successfully.")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load nexus_bridge: ${e.message}")
        }
    }

    external fun grantUidAccess(uid: Int): Boolean
    external fun getEngineVersion(): Int
    external fun escalateSelf(): Boolean

    fun isKernelActive(): Boolean = getEngineVersion() == 100

    /**
     * REAL ROOT: Extracts the real su binary from the APK assets and bind-mounts it
     * to /system/bin/su so other apps can find it.
     */
    fun installSuBinary(context: Context): Boolean {
        try {
            // 1. Escalate OURSELVES to root using the kernel IOCTL
            if (!escalateSelf()) {
                Log.e(TAG, "Self-escalation failed! Kernel not patched or IOCTL blocked.")
                return false
            }

            // 2. Extract the real su binary from the app's assets
            val suAsset = context.assets.open("su.bin")
            val suFile = File(context.filesDir, "su")
            FileOutputStream(suFile).use { output -> suAsset.copyTo(output) }
            suFile.setExecutable(true, false)

            // 3. We are now UID 0. Use raw Linux commands to mount and copy it to the system path.
            // We use "sh -c" so we can string multiple commands together with the elevated permissions.
            val commands = arrayOf(
                "sh", "-c",
                "mount -o rw,remount /system && " +
                "cp ${suFile.absolutePath} /system/bin/su && " +
                "chmod 0755 /system/bin/su && " +
                "mount -o ro,remount /system"
            )
            
            val process = Runtime.getRuntime().exec(commands)
            process.waitFor()
            
            if (process.exitValue() == 0) {
                Log.i(TAG, "Real su binary successfully mounted to /system/bin/su")
                return true
            } else {
                Log.e(TAG, "Failed to mount su binary. Exit code: ${process.exitValue()}")
                return false
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to install real su binary: ${e.message}")
            return false
        }
    }
}
