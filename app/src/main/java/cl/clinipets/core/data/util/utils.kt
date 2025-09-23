// core/data/util/Geo.kt
package cl.clinipets.core.data.util

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    val R = 6371000.0 // metros
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat/2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon/2).pow(2)
    val c = 2 * asin(min(1.0, sqrt(a)))
    return (R * c).roundToInt()
}
