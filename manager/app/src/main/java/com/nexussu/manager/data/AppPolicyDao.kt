package com.nexussu.manager.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppPolicyDao {
    @Query("SELECT * FROM app_policies") fun getAll(): Flow<List<AppPolicy>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(p: AppPolicy)
}
