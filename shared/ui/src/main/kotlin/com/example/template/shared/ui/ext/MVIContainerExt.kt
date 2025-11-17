package com.example.template.shared.ui.ext

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.template.shared.ui.base.MVIContainer
import kotlinx.coroutines.launch

/**
 * Observe [MVIContainer.uiState] and [MVIContainer.sideEffect] correctly on Android in one-line of code.
 * These streams are observed when the view is in [Lifecycle.State.STARTED].
 *
 * In Activities, call from onCreate, where viewModel is a [MVIContainer]:
 *
 * ```
 * viewModel.observe(this, state = ::state, sideEffect = ::sideEffect)
 * ```
 *
 * In Fragments, call from onViewCreated, where viewModel is a [MVIContainer]:
 *
 * ```
 * viewModel.observe(viewLifecycleOwner, state = ::state, sideEffect = ::sideEffect)
 * ```
 *
 * @author WangZhiYao
 * @since 2025/11/16
 */
fun <STATE : Any, SIDE_EFFECT : Any> MVIContainer<STATE, SIDE_EFFECT>.observe(
    lifecycleOwner: LifecycleOwner,
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    state: (suspend (state: STATE) -> Unit)? = null,
    sideEffect: (suspend (sideEffect: SIDE_EFFECT) -> Unit)? = null
) {
    lifecycleOwner.lifecycleScope.launch {
        // See https://medium.com/androiddevelopers/a-safer-way-to-collect-flows-from-android-uis-23080b1f8bda
        lifecycleOwner.lifecycle.repeatOnLifecycle(lifecycleState) {
            state?.let { launch { this@observe.uiState.collect { state(it) } } }
            sideEffect?.let { launch { this@observe.sideEffect.collect { sideEffect(it) } } }
        }
    }
}