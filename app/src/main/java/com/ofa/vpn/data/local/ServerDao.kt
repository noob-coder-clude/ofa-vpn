package com.ofa.vpn.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ofa.vpn.data.model.Server
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(servers: List<Server>)

    @Update
    suspend fun update(server: Server)

    @Query("SELECT * FROM servers ORDER BY ping ASC")
    fun getAllServers(): Flow<List<Server>>

    @Query("SELECT * FROM servers WHERE isFavorite = 1 ORDER BY ping ASC")
    fun getFavorites(): Flow<List<Server>>

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getById(id: Long): Server?

    @Query("SELECT * FROM servers WHERE subscriptionId = :subId")
    suspend fun getBySubscription(subId: Long): List<Server>

    @Query("UPDATE servers SET ping = :ping WHERE id = :id")
    suspend fun updatePing(id: Long, ping: Int)

    @Query("UPDATE servers SET isFavorite = :fav WHERE id = :id")
    suspend fun updateFavorite(id: Long, fav: Boolean)

    @Query("UPDATE servers SET lastConnected = :ts WHERE id = :id")
    suspend fun updateLastConnected(id: Long, ts: Long)

    @Query("DELETE FROM servers WHERE subscriptionId = :subId")
    suspend fun deleteBySubscription(subId: Long)

    @Query("SELECT COUNT(*) FROM servers")
    suspend fun count(): Int
}
