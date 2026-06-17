package com.safenet.vpn.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.safenet.vpn.presentation.screen.home.HomeScreen
import com.safenet.vpn.presentation.screen.settings.SettingsScreen
import com.safenet.vpn.presentation.viewmodel.HomeViewModel

sealed class Screen(val route: String) {
    object Home        : Screen("home")
    object Settings    : Screen("settings")
}

@Composable
fun SafeNetNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
    ) {
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                viewModel            = homeViewModel,
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
