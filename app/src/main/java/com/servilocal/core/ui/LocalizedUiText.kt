package com.servilocal.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.servilocal.R

@Composable
fun localizedServiceCategory(category: String): String = stringResource(
    when (category.trim().lowercase()) {
        "hogar" -> R.string.category_home
        "educación", "educacion" -> R.string.category_education
        "mascotas" -> R.string.category_pets
        "tecnología", "tecnologia" -> R.string.category_technology
        "electricidad" -> R.string.category_electricity
        "transporte" -> R.string.category_transport
        else -> R.string.category_all
    }
)

@Composable
fun localizedDistance(distance: String): String {
    val match = Regex("^A\\s+([0-9]+(?:[.,][0-9]+)?)\\s+km(.*)$", RegexOption.IGNORE_CASE).find(distance)
        ?: return distance
    return stringResource(
        R.string.distance_description,
        match.groupValues[1],
        match.groupValues[2]
    )
}
