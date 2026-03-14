package com.focusfarm.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.focusfarm.app.ui.components.BottomNavBar
import com.focusfarm.app.ui.screens.collection.CollectionScreen
import com.focusfarm.app.ui.screens.garden.GardenScreen
import com.focusfarm.app.ui.screens.home.HomeScreen
import com.focusfarm.app.ui.screens.session.SessionScreen
import com.focusfarm.app.ui.screens.shop.ShopScreen
import com.focusfarm.app.ui.screens.stats.StatsScreen

object Routes {
    const val HOME = "home"
    const val GARDEN = "garden"
    const val STATS = "stats"
    const val SHOP = "shop"
    const val COLLECTION = "collection"
    const val SESSION = "session/{plantId}/{minutes}"

    fun session(plantId: String, minutes: Int) = "session/$plantId/$minutes"
}

val TAB_ROUTES = listOf(Routes.HOME, Routes.GARDEN, Routes.STATS, Routes.SHOP)

@Composable
fun AppNavigation() {
    val navController: NavHostController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute in TAB_ROUTES

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute ?: Routes.HOME,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Routes.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onStartSession = { plantId, minutes ->
                        navController.navigate(Routes.session(plantId, minutes))
                    },
                )
            }
            composable(Routes.GARDEN) {
                GardenScreen(
                    onOpenCollection = { navController.navigate(Routes.COLLECTION) },
                )
            }
            composable(Routes.STATS) { StatsScreen() }
            composable(Routes.SHOP) { ShopScreen() }
            composable(Routes.COLLECTION) {
                CollectionScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.SESSION,
                arguments = listOf(
                    navArgument("plantId") { type = NavType.StringType },
                    navArgument("minutes") { type = NavType.IntType },
                ),
            ) { backStack ->
                val plantId = backStack.arguments?.getString("plantId") ?: "sprout"
                val minutes = backStack.arguments?.getInt("minutes") ?: 25
                SessionScreen(
                    plantId = plantId,
                    targetMinutes = minutes,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
