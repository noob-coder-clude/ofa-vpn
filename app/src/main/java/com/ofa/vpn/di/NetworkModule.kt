package com.ofa.vpn.di

import com.ofa.vpn.data.remote.SubFetcher
import com.ofa.vpn.data.local.ServerDao
import com.ofa.vpn.data.local.SubscriptionDao
import com.ofa.vpn.di.NetworkModule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
}