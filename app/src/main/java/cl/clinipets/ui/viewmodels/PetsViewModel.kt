// ui/viewmodels/PetsViewModel.kt
package cl.clinipets.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
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

    fun loadPets() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _petsState.value = _petsState.value.copy(isLoading = true)

                val snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("pets")
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

    fun addPet(name: String, species: String, breed: String, age: String, weight: String) {
        val userId = auth.currentUser?.uid ?: return

        if (name.isBlank() || species.isBlank()) {
            _petsState.value = _petsState.value.copy(error = "Complete los campos obligatorios")
            return
        }

        viewModelScope.launch {
            try {
                _petsState.value = _petsState.value.copy(isLoading = true)

                val pet = hashMapOf(
                    "name" to name,
                    "species" to species,
                    "breed" to breed,
                    "age" to (age.toIntOrNull() ?: 0),
                    "weight" to (weight.toFloatOrNull() ?: 0f),
                    "createdAt" to System.currentTimeMillis(),
                    "ownerId" to userId
                )

                firestore.collection("users")
                    .document(userId)
                    .collection("pets")
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

    fun loadPetDetail(petId: String) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _petsState.value = _petsState.value.copy(isLoading = true)

                val doc = firestore.collection("users")
                    .document(userId)
                    .collection("pets")
                    .document(petId)
                    .get()
                    .await()

                val pet = doc.toObject<Pet>()?.copy(id = doc.id)

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

    fun deletePet(petId: String) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                _petsState.value = _petsState.value.copy(isLoading = true)

                firestore.collection("users")
                    .document(userId)
                    .collection("pets")
                    .document(petId)
                    .delete()
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

    fun clearState() {
        _petsState.value = _petsState.value.copy(
            isPetAdded = false,
            isPetDeleted = false,
            error = null
        )
    }
}

data class PetsState(
    val pets: List<Pet> = emptyList(),
    val selectedPet: Pet? = null,
    val isLoading: Boolean = false,
    val isPetAdded: Boolean = false,
    val isPetDeleted: Boolean = false,
    val error: String? = null
)

data class Pet(
    val id: String = "",
    val name: String = "",
    val species: String = "",
    val breed: String = "",
    val age: Int = 0,
    val weight: Float = 0f,
    val ownerId: String = "",
    val createdAt: Long = 0,
    val medicalHistory: List<MedicalRecord> = emptyList()
)

data class MedicalRecord(
    val date: String = "",
    val type: String = "",
    val description: String = "",
    val veterinarianName: String = ""
)