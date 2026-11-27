package com.focusfarm.app.ui.components

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.focusfarm.core.ui.R

/**
 * Localized plant names by plant id. The catalogs and stored sessions keep their Turkish names as
 * data; only display goes through here, so existing records stay untouched.
 */
@StringRes
fun plantNameRes(plantId: String): Int? = when (plantId) {
    "sprout" -> R.string.plant_sprout
    "sunflower" -> R.string.plant_sunflower
    "cactus" -> R.string.plant_cactus
    "tree" -> R.string.plant_tree
    "mushroom" -> R.string.plant_mushroom
    "cherry" -> R.string.plant_cherry
    "bamboo" -> R.string.plant_bamboo
    "lotus" -> R.string.plant_lotus
    "clover" -> R.string.plant_clover
    "bonsai" -> R.string.plant_bonsai
    "carrot" -> R.string.plant_carrot
    "lavender" -> R.string.plant_lavender
    else -> null
}

@Composable
fun plantName(plantId: String, fallback: String): String =
    plantNameRes(plantId)?.let { stringResource(it) } ?: fallback

fun Context.plantName(plantId: String, fallback: String): String =
    plantNameRes(plantId)?.let(::getString) ?: fallback
