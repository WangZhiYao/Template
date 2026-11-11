package com.example.template.feature.main

/**
 * One-shot events of the main screen; emitted through
 * [com.example.template.shared.ui.base.MVIContainer.sideEffect].
 *
 * @author WangZhiYao
 * @since 2026/11/11
 */
sealed interface MainSideEffect {

    data class GreetingSaved(val text: String) : MainSideEffect
}
