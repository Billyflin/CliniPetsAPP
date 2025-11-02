package cl.clinipets.ui.mascotas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.clinipets.openapi.apis.MascotasApi
import cl.clinipets.openapi.models.Mascota
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.logging.Logger
import javax.inject.Inject

@HiltViewModel
class MascotasViewModel @Inject constructor(
    private val api: MascotasApi
) : ViewModel() {
    private val _items = MutableStateFlow<List<Mascota>>(emptyList())
    val items = _items.asStateFlow()

    fun cargar() = viewModelScope.launch {
        val r = api.listarMisMascotas()
        Logger.getLogger("MascotasViewModel").warning(r.body().toString())
        if (r.isSuccessful) _items.value = r.body().orEmpty()
    }
}
