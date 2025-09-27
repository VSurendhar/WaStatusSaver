package com.voidDeveloper.wastatussaver.di.module

import android.content.Context
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStoreManager
import com.voidDeveloper.wastatussaver.data.datastoremanager.DataStorePreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AppModule {

    @Provides
    @Singleton
    fun provideDataStoreManager(
        @ApplicationContext context: Context,
    ): DataStorePreferenceManager {
        return DataStoreManager(context)
    }

}