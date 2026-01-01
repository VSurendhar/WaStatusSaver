package com.voidDeveloper.wastatussaver.data.protodatastoremanager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.voidDeveloper.wastatussaver.data.datastore.proto.AutoSaveUserPref
import com.voidDeveloper.wastatussaver.data.serializers.AutoSaveSerializer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoSaveProtoDataStoreManager @Inject constructor(@ApplicationContext val context: Context) {

    private val Context.autoSaveDataStore: DataStore<AutoSaveUserPref> by dataStore(
        fileName = "auto_save.pb",
        serializer = AutoSaveSerializer
    )

    suspend fun readAutoSaveUserPref(): AutoSaveUserPref {
        return context.autoSaveDataStore.data.firstOrNull()
            ?: AutoSaveUserPref.getDefaultInstance()
    }

    suspend fun updateAutoSaveUserPref(
        update: (AutoSaveUserPref.Builder) -> Unit
    ) {
        context.autoSaveDataStore.updateData { current ->
            current.toBuilder()
                .apply(update)
                .build()
        }
    }

}