package com.servilocal.domain.usecase.posts

import com.servilocal.domain.model.ServicePost
import jakarta.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Filters by a typed neighborhood/city and by Haversine distance from the fixed
 * demo origin in Armenia. Seed coordinates are approximate; GPS/geocoding are out of scope.
 */
class FilterPostsByLocationUseCase @Inject constructor() {
    operator fun invoke(
        posts: List<ServicePost>,
        locationQuery: String,
        maxDistanceKm: Double? = null,
        originLatitude: Double = TEST_ORIGIN_LATITUDE,
        originLongitude: Double = TEST_ORIGIN_LONGITUDE
    ): List<ServicePost> {
        val query = locationQuery.trim()
        return posts.filter { post ->
            val matchesLocation = query.isEmpty() || post.location.contains(query, ignoreCase = true)
            val distance = distanceFromOriginKm(post, originLatitude, originLongitude)
            val matchesDistance = maxDistanceKm == null || (distance != null && distance <= maxDistanceKm)
            matchesLocation && matchesDistance
        }.let { matches ->
            if (maxDistanceKm == null) matches else matches.sortedBy { post ->
                distanceFromOriginKm(post, originLatitude, originLongitude) ?: Double.POSITIVE_INFINITY
            }
        }
    }

    private fun distanceFromOriginKm(
        post: ServicePost,
        originLatitude: Double,
        originLongitude: Double
    ): Double? {
        val latitude = post.latitude ?: return null
        val longitude = post.longitude ?: return null
        val earthRadiusKm = 6_371.0
        val latitudeDelta = Math.toRadians(latitude - originLatitude)
        val longitudeDelta = Math.toRadians(longitude - originLongitude)
        val a = sin(latitudeDelta / 2).let { it * it } +
            cos(Math.toRadians(originLatitude)) * cos(Math.toRadians(latitude)) *
            sin(longitudeDelta / 2).let { it * it }
        return 2 * earthRadiusKm * atan2(sqrt(a), sqrt(1 - a))
    }

    companion object {
        const val TEST_ORIGIN_LATITUDE = 4.5339
        const val TEST_ORIGIN_LONGITUDE = -75.6811
    }
}
