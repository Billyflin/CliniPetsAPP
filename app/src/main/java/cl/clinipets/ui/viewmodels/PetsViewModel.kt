// ui/viewmodels/PetsViewModel.kt
package cl.clinipets.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.data.model.Consultation
import cl.clinipets.data.model.Pet
import cl.clinipets.data.model.PetSex
import cl.clinipets.data.model.PetSpecies
import cl.clinipets.data.model.VaccinationRecord
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
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

                if (pet != null) {
                    loadPetConsultations(petId)
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

    private fun loadPetConsultations(petId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("consultations")
                    .whereEqualTo("petId", petId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .await()

                Log.d("Consultations", snapshot.documents.toString())

                val consultations = snapshot.documents.mapNotNull { doc ->
                    doc.toObject<Consultation>()?.copy(id = doc.id)
                }
                Log.d("Consultations", consultations.toString())
                _petsState.value = _petsState.value.copy(
                    selectedPetConsultations = consultations
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

    fun getNextVaccinations(): List<VaccinationRecord> {
        val thirtyDaysFromNow = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L)

        return _petsState.value.vaccinationRecords
            .filter { record ->
                record.nextDoseDate?.let { nextDate ->
                    nextDate <= thirtyDaysFromNow && nextDate >= System.currentTimeMillis()
                } ?: false
            }
            .sortedBy { it.nextDoseDate }
    }

    fun clearState() {
        _petsState.value = _petsState.value.copy(
            isPetAdded = false,
            isPetUpdated = false,
            isPetDeleted = false,
            error = null
        )
    }
}

data class PetsState(
    val pets: List<Pet> = emptyList(),
    val selectedPet: Pet? = null,
    val selectedPetConsultations: List<Consultation> = emptyList(),
    val vaccinationRecords: List<VaccinationRecord> = emptyList(),
    val isLoading: Boolean = false,
    val isPetAdded: Boolean = false,
    val isPetUpdated: Boolean = false,
    val isPetDeleted: Boolean = false,
    val error: String? = null
)