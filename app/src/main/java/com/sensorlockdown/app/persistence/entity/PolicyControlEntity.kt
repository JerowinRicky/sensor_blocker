package com.sensorlockdown.app.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "policy_controls")
data class PolicyControlEntity(
    @PrimaryKey
    val controlId: String,
    val category: String,
    val requestedState: Boolean,
    val enforcedState: Boolean,
    val status: String,
    val reason: String?,
    val lastVerifiedAt: Long
)

