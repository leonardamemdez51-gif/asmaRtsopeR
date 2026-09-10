package com.example.core.location

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.local.ClientEntity
import kotlin.math.*

data class LatLng(
    val latitude: Double,
    val longitude: Double
)

data class RoutePoint(
    val clientId: Long,
    val clientName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMetersFromPrevious: Double = 0.0,
    val estimatedMinutesFromPrevious: Int = 0,
    val cumulativeDistanceKm: Double = 0.0,
    val cumulativeMinutes: Int = 0
)

object LocationUtils {

    const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates the exact Haversine distance between two geographic coordinates in meters.
     */
    fun haversineDistanceMeters(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Calculates distance in kilometers.
     */
    fun haversineDistanceKm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        return haversineDistanceMeters(lat1, lon1, lat2, lon2) / 1000.0
    }

    /**
     * Formats distance nicely (e.g. "450 m" or "3.2 km").
     */
    fun formatDistance(meters: Double): String {
        return if (meters < 1000) {
            "${meters.roundToInt()} m"
        } else {
            String.format("%.1f km", meters / 1000.0)
        }
    }

    /**
     * Estimates travel time in minutes based on urban motorcycle collection speed (~25 km/h).
     */
    fun estimateTravelTimeMinutes(distanceMeters: Double, averageSpeedKmH: Double = 25.0): Int {
        if (distanceMeters <= 0) return 0
        val distanceKm = distanceMeters / 1000.0
        val hours = distanceKm / averageSpeedKmH
        val minutes = (hours * 60).roundToInt()
        return max(1, minutes)
    }

    /**
     * Optimizes a list of clients using Greedy Nearest Neighbor Traveling Salesperson algorithm starting from collector position.
     */
    fun optimizeClientOrder(
        startLat: Double,
        startLng: Double,
        clients: List<ClientEntity>
    ): List<ClientEntity> {
        if (clients.isEmpty()) return emptyList()

        val unvisited = clients.toMutableList()
        val sortedList = mutableListOf<ClientEntity>()

        var currentLat = startLat
        var currentLng = startLng

        while (unvisited.isNotEmpty()) {
            var nearestIndex = 0
            var minDistance = Double.MAX_VALUE

            for (i in unvisited.indices) {
                val client = unvisited[i]
                val dist = haversineDistanceMeters(currentLat, currentLng, client.latitude, client.longitude)
                if (dist < minDistance) {
                    minDistance = dist
                    nearestIndex = i
                }
            }

            val nextClient = unvisited.removeAt(nearestIndex)
            sortedList.add(nextClient)
            currentLat = nextClient.latitude
            currentLng = nextClient.longitude
        }

        return sortedList
    }

    /**
     * Builds calculated route points with cumulative metrics.
     */
    fun buildRoutePoints(
        startLat: Double,
        startLng: Double,
        orderedClients: List<ClientEntity>
    ): List<RoutePoint> {
        val result = mutableListOf<RoutePoint>()
        var currLat = startLat
        var currLng = startLng
        var totalDistKm = 0.0
        var totalTimeMin = 0

        for (client in orderedClients) {
            val distMeters = haversineDistanceMeters(currLat, currLng, client.latitude, client.longitude)
            val travelMin = estimateTravelTimeMinutes(distMeters)
            totalDistKm += (distMeters / 1000.0)
            totalTimeMin += travelMin

            result.add(
                RoutePoint(
                    clientId = client.id,
                    clientName = client.fullName,
                    address = client.address,
                    latitude = client.latitude,
                    longitude = client.longitude,
                    distanceMetersFromPrevious = distMeters,
                    estimatedMinutesFromPrevious = travelMin,
                    cumulativeDistanceKm = totalDistKm,
                    cumulativeMinutes = totalTimeMin
                )
            )

            currLat = client.latitude
            currLng = client.longitude
        }

        return result
    }

    /**
     * Launches external Google Maps turn-by-turn navigation Intent safely.
     */
    fun launchGoogleMapsNavigation(
        context: Context,
        latitude: Double,
        longitude: Double,
        label: String
    ): Boolean {
        return try {
            val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label)})")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
                true
            } else {
                // Fallback to any maps app / web browser
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
