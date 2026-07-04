package coms.binitops.nexussu.core

import android.util.Log

object NexusEngine {
    private const val TAG = "NexusEngine"

    // Load the compiled C++ library when this object is initialized
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
     * Returns true if the kernel accepted the ioctl command.
     */
    external fun grantUidAccess(uid: Int): Boolean

    /**
     * Fetches the active engine version from the kernel space.
     * Returns the version code (e.g., 100 for v1.0.0), or -1 if unreachable.
     */
    external fun getEngineVersion(): Int
}
