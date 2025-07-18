// data/model/Appointment.kt
package cl.clinipets.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Appointment(
    val id: String,
    val petId: String,
    val petName: String,
    val serviceType: ServiceType,
    val dateTime: Long,
    val veterinarianId: String,
    val veterinarianName: String,
    val status: AppointmentStatus,
    val notes: String? = null
)

@Serializable
enum class AppointmentStatus {
    SCHEDULED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED
}

@Serializable
data class ServiceType(
    val id: String,
    val name: String,
    val icon: String,
    val duration: Int, // en minutos
    val price: Int
)