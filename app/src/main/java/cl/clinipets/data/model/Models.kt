package cl.clinipets.data.model

import kotlinx.serialization.Serializable

// ====================== USUARIOS Y AUTENTICACIÓN ======================

@Serializable
data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String? = null,
    val photoUrl: String? = null,
    val role: UserRole = UserRole.CLIENT,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis()
)

@Serializable
enum class UserRole {
    CLIENT,        // Dueño de mascota
    VETERINARIAN,  // Veterinario
    ADMIN         // Administrador
}

// ====================== MASCOTAS ======================

@Serializable
data class Pet(
    val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val species: PetSpecies = PetSpecies.DOG,
    val breed: String = "",
    val birthDate: Long? = null,  // Para calcular edad automáticamente
    val weight: Float = 0f,
    val sex: PetSex = PetSex.MALE,
    val isNeutered: Boolean = false,
    val microchipId: String? = null,
    val photoUrl: String? = null,
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class PetSpecies {
    DOG, CAT, RABBIT, BIRD, REPTILE, OTHER
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
    val veterinarianId: String = "",
    val date: String = "",  // formato: "2025-07-20"
    val time: String = "",  // formato: "14:30"
    val dateTime: Long = 0,  // timestamp para ordenar
    val serviceType: ServiceCategory = ServiceCategory.CONSULTATION,
    val reason: String = "",
    val status: AppointmentStatus = AppointmentStatus.SCHEDULED,
    val consultationId: String? = null,  // Se llena cuando se completa
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class AppointmentStatus {
    SCHEDULED,    // Agendada
    CONFIRMED,    // Confirmada
    IN_PROGRESS,  // En curso
    COMPLETED,    // Completada
    CANCELLED,    // Cancelada
    NO_SHOW      // No se presentó
}

// ====================== CONSULTAS MÉDICAS ======================

@Serializable
data class MedicalConsultation(
    val id: String = "",
    val appointmentId: String = "",
    val petId: String = "",
    val veterinarianId: String = "",

    // Datos clínicos
    val weight: Float? = null,  // Peso actual en la consulta
    val temperature: Float? = null,  // Temperatura en °C
    val heartRate: Int? = null,  // Frecuencia cardíaca
    val respiratoryRate: Int? = null,  // Frecuencia respiratoria

    val symptoms: String = "",
    val clinicalExam: String = "",  // Examen físico
    val diagnosis: String = "",
    val treatment: String = "",
    val observations: String = "",
    val recommendations: String = "",  // Recomendaciones para el dueño

    // Servicios y medicamentos
    val services: List<ServiceProvided> = emptyList(),
    val medications: List<MedicationUsed> = emptyList(),
    val vaccines: List<VaccineApplied> = emptyList(),

    // Próxima cita
    val nextCheckupDays: Int? = null,  // En cuántos días debe volver
    val nextCheckupReason: String? = null,

    // Facturación
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val discountReason: String? = null,
    val total: Double = 0.0,
    val amountPaid: Double = 0.0,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val paymentMethod: PaymentMethod? = null,

    // Timestamps
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class ServiceProvided(
    val serviceId: String = "",
    val name: String = "",
    val category: ServiceCategory = ServiceCategory.CONSULTATION,
    val price: Double = 0.0
)

@Serializable
data class MedicationUsed(
    val medicationId: String = "",
    val name: String = "",
    val dose: String = "",  // ej: "2.5 ml"
    val frequency: String = "",  // ej: "cada 8 horas"
    val duration: String = "",  // ej: "7 días"
    val route: String = "",  // ej: "oral", "subcutánea"
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0
)

@Serializable
data class VaccineApplied(
    val vaccineId: String = "",
    val name: String = "",
    val batch: String = "",
    val expirationDate: Long = 0,
    val nextDoseDate: Long? = null,
    val price: Double = 0.0
)

@Serializable
enum class PaymentStatus {
    PENDING,   // Pendiente
    PARTIAL,   // Pago parcial
    PAID,      // Pagado
    CANCELLED  // Cancelado
}

@Serializable
enum class PaymentMethod {
    CASH,         // Efectivo
    DEBIT_CARD,   // Débito
    CREDIT_CARD,  // Crédito
    TRANSFER,     // Transferencia
    CHECK,        // Cheque
    OTHER
}

// ====================== SERVICIOS VETERINARIOS ======================

@Serializable
data class VeterinaryService(
    val id: String = "",
    val name: String = "",
    val category: ServiceCategory = ServiceCategory.CONSULTATION,
    val description: String = "",
    val basePrice: Double = 0.0,
    val estimatedDuration: Int = 30,  // minutos
    val requiresAppointment: Boolean = true,
    val isActive: Boolean = true
)

@Serializable
enum class ServiceCategory {
    CONSULTATION,      // Consulta general
    VACCINATION,       // Vacunación
    SURGERY,          // Cirugía
    LABORATORY,       // Exámenes de laboratorio
    RADIOLOGY,        // Radiografías
    ULTRASOUND,       // Ecografías
    GROOMING,         // Peluquería
    HOSPITALIZATION,  // Hospitalización
    EMERGENCY,        // Emergencia
    DEWORMING,        // Desparasitación
    DENTAL,           // Dental
    OTHER
}

// ====================== INVENTARIO ======================

@Serializable
data class Medication(
    val id: String = "",
    val name: String = "",
    val activeIngredient: String = "",
    val presentation: String = "",  // ej: "Tabletas 500mg", "Suspensión 250mg/5ml"
    val laboratory: String = "",
    val category: MedicationCategory = MedicationCategory.OTHER,
    val unitPrice: Double = 0.0,
    val purchasePrice: Double = 0.0,  // Precio de compra
    val stock: Int = 0,
    val minStock: Int = 10,
    val expirationDate: Long? = null,
    val batch: String = "",
    val isControlled: Boolean = false,  // Si requiere receta retenida
    val isActive: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
enum class MedicationCategory {
    ANTIBIOTIC,
    ANALGESIC,
    ANTI_INFLAMMATORY,
    ANTIPARASITIC,
    VACCINE,
    VITAMIN,
    ANESTHETIC,
    HORMONE,
    OTHER
}

@Serializable
data class InventoryMovement(
    val id: String = "",
    val itemId: String = "",  // medicationId o vaccineId
    val itemType: InventoryItemType = InventoryItemType.MEDICATION,
    val movementType: MovementType = MovementType.OUT,
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0,
    val reason: String = "",
    val consultationId: String? = null,  // Si fue usado en consulta
    val supplierId: String? = null,  // Si fue una compra
    val invoiceNumber: String? = null,  // Número de factura de compra
    val performedBy: String = "",  // userId
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
enum class InventoryItemType {
    MEDICATION,
    VACCINE,
    SUPPLY  // Insumos
}

@Serializable
enum class MovementType {
    IN,         // Entrada (compra)
    OUT,        // Salida (uso en consulta)
    ADJUSTMENT, // Ajuste de inventario
    EXPIRED,    // Vencido
    DAMAGED     // Dañado
}

// ====================== VACUNAS ======================

@Serializable
data class Vaccine(
    val id: String = "",
    val name: String = "",
    val manufacturer: String = "",
    val diseases: List<String> = emptyList(),  // Enfermedades que previene
    val species: List<PetSpecies> = emptyList(),  // Especies aplicables
    val doses: Int = 1,  // Número de dosis necesarias
    val daysBetweenDoses: Int = 0,  // Días entre dosis
    val validityMonths: Int = 12,  // Meses de validez
    val unitPrice: Double = 0.0,
    val purchasePrice: Double = 0.0,
    val stock: Int = 0,
    val minStock: Int = 5,
    val isActive: Boolean = true
)

@Serializable
data class VaccinationRecord(
    val id: String = "",
    val petId: String = "",
    val vaccineId: String = "",
    val vaccineName: String = "",
    val consultationId: String = "",
    val veterinarianId: String = "",
    val applicationDate: Long = System.currentTimeMillis(),
    val batch: String = "",
    val expirationDate: Long = 0,
    val doseNumber: Int = 1,
    val nextDoseDate: Long? = null,
    val certificateNumber: String? = null
)

// ====================== HISTORIAL MÉDICO ======================

@Serializable
data class MedicalHistory(
    val petId: String = "",
    val consultations: List<MedicalHistoryEntry> = emptyList(),
    val vaccinations: List<VaccinationRecord> = emptyList(),
    val surgeries: List<SurgeryRecord> = emptyList(),
    val allergies: List<String> = emptyList(),
    val chronicConditions: List<String> = emptyList()
)

@Serializable
data class MedicalHistoryEntry(
    val consultationId: String = "",
    val date: Long = 0,
    val veterinarianName: String = "",
    val diagnosis: String = "",
    val treatment: String = "",
    val weight: Float? = null
)

@Serializable
data class SurgeryRecord(
    val id: String = "",
    val consultationId: String = "",
    val date: Long = 0,
    val surgeryType: String = "",
    val veterinarianName: String = "",
    val anesthesiaType: String = "",
    val complications: String? = null,
    val outcome: String = ""
)
