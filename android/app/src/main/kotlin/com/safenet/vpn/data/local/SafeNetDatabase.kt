package com.safenet.vpn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.safenet.vpn.data.local.dao.SafeNetDao
import com.safenet.vpn.data.local.entity.ExternalKeyEntity
import com.safenet.vpn.data.local.entity.KeyEntity
import com.safenet.vpn.data.local.entity.ServerEntity

@Database(
    entities = [ServerEntity::class, KeyEntity::class, ExternalKeyEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class SafeNetDatabase : RoomDatabase() {
    abstract fun safeNetDao(): SafeNetDao

    companion object {
        /** Adds the external_keys table introduced in version 2. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `external_keys` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `connectedServerId` TEXT,
                        `connectedServerName` TEXT,
                        `isActive` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}

