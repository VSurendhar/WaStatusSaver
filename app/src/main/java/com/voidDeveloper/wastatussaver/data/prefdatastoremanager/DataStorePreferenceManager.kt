package com.voidDeveloper.wastatussaver.data.prefdatastoremanager

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow

interface DataStorePreferenceManager {
    fun<T> getPreference(key : Preferences.Key<T>, defaultValue: T) : Flow<T>
    suspend fun<T> putPreference(key : Preferences.Key<T>,value : T)
    suspend fun<T> removePreference(key : Preferences.Key<T>)
    suspend fun clearAllPreferences()
}