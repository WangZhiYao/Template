package com.example.template.domain.main

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Domain modules are pure Kotlin, so UseCases are tested on the JVM against a fake
 * repository — no Robolectric, no mocks.
 */
class ObserveGreetingUseCaseTest {

    @Test
    fun `emits the repository value`() = runTest {
        val useCase = ObserveGreetingUseCase(FakeGreetingRepository(Greeting("Hello, Template!")))

        assertEquals(Greeting("Hello, Template!"), useCase().first())
    }

    @Test
    fun `emits the new value after save`() = runTest {
        val repository = FakeGreetingRepository(Greeting("old"))
        val observe = ObserveGreetingUseCase(repository)
        val save = SaveGreetingUseCase(repository)

        save("new")
        advanceUntilIdle()

        assertEquals(Greeting("new"), observe().first())
    }

    private class FakeGreetingRepository(initial: Greeting) : GreetingRepository {
        private val greeting = MutableStateFlow(initial)

        override fun observe(): Flow<Greeting> = greeting

        override suspend fun save(text: String) {
            greeting.update { Greeting(text) }
        }
    }
}
