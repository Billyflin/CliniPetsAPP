package cl.clinipets.core.common.location

import android.annotation.SuppressLint
import android.content.Context
import cl.clinipets.core.data.model.common.GeoPoint
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LocationServiceImpl(private val context: Context) : LocationService {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    @SuppressLint("MissingPermission") // tú gestionas runtime permission en la UI
    override suspend fun getLastKnownLocation(): GeoPoint? {
        val loc = client.lastLocation.await() ?: return null
        return GeoPoint(loc.latitude, loc.longitude)
    }

    @SuppressLint("MissingPermission")
    override fun observeLocation(): Flow<GeoPoint> = callbackFlow {
        val req = LocationRequest.Builder(
            /* intervalMillis = */ 2000L // 2s (ajusta según batería)
        ).setMinUpdateIntervalMillis(1000L)   // 1s
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                trySend(GeoPoint(loc.latitude, loc.longitude))
            }
        }

        client.requestLocationUpdates(req, cb, context.mainLooper)
        awaitClose { client.removeLocationUpdates(cb) }
    }
}