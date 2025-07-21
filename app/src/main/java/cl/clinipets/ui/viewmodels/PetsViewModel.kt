// ui/viewmodels/PetsViewModel.kt
package cl.clinipets.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.model.MedicalConsultation
import cl.clinipets.data.model.MedicalHistory
import cl.clinipets.data.model.MedicalHistoryEntry
import cl.clinipets.data.model.Pet
import cl.clinipets.data.model.PetSex
import cl.clinipets.data.model.PetSpecies
import cl.clinipets.data.model.SurgeryRecord
import cl.clinipets.data.model.VaccinationRecord
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class PetsViewModel @Inject constructor() : ViewModel() {
    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _petsState = MutableStateFlow(PetsState())
    val petsState: StateFlow<PetsState> = _petsState

    init {
        loadPets()
    }

    // ====================== CRUD MASCOTAS ======================

    fun loadPets() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _petsState.value = _petsState.value.copy(isLoading = true)

                val snapshot = firestore.collection("pets")
                    .whereEqualTo("ownerId", userId)
                    .whereEqualTo("active", true)
                    .get()
                    .await()

                val pets = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<Pet>()?.copy(id = doc.id)
                }

                _petsState.value = _petsState.value.copy(
                    pets = pets,
                    isLoading = false
                )
            } catch (e: Exception) {
                _petsState.value = _petsState.value.copy(
                    isLoading = false,
                    error = "Error al cargar mascotas: ${e.message}"
                )
            }
        }
    }

    fun loadPetDetail(petId: String) {
        viewModelScope.launch {
            try {
                _petsState.value = _petsState.value.copy(isLoading = true)

                val doc = firestore.collection("pets")
                    .document(petId)
                    .get()
                    .await()

                val pet = doc.toObject<Pet>()?.copy(id = doc.id)

                // Cargar historial médico
                if (pet != null) {
                    loadMedicalHistory(petId)
                    loadVaccinationRecords(petId)
                }

                _petsState.value = _petsState.value.copy(
                    selectedPet = pet,
                    isLoading = false
                )
            } catch (e: Exception) {
                _petsState.value = _petsState.value.copy(
                    isLoading = false,
                    error = "Error al cargar mascota: ${e.message}"
                )
            }
        }
    }

    fun addPet(
        name: String,
        species: PetSpecies,
        breed: String,
        birthDate: Long?,
        weight: Float,
        sex: PetSex,
        neutered: Boolean,
        microchipId: String?,
        notes: String
    ) {
        val userId = auth.currentUser?.uid ?: return

        if (name.isBlank()) {
            _petsState.value = _petsState.value.copy(error = "El nombre es obligatorio")
            return
        }

        viewModelScope.launch {
            try {
                _petsState.value = _petsState.value.copy(isLoading = true)

                val pet = Pet(
                    ownerId = userId,
                    name = name,
                    species = species,
                    breed = breed,
                    birthDate = birthDate,
                    weight = weight,
                    sex = sex,
                    neutered = neutered,
                    microchipId = microchipId,
                    notes = notes,
                    createdAt = System.currentTimeMillis()
                )

                firestore.collection("pets")
                    .add(pet)
                    .await()

                _petsState.value = _petsState.value.copy(
                    isLoading = false,
                    isPetAdded = true
                )

                loadPets()
            } catch (e: Exception) {
                _petsState.value = _petsState.value.copy(
                    isLoading = false,
                    error = "Error al agregar mascota: ${e.message}"
                )
            }
        }
    }

    fun updatePet(
        petId: String,
        name: String,
        species: PetSpecies,
        breed: String,
        birthDate: Long?,
        weight: Float,
        sex: PetSex,
        neutered: Boolean,
        microchipId: String?,
        notes: String
    ) {
        viewModelScope.launch {
            try {
                _petsState.value = _petsState.value.copy(isLoading = true)

                val updates = mapOf(
                    "name" to name,
                    "species" to species.name,
                    "breed" to breed,
                    "birthDate" to birthDate,
                    "weight" to weight,
                    "sex" to sex.name,
                    "neutered" to neutered,
                    "microchipId" to microchipId,
                    "notes" to notes
                )

                firestore.collection("pets")
                    .document(petId)
                    .update(updates)
                    .await()

                _petsState.value = _petsState.value.copy(
                    isLoading = false,
                    isPetUpdated = true
                )

                loadPetDetail(petId)
            } catch (e: Exception) {
                _petsState.value = _petsState.value.copy(
                    isLoading = false,
                    error = "Error al actualizar mascota: ${e.message}"
                )
            }
        }
    }

    fun deletePet(petId: String) {
        viewModelScope.launch {
            try {
                _petsState.value = _petsState.value.copy(isLoading = true)

                // Soft delete - solo marcar como inactivo
                firestore.collection("pets")
                    .document(petId)
                    .update("active", false)
                    .await()

                _petsState.value = _petsState.value.copy(
                    isLoading = false,
                    isPetDeleted = true
                )

                loadPets()
            } catch (e: Exception) {
                _petsState.value = _petsState.value.copy(
                    isLoading = false,
                    error = "Error al eliminar mascota: ${e.message}"
                )
            }
        }
    }

    // ====================== HISTORIAL MÉDICO ======================

    fun loadMedicalHistory(petId: String) {
        viewModelScope.launch {
            try {
                // Cargar consultas médicas
                val consultationsSnapshot = firestore.collection("consultations")
                    .whereEqualTo("petId", petId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()

                val consultations = consultationsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject<MedicalConsultation>()?.let { consultation ->
                        MedicalHistoryEntry(
                            consultationId = doc.id,
                            date = consultation.createdAt,
                            veterinarianName = "", // Se cargará después
                            diagnosis = consultation.diagnosis,
                            treatment = consultation.treatment,
                            weight = consultation.weight
                        )
                    }
                }

                // Cargar cirugías
                val surgeries = consultations
                    .filter { it.treatment.contains("cirugía", ignoreCase = true) }
                    .map { entry ->
                        SurgeryRecord(
                            id = entry.consultationId,
                            consultationId = entry.consultationId,
                            date = entry.date,
                            surgeryType = entry.diagnosis,
                            veterinarianName = entry.veterinarianName,
                            anesthesiaType = "General", // Por defecto
                            outcome = "Exitosa"
                        )
                    }

                val medicalHistory = MedicalHistory(
                    petId = petId,
                    consultations = consultations,
                    surgeries = surgeries,
                    allergies = _petsState.value.selectedPet?.notes?.let {
                        extractAllergies(it)
                    } ?: emptyList()
                )

                _petsState.value = _petsState.value.copy(
                    selectedPetMedicalHistory = medicalHistory
                )
            } catch (e: Exception) {
                // Error handling silencioso
            }
        }
    }

    fun loadVaccinationRecords(petId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("vaccinations")
                    .whereEqualTo("petId", petId)
                    .orderBy("applicationDate", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val records = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<VaccinationRecord>()?.copy(id = doc.id)
                }

                _petsState.value = _petsState.value.copy(
                    vaccinationRecords = records
                )
            } catch (e: Exception) {
                // Error handling silencioso
            }
        }
    }

    fun addVaccinationRecord(
        petId: String,
        vaccineId: String,
        vaccineName: String,
        batch: String,
        expirationDate: Long,
        veterinarianId: String,
        nextDoseDate: Long?
    ) {
        viewModelScope.launch {
            try {
                _petsState.value = _petsState.value.copy(isLoading = true)

                val record = VaccinationRecord(
                    petId = petId,
                    vaccineId = vaccineId,
                    vaccineName = vaccineName,
                    veterinarianId = veterinarianId,
                    applicationDate = System.currentTimeMillis(),
                    batch = batch,
                    expirationDate = expirationDate,
                    nextDoseDate = nextDoseDate,
                    certificateNumber = generateCertificateNumber()
                )

                firestore.collection("vaccinations")
                    .add(record)
                    .await()

                _petsState.value = _petsState.value.copy(
                    isLoading = false,
                    isVaccinationAdded = true
                )

                loadVaccinationRecords(petId)
            } catch (e: Exception) {
                _petsState.value = _petsState.value.copy(
                    isLoading = false,
                    error = "Error al registrar vacuna: ${e.message}"
                )
            }
        }
    }

    // ====================== FUNCIONES AUXILIARES ======================

    private fun extractAllergies(notes: String): List<String> {
        // Buscar patrones como "alérgico a", "alergia:", etc.
        val allergies = mutableListOf<String>()
        val patterns = listOf("alérgico a", "alergia:", "alergias:")

        patterns.forEach { pattern ->
            if (notes.contains(pattern, ignoreCase = true)) {
                val startIndex = notes.indexOf(pattern, ignoreCase = true) + pattern.length
                val endIndex = notes.indexOf("\n", startIndex).takeIf { it != -1 } ?: notes.length
                val allergyText = notes.substring(startIndex, endIndex).trim()
                allergies.addAll(allergyText.split(",").map { it.trim() })
            }
        }

        return allergies
    }

    private fun generateCertificateNumber(): String {
        return "CERT-${System.currentTimeMillis()}-${(1000..9999).random()}"
    }

    fun clearState() {
        _petsState.value = _petsState.value.copy(
            isPetAdded = false,
            isPetUpdated = false,
            isPetDeleted = false,
            isVaccinationAdded = false,
            error = null
        )
    }

    // ====================== BÚSQUEDA Y FILTROS ======================

    fun searchPets(query: String) {
        val filteredPets = if (query.isBlank()) {
            _petsState.value.pets
        } else {
            _petsState.value.pets.filter { pet ->
                pet.name.contains(query, ignoreCase = true) ||
                        pet.breed.contains(query, ignoreCase = true) ||
                        pet.microchipId?.contains(query, ignoreCase = true) == true
            }
        }

        _petsState.value = _petsState.value.copy(
            filteredPets = filteredPets,
            searchQuery = query
        )
    }

    fun filterBySpecies(species: PetSpecies?) {
        val filteredPets = if (species == null) {
            _petsState.value.pets
        } else {
            _petsState.value.pets.filter { it.species == species }
        }

        _petsState.value = _petsState.value.copy(
            filteredPets = filteredPets,
            selectedSpeciesFilter = species
        )
    }
}

// ====================== ESTADO ======================

data class PetsState(
    val pets: List<Pet> = emptyList(),
    val filteredPets: List<Pet> = emptyList(),
    val selectedPet: Pet? = null,
    val selectedPetMedicalHistory: MedicalHistory? = null,
    val vaccinationRecords: List<VaccinationRecord> = emptyList(),
    val isLoading: Boolean = false,
    val isPetAdded: Boolean = false,
    val isPetUpdated: Boolean = false,
    val isPetDeleted: Boolean = false,
    val isVaccinationAdded: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedSpeciesFilter: PetSpecies? = null
)