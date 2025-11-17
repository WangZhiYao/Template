package com.example.template.shared.ui.ext

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import com.example.template.shared.ui.base.MVIContainer


/**
 * Observe [MVIContainer.uiState] as [State].
 *
 * @param lifecycleState The minimum lifecycle state at which the state is observed.
 *
 * @author WangZhiYao
 * @since 2025/11/17
 */
@Composable
fun <STATE : Any, SIDE_EFFECT : Any> MVIContainer<STATE, SIDE_EFFECT>.collectState(
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED
): State<STATE> {
    return this.uiState.collectAsStateWithLifecycle(minActiveState = lifecycleState)
}

/**
 * Observe [MVIContainer.sideEffect] in a Compose [LaunchedEffect].
 *
 * @param lifecycleState [Lifecycle.State] in which side effects are collected.
 */
@SuppressLint("ComposableNaming")
@Composable
fun <STATE : Any, SIDE_EFFECT : Any> MVIContainer<STATE, SIDE_EFFECT>.collectSideEffect(
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    sideEffect: (suspend (sideEffect: SIDE_EFFECT) -> Unit)
) {
    val sideEffectFlow = this.sideEffect
    val lifecycleOwner = LocalLifecycleOwner.current

    val callback by rememberUpdatedState(newValue = sideEffect)

    LaunchedEffect(key1 = sideEffectFlow, key2 = lifecycleOwner) {
        sideEffectFlow
            .flowWithLifecycle(lifecycleOwner.lifecycle, lifecycleState)
            .collect { callback(it) }
    }
}