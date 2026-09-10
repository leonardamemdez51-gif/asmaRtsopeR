package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.ui.MainViewModel

@Composable
fun DailyTasksScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    CollectorMainScreen(
        viewModel = viewModel,
        onBack = onBack,
        onNavigate = onNavigate
    )
}
