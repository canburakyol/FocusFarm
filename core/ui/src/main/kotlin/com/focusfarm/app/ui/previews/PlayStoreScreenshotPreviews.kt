package com.focusfarm.app.ui.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusfarm.app.domain.GrowthStage
import com.focusfarm.app.domain.Plant
import com.focusfarm.app.domain.PlantCatalog
import com.focusfarm.app.ui.components.FocusBackdrop
import com.focusfarm.app.ui.components.PlantDisplay
import com.focusfarm.app.ui.theme.*

@Composable
fun PhoneStatusBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "10:30",
            style = MaterialTheme.typography.bodySmall,
            color = CozyTextSecondary,
            fontWeight = FontWeight.Bold
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "📶 🔋 98%",
                style = MaterialTheme.typography.bodySmall,
                color = CozyTextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun AppHeaderMock(
    seeds: Int = 120,
    title: String = "FocusFarm"
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = CozyTextPrimary
            )
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(CozySurface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = "✨", fontSize = 12.sp)
            Text(
                text = seeds.toString(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = CozyTextPrimary
                )
            )
        }
    }
}

@Composable
fun ScreenshotWrapper(
    marketingTitle: String,
    marketingSubtitle: String? = null,
    content: @Composable () -> Unit
) {
    FocusFarmTheme {
        FocusBackdrop {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Marketing Message (Playfair Display)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = marketingTitle,
                        style = MaterialTheme.typography.displayMedium.copy(
                            color = CozyTextPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
                    )
                    if (marketingSubtitle != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = marketingSubtitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = CozyAccent,
                                fontWeight = FontWeight.Medium
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Phone Frame Wrapper
                Box(
                    modifier = Modifier
                        .width(260.dp)
                        .height(460.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(28.dp),
                            clip = false,
                            ambientColor = CozyTextPrimary.copy(alpha = 0.2f),
                            spotColor = CozyTextPrimary.copy(alpha = 0.4f)
                        )
                        .background(CozyBackground, RoundedCornerShape(28.dp))
                        .border(
                            width = 5.dp,
                            color = CozyTextPrimary,
                            shape = RoundedCornerShape(28.dp)
                        )
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Notch
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                                    .background(CozyTextPrimary)
                            )
                        }
                        PhoneStatusBar()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 8.dp)
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimerMockup(isTurkish: Boolean) {
    val plant = PlantCatalog.getById("sunflower") ?: PlantCatalog.ALL.first()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        AppHeaderMock(seeds = 120, title = if (isTurkish) "Odaklan" else "Focus")
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            PlantDisplay(
                plant = plant,
                stage = GrowthStage.GROWING,
                size = 130.dp,
                breathing = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "24:59",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = TimerFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = CozyTextPrimary
                )
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = if (isTurkish) "Ayçiçeği büyüyor..." else "Sunflower is growing...",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = CozyTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            )
        }
        
        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(containerColor = CozyAccent),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .height(44.dp)
        ) {
            Text(
                text = if (isTurkish) "Derin Odak" else "Deep Focus",
                color = CozyBackground,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GardenMockup(isTurkish: Boolean) {
    val plants = listOf(
        PlantCatalog.getById("sprout") to GrowthStage.FULL,
        PlantCatalog.getById("sunflower") to GrowthStage.FULL,
        PlantCatalog.getById("cactus") to GrowthStage.FULL,
        PlantCatalog.getById("tree") to GrowthStage.FULL,
        PlantCatalog.getById("mushroom") to GrowthStage.FULL,
        PlantCatalog.getById("cherry") to GrowthStage.FULL,
        PlantCatalog.getById("bamboo") to GrowthStage.FULL,
        PlantCatalog.getById("lotus") to GrowthStage.FULL,
        PlantCatalog.getById("bonsai") to GrowthStage.FULL
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        AppHeaderMock(seeds = 350, title = if (isTurkish) "Bahçem" else "My Garden")
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(plants) { (plant, stage) ->
                    if (plant != null) {
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CozySurface.copy(alpha = 0.6f))
                                .border(1.dp, CozySurface, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            PlantDisplay(
                                plant = plant,
                                stage = stage,
                                size = 48.dp
                            )
                        }
                    }
                }
            }
        }
        
        Card(
            colors = CardDefaults.cardColors(containerColor = CozySurface.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "9/9",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CozyTextPrimary
                        )
                    )
                    Text(
                        text = if (isTurkish) "Alan" else "Slots",
                        style = MaterialTheme.typography.bodySmall,
                        color = CozyTextSecondary
                    )
                }
                Divider(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp),
                    color = CozyTextSecondary.copy(alpha = 0.3f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "24",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CozyTextPrimary
                        )
                    )
                    Text(
                        text = if (isTurkish) "Toplam Odak" else "Total Focus",
                        style = MaterialTheme.typography.bodySmall,
                        color = CozyTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun QuestsMockup(isTurkish: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        AppHeaderMock(seeds = 180, title = if (isTurkish) "Görevler" else "Quests")
        
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
        ) {
            // Streak Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CozyAccent.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = "🔥", fontSize = 24.sp)
                    Column {
                        Text(
                            text = if (isTurkish) "5 Günlük Seri!" else "5 Day Streak!",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = CozyTextPrimary
                            )
                        )
                        Text(
                            text = if (isTurkish) "Alışkanlığını koru" else "Keep the habit going",
                            style = MaterialTheme.typography.bodySmall,
                            color = CozyTextSecondary
                        )
                    }
                }
            }
            
            // Quest 1 (Completed)
            QuestCardMock(
                title = if (isTurkish) "25 Dakika Odaklan" else "Focus for 25 Mins",
                progress = 1f,
                progressText = "25/25",
                reward = 15,
                isClaimed = true,
                isTurkish = isTurkish
            )
            
            // Quest 2 (In Progress)
            QuestCardMock(
                title = if (isTurkish) "2 Seans Tamamla" else "Complete 2 Sessions",
                progress = 0.5f,
                progressText = "1/2",
                reward = 20,
                isClaimed = false,
                isTurkish = isTurkish
            )
            
            // Quest 3 (Not Started)
            QuestCardMock(
                title = if (isTurkish) "Yeni Bir Tohum Ek" else "Plant a New Seed",
                progress = 0f,
                progressText = "0/1",
                reward = 10,
                isClaimed = false,
                isTurkish = isTurkish
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun QuestCardMock(
    title: String,
    progress: Float,
    progressText: String,
    reward: Int,
    isClaimed: Boolean,
    isTurkish: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CozySurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CozyTextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = progress,
                        color = CozyAccent,
                        trackColor = CozyBackground,
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.bodySmall,
                        color = CozyTextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {},
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isClaimed) CozySuccess.copy(alpha = 0.2f) else CozyAccent
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isClaimed) {
                        if (isTurkish) "Alındı" else "Claimed"
                    } else {
                        "+$reward ✨"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isClaimed) CozyTextSecondary else CozyBackground
                    )
                )
            }
        }
    }
}

@Composable
fun CollectionMockup(isTurkish: Boolean) {
    val items = listOf(
        PlantCatalog.getById("sprout") to true,
        PlantCatalog.getById("sunflower") to true,
        PlantCatalog.getById("cactus") to true,
        PlantCatalog.getById("tree") to true,
        PlantCatalog.getById("mushroom") to false,
        PlantCatalog.getById("cherry") to false
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        AppHeaderMock(seeds = 80, title = if (isTurkish) "Koleksiyon" else "Collection")
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(items) { (plant, unlocked) ->
                    if (plant != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (unlocked) CozySurface else CozySurface.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.size(56.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (unlocked) {
                                        PlantDisplay(plant = plant, stage = GrowthStage.FULL, size = 50.dp)
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(CozyTextSecondary.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "🔒", fontSize = 20.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (unlocked) plant.name else "???",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (unlocked) CozyTextPrimary else CozyTextSecondary
                                    )
                                )
                                Text(
                                    text = if (unlocked) "${plant.minMinutes} dk" else "50 ✨",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CozyTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ReferralMockup(isTurkish: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        AppHeaderMock(seeds = 120, title = if (isTurkish) "Davet Et" else "Referral")
        
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(CozyAccent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🎁", fontSize = 40.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (isTurkish) "+50 Tohum Kazan!" else "Earn +50 Seeds!",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = CozyTextPrimary
                )
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = if (isTurkish) 
                    "Davet ettiğin her arkadaşın için 50 tohum kazan." 
                else 
                    "Get 50 seeds for every friend you invite.",
                style = MaterialTheme.typography.bodySmall,
                color = CozyTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Referral Code Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CozySurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CozyAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isTurkish) "Senin Kodun" else "Your Code",
                        style = MaterialTheme.typography.bodySmall,
                        color = CozyTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "FOCUS50",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = CozyTextPrimary,
                            letterSpacing = 2.sp
                        )
                    )
                }
            }
        }
        
        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(containerColor = CozyAccent),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .height(44.dp)
        ) {
            Text(
                text = if (isTurkish) "Kodu Paylaş" else "Share Code",
                color = CozyBackground,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ======================== PREVIEWS ========================

@Preview(name = "1. Timer - TR", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun PreviewTimerTR() {
    ScreenshotWrapper(
        marketingTitle = "Odaklan, Bahçeni Büyüt!",
        marketingSubtitle = "Gereksiz Bildirimleri Sessize Al",
        content = { TimerMockup(isTurkish = true) }
    )
}

@Preview(name = "1. Timer - EN", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun PreviewTimerEN() {
    ScreenshotWrapper(
        marketingTitle = "Focus & Grow Your Garden!",
        marketingSubtitle = "Silence Distractions Effortlessly",
        content = { TimerMockup(isTurkish = false) }
    )
}

@Preview(name = "2. Garden - TR", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun PreviewGardenTR() {
    ScreenshotWrapper(
        marketingTitle = "Kendi Vahanı Yarat!",
        marketingSubtitle = "Çalıştıkça Yeşeren Sanal Çiftlik",
        content = { GardenMockup(isTurkish = true) }
    )
}

@Preview(name = "2. Garden - EN", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun PreviewGardenEN() {
    ScreenshotWrapper(
        marketingTitle = "Build Your Oasis!",
        marketingSubtitle = "Your Hard Work Spreads Life",
        content = { GardenMockup(isTurkish = false) }
    )
}

@Preview(name = "3. Quests - TR", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun PreviewQuestsTR() {
    ScreenshotWrapper(
        marketingTitle = "Görevleri Yap, Tohum Kazan!",
        marketingSubtitle = "Günlük Hedeflerle Alışkanlık Edin",
        content = { QuestsMockup(isTurkish = true) }
    )
}

@Preview(name = "3. Quests - EN", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun PreviewQuestsEN() {
    ScreenshotWrapper(
        marketingTitle = "Complete Quests, Earn Seeds!",
        marketingSubtitle = "Stay Consistent with Daily Goals",
        content = { QuestsMockup(isTurkish = false) }
    )
}

@Preview(name = "4. Collection - TR", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun PreviewCollectionTR() {
    ScreenshotWrapper(
        marketingTitle = "Eşsiz Bitkiler Keşfet!",
        marketingSubtitle = "Odaklandıkça Yeni Türleri Aç",
        content = { CollectionMockup(isTurkish = true) }
    )
}

@Preview(name = "4. Collection - EN", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun PreviewCollectionEN() {
    ScreenshotWrapper(
        marketingTitle = "Unlock Unique Flora!",
        marketingSubtitle = "Discover Rare Species as You Focus",
        content = { CollectionMockup(isTurkish = false) }
    )
}

@Preview(name = "5. Referral - TR", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun PreviewReferralTR() {
    ScreenshotWrapper(
        marketingTitle = "Birlikte Büyütün!",
        marketingSubtitle = "Arkadaşını Davet Et, +50 Tohum Kap",
        content = { ReferralMockup(isTurkish = true) }
    )
}

@Preview(name = "5. Referral - EN", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
fun PreviewReferralEN() {
    ScreenshotWrapper(
        marketingTitle = "Grow Together!",
        marketingSubtitle = "Invite Friends for +50 Bonus Seeds",
        content = { ReferralMockup(isTurkish = false) }
    )
}
