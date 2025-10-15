package cl.clinipets.core.prefs

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "map_prefs")

object MapPrefs {
    private val KEY_LAST_LAT = doublePreferencesKey("last_lat")
    private val KEY_LAST_LON = doublePreferencesKey("last_lon")
    private val KEY_LAST_RADIO = intPreferencesKey("last_radio")

    suspend fun setLastPosition(ctx: Context, lat: Double, lon: Double) {
        ctx.dataStore.edit { it[KEY_LAST_LAT] = lat; it[KEY_LAST_LON] = lon }
    }

    suspend fun setLastRadio(ctx: Context, radio: Int) {
        ctx.dataStore.edit { it[KEY_LAST_RADIO] = radio }
    }

    fun lastLat(ctx: Context): Flow<Double?> = ctx.dataStore.data.map { it[KEY_LAST_LAT] }
    fun lastLon(ctx: Context): Flow<Double?> = ctx.dataStore.data.map { it[KEY_LAST_LON] }
    fun lastRadio(ctx: Context): Flow<Int?> = ctx.dataStore.data.map { it[KEY_LAST_RADIO] }
}

