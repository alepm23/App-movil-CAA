package com.pictofly.ui.screens.config

import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable
fun CommunicationVerbScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    CommunicationSelectorScreen(
        navController = navController,
        selectorType = SelectorType.VERB,
        onBack = onBack
    )
}