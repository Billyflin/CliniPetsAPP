package cl.clinipets.core.common.location

import android.annotation.SuppressLint
import android.content.Context
import cl.clinipets.core.data.model.common.GeoPoint
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

class LocationServiceImpl(private val context: Context) : LocationService {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    @SuppressLint("MissingPermission") // tú gestionas runtime permission en la UI
    override suspend fun getLastKnownLocation(): GeoPoint? {
        val loc = client.lastLocation.await() ?: return null
        return GeoPoint(loc.latitude, loc.longitude)
    }
}