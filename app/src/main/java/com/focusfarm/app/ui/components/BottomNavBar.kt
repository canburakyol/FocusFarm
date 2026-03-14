package com.focusfarm.app.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.focusfarm.app.R
import com.focusfarm.app.ui.navigation.Routes
import com.focusfarm.app.ui.theme.ForestDark
import com.focusfarm.app.ui.theme.ForestMid

data class NavItem(
    val route: String,
    val label: Int,
    val emoji: String,
)

private val NAV_ITEMS = listOf(
    NavItem(Routes.HOME, R.string.tab_home, "🌱"),
    NavItem(Routes.GARDEN, R.string.tab_garden, "🌿"),
    NavItem(Routes.STATS, R.string.tab_stats, "📊"),
    NavItem(Routes.SHOP, R.string.tab_shop, "🌸"),
)

@Composable
fun BottomNavBar(currentRoute: String, onNavigate: (String) -> Unit) {
    NavigationBar(
        containerColor = ForestMid,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        NAV_ITEMS.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Text(
                        text = item.emoji,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                label = {
                    Text(
                        text = stringResource(item.label),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = ForestDark,
                ),
            )
        }
    }
}
