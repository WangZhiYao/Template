package com.example.template.shared.ui.base

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 *
 *
 * @author WangZhiYao
 * @since 2025/9/27
 */
interface MVIContainer<STATE : Any, SIDE_EFFECT : Any> {

    val uiState: StateFlow<STATE>

    val sideEffect: SharedFlow<SIDE_EFFECT>

    fun intent(action: suspend IntentContext<STATE, SIDE_EFFECT>.() -> Unit): Job
}

/**
 * Provides a context for intent execution.
 * This context allows mutation of state and posting of side effects.
 */
interface IntentContext<STATE : Any, SIDE_EFFECT : Any> {

    /**
     * The current state at the time of intent execution.
     */
    val state: STATE

    /**
     * Reduce the current state to a new state.
     *
     * @param reducer a lambda that receives the current state and returns a new state.
     */
    fun reduce(reducer: STATE.() -> STATE)

    /**
     * Post a side effect to be handled by the UI.
     *
     * @param sideEffect the side effect to be posted.
     */
    suspend fun postSideEffect(sideEffect: SIDE_EFFECT)
}