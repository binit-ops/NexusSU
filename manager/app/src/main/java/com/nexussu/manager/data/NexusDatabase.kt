package com.nexussu.manager.data
import androidx.room.*
@Database(entities = [AppPolicy::class], version = 1)
abstract class NexusDatabase : RoomDatabase() { abstract fun dao(): AppPolicyDao }
