package com.example.template.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import com.example.template.feature.main.navigation.MainRoute
import com.example.template.feature.main.navigation.mainScreen

/**
 *
 *
 * @author WangZhiYao
 * @since 2025/11/17
 */
@Composable
fun TemplateNavHost(
    appState: TemplateAppState,
    modifier: Modifier = Modifier,
) {

    val navController = appState.navController

    NavHost(
        navController = navController,
        startDestination = MainRoute,
        modifier = modifier,
    ) {
        mainScreen()
    }
}