package com.voidDeveloper.wastatussaver.data.protodatastoremanager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.voidDeveloper.wastatussaver.data.datastore.proto.StatusMedia
import com.voidDeveloper.wastatussaver.data.datastore.proto.StatusMediaList
import com.voidDeveloper.wastatussaver.data.serializers.StatusMediaListSerializer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatusMediaProtoDataStoreManager @Inject constructor(@ApplicationContext val context: Context) {

    private val Context.statusMediaDataStore: DataStore<StatusMediaList> by dataStore(
        fileName = "status_media.pb",
        serializer = StatusMediaListSerializer
    )

    suspend fun readStatusMedia(): List<StatusMedia> {
        return context.statusMediaDataStore.data.firstOrNull()?.itemsList.orEmpty()
    }

    suspend fun writeStatusMedia(statusMediaList: List<StatusMedia>) {
        context.statusMediaDataStore.updateData { currentList ->
            currentList.toBuilder()
                .clearItems()
                .addAllItems(statusMediaList)
                .build()
        }
    }

}