package cl.clinipets.ui.catalogo

import androidx.lifecycle.ViewModel
import cl.clinipets.openapi.apis.CatlogoApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CatalogoViewModel @Inject constructor(
    private val api: CatlogoApi
) : ViewModel() {

}