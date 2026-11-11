package com.example.template.domain.main

import kotlinx.coroutines.flow.Flow

/**
 * Contract of the "main" capability. Implemented by `:data:main`; features only ever see
 * this interface.
 *
 * @author WangZhiYao
 * @since 2026/11/11
 */
interface GreetingRepository {

    fun observe(): Flow<Greeting>

    suspend fun save(text: String)
}
