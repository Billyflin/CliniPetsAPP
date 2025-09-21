// data/model/Models.kt
package cl.clinipets.data.model

import kotlinx.serialization.Serializable

// ====================== USUARIOS ======================

/**
 * Represents a user in the system.
 *
 * @property id The unique identifier for the user.
 * @property email The email address of the user, used for login and communication.
 * @property name The full name of the user.
 * @property phone The phone number of the user (optional).
 * @property isVet A boolean flag indicating whether the user is a veterinarian.
 * @property createdAt The timestamp indicating when the user account was created, in milliseconds since the epoch.
 */
@Serializable
data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String? = null,
    val isVet: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// ====================== MASCOTAS ======================

@Serializable
data class Pet(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val species: PetSpecies = PetSpecies.DOG,
    val breed: String = "",
    val birthDate: Long? = null,
    val weight: Float = 0f,
    val sex: PetSex = PetSex.MALE,
    val notes: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class PetSpecies { DOG, CAT, OTHER }

@Serializable
enum class PetSex { MALE, FEMALE }

// ====================== CITAS ======================

@Serializable
data class Appointment(
    val id: String = "",
    val petId: String = "",
    val ownerId: String = "",
    val date: String = "",
    val time: String = "",
    val dateTime: Long = 0,
    val reason: String = "",
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    val consultationId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class AppointmentStatus { SCHEDULED, CONFIRMED, COMPLETED, CANCELLED }

// ====================== CONSULTAS ======================

@Serializable
data class Consultation(
    val id: String = "",
    val appointmentId: String = "",
    val petId: String = "",
    val weight: Float? = null,
    val temperature: Float? = null,
    val symptoms: String = "",
    val diagnosis: String = "",
    val treatment: String = "",
    val observations: String = "",
    val services: List<ServiceApplied> = emptyList(),
    val medications: List<MedicationUsed> = emptyList(),
    val vaccines: List<VaccineApplied> = emptyList(),
    val total: Double = 0.0,
    val paid: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class ServiceApplied(
    val serviceId: String = "",
    val name: String = "",
    val price: Double = 0.0
)

@Serializable
data class MedicationUsed(
    val medicationId: String = "",
    val name: String = "",
    val dose: String = "",
    val price: Double = 0.0
)

@Serializable
data class VaccineApplied(
    val vaccineId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val nextDoseDate: Long? = null
)

// ====================== SERVICIOS ======================

@Serializable
data class Service(
    val id: String = "",
    val name: String = "",
    val active: Boolean = true,
    val category: ServiceCategory = ServiceCategory.CONSULTATION,
    val basePrice: Double = 0.0,
)

@Serializable
enum class ServiceCategory { CONSULTATION, VACCINATION, SURGERY, GROOMING, OTHER }

// ====================== MEDICAMENTOS ======================

@Serializable
data class Medication(
    val id: String = "",
    val name: String = "",
    val presentation: MedicationPresentation = MedicationPresentation.TABLET,
    val stock: Int = 0,
    val unitPrice: Double = 0.0,
    val active: Boolean = true
)

@Serializable
enum class MedicationPresentation { TABLET, SYRUP, INJECTION, CREAM, DROPS, OTHER }

// ====================== VACUNAS ======================

@Serializable
data class Vaccine(
    val id: String = "",
    val name: String = "",
    val stock: Int = 0,
    val unitPrice: Double = 0.0,
    val active: Boolean = true
)

// ====================== HISTORIAL SIMPLIFICADO ======================

@Serializable
data class VaccinationRecord(
    val id: String = "",
    val petId: String = "",
    val vaccineName: String = "",
    val applicationDate: Long = System.currentTimeMillis(),
    val nextDoseDate: Long? = null,
    val veterinarianId: String = ""
)

// ====================== HORARIOS VETERINARIA ======================

@Serializable
data class VetSchedule(
    val id: String = "",
    val dayOfWeek: Int = 1, // 1 = Lunes, 7 = Domingo
    val startTime: String = "", // Formato "HH:mm"
    val endTime: String = "", // Formato "HH:mm"
    val active: Boolean = true
)