package com.safenet.vpn.data.local.dao

import androidx.room.*
import com.safenet.vpn.data.local.entity.ExternalKeyEntity
import com.safenet.vpn.data.local.entity.KeyEntity
import com.safenet.vpn.data.local.entity.ServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SafeNetDao {
    // Servers
    @Query("SELECT * FROM cached_servers")
    fun getServersFlow(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM cached_servers WHERE id = :id")
    suspend fun getServerById(id: String): ServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServers(servers: List<ServerEntity>)

    @Query("DELETE FROM cached_servers")
    suspend fun clearServers()

    // Keys
    @Query("SELECT * FROM cached_keys")
    fun getKeysFlow(): Flow<List<KeyEntity>>

    @Query("SELECT * FROM cached_keys WHERE id = :id")
    suspend fun getKeyById(id: String): KeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeys(keys: List<KeyEntity>)

    @Query("DELETE FROM cached_keys")
    suspend fun clearKeys()

    // External Keys (user-imported, multi-key support)
    @Query("SELECT * FROM external_keys ORDER BY createdAt DESC")
    fun getExternalKeysFlow(): Flow<List<ExternalKeyEntity>>

    @Query("SELECT * FROM external_keys WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveExternalKey(): ExternalKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExternalKey(key: ExternalKeyEntity)

    @Query("DELETE FROM external_keys WHERE id = :id")
    suspend fun deleteExternalKey(id: String)

    @Query("UPDATE external_keys SET isActive = 0")
    suspend fun clearAllActiveFlags()

    @Query("UPDATE external_keys SET isActive = 1 WHERE id = :id")
    suspend fun setKeyActive(id: String)

    @Transaction
    suspend fun setActiveExternalKey(id: String) {
        clearAllActiveFlags()
        setKeyActive(id)
    }

    @Query("SELECT COUNT(*) FROM external_keys")
    suspend fun getExternalKeyCount(): Int
}

