// data/model/Models.kt
package cl.clinipets.data.model

import kotlinx.serialization.Serializable

// ====================== USUARIOS ======================

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
enum class PetSpecies {
    DOG, CAT, OTHER
}

@Serializable
enum class PetSex {
    MALE, FEMALE
}

// ====================== CITAS ======================

@Serializable
data class Appointment(
    val id: String = "",
    val petId: String = "",
    val ownerId: String = "",
    val date: String = "",  // formato: "2025-07-20"
    val time: String = "",  // formato: "14:30"
    val dateTime: Long = 0,  // timestamp para ordenar
    val reason: String = "",
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    val consultationId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class AppointmentStatus {
    SCHEDULED,    // Agendada
    CONFIRMED,    // Confirmada
    COMPLETED,    // Completada
    CANCELLED     // Cancelada
}

// ====================== CONSULTAS ======================

@Serializable
data class Consultation(
    val id: String = "",
    val appointmentId: String = "",
    val petId: String = "",
    val veterinarianId: String = "",

    // Datos clínicos básicos
    val weight: Float? = null,
    val temperature: Float? = null,
    val symptoms: String = "",
    val diagnosis: String = "",
    val treatment: String = "",
    val observations: String = "",

    // Servicios aplicados
    val services: List<ServiceApplied> = emptyList(),
    val medications: List<MedicationUsed> = emptyList(),
    val vaccines: List<VaccineApplied> = emptyList(),

    // Cobro
    val total: Double = 0.0,
    val paid: Boolean = false,

    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class ServiceApplied(
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
    val category: ServiceCategory = ServiceCategory.CONSULTATION,
    val basePrice: Double = 0.0,
    val isActive: Boolean = true
)

@Serializable
enum class ServiceCategory {
    CONSULTATION,  // Consulta
    VACCINATION,   // Vacunación
    SURGERY,       // Cirugía
    GROOMING,      // Peluquería
    OTHER         // Otro
}

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
enum class MedicationPresentation {
    TABLET,        // Tableta
    SYRUP,         // Jarabe
    INJECTION,     // Inyectable
    CREAM,         // Crema
    DROPS,         // Gotas
    OTHER         // Otro
}

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