package cl.clinipets.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.DefaultApi
import cl.clinipets.openapi.models.CrearMascota
import cl.clinipets.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AddPetViewModel @Inject constructor(
    private val defaultApi: DefaultApi
) : ViewModel() {

    private val _addPetState = MutableStateFlow<Result<Unit>>(Result.Success(Unit))
    val addPetState: StateFlow<Result<Unit>> = _addPetState

    fun addPet(name: String, species: String, breed: String, weight: Double?, birthDate: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _addPetState.value = Result.Loading

            if (name.isBlank()) {
                _addPetState.value = Result.Error(IllegalArgumentException("Name cannot be empty."))
                return@launch
            }
            if (species.isBlank()) {
                _addPetState.value = Result.Error(IllegalArgumentException("Species cannot be empty."))
                return@launch
            }
            val petSpecies = try { CrearMascota.Especie.valueOf(species.uppercase()) } catch (e: IllegalArgumentException) { null }
            if (petSpecies == null) {
                _addPetState.value = Result.Error(IllegalArgumentException("Invalid species."))
                return@launch
            }

            try {
                val crearMascotaRequest = CrearMascota(
                    nombre = name,
                    especie = petSpecies,
                    raza = breed,
                    pesoKg = weight,
                    fechaNacimiento = if (birthDate.isNotBlank()) LocalDate.parse(birthDate) else null
                )
                val response = defaultApi.crearMascota(crearMascotaRequest)
                if (response.isSuccessful) {
                    _addPetState.value = Result.Success(Unit)
                    onSuccess()
                } else {
                    _addPetState.value = Result.Error(Exception(response.errorBody()?.string() ?: "Unknown error"))
                }
            } catch (e: Exception) {
                _addPetState.value = Result.Error(e)
            }
        }
    }
}
