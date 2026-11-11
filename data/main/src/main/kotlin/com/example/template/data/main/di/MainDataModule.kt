package com.example.template.data.main.di

import com.example.template.data.main.GreetingRepositoryImpl
import com.example.template.domain.main.GreetingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the domain contract to its implementation. Aggregated by Hilt into the component
 * generated in `:app`, which is why `:app` must depend on this module (see README 2.4).
 *
 * @author WangZhiYao
 * @since 2026/11/11
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MainDataModule {

    @Binds
    abstract fun bindGreetingRepository(impl: GreetingRepositoryImpl): GreetingRepository
}
