package com.example.template.shared.ui.base

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 *
 *
 * @author WangZhiYao
 * @since 2025/9/12
 */
abstract class BaseAndroidMVIViewModel<STATE : Any, SIDE_EFFECT : Any>(
    application: Application
) : AndroidViewModel(application), MVIContainer<STATE, SIDE_EFFECT> {

    protected abstract val initialState: STATE

    private val _state: MutableStateFlow<STATE> = MutableStateFlow(initialState)
    override val state: StateFlow<STATE> = _state.asStateFlow()

    private val _sideEffect: MutableSharedFlow<SIDE_EFFECT> = MutableSharedFlow()
    override val sideEffect: SharedFlow<SIDE_EFFECT> = _sideEffect.asSharedFlow()

    private val intentContext = IntentContextImpl()

    override fun intent(
        action: suspend IntentContext<STATE, SIDE_EFFECT>.() -> Unit
    ): Job = viewModelScope.launch {
        intentContext.action()
    }

    private inner class IntentContextImpl : IntentContext<STATE, SIDE_EFFECT> {
        override val state: STATE
            get() = _state.value

        override fun reduce(reducer: (STATE) -> STATE) {
            _state.update(reducer)
        }

        override suspend fun postSideEffect(sideEffect: SIDE_EFFECT) {
            _sideEffect.emit(sideEffect)
        }
    }
}