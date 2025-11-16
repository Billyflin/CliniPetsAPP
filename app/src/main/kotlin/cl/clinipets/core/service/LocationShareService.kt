package cl.clinipets.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import cl.clinipets.MainActivity
import cl.clinipets.R
import cl.clinipets.core.ws.StompClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class LocationShareService : Service() {

    @Inject lateinit var fused: FusedLocationProviderClient
    @Inject lateinit var stomp: StompClient

    private var reservaId: UUID? = null
    private var cb: LocationCallback? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        reservaId = intent?.getStringExtra(EXTRA_RESERVA_ID)?.let { UUID.fromString(it) }
        val token = intent?.getStringExtra(EXTRA_TOKEN)
        if (token != null) {
            val headers = mapOf("Authorization" to "Bearer $token")
            stomp.connect(headers)
        }
        createChannel()
        startForeground(NOTIF_ID, buildNotification("Compartiendo ubicación…"))
        startLocationStreaming()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationStreaming()
        stomp.disconnect()
    }

    private fun startLocationStreaming() {
        val req = LocationRequest.Builder(2000L)
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val id = reservaId ?: return
                val speedMs = loc.speed
                val speedKmh: Double? = if (speedMs.isNaN()) null else (speedMs * 3.6)
                val bearing = loc.bearing
                val heading: Double? = if (bearing.isNaN()) null else bearing.toDouble()
                val payload = JSONObject().apply {
                    put("lat", loc.latitude)
                    put("lng", loc.longitude)
                    speedKmh?.let { put("speed", it) }
                    heading?.let { put("heading", it) }
                    put("ts", System.currentTimeMillis())
                }.toString()
                CoroutineScope(Dispatchers.IO).launch {
                    stomp.send("/app/junta/$id/pos", payload)
                }
            }
        }
        cb = callback
        try {
            fused.requestLocationUpdates(req, callback, mainLooper)
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    private fun stopLocationStreaming() {
        cb?.let { fused.removeLocationUpdates(it) }
        cb = null
    }

    private fun buildNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(CHANNEL_ID, "Clinipets Tracking", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(ch)
        }
    }

    companion object {
        private const val CHANNEL_ID = "clinipets_tracking"
        private const val NOTIF_ID = 1001
        const val EXTRA_RESERVA_ID = "reservaId"
        const val EXTRA_TOKEN = "token"
    }
}
