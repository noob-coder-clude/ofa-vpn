package com.ofa.vpn.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ofa_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        val SUBSCRIPTION_URL = stringPreferencesKey("subscription_url")
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DARK_THEME] ?: true
    }

    val autoConnect: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_CONNECT] ?: false
    }

    val subscriptionUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.SUBSCRIPTION_URL] ?: ""
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setAutoConnect(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_CONNECT] = enabled }
    }

    suspend fun setSubscriptionUrl(url: String) {
        context.dataStore.edit { it[Keys.SUBSCRIPTION_URL] = url }
    }
}
