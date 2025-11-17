package com.example.template.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

/**
 *
 *
 * @author WangZhiYao
 * @since 2025/11/17
 */
@Composable
fun rememberTemplateAppState(
    navController: NavHostController = rememberNavController()
) = remember(navController) {
    TemplateAppState(navController)
}

@Stable
data class TemplateAppState(
    val navController: NavHostController
)