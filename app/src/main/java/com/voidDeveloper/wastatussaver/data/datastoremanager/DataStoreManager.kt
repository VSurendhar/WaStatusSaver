package com.voidDeveloper.wastatussaver.data.datastoremanager

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreManager(private val context: Context) : DataStorePreferenceManager {

    companion object DataStoreKeys {
        val KEY_SHOULD_SHOW_ONBOARDING_UI = booleanPreferencesKey("key.should.show.onboarding.ui")
        val KEY_PREFERRED_TITLE = stringPreferencesKey("key.preferred.title")
        val KEY_AUTO_SAVE_INTERVAL = intPreferencesKey("key.auto.save.interval")
        val LAST_ALARM_SET_MILLIS_KEY = longPreferencesKey("key.last.alarm.set.millis")
        val USER_PREF_WIDGET_REFRESH_INTERVAL_KEY = intPreferencesKey("user.pref.widget.refresh.interval")
    }

    private val preferenceName = "WhatsappAppStatusSaver"
    private val Context.dataStore by preferencesDataStore(name = preferenceName)

    override fun <T> getPreference(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: defaultValue
        }
    }

    override suspend fun <T> putPreference(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    override suspend fun <T> removePreference(key: Preferences.Key<T>) {
        context.dataStore.edit { preferences ->
            preferences.remove(key)
        }
    }

    override suspend fun clearAllPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}