package com.voidDeveloper.wastatussaver.di.module


import com.voidDeveloper.wastatussaver.domain.repo.main.MainRepo
import com.voidDeveloper.wastatussaver.domain.repo.main.MainRepoImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMainRepository(
        impl: MainRepoImpl,
    ): MainRepo

}