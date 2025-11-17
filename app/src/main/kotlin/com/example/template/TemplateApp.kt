package com.example.template

import androidx.compose.runtime.Composable
import com.example.template.navigation.TemplateAppState
import com.example.template.navigation.TemplateNavHost
import com.example.template.navigation.rememberTemplateAppState

/**
 *
 *
 * @author WangZhiYao
 * @since 2025/11/17
 */
@Composable
fun TemplateApp(
    appState: TemplateAppState = rememberTemplateAppState()
) {
    TemplateNavHost(appState)
}