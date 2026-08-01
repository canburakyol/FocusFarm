package com.focusfarm.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusfarm.app.R

data class NavItem(
    val route: String,
    val labelRes: Int,
    val emoji: String,
    val contentDescription: String,
)

@Composable
fun BottomNavBar(currentRoute: String, onNavigate: (String) -> Unit) {
    val items = listOf(
        NavItem(
            route = Routes.GARDEN,
            labelRes = R.string.tab_garden,
            emoji = "🏡",
            contentDescription = "Bahçe Sekmesi"
        ),
        NavItem(
            route = Routes.COLLECTION,
            labelRes = R.string.collection_book,
            emoji = "📖",
            contentDescription = "Koleksiyon Sekmesi"
        ),
        NavItem(
            route = Routes.HOME,
            labelRes = R.string.tab_home,
            emoji = "⏱️",
            contentDescription = "Odaklanma Seansı Başlat"
        ),
        NavItem(
            route = Routes.STATS,
            labelRes = R.string.tab_stats,
            emoji = "📊",
            contentDescription = "İstatistik Sekmesi"
        ),
        NavItem(
            route = Routes.SETTINGS,
            labelRes = R.string.settings_title,
            emoji = "⚙️",
            contentDescription = "Ayarlar Sekmesi"
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .shadow(8.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isCenter = item.route == Routes.HOME
                val isSelected = currentRoute == item.route
                val interactionSource = remember { MutableInteractionSource() }

                if (isCenter) {
                    // Center prominent focus action
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                role = Role.Button,
                                onClick = { onNavigate(item.route) }
                            )
                            .semantics {
                                contentDescription = item.contentDescription
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "🌱",
                                fontSize = 28.sp,
                                modifier = Modifier.offset(y = (-1).dp)
                            )
                            Text(
                                text = "Odaklan",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                } else {
                    // Regular bottom bar item
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                role = Role.Tab,
                                onClick = { onNavigate(item.route) }
                            )
                            .semantics {
                                contentDescription = item.contentDescription
                            }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = item.emoji,
                                fontSize = 22.sp
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(item.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
