// data/model/MedicalConsultation.kt
package cl.clinipets.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MedicalConsultation(
    val id: String = "",
    val appointmentId: String = "",
    val petId: String = "",
    val veterinarianId: String = "",
    val date: Long = 0,
    val symptoms: String = "",
    val diagnosis: String = "",
    val treatment: String = "",
    val medications: List<MedicationApplication> = emptyList(),
    val observations: String = "",
    val nextCheckup: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class MedicationApplication(
    val medicationId: String = "",
    val medicationName: String = "",
    val dose: String = "", // ej: "2.5 ml" o "1 tableta"
    val frequency: String = "", // ej: "cada 8 horas"
    val duration: String = "", // ej: "7 días"
    val route: String = "", // ej: "oral", "intramuscular", "subcutánea"
    val notes: String = ""
)

@Serializable
data class Medication(
    val id: String = "",
    val name: String = "",
    val activeIngredient: String = "",
    val presentation: String = "", // ej: "Tabletas 500mg", "Suspensión 250mg/5ml"
    val type: MedicationType = MedicationType.OTHER,
    val isActive: Boolean = true
)

@Serializable
enum class MedicationType {
    ANTIBIOTIC,
    ANALGESIC,
    ANTI_INFLAMMATORY,
    ANTIPARASITIC,
    VACCINE,
    VITAMIN,
    OTHER
}