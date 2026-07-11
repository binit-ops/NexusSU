package com.nexussu.manager.core

import android.util.Log

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

    /**
     * Sends the target UID to the kernel to grant superuser access.
     */
    external fun grantUidAccess(uid: Int): Boolean

    /**
     * Fetches the active engine version from the kernel space.
     */
    external fun getEngineVersion(): Int

    /**
     * Returns true if the NexusSU kernel engine is active and running.
     */
    fun isKernelActive(): Boolean {
        // We expect version 100 based on the kernel's ENGINE_VERSION constant
        return getEngineVersion() == 100
    }
}
