package cl.clinipets.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.DefaultApi
import cl.clinipets.openapi.models.OfertaItem
import cl.clinipets.openapi.models.ProcedimientoItem
import cl.clinipets.openapi.models.VetItem
import cl.clinipets.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val defaultApi: DefaultApi
) : ViewModel() {

    private val _offersState = MutableStateFlow<Result<List<OfertaItem>>>(Result.Loading)
    val offersState: StateFlow<Result<List<OfertaItem>>> = _offersState

    private val _proceduresState = MutableStateFlow<Result<List<ProcedimientoItem>>>(Result.Loading)
    val proceduresState: StateFlow<Result<List<ProcedimientoItem>>> = _proceduresState

    private val _veterinariansState = MutableStateFlow<Result<List<VetItem>>>(Result.Loading)
    val veterinariansState: StateFlow<Result<List<VetItem>>> = _veterinariansState

    init {
        fetchDiscoveryData()
    }

    fun fetchDiscoveryData() {
        viewModelScope.launch {
            _offersState.value = Result.Loading
            _proceduresState.value = Result.Loading
            _veterinariansState.value = Result.Loading
            try {
                val offersResponse = defaultApi.apiDescubrimientoOfertasGet()
                if (offersResponse.isSuccessful) {
                    offersResponse.body()?.let { _offersState.value = Result.Success(it) }
                        ?: run { _offersState.value = Result.Error(Exception("Offers response is null")) }
                } else {
                    _offersState.value = Result.Error(Exception(offersResponse.errorBody()?.string() ?: "Unknown error"))
                }

                val proceduresResponse = defaultApi.apiDescubrimientoProcedimientosGet()
                if (proceduresResponse.isSuccessful) {
                    proceduresResponse.body()?.let { _proceduresState.value = Result.Success(it) }
                        ?: run { _proceduresState.value = Result.Error(Exception("Procedures response is null")) }
                } else {
                    _proceduresState.value = Result.Error(Exception(proceduresResponse.errorBody()?.string() ?: "Unknown error"))
                }

                val veterinariansResponse = defaultApi.apiDescubrimientoVeterinariosGet()
                if (veterinariansResponse.isSuccessful) {
                    veterinariansResponse.body()?.let { _veterinariansState.value = Result.Success(it) }
                        ?: run { _veterinariansState.value = Result.Error(Exception("Veterinarians response is null")) }
                } else {
                    _veterinariansState.value = Result.Error(Exception(veterinariansResponse.errorBody()?.string() ?: "Unknown error"))
                }

            } catch (e: Exception) {
                _offersState.value = Result.Error(e)
                _proceduresState.value = Result.Error(e)
                _veterinariansState.value = Result.Error(e)
            }
        }
    }
}
