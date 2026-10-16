package com.sensorlockdown.app.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lockdown_state")
data class LockdownStateEntity(
    @PrimaryKey
    val id: Int = 1,
    val isActive: Boolean,
    val policyVersion: Int,
    val lastActivatedAt: Long?,
    val overallStatus: String
)

