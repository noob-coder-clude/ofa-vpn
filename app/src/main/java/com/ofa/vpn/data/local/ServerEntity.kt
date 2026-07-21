package com.ofa.vpn.data.local
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "servers")
data class ServerEntity(@PrimaryKey(autoGenerate = true) val id: Int = 0, val name: String, val protocol: String, val address: String, val port: Int, val uuid: String, val rawUri: String)