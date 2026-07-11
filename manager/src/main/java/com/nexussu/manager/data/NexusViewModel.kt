package com.nexussu.manager.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexussu.manager.core.NexusEngine
import com.nexussu.manager.data.AppDatabase
import com.nexussu.manager.data.GrantedAppEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NexusViewModel(application: Application) : AndroidViewModel(application) {
    private val appDao = AppDatabase.getDatabase(application).appDao()

    // This Flow automatically updates the UI whenever the database changes
    val savedApps = appDao.getAllGrantedApps()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun toggleRootAccess(app: GrantedApp, isGranted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isGranted) {
                // 1. Tell the Kernel
                val success = NexusEngine.grantUidAccess(app.uid)
                if (success) {
                    // 2. Save to Database
                    appDao.insertOrUpdateApp(
                        GrantedAppEntity(app.uid, app.packageName, app.name, true, app.excludeMod)
                    )
                }
            } else {
                // Remove from Database (You'll need a C++ revoke function eventually)
                appDao.removeApp(app.uid)
            }
        }
    }

    fun toggleExclusion(app: GrantedApp, isExcluded: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            appDao.insertOrUpdateApp(
                GrantedAppEntity(app.uid, app.packageName, app.name, app.toggledOn, isExcluded)
            )
        }
    }
}
