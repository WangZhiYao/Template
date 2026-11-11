package com.example.template.domain.main

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Emits the persisted greeting whenever it changes.
 *
 * @author WangZhiYao
 * @since 2026/11/11
 */
class ObserveGreetingUseCase @Inject constructor(
    private val repository: GreetingRepository,
) {

    operator fun invoke(): Flow<Greeting> = repository.observe()
}
