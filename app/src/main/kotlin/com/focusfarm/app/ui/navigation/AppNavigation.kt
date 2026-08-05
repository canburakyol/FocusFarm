package com.focusfarm.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.focusfarm.app.ui.screens.collection.CollectionScreen
import com.focusfarm.app.ui.screens.garden.GardenScreen
import com.focusfarm.app.ui.screens.home.HomeScreen
import com.focusfarm.app.ui.screens.session.SessionScreen
import com.focusfarm.app.ui.screens.settings.SettingsScreen
import com.focusfarm.app.ui.screens.onboarding.OnboardingScreen
import com.focusfarm.app.ui.screens.shop.ShopScreen
import com.focusfarm.app.ui.screens.stats.StatsScreen
import com.focusfarm.app.ui.screens.missions.MissionsScreen
import com.focusfarm.app.ui.screens.streak.StreakDetailScreen
import com.focusfarm.app.ui.navigation.FocusFarmNavigationContracts
import com.focusfarm.app.monetization.BannerAdSlot
import com.focusfarm.app.monetization.BannerAdViewModel

object Routes {
    const val HOME = FocusFarmNavigationContracts.HOME
    const val ONBOARDING = FocusFarmNavigationContracts.ONBOARDING
    const val GARDEN = FocusFarmNavigationContracts.GARDEN
    const val STATS = FocusFarmNavigationContracts.STATS
    const val SHOP = FocusFarmNavigationContracts.SHOP
    const val SETTINGS = FocusFarmNavigationContracts.SETTINGS
    const val COLLECTION = FocusFarmNavigationContracts.COLLECTION
    const val SESSION = FocusFarmNavigationContracts.SESSION
    const val MISSIONS = "missions"
    const val STREAK_DETAIL = "streak_detail"

    fun session(sessionId: String, plantId: String, minutes: Int) =
        FocusFarmNavigationContracts.sessionRoute(sessionId, plantId, minutes)

    fun recoverSession(sessionId: String) =
        FocusFarmNavigationContracts.recoverSessionRoute(sessionId)
}

val TAB_ROUTES = listOf(Routes.GARDEN, Routes.COLLECTION, Routes.HOME, Routes.STATS, Routes.SETTINGS)

@Composable
fun AppNavigation(
    onboardingCompleted: Boolean,
    bannerAdViewModel: BannerAdViewModel = hiltViewModel(),
) {
    val navController: NavHostController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val canShowAds by bannerAdViewModel.canShowAds.collectAsStateWithLifecycle()

    val showBottomBar = currentRoute in TAB_ROUTES

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    BannerAdSlot(enabled = canShowAds)
                    BottomNavBar(
                        currentRoute = currentRoute ?: Routes.HOME,
                        onNavigate = { route ->
                            if (route != currentRoute) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (onboardingCompleted) Routes.HOME else Routes.ONBOARDING,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(onCompleteOnboarding = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                })
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onStartSession = { sessionId, plantId, minutes ->
                        navController.navigate(Routes.session(sessionId, plantId, minutes))
                    },
                    onRecoverSession = { navController.navigate(Routes.recoverSession(it)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.GARDEN) {
                GardenScreen(
                    onOpenCollection = { navController.navigate(Routes.COLLECTION) },
                )
            }
            composable(Routes.STATS) {
                StatsScreen(
                    onOpenMissions = { navController.navigate(Routes.MISSIONS) },
                    onOpenStreak = { navController.navigate(Routes.STREAK_DETAIL) }
                )
            }
            composable(Routes.SHOP) { ShopScreen() }
            composable(Routes.MISSIONS) {
                MissionsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.STREAK_DETAIL) {
                StreakDetailScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = null,
                )
            }
            composable(Routes.COLLECTION) {
                CollectionScreen(
                    onBack = null,
                )
            }
            composable(
                route = Routes.SESSION,
                arguments = listOf(
                    navArgument("plantId") { type = NavType.StringType },
                    navArgument("minutes") { type = NavType.IntType },
                    navArgument("sessionId") { type = NavType.StringType },
                ),
            ) { backStack ->
                val plantId = backStack.arguments?.getString("plantId") ?: "sprout"
                val minutes = backStack.arguments?.getInt("minutes") ?: 25
                val sessionId = backStack.arguments?.getString("sessionId") ?: return@composable
                SessionScreen(
                    sessionId = sessionId,
                    plantId = plantId,
                    targetMinutes = minutes,
                    onBack = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.SESSION) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
    }
}
