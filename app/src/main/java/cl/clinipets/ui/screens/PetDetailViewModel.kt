package cl.clinipets.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.DefaultApi
import cl.clinipets.openapi.models.ActualizarMascota
import cl.clinipets.openapi.models.Mascota
import cl.clinipets.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PetDetailViewModel @Inject constructor(
    private val defaultApi: DefaultApi
) : ViewModel() {

    private val _petState = MutableStateFlow<Result<Mascota>>(Result.Loading)
    val petState: StateFlow<Result<Mascota>> = _petState

    private val _updatePetState = MutableStateFlow<Result<Unit>>(Result.Success(Unit))
    val updatePetState: StateFlow<Result<Unit>> = _updatePetState

    private val _deletePetState = MutableStateFlow<Result<Unit>>(Result.Success(Unit))
    val deletePetState: StateFlow<Result<Unit>> = _deletePetState

    fun fetchPetDetails(petId: UUID) {
        viewModelScope.launch {
            _petState.value = Result.Loading
            try {
                val response = defaultApi.detalleMascota(petId)
                if (response.isSuccessful) {
                    response.body()?.let { _petState.value = Result.Success(it) }
                        ?: run { _petState.value = Result.Error(Exception("Pet details are null")) }
                } else {
                    _petState.value = Result.Error(Exception(response.errorBody()?.string() ?: "Unknown error"))
                }
            } catch (e: Exception) {
                _petState.value = Result.Error(e)
            }
        }
    }

    fun updatePet(petId: UUID, name: String, breed: String?, weight: Double?, birthDate: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _updatePetState.value = Result.Loading
            try {
                val actualizarMascotaRequest = ActualizarMascota(
                    nombre = name,
                    raza = breed,
                    pesoKg = weight,
                    fechaNacimiento = if (birthDate?.isNotBlank() == true) LocalDate.parse(birthDate) else null
                )
                val response = defaultApi.actualizarMascota(petId, actualizarMascotaRequest)
                if (response.isSuccessful) {
                    _updatePetState.value = Result.Success(Unit)
                    onSuccess()
                } else {
                    _updatePetState.value = Result.Error(Exception(response.errorBody()?.string() ?: "Unknown error"))
                }
            } catch (e: Exception) {
                _updatePetState.value = Result.Error(e)
            }
        }
    }

    fun deletePet(petId: UUID, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _deletePetState.value = Result.Loading
            try {
                val response = defaultApi.eliminarMascota(petId)
                if (response.isSuccessful) {
                    _deletePetState.value = Result.Success(Unit)
                    onSuccess()
                } else {
                    _deletePetState.value = Result.Error(Exception(response.errorBody()?.string() ?: "Unknown error"))
                }
            } catch (e: Exception) {
                _deletePetState.value = Result.Error(e)
            }
        }
    }
}
