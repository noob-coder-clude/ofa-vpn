package com.ofa.vpn.di

import android.content.Context
import com.ofa.vpn.core.update.UpdateManager
import com.ofa.vpn.data.local.AppDatabase
import com.ofa.vpn.data.local.ServerDao
import com.ofa.vpn.data.local.SubscriptionDao
import com.ofa.vpn.data.remote.SubFetcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideSubFetcher(client: OkHttpClient): SubFetcher = SubFetcher(client)

    @Provides
    @Singleton
    fun provideUpdateManager(@ApplicationContext context: Context): UpdateManager =
        UpdateManager(context, provideOkHttpClient())
}

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
