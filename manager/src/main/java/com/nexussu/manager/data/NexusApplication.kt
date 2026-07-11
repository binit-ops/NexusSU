package com.nexussu.manager

import android.app.Application
import android.util.Log
import com.nexussu.manager.core.NexusEngine
import com.nexussu.manager.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NexusApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Push the database to the kernel on startup
        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.getDatabase(this@NexusApplication).appDao()
            
            // We collect once on boot to populate the kernel
            dao.getAllGrantedApps().collect { savedApps ->
                var successCount = 0
                for (app in savedApps) {
                    if (app.isRootGranted) {
                        if (NexusEngine.grantUidAccess(app.uid)) {
                            successCount++
                        }
                    }
                }
                Log.i("NexusSU", "Boot initialization complete. Granted root to $successCount apps.")
            }
        }
    }
}
