package com.sensorlockdown.app.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sensorlockdown.app.persistence.dao.AuditLogDao
import com.sensorlockdown.app.persistence.dao.PolicyDao
import com.sensorlockdown.app.persistence.entity.AuditLogEntity
import com.sensorlockdown.app.persistence.entity.LockdownStateEntity
import com.sensorlockdown.app.persistence.entity.PolicyControlEntity

@Database(
    entities = [
        AuditLogEntity::class,
        PolicyControlEntity::class,
        LockdownStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LockdownDatabase : RoomDatabase() {

    abstract fun auditLogDao(): AuditLogDao
    abstract fun policyDao(): PolicyDao

    companion object {
        @Volatile
        private var INSTANCE: LockdownDatabase? = null

        fun getInstance(context: Context): LockdownDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LockdownDatabase::class.java,
                    "sensor_lockdown.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

