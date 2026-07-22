package com.ofa.vpn.data.local
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ServerEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    companion object { @Volatile private var INSTANCE: AppDatabase? = null; fun getDatabase(context: Context): AppDatabase { return INSTANCE ?: synchronized(this) { Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "ofa_vpn_db").build() } } }
}