package com.sensorlockdown.app.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Immutable audit event entry stored locally in Room.
 * Plaintext PINs and private keys are strictly forbidden from being stored here.
 */
@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val actor: String,
    val details: String,
    val isSuccess: Boolean
)

