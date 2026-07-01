package com.nexussu.manager.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "app_policies")
data class AppPolicy(@PrimaryKey val uid: Int, val appName: String, val isGranted: Boolean)
