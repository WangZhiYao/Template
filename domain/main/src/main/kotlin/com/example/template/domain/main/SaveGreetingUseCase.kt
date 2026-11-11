package com.example.template.domain.main

import javax.inject.Inject

/**
 * Persists a new greeting.
 *
 * @author WangZhiYao
 * @since 2026/11/11
 */
class SaveGreetingUseCase @Inject constructor(
    private val repository: GreetingRepository,
) {

    suspend operator fun invoke(text: String) = repository.save(text)
}
