package com.example.template.feature.main

import com.example.template.domain.main.ObserveGreetingUseCase
import com.example.template.domain.main.SaveGreetingUseCase
import com.example.template.shared.ui.base.BaseMVIViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Demonstrates the full vertical slice: UseCases from `:domain:main`, MVI runtime from
 * `:shared:ui`. Cross-capability orchestration would also happen here — inject two UseCases
 * and combine them, never let one domain module call another.
 *
 * @author WangZhiYao
 * @since 2026/11/11
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeGreeting: ObserveGreetingUseCase,
    private val saveGreeting: SaveGreetingUseCase,
) : BaseMVIViewModel<MainState, MainSideEffect>() {

    /**
     * Getter form on purpose: base-class property initializers run before the subclass
     * assigns its fields, so a field initializer here would capture the not-yet-initialised
     * `null` into the StateFlow and crash the first composition (see README 2.4).
     */
    override val initialState: MainState get() = MainState()

    init {
        // intent { ... } runs in viewModelScope; reduce produces the next state.
        intent {
            observeGreeting().collect { greeting ->
                reduce { copy(greeting = greeting.text) }
            }
        }
    }

    fun onRefreshGreeting() = intent {
        val text = GREETINGS.random()
        saveGreeting(text)
        postSideEffect(MainSideEffect.GreetingSaved(text))
    }

    private companion object {
        val GREETINGS = listOf(
            "Hello, Template!",
            "Clean Architecture, wired end to end.",
            "MVI: data flows one way.",
            "Room -> domain -> UI.",
        )
    }
}
