package com.nexussu.manager.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "granted_apps")
data class GrantedAppEntity(
    @PrimaryKey val uid: Int,
    val packageName: String,
    val appName: String,
    val isRootGranted: Boolean,
    val isExcluded: Boolean
)

@Dao
interface AppDao {
    @Query("SELECT * FROM granted_apps")
    fun getAllGrantedApps(): Flow<List<GrantedAppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateApp(app: GrantedAppEntity)

    @Query("DELETE FROM granted_apps WHERE uid = :uid")
    suspend fun removeApp(uid: Int)
}

@Database(entities = [GrantedAppEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nexussu_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
