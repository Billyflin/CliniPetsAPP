// data/model/Pet.kt
package cl.clinipets.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Pet(
    val id: String,
    val name: String,
    val species: PetSpecies,
    val breed: String,
    val age: Int,
    val weight: Float,
    val imageUrl: String? = null,
    val ownerId: String,
    val medicalHistory: List<MedicalRecord> = emptyList()
)

@Serializable
enum class PetSpecies {
    DOG, CAT, RABBIT, BIRD, OTHER
}

@Serializable
data class MedicalRecord(
    val id: String,
    val date: Long,
    val type: RecordType,
    val description: String,
    val veterinarianId: String
)

@Serializable
enum class RecordType {
    VACCINATION, CONSULTATION, SURGERY, EMERGENCY, CHECKUP
}