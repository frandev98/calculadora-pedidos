package com.francisco.calculadorapedidos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "fuxion_prefs")

class FuxionDataStore(context: Context) {
    private val dataStore = context.dataStore

    companion object {
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USER_START_TIMESTAMP_KEY = longPreferencesKey("user_start_timestamp")
    }

    val userNameFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_NAME_KEY]
    }

    val userStartTimestampFlow: Flow<Long?> = dataStore.data.map { preferences ->
        preferences[USER_START_TIMESTAMP_KEY]
    }

    suspend fun saveUserName(name: String) {
        dataStore.edit { preferences -> preferences[USER_NAME_KEY] = name }
    }

    suspend fun saveUserStartTimestamp(timestamp: Long) {
        dataStore.edit { preferences -> preferences[USER_START_TIMESTAMP_KEY] = timestamp }
    }

    suspend fun clearData() {
        dataStore.edit { it.clear() }
    }
}