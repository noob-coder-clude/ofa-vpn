package com.ofa.vpn.di

import android.content.Context
import com.ofa.vpn.data.local.AppDatabase
import com.ofa.vpn.data.local.ServerDao
import com.ofa.vpn.data.local.SubscriptionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideServerDao(db: AppDatabase): ServerDao = db.serverDao()

    @Provides
    @Singleton
    fun provideSubscriptionDao(db: AppDatabase): SubscriptionDao = db.subscriptionDao()
}
