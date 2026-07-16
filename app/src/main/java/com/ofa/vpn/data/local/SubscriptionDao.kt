package com.ofa.vpn.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ofa.vpn.data.model.Subscription
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: Subscription): Long

    @Update
    suspend fun update(subscription: Subscription)

    @Delete
    suspend fun delete(subscription: Subscription)

    @Query("SELECT * FROM subscriptions ORDER BY id ASC")
    fun getAll(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE isEnabled = 1")
    suspend fun getEnabled(): List<Subscription>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getById(id: Long): Subscription?

    @Query("UPDATE subscriptions SET lastUpdate = :ts, serverCount = :count WHERE id = :id")
    suspend fun updateRefresh(id: Long, ts: Long, count: Int)

    @Query("UPDATE subscriptions SET isEnabled = :enabled WHERE id = :id")
    suspend fun toggleEnabled(id: Long, enabled: Boolean)

    @Query("SELECT COUNT(*) FROM subscriptions")
    suspend fun count(): Int
}
