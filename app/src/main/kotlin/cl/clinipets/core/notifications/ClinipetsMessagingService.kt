package cl.clinipets.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cl.clinipets.MainActivity
import cl.clinipets.core.session.SessionManager
import cl.clinipets.openapi.apis.DeviceTokenControllerApi
import cl.clinipets.openapi.models.DeviceTokenRequest
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ClinipetsMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo token FCM: $token")
        val entryPoint = entryPoint()
        val api = entryPoint.deviceTokenApi()
        val sessionManager = entryPoint.sessionManager()

        CoroutineScope(Dispatchers.IO).launch {
            sessionManager.restoreIfAny()
            val authToken = runCatching { sessionManager.tokenFlow.first() }.getOrNull()

            if (authToken.isNullOrBlank()) {
                Log.w(TAG, "Token FCM recibido pero no hay usuario logueado. Se enviará al hacer login.")
                return@launch
            }

            val result = runCatching { api.saveDeviceToken(DeviceTokenRequest(token)) }
            result.onSuccess { response ->
                if (response.isSuccessful) {
                    Log.d(TAG, "Token FCM enviado al backend")
                } else {
                    Log.e(TAG, "Backend rechazó token FCM: ${response.code()}")
                }
            }.onFailure { throwable ->
                Log.e(TAG, "Error enviando token FCM", throwable)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        ensureDefaultChannel()

        val notification = message.notification ?: return
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            message.data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, DEFAULT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notification.title ?: "Clinipets")
            .setContentText(notification.body ?: "")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        with(NotificationManagerCompat.from(this)) {
            notify((System.currentTimeMillis() % 100000).toInt(), builder.build())
        }
    }

    private fun ensureDefaultChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notificaciones"
            val descriptionText = "Notificaciones generales de Clinipets"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(DEFAULT_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun entryPoint(): DeviceTokenEntryPoint =
        EntryPointAccessors.fromApplication(applicationContext, DeviceTokenEntryPoint::class.java)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DeviceTokenEntryPoint {
        fun deviceTokenApi(): DeviceTokenControllerApi
        fun sessionManager(): SessionManager
    }

    companion object {
        private const val TAG = "ClinipetsFCM"
        private const val DEFAULT_CHANNEL_ID = "default_channel"
    }
}
