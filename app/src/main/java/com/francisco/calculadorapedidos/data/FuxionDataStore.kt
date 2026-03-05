package com.francisco.calculadorapedidos.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extensión para crear el DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fuxion_prefs")

class FuxionDataStore(private val context: Context) {

    companion object {
        val KEY_ANCHOR_DATE = longPreferencesKey("anchor_date_p1")
        // Declaración de Llave Estricta para Desfase de Matriz
        val USER_START_PERIOD = intPreferencesKey("user_start_period")
    }

    // --- ANCHOR DATE (Inicio P1) ---
    val anchorDateFlow: Flow<Long?> = context.dataStore.data
        .map { preferences ->
            preferences[KEY_ANCHOR_DATE]
        }

    suspend fun saveAnchorDate(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ANCHOR_DATE] = timestamp
        }
    }

    // --- COORDENADA DE DESFASE (MATRIZ RELATIVA) ---
    val userStartPeriodFlow: Flow<Int?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_START_PERIOD]
        }

    suspend fun saveUserStartPeriod(period: Int) {
        context.dataStore.edit { preferences ->
            preferences[USER_START_PERIOD] = period
        }
    }

    // --- METAS POR PERIODO ---
    fun getGoalForPeriod(period: Int): Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[intPreferencesKey("goal_p$period")] ?: 540 // Default 540 (Base)
        }

    suspend fun saveGoalForPeriod(period: Int, goal: Int) {
        context.dataStore.edit { preferences ->
            preferences[intPreferencesKey("goal_p$period")] = goal
        }
    }

    // --- PURGA DE DATOS ---
    suspend fun clearData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}