package com.focusfarm.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.focusfarm.app.domain.GardenPlantState
import com.focusfarm.app.domain.GrowthStage

@Composable
fun PlantVectorRenderer(
    plantId: String,
    stage: GrowthStage,
    state: GardenPlantState,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
) {
    Box(
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val width = size.toPx()
            val height = size.toPx()
            
            // Colors based on state
            val isWithered = state == GardenPlantState.WITHERED
            
            val potColor = if (isWithered) Color(0xFF5D4037) else Color(0xFF8D6E63)
            val stemColor = if (isWithered) Color(0xFF6D4C41) else Color(0xFF4CAF50)
            val leafColor = if (isWithered) Color(0xFF8D6E63) else Color(0xFF81C784)
            val dirtColor = if (isWithered) Color(0xFF3E2723) else Color(0xFF4E342E)

            // 1. Draw base / soil / pot
            drawBase(width, height, dirtColor, potColor)

            // If it's a seed, we only draw the seed/soil
            if (stage == GrowthStage.SEED) {
                drawSeed(width, height, isWithered)
                return@Canvas
            }

            // If it's a sprout, we draw a generic tiny shoot
            if (stage == GrowthStage.SPROUT) {
                drawSprout(width, height, stemColor, leafColor, isWithered)
                return@Canvas
            }

            // 2. Draw specific species for GROWING, MATURING, FULL stages
            when (plantId) {
                "sprout" -> drawSproutPlant(width, height, stage, stemColor, leafColor, isWithered)
                "sunflower" -> drawSunflower(width, height, stage, stemColor, leafColor, isWithered)
                "cactus" -> drawCactus(width, height, stage, isWithered)
                "mushroom" -> drawMushroom(width, height, stage, isWithered)
                "tree" -> drawTree(width, height, stage, isWithered)
                "cherry" -> drawCherry(width, height, stage, stemColor, isWithered)
                "bamboo" -> drawBamboo(width, height, stage, isWithered)
                "lotus" -> drawLotus(width, height, stage, isWithered)
                "clover" -> drawClover(width, height, stage, stemColor, leafColor, isWithered)
                "bonsai" -> drawBonsai(width, height, stage, potColor, isWithered)
                "carrot" -> drawCarrot(width, height, stage, isWithered)
                "lavender" -> drawLavender(width, height, stage, isWithered)
                else -> drawSproutPlant(width, height, stage, stemColor, leafColor, isWithered)
            }
        }
    }
}

private fun DrawScope.drawBase(width: Float, height: Float, dirtColor: Color, potColor: Color) {
    // Soil mound
    drawArc(
        color = dirtColor,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(width * 0.15f, height * 0.7f),
        size = Size(width * 0.7f, height * 0.3f)
    )
    // Small pot rim
    drawRoundRect(
        color = potColor,
        topLeft = Offset(width * 0.2f, height * 0.85f),
        size = Size(width * 0.6f, height * 0.08f),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )
}

private fun DrawScope.drawSeed(width: Float, height: Float, isWithered: Boolean) {
    val seedColor = if (isWithered) Color(0xFF4E342E) else Color(0xFFD7CCC8)
    // Tiny seed lying on the soil
    drawOval(
        color = seedColor,
        topLeft = Offset(width * 0.45f, height * 0.76f),
        size = Size(width * 0.1f, width * 0.07f)
    )
}

private fun DrawScope.drawSprout(width: Float, height: Float, stemColor: Color, leafColor: Color, isWithered: Boolean) {
    val stemPath = Path().apply {
        moveTo(width * 0.5f, height * 0.8f)
        quadraticTo(width * 0.48f, height * 0.68f, width * 0.46f, height * 0.6f)
    }
    drawPath(
        path = stemPath,
        color = stemColor,
        style = Stroke(width = 3.dp.toPx())
    )

    // Left Leaf
    val leafPathLeft = Path().apply {
        moveTo(width * 0.46f, height * 0.6f)
        quadraticTo(width * 0.36f, height * 0.52f, width * 0.38f, height * 0.62f)
        quadraticTo(width * 0.44f, height * 0.64f, width * 0.46f, height * 0.6f)
    }
    drawPath(path = leafPathLeft, color = leafColor)

    // Right Leaf (if not withered, withered only has one drooping leaf)
    if (!isWithered) {
        val leafPathRight = Path().apply {
            moveTo(width * 0.46f, height * 0.6f)
            quadraticTo(width * 0.56f, height * 0.54f, width * 0.54f, height * 0.64f)
            quadraticTo(width * 0.48f, height * 0.64f, width * 0.46f, height * 0.6f)
        }
        drawPath(path = leafPathRight, color = leafColor)
    }
}

private fun DrawScope.drawSproutPlant(
    width: Float,
    height: Float,
    stage: GrowthStage,
    stemColor: Color,
    leafColor: Color,
    isWithered: Boolean,
) {
    val scale = if (stage == GrowthStage.GROWING) 0.65f else if (stage == GrowthStage.MATURING) 0.85f else 1f
    
    withTransform({
        scale(scale, scale, Offset(width * 0.5f, height * 0.8f))
        if (isWithered) rotate(15f, Offset(width * 0.5f, height * 0.8f))
    }) {
        // Main stem
        val stemPath = Path().apply {
            moveTo(width * 0.5f, height * 0.8f)
            quadraticTo(width * 0.5f, height * 0.55f, width * 0.48f, height * 0.35f)
        }
        drawPath(path = stemPath, color = stemColor, style = Stroke(width = 4.dp.toPx()))

        // Lower leaves
        val leaf1 = Path().apply {
            moveTo(width * 0.49f, height * 0.6f)
            quadraticTo(width * 0.35f, height * 0.52f, width * 0.35f, height * 0.62f)
            quadraticTo(width * 0.45f, height * 0.65f, width * 0.49f, height * 0.6f)
        }
        drawPath(path = leaf1, color = leafColor)

        if (!isWithered) {
            val leaf2 = Path().apply {
                moveTo(width * 0.49f, height * 0.55f)
                quadraticTo(width * 0.65f, height * 0.47f, width * 0.65f, height * 0.57f)
                quadraticTo(width * 0.52f, height * 0.60f, width * 0.49f, height * 0.55f)
            }
            drawPath(path = leaf2, color = leafColor)
        }

        // Top crown leaves
        val crownLeft = Path().apply {
            moveTo(width * 0.48f, height * 0.35f)
            quadraticTo(width * 0.38f, height * 0.22f, width * 0.42f, height * 0.32f)
            quadraticTo(width * 0.46f, height * 0.35f, width * 0.48f, height * 0.35f)
        }
        drawPath(path = crownLeft, color = leafColor)

        val crownRight = Path().apply {
            moveTo(width * 0.48f, height * 0.35f)
            quadraticTo(width * 0.58f, height * 0.24f, width * 0.54f, height * 0.34f)
            quadraticTo(width * 0.50f, height * 0.36f, width * 0.48f, height * 0.35f)
        }
        drawPath(path = crownRight, color = if (isWithered) leafColor else Color(0xFF66BB6A))
    }
}

private fun DrawScope.drawSunflower(
    width: Float,
    height: Float,
    stage: GrowthStage,
    stemColor: Color,
    leafColor: Color,
    isWithered: Boolean,
) {
    val scale = if (stage == GrowthStage.GROWING) 0.6f else if (stage == GrowthStage.MATURING) 0.8f else 1f
    
    withTransform({
        scale(scale, scale, Offset(width * 0.5f, height * 0.8f))
        if (isWithered) rotate(22f, Offset(width * 0.5f, height * 0.8f))
    }) {
        // Stem
        drawLine(
            color = stemColor,
            start = Offset(width * 0.5f, height * 0.8f),
            end = Offset(width * 0.5f, height * 0.45f),
            strokeWidth = 4.dp.toPx()
        )

        // Leaf
        val leaf = Path().apply {
            moveTo(width * 0.5f, height * 0.65f)
            quadraticTo(width * 0.38f, height * 0.58f, width * 0.4f, height * 0.68f)
            quadraticTo(width * 0.48f, height * 0.68f, width * 0.5f, height * 0.65f)
        }
        drawPath(path = leaf, color = leafColor)

        // Sunflower head
        val headCenter = Offset(width * 0.5f, height * 0.42f)
        val flowerRadius = width * 0.16f
        val petalColor = if (isWithered) Color(0xFF8D6E63) else Color(0xFFFFCA28)
        val centerColor = if (isWithered) Color(0xFF3E2723) else Color(0xFF5D4037)

        // Draw petals
        val numPetals = 8
        for (i in 0 until numPetals) {
            val angle = (i * (360f / numPetals))
            withTransform({
                rotate(angle, headCenter)
            }) {
                drawOval(
                    color = petalColor,
                    topLeft = Offset(headCenter.x - width * 0.05f, headCenter.y - flowerRadius),
                    size = Size(width * 0.1f, flowerRadius)
                )
            }
        }

        // Draw dark center
        drawCircle(
            color = centerColor,
            radius = width * 0.09f,
            center = headCenter
        )
    }
}

private fun DrawScope.drawCactus(
    width: Float,
    height: Float,
    stage: GrowthStage,
    isWithered: Boolean,
) {
    val scale = if (stage == GrowthStage.GROWING) 0.6f else if (stage == GrowthStage.MATURING) 0.8f else 1f
    val cColor = if (isWithered) Color(0xFF8D6E63) else Color(0xFF2E7D32)
    val thornColor = if (isWithered) Color(0xFF5D4037) else Color(0xFFE0E0E0)
    val potBorderColor = if (isWithered) Color(0xFF4E342E) else Color(0xFFD84315)

    withTransform({
        scale(scale, scale, Offset(width * 0.5f, height * 0.8f))
        if (isWithered) rotate(-12f, Offset(width * 0.5f, height * 0.8f))
    }) {
        // Main Cactus Body
        drawRoundRect(
            color = cColor,
            topLeft = Offset(width * 0.42f, height * 0.35f),
            size = Size(width * 0.16f, height * 0.45f),
            cornerRadius = CornerRadius(width * 0.08f, width * 0.08f)
        )

        // Thorns on main body
        drawLine(thornColor, Offset(width * 0.5f, height * 0.42f), Offset(width * 0.55f, height * 0.4f), 1.5.dp.toPx())
        drawLine(thornColor, Offset(width * 0.5f, height * 0.52f), Offset(width * 0.45f, height * 0.5f), 1.5.dp.toPx())
        drawLine(thornColor, Offset(width * 0.5f, height * 0.62f), Offset(width * 0.56f, height * 0.6f), 1.5.dp.toPx())

        // Right branch (only in MATURING and FULL)
        if (stage != GrowthStage.GROWING) {
            val rightBranch = Path().apply {
                moveTo(width * 0.58f, height * 0.58f)
                lineTo(width * 0.68f, height * 0.58f)
                quadraticTo(width * 0.7f, height * 0.58f, width * 0.7f, height * 0.54f)
                lineTo(width * 0.7f, height * 0.44f)
            }
            drawPath(
                path = rightBranch,
                color = cColor,
                style = Stroke(width = width * 0.07f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }

        // Left branch (only in FULL)
        if (stage == GrowthStage.FULL) {
            val leftBranch = Path().apply {
                moveTo(width * 0.42f, height * 0.52f)
                lineTo(width * 0.32f, height * 0.52f)
                quadraticTo(width * 0.3f, height * 0.52f, width * 0.3f, height * 0.48f)
                lineTo(width * 0.3f, height * 0.38f)
            }
            drawPath(
                path = leftBranch,
                color = cColor,
                style = Stroke(width = width * 0.07f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )

            // Red flower on top (signature of FULL stage)
            val flowerColor = if (isWithered) Color(0xFF5D4037) else Color(0xFFEC407A)
            drawCircle(
                color = flowerColor,
                radius = width * 0.035f,
                center = Offset(width * 0.5f, height * 0.32f)
            )
        }
    }
}

private fun DrawScope.drawMushroom(
    width: Float,
    height: Float,
    stage: GrowthStage,
    isWithered: Boolean,
) {
    val scale = if (stage == GrowthStage.GROWING) 0.6f else if (stage == GrowthStage.MATURING) 0.8f else 1f
    val capColor = if (isWithered) Color(0xFF6D4C41) else Color(0xFFE53935)
    val stemColor = if (isWithered) Color(0xFF4E342E) else Color(0xFFEEEEEE)
    val spotColor = if (isWithered) Color(0xFF5D4037) else Color(0xFFFFFFFF)

    withTransform({
        scale(scale, scale, Offset(width * 0.5f, height * 0.8f))
        if (isWithered) rotate(-15f, Offset(width * 0.5f, height * 0.8f))
    }) {
        // Stem
        drawRoundRect(
            color = stemColor,
            topLeft = Offset(width * 0.44f, height * 0.55f),
            size = Size(width * 0.12f, height * 0.25f),
            cornerRadius = CornerRadius(width * 0.04f, width * 0.04f)
        )

        // Cap (semi circle)
        drawArc(
            color = capColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(width * 0.28f, height * 0.38f),
            size = Size(width * 0.44f, height * 0.38f)
        )

        // White spots (only in MATURING and FULL)
        if (stage != GrowthStage.GROWING) {
            drawCircle(spotColor, width * 0.03f, Offset(width * 0.42f, height * 0.46f))
            drawCircle(spotColor, width * 0.02f, Offset(width * 0.58f, height * 0.48f))
            drawCircle(spotColor, width * 0.025f, Offset(width * 0.5f, height * 0.42f))
        }
    }
}

private fun DrawScope.drawTree(
    width: Float,
    height: Float,
    stage: GrowthStage,
    isWithered: Boolean,
) {
    val scale = if (stage == GrowthStage.GROWING) 0.6f else if (stage == GrowthStage.MATURING) 0.8f else 1f
    val trunkColor = if (isWithered) Color(0xFF4E342E) else Color(0xFF5D4037)
    val leavesColor = if (isWithered) Color(0xFF795548) else Color(0xFF2E7D32)

    withTransform({
        scale(scale, scale, Offset(width * 0.5f, height * 0.8f))
        if (isWithered) rotate(18f, Offset(width * 0.5f, height * 0.8f))
    }) {
        // Trunk
        val trunkPath = Path().apply {
            moveTo(width * 0.46f, height * 0.8f)
            lineTo(width * 0.48f, height * 0.5f)
            lineTo(width * 0.52f, height * 0.5f)
            lineTo(width * 0.54f, height * 0.8f)
        }
        drawPath(path = trunkPath, color = trunkColor)

        // Foliage / Canopy
        if (stage == GrowthStage.GROWING) {
            drawCircle(
                color = leavesColor,
                radius = width * 0.16f,
                center = Offset(width * 0.5f, height * 0.46f)
            )
        } else if (stage == GrowthStage.MATURING) {
            drawCircle(color = leavesColor, radius = width * 0.15f, center = Offset(width * 0.42f, height * 0.44f))
            drawCircle(color = leavesColor, radius = width * 0.17f, center = Offset(width * 0.52f, height * 0.42f))
        } else {
            // Full scale cloud canopy
            drawCircle(color = leavesColor, radius = width * 0.14f, center = Offset(width * 0.38f, height * 0.46f))
            drawCircle(color = leavesColor, radius = width * 0.16f, center = Offset(width * 0.5f, height * 0.38f))
            drawCircle(color = leavesColor, radius = width * 0.14f, center = Offset(width * 0.62f, height * 0.46f))
            
            // Add a little fruit/flower to full tree if not withered
            if (!isWithered) {
                drawCircle(Color(0xFFE53935), width * 0.015f, Offset(width * 0.42f, height * 0.42f))
                drawCircle(Color(0xFFE53935), width * 0.015f, Offset(width * 0.58f, height * 0.44f))
                drawCircle(Color(0xFFE53935), width * 0.015f, Offset(width * 0.5f, height * 0.48f))
            }
        }
    }
}

private fun DrawScope.drawCherry(
    width: Float,
    height: Float,
    stage: GrowthStage,
    stemColor: Color,
    isWithered: Boolean,
) {
    val scale = if (stage == GrowthStage.GROWING) 0.6f else if (stage == GrowthStage.MATURING) 0.8f else 1f
    val trunkColor = if (isWithered) Color(0xFF4E342E) else Color(0xFF3E2723)
    val bloomColor = if (isWithered) Color(0xFF8D6E63) else Color(0xFFF48FB1)
    val innerBloomColor = if (isWithered) Color(0xFF5D4037) else Color(0xFFF06292)

    withTransform({
        scale(scale, scale, Offset(width * 0.5f, height * 0.8f))
        if (isWithered) rotate(20f, Offset(width * 0.5f, height * 0.8f))
    }) {
        // Branch stem
        val branchPath = Path().apply {
            moveTo(width * 0.5f, height * 0.8f)
            quadraticTo(width * 0.48f, height * 0.58f, width * 0.44f, height * 0.45f)
            moveTo(width * 0.46f, height * 0.55f)
            quadraticTo(width * 0.55f, height * 0.52f, width * 0.6f, height * 0.48f)
        }
        drawPath(path = branchPath, color = trunkColor, style = Stroke(width = 3.5.dp.toPx()))

        // Cherry blossoms
        if (stage == GrowthStage.GROWING) {
            drawCircle(bloomColor, width * 0.08f, Offset(width * 0.44f, height * 0.42f))
            drawCircle(innerBloomColor, width * 0.04f, Offset(width * 0.44f, height * 0.42f))
        } else if (stage == GrowthStage.MATURING) {
            drawCircle(bloomColor, width * 0.08f, Offset(width * 0.44f, height * 0.42f))
            drawCircle(innerBloomColor, width * 0.04f, Offset(width * 0.44f, height * 0.42f))

            drawCircle(bloomColor, width * 0.07f, Offset(width * 0.6f, height * 0.46f))
            drawCircle(innerBloomColor, width * 0.03f, Offset(width * 0.6f, height * 0.46f))
        } else {
            // Full bloom
            drawCircle(bloomColor, width * 0.09f, Offset(width * 0.44f, height * 0.42f))
            drawCircle(innerBloomColor, width * 0.04f, Offset(width * 0.44f, height * 0.42f))

            drawCircle(bloomColor, width * 0.08f, Offset(width * 0.6f, height * 0.46f))
            drawCircle(innerBloomColor, width * 0.03f, Offset(width * 0.6f, height * 0.46f))

            drawCircle(bloomColor, width * 0.07f, Offset(Offset(width * 0.35f, height * 0.5f).x, Offset(width * 0.35f, height * 0.5f).y))
            drawCircle(innerBloomColor, width * 0.03f, Offset(Offset(width * 0.35f, height * 0.5f).x, Offset(width * 0.35f, height * 0.5f).y))
        }
    }
}

private fun DrawScope.drawBamboo(
    width: Float,
    height: Float,
    stage: GrowthStage,
    isWithered: Boolean,
) {
    val scale = if (stage == GrowthStage.GROWING) 0.6f else if (stage == GrowthStage.MATURING) 0.8f else 1f
    val bColor = if (isWithered) Color(0xFF8D6E63) else Color(0xFF4CAF50)
    val rimColor = if (isWithered) Color(0xFF5D4037) else Color(0xFF2E7D32)
    val leafColor = if (isWithered) Color(0xFF6D4C41) else Color(0xFF81C784)

    withTransform({
        scale(scale, scale, Offset(width * 0.5f, height * 0.8f))
        if (isWithered) rotate(-15f, Offset(width * 0.5f, height * 0.8f))
    }) {
        // Left Bamboo Segment
        drawRoundRect(bColor, Offset(width * 0.42f, height * 0.65f), Size(width * 0.06f, height * 0.16f), CornerRadius(2.dp.toPx(), 2.dp.toPx()))
        drawRect(rimColor, Offset(width * 0.41f, height * 0.64f), Size(width * 0.08f, height * 0.02f))
        
        drawRoundRect(bColor, Offset(width * 0.42f, height * 0.48f), Size(width * 0.06f, height * 0.15f), CornerRadius(2.dp.toPx(), 2.dp.toPx()))
        drawRect(rimColor, Offset(width * 0.41f, height * 0.47f), Size(width * 0.08f, height * 0.02f))

        // Right Bamboo Segment (if Maturing/Full)
        if (stage != GrowthStage.GROWING) {
            drawRoundRect(bColor, Offset(width * 0.54f, height * 0.68f), Size(width * 0.05f, height * 0.13f), CornerRadius(2.dp.toPx(), 2.dp.toPx()))
            drawRect(rimColor, Offset(width * 0.53f, height * 0.67f), Size(width * 0.07f, height * 0.018f))

            drawRoundRect(bColor, Offset(width * 0.54f, height * 0.54f), Size(width * 0.05f, height * 0.12f), CornerRadius(2.dp.toPx(), 2.dp.toPx()))
            drawRect(rimColor, Offset(width * 0.53f, height * 0.53f), Size(width * 0.07f, height * 0.018f))
        }

        // Full stage leaves
        if (stage == GrowthStage.FULL) {
            drawRoundRect(bColor, Offset(width * 0.42f, height * 0.33f), Size(width * 0.06f, height * 0.14f), CornerRadius(2.dp.toPx(), 2.dp.toPx()))
            
            // Side leaves
            val lPath1 = Path().apply {
                moveTo(width * 0.41f, height * 0.48f)
                quadraticTo(width * 0.32f, height * 0.44f, width * 0.33f, height * 0.5f)
                quadraticTo(width * 0.4f, height * 0.51f, width * 0.41f, height * 0.48f)
            }
            drawPath(path = lPath1, color = leafColor)

            val lPath2 = Path().apply {
                moveTo(width * 0.59f, height * 0.54f)
                quadraticTo(width * 0.68f, height * 0.5f, width * 0.67f, height * 0.56f)
                quadraticTo(width * 0.6f, height * 0.57f, width * 0.59f, height * 0.54f)
            }
            drawPath(path = lPath2, color = leafColor)
        }
    }
}

private fun DrawScope.drawLotus(
    width: Float,
    height: Float,
    stage: GrowthStage,
    isWithered: Boolean,
) {
    val scale = if (stage == GrowthStage.GROWING) 0.6f else if (stage == GrowthStage.MATURING) 0.8f else 1f
    val pColor = if (isWithered) Color(0xFF6D4C41) else Color(0xFFE040FB)
    val pLightColor = if (isWithered) Color(0xFF8D6E63) else Color(0xFFF50057)

    withTransform({
        scale(scale, scale, Offset(width * 0.5f, height * 0.8f))
        if (isWithered) rotate(15f, Offset(width * 0.5f, height * 0.8f))
    }) {
        // Base leaf (pad)
        val padColor = if (isWithered) Color(0xFF4E342E) else Color(0xFF00695C)
        drawOval(padColor, Offset(width * 0.22f, height * 0.72f), Size(width * 0.56f, height * 0.1f))

        // Petals (overlapping arcs/ovals)
        if (stage == GrowthStage.GROWING) {
            // Tiny closed bud
            drawOval(pColor, Offset(width * 0.44f, height * 0.58f), Size(width * 0.12f, height * 0.16f))
        } else if (stage == GrowthStage.MATURING) {
            // Partially open
            drawOval(pLightColor, Offset(width * 0.38f, height * 0.6f), Size(width * 0.12f, height * 0.14f))
            drawOval(pLightColor, Offset(width * 0.5f, height * 0.6f), Size(width * 0.12f, height * 0.14f))
            drawOval(pColor, Offset(width * 0.43f, height * 0.54f), Size(width * 0.14f, height * 0.18f))
        } else {
            // Full bloom lotus
            drawOval(pLightColor, Offset(width * 0.3f, height * 0.62f), Size(width * 0.15f, height * 0.12f))
            drawOval(pLightColor, Offset(width * 0.55f, height * 0.62f), Size(width * 0.15f, height * 0.12f))
            drawOval(pLightColor, Offset(width * 0.36f, height * 0.56f), Size(width * 0.13f, height * 0.16f))
            drawOval(pLightColor, Offset(width * 0.51f, height * 0.56f), Size(width * 0.13f, height * 0.16f))
            drawOval(pColor, Offset(width * 0.42f, height * 0.48f), Size(width * 0.16f, height * 0.24f))
        }
    }
}

private fun DrawScope.drawClover(
    width: Float,
    height: Float,
    stage: GrowthStage,
    stemColor: Color,
    leafColor: Color,
    isWithered: Boolean,
) {
    val scale = if (stage == GrowthStage.GROWING) 0.6f else if (stage == GrowthStage.MATURING) 0.8f else 1f

    withTransform({
        scale(scale, scale, Offset(width * 0.5f, height * 0.8f))
        if (isWithered) rotate(-20f, Offset(width * 0.5f, height * 0.8f))
    }) {
        // Thin stem
        val stemPath = Path().apply {
            moveTo(width * 0.5f, height * 0.8f)
            quadraticTo(width * 0.48f, height * 0.6f, width * 0.5f, height * 0.48f)
        }
        drawPath(path = stemPath, color = stemColor, style = Stroke(width = 2.5.dp.toPx()))

        // Hearts as leaves
        val center = Offset(width * 0.5f, height * 0.48f)
        val leafSize = width * 0.08f

        // Draw 3 leaves for GROWING/MATURING, 4 leaves for FULL
        val leavesCount = if (stage == GrowthStage.FULL) 4 else 3
        for (i in 0 until leavesCount) {
            val angle = i * (360f / leavesCount)
            withTransform({
                rotate(angle, center)
            }) {
                // Heart leaf representation using 2 overlapping circles and a polygon or oval
                drawCircle(
                    color = leafColor,
                    radius = leafSize * 0.6f,
                    center = Offset(center.x - leafSize * 0.35f, center.y - leafSize * 0.5f)
                )
                drawCircle(
                    color = leafColor,
                    radius = leafSize * 0.6f,
                    center = Offset(center.x + leafSize * 0.35f, center.y - leafSize * 0.5f)
                )
                val leafBase = Path().apply {
                    moveTo(center.x - leafSize * 0.7f, center.y - leafSize * 0.3f)
                    lineTo(center.x, center.y)
                    lineTo(center.x + leafSize * 0.7f, center.y - leafSize * 0.3f)
                }
                drawPath(path = leafBase, color = leafColor)
            }
        }
    }
}

private fun DrawScope.drawBonsai(
    width: Float,
    height: Float,
    stage: GrowthStage,
    basePotColor: Color,
    isWithered: Boolean,
) {
    val scale = if (stage == GrowthStage.GROWING) 0.6f else if (stage == GrowthStage.MATURING) 0.8f else 1f
    val trunkColor = if (isWithered) Color(0xFF3E2723) else Color(0xFF4E342E)
    val foliageColor = if (isWithered) Color(0xFF5D4037) else Color(0xFF1B5E20)
    val potColor = if (isWithered) Color(0xFF37474F) else Color(0xFF006064)

    withTransform({
        scale(scale, scale, Offset(width * 0.5f, height * 0.8f))
    }) {
        // Oval shallow bonsai pot (override standard pot slightly)
        drawOval(potColor, Offset(width * 0.22f, height * 0.78f), Size(width * 0.56f, height * 0.08f))

        // Twisted trunk
        val trunkPath = Path().apply {
            moveTo(width * 0.5f, height * 0.8f)
            quadraticTo(width * 0.4f, height * 0.68f, width * 0.44f, height * 0.58f)
            quadraticTo(width * 0.52f, height * 0.5f, width * 0.42f, height * 0.42f)
        }
        drawPath(path = trunkPath, color = trunkColor, style = Stroke(width = 5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))

        // Foliage clouds
        if (stage == GrowthStage.GROWING) {
            drawCircle(foliageColor, width * 0.12f, Offset(width * 0.42f, height * 0.38f))
        } else if (stage == GrowthStage.MATURING) {
            drawOval(foliageColor, Offset(width * 0.32f, height * 0.32f), Size(width * 0.22f, height * 0.13f))
            drawOval(foliageColor, Offset(width * 0.46f, height * 0.44f), Size(width * 0.16f, height * 0.1f))
        } else {
            // Triple foliage clouds
            drawOval(foliageColor, Offset(width * 0.3f, height * 0.3f), Size(width * 0.25f, height * 0.14f))
            drawOval(foliageColor, Offset(width * 0.5f, height * 0.38f), Size(width * 0.2f, height * 0.11f))
            drawOval(foliageColor, Offset(width * 0.26f, height * 0.48f), Size(width * 0.18f, height * 0.1f))
        }
    }
}

private fun DrawScope.drawCarrot(
    width: Float,
    height: Float,
    stage: GrowthStage,
    isWithered: Boolean,
) {
    val scale = if (stage == GrowthStage.GROWING) 0.6f else if (stage == GrowthStage.MATURING) 0.8f else 1f
    val carrotColor = if (isWithered) Color(0xFF8D6E63) else Color(0xFFFF9800)
    val leafColor = if (isWithered) Color(0xFF6D4C41) else Color(0xFF4CAF50)

    withTransform({
        scale(scale, scale, Offset(width * 0.5f, height * 0.8f))
        if (isWithered) rotate(15f, Offset(width * 0.5f, height * 0.8f))
    }) {
        // Draw green leaves on top
        val leafPath = Path().apply {
            moveTo(width * 0.5f, height * 0.7f)
            quadraticTo(width * 0.44f, height * 0.5f, width * 0.46f, height * 0.42f)
            quadraticTo(width * 0.52f, height * 0.52f, width * 0.5f, height * 0.7f)
            
            moveTo(width * 0.5f, height * 0.7f)
            quadraticTo(width * 0.56f, height * 0.52f, width * 0.58f, height * 0.45f)
            quadraticTo(width * 0.52f, height * 0.58f, width * 0.5f, height * 0.7f)
        }
        drawPath(path = leafPath, color = leafColor)

        // Draw orange carrot body
        val carrotPath = Path().apply {
            moveTo(width * 0.44f, height * 0.68f)
            quadraticTo(width * 0.5f, height * 0.66f, width * 0.56f, height * 0.68f)
            lineTo(width * 0.52f, height * 0.82f)
            quadraticTo(width * 0.5f, height * 0.85f, width * 0.48f, height * 0.82f)
            close()
        }
        drawPath(path = carrotPath, color = carrotColor)
    }
}

private fun DrawScope.drawLavender(
    width: Float,
    height: Float,
    stage: GrowthStage,
    isWithered: Boolean,
) {
    val scale = if (stage == GrowthStage.GROWING) 0.6f else if (stage == GrowthStage.MATURING) 0.8f else 1f
    val stemColor = if (isWithered) Color(0xFF6D4C41) else Color(0xFF4CAF50)
    val bloomColor = if (isWithered) Color(0xFF8D6E63) else Color(0xFFAB47BC)

    withTransform({
        scale(scale, scale, Offset(width * 0.5f, height * 0.8f))
        if (isWithered) rotate(-15f, Offset(width * 0.5f, height * 0.8f))
    }) {
        val stems = listOf(
            Offset(width * 0.44f, height * 0.45f) to Offset(width * 0.46f, height * 0.8f),
            Offset(width * 0.5f, height * 0.38f) to Offset(width * 0.5f, height * 0.8f),
            Offset(width * 0.56f, height * 0.48f) to Offset(width * 0.54f, height * 0.8f)
        )

        stems.forEachIndexed { index, (top, bottom) ->
            if (index == 0 && stage == GrowthStage.GROWING) return@forEachIndexed
            if (index == 2 && stage != GrowthStage.FULL) return@forEachIndexed

            drawLine(
                color = stemColor,
                start = bottom,
                end = top,
                strokeWidth = 2.dp.toPx()
            )

            val segments = 4
            for (i in 0 until segments) {
                val fraction = i.toFloat() / (segments - 1)
                val x = top.x + (bottom.x - top.x) * fraction * 0.3f
                val y = top.y + (bottom.y - top.y) * fraction * 0.3f
                
                drawCircle(
                    color = bloomColor,
                    radius = (4.dp.toPx() - i * 0.5.dp.toPx()).coerceAtLeast(1.dp.toPx()),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = bloomColor,
                    radius = (3.dp.toPx() - i * 0.5.dp.toPx()).coerceAtLeast(1.dp.toPx()),
                    center = Offset(x - 3.dp.toPx(), y)
                )
                drawCircle(
                    color = bloomColor,
                    radius = (3.dp.toPx() - i * 0.5.dp.toPx()).coerceAtLeast(1.dp.toPx()),
                    center = Offset(x + 3.dp.toPx(), y)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlantVectorRendererPreview() {
    PlantVectorRenderer(
        plantId = "sunflower",
        stage = GrowthStage.FULL,
        state = GardenPlantState.BLOOMED,
        size = 120.dp
    )
}
