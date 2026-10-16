package com.sensorlockdown.app.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sensorlockdown.app.persistence.entity.LockdownStateEntity
import com.sensorlockdown.app.persistence.entity.PolicyControlEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PolicyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateControls(controls: List<PolicyControlEntity>)

    @Query("SELECT * FROM policy_controls")
    fun getAllControls(): Flow<List<PolicyControlEntity>>

    @Query("SELECT * FROM policy_controls")
    suspend fun getAllControlsSnapshot(): List<PolicyControlEntity>

    @Query("SELECT * FROM policy_controls WHERE controlId = :controlId LIMIT 1")
    suspend fun getControl(controlId: String): PolicyControlEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLockdownState(state: LockdownStateEntity)

    @Query("SELECT * FROM lockdown_state WHERE id = 1 LIMIT 1")
    fun getLockdownState(): Flow<LockdownStateEntity?>

    @Query("SELECT * FROM lockdown_state WHERE id = 1 LIMIT 1")
    suspend fun getLockdownStateSnapshot(): LockdownStateEntity?
}

