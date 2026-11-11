package com.example.template.shared.ui.base

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Initialization-order contract: base-class property initializers run before the subclass
 * assigns its fields, so `initialState` MUST be overridden with the getter form
 * (`get() = ...`). A field with an initializer would capture the JVM default (`null`) into
 * the StateFlow and crash the first render (see README 2.4).
 */
class BaseMVIViewModelTest {

    private class TestViewModel : BaseMVIViewModel<String, String>() {
        override val initialState: String get() = "initial"
    }

    @Test
    fun `initial state is available right after construction`() {
        val viewModel = TestViewModel()

        assertEquals("initial", viewModel.uiState.value)
    }
}
