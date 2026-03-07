package com.francisco.calculadorapedidos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "fuxion_prefs")

class FuxionDataStore(context: Context) {
    private val dataStore = context.dataStore

    companion object {
        // VARIABLE MUTADA: De AnchorDate (Long) a UserName (String)
        val USER_NAME_KEY = stringPreferencesKey("user_name")
        val USER_START_PERIOD_KEY = intPreferencesKey("user_start_period")
    }

    val userNameFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_NAME_KEY]
    }

    val userStartPeriodFlow: Flow<Int?> = dataStore.data.map { preferences ->
        preferences[USER_START_PERIOD_KEY]
    }

    suspend fun saveUserName(name: String) {
        dataStore.edit { preferences -> preferences[USER_NAME_KEY] = name }
    }

    suspend fun saveUserStartPeriod(period: Int) {
        dataStore.edit { preferences -> preferences[USER_START_PERIOD_KEY] = period }
    }

    suspend fun clearData() {
        dataStore.edit { it.clear() }
    }
}