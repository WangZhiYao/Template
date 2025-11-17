package com.example.template.feature.main.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.example.template.feature.main.MainRoute
import kotlinx.serialization.Serializable

/**
 *
 *
 * @author WangZhiYao
 * @since 2025/11/17
 */
@Serializable
data object MainRoute

fun NavController.navigateToMain(navOptions: NavOptions) =
    navigate(route = MainRoute, navOptions = navOptions)

fun NavGraphBuilder.mainScreen() {
    composable<MainRoute> {
        MainRoute()
    }
}