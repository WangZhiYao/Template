package com.example.template.feature.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.template.shared.ui.ext.collectSideEffect
import com.example.template.shared.ui.ext.collectState

/**
 * Stateful route: owns the ViewModel, collects state and one-shot side effects.
 *
 * @author WangZhiYao
 * @since 2025/11/17
 */
@Composable
fun MainRoute(
    viewModel: MainViewModel = hiltViewModel(),
) {
    val state by viewModel.collectState()
    val snackbarHostState = remember { SnackbarHostState() }

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is MainSideEffect.GreetingSaved -> snackbarHostState.showSnackbar(sideEffect.text)
        }
    }

    MainScreen(
        greeting = state.greeting,
        onRefreshGreeting = viewModel::onRefreshGreeting,
        snackbarHostState = snackbarHostState,
    )
}

/**
 * Stateless screen: no ViewModel access, parameters only.
 */
@Composable
private fun MainScreen(
    greeting: String,
    onRefreshGreeting: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SnackbarHost(hostState = snackbarHostState)

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRefreshGreeting) {
            Text(text = "Refresh greeting")
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
