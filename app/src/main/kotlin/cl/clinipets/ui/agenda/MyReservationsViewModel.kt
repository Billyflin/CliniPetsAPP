package cl.clinipets.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.ReservaControllerApi
import cl.clinipets.openapi.models.CitaDetalladaResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyReservationsViewModel @Inject constructor(
    private val reservaApi: ReservaControllerApi
) : ViewModel() {
    private val _reservas = MutableStateFlow<List<CitaDetalladaResponse>>(emptyList())
    val reservas = _reservas.asStateFlow()
    val isLoading = MutableStateFlow(false)

    init {
        loadReservas()
    }

    fun loadReservas() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val response = reservaApi.listarReservas()
                if (response.isSuccessful) {
                    _reservas.value = response.body() ?: emptyList()

                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading.value = false
            }
        }
    }

    fun refresh() {
        loadReservas()
    }
}
