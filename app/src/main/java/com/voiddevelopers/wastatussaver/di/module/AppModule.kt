package com.voiddevelopers.wastatussaver.di.module

import android.content.Context
import com.voiddevelopers.wastatussaver.BuildConfig
import com.voiddevelopers.wastatussaver.core.analytics.MixpanelAnalytics
import com.voiddevelopers.wastatussaver.data.prefdatastoremanager.DataStorePreferenceManager
import com.voiddevelopers.wastatussaver.data.prefdatastoremanager.DataStorePreferenceManagerImpl
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
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStorePreferenceManager =
        DataStorePreferenceManagerImpl(context)

    @Provides
    @Singleton
    fun provideMixpanelAnalytics(@ApplicationContext context: Context): MixpanelAnalytics =
        MixpanelAnalytics(context, BuildConfig.MIXPANEL_TOKEN)
}
